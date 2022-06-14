package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.TickInfo;
import gg.xp.xivsupport.events.state.combatstate.TickTracker;
import gg.xp.xivsupport.events.state.combatstate.TickUpdatedEvent;
import gg.xp.xivsupport.events.triggers.jobs.DotRefreshReminders;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.RefreshType;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public class DotTrackerOverlay extends XivOverlay {

	private static final Logger log = LoggerFactory.getLogger(DotTrackerOverlay.class);

	private final IntSetting numberOfRows;
	private final TickTracker ticker;

	private final DotRefreshReminders dots;
	private final CustomTableModel<VisualDotInfo> tableModel;
	private final JTable table;
	private volatile List<BuffApplied> currentDots = Collections.emptyList();
	private volatile List<VisualDotInfo> croppedDots = Collections.emptyList();
	private final BooleanSetting showTicks;
	private volatile boolean tickUpdatePending;

	private static final int BAR_WIDTH = 150;


	public DotTrackerOverlay(PersistenceProvider persistence, DotRefreshReminders dots, OverlayConfig oc, TickTracker ticker) {
		super("Dot Tracker", "dot-tracker.overlay", oc, persistence);
		this.numberOfRows = dots.getNumberToDisplay();
		this.showTicks = new BooleanSetting(persistence, "dot-tracker.overlay.show-ticks", true);
		this.ticker = ticker;
		numberOfRows.addListener(this::repackSize);
		this.dots = dots;
		tableModel = CustomTableModel.builder(() -> croppedDots)
				.addColumn(new CustomColumn<>("Icon", c -> c.getEvent().getBuff(), c -> {
					c.setCellRenderer(new ActionAndStatusRenderer(true, false, false));
					c.setMaxWidth(20);
					c.setMinWidth(20);
				}))
				.addColumn(new CustomColumn<>("Bar", Function.identity(),
						c -> {
							c.setCellRenderer(new DotBarRenderer(dots));
							c.setMaxWidth(BAR_WIDTH);
							c.setMinWidth(BAR_WIDTH);
						}))
				.build();
//		getPanel().setPreferredSize();
		table = new JTable(tableModel);
//		table.getPreferredSize();
		table.setOpaque(false);
		tableModel.configureColumns(table);
		table.setCellSelectionEnabled(false);
		getPanel().add(table);
		// Logic behind 200ms refresh basis: 150 pixel bar with a standard 30 second dot means each pixel would
		// represent 200ms. However, we need to factor scaling into the equation.
		RefreshLoop<DotTrackerOverlay> refresher = new RefreshLoop<>("DotTrackerOverlay", this, DotTrackerOverlay::refresh, dt -> dt.calculateScaledFrameTime(200));
		repackSize();
		refresher.start();
	}

	@HandleEvents
	public void ticksUpdated(EventContext context, TickUpdatedEvent event) {
		if (showTicks.get()) {
			tickUpdatePending = true;
		}
	}

	@Override
	protected void repackSize() {
		table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * numberOfRows.get()));
		super.repackSize();
	}


	private RefreshType getAndSort() {
		if (!getEnabled().get()) {
			croppedDots = Collections.emptyList();
			return RefreshType.NONE;
		}
		List<BuffApplied> newCurrentDots = dots.getCurrentDots();
		// Logic for determining refresh type:
		// If data changed, do a full table model refresh
		// If nothing changed, but we have data, do a repaint only
		// If there's just plain nothing, do nothing. Unless it was the first time we saw nothing, in which case we
		// still need to do the table model refresh.
		if (newCurrentDots.equals(currentDots) && !tickUpdatePending) {
			if (newCurrentDots.isEmpty()) {
				return RefreshType.NONE;
			}
			return RefreshType.REPAINT;
		}
		else {
			// Technically, there's a concurrency issue here, but not enough to make a practical difference
			tickUpdatePending = false;
			if (newCurrentDots.isEmpty()) {
				currentDots = Collections.emptyList();
				croppedDots = Collections.emptyList();
				return RefreshType.FULL;
			}
			currentDots = newCurrentDots;
			Map<Long, List<BuffApplied>> dotsForBuffId = newCurrentDots
					.stream()
					.collect(Collectors.groupingBy(dot -> dot.getBuff().getId()));
			List<VisualDotInfo> out = new ArrayList<>();
			dotsForBuffId.forEach((unused, buffs) -> {
				MutableLong ml = new MutableLong(buffs.get(0).getEstimatedRemainingDuration().toMillis());
				buffs.stream()
						.sorted(Comparator.comparing(b -> b.getEstimatedRemainingDuration().toMillis()))
						.collect(Collectors.groupingBy(b -> {
							long est = b.getEstimatedRemainingDuration().toMillis();
							long lastValue = ml.longValue();
							if (Math.abs(est - lastValue) < 500) {
								return lastValue;
							}
							else {
								return est;
							}
						}))
						.forEach((k, v) -> {
							BuffApplied first = v.get(0);
							if (v.size() == 1) {
								if (first.getTarget().isThePlayer()) {
									out.add(new VisualDotInfo(first, first.getBuff().getName(), null));
								}
								else {
									TickInfo tick = showTicks.get() ? ticker.getTick(first.getTarget()) : null;
									out.add(new VisualDotInfo(first, null, tick));
								}
							}
							else {
								String firstTargetName = first.getTarget().getName();
								if (v.stream().allMatch(e -> e.getTarget().getName().equals(firstTargetName))) {
									out.add(new VisualDotInfo(first, String.format("%s x%s", firstTargetName, v.size()), null));
								}
								else {
									out.add(new VisualDotInfo(first, String.format("%s Targets", v.size()), null));
								}
							}
						});
				buffs.sort(Comparator.comparing(BuffApplied::getEstimatedRemainingDuration));
			});

			croppedDots = out.stream()
					.sorted(Comparator.<VisualDotInfo, Long>comparing(event -> event.getEvent().getTarget().getHp() != null ? event.getEvent().getTarget().getHp().getMax() : 0)
							.reversed()
							.thenComparing(event -> event.getEvent().getTarget().getId()))
					.limit(numberOfRows.get())
					.collect(Collectors.toList());
			return RefreshType.FULL;
		}
	}


	private void refresh() {
		RefreshType refreshTypeNeeded = getAndSort();
		switch (refreshTypeNeeded) {
			case FULL -> tableModel.fullRefresh();
			case REPAINT -> table.repaint();
		}
	}

	public BooleanSetting showTicks() {
		return showTicks;
	}
}
