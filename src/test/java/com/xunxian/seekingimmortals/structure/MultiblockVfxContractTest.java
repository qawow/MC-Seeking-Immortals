package com.xunxian.seekingimmortals.structure;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockVfxContractTest {
    @Test
    void stationSemanticsChooseUsefulFamiliesAndMotifs() {
        assertEquals(TechniqueVfxPacket.Motif.TELEPORT,
                MultiblockOperationalService.stationMotif("ancient_rift_gate"));
        assertEquals(TechniqueVfxPacket.Motif.FORMATION,
                MultiblockOperationalService.stationMotif("kill_sword_formation"));
        assertEquals(TechniqueVfxPacket.Motif.CHANNEL,
                MultiblockOperationalService.stationMotif("alchemy_furnace"));
        assertEquals(TechniqueVfxPacket.Motif.HEAL,
                MultiblockOperationalService.stationMotif("spirit_herb_planter"));

        assertEquals(TechniqueVfxPalette.Family.FIRE,
                MultiblockOperationalService.stationFamily("alchemy_furnace"));
        assertEquals(TechniqueVfxPalette.Family.WOOD,
                MultiblockOperationalService.stationFamily("spirit_herb_planter"));
        assertEquals(TechniqueVfxPalette.Family.VOID,
                MultiblockOperationalService.stationFamily("ancient_rift_gate"));
    }

    @Test
    void operationalTransitionsEmitLifecycleIntents() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "structure", "MultiblockOperationalService.java"));
        for (String kind : new String[]{"CAST", "FORMATION", "STATUS", "IMPACT", "DISSIPATE"}) {
            assertTrue(source.contains("TechniqueVfxPacket.Kind." + kind), kind);
        }
        assertTrue(source.contains("emitStationVfx(level, stationId, origin"));
        assertTrue(source.contains("disabledNow"));
        assertTrue(source.contains("transitioned"));
    }
}
