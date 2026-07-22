package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSettlementServiceContractTest {
    private static final Path JAVA = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void starterBankerUsesThePersistentWorldAnchorAndNearbyDeduplication() throws Exception {
        String source = Files.readString(JAVA.resolve("npc/NpcSettlementService.java"));

        assertTrue(source.contains("savedData.ensureStarterAnchor(overworld, snapshot)"));
        assertTrue(source.contains("WorldpackGameplayService.DEFAULT_REGION_ID"));
        assertTrue(source.contains("getEntitiesOfClass(SpiritStoneBankerEntity.class"));
        assertTrue(source.contains("ModEntities.SPIRIT_STONE_BANKER.get().create(overworld)"));
        assertFalse(source.contains("NamedNpcRegistry.all()"));
    }

    @Test
    void loginCreatesTheBankerAndVanillaVillagersNoLongerProvideBanking() throws Exception {
        String events = Files.readString(JAVA.resolve("event/ModEvents.java"));
        String login = methodSource(events, "public static void onPlayerLogin(");
        String interaction = methodSource(events, "public static void onNpcInteract(");
        String villagerBranch = interaction.substring(interaction.indexOf("target instanceof Villager"));

        assertTrue(login.contains("NpcSettlementService.ensureStarterHub(serverPlayer)"));
        assertTrue(villagerBranch.contains("handleLegacyNamedVillagerInteraction"));
        assertFalse(villagerBranch.contains("handleSpiritStoneBankerExchange"));
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
}
