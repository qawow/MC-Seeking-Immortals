package com.xunxian.seekingimmortals.skill;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastTalismanRegistryTest {
    @Test
    void castTalismansRegisterModeAwareConsumeSpells() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "effect", "SkillEffectRegistry.java"));
        String compact = source.replaceAll("\\s+", "");
        assertTrue(compact.contains("castTalisman("));
        assertTrue(compact.contains("TalismanConsumeSpell"));
        // All CAST_* entries should go through the helper, not ElementalProjectileSpell.
        int castBlock = compact.indexOf("CAST_FIRE_BURST_TALISMAN");
        int ghost = compact.indexOf("GHOST_KING_SUMMON", castBlock);
        assertTrue(castBlock >= 0 && ghost > castBlock);
        String block = compact.substring(castBlock, ghost);
        assertFalse(block.contains("ElementalProjectileSpell"),
                "CAST_* entries must not use ElementalProjectileSpell");
        assertTrue(block.contains("SelfBuffSpell") || block.contains("conceal_qi"),
                "ghost-hide CAST must retain conceal_qi self-buff status production");
        assertTrue(block.contains("aoe_burst_fire"));
        assertTrue(block.contains("escape_teleport"));
        assertTrue(block.contains("armor_protect_buff"));
        assertTrue(block.contains("projectile_blade_metal"));
    }
}
