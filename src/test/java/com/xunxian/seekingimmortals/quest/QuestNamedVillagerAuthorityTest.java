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
    void dedicatedQuestNpcsCannotEnterSectMenus() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("quest", "QuestService.java")));
        String interaction = compact(methodSource(source,
                "public static boolean handleNamedNpcInteraction("));

        assertFalse(interaction.contains("SectContributionService"),
                "quest NPCs must not reach sect services through a custom name");
        assertFalse(interaction.contains("handleStewardInteraction("),
                "quest NPCs must not use an entity-free steward entry");
        assertTrue(interaction.contains("CultivatorNpcEntitynpc"),
                "quest authority must accept the dedicated NPC hierarchy");
    }

    @Test
    void legacyNamedQuestVillagersRemainAvailableForExistingWorlds() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("quest", "QuestService.java")));
        String interaction = compact(methodSource(source,
                "public static boolean handleLegacyNamedVillagerInteraction("));

        assertTrue(interaction.contains("handleSevenMysteriesNpc(player,name)"),
                "legacy Mo Lao/steward names must remain usable after upgrading a world");
        assertTrue(interaction.contains("handleLegacyNamedVillagerInteraction(player,villager)"),
                "legacy text-quest villagers must reach only the explicit compatibility hook");
    }

    @Test
    void dedicatedNpcTypesRunBeforeTheLegacyVillagerBranch() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("event", "ModEvents.java")));
        String interaction = compact(methodSource(source,
                "public static void onNpcInteract("));

        int traderGate = interaction.indexOf("targetinstanceofMarketTraderEntitytrader");
        int stewardGate = interaction.indexOf("targetinstanceofSectStewardEntitysteward");
        int questGate = interaction.indexOf("targetinstanceofQuestNpcEntityquestNpc");
        int legacyGate = interaction.indexOf("targetinstanceofVillagervillager");

        assertTrue(traderGate >= 0 && stewardGate > traderGate && questGate > stewardGate,
                "dedicated NPC roles must be dispatched by entity type");
        assertTrue(legacyGate > questGate,
                "the vanilla villager compatibility path must run after dedicated NPCs");
    }

    @Test
    void newQuestNpcSpawnsNeverCreateVanillaVillagers() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("quest", "QuestService.java")));
        String spawn = compact(methodSource(source,
                "public static boolean spawnQuestNpc(ServerPlayer player, String name, String namedNpcId)"));
        assertTrue(spawn.contains("NpcSpawnService.spawnQuestNpc("),
                "quest spawns must use the shared collision-checked NPC path");
        assertFalse(spawn.contains("ModEntities.QUEST_NPC"));
        assertFalse(spawn.contains("addFreshEntity("));
        assertFalse(source.contains("EntityType.VILLAGER.create"));
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
