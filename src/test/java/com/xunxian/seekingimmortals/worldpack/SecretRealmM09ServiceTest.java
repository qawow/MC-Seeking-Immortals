package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRealmM09ServiceTest {
    @Test
    void catalogLoadsTwentyAuthorRealms() {
        assertEquals(20, SecretRealmCatalogService.size(),
                "expected 20 author secret realms, got " + SecretRealmCatalogService.size());
        assertTrue(SecretRealmCatalogService.find("blood_forbidden").isPresent());
        assertTrue(SecretRealmCatalogService.find("void_palace").isPresent());
        assertTrue(SecretRealmCatalogService.find("seven_meridian_cave").isPresent());
        assertTrue(SecretRealmCatalogService.find("chaotic_sea_abyss_rift").isPresent());
        // Y-A: yinyang_ku now owns the yy_* layers; the catacomb keeps its own four.
        assertTrue(SecretRealmCatalogService.find("yinyang_ku").isPresent());
        assertTrue(SecretRealmCatalogService.find("yin_mountain_catacomb").isPresent());
        SecretRealmCatalogService.RealmDef yinyang = SecretRealmCatalogService.find("yinyang_ku").orElseThrow();
        assertEquals("dajin", yinyang.regionId());
        assertEquals("yinyang_cave_gate", yinyang.gate());
        assertEquals(List.of("yy_outer", "yy_split", "yy_yezha", "yy_yinzhi", "yy_alchemy_coop"),
                yinyang.layers().stream().map(SecretRealmCatalogService.LayerDef::id).toList());
        SecretRealmCatalogService.RealmDef catacomb = SecretRealmCatalogService.find("yin_mountain_catacomb").orElseThrow();
        assertTrue(catacomb.layers().stream().noneMatch(layer -> layer.id().startsWith("yy_")),
                "yin_mountain_catacomb must not carry yinyang-ku yy_* layers");
        assertEquals(List.of("corpse_gate", "yin_lang", "jiang_guan_shi", "yin_si_stela"),
                catacomb.layers().stream().map(SecretRealmCatalogService.LayerDef::id).toList());
    }

    @Test
    void deepDiveLayersAndTrapsPresent() {
        SecretRealmCatalogService.RealmDef blood = SecretRealmCatalogService.find("blood_forbidden").orElseThrow();
        assertTrue(blood.layersCount() >= 3);
        assertFalse(blood.layers().isEmpty());
        assertFalse(blood.bosses().isEmpty());
        assertEquals("blood_forbidden_gate", blood.gate());
        int traps = blood.layers().stream().mapToInt(layer -> layer.traps().size()).sum();
        assertTrue(traps > 0, "blood_forbidden should expose M07 trap field kinds");
        assertTrue(SecretRealmTrapService.parseKind("ILLUSION_MAZE") != null);
        assertTrue(SecretRealmTrapService.parseKind("SEAL_DEMON") != null);
    }

    @Test
    void sevenGatesBindAtLeastOneRealm() {
        for (String gate : List.of(
                "blood_forbidden_gate",
                "cycle_gate",
                "ancient_rift_gate",
                "nether_ferry_gate",
                "hidden_rift_gate",
                "king_territory_gate",
                "ascension_gate")) {
            assertTrue(SecretRealmCatalogService.primaryRealmForGate(gate).isPresent(),
                    "gate missing binding: " + gate);
        }
    }

    @Test
    void worldpackContainsAuthorNineteenAndRegionRefs() {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        Set<String> ids = new HashSet<>();
        for (WorldpackDataService.SecretRealm realm : snapshot.secretRealms()) {
            ids.add(realm.id());
            assertTrue(snapshot.findRegion(realm.regionId()).isPresent(),
                    "missing region for " + realm.id() + " -> " + realm.regionId());
        }
        for (String required : List.of(
                "blood_forbidden", "void_palace", "fallen_demon_valley", "kunwu_mountain",
                "nether_river_land", "guanghan_realm", "demon_gold_mountain", "minor_asura_realm",
                "diyuan", "jiuxian_seclusion", "ancient_cultivator_ruins", "wild_ancient_tomb",
                "thousand_bamboo_puppet_tower", "wild_ancient_ruins", "tianlan_secret_grotto",
                "chaotic_sea_abyss_rift", "spirit_grass_valley", "yin_mountain_catacomb",
                "seven_meridian_cave")) {
            assertTrue(ids.contains(required), "worldpack missing author realm " + required);
        }
        assertTrue(snapshot.secretRealms().size() >= 19);
    }

    @Test
    void bossLootTablesParseAndUniqueRedline() {
        assertTrue(BossLootService.size() >= 27);
        assertTrue(BossLootService.find("blood_jiao_guardian").isPresent());
        assertTrue(BossLootService.find("void_palace_lord").isPresent());

        List<BossLootService.DropDef> first = BossLootService.dropsFor("void_palace_lord", true);
        List<BossLootService.DropDef> repeat = BossLootService.dropsFor("void_palace_lord", false);
        assertFalse(first.isEmpty());
        // unique/first_clear_only must not leak into repeat table
        assertTrue(repeat.stream().noneMatch(drop -> drop.unique() || drop.firstClearOnly()),
                "repeat drops still contain unique flags");
        assertTrue(repeat.stream().noneMatch(drop -> BossLootService.isForbidden(drop.itemId())));
        // void_key style uniques present on first if catalog marks them
        boolean anyUnique = first.stream().anyMatch(drop -> drop.unique() || drop.firstClearOnly()
                || drop.itemId().toLowerCase().contains("void_key"));
        assertTrue(anyUnique || first.size() >= repeat.size());
    }

    @Test
    void everyAuthorRealmHasRunnableNamedBossAndRewardTable() {
        assertEquals(20, SecretRealmCatalogService.size());
        for (SecretRealmCatalogService.RealmDef realm : SecretRealmCatalogService.snapshot().byId().values()) {
            assertFalse(realm.bosses().isEmpty(), "realm missing named boss: " + realm.id());
            for (String bossId : realm.bosses()) {
                BossLootService.TableDef table = BossLootService.find(bossId)
                        .orElseThrow(() -> new AssertionError(
                                "realm boss missing runtime reward table: " + realm.id() + " -> " + bossId));
                assertEquals(realm.id(), table.secretRealmId(),
                        "boss reward table points at wrong realm: " + bossId);
                assertFalse(table.drops().isEmpty(), "realm boss has empty reward table: " + bossId);
                assertTrue(BossEncounterService.isKnownBossId(bossId),
                        "realm boss is not spawnable by the named encounter path: " + bossId);
            }
        }
    }

    @Test
    void authorSourceAndRuntimeBossCoverageStayInSync() throws Exception {
        JsonObject sourceRealms = JsonParser.parseString(Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "text_material", "secret_realms.json"))).getAsJsonObject();
        JsonObject sourceLoot = JsonParser.parseString(Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "text_material", "boss_loot_tables.json"))).getAsJsonObject();

        Map<String, Set<String>> sourceBosses = new HashMap<>();
        for (JsonElement element : sourceRealms.getAsJsonArray("realms")) {
            JsonObject realm = element.getAsJsonObject();
            Set<String> bosses = new HashSet<>();
            JsonArray array = realm.has("bosses") ? realm.getAsJsonArray("bosses") : new JsonArray();
            array.forEach(value -> bosses.add(value.getAsString()));
            sourceBosses.put(realm.get("id").getAsString(), bosses);
        }
        Set<String> sourceLootBosses = new HashSet<>();
        for (JsonElement element : sourceLoot.getAsJsonArray("tables")) {
            JsonObject table = element.getAsJsonObject();
            JsonElement bossId = table.has("boss_id") ? table.get("boss_id") : table.get("id");
            if (bossId != null && bossId.isJsonPrimitive()) {
                sourceLootBosses.add(bossId.getAsString());
            }
        }

        for (SecretRealmCatalogService.RealmDef realm : SecretRealmCatalogService.snapshot().byId().values()) {
            assertEquals(new HashSet<>(realm.bosses()), sourceBosses.get(realm.id()),
                    "text-material/runtime boss drift for " + realm.id());
            assertTrue(sourceLootBosses.containsAll(realm.bosses()),
                    "text-material loot tables missing a boss for " + realm.id());
        }
    }

    @Test
    void dimensionMapCoversMajorCatalogRealms() {
        assertTrue(SecretRealmDimensionService.dimensionIdFor("blood_forbidden").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("void_palace").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("nether_river_land").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("yinming_pocket").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("diyuan").isPresent());
        assertTrue(SecretRealmDimensionService.dimensionIdFor("minor_asura_realm").isPresent());
        assertTrue(SecretRealmDimensionService.dedicatedDimensionCount() >= 10);
    }

    @Test
    void realmMinNormalizationAcceptsAuthorDesignIds() {
        assertEquals("FOUNDATION", SecretRealmSessionService.normalizeRealmMin("FOUNDATION"));
        assertEquals("QI_REFINING", SecretRealmSessionService.normalizeRealmMin("qi_refining"));
        assertEquals("CORE_FORMATION", SecretRealmSessionService.normalizeRealmMin("CORE_FORMATION"));
    }

    @Test
    void expiredOfflineSessionsDoNotConsumePartyCapacity() {
        CompoundTag root = new CompoundTag();
        ListTag sessions = new ListTag();
        sessions.add(new SecretRealmProgressSavedData.Session(
                "offline-player", "blood_forbidden", 0L, 100L, 4, false).save());
        root.put("Sessions", sessions);

        SecretRealmProgressSavedData data = SecretRealmProgressSavedData.load(root);
        assertEquals(1, data.activeCountForRealm("blood_forbidden", 99L));
        assertEquals(0, data.activeCountForRealm("blood_forbidden", 100L));
        assertTrue(data.canJoin("blood_forbidden", 1, 100L));
    }

    @Test
    void sessionIdsAndEncounterClaimsPersistAndStaySessionScoped() {
        UUID playerId = UUID.randomUUID();
        CompoundTag root = new CompoundTag();
        ListTag sessions = new ListTag();
        sessions.add(new SecretRealmProgressSavedData.Session(
                playerId.toString(), "session-a", "blood_forbidden", 10L, 200L, 4, false).save());
        root.put("Sessions", sessions);

        SecretRealmProgressSavedData data = SecretRealmProgressSavedData.load(root);
        assertEquals("session-a", data.getSession(playerId).orElseThrow().sessionId());
        assertTrue(data.claimEncounter(playerId, "session-a", "trial:core"));
        assertFalse(data.claimEncounter(playerId, "session-a", "trial:core"));
        assertTrue(data.claimEncounter(playerId, "session-b", "trial:core"));

        SecretRealmProgressSavedData restored = SecretRealmProgressSavedData.load(data.save(new CompoundTag()));
        assertTrue(restored.hasClaimedEncounter(playerId, "session-a", "trial:core"));
        assertTrue(restored.hasClaimedEncounter(playerId, "session-b", "trial:core"));
    }

    @Test
    void legacySessionsReceiveStableCompatibilityIds() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("PlayerId", "legacy-player");
        legacy.putString("RealmId", "blood_forbidden");
        legacy.putLong("EnteredAt", 42L);
        legacy.putLong("ExpiresAt", 500L);
        legacy.putInt("PartyLimit", 4);

        String first = SecretRealmProgressSavedData.Session.load(legacy).sessionId();
        String second = SecretRealmProgressSavedData.Session.load(legacy).sessionId();
        assertFalse(first.isBlank());
        assertEquals(first, second);
    }
}
