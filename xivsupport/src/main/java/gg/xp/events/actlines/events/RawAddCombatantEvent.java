package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.Event;
import gg.xp.events.models.XivCombatant;

/**
 * Equivalent to an ACT 03 line. This event is intentionally left pretty bare, because
 * you should get all of the info from XivState.
 */
@SystemEvent
public class RawAddCombatantEvent extends BaseEvent {
	private static final long serialVersionUID = 3615674340829313314L;
	private final XivCombatant entity;

	public RawAddCombatantEvent(XivCombatant entity) {
		this.entity = entity;
	}

	public XivCombatant getEntity() {
		return entity;
	}

}
