package com.xunxian.seekingimmortals.combat.status;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Emits bounded server-authoritative VFX intents for custom combat statuses. */
public final class StatusVfxService {
    private static final int MIN_EMIT_INTERVAL_TICKS = 10;
    private static final int STALE_ENTRY_TICKS = 20 * 60;
    private static final int MAX_EMISSIONS_PER_TICK = 48;
    private static final Map<EmissionKey, Long> LAST_EMIT_TICK = new ConcurrentHashMap<>();

    private static long lastPruneTick = Long.MIN_VALUE;
    private static long budgetTick = Long.MIN_VALUE;
    private static int emissionsThisTick;

    private StatusVfxService() {}

    public static void emitApplied(LivingEntity entity, SeekingStatusEffect effect, int amplifier) {
        emit(entity, effect, amplifier, TechniqueVfxPacket.Kind.STATUS, 24, 4, false);
    }

    public static void emitPulse(LivingEntity entity, SeekingStatusEffect effect, int amplifier) {
        emit(entity, effect, amplifier, TechniqueVfxPacket.Kind.STATUS, 10, 2, true);
    }

    public static void emitDissipate(LivingEntity entity, SeekingStatusEffect effect, int amplifier) {
        emit(entity, effect, amplifier, TechniqueVfxPacket.Kind.DISSIPATE, 18, 3, false);
    }

