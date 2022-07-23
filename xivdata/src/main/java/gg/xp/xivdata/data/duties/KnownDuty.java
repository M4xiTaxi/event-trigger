package gg.xp.xivdata.data.duties;

public enum KnownDuty implements Duty {
	None("General", Expansion.GENERAL, DutyType.OTHER),
	Odin("Urth's Fount", Expansion.ARR, DutyType.TRIAL),
	UWU("Weapon's Refrain", Expansion.SB, DutyType.ULTIMATE),
	P1S("P1S", Expansion.EW, DutyType.SAVAGE_RAID),
	P2S("P2S", Expansion.EW, DutyType.SAVAGE_RAID),
	P3S("P3S", Expansion.EW, DutyType.SAVAGE_RAID),
	P4S("P4S", Expansion.EW, DutyType.SAVAGE_RAID),
	EndsingerEx("EX3", Expansion.EW, DutyType.TRIAL_EX),
	Dragonsong("Dragonsong", Expansion.EW, DutyType.ULTIMATE);

	private final String name;
	private final Expansion expac;
	private final DutyType type;

	KnownDuty(String name, Expansion expac, DutyType type) {
		this.name = name;
		this.expac = expac;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Expansion getExpac() {
		return expac;
	}

	@Override
	public DutyType getType() {
		return type;
	}
}
