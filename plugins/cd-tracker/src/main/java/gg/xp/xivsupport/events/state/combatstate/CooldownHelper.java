package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ScanMe
public class CooldownHelper {

	private final StatusEffectRepository buffTracker;
	private final CdTracker cdTracker;
	private final XivState state;


	public CooldownHelper(StatusEffectRepository buffTracker, CdTracker cdTracker, XivState state) {
		this.buffTracker = buffTracker;
		this.cdTracker = cdTracker;
		this.state = state;
	}

	// TODO: one thing that isn't supported yet is stuff that has no buff whatsoever

	public List<CooldownStatus> getCooldowns(Predicate<XivCombatant> sourceFilter, Predicate<ExtendedCooldownDescriptor> cdFilter) {
		Predicate<XivCombatant> actualSourceFilter = xc -> sourceFilter.test(xc.walkParentChain());
		Map<CdTrackingKey, AbilityUsedEvent> cds = cdTracker
				.getCds(e -> cdFilter.test(e.getKey().getCooldown()) && actualSourceFilter.test(e.getKey().getSource()));
		List<BuffApplied> buffs = buffTracker.getBuffsAndPreapps().stream()
				.filter(ba -> actualSourceFilter.test(ba.getSource()))
				.toList();
		return cds.entrySet()
				.stream().map(e -> {
					CdTrackingKey key = e.getKey();
					AbilityUsedEvent abilityUsed = e.getValue();
					Instant replenishedAt = cdTracker.getReplenishedAt(key);
					ExtendedCooldownDescriptor cd = key.getCooldown();
					Predicate<BuffApplied> buffFilter;
					if (cd.autoBuffs()) {
						Set<Long> buffIds = abilityUsed.getEffects().stream().map(effect -> {
							if (effect instanceof StatusAppliedEffect sae) {
								return sae.getStatus().getId();
							}
							return null;
						}).filter(Objects::nonNull).collect(Collectors.toSet());
						buffFilter = b -> buffIds.contains(b.getBuff().getId());
					}
					else {
						buffFilter = b -> cd.buffIdMatches(b.getBuff().getId());
					}
					@Nullable BuffApplied buffApplied = buffs.stream()
							.filter(buffFilter)
							.filter(b -> b.getSource().walkParentChain().equals(abilityUsed.getSource().walkParentChain()))
							.max(Comparator.comparing(BuffApplied::getEffectiveHappenedAt))
							.orElse(null);
					return new CooldownStatus(key, abilityUsed, buffApplied, replenishedAt);
				}).toList();
	}

	public @Nullable CooldownStatus getPersonalCd(Cooldown cd) {
		return getCooldowns(XivCombatant::isThePlayer, cd::equals).stream().findFirst().orElse(null);
	}
}
