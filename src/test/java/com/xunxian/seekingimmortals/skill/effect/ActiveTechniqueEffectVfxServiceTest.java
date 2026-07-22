package com.xunxian.seekingimmortals.skill.effect;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveTechniqueEffectVfxServiceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void trackingAndPerTickTrafficRemainBounded() {
        assertTrue(ActiveTechniqueEffectVfxService.statusIntervalTicks() >= 20);
        assertTrue(ActiveTechniqueEffectVfxService.maxStatusPacketsPerServerTick() > 0);
        assertTrue(ActiveTechniqueEffectVfxService.maxStatusPacketsPerServerTick() <= 8);
        assertTrue(ActiveTechniqueEffectVfxService.maxDissipatePacketsPerServerTick() > 0);
        assertTrue(ActiveTechniqueEffectVfxService.maxDissipatePacketsPerServerTick() <= 16);
        assertTrue(ActiveTechniqueEffectVfxService.maxTrackedEffects() >= 256);
        assertTrue(ActiveTechniqueEffectVfxService.maxTrackedEffects() <= 512);
        assertTrue(ActiveTechniqueEffectVfxService.maxTracksPerEntity() > 0);
        assertTrue(ActiveTechniqueEffectVfxService.maxTracksPerEntity()
                < ActiveTechniqueEffectVfxService.maxTrackedEffects());
        assertEquals("targeted_debuff",
                ActiveTechniqueEffectVfxService.normalizeSemantic(" Targeted_Debuff "));
    }

    @Test
    void serviceUsesRegisteredLiveEffectsAndFairCursor() throws Exception {
        String service = source("skill", "effect", "ActiveTechniqueEffectVfxService.java");

        assertTrue(service.contains("record TrackKey(UUID entityId, String semantic)"));
        assertTrue(service.contains("ForgeRegistries.MOB_EFFECTS.getKey(effect)"));
        assertTrue(service.contains("entity.getEffect(effect)"));
        assertTrue(service.contains("active.getDuration() > 0"));
        assertTrue(service.contains("MAX_STATUS_PACKETS_PER_SERVER_TICK"));
        assertTrue(service.contains("MAX_DISSIPATE_PACKETS_PER_SERVER_TICK"));
        assertTrue(service.contains("statusCursor"));
        assertTrue(service.contains("TechniqueVfxPacket.Kind.STATUS"));
        assertTrue(service.contains("TechniqueVfxPacket.Kind.DISSIPATE"));
        assertTrue(service.contains("level.getEntity(key.entityId())"));
        assertTrue(service.contains("public static void clearLevel(ServerLevel level)"));
        assertTrue(service.contains("public static void clearEntity(LivingEntity entity)"));
        assertTrue(service.contains("public static void onEntityLeave(LivingEntity entity)"));
        assertTrue(service.contains("reason == Entity.RemovalReason.CHANGED_DIMENSION"));
        assertTrue(service.contains("reason.shouldDestroy()"));
        assertTrue(service.contains("public static int relocateEntity(ServerPlayer player)"));
        assertTrue(service.contains("track.withRelocation("));
        assertTrue(service.contains("public static void clearAll()"));
        assertTrue(service.contains("if (track.pendingDissipate())"));
        assertTrue(service.contains("track.withPendingDissipate(center)"));
        assertTrue(service.contains("lastDissipateOffset"));
        assertTrue(service.contains("now >= track.observedEndsAt()"));
        assertTrue(service.contains("track.withObservedEffects(active.effectIds())"));
    }

    @Test
    void sustainedSpellFamiliesRegisterTheirActualMobEffects() throws Exception {
        String formation = source("skill", "effect", "spell", "FormationSpell.java");
        String targeted = source("skill", "effect", "spell", "TargetedDebuffSpell.java");
        String area = source("skill", "effect", "spell", "AreaDebuffSpell.java");
        String elemental = source("skill", "effect", "spell", "ElementalAreaSpell.java");

        assertEquals(5, count(formation, "ActiveTechniqueEffectVfxService.track("));
        assertTrue(formation.contains("MobEffect[] appliedEffects = form.applyTarget("));
        assertTrue(formation.contains("glowingApplied ? MobEffects.GLOWING : null"));
        assertTrue(targeted.contains("ActiveTechniqueEffectVfxService.track("));
        assertTrue(targeted.contains("primaryApplied ? primaryEffect : null"));
        assertTrue(targeted.contains("return target.addEffect(new MobEffectInstance("));
        assertTrue(area.contains("ActiveTechniqueEffectVfxService.track("));
        assertTrue(area.contains("secondaryApplied ? secondaryEffect : null"));
        assertTrue(area.contains("return target.addEffect(new MobEffectInstance("));
        assertTrue(elemental.contains("MobEffect[] appliedEffects = element.applyEffects("));
        assertTrue(elemental.contains("if (target.addEffect(new MobEffectInstance("));
        assertTrue(formation.contains("if (add(target, effect, durationTicks, amplifier))"));
    }

    @Test
    void forgeEventsDriveCleanupRelocationAndServerTicks() throws Exception {
        String events = source("event", "ModEvents.java");

        assertTrue(events.contains("ActiveTechniqueEffectVfxService.serverTick(event.getServer())"));
        assertTrue(events.contains("ActiveTechniqueEffectVfxService.clearLevel(serverLevel)"));
        assertTrue(events.contains("ActiveTechniqueEffectVfxService.onEntityLeave(living)"));
        assertTrue(events.contains("ActiveTechniqueEffectVfxService.relocateEntity(player)"));
        assertTrue(events.contains("ActiveTechniqueEffectVfxService.clearEntity(player)"));
        assertTrue(events.contains("ActiveTechniqueEffectVfxService.clearAll()"));
    }

    private static int count(String source, String token) {
        int matches = 0;
        int from = 0;
        while ((from = source.indexOf(token, from)) >= 0) {
            matches++;
            from += token.length();
        }
        return matches;
    }

    private static String source(String... relative) throws Exception {
        Path path = JAVA_ROOT;
        for (String part : relative) {
            path = path.resolve(part);
        }
        return Files.readString(path);
    }
}
