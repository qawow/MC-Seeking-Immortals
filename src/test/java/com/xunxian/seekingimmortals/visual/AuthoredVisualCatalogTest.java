package com.xunxian.seekingimmortals.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.AuthoredSpellEffectCatalog;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoredVisualCatalogTest {
    private static final Path GENERATED = Path.of(
            "src", "main", "resources", "data", "seeking_immortals", "visual",
            "authored_visual_catalog.json");

    @Test
    void unifiedCatalogLoadsEveryDomainWithoutCollapsingSharedIds() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        int techniqueCount = AuthoredSpellEffectCatalog.builtin().counts().total();
        int totalCount = 3779 - 344 + techniqueCount;

        assertEquals(3, catalog.schemaVersion());
        assertEquals(totalCount, catalog.declaredProfileCount());
        assertEquals(totalCount, catalog.count());
        assertEquals(techniqueCount, catalog.count(VisualDomain.TECHNIQUE));
        assertEquals(217, catalog.count(VisualDomain.ARTIFACT));
        assertEquals(114, catalog.count(VisualDomain.PILL));
        assertEquals(57, catalog.count(VisualDomain.CONSUMABLE));
        assertEquals(136, catalog.count(VisualDomain.METHOD));
        assertEquals(79, catalog.count(VisualDomain.HERB));
        assertEquals(457, catalog.count(VisualDomain.MATERIAL));
        assertEquals(1890, catalog.count(VisualDomain.BEAST));
        assertEquals(179, catalog.count(VisualDomain.NPC));
        assertEquals(19, catalog.count(VisualDomain.REALM));
        assertEquals(75, catalog.count(VisualDomain.ZONE));
        assertEquals(27, catalog.count(VisualDomain.BOSS));
        assertEquals(22, catalog.count(VisualDomain.STATUS));
        assertEquals(92, catalog.count(VisualDomain.STRUCTURE));
        assertEquals(8, catalog.count(VisualDomain.VEHICLE));
        assertEquals(56, catalog.count(VisualDomain.FORMATION));
        assertEquals(7, catalog.count(VisualDomain.TRIBULATION));
        assertEquals(Map.ofEntries(
                Map.entry(VisualDomain.TECHNIQUE, techniqueCount),
                Map.entry(VisualDomain.ARTIFACT, 217),
                Map.entry(VisualDomain.PILL, 114),
                Map.entry(VisualDomain.CONSUMABLE, 57),
                Map.entry(VisualDomain.METHOD, 136),
                Map.entry(VisualDomain.HERB, 79),
                Map.entry(VisualDomain.MATERIAL, 457),
                Map.entry(VisualDomain.BEAST, 1890),
                Map.entry(VisualDomain.NPC, 179),
                Map.entry(VisualDomain.REALM, 19),
                Map.entry(VisualDomain.ZONE, 75),
                Map.entry(VisualDomain.BOSS, 27),
                Map.entry(VisualDomain.STATUS, 22),
                Map.entry(VisualDomain.STRUCTURE, 92),
                Map.entry(VisualDomain.VEHICLE, 8),
                Map.entry(VisualDomain.FORMATION, 56),
                Map.entry(VisualDomain.TRIBULATION, 7)), catalog.counts());
        assertEquals(29, catalog.sourceHashes().size());
        assertTrue(catalog.invalidRows().isEmpty(), catalog.invalidRows().toString());

        VisualProfile pill = catalog.find(VisualDomain.PILL, "bigu_pill").orElseThrow();
        VisualProfile consumable = catalog.find(VisualDomain.CONSUMABLE, "bigu_pill").orElseThrow();
        assertEquals("pill:bigu_pill", pill.key());
        assertEquals("consumable:bigu_pill", consumable.key());
        assertNotEquals(pill.domain(), consumable.domain());
        assertTrue(catalog.find("bigu_pill").isEmpty(), "unqualified collisions must fail closed");
        assertTrue(catalog.find(VisualDomain.METHOD, "blood_demon_art").isPresent());
        assertTrue(catalog.find(VisualDomain.BEAST, "abyss_jiao").isPresent());
        assertTrue(catalog.find(VisualDomain.NPC, "npc_dajin_auctioneer").isPresent());
        assertTrue(catalog.find(VisualDomain.FORMATION, "spirit_gather").isPresent());
        VisualProfile realm = catalog.resolve("realm:blood_forbidden").orElseThrow();
        assertEquals("vis_realm_blood_forbidden", realm.sources().get("v118"));
        assertEquals("light_blood_forbidden", realm.sources().get("v120"));
        VisualProfile status = catalog.resolve("status:array_bind").orElseThrow();
        assertEquals("fx_array", status.sources().get("v119"));
        assertEquals("fx_array", status.sources().get("visual_style"));
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.profiles().put("technique:invalid", pill));
    }

    @Test
    void aliasesAndCollisionDecisionsRemainExplicitAndDomainScoped() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();

        assertEquals(80, catalog.aliases().size());
        assertEquals("jiangchen_pill",
                catalog.resolve(VisualDomain.PILL, "jiangying_pill").orElseThrow().id());
        assertEquals("dingyan_pill",
                catalog.resolve("pill:appearance_lock_pill").orElseThrow().id());
        assertTrue(catalog.resolve(VisualDomain.CONSUMABLE, "appearance_lock_pill").isEmpty());
        catalog.aliases().forEach((alias, target) ->
                assertTrue(catalog.find(target).isPresent(), alias + " -> " + target));

        assertTrue(catalog.collisionResolutions().get("TECHNIQUE")
                .containsKey("inverse_star_veil"));
        assertTrue(catalog.collisionResolutions().get("TECHNIQUE")
                .get("golden_armor_talisman_cast").contains("talisman"));
        assertEquals(4, catalog.collisionResolutions().get("MATERIAL").size());
        assertEquals("blood_forbidden",
                catalog.resolve("realm:vis_realm_blood_forbidden").orElseThrow().id());
        assertEquals("bf_inner_core",
                catalog.resolve("zone:zone_vis_blood_forbidden_inner_core").orElseThrow().id());
        assertEquals("void_palace_lord",
                catalog.resolve("boss:boss_void_palace_lord").orElseThrow().id());
        assertEquals("alchemy_furnace_g1",
                catalog.resolve("structure:prop_alchemy_furnace").orElseThrow().id());
    }

    @Test
    void malformedProfileDoesNotHideValidRows() throws Exception {
        JsonObject shipped = JsonParser.parseString(Files.readString(GENERATED)).getAsJsonObject();
        JsonObject valid = shipped.getAsJsonArray("profiles").get(0).getAsJsonObject().deepCopy();
        JsonObject invalid = valid.deepCopy();
        invalid.addProperty("key", "technique:invalid_trigger");
        invalid.addProperty("id", "invalid_trigger");
        invalid.getAsJsonArray("timeline").get(0).getAsJsonObject()
                .addProperty("trigger", "NOT_A_TRIGGER");

        JsonObject fixture = new JsonObject();
        fixture.addProperty("schema_version", 3);
        fixture.addProperty("profile_count", 2);
        JsonArray profiles = new JsonArray();
        profiles.add(valid);
        profiles.add(invalid);
        fixture.add("profiles", profiles);

        AuthoredVisualCatalog.Snapshot parsed = AuthoredVisualCatalog.parseForTest(
                new StringReader(fixture.toString()));

        assertEquals(1, parsed.count());
        assertEquals(valid.get("key").getAsString(),
                parsed.profiles().values().iterator().next().key());
        assertEquals(1, parsed.invalidRows().size());
        assertTrue(parsed.invalidRows().get(0).contains("profiles[1]"));
        assertTrue(parsed.invalidRows().get(0).contains("NOT_A_TRIGGER"));
    }

    @Test
    void everyTimelineIsTypedBoundedAndDeterministicallyOrdered() {
        Comparator<VisualTimelineEvent> order = Comparator
                .comparingInt(VisualTimelineEvent::startTick)
                .thenComparingInt(VisualTimelineEvent::ordinal);

        for (VisualProfile profile : AuthoredVisualCatalog.profiles().values()) {
            assertFalse(profile.timeline().isEmpty(), profile.key());
            assertNotNull(TechniqueVfxPalette.Family.valueOf(profile.family()), profile.key());
            assertNotNull(TechniqueVfxPacket.Motif.valueOf(profile.motif()), profile.key());
            List<VisualTimelineEvent> sorted = profile.timeline().stream().sorted(order).toList();
            assertEquals(sorted, profile.timeline(), profile.key());
            assertEquals(profile.primaryArgb(), AuthoredVisualCatalog.builtin()
                    .palette(profile.paletteKey()).orElseThrow().argb(), profile.key());
            assertTrue(profile.radius() > 0.0D, profile.key());
            assertTrue(profile.intensity() > 0, profile.key());
            for (VisualTimelineEvent event : profile.timeline()) {
                assertNotNull(event.trigger(), profile.key());
                assertNotNull(event.action(), profile.key());
                assertTrue(event.startTick() >= 0, profile.key());
                assertTrue(event.durationTicks() > 0, profile.key());
                assertTrue(event.radius() > 0.0D, profile.key());
                assertTrue(event.intensity() > 0, profile.key());
            }
            profile.states().values().forEach(action -> assertNotNull(action, profile.key()));
            if (profile.domain() == VisualDomain.TECHNIQUE) {
                assertTrue(profile.visualProgram().executable(), profile.key());
                assertEquals(profile.visualProgram().sourceQuoteCount(),
                        profile.visualProgram().coveredQuoteCount(), profile.key());
                assertEquals("semantic_layers_v2", profile.visualProgram().compiler(), profile.key());
                assertTrue(profile.visualProgram().layers().stream()
                        .allMatch(layer -> layer.eventOrdinal() < profile.timeline().size()), profile.key());
            }
        }
    }

    @Test
    void authoredTechniqueQuotesCompileToMultipleTypedLayers() {
        long authored = AuthoredVisualCatalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .filter(profile -> profile.authored() && !profile.visualProgram().inferredFallback())
                .count();
        long directQuotes = AuthoredVisualCatalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .mapToLong(profile -> profile.visualProgram().sourceQuoteCount())
                .sum();
        long covered = AuthoredVisualCatalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .mapToLong(profile -> profile.visualProgram().coveredQuoteCount())
                .sum();
        assertEquals(1767, authored);
        assertEquals(directQuotes, covered);
        assertTrue(directQuotes >= 9000);
        assertTrue(AuthoredVisualCatalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .anyMatch(profile -> profile.visualProgram().layers().size() >= 4));
    }

    @Test
    void paletteArgbValuesExactlyMatchV118() throws Exception {
        JsonObject source = JsonParser.parseString(Files.readString(Path.of(
                "文本材料", "data", "visual_style_v118.json"))).getAsJsonObject()
                .getAsJsonObject("palette");
        assertEquals(source.size(), AuthoredVisualCatalog.builtin().palette().size());
        source.entrySet().forEach(entry -> {
            String rgb = entry.getValue().getAsString();
            long expected = 0xff00_0000L | Long.parseLong(rgb.substring(1), 16);
            AuthoredVisualCatalog.PaletteColor actual = AuthoredVisualCatalog.builtin()
                    .palette(entry.getKey()).orElseThrow();
            assertEquals(rgb, actual.rgb(), entry.getKey());
            assertEquals(expected, actual.argb(), entry.getKey());
        });
    }
}
