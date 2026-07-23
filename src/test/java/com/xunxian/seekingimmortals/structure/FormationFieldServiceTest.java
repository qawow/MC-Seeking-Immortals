package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationFieldServiceTest {
    @Test
    void fieldKindsExposeRingConfig() {
        assertEquals(2, FormationFieldService.FieldKind.SPIRIT_GATHER.radius());
        assertTrue(FormationFieldService.FieldKind.SPIRIT_GATHER.usesSpiritGatheringRing());
        assertEquals(2, FormationFieldService.FieldKind.KILL_SWORD.radius());
        assertTrue(!FormationFieldService.FieldKind.KILL_SWORD.usesSpiritGatheringRing());
    }

    @Test
    void activeCountStartsEmptyAfterClear() {
        FormationFieldService.clearAll();
        assertEquals(0, FormationFieldService.activeCount());
    }

    @Test
    void fieldEffectContainsUsesRadius() {
        FormationFieldService.FieldEffect effect = new FormationFieldService.FieldEffect(
                "spirit_gather",
                FormationFieldService.FieldKind.SPIRIT_GATHER,
                new net.minecraft.core.BlockPos(0, 64, 0),
                2,
                100,
                50,
                "cultivation_speed_1.2",
                false);
        assertTrue(effect.contains(new net.minecraft.core.BlockPos(2, 64, 0)));
        assertTrue(!effect.contains(new net.minecraft.core.BlockPos(4, 64, 0)));
    }

    @Test
    void effectVisualRadiusMatchesPulseAabbHorizontalExtent() {
        assertEquals(4.0D, FormationFieldService.effectRadiusFor(2), 0.0001D);
        assertEquals(8.0D, FormationFieldService.effectRadiusFor(6), 0.0001D);
    }

    @Test
    void pulseCursorAdvancesEvenWhenEveryFieldWasServed() {
        assertEquals(1, FormationFieldService.nextPulseCursor(0, 4, 4));
        assertEquals(0, FormationFieldService.nextPulseCursor(3, 4, 4));
        assertEquals(58, FormationFieldService.nextPulseCursor(10, 48, 100));
    }

    @Test
    void runtimeFieldsClearOnLevelUnloadAndBlockEntityCannotResurrectThem() throws Exception {
        String events = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals", "event", "ModEvents.java"));
        String core = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "block", "entity", "FormationCoreBlockEntity.java"));

        assertTrue(events.contains("FormationFieldService.unload(serverLevel)"));
        assertTrue(!core.contains("rehydrateIfMissing"));
    }

    @Test
    void semanticElementOverridesBroadFormationKind() {
        assertEquals(TechniqueVfxPalette.Family.FIRE, FormationFieldService.familyFor(
                "nine_dragon_flame_barrier", "aoe_fire_barrier", FormationFieldService.FieldKind.DEFENSE));
        assertEquals(TechniqueVfxPalette.Family.ILLUSION, FormationFieldService.familyFor(
                "illusion_maze_array", "illusion_trap", FormationFieldService.FieldKind.KILL_SWORD));
        assertEquals(TechniqueVfxPalette.Family.EARTH, FormationFieldService.familyFor(
                "defense_formation", "defense", FormationFieldService.FieldKind.DEFENSE));
    }

    @Test
    void authoredFormationIdsAvoidRomanizationAndSubstringColorCollisions() {
        assertEquals(TechniqueVfxPalette.Family.VOID, FormationFieldService.familyFor(
                "xinggong_teleport", "内星海枢纽传送", FormationFieldService.FieldKind.CATALOG_GENERIC));
        assertEquals(TechniqueVfxPalette.Family.VOID, FormationFieldService.familyFor(
                "miaoyin_teleport", "", FormationFieldService.FieldKind.CATALOG_GENERIC));
        assertEquals(TechniqueVfxPalette.Family.EARTH, FormationFieldService.familyFor(
                "huangsha_zhen", "沙海迷困", FormationFieldService.FieldKind.KILL_SWORD));
        assertEquals(TechniqueVfxPalette.Family.THUNDER, FormationFieldService.familyFor(
                "lei_zhen_double", "", FormationFieldService.FieldKind.CATALOG_GENERIC));
        assertEquals(TechniqueVfxPalette.Family.LIGHT, FormationFieldService.familyFor(
                "jin_guang_fang", "抵挡低阶法术", FormationFieldService.FieldKind.DEFENSE));
        assertEquals(TechniqueVfxPalette.Family.BLOOD, FormationFieldService.familyFor(
                "xue_luo_zhao", "困锁一域", FormationFieldService.FieldKind.DEFENSE));
        assertEquals(TechniqueVfxPalette.Family.WOOD, FormationFieldService.familyFor(
                "chunli_jianzhen", "木系幻剑", FormationFieldService.FieldKind.KILL_SWORD));
        assertEquals(TechniqueVfxPalette.Family.VOID, FormationFieldService.familyFor(
                "jiezi_boundary", "隔绝内外", FormationFieldService.FieldKind.CATALOG_GENERIC));

        assertEquals(TechniqueVfxPacket.Motif.TELEPORT, FormationFieldService.motifFor(
                "miaoyin_teleport", "", FormationFieldService.FieldKind.CATALOG_GENERIC));
        assertEquals(TechniqueVfxPacket.Motif.ILLUSION, FormationFieldService.motifFor(
                "huangsha_zhen", "沙海迷困", FormationFieldService.FieldKind.KILL_SWORD));
        assertEquals(TechniqueVfxPacket.Motif.RAIN, FormationFieldService.motifFor(
                "lei_zhen_double", "", FormationFieldService.FieldKind.CATALOG_GENERIC));
    }

    @Test
    void everyAuthoredFieldResolvesSemanticFamilyAndMotif() {
        FormationFieldCatalog.Snapshot catalog = FormationFieldCatalog.builtin();
        assertTrue(catalog.size() >= 56);
        for (FormationFieldCatalog.FieldParams field : catalog.fields().values()) {
            assertTrue(FormationFieldService.familyFor(field.id(), field.effect(), field.kind())
                            != TechniqueVfxPalette.Family.NEUTRAL,
                    field.id() + " should have a semantic family");
            assertTrue(FormationFieldService.motifFor(field.id(), field.effect(), field.kind())
                            != TechniqueVfxPacket.Motif.GENERIC,
                    field.id() + " should have a semantic motif");
        }
    }

    @Test
    void everyFormationLifecycleSharesOneBoundedPerDimensionPacketPath() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "FormationFieldService.java"));
        assertTrue(source.contains("MAX_PULSE_TARGET_VFX = 8"));
        assertTrue(source.contains("MAX_VFX_PACKETS_PER_DIMENSION_TICK = 48"));
        assertTrue(source.contains("MAX_PENDING_VFX_PER_DIMENSION = MAX_VFX_PACKETS_PER_DIMENSION_TICK * 8"));
        assertTrue(source.contains("Map<String, VfxBudgetState> VFX_BUDGETS"));
        assertTrue(source.contains("ArrayDeque<VfxEmission> lifecyclePending"));
        assertTrue(source.contains("ArrayDeque<VfxEmission> pulsePending"));
        assertTrue(source.contains("pendingCount() >= MAX_PENDING_VFX_PER_DIMENSION"));
        assertTrue(source.contains("budget.sentThisTick < MAX_VFX_PACKETS_PER_DIMENSION_TICK"));
        assertTrue(source.contains("String formationId"));
        assertTrue(source.contains("AuthoredVisualCatalog.resolve(\"formation:\" + formationId)"));
        assertTrue(source.contains("VisualEventDispatcher.event(level, \"formation\", formationId"));
        assertEquals(1, occurrences(source, "TechniqueVfxPacket.send("),
                "unknown formation profiles must retain one bounded legacy fallback sender");

        int flush = source.indexOf("flushPendingVfx(level);");
        int emptyReturn = source.indexOf("if (ACTIVE.isEmpty())", flush);
        assertTrue(flush >= 0 && emptyReturn > flush,
                "pending lifecycle visuals must drain even after the last field expires");
        assertTrue(source.contains("emitDissipateVfx(level, replaced, false)"));
        assertTrue(source.contains("emitFormationVfx(level, field, TechniqueVfxPacket.Kind.STATUS"));
        assertTrue(source.contains("emitFormationVfx(level, field, TechniqueVfxPacket.Kind.DISSIPATE"));
    }

    @Test
    void pulseSchedulingRotatesFieldsAndUsesBoundedFairTargetSampling() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "FormationFieldService.java"));
        assertTrue(source.contains("emitPulseVisuals(level, dim, pulseVisuals)"));
        assertTrue(source.contains("PULSE_VFX_CURSOR"));
        assertTrue(source.contains("remaining / Math.max(1, remainingFields)"));
        assertTrue(source.contains("nextPulseCursor(start, served, size)"));
        assertTrue(source.contains("served >= size ? 1 : Math.max(1, served)"));
        assertTrue(source.contains("BoundedPulseTargetSampler"));
        assertTrue(source.contains("new ArrayList<>(this.capacity)"));
        assertTrue(source.contains("random.nextInt(eligibleCount)"),
                "reservoir sampling must give later targets a fair chance without an unbounded list");
        assertTrue(source.contains("return new PulseVisual(field, targets.snapshot())"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.STATUS"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.IMPACT"));
        assertTrue(source.contains("AABB.ofSize(Vec3.atCenterOf(field.corePos), radius * 2.0D"));
        assertTrue(source.contains("effectRadiusFor(field.radius)"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
