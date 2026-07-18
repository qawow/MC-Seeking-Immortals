package com.xunxian.seekingimmortals.skill;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillStatusProductionTest {
    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void authoredSelfBuffSkillsProduceCanonicalStatuses() throws Exception {
        String registry = Files.readString(JAVA_ROOT.resolve(Path.of("skill", "effect", "SkillEffectRegistry.java")));
        assertBinding(registry, "BLOOD_SACRIFICE", "berserk", 160);
        assertBinding(registry, "TIANMO_BERSERK", "berserk", 120);
        assertBinding(registry, "CAST_GHOST_HIDE_TALISMAN", "conceal_qi", 600);

        String sword = Files.readString(JAVA_ROOT.resolve(
                Path.of("skill", "effect", "spell", "SwordTechniqueSpell.java")));
        assertTrue(sword.contains("StatusRegistry.applyStatus(player, \"sword_intent\", 0, duration);"));
    }

    private static void assertBinding(String source, String skillId, String statusId, int duration) {
        Pattern pattern = Pattern.compile(
                "register\\(SkillType\\." + skillId
                        + ",\\s*new\\s+com\\.xunxian\\.seekingimmortals\\.skill\\.effect\\.spell\\.SelfBuffSpell\\("
                        + ".*?\\\"" + statusId + "\\\",\\s*" + duration + ",",
                Pattern.DOTALL);
        assertTrue(pattern.matcher(source).find(), skillId + " must apply " + statusId);
    }
}
