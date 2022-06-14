package gg.xp.xivsupport.models;

import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.state.RawXivCombatantInfo;

import java.io.Serial;

public class XivPlayerCharacter extends XivCombatant {
	@Serial
	private static final long serialVersionUID = 8719229961190925919L;
	private final Job job;
	private final XivWorld world;
	private final String firstName;
	private final String lastName;

	public XivPlayerCharacter(long id,
	                          String name,
	                          Job job,
	                          XivWorld world,
	                          boolean isLocalPlayerCharacter,
	                          long typeRaw,
	                          HitPoints hp,
	                          ManaPoints mp,
	                          Position pos,
	                          long bNpcId,
	                          long bNpcNameId,
	                          long partyType,
	                          long level,
	                          long ownerId,
	                          long shieldAmount) {
		super(id, name, true, isLocalPlayerCharacter, typeRaw, hp, mp, pos, bNpcId, bNpcNameId, partyType, level, ownerId, shieldAmount);
		this.job = job;
		this.world = world;
		String[] split = name.split("\\s+");
		if (split.length == 2) {
			this.firstName = split[0];
			this.lastName = split[1];
		}
		else {
			this.firstName = "";
			this.lastName = "";
		}
	}

	public Job getJob() {
		return job;
	}

	public XivWorld getWorld() {
		return world;
	}

	@Override
	public String toString() {
		return String.format("XivPlayerCharacter(0x%X:%s, %s, %s, %s, %s)", getId(), getName(), getJob(), getWorld(), getLevel(), isThePlayer());
	}

	@Override
	public RawXivCombatantInfo toRaw() {
		HitPoints hp = getHp();
		if (hp == null) {
			hp = new HitPoints(50_000, 50_000);
		}
		Position pos = getPos();
		if (pos == null) {
			pos = new Position(100, 100, 100, 0.0);
		}
		ManaPoints mp = getMp();
		if (mp == null) {
			mp = new ManaPoints(10_000, 10_000);
		}
		return new RawXivCombatantInfo(getId(), getName(), job.getId(), getRawType(), hp.getCurrent(), hp.getMax(), mp.getCurrent(), mp.getMax(), getLevel(), pos.x(), pos.y(), pos.z(), pos.heading(), 0, "TODO", getbNpcId(), getbNpcNameId(), getPartyType(), getOwnerId());
	}
}
