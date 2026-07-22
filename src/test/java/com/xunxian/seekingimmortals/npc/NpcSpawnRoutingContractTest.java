package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcSpawnRoutingContractTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void namedNpcRoutingUsesExplicitKindSwitch() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("npc", "NpcSpawnService.java")));
        String spawnNamed = compact(methodSource(source,
                "public static Optional<CultivatorNpcEntity> spawnNamed("));

        assertTrue(spawnNamed.contains("switch(kindFor(npc))"));
        assertTrue(spawnNamed.contains("caseSTEWARD->spawnSteward(level,pos,namedNpcId)"));
        assertTrue(spawnNamed.contains("caseMERCHANT->spawnTrader(level,pos,namedNpcId)"));
        assertTrue(spawnNamed.contains("caseQUEST->spawnQuestNpc(level,pos,namedNpcId)"));
    }

    @Test
    void all179NamedNpcsHaveDeterministicEntityKinds() {
        assertEquals(179, NamedNpcRegistry.count());
        EnumMap<NpcSpawnService.NpcKind, Set<String>> matrix =
                new EnumMap<>(NpcSpawnService.NpcKind.class);
        for (NpcSpawnService.NpcKind kind : NpcSpawnService.NpcKind.values()) {
            matrix.put(kind, new LinkedHashSet<>());
        }
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
            assertTrue(matrix.get(NpcSpawnService.kindFor(npc)).add(npc.id()),
                    "duplicate or unclassified npc: " + npc.id());
        }

        assertEquals(179, matrix.values().stream().mapToInt(Set::size).sum());
        assertEquals(NpcSpawnService.NpcKind.STEWARD, kind("npc_star_palace_sect_master"));
        assertEquals(NpcSpawnService.NpcKind.STEWARD, kind("npc_star_palace_great_elder"));
        assertEquals(NpcSpawnService.NpcKind.STEWARD, kind("npc_star_palace_outer_deacon"));
        assertEquals(NpcSpawnService.NpcKind.MERCHANT, kind("npc_wanbao_auctioneer"));
        assertEquals(NpcSpawnService.NpcKind.QUEST, kind("npc_heavenly_court_inspector"));
    }

    @Test
    void regionAndCommandSpawnsShareNamedNpcRouter() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve(Path.of("npc", "NpcSpawnService.java")));
        String ensure = compact(methodSource(service,
                "public static List<String> ensureRegionNpcs("));
        assertTrue(ensure.contains("spawnNamed(level,at,npc.id())"));
        assertFalse(ensure.contains("spawnTrader("));
        assertFalse(ensure.contains("spawnSteward("));
        assertFalse(ensure.contains("spawnQuestNpc("));

        String command = Files.readString(JAVA_ROOT.resolve(Path.of("command", "SeekingImmortalsCommand.java")));
        String npcSpawn = compact(methodSource(command, "private static int npcSpawn("));
        assertTrue(npcSpawn.contains("NpcSpawnService.spawnNamed("));
        assertFalse(npcSpawn.contains("NpcSpawnService.spawnTrader("));
        assertFalse(npcSpawn.contains("NpcSpawnService.spawnSteward("));
        assertFalse(npcSpawn.contains("NpcSpawnService.spawnQuestNpc("));
    }

    @Test
    void allDedicatedNpcTypesRequireAValidatedGroundPosition() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve(Path.of("npc", "NpcSpawnService.java")));
        assertEquals(3, occurrences(service, "if (!positionForSpawn(level,"));

        String safeSpawn = compact(methodSource(service,
                "private static boolean positionForSpawn("));
        assertTrue(safeSpawn.contains("radius<=SAFE_SPAWN_RADIUS"));
        assertTrue(safeSpawn.contains("getBlockState(feet).getCollisionShape(level,feet).isEmpty()"));
        assertTrue(safeSpawn.contains("getBlockState(feet.above()).getCollisionShape(level,feet.above()).isEmpty()"));
        assertTrue(safeSpawn.contains("isFaceSturdy(level,feet.below(),Direction.UP)"));
        assertTrue(safeSpawn.contains("level.noCollision(npc)"));
        assertTrue(safeSpawn.endsWith("returnfalse;}"),
                "no safe position must prevent addFreshEntity");
    }

    @Test
    void questServiceDelegatesToSharedSafeSpawner() throws Exception {
        String source = Files.readString(JAVA_ROOT.resolve(Path.of("quest", "QuestService.java")));
        String spawn = compact(methodSource(source,
                "public static boolean spawnQuestNpc(ServerPlayer player, String name, String namedNpcId)"));
        assertTrue(spawn.contains("NpcSpawnService.spawnQuestNpc("));
        assertFalse(spawn.contains("ModEntities.QUEST_NPC"));
        assertFalse(spawn.contains("addFreshEntity("));
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

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static NpcSpawnService.NpcKind kind(String npcId) {
        return NpcSpawnService.kindFor(NamedNpcRegistry.find(npcId).orElseThrow());
    }
}
