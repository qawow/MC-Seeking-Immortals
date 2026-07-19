package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestNamedVillagerAuthorityTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void namedVanillaVillagersCannotEnterSectMenus() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("quest", "QuestService.java")));
        String interaction = compact(methodSource(source,
                "public static boolean handleNamedVillagerInteraction("));

        assertFalse(interaction.contains("SectContributionService"),
                "ordinary villagers must not reach sect services through a custom name");
        assertFalse(interaction.contains("handleStewardInteraction("),
                "ordinary villagers must not use an entity-free steward entry");
    }

    @Test
    void legitimateNamedQuestVillagersRemainAvailable() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("quest", "QuestService.java")));
        String interaction = compact(methodSource(source,
                "public static boolean handleNamedVillagerInteraction("));

        assertTrue(interaction.contains("SevenMysteriesQuest.NPC_MO_LAO.equals(name)"),
                "Mo Lao must remain a valid Seven Mysteries quest NPC");
        assertTrue(interaction.contains("SevenMysteriesQuest.NPC_STEWARD.equals(name)"),
                "the Seven Mysteries quest steward must remain available");
        assertTrue(interaction.contains(
                        "returnTextQuestNpcHookService.handleNamedVillagerInteraction(player,villager);"),
                "other named quest villagers must still reach the text-quest hook");
    }

    @Test
    void realSectStewardsUseTheEntityBoundEntryFirst() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("event", "ModEvents.java")));
        String interaction = compact(methodSource(source,
                "public static void onVillagerExchange("));

        int stewardTypeGate = interaction.indexOf("villagerinstanceofSectStewardEntitysteward");
        int entityBoundEntry = interaction.indexOf(
                "SectContributionService.handleStewardInteraction(serverPlayer,steward)");
        int genericVillagerEntry = interaction.indexOf(
                "QuestService.handleNamedVillagerInteraction(serverPlayer,villager)");

        assertTrue(stewardTypeGate >= 0, "real sect stewards must be identified by entity type");
        assertTrue(entityBoundEntry > stewardTypeGate && entityBoundEntry < genericVillagerEntry,
                "the entity-bound sect entry must run before generic named-villager handling");
    }

    private static String methodSource(String source, String declaration) {
        int start = source.indexOf(declaration);
        assertTrue(start >= 0, "missing source method: " + declaration);
        int openingBrace = source.indexOf('{', start);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("unterminated source method: " + declaration);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
