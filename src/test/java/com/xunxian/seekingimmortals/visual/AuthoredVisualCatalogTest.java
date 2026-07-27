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
                assertEquals("semantic_layers_v3", profile.visualProgram().compiler(), profile.key());
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
        assertEquals(1806, authored);
        assertEquals(directQuotes, covered);
        assertTrue(directQuotes >= 9000);
        assertTrue(AuthoredVisualCatalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .anyMatch(profile -> profile.visualProgram().layers().size() >= 4));
    }

    @Test
    void namedFiguresKeepTheirOwnLocalCounts() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        VisualProfile ghost = catalog.find(VisualDomain.TECHNIQUE, "technique_065").orElseThrow();
        VisualProgramLayer loneGhost = ghost.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.GHOST_HEAD)
                .filter(layer -> layer.sourceQuote().contains("上百颗火球"))
                .findFirst().orElseThrow();
        VisualProgramLayer fireballs = ghost.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE)
                .filter(layer -> layer.sourceQuote().contains("上百颗火球"))
                .findFirst().orElseThrow();
        assertEquals(1, loneGhost.copies());
        assertEquals(12, fireballs.copies());

        VisualProfile axes = catalog.find(VisualDomain.TECHNIQUE, "technique_1342").orElseThrow();
        assertEquals(2, axes.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.GIANT_AXE)
                .filter(layer -> layer.sourceQuote().contains("两柄晶莹巨斧"))
                .findFirst().orElseThrow().copies());

        VisualProfile bowls = catalog.find(VisualDomain.TECHNIQUE, "technique_834").orElseThrow();
        VisualProgramLayer blackBowl = bowls.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.RITUAL_BOWL)
                .filter(layer -> layer.sourceQuote().contains("乌黑圆钵"))
                .findFirst().orElseThrow();
        assertEquals(1, blackBowl.copies());
        assertEquals(catalog.palette("yin").orElseThrow().argb(), blackBowl.primaryArgb());
        VisualProgramLayer openedBowl = bowls.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.RITUAL_BOWL)
                .filter(layer -> layer.sourceQuote().contains("从圆钵里面"))
                .findFirst().orElseThrow();
        assertEquals(catalog.palette("yin").orElseThrow().argb(), openedBowl.primaryArgb());
        assertEquals(catalog.palette("yin").orElseThrow().argb(), openedBowl.secondaryArgb());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_343").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RITUAL_BOWL));
        VisualProfile mixedRelics = catalog.find(
                VisualDomain.TECHNIQUE, "technique_308").orElseThrow();
        assertTrue(mixedRelics.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RITUAL_BOWL));
        assertTrue(mixedRelics.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_SWORD));
        assertEquals(catalog.palette("qi").orElseThrow().argb(),
                mixedRelics.visualProgram().layers().stream()
                        .filter(layer -> layer.primitive() == VisualPrimitive.RITUAL_BOWL)
                        .findFirst().orElseThrow().primaryArgb());

        VisualProfile rulers = catalog.find(VisualDomain.TECHNIQUE, "technique_855").orElseThrow();
        assertEquals(2, rulers.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_RULER)
                .filter(layer -> layer.sourceQuote().contains("两道尺影"))
                .findFirst().orElseThrow().copies());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_861").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_RULER
                        && layer.copies() == 1
                        && layer.primaryArgb() == catalog.palette("qi").orElseThrow().argb()
                        && layer.secondaryArgb() == catalog.palette("qi").orElseThrow().argb()));

        VisualProfile hammer = catalog.find(VisualDomain.TECHNIQUE, "technique_715").orElseThrow();
        assertEquals(1, hammer.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.GIANT_HAMMER)
                .filter(layer -> layer.sourceQuote().contains("单手提着一柄大锤"))
                .findFirst().orElseThrow().copies());
        VisualProgramLayer skullHammer = hammer.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.GIANT_HAMMER)
                .filter(layer -> layer.sourceQuote().contains("八个白森森的骷髅头"))
                .findFirst().orElseThrow();
        assertEquals(1, skullHammer.copies());
        assertEquals(catalog.palette("qi").orElseThrow().argb(), skullHammer.primaryArgb());
        assertEquals(catalog.palette("wood").orElseThrow().argb(), skullHammer.secondaryArgb());
        assertFalse(hammer.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("八个白森森的骷髅头"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.GHOST_HEAD
                        || layer.primitive() == VisualPrimitive.SPIRIT_AVATAR));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_056").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SHIELD_PLATE));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1082").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_BLADE));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1186").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_SWORD
                        && layer.copies() == 72));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1432").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_SWORD
                        && layer.copies() == 72));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1388").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAST_PHANTOM
                        && layer.copies() == 1));
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
