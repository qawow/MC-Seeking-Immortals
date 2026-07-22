package com.xunxian.seekingimmortals.cultivation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TribulationVfxContractTest {
    @Test
    void tribulationCoversGatheringStrikeSuccessFailureAndCollapse() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "cultivation", "TribulationService.java"));

        assertTrue(source.contains("emitTribulationStartVfx(player, targetRealm, strikeCount)"));
        assertTrue(source.contains("emitStrikeWarning(player, targetRealm, strikeNumber, totalStrikes)"));
        assertTrue(source.contains("emitStrikeImpact(player, strikeVfx, strikeNumber)"));
        assertTrue(source.contains("emitTribulationEndVfx(player, targetRealm, true)"));
        assertTrue(source.contains("emitTribulationEndVfx(player, targetRealm, false)"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.FORMATION"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.BEAM"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.IMPACT"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.DISSIPATE"));
        assertTrue(source.contains("TechniqueVfxPacket.Motif.RAIN"));
        assertTrue(source.contains("TechniqueVfxPacket.Motif.DOMAIN"));
        assertTrue(source.contains("STRIKE_WARNING_TICKS = 8"));
        assertTrue(source.indexOf("TechniqueVfxPacket.Kind.BEAM")
                < source.indexOf("TechniqueVfxPacket.Kind.IMPACT"));
        assertTrue(source.contains("impact,\n                impact,"));
        assertTrue(source.indexOf("emitStrikeImpact(player, strikeVfx, strikeNumber)")
                > source.indexOf("if (!damageAccepted)"));
        assertTrue(source.indexOf("emitStrikeWarning(player, targetRealm, strikeNumber, totalStrikes)")
                < source.indexOf("if (!cultivation.tickTribulationCountdown())"));
        assertTrue(source.indexOf("if (!cultivation.tickTribulationCountdown())")
                < source.indexOf("damageAccepted = player.hurt"));
    }
}
