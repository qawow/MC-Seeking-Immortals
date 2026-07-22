package com.xunxian.seekingimmortals.structure;

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
}
