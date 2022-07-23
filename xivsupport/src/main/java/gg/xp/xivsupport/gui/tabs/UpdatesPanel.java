package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SimplifiedPropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import gg.xp.xivsupport.sys.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UpdatesPanel extends TitleBorderFullsizePanel implements TabAware {
	private static final Logger log = LoggerFactory.getLogger(UpdatesPanel.class);
	private static final String propsOverrideFileName = "update.properties";
	private static final ExecutorService exs = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("UpdateCheck"));
	private JButton button;
	private volatile UpdateCheckStatus updateCheckStatus = UpdateCheckStatus.NOT_STARTED;
	private JLabel checkingLabel;
	private File installDir;
	private File propsOverride;
	private PersistenceProvider updatePropsFilePers;
	private static final boolean isIde = Platform.isInIDE();
	private final BooleanSetting updateCheckNag;
	private boolean updateCheckedThisRun;

	private enum UpdateCheckStatus {
		NOT_STARTED,
		IN_PROGRESS,
		UPDATE_AVAILABLE,
		NO_UPDATE,
		ERROR
	}

	public UpdatesPanel(PersistenceProvider pers) {
		super("Updates");
		updateCheckNag = new BooleanSetting(pers, "updater.nag-next-update", true);
		try {
			this.installDir = Platform.getInstallDir();
			propsOverride = Paths.get(installDir.toString(), propsOverrideFileName).toFile();
			updatePropsFilePers = new SimplifiedPropertiesFilePersistenceProvider(propsOverride);
		}
		catch (Throwable e) {
			log.error("Error setting up updates tab", e);
			add(new JLabel("There was an error. You can try running the updater manually by running triggevent-upd.exe."));
			return;
		}
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.weighty = 0;
		{
			checkingLabel = new JLabel("Update Status");
//			doUpdateCheckInBackground();
			add(checkingLabel, c);
		}
		button = new JButton("Check for Updates and Restart");
		button.addActionListener(l -> updateNow());
		c.gridy++;
		//noinspection InstanceVariableUsedBeforeInitialized
		add(new JLabel("Install Dir: " + installDir), c);
		c.gridy++;
		JPanel content = new JPanel();
		//noinspection InstanceVariableUsedBeforeInitialized
		StringSetting branchSetting = new StringSetting(updatePropsFilePers, "branch", "stable");
		content.add(new StringSettingGui(branchSetting, "Branch").getComponent());
		branchSetting.addListener(this::doUpdateCheckInBackground);
		content.add(button);
		add(content, c);
		c.gridy++;
		StringSetting urlTemplateSetting = new StringSetting(updatePropsFilePers, "url_template", "https://xpdota.github.io/event-trigger/%s/v2/%s");
		StringSettingGui templateGui = new StringSettingGui(urlTemplateSetting, "URL Template");
		templateGui.getTextBoxOnly().setColumns(50);
		add(templateGui.getComponent(), c);
		urlTemplateSetting.addListener(this::doUpdateCheckInBackground);
		c.gridy++;

		JButton openInstallDirButton = new JButton("Open Install Dir");
		openInstallDirButton.addActionListener(l -> GuiUtil.openFile(installDir));
		add(openInstallDirButton, c);
		c.gridy++;

		JCheckBox nagCheckbox = new BooleanSettingGui(updateCheckNag, "Update Popup on Startup").getComponent();
		add(nagCheckbox, c);
		c.gridy++;
		c.weighty = 1;
		add(Box.createGlue(), c);
		new RefreshLoop<>(
				"UpdatePeriodicCheck",
				this,
				i -> doUpdateCheckInBackground(),
				// 15 minutes * 60 seconds * 1000 ms
				i -> 15 * 60 * 1000L
		).start();
	}

	private void updateNow() {
		exs.submit(() -> {
			// First, try to update the updater itself
			try {
				try {
					Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.Update");
					clazz.getMethod("updateTheUpdater").invoke(null);
				}
				catch (Throwable e) {
					Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.UpdateCopyForLegacyMigration");
					clazz.getMethod("updateTheUpdater").invoke(null);
				}
			}
			catch (Throwable e) {
				log.error("Error updating the updater - you may not have a recent enough version (or are running in an IDE).", e);
				JOptionPane.showMessageDialog(SwingUtilities.getRoot(button), "There was an error updating the updater. This may fix itself after updates. ");
			}
			try {
				// Desktop.open seems to open it in such a way that when we exit, we release the mutex, so the updater
				// can relaunch the application correctly.
				if (Platform.isWindows()) {
					Desktop.getDesktop().open(Paths.get(installDir.toString(), "triggevent-upd.exe").toFile());
				}
				else {
					Runtime.getRuntime().exec(new String[]{"sh", "triggevent-upd.sh"});
				}
			}
			catch (Throwable e) {
				log.error("Error launching updater", e);
				JOptionPane.showMessageDialog(SwingUtilities.getRoot(button), "There was an error launching the updater. You can try running the updater manually by running triggevent-upd.exe, or reinstall if that doesn't work.");
				return;
			}
			System.exit(0);
		});
	}

	private void setUpdateCheckStatus(UpdateCheckStatus updateCheckStatus) {
		this.updateCheckStatus = updateCheckStatus;
		checkingLabel.setText(
				switch (updateCheckStatus) {
					case NOT_STARTED -> "Update Status";
					case IN_PROGRESS -> "Checking for updates...";
					case NO_UPDATE -> "It looks like you are up to date.";
					case UPDATE_AVAILABLE -> "There are updates available!";
					case ERROR ->
							"Automatic Check Failed, but you can try updating anyway. Perhaps the branch does not exist?";
				}
		);
		if (updateCheckStatus != UpdateCheckStatus.IN_PROGRESS) {
			notifyParents();
		}
		if (updateCheckStatus == UpdateCheckStatus.NO_UPDATE || updateCheckStatus == UpdateCheckStatus.ERROR) {
			updateCheckedThisRun = true;
		}
		if (!updateCheckedThisRun && updateCheckNag.get() && updateCheckStatus == UpdateCheckStatus.UPDATE_AVAILABLE) {
			Object[] options = {"Yes", "No", "No, Don't Ask Again"};
			updateCheckedThisRun = true;
			int answer = JOptionPane.showOptionDialog(
					SwingUtilities.getWindowAncestor(checkingLabel),
					"There is an update available. Would you like to install it?",
					"Update",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[0]);
			log.info("Update answer: {}", answer);
			switch (answer) {
				case 0 -> updateNow();
				case 1 -> {
				}
				case 2 -> updateCheckNag.set(false);
			}
		}
	}

	@Override
	public boolean hasWarning() {
		if (isIde) {
			return false;
		}
		return updateCheckStatus == UpdateCheckStatus.UPDATE_AVAILABLE || updateCheckStatus == UpdateCheckStatus.ERROR;
	}

	private void doUpdateCheckInBackground() {
		exs.submit(() -> {
			setUpdateCheckStatus(UpdateCheckStatus.IN_PROGRESS);
			try {
				Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.Update");
				boolean result = (boolean) clazz.getMethod("justCheck", Consumer.class).invoke(null, (Consumer<String>) s -> log.info("From Updater: {}", s));
				if (result) {
					setUpdateCheckStatus(UpdateCheckStatus.UPDATE_AVAILABLE);
				}
				else {
					setUpdateCheckStatus(UpdateCheckStatus.NO_UPDATE);
				}
			}
			catch (Throwable firstError) {
				log.error("Error updating, will try backup updater", firstError);
				try {
					Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.UpdateCopyForLegacyMigration");
					boolean result = (boolean) clazz.getMethod("justCheck", Consumer.class).invoke(null, (Consumer<String>) s -> log.info("From Updater: {}", s));
					if (result) {
						setUpdateCheckStatus(UpdateCheckStatus.UPDATE_AVAILABLE);
					}
					else {
						setUpdateCheckStatus(UpdateCheckStatus.NO_UPDATE);
					}
				}
				catch (Throwable e) {
					log.error("Error checking for updates - you may not have a recent enough version (or are running in an IDE).", e);
					setUpdateCheckStatus(UpdateCheckStatus.ERROR);
				}
			}
		});
	}
}