    private static void emit(LivingEntity entity, SeekingStatusEffect effect, int amplifier,
                             TechniqueVfxPacket.Kind kind, int baseIntensity, int intensityPerLevel,
                             boolean throttlePulse) {
        if (entity == null || effect == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        String statusId = normalize(effect.getStatusId());
        if (statusId.isEmpty()
                || (throttlePulse && !claimPulse(level, entity, statusId))
                || !claimBudget(level)) {
            return;
        }

        VisualProfile profile = profile(statusId, effect.isBeneficial());
        int safeAmplifier = Mth.clamp(amplifier, 0, 8);
        int intensity = baseIntensity + safeAmplifier * intensityPerLevel;
        double radius = Mth.clamp(Math.max(0.65D, entity.getBbWidth() * 0.9D), 0.65D, 2.5D);
        double centerHeight = Mth.clamp(entity.getBbHeight() * 0.52D, 0.35D, 1.8D);
        Vec3 center = entity.position().add(0.0D, centerHeight, 0.0D);
        Vec3 top = center.add(0.0D, Math.max(0.3D, entity.getBbHeight() * 0.35D), 0.0D);
        long seed = level.getGameTime() * 31L
                ^ entity.getUUID().getMostSignificantBits()
                ^ entity.getUUID().getLeastSignificantBits()
                ^ ((long) statusId.hashCode() << 17)
                ^ ((long) kind.ordinal() << 52);

        TechniqueVfxPacket.send(level, kind, profile.family(), profile.motif(),
                center, top, radius, intensity, seed);
    }

    private static boolean claimPulse(ServerLevel level, LivingEntity entity, String statusId) {
        long now = level.getGameTime();
        EmissionKey key = new EmissionKey(level.dimension().location().toString(), entity.getUUID(), statusId);
        Long previous = LAST_EMIT_TICK.get(key);
        if (previous != null && now >= previous && now - previous < MIN_EMIT_INTERVAL_TICKS) {
            return false;
        }
        LAST_EMIT_TICK.put(key, now);
        prune(now);
        return true;
    }

    private static boolean claimBudget(ServerLevel level) {
        long now = level.getGameTime();
        if (budgetTick != now) {
            budgetTick = now;
            emissionsThisTick = 0;
        }
        if (emissionsThisTick >= MAX_EMISSIONS_PER_TICK) {
            return false;
        }
        emissionsThisTick++;
        return true;
    }

    private static void prune(long now) {
        if (lastPruneTick != Long.MIN_VALUE && now >= lastPruneTick
                && now - lastPruneTick < STALE_ENTRY_TICKS) {
            return;
        }
        lastPruneTick = now;
        LAST_EMIT_TICK.entrySet().removeIf(entry -> now < entry.getValue()
                || now - entry.getValue() > STALE_ENTRY_TICKS);
    }

    private static VisualProfile profile(String statusId, boolean beneficial) {
        return switch (statusId) {
            case "burn" -> visual(TechniqueVfxPalette.Family.FIRE, TechniqueVfxPacket.Motif.CHANNEL);
            case "frozen" -> visual(TechniqueVfxPalette.Family.ICE, TechniqueVfxPacket.Motif.SEAL);
            case "soul_shock", "soul_wound" -> visual(
                    TechniqueVfxPalette.Family.SOUL, TechniqueVfxPacket.Motif.GHOST);
            case "illusion" -> visual(
                    TechniqueVfxPalette.Family.ILLUSION, TechniqueVfxPacket.Motif.ILLUSION);
            case "karma" -> visual(TechniqueVfxPalette.Family.LIGHT, TechniqueVfxPacket.Motif.BUDDHIST);
            case "demonic_qi" -> visual(TechniqueVfxPalette.Family.DARK, TechniqueVfxPacket.Motif.GHOST);
            case "foundation_unstable" -> visual(
                    TechniqueVfxPalette.Family.EARTH, TechniqueVfxPacket.Motif.CHANNEL);
            case "marrow_drain" -> visual(TechniqueVfxPalette.Family.BLOOD, TechniqueVfxPacket.Motif.CHANNEL);
            case "seal_nascent" -> visual(TechniqueVfxPalette.Family.SOUL, TechniqueVfxPacket.Motif.SEAL);
            case "conceal_qi" -> visual(TechniqueVfxPalette.Family.VOID, TechniqueVfxPacket.Motif.ILLUSION);
            case "poison" -> visual(TechniqueVfxPalette.Family.WOOD, TechniqueVfxPacket.Motif.CHANNEL);
            case "bleed" -> visual(TechniqueVfxPalette.Family.BLOOD, TechniqueVfxPacket.Motif.BLADE);
            case "stun" -> visual(TechniqueVfxPalette.Family.THUNDER, TechniqueVfxPacket.Motif.SEAL);
            case "shield" -> visual(TechniqueVfxPalette.Family.LIGHT, TechniqueVfxPacket.Motif.SHIELD);
            case "fear" -> visual(TechniqueVfxPalette.Family.DARK, TechniqueVfxPacket.Motif.GHOST);
            case "berserk" -> visual(TechniqueVfxPalette.Family.FIRE, TechniqueVfxPacket.Motif.CHANNEL);
            case "qi_disorder" -> visual(TechniqueVfxPalette.Family.SOUL, TechniqueVfxPacket.Motif.CHANNEL);
            case "sword_intent" -> visual(TechniqueVfxPalette.Family.METAL, TechniqueVfxPacket.Motif.BLADE);
            case "array_bind" -> visual(TechniqueVfxPalette.Family.VOID, TechniqueVfxPacket.Motif.FORMATION);
            case "heal_hot" -> visual(TechniqueVfxPalette.Family.WOOD, TechniqueVfxPacket.Motif.HEAL);
            case "tribulation_mark" -> visual(
                    TechniqueVfxPalette.Family.THUNDER, TechniqueVfxPacket.Motif.SEAL);
            default -> fallbackProfile(statusId, beneficial);
        };
    }

    private static VisualProfile fallbackProfile(String statusId, boolean beneficial) {
        if (contains(statusId, "shield", "barrier", "guard", "armor")) {
            return visual(TechniqueVfxPalette.Family.LIGHT, TechniqueVfxPacket.Motif.SHIELD);
        }
        if (contains(statusId, "heal", "regen", "recovery", "life")) {
            return visual(TechniqueVfxPalette.Family.WOOD, TechniqueVfxPacket.Motif.HEAL);
        }
        if (contains(statusId, "cleanse", "purify", "dispel")) {
            return visual(TechniqueVfxPalette.Family.LIGHT, TechniqueVfxPacket.Motif.CLEANSE);
        }
        if (contains(statusId, "bleed", "blood")) {
            return visual(TechniqueVfxPalette.Family.BLOOD, TechniqueVfxPacket.Motif.BLADE);
        }
        if (contains(statusId, "poison", "toxin", "venom")) {
            return visual(TechniqueVfxPalette.Family.WOOD, TechniqueVfxPacket.Motif.CHANNEL);
        }
        if (contains(statusId, "illusion", "mirage", "dream")) {
            return visual(TechniqueVfxPalette.Family.ILLUSION, TechniqueVfxPacket.Motif.ILLUSION);
        }
        if (contains(statusId, "soul", "ghost", "spirit")) {
            return visual(TechniqueVfxPalette.Family.SOUL, TechniqueVfxPacket.Motif.GHOST);
        }
        if (contains(statusId, "demon", "demonic", "corrupt")) {
            return visual(TechniqueVfxPalette.Family.DARK, TechniqueVfxPacket.Motif.GHOST);
        }
        if (contains(statusId, "sword", "blade")) {
            return visual(TechniqueVfxPalette.Family.METAL, TechniqueVfxPacket.Motif.BLADE);
        }
        if (contains(statusId, "seal", "bind", "stun")) {
            return visual(TechniqueVfxPalette.Family.VOID, TechniqueVfxPacket.Motif.SEAL);
        }
        return beneficial
                ? visual(TechniqueVfxPalette.Family.LIGHT, TechniqueVfxPacket.Motif.CHANNEL)
                : visual(TechniqueVfxPalette.Family.DARK, TechniqueVfxPacket.Motif.CHANNEL);
    }

    private static VisualProfile visual(TechniqueVfxPalette.Family family, TechniqueVfxPacket.Motif motif) {
        return new VisualProfile(family, motif);
    }

    private static boolean contains(String value, String... parts) {
        for (String part : parts) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record VisualProfile(TechniqueVfxPalette.Family family, TechniqueVfxPacket.Motif motif) {}

    private record EmissionKey(String dimensionId, UUID entityId, String statusId) {}
}
