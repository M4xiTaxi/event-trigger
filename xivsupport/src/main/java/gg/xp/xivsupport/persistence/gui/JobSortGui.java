package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.JobSortSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JobSortGui {

	private static final Logger log = LoggerFactory.getLogger(JobSortGui.class);
	private final JScrollPane jobListPane;
	private final JScrollPane partyPane;
	private final JButton resetButton;
	private final JButton exportButton;
	private final JButton importButton;

	private CustomTableModel<XivPlayerCharacter> partyTableModel;

	public JobSortGui(JobSortSetting sorter) {

		// Defined first so we can trigger refresh
		partyTableModel = CustomTableModel.builder(
						sorter::partyOrderPreview)
				.addColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addColumn(new CustomColumn<>("Job", XivPlayerCharacter::getJob, c -> c.setCellRenderer(new JobRenderer())))
				.setItemEquivalence((a, b) -> a.getId() == b.getId() && a.getJob() == b.getJob())
				.build();

		List<Job> items = sorter.getCurrentJailSort();
		RearrangeableList<Job> jobList = new RearrangeableList<>(items, l -> {
			sorter.setJailSort(l);
			log.info("Changed jail prio: {}", l.stream().map(Enum::name).collect(Collectors.joining(", ")));
			partyTableModel.fullRefresh();
		});
		resetButton = new JButton("Reset Order");
		resetButton.addActionListener(l -> {
			sorter.resetJailSort();
			partyTableModel.fullRefresh();
			jobList.setValues(sorter.getCurrentJailSort());
		});
		importButton = new JButton("Import");
		importButton.addActionListener(l -> {
			List<Job> newJobOrder = GuiUtil.doImportDialog("Import Job Order", s -> {
				try {
					List<Job> jobs = Arrays.stream(s.strip().split(","))
							.map(Job::valueOf)
							.toList();
					sorter.validateJobSortOrder(jobs);
					return jobs;
				} catch (Throwable t) {
					throw new ValidationError("Bad job order: " + t.getMessage(), t);
				}
			});
			sorter.setJailSort(newJobOrder);
			partyTableModel.fullRefresh();
			jobList.setValues(sorter.getCurrentJailSort());
		});
		exportButton = new JButton("Export");
		exportButton.addActionListener(l -> {
			String exportedJobList = sorter.getCurrentJailSort().stream().map(Enum::name).collect(Collectors.joining(","));
			GuiUtil.copyTextToClipboard(exportedJobList);
			JOptionPane.showMessageDialog(exportButton, "Copied to clipboard");
		});
		jobList.setCellRenderer(new JobRenderer());
		jobListPane = new JScrollPane(jobList);
		Dimension size = new Dimension(100, 50);
		jobListPane.setMinimumSize(size);
		jobListPane.setPreferredSize(size);
		JTable partyMembersTable = new JTable(8, 3);
		partyMembersTable.setModel(partyTableModel);
		partyTableModel.configureColumns(partyMembersTable);
		partyMembersTable.getSelectionModel().addListSelectionListener(l -> {
			int[] selectedRows = partyMembersTable.getSelectedRows();
			jobList.clearSelection();
			Arrays.stream(selectedRows).forEach(partyRow -> {
				XivPlayerCharacter player = partyTableModel.getValueForRow(partyRow);
				int jobRow = jobList.getValues().indexOf(player.getJob());
				jobList.addSelectionInterval(jobRow, jobRow);

			});
		});
		this.partyPane = new JScrollPane(partyMembersTable);
	}

	public JScrollPane getJobListPane() {
		return jobListPane;
	}

	public JPanel getJobListWithButtons() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(jobListPane, BorderLayout.CENTER);
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.LINE_AXIS));
		bottom.add(exportButton);
		bottom.add(importButton);
		panel.add(bottom, BorderLayout.NORTH);
		return panel;
	}

	public JScrollPane getPartyPane() {
		return partyPane;
	}

	public JButton getResetButton() {
		return resetButton;
	}

	public void externalRefresh() {
		SwingUtilities.invokeLater(() -> {
			if (partyTableModel != null) {
				partyTableModel.fullRefresh();
			}
		});
	}
}
