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
        assertTrue(source.contains("NamedNpcPlacementSavedData.get(player.getServer())"));
        assertTrue(source.contains("REGIONAL_BATCH_SIZE = 3"));
        assertTrue(source.contains("placements.find(npc.id())"));
        assertTrue(source.contains("level.hasChunkAt(placement.pos())"));
        assertTrue(source.contains("level.getEntity(placement.entityId())"));
        assertTrue(source.contains("placements.remove(npc.id())"));
        assertFalse(source.contains("if (placements.contains(npc.id()))"));
    }

    @Test
    void loginCreatesTheBankerAndVanillaVillagersNoLongerProvideBanking() throws Exception {
        String events = Files.readString(JAVA.resolve("event/ModEvents.java"));
        String login = methodSource(events, "public static void onPlayerLogin(");
        String interaction = methodSource(events, "public static void onNpcInteract(");
        String villagerBranch = interaction.substring(interaction.indexOf("target instanceof Villager"));

        assertTrue(login.contains("NpcSettlementService.ensureStarterHub(serverPlayer)"));
        assertTrue(login.contains("NpcSettlementService.ensureRegionalRoster(serverPlayer)"));
        assertTrue(villagerBranch.contains("handleLegacyNamedVillagerInteraction"));
        assertFalse(villagerBranch.contains("handleSpiritStoneBankerExchange"));
    }

    @Test
    void playerTickOnlyBackfillsRegionalRosterAtLowFrequency() throws Exception {
        String events = Files.readString(JAVA.resolve("event/ModEvents.java"));
        String tick = methodSource(events, "public static void onPlayerTick(");

        assertTrue(tick.contains("serverPlayer.tickCount % 600 == 0"));
        assertTrue(tick.contains("NpcSettlementService.ensureRegionalRoster(serverPlayer)"));
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
