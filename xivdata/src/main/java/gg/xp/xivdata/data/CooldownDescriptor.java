package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface CooldownDescriptor {
	String getLabel();

	boolean abilityIdMatches(long abilityId);

	boolean buffIdMatches(long buffId);

	double getCooldown();

	default Duration getCooldownAsDuration() {
		return Duration.ofMillis((long) (getCooldown() * 1000L));
	}
	// Purposefully saying "primary" here - as some might require multiple CDs (see: Raw/Nascent)
	long getPrimaryAbilityId();

	int getMaxCharges();

	@Nullable Double getDurationOverride();
}
