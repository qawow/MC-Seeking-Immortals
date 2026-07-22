package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueLifecycleVfxServiceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void selfBuffMotifsRemainSemanticAndBounded() {
        assertEquals(TechniqueVfxPacket.Motif.SHIELD,
                TechniqueLifecycleVfxService.selfBuffMotif("shield", ""));
        assertEquals(TechniqueVfxPacket.Motif.CLEANSE,
                TechniqueLifecycleVfxService.selfBuffMotif("buff", "purify_qi"));
        assertEquals(TechniqueVfxPacket.Motif.HEAL,
                TechniqueLifecycleVfxService.selfBuffMotif("regeneration", ""));
        assertEquals(TechniqueVfxPacket.Motif.CHANNEL,
                TechniqueLifecycleVfxService.selfBuffMotif("scan", "conceal_qi"));
        assertEquals(TechniqueVfxPacket.Motif.TELEPORT,
                TechniqueLifecycleVfxService.selfBuffMotif("movement", ""));
        assertEquals(TechniqueVfxPacket.Motif.TELEPORT,
                TechniqueLifecycleVfxService.selfBuffMotif("ghost_walk", ""));
        assertEquals(TechniqueVfxPacket.Motif.DAO,
                TechniqueLifecycleVfxService.selfBuffMotif("buff", "sword_intent"));
        assertTrue(TechniqueLifecycleVfxService.selfBuffStatusIntervalTicks() >= 40);
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffsPerPlayer() >= 42);
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffsPerPlayer() <= 64);
        assertTrue(TechniqueLifecycleVfxService.maxTrackedSelfBuffs()
                >= TechniqueLifecycleVfxService.maxSelfBuffsPerPlayer());
        assertTrue(TechniqueLifecycleVfxService.maxTrackedSelfBuffs() <= 512);
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffStatusPulsesPerTick() > 0);
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffStatusPulsesPerTick() <= 4);
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffStatusPulsesPerServerTick()
                >= TechniqueLifecycleVfxService.maxSelfBuffStatusPulsesPerTick());
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffStatusPulsesPerServerTick() <= 16);
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffDissipatesPerServerTick() > 0);
        assertTrue(TechniqueLifecycleVfxService.maxSelfBuffDissipatesPerServerTick() <= 16);
        assertTrue(TechniqueLifecycleVfxService.maxPendingSelfBuffStatuses()
                >= TechniqueLifecycleVfxService.maxTrackedSelfBuffs());
        assertTrue(TechniqueLifecycleVfxService.maxPendingSelfBuffDissipates()
                >= TechniqueLifecycleVfxService.maxSelfBuffsPerPlayer());
        assertFalse(TechniqueLifecycleVfxService.selfBuffPersistentDataKey().isBlank());
    }

    @Test
    void selfBuffLifetimeCannotOutliveTheTechniqueApplication() {
        long castAt = 10_000L;
        long originalEndsAt = TechniqueLifecycleVfxService.boundedSelfBuffEndsAt(
                castAt, 160, 1_200, Long.MAX_VALUE);
        assertEquals(castAt + 160L, originalEndsAt,
                "an external longer effect must not extend this technique palette");

        long restoredAt = castAt + 80L;
        assertEquals(originalEndsAt, TechniqueLifecycleVfxService.boundedSelfBuffEndsAt(
                restoredAt, 160, 1_120, originalEndsAt));
        assertEquals(restoredAt, TechniqueLifecycleVfxService.boundedSelfBuffEndsAt(
                restoredAt, 160, 1_120, restoredAt));

        long recastEndsAt = TechniqueLifecycleVfxService.boundedSelfBuffEndsAt(
                restoredAt, 160, 1_200, Long.MAX_VALUE);
        assertTrue(recastEndsAt > originalEndsAt, "recasting may refresh its own boundary");
    }

    @Test
    void selfBuffPauseFreezesOnlyItsBoundedRemainingLifetime() {
        long pauseAt = 10_080L;
        assertEquals(80, TechniqueLifecycleVfxService.boundedSelfBuffRemainingTicks(
                pauseAt, 160, 1_120, 10_160L));
        assertEquals(20, TechniqueLifecycleVfxService.boundedSelfBuffRemainingTicks(
                pauseAt, 160, 20, 10_160L));
        assertEquals(0, TechniqueLifecycleVfxService.boundedSelfBuffRemainingTicks(
                pauseAt, 160, 1_120, pauseAt));

        long restoredAt = 20_000L;
        assertEquals(restoredAt + 80L, TechniqueLifecycleVfxService.boundedSelfBuffEndsAt(
                restoredAt, 80, 1_120, Long.MAX_VALUE));
    }

    @Test
    void selfBuffTracksBindRegisteredEffectsAndExposeSessionLifecycleHooks() throws Exception {
        String service = source("skill", "effect", "TechniqueLifecycleVfxService.java");
        String spell = source("skill", "effect", "spell", "SelfBuffSpell.java");
        String resolver = source("skill", "effect", "AbstractTechniqueEffectResolver.java");
        String events = source("event", "ModEvents.java");

        assertTrue(service.contains("ResourceLocation effectId"));
        assertTrue(service.contains("String safeSemantic"));
        assertTrue(service.contains("new SelfBuffKey(playerId, safeSemantic, effectId)"));
        assertTrue(service.contains("ForgeRegistries.MOB_EFFECTS.getKey(effect)"));
        assertTrue(service.contains("hasActiveEffect(player, entry.getKey().effectId())"));
        assertTrue(service.contains("TechniqueVfxPacket.Kind.DISSIPATE"));
        assertTrue(service.contains("public static int restoreSelfBuffs(ServerPlayer player)"));
        assertTrue(service.contains("public static void pauseSelfBuffs(ServerPlayer player)"));
        assertTrue(service.contains("public static int relocateSelfBuffs(ServerPlayer player)"));
        assertTrue(service.contains("public static void serverTick(MinecraftServer server)"));
        assertTrue(service.contains("public static void clearRuntimeState()"));
        assertTrue(service.contains("player.getPersistentData().remove(SELF_BUFF_DATA_KEY)"));
        assertTrue(service.contains("PENDING_SELF_BUFF_STATUSES.add(entry.getKey())"));
        assertTrue(service.contains("PENDING_SELF_BUFF_DISSIPATES.addLast("));
        assertTrue(service.contains(
                "PENDING_SELF_BUFF_STATUSES.size() < MAX_PENDING_SELF_BUFF_STATUSES"));
        assertTrue(service.contains(
                "PENDING_SELF_BUFF_DISSIPATES.size() >= MAX_PENDING_SELF_BUFF_DISSIPATES"));
        assertTrue(service.contains(
                "while (statusIterator.hasNext() && statusBudget > 0)"));
        assertTrue(service.contains(
                "while (!PENDING_SELF_BUFF_DISSIPATES.isEmpty() && dissipateBudget > 0)"));
        assertTrue(service.contains("lastTickServer == server && lastTickTime == now"));
        assertTrue(service.contains("alignSelfBuffStatusPulses(playerId, visualKey, now)"));
        assertTrue(service.contains("Set<SelfBuffOwnerVisualKey> emittedStatusVisuals"));
        assertTrue(service.contains("hasLiveVisualTrack(player.getUUID(), visualKey)"));
        assertTrue(service.contains("track.withPausedRemaining(remainingTicks)"));
        assertTrue(service.contains("saved.putString(SEMANTIC_TAG"));
        assertTrue(service.contains("saved.putInt(CAPTURED_REMAINING_TAG"));
        assertTrue(service.contains("saved.putLong(ENDS_AT_TAG"));
        assertTrue(service.contains("PENDING_SELF_BUFF_STATUSES.clear()"));
        assertTrue(service.contains("PENDING_SELF_BUFF_DISSIPATES.clear()"));
        assertFalse(service.contains("MAX_SELF_BUFF_DISSIPATES_PER_EVENT"));
        assertFalse(service.contains("evictForPlayerIfNeeded"));

        assertTrue(spell.contains("boolean applied = player.addEffect("));
        assertTrue(spell.contains("statusApplied = StatusRegistry.applyStatus("));
        assertTrue(spell.contains("ActiveTechniqueEffectVfxService.semantic("));
        assertTrue(spell.contains("ActiveTechniqueEffectVfxService.familyForSkill("));
        assertTrue(spell.contains("String authoredSemantic"));
        assertTrue(spell.contains("String authoredEffectType"));
        assertTrue(spell.contains("String authoredElement"));
        assertTrue(spell.contains(
                "player, semantic, resolvedPrimary, primaryVisualDuration, family, motif)"));
        assertTrue(spell.contains(
                "player, semantic, resolvedSecondary, secondaryVisualDuration, family, motif)"));
        assertTrue(spell.contains(
                "player, semantic, resolvedStatus, statusVisualDuration, family, motif)"));
        assertTrue(resolver.contains("createBuff(technique, spec, cost, cooldown)"));
        assertTrue(resolver.contains("technique.id(), spec.type(), spec.element()"));
        assertTrue(resolver.contains(
                "\"message.seeking_immortals.spell.generic_buff.success\", technique.id()"));
        assertTrue(events.contains("TechniqueLifecycleVfxService.serverTick(event.getServer())"));
    }

    @Test
    void projectileTrailsAreClientOwnedAndTerminalEventsAreExplicit() throws Exception {
        String fireball = source("entity", "CultivationFireballEntity.java");
        String sword = source("entity", "SwordProjectileEntity.java");
        assertFalse(fireball.contains("spawnTrailParticles()"));
        assertFalse(sword.contains(".trailAt("));
        assertTrue(fireball.contains("Motif.PROJECTILE"));
        assertTrue(sword.contains("Motif.BLADE"));
        assertTrue(fireball.contains("sendDissipate();"));
        assertTrue(sword.contains("sendDissipate();"));
        assertTrue(fireball.contains("void remove(RemovalReason reason)"));
        assertTrue(sword.contains("void remove(RemovalReason reason)"));
    }

    @Test
    void successfulSpellsCaptureRealGeometryAfterFailureGuards() throws Exception {
        assertSuccessCaptureAfterFailure("skill", "effect", "spell", "ElementalAreaSpell.java");
        assertSuccessCaptureAfterFailure("skill", "effect", "spell", "TargetedDebuffSpell.java");
        assertSuccessCaptureAfterFailure("skill", "effect", "spell", "AreaDebuffSpell.java");

        String formation = source("skill", "effect", "spell", "FormationSpell.java");
        assertTrue(formation.contains("captureFormation(level, player, center, radius);"));
        assertTrue(formation.contains("TechniqueVfxPacket.Kind.FORMATION"));
    }

    @Test
    void servitorLifecycleCoversSpawnStatusImpactAndTerminalRemoval() throws Exception {
        String servitor = source("entity", "SummonedServitorEntity.java");
        assertTrue(servitor.contains("void onAddedToWorld()"));
        assertTrue(servitor.contains("summonVfxArmed && !summonVfxSent"));
        assertTrue(servitor.contains("TechniqueLifecycleVfxService.summon(this)"));
        assertTrue(servitor.contains("TechniqueLifecycleVfxService.servitorStatus(this)"));
        assertTrue(servitor.contains("TechniqueLifecycleVfxService.servitorImpact("));
        assertTrue(servitor.contains("TechniqueLifecycleVfxService.servitorDissipate(this)"));
        assertTrue(servitor.contains("terminalVfxSent"));
    }

    private static void assertSuccessCaptureAfterFailure(String... relative) throws Exception {
        String source = source(relative);
        int failureReturn = source.indexOf("return false;");
        int capture = source.indexOf("TechniqueLifecycleVfxService.captureGeometry(");
        assertTrue(failureReturn >= 0 && capture > failureReturn,
                String.join("/", relative) + " must capture only after its failure guard");
    }

    private static String source(String... relative) throws Exception {
        Path path = JAVA_ROOT;
        for (String part : relative) {
            path = path.resolve(part);
        }
        return Files.readString(path);
    }
}
