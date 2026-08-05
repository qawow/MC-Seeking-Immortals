package com.xunxian.seekingimmortals.npc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M-C: legacy named-villager migration.
 *
 * <p>Before this batch a vanilla villager was recognised purely by {@code getCustomName()} with no
 * persistent id, no region check and no one-time latch. Anyone could put a name tag reading
 * 「墨老先生」 on any villager and get {@code SevenMysteriesQuest.start} plus a free spiritual-root
 * test, repeatedly, anywhere in the world. Recognition now happens once, inside a narrow window,
 * and afterwards only the persistent id is trusted.</p>
 */
class LegacyNpcMigrationServiceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void migrationWindowRequiresEveryConditionAtOnce() {
        // The authored window: no persistent id + known legacy type + legal name + matching
        // region + not yet migrated. Dropping any single condition must close it.
        assertTrue(LegacyNpcMigrationService.isMigrationCandidate(false, true, true, true, false));

        assertFalse(LegacyNpcMigrationService.isMigrationCandidate(true, true, true, true, false),
                "an entity that already carries a persistent id must never be re-derived from its name");
        assertFalse(LegacyNpcMigrationService.isMigrationCandidate(false, false, true, true, false),
                "an unknown legacy entity type must not be adopted");
        assertFalse(LegacyNpcMigrationService.isMigrationCandidate(false, true, false, true, false),
                "an arbitrary name-tag string must not resolve to an authored npc id");
        assertFalse(LegacyNpcMigrationService.isMigrationCandidate(false, true, true, false, false),
                "a legal name in the wrong region is a forged name tag");
        assertFalse(LegacyNpcMigrationService.isMigrationCandidate(false, true, true, true, true),
                "the window opens once; a rejected/finished entity must stay closed");
    }

    @Test
    void legalNamesResolveOnlyToAuthoredNpcIds() {
        assertEquals("npc_mo_lao", LegacyNpcMigrationService.npcIdForLegacyName("墨老先生"));
        assertEquals("npc_kunwu_steward", LegacyNpcMigrationService.npcIdForLegacyName("昆吾执事"));
        // Case/whitespace tolerance is fine; invented names are not.
        assertEquals("npc_mo_lao", LegacyNpcMigrationService.npcIdForLegacyName("  Mo Lao  "));
        assertTrue(LegacyNpcMigrationService.npcIdForLegacyName("Steve").isBlank());
        assertTrue(LegacyNpcMigrationService.npcIdForLegacyName("").isBlank());
        assertTrue(LegacyNpcMigrationService.npcIdForLegacyName(null).isBlank());
        // A near-miss must not pass: no prefix/substring matching.
        assertTrue(LegacyNpcMigrationService.npcIdForLegacyName("墨老").isBlank());
        assertTrue(LegacyNpcMigrationService.npcIdForLegacyName("墨老先生的徒弟").isBlank());
    }

    @Test
    void legalRegionsComeFromTheQuestChainsBoundToThatNpc() {
        // The six legacy ids exist nowhere in the data tree; their only authority is the
        // chain->npc keyword rule, so the legal regions must be derived from those chains.
        Set<String> moLao = LegacyNpcMigrationService.legalRegionsFor("npc_mo_lao");
        assertTrue(moLao.contains("tiannan"), "墨老先生 belongs to the 天南 chains");
        assertFalse(moLao.contains("mulan"), "a 天南 npc must not be adoptable in 慕兰");

        assertTrue(LegacyNpcMigrationService.legalRegionsFor("npc_mulan_envoy").contains("mulan"));
        assertTrue(LegacyNpcMigrationService.legalRegionsFor("npc_star_palace_broker").contains("chaotic_sea"));
        assertTrue(LegacyNpcMigrationService.legalRegionsFor("npc_kunwu_steward").contains("kunwu")
                        || LegacyNpcMigrationService.legalRegionsFor("npc_kunwu_steward").contains("dajin"),
                "昆吾执事 belongs to the 大晋/昆吾 chains");

        // Fails closed for anything unmapped.
        assertTrue(LegacyNpcMigrationService.legalRegionsFor("npc_not_real").isEmpty());
        assertTrue(LegacyNpcMigrationService.legalRegionsFor("").isEmpty());
        // An empty region set must make the region gate reject, never accept.
        assertFalse(LegacyNpcMigrationService.regionMatches("npc_not_real", "tiannan"));
        assertFalse(LegacyNpcMigrationService.regionMatches("npc_mo_lao", ""));
        assertTrue(LegacyNpcMigrationService.regionMatches("npc_mo_lao", "tiannan"));
    }

    @Test
    void migratedEntitiesRecordIdVersionRegionAndTime() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("npc/LegacyNpcMigrationService.java"));
        assertTrue(service.contains("TAG_NPC_ID") && service.contains("TAG_MIGRATION_VERSION")
                        && service.contains("TAG_SOURCE_REGION") && service.contains("TAG_MIGRATED_AT"),
                "a migrated villager must carry id + version + source region + timestamp");
        assertEquals(1, LegacyNpcMigrationService.MIGRATION_VERSION);
        assertTrue(service.contains("getPersistentData()"),
                "the id must live in entity persistent data so it survives restart and chunk unload");
        // A rejected forgery is latched too, so the same name tag cannot be retried forever.
        assertTrue(service.contains("TAG_REJECTED"),
                "a rejected candidate must be recorded rather than re-evaluated on every interaction");
    }

    @Test
    void interactionPathsReadThePersistentIdInsteadOfTheNameTag() throws Exception {
        String hook = Files.readString(JAVA_ROOT.resolve("quest/TextQuestNpcHookService.java"));
        String quest = Files.readString(JAVA_ROOT.resolve("quest/QuestService.java"));

        String legacyHook = compact(methodSource(hook,
                "public static boolean handleLegacyNamedVillagerInteraction("));
        assertTrue(legacyHook.contains("LegacyNpcMigrationService.resolveNpcId(player,villager)"),
                "the legacy hook must resolve through the migration service, not getCustomName()");
        assertFalse(legacyHook.contains("villager.getCustomName()"),
                "a raw name-tag read is exactly the forgery vector");

        // The seven-mysteries branch was the worst case: a name tag granted a free root test.
        String legacyQuest = compact(methodSource(quest,
                "public static boolean handleLegacyNamedVillagerInteraction("));
        assertTrue(legacyQuest.contains("LegacyNpcMigrationService.resolveNpcId(player,villager)"),
                "SevenMysteriesQuest must not be startable from an unverified name tag");
        assertFalse(legacyQuest.contains("villager.getCustomName()"));

        // Proximity checks must not accept a name-tagged villager either.
        String near = compact(methodSource(hook, "public static boolean isNearBoundNpc("));
        assertFalse(near.contains("villager.getCustomName()"),
                "quest advancement must not be satisfiable by a forged nearby name tag");
        assertTrue(near.contains("LegacyNpcMigrationService.persistentNpcId(villager)"),
                "proximity must trust the migrated persistent id only");
    }

    @Test
    void auditCommandIsOperatorOnlyAndNeverDeletesPlayerEntities() throws Exception {
        String command = Files.readString(JAVA_ROOT.resolve("command/SeekingImmortalsCommand.java"));
        assertTrue(command.contains("npcMigrationAudit("),
                "world upgrades need a report of migrated/ambiguous/rejected/pending");
        assertTrue(compact(command).contains(
                        "Commands.literal(\"audit\").requires(source->source.hasPermission(2))"),
                "the audit must be operator-only");

        String service = Files.readString(JAVA_ROOT.resolve("npc/LegacyNpcMigrationService.java"));
        String audit = compact(methodSource(service, "public static AuditReport audit("));
        assertFalse(audit.contains(".discard()") || audit.contains(".remove(") || audit.contains(".kill("),
                "an audit must report, never destroy a player's entities");
        // All four outcomes must be reported.
        for (String bucket : new String[]{"migrated", "ambiguous", "rejected", "pending"}) {
            assertTrue(service.contains(bucket), "audit must report " + bucket);
        }
    }

    @Test
    void dedicatedSpawnsStampTheirIdSoARenameCannotChangeTheirRole() throws Exception {
        String quest = Files.readString(JAVA_ROOT.resolve("quest/QuestService.java"));

        // An admin spawn used to pass a blank namedNpcId, leaving the entity identified only by
        // its display name — which is why the dedicated path had to dispatch on the name tag.
        String spawn = compact(methodSource(quest,
                "public static boolean spawnQuestNpc(ServerPlayer player, String name, String namedNpcId)"));
        assertTrue(spawn.contains("LegacyNpcMigrationService") && spawn.contains("npcIdForLegacyName(name)"),
                "a dedicated spawn must stamp the authoritative id, not rely on its display name");

        // A stamped NPC dispatches on its id; only a pre-stamping legacy entity may fall back to
        // the display name, so existing saves keep working without reopening the rename vector.
        String named = compact(methodSource(quest, "public static boolean handleNamedNpcInteraction("));
        int idBranch = named.indexOf("handleSevenMysteriesNpcId(player,dedicatedId)");
        int blankGuard = named.indexOf("dedicatedId.isBlank()");
        int nameFallback = named.indexOf("handleSevenMysteriesNpc(player,");
        assertTrue(blankGuard >= 0 && idBranch > blankGuard,
                "the id must be preferred whenever it is present");
        assertTrue(nameFallback > idBranch,
                "the display-name fallback must only be reachable for a blank-id legacy entity");
    }

    @Test
    void dedicatedNpcsKeepTheirOwnAuthorityAndAreUnaffected() throws Exception {
        // CultivatorNpcEntity already carries an authoritative id; migration must not touch it.
        // Doc references to it are fine and useful — only executable code must stay clear.
        String service = stripComments(
                Files.readString(JAVA_ROOT.resolve("npc/LegacyNpcMigrationService.java")));
        assertFalse(service.contains("CultivatorNpcEntity"),
                "the migration path is for vanilla villagers only");

        String hook = Files.readString(JAVA_ROOT.resolve("quest/TextQuestNpcHookService.java"));
        String named = compact(methodSource(hook, "public static boolean handleNamedNpcInteraction("));
        assertTrue(named.contains("npc.getNamedNpcId()"),
                "dedicated NPCs stay on their own id authority");
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

    /** Strips block and line comments so a doc reference cannot satisfy a code assertion. */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }
}
