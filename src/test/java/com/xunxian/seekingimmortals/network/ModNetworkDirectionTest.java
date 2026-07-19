package com.xunxian.seekingimmortals.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModNetworkDirectionTest {
    private static final Path NETWORK_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals", "network");
    private static final Path MOD_NETWORK = NETWORK_ROOT.resolve("ModNetwork.java");
    private static final Pattern ANY_REGISTRATION = Pattern.compile("CHANNEL\\.messageBuilder\\(");
    private static final Pattern EXPLICIT_REGISTRATION = Pattern.compile(
            "CHANNEL\\.messageBuilder\\(\\s*([A-Za-z0-9_]+)\\.class\\s*,\\s*id\\+\\+\\s*,\\s*"
                    + "NetworkDirection\\.(PLAY_TO_SERVER|PLAY_TO_CLIENT)\\s*\\)");

    private static final Set<String> C2S_ACTIONS = Set.of(
            "SetMeditatingPacket",
            "ReleaseTechniquePacket",
            "SetTechniqueSlotPacket",
            "AttemptBreakthroughPacket",
            "SetMovementSpeedScalePacket",
            "SectActionPacket",
            "ShopActionPacket",
            "WorldpackActionPacket",
            "AuctionActionPacket",
            "DialogueActionPacket",
            "QuestTrackerActionPacket",
            "MethodActionPacket",
            "MethodLayoutActionPacket",
            "SkillTreeActionPacket",
            "LoreScreenActionPacket");

    private static final Set<String> S2C_SYNCS = Set.of(
            "SyncLearnedTechniquesPacket",
            "SyncCultivationDataPacket",
            "SyncLearnedMethodsPacket",
            "SyncSectDataPacket",
            "SyncShopDataPacket",
            "SyncWorldpackDataPacket",
            "SyncQuestTrackerPacket",
            "SyncMethodLayoutPacket",
            "SyncAuctionLadderPacket",
            "SyncSkillDataPacket",
            "SyncLoreUnlockPacket");

    private static final Set<String> S2C_SCREEN_OPENS = Set.of(
            "OpenAuctionScreenPacket",
            "OpenDialogueScreenPacket",
            "OpenAlchemyStatusPacket",
            "OpenStoragePreviewPacket",
            "OpenRefinePlanPacket");

    @Test
    void everyRegistrationDeclaresItsExpectedDirection() throws Exception {
        String source = Files.readString(MOD_NETWORK);
        Map<String, Direction> actual = parseExplicitRegistrations(source);
        long registrationCount = ANY_REGISTRATION.matcher(source).results().count();

        assertEquals(registrationCount, actual.size(),
                "every SimpleChannel registration must declare an explicit NetworkDirection");
        assertEquals(expectedDirections(), actual);
    }

    @Test
    void keyPacketCategoriesStayOnTheirAuthoritySide() throws Exception {
        Map<String, Direction> actual = parseExplicitRegistrations(Files.readString(MOD_NETWORK));

        assertDirection(actual, C2S_ACTIONS, Direction.PLAY_TO_SERVER);
        assertDirection(actual, S2C_SYNCS, Direction.PLAY_TO_CLIENT);
        assertDirection(actual, S2C_SCREEN_OPENS, Direction.PLAY_TO_CLIENT);
    }

    @Test
    void handlerAndServerSendContractsAgreeWithDirections() throws Exception {
        for (String packet : C2S_ACTIONS) {
            String source = Files.readString(NETWORK_ROOT.resolve(packet + ".java"));
            assertTrue(source.contains("context.getSender()"),
                    packet + " must be handled as a client intent on the server");
        }

        for (String packet : union(S2C_SYNCS, S2C_SCREEN_OPENS)) {
            String source = Files.readString(NETWORK_ROOT.resolve(packet + ".java"));
            assertTrue(source.contains("PacketDistributor.PLAYER"),
                    packet + " must be sent from the server to a player");
            assertTrue(source.contains("DistExecutor.unsafeRunWhenOn(Dist.CLIENT"),
                    packet + " must be handled on the client");
        }
    }

    private static Map<String, Direction> parseExplicitRegistrations(String source) {
        Map<String, Direction> registrations = new LinkedHashMap<>();
        Matcher matcher = EXPLICIT_REGISTRATION.matcher(source);
        while (matcher.find()) {
            String packet = matcher.group(1);
            Direction previous = registrations.put(packet, Direction.valueOf(matcher.group(2)));
            assertNull(previous, "duplicate packet registration: " + packet);
        }
        return registrations;
    }

    private static Map<String, Direction> expectedDirections() {
        Map<String, Direction> expected = new LinkedHashMap<>();
        C2S_ACTIONS.forEach(packet -> expected.put(packet, Direction.PLAY_TO_SERVER));
        S2C_SYNCS.forEach(packet -> expected.put(packet, Direction.PLAY_TO_CLIENT));
        S2C_SCREEN_OPENS.forEach(packet -> expected.put(packet, Direction.PLAY_TO_CLIENT));
        return expected;
    }

    private static void assertDirection(Map<String, Direction> registrations,
                                        Set<String> packets,
                                        Direction expected) {
        packets.forEach(packet -> assertEquals(expected, registrations.get(packet), packet));
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        java.util.HashSet<String> union = new java.util.HashSet<>(first);
        union.addAll(second);
        return union;
    }

    private enum Direction {
        PLAY_TO_SERVER,
        PLAY_TO_CLIENT
    }
}
