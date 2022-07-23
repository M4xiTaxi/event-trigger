package gg.xp.xivsupport.callouts;

import gg.xp.xivdata.data.duties.Duty;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CalloutGroup {
	private final Class<?> clazz;
	private final String name;
	private final BooleanSetting enabled;
	private final List<ModifiedCalloutHandle> callouts;

	public CalloutGroup(Class<?> clazz, String name, String topLevelPropStub, PersistenceProvider persistence, List<ModifiedCalloutHandle> callouts) {
		this.clazz = clazz;
		this.name = name;
		this.enabled = new BooleanSetting(persistence, topLevelPropStub + ".enabled", true);
		this.callouts = new ArrayList<>(callouts);
		updateChildren();
	}

	public void updateChildren() {
		callouts.forEach(call -> call.setEnabledByParent(enabled.get()));
	}

	public String getName() {
		return name;
	}

	public BooleanSetting getEnabled() {
		return enabled;
	}

	public List<ModifiedCalloutHandle> getCallouts() {
		return Collections.unmodifiableList(callouts);
	}

	public Class<?> getCallClass() {
		return clazz;
	}

	public KnownDuty getDuty() {
		CalloutRepo ann = getCallClass().getAnnotation(CalloutRepo.class);
		if (ann == null) {
			return KnownDuty.None;
		}
		else {
			return ann.duty();
		}
	}
}
