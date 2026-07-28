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

        VisualProfile mixedImplements = catalog.find(
                VisualDomain.TECHNIQUE, "technique_308").orElseThrow();
        VisualProgramLayer blackStaff = mixedImplements.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_STAFF)
                .findFirst().orElseThrow();
        assertEquals(1, blackStaff.copies());
        assertEquals(catalog.palette("yin").orElseThrow().argb(), blackStaff.primaryArgb());

        VisualProfile demonStaff = catalog.find(
                VisualDomain.TECHNIQUE, "technique_479").orElseThrow();
        VisualProgramLayer enlargedStaff = demonStaff.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_STAFF)
                .filter(layer -> layer.sourceQuote().contains("十余丈之长"))
                .findFirst().orElseThrow();
        assertEquals(1, enlargedStaff.copies());
        assertTrue(enlargedStaff.lengthScale() > 1.0D);
        assertEquals(catalog.palette("metal").orElseThrow().argb(),
                enlargedStaff.primaryArgb());
        assertEquals(catalog.palette("qi").orElseThrow().argb(),
                enlargedStaff.secondaryArgb());
        assertTrue(demonStaff.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_STAFF
                        && layer.sourceQuote().contains("降魔巨杖")
                        && layer.copies() == 1));

        VisualProfile windEscape = catalog.find(
                VisualDomain.TECHNIQUE, "technique_268").orElseThrow();
        assertTrue(windEscape.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("手捧古灯"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RITUAL_LAMP));
        assertTrue(windEscape.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("脚踩白莲"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.LOTUS_MANDALA));
        assertTrue(windEscape.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("化为一股轻风"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.AFTERIMAGE_PATH));

        VisualProfile soulReturn = catalog.find(
                VisualDomain.TECHNIQUE, "technique_186").orElseThrow();
        VisualProgramLayer jadeCoffin = soulReturn.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.RITUAL_COFFIN)
                .findFirst().orElseThrow();
        assertEquals(1, jadeCoffin.copies());
        assertEquals(catalog.palette("water").orElseThrow().argb(),
                jadeCoffin.primaryArgb());
        VisualProfile corpseArt = catalog.find(
                VisualDomain.TECHNIQUE, "technique_358").orElseThrow();
        assertTrue(corpseArt.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RITUAL_COFFIN));
        assertTrue(corpseArt.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPIRIT_AVATAR));

        VisualProfile talismanCraft = catalog.find(
                VisualDomain.TECHNIQUE, "technique_275").orElseThrow();
        VisualProgramLayer blueBrush = talismanCraft.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.TALISMAN_BRUSH)
                .filter(layer -> layer.sourceQuote().contains("六七寸长"))
                .findFirst().orElseThrow();
        assertEquals(1, blueBrush.copies());
        assertEquals(catalog.palette("water").orElseThrow().argb(), blueBrush.primaryArgb());
        VisualProgramLayer glyphBrush = talismanCraft.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.TALISMAN_BRUSH)
                .filter(layer -> layer.sourceQuote().contains("金色符文"))
                .findFirst().orElseThrow();
        assertEquals(catalog.palette("water").orElseThrow().argb(), glyphBrush.primaryArgb());
        assertEquals(catalog.palette("metal").orElseThrow().argb(), glyphBrush.secondaryArgb());

        VisualProgramLayer whiteQin = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_1487").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SPIRIT_QIN)
                .findFirst().orElseThrow();
        assertEquals(1, whiteQin.copies());
        assertEquals(catalog.palette("qi").orElseThrow().argb(), whiteQin.primaryArgb());
        assertEquals(catalog.palette("qi").orElseThrow().argb(), whiteQin.secondaryArgb());

        VisualProgramLayer threeFlameFan = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_446").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_FAN)
                .findFirst().orElseThrow();
        assertEquals(1, threeFlameFan.copies());
        assertEquals(catalog.palette("fire").orElseThrow().argb(),
                threeFlameFan.primaryArgb());
        assertEquals(catalog.palette("qi").orElseThrow().argb(),
                threeFlameFan.secondaryArgb());
        VisualProfile greenFan = catalog.find(
                VisualDomain.TECHNIQUE, "technique_980").orElseThrow();
        assertTrue(greenFan.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("青色羽扇"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_FAN
                        && layer.primaryArgb() == catalog.palette("wood").orElseThrow().argb()));
        assertFalse(greenFan.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("青色羽扇"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.WING_FAN));
        VisualProfile fanFire = catalog.find(
                VisualDomain.TECHNIQUE, "technique_565").orElseThrow();
        assertTrue(fanFire.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("单手持扇"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_FAN));
        assertTrue(fanFire.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("单手持扇"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FIRE_PLUME));

        VisualProfile alchemy = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1371").orElseThrow();
        VisualProgramLayer silverFlameFurnace = alchemy.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.ALCHEMY_FURNACE)
                .filter(layer -> layer.sourceQuote().contains("银白火焰"))
                .findFirst().orElseThrow();
        assertEquals(1, silverFlameFurnace.copies());
        assertEquals(catalog.palette("metal").orElseThrow().argb(),
                silverFlameFurnace.primaryArgb());
        assertEquals(catalog.palette("fire").orElseThrow().argb(),
                silverFlameFurnace.secondaryArgb());
        assertTrue(alchemy.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("淡紫色雾气"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MIST_VEIL));
        VisualProfile fireFurnace = catalog.find(
                VisualDomain.TECHNIQUE, "technique_649").orElseThrow();
        assertTrue(fireFurnace.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("巨大火炉"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.ALCHEMY_FURNACE
                        && layer.copies() == 1));
        assertTrue(fireFurnace.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("巨大火炉"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FIRE_PLUME));

        VisualProfile ghostScroll = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1301").orElseThrow();
        assertTrue(ghostScroll.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_SCROLL
                        && layer.copies() == 1
                        && layer.primaryArgb() == catalog.palette("yin").orElseThrow().argb()));
        assertTrue(ghostScroll.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.PROJECTILE_SWARM
                        && layer.copies() == 12));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_911").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_SCROLL
                        && layer.primaryArgb() == catalog.palette("metal").orElseThrow().argb()));

        VisualProfile waterFormation = catalog.find(
                VisualDomain.TECHNIQUE, "technique_102").orElseThrow();
        assertTrue(waterFormation.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("阵旗、阵盘"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FORMATION_DISC
                        && layer.primaryArgb() == catalog.palette("water").orElseThrow().argb()));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1382").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("青光阵盘"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FORMATION_DISC
                        && layer.primaryArgb() == catalog.palette("wood").orElseThrow().argb()));
        VisualProfile nodeRelease = catalog.find(
                VisualDomain.TECHNIQUE, "technique_572").orElseThrow();
        assertTrue(nodeRelease.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("阵旗阵盘"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FORMATION_DISC));
        assertTrue(nodeRelease.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("阵旗阵盘"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAM_LANCE));

        VisualProfile giantClub = catalog.find(
                VisualDomain.TECHNIQUE, "technique_703").orElseThrow();
        VisualProgramLayer longClub = giantClub.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SPIKED_CLUB)
                .findFirst().orElseThrow();
        assertEquals(1, longClub.copies());
        assertTrue(longClub.lengthScale() > 1.0D);
        assertEquals(catalog.palette("metal").orElseThrow().argb(), longClub.primaryArgb());
        assertTrue(giantClub.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.GROUND_FIELD));
        VisualProfile windClub = catalog.find(
                VisualDomain.TECHNIQUE, "technique_582").orElseThrow();
        assertTrue(windClub.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPIKED_CLUB));
        assertTrue(windClub.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SOUND_WAVE));

        VisualProfile tokenEscape = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1050").orElseThrow();
        VisualProgramLayer blackToken = tokenEscape.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.COMMAND_TOKEN)
                .findFirst().orElseThrow();
        assertEquals(1, blackToken.copies());
        assertEquals(catalog.palette("yin").orElseThrow().argb(), blackToken.primaryArgb());
        assertTrue(tokenEscape.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.AFTERIMAGE_PATH));
        assertTrue(tokenEscape.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MIST_VEIL));
        VisualProfile wingedJiaoToken = catalog.find(
                VisualDomain.TECHNIQUE, "technique_753").orElseThrow();
        assertTrue(wingedJiaoToken.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.COMMAND_TOKEN));
        assertTrue(wingedJiaoToken.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SERPENT_DRAGON));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_965").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("金银令牌"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAM_LANCE));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_316").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.COMMAND_TOKEN)
                .anyMatch(layer -> layer.primaryArgb()
                        == catalog.palette("fire").orElseThrow().argb()));

        VisualProgramLayer goldScissors = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_1081").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_SCISSORS)
                .findFirst().orElseThrow();
        assertEquals(1, goldScissors.copies());
        assertEquals(catalog.palette("metal").orElseThrow().argb(),
                goldScissors.primaryArgb());
        VisualProfile thunderScissors = catalog.find(
                VisualDomain.TECHNIQUE, "technique_683").orElseThrow();
        assertTrue(thunderScissors.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_SCISSORS)
                .anyMatch(layer -> layer.copies() == 1
                        && layer.primaryArgb() == catalog.palette("thunder").orElseThrow().argb()));
        assertTrue(thunderScissors.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SERPENT_DRAGON)
                .anyMatch(layer -> layer.copies() == 2));
        assertTrue(thunderScissors.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SOUND_WAVE));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1098").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SERPENT_DRAGON)
                .anyMatch(layer -> layer.copies() == 6));

        VisualProfile healingBrick = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1200").orElseThrow();
        assertTrue(healingBrick.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_BRICK)
                .anyMatch(layer -> layer.copies() == 1
                        && layer.primaryArgb() == catalog.palette("water").orElseThrow().argb()));
        assertTrue(healingBrick.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.HALO_RING));
        VisualProfile fireBrick = catalog.find(
                VisualDomain.TECHNIQUE, "technique_750").orElseThrow();
        assertTrue(fireBrick.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_BRICK)
                .anyMatch(layer -> layer.primaryArgb()
                        == catalog.palette("fire").orElseThrow().argb()));
        assertTrue(fireBrick.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.LIGHT_CURTAIN));

        VisualProfile splitUmbrella = catalog.find(
                VisualDomain.TECHNIQUE, "technique_544").orElseThrow();
        VisualProgramLayer jadeUmbrella = splitUmbrella.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_UMBRELLA)
                .findFirst().orElseThrow();
        assertEquals(catalog.palette("qi").orElseThrow().argb(),
                jadeUmbrella.primaryArgb());
        assertEquals(catalog.palette("metal").orElseThrow().argb(),
                jadeUmbrella.secondaryArgb());
        assertTrue(splitUmbrella.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BLADE_ARC));

        VisualProfile puppetWeapons = catalog.find(
                VisualDomain.TECHNIQUE, "technique_049").orElseThrow();
        VisualProgramLayer drawnBow = puppetWeapons.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_BOW)
                .findFirst().orElseThrow();
        assertEquals(catalog.palette("metal").orElseThrow().argb(), drawnBow.primaryArgb());
        assertEquals(catalog.palette("fire").orElseThrow().argb(), drawnBow.secondaryArgb());
        assertTrue(puppetWeapons.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_BLADE));

        VisualProfile silverBell = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1494").orElseThrow();
        assertTrue(silverBell.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("铃铛"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BELL_CHIME
                        && layer.copies() == 1
                        && layer.primaryArgb() == catalog.palette("metal").orElseThrow().argb()));
        assertTrue(silverBell.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("声波法则"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SOUND_WAVE
                        && layer.copies() == 1));
        assertTrue(silverBell.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_SWORD));
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
    void existingGeometryCoversAuthoredNetsWeaponsRingsCagesAndYellowBell() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();

        VisualProgramLayer goldenThunderNet = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_961").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.CHAIN_NET)
                .filter(layer -> layer.sourceQuote().contains("一张金色雷网"))
                .findFirst().orElseThrow();
        assertEquals(1, goldenThunderNet.copies());
        assertEquals(catalog.palette("metal").orElseThrow().argb(), goldenThunderNet.primaryArgb());
        assertEquals(catalog.palette("thunder").orElseThrow().argb(), goldenThunderNet.secondaryArgb());
        VisualProgramLayer silverFireNet = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_1337").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.CHAIN_NET)
                .findFirst().orElseThrow();
        assertEquals(catalog.palette("qi").orElseThrow().argb(), silverFireNet.primaryArgb());
        assertEquals(catalog.palette("fire").orElseThrow().argb(), silverFireNet.secondaryArgb());

        VisualProfile iceGuns = catalog.find(VisualDomain.TECHNIQUE, "technique_278").orElseThrow();
        assertTrue(iceGuns.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SPEAR_SPIKE)
                .anyMatch(layer -> layer.sourceQuote().contains("一根根小树粗细的冰枪")
                        && layer.copies() == 12
                        && layer.primaryArgb() == catalog.palette("water").orElseThrow().argb()));
        assertTrue(iceGuns.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SPEAR_SPIKE)
                .anyMatch(layer -> layer.sourceQuote().contains("千余根巨大冰枪")
                        && layer.copies() == 20));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_728").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPEAR_SPIKE
                        && layer.copies() == 6));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_833").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPEAR_SPIKE
                        && layer.copies() == 12));
        List<VisualProgramLayer> tripleGlaives = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_843").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SPEAR_SPIKE)
                .toList();
        assertEquals(3, tripleGlaives.size());
        assertTrue(tripleGlaives.stream().allMatch(layer -> layer.copies() == 3));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_848").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPEAR_SPIKE
                        && layer.copies() == 9));

        VisualProfile beetleRing = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1039").orElseThrow();
        assertTrue(beetleRing.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.WHEEL_DISC
                        && layer.copies() == 1
                        && layer.primaryArgb() == catalog.palette("yin").orElseThrow().argb()));
        assertTrue(beetleRing.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.INSECT_SWARM
                        && layer.copies() == 20));
        VisualProfile brokenRing = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1320").orElseThrow();
        assertTrue(brokenRing.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.WHEEL_DISC
                        && layer.copies() == 1));
        assertTrue(brokenRing.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.IMPACT_ARCS
                        && layer.copies() == 1));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_578").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.WHEEL_DISC
                        && layer.primaryArgb() == catalog.palette("earth").orElseThrow().argb()));

        VisualProfile blackCage = catalog.find(
                VisualDomain.TECHNIQUE, "technique_777").orElseThrow();
        assertTrue(blackCage.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SEAL_CAGE));
        assertTrue(blackCage.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("蛟影一吸收"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SERPENT_DRAGON));
        VisualProfile goldCage = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1486").orElseThrow();
        assertTrue(goldCage.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SEAL_CAGE));
        assertTrue(goldCage.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FIRE_PLUME));

        VisualProfile yellowBell = catalog.find(
                VisualDomain.TECHNIQUE, "technique_673").orElseThrow();
        assertTrue(yellowBell.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BELL_CHIME
                        && layer.copies() == 1
                        && layer.primaryArgb() == catalog.palette("metal").orElseThrow().argb()));
        assertTrue(yellowBell.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("钟音爆发"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SOUND_WAVE));

        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_853").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.CHAIN_NET));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_182").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.CHAIN_NET));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1003").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPEAR_SPIKE));
    }

    @Test
    void existingObjectGeometryKeepsAuthoredCountsAndCompanions() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long earth = catalog.palette("earth").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long thunder = catalog.palette("thunder").orElseThrow().argb();
        long water = catalog.palette("water").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();

        List.of("technique_764", "technique_765", "technique_768", "technique_873")
                .forEach(id -> assertTrue(catalog.find(VisualDomain.TECHNIQUE, id)
                        .orElseThrow().visualProgram().layers().stream()
                        .filter(layer -> layer.sourceQuote().contains("法盘"))
                        .anyMatch(layer -> layer.primitive() == VisualPrimitive.FORMATION_DISC
                                && layer.copies() == 1 && layer.primaryArgb() == qi), id));
        VisualProfile inspection = catalog.find(
                VisualDomain.TECHNIQUE, "technique_764").orElseThrow();
        assertTrue(inspection.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("数股神念之力"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.CHANNEL_STREAM
                        && layer.path() == VisualProgramLayer.Path.TRACK));
        VisualProfile fiveColorArray = catalog.find(
                VisualDomain.TECHNIQUE, "technique_765").orElseThrow();
        assertTrue(fiveColorArray.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("法盘顿时光芒大放"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_ORBIT
                        && layer.copies() == 1
                        && layer.path() == VisualProgramLayer.Path.ORBIT));
        VisualProfile discVolley = catalog.find(
                VisualDomain.TECHNIQUE, "technique_768").orElseThrow();
        assertTrue(discVolley.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("十几团灵光"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.PROJECTILE_SWARM
                        && layer.copies() == 12));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_873").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("雾海一阵翻滚"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MIST_VEIL
                        && layer.path() == VisualProgramLayer.Path.EXPAND));

        VisualProfile puppetDefense = catalog.find(
                VisualDomain.TECHNIQUE, "technique_073").orElseThrow();
        assertTrue(puppetDefense.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("龟壳法器"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SHIELD_PLATE
                        && layer.copies() == 1 && layer.primaryArgb() == earth));
        assertTrue(puppetDefense.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("四只傀儡兽"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAST_PHANTOM
                        && layer.copies() == 4));
        assertTrue(puppetDefense.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("四道碗口粗的光柱"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAM_LANCE
                        && layer.copies() == 4));
        VisualProgramLayer glowingCopperShield = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_053").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SHIELD_PLATE)
                .filter(layer -> layer.sourceQuote().contains("铜盾"))
                .findFirst().orElseThrow();
        assertEquals(1, glowingCopperShield.copies());
        assertEquals(metal, glowingCopperShield.primaryArgb());
        assertEquals(earth, glowingCopperShield.secondaryArgb());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_189").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("铜盾"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SHIELD_PLATE
                        && layer.copies() == 1 && layer.primaryArgb() == metal));

        VisualProfile controlledThreads = catalog.find(
                VisualDomain.TECHNIQUE, "technique_428").orElseThrow();
        assertTrue(controlledThreads.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("指环一个盘旋"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.WHEEL_DISC
                        && layer.copies() == 1 && layer.primaryArgb() == yin));
        assertTrue(controlledThreads.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("数十丈范围的银色光丝"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.CHAIN_NET
                        && layer.copies() == 1 && layer.primaryArgb() == qi));

        VisualProfile thunderDaggers = catalog.find(
                VisualDomain.TECHNIQUE, "technique_670").orElseThrow();
        assertTrue(thunderDaggers.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("五口黑色匕首"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_BLADE
                        && layer.copies() == 5 && layer.primaryArgb() == yin
                        && layer.secondaryArgb() == thunder
                        && layer.path() == VisualProgramLayer.Path.TRACK));
        assertTrue(thunderDaggers.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("三种不同电弧"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.PROJECTILE_SWARM
                        && layer.copies() == 5));
        VisualProfile sixteenBlades = catalog.find(
                VisualDomain.TECHNIQUE, "technique_475").orElseThrow();
        List<VisualProgramLayer> iceBlades = sixteenBlades.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.FLYING_BLADE)
                .toList();
        assertEquals(3, iceBlades.size());
        assertTrue(iceBlades.stream().allMatch(layer -> layer.copies() == 16));
        assertEquals(2, iceBlades.stream()
                .filter(layer -> layer.secondaryArgb() == water).count());
        VisualProfile bladeBuddha = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1277").orElseThrow();
        assertTrue(bladeBuddha.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("一圈晶莹的短刃"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_BLADE
                        && layer.copies() == 12));
        assertTrue(bladeBuddha.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPIRIT_AVATAR));
        assertTrue(bladeBuddha.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.EYE_GAZE));

        VisualProfile puppetArmy = catalog.find(
                VisualDomain.TECHNIQUE, "technique_066").orElseThrow();
        assertTrue(puppetArmy.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("十余头傀儡兽"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAST_PHANTOM
                        && layer.copies() == 12));
        assertTrue(puppetArmy.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("十余头傀儡兽"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SUMMON_GATE
                        && layer.copies() == 1));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_030").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAST_PHANTOM));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1296").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_BLADE));
    }

    @Test
    void crystalOrbsKeepBodiesContentsAndIndependentCounts() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long fire = catalog.palette("fire").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long water = catalog.palette("water").orElseThrow().argb();
        long wood = catalog.palette("wood").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();

        VisualProgramLayer sealedGreenQi = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_1034").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE)
                .filter(layer -> layer.sourceQuote().contains("黑色晶球"))
                .findFirst().orElseThrow();
        assertEquals(1, sealedGreenQi.copies());
        assertEquals(yin, sealedGreenQi.primaryArgb());
        assertEquals(wood, sealedGreenQi.secondaryArgb());

        VisualProfile whiteOrb = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1048").orElseThrow();
        assertEquals(2, whiteOrb.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE)
                .count());
        assertTrue(whiteOrb.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("白色晶球"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE
                        && layer.copies() == 1 && layer.primaryArgb() == qi));
        assertTrue(whiteOrb.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("一道金色光柱"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAM_LANCE
                        && layer.primaryArgb() == metal));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1205").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE
                        && layer.copies() == 1 && layer.primaryArgb() == wood));

        VisualProfile blueOrbAndMouse = catalog.find(
                VisualDomain.TECHNIQUE, "technique_830").orElseThrow();
        assertTrue(blueOrbAndMouse.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("蓝色晶球"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE
                        && layer.copies() == 1 && layer.primaryArgb() == water));
        assertTrue(blueOrbAndMouse.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("晶莹玉鼠"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAST_PHANTOM
                        && layer.copies() == 1));
        assertTrue(blueOrbAndMouse.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("黑色光柱一击在光团上"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BEAM_LANCE
                        && layer.primaryArgb() == yin));

        VisualProfile brokenOrb = catalog.find(
                VisualDomain.TECHNIQUE, "technique_973").orElseThrow();
        assertEquals(2, brokenOrb.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE)
                .count());
        assertTrue(brokenOrb.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("一团刺目爆裂而开"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.IMPACT_ARCS
                        && layer.copies() == 1));
        assertTrue(brokenOrb.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("血色符文"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE
                        && layer.secondaryArgb() == fire));

        List<VisualProgramLayer> coldOrbs = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_980").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE)
                .toList();
        assertEquals(2, coldOrbs.size());
        assertTrue(coldOrbs.stream().allMatch(layer -> layer.copies() == 1
                && layer.primaryArgb() == water && layer.secondaryArgb() == qi));

        VisualProfile yinYangStone = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1198").orElseThrow();
        assertTrue(yinYangStone.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE
                        && layer.copies() == 1 && layer.primaryArgb() == yin
                        && layer.secondaryArgb() == qi));
        assertTrue(yinYangStone.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_ORBIT
                        && layer.copies() == 20 && layer.primaryArgb() == metal
                        && layer.secondaryArgb() == qi));
        assertFalse(yinYangStone.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.PROJECTILE_SWARM));

        VisualProgramLayer blackCore = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_1333").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.ORB_PROJECTILE)
                .filter(layer -> layer.sourceQuote().contains("黑色晶核"))
                .findFirst().orElseThrow();
        assertEquals(1, blackCore.copies());
        assertEquals(yin, blackCore.primaryArgb());
        assertEquals(VisualProgramLayer.Path.DIRECT, blackCore.path());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1333").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("黑色晶核"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MIST_VEIL));
    }

    @Test
    void namedGongMaskClothPillarsAndArmorUseDedicatedFigures() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long water = catalog.palette("water").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long earth = catalog.palette("earth").orElseThrow().argb();
        long fire = catalog.palette("fire").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();

        VisualProgramLayer gong = catalog.find(VisualDomain.TECHNIQUE, "technique_906")
                .orElseThrow().visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_GONG)
                .findFirst().orElseThrow();
        assertEquals(1, gong.copies());
        assertEquals(yin, gong.primaryArgb());
        VisualProgramLayer mask = catalog.find(VisualDomain.TECHNIQUE, "technique_1382")
                .orElseThrow().visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_MASK)
                .findFirst().orElseThrow();
        assertEquals(water, mask.primaryArgb());

        VisualProfile cloakProfile = catalog.find(
                VisualDomain.TECHNIQUE, "technique_157").orElseThrow();
        List<VisualProgramLayer> cloakLayers = cloakProfile.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_CLOTH)
                .toList();
        assertEquals(2, cloakLayers.size());
        assertTrue(cloakLayers.stream().allMatch(layer -> layer.copies() == 1
                && layer.path() == VisualProgramLayer.Path.DIRECT
                && layer.primaryArgb() == fire));
        assertEquals(2, cloakProfile.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.AFTERIMAGE_PATH)
                .count());

        VisualProfile silkProfile = catalog.find(
                VisualDomain.TECHNIQUE, "technique_626").orElseThrow();
        assertEquals(2, silkProfile.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_CLOTH)
                .count());
        assertTrue(silkProfile.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("五道粗大金弧"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.IMPACT_ARCS
                        && layer.copies() == 5
                        && layer.path() == VisualProgramLayer.Path.FALL));
        assertTrue(silkProfile.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("成千上万根纤细银丝"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.CHAIN_NET
                        && layer.copies() == 20
                        && layer.primaryArgb() == qi));
        assertTrue(silkProfile.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("成千上万根纤细银丝"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.IMPACT_ARCS
                        && layer.copies() == 1));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_827").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_CLOTH
                        && layer.primaryArgb() == yin));

        VisualProfile bronzePillars = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1201").orElseThrow();
        List<VisualProgramLayer> bronzeLayers = bronzePillars.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.RUNE_PILLAR)
                .toList();
        assertEquals(2, bronzeLayers.size());
        assertTrue(bronzeLayers.stream().allMatch(layer -> layer.copies() == 72
                && layer.path() == VisualProgramLayer.Path.ORBIT
                && layer.primaryArgb() == metal));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_071").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_PILLAR
                        && layer.copies() == 20
                        && layer.path() == VisualProgramLayer.Path.EXPAND
                        && layer.primaryArgb() == earth));

        VisualProfile crystalPrison = catalog.find(
                VisualDomain.TECHNIQUE, "technique_122").orElseThrow();
        assertTrue(crystalPrison.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_PILLAR
                        && layer.copies() == 1
                        && layer.primaryArgb() == water
                        && layer.secondaryArgb() == qi));
        assertTrue(crystalPrison.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPIRIT_AVATAR));

        VisualProfile dragonPillars = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1488").orElseThrow();
        assertTrue(dragonPillars.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("七根金色巨柱"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_PILLAR
                        && layer.copies() == 7));
        assertTrue(dragonPillars.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("金龙缠绕"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SERPENT_DRAGON
                        && layer.copies() == 7
                        && layer.primaryArgb() == metal));
        assertTrue(dragonPillars.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("八头金色蟠龙"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SERPENT_DRAGON
                        && layer.copies() == 8
                        && layer.path() == VisualProgramLayer.Path.TRACK
                        && layer.primaryArgb() == metal));

        VisualProfile ringedPillars = catalog.find(
                VisualDomain.TECHNIQUE, "technique_457").orElseThrow();
        assertTrue(ringedPillars.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_PILLAR
                        && layer.copies() == 12));
        assertTrue(ringedPillars.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.HALO_RING
                        && layer.copies() == 12));

        List.of("technique_1016", "technique_1068", "technique_1069",
                        "technique_1259", "technique_237", "technique_683", "technique_820")
                .forEach(id -> assertTrue(catalog.find(VisualDomain.TECHNIQUE, id)
                        .orElseThrow().visualProgram().layers().stream()
                        .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPIRIT_ARMOR
                                && layer.copies() == 1), id));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1068").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MIST_VEIL));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_237").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.INSECT_SWARM
                        && layer.copies() == 20));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_683").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SOUND_WAVE));

        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1239").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_PILLAR));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1349").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_MASK));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_094").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_CLOTH));
    }

    @Test
    void giantSwordsKeepAuthoredBodiesCountsCompanionsAndLocalColors() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long wood = catalog.palette("wood").orElseThrow().argb();
        long earth = catalog.palette("earth").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long fire = catalog.palette("fire").orElseThrow().argb();
        long thunder = catalog.palette("thunder").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();

        List<VisualProgramLayer> triColor = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_053").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.GIANT_SWORD)
                .filter(layer -> layer.sourceQuote().contains("金、黑、红三色交错"))
                .toList();
        assertEquals(3, triColor.size());
        assertEquals(3, triColor.stream().map(VisualProgramLayer::primaryArgb).distinct().count());
        assertTrue(triColor.stream().anyMatch(layer -> layer.primaryArgb() == metal));
        assertTrue(triColor.stream().anyMatch(layer -> layer.primaryArgb() == yin));
        assertTrue(triColor.stream().anyMatch(layer -> layer.primaryArgb() == fire));

        List<VisualProgramLayer> distinctSwords = catalog.find(
                        VisualDomain.TECHNIQUE, "technique_791").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.GIANT_SWORD)
                .filter(layer -> layer.sourceQuote().contains("一口薄如纸片"))
                .toList();
        assertEquals(3, distinctSwords.size());
        assertEquals(3, distinctSwords.stream().map(VisualProgramLayer::primaryArgb).distinct().count());
        assertEquals(3, distinctSwords.stream().map(VisualProgramLayer::verticalOffset).distinct().count());

        VisualProgramLayer sixSwords = giantSwordLayer(catalog, "technique_1112", "六口丈许长金色巨剑");
        assertEquals(6, sixSwords.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, sixSwords.path());
        assertEquals(metal, sixSwords.primaryArgb());
        VisualProgramLayer fourStoneSwords = giantSwordLayer(
                catalog, "technique_1411", "四柄石剑身上爆发");
        assertEquals(4, fourStoneSwords.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, fourStoneSwords.path());
        assertEquals(earth, fourStoneSwords.primaryArgb());
        assertEquals(yin, fourStoneSwords.secondaryArgb());
        assertEquals(2, giantSwordLayer(catalog, "technique_159", "一般无二的巨剑").copies());

        assertEquals(7, figureLayer(catalog, "technique_103", VisualPrimitive.FLYING_SWORD,
                "这些小剑围着他身体").copies());
        assertEquals(12, figureLayer(catalog, "technique_328", VisualPrimitive.FLYING_SWORD,
                "数十口金色飞剑").copies());
        assertEquals(36, figureLayer(catalog, "technique_512", VisualPrimitive.FLYING_SWORD,
                "三十六口金色飞剑").copies());
        assertEquals(72, figureLayer(catalog, "technique_1003", VisualPrimitive.FLYING_SWORD,
                "七十二口青色小剑").copies());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_328").orElseThrow()
                .visualProgram().layers().stream().anyMatch(
                        layer -> layer.sourceQuote().contains("数十口金色飞剑")
                                && layer.primitive() == VisualPrimitive.PROJECTILE_SWARM));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_512").orElseThrow()
                .visualProgram().layers().stream().anyMatch(
                        layer -> layer.sourceQuote().contains("三十六口金色飞剑")
                                && layer.primitive() == VisualPrimitive.PROJECTILE_SWARM));

        VisualProgramLayer lightning = figureLayer(catalog, "technique_1003",
                VisualPrimitive.LIGHTNING_STORM, "无数电弧狂涌而出");
        assertEquals(metal, lightning.primaryArgb());
        assertEquals(thunder, lightning.secondaryArgb());
        VisualProgramLayer pythons = figureLayer(catalog, "technique_1003",
                VisualPrimitive.SERPENT_DRAGON, "两条电蟒");
        assertEquals(2, pythons.copies());
        assertEquals(metal, pythons.primaryArgb());
        assertEquals(thunder, pythons.secondaryArgb());
        for (VisualPrimitive primitive : List.of(
                VisualPrimitive.ORB_PROJECTILE, VisualPrimitive.FLAME_BIRD,
                VisualPrimitive.FIRE_PLUME)) {
            VisualProgramLayer silverFire = figureLayer(
                    catalog, "technique_1003", primitive, "银色火球");
            assertEquals(qi, silverFire.primaryArgb(), primitive.name());
            assertEquals(fire, silverFire.secondaryArgb(), primitive.name());
        }

        assertEquals(wood, figureLayer(catalog, "technique_1411",
                VisualPrimitive.PROJECTILE_SWARM, "迷蒙的青色灵光").primaryArgb());
        assertEquals(wood, figureLayer(catalog, "technique_234",
                VisualPrimitive.PROJECTILE_SWARM, "百余道青光").primaryArgb());
        assertEquals(wood, figureLayer(catalog, "technique_234",
                VisualPrimitive.AFTERIMAGE_PATH, "十丈长的青虹").primaryArgb());
        assertEquals(wood, giantSwordLayer(catalog, "technique_759", "青光濛濛").primaryArgb());
        assertEquals(yin, figureLayer(catalog, "technique_1519",
                VisualPrimitive.BEAM_LANCE, "黑色光柱").primaryArgb());
        assertEquals(yin, figureLayer(catalog, "technique_404",
                VisualPrimitive.CHAIN_NET, "上百条灰丝").primaryArgb());
        assertEquals(metal, figureLayer(catalog, "technique_645",
                VisualPrimitive.CHAIN_NET, "密密麻麻金丝").primaryArgb());
        assertEquals(fire, figureLayer(catalog, "technique_977",
                VisualPrimitive.BLOOD_THREAD, "脖颈处一丝血线").primaryArgb());

        VisualProgramLayer blackRunes = figureLayer(catalog, "technique_1310",
                VisualPrimitive.RUNE_ORBIT, "无数黑色符文");
        assertEquals(20, blackRunes.copies());
        assertEquals(yin, blackRunes.primaryArgb());
        VisualProgramLayer blackRay = figureLayer(catalog, "technique_1310",
                VisualPrimitive.PROJECTILE_SWARM, "一道粗大乌光");
        assertEquals(1, blackRay.copies());
        assertEquals(yin, blackRay.primaryArgb());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_076").orElseThrow()
                .visualProgram().layers().stream().anyMatch(
                        layer -> layer.primitive() == VisualPrimitive.RUNE_ORBIT
                                && layer.sourceQuote().contains("传送阵的一角")));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_024").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.GIANT_SWORD));
    }

    @Test
    void ruyiHooksAndWhipsKeepBodiesCompanionsPathsAndLocalColors() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long earth = catalog.palette("earth").orElseThrow().argb();
        long fire = catalog.palette("fire").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();

        List<VisualProgramLayer> ruyiLayers = catalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .flatMap(profile -> profile.visualProgram().layers().stream())
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_RUYI)
                .toList();
        assertEquals(6, ruyiLayers.size());
        assertTrue(ruyiLayers.stream().allMatch(layer -> layer.copies() == 1
                && layer.path() == VisualProgramLayer.Path.STATIC));
        VisualProgramLayer redYellowRuyi = figureLayer(
                catalog, "technique_503", VisualPrimitive.MAGIC_RUYI, "红黄两色玉如意");
        assertEquals(fire, redYellowRuyi.primaryArgb());
        assertEquals(earth, redYellowRuyi.secondaryArgb());
        VisualProgramLayer bloodRuyi = figureLayer(
                catalog, "technique_866", VisualPrimitive.MAGIC_RUYI, "白色光霞");
        assertEquals(fire, bloodRuyi.primaryArgb());
        assertEquals(qi, bloodRuyi.secondaryArgb());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_137").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("分别召唤出红黄两只小狼"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SUMMON_GATE));

        List<VisualProgramLayer> hookLayers = catalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .flatMap(profile -> profile.visualProgram().layers().stream())
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_HOOK)
                .toList();
        assertEquals(5, hookLayers.size());
        assertTrue(hookLayers.stream().allMatch(layer -> layer.copies() == 1
                && layer.path() == VisualProgramLayer.Path.DIRECT
                && layer.primaryArgb() == yin && layer.secondaryArgb() == yin));

        List<VisualProgramLayer> whipLayers = catalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .flatMap(profile -> profile.visualProgram().layers().stream())
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_WHIP)
                .toList();
        assertEquals(5, whipLayers.size());
        VisualProgramLayer fireWhip = figureLayer(catalog, "huoyuan_fire_whip",
                VisualPrimitive.MAGIC_WHIP, "火元功火鞭");
        assertEquals(fire, fireWhip.primaryArgb());
        assertEquals(fire, fireWhip.secondaryArgb());
        assertEquals(VisualProgramLayer.Path.DIRECT, fireWhip.path());
        VisualProfile swordTethers = catalog.find(
                VisualDomain.TECHNIQUE, "technique_200").orElseThrow();
        assertEquals(3, swordTethers.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.FLYING_SWORD).count());
        assertEquals(3, swordTethers.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_WHIP).count());
        VisualProfile braceletWhip = catalog.find(
                VisualDomain.TECHNIQUE, "technique_578").orElseThrow();
        assertTrue(braceletWhip.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_WHIP
                        && layer.path() == VisualProgramLayer.Path.DIRECT));
        assertTrue(braceletWhip.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.WHEEL_DISC));

        VisualProfile timeSkull = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1155").orElseThrow();
        assertTrue(timeSkull.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.GHOST_HEAD));
        assertTrue(timeSkull.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.AFTERIMAGE_PATH));

        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1327").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_HOOK));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1389").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_WHIP));
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1483").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_RUYI));
    }

    @Test
    void vajraBoxesFlagsWheelsAndShieldsKeepAuthoredCompanions() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long water = catalog.palette("water").orElseThrow().argb();
        long wood = catalog.palette("wood").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long thunder = catalog.palette("thunder").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();
        long soul = catalog.palette("soul").orElseThrow().argb();
        List<VisualProgramLayer> allLayers = catalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .flatMap(profile -> profile.visualProgram().layers().stream())
                .toList();

        assertEquals(4, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_VAJRA).count());
        assertEquals(19, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_BOX).count());
        assertEquals(1, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.SPIKED_SHIELD).count());
        assertTrue(allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_VAJRA)
                .allMatch(layer -> layer.copies() == 1
                        && layer.path() == VisualProgramLayer.Path.DIRECT
                        && layer.primaryArgb() == qi && layer.secondaryArgb() == qi));
        VisualProfile vajra = catalog.find(
                VisualDomain.TECHNIQUE, "dajin_buddhist_vajra").orElseThrow();
        assertEquals(4, vajra.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.IMPACT_ARCS).count());

        VisualProfile boxedScripture = catalog.find(
                VisualDomain.TECHNIQUE, "technique_031").orElseThrow();
        assertEquals(1, figureLayer(catalog, "technique_031",
                VisualPrimitive.MAGIC_BOX, "一个玉盒内").copies());
        VisualProgramLayer glyphs = figureLayer(catalog, "technique_031",
                VisualPrimitive.SCRIPTURE_GLYPH, "密密麻麻的古文");
        assertEquals(12, glyphs.copies());
        assertEquals(metal, glyphs.primaryArgb());
        assertFalse(boxedScripture.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.PROJECTILE_SWARM));

        VisualProgramLayer boxedSoul = figureLayer(catalog, "technique_960",
                VisualPrimitive.SPIRIT_AVATAR, "金色元婴");
        assertEquals(1, boxedSoul.copies());
        assertEquals(VisualProgramLayer.Path.STATIC, boxedSoul.path());
        assertEquals(metal, boxedSoul.primaryArgb());
        VisualProgramLayer fiveEyes = figureLayer(catalog, "technique_960",
                VisualPrimitive.EYE_GAZE, "五色眼珠");
        assertEquals(5, fiveEyes.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, fiveEyes.path());
        assertEquals(qi, fiveEyes.primaryArgb());

        VisualProgramLayer bristledShield = figureLayer(catalog, "technique_026",
                VisualPrimitive.SPIKED_SHIELD, "刺猬一样的芒刺状");
        assertEquals(1, bristledShield.copies());
        assertEquals(VisualProgramLayer.Path.STATIC, bristledShield.path());
        assertEquals(wood, bristledShield.primaryArgb());
        assertEquals(soul, bristledShield.secondaryArgb());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_026").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("刺猬一样的芒刺状"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SHIELD_PLATE));
        VisualProgramLayer iceShield = figureLayer(catalog, "ice_jade_shield",
                VisualPrimitive.SHIELD_PLATE, "冰玉盾");
        assertEquals(water, iceShield.primaryArgb());
        assertEquals(qi, iceShield.secondaryArgb());
        assertEquals(metal, figureLayer(catalog, "sword_shield",
                VisualPrimitive.SHIELD_PLATE, "剑盾").primaryArgb());
        for (String id : List.of("tianque_shield_bash", "wuzang_organ_shield")) {
            VisualProfile profile = catalog.find(VisualDomain.TECHNIQUE, id).orElseThrow();
            assertTrue(profile.visualProgram().layers().stream()
                    .anyMatch(layer -> layer.primitive() == VisualPrimitive.SHIELD_PLATE));
            assertTrue(profile.visualProgram().layers().stream()
                    .anyMatch(layer -> layer.primitive() == VisualPrimitive.IMPACT_ARCS));
        }

        VisualProgramLayer hallFlags = figureLayer(catalog, "technique_456",
                VisualPrimitive.FORMATION_BANNER, "一叠法旗");
        assertEquals(6, hallFlags.copies());
        assertEquals(wood, hallFlags.primaryArgb());
        VisualProgramLayer hallCurtain = figureLayer(catalog, "technique_456",
                VisualPrimitive.LIGHT_CURTAIN, "一层青色霞光");
        assertEquals(1, hallCurtain.copies());
        assertEquals(wood, hallCurtain.primaryArgb());

        VisualProgramLayer returnedBlackFlag = figureLayer(catalog, "technique_801",
                VisualPrimitive.FORMATION_BANNER, "重新化为一张符箓");
        assertEquals(VisualProgramLayer.Path.FALL, returnedBlackFlag.path());
        assertEquals(yin, returnedBlackFlag.primaryArgb());
        VisualProgramLayer blackOrbs = figureLayer(catalog, "technique_987",
                VisualPrimitive.ORB_PROJECTILE, "六团黑色光球");
        assertEquals(6, blackOrbs.copies());
        assertEquals(yin, blackOrbs.primaryArgb());
        VisualProgramLayer demonCloud = figureLayer(catalog, "technique_987",
                VisualPrimitive.CLOUD_VORTEX, "一团魔云");
        assertEquals(1, demonCloud.copies());
        assertEquals(yin, demonCloud.primaryArgb());

        VisualProgramLayer eightBanners = figureLayer(catalog, "technique_726",
                VisualPrimitive.BANNER_STREAMER, "八只巨幡滴溜溜一转");
        assertEquals(8, eightBanners.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, eightBanners.path());
        VisualProgramLayer eightBeams = figureLayer(catalog, "technique_726",
                VisualPrimitive.BEAM_LANCE, "八道碗口粗光柱");
        assertEquals(8, eightBeams.copies());
        assertEquals(VisualProgramLayer.Path.DIRECT, eightBeams.path());
        VisualProgramLayer raisedSword = figureLayer(catalog, "technique_726",
                VisualPrimitive.GIANT_SWORD, "青色光剑瞬间竖立");
        assertEquals(VisualProgramLayer.Path.RISE, raisedSword.path());
        assertEquals(wood, raisedSword.primaryArgb());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_726").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("青色光剑瞬间竖立"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.PROJECTILE_SWARM));

        VisualProgramLayer bannerArc = figureLayer(catalog, "technique_1097",
                VisualPrimitive.LIGHTNING_STORM, "粗若蛟龙的青色电弧");
        assertEquals(1, bannerArc.copies());
        assertEquals(VisualProgramLayer.Path.DIRECT, bannerArc.path());
        assertEquals(wood, bannerArc.primaryArgb());
        assertEquals(thunder, bannerArc.secondaryArgb());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1097").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("粗若蛟龙"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SERPENT_DRAGON));
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_341").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("幡面上浮现"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.BANNER_STREAMER));

        VisualProgramLayer timeWheel = figureLayer(catalog, "technique_1444",
                VisualPrimitive.WHEEL_DISC, "二十四团半透明符纹");
        assertEquals(1, timeWheel.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, timeWheel.path());
        VisualProgramLayer timeRunes = figureLayer(catalog, "technique_1444",
                VisualPrimitive.RUNE_ORBIT, "二十四团半透明符纹");
        assertEquals(24, timeRunes.copies());
        assertEquals(VisualProgramLayer.Path.WAVE, timeRunes.path());
        VisualProgramLayer giantNails = figureLayer(catalog, "technique_504",
                VisualPrimitive.SPEAR_SPIKE, "金色巨钉");
        assertEquals(5, giantNails.copies());
        assertEquals(VisualProgramLayer.Path.DIRECT, giantNails.path());
        assertEquals(metal, giantNails.primaryArgb());
    }

    @Test
    void ropesAndExistingObjectsKeepExactAuthoredContinuity() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long fire = catalog.palette("fire").orElseThrow().argb();
        long wood = catalog.palette("wood").orElseThrow().argb();
        long water = catalog.palette("water").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();
        long earth = catalog.palette("earth").orElseThrow().argb();

        VisualProfile immortalRope = catalog.find(
                VisualDomain.TECHNIQUE, "immortal_rope").orElseThrow();
        assertEquals(immortalRope.visualProgram().sourceQuoteCount(),
                immortalRope.visualProgram().layers().stream()
                        .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_ROPE)
                        .count());
        assertTrue(immortalRope.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_ROPE)
                .allMatch(layer -> layer.copies() == 1 && layer.primaryArgb() == qi));
        VisualProfile ghostRope = catalog.find(
                VisualDomain.TECHNIQUE, "ghost_bind").orElseThrow();
        assertEquals(ghostRope.visualProgram().sourceQuoteCount(),
                ghostRope.visualProgram().layers().stream()
                        .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_ROPE)
                        .count());
        assertTrue(ghostRope.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_ROPE)
                .allMatch(layer -> layer.primaryArgb() == yin
                        && layer.secondaryArgb() == yin));

        VisualProgramLayer goldRopes = figureLayer(catalog, "technique_478",
                VisualPrimitive.MAGIC_ROPE, "几根金索");
        assertEquals(5, goldRopes.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, goldRopes.path());
        assertEquals(metal, goldRopes.primaryArgb());
        VisualProgramLayer blackGreenRope = figureLayer(catalog, "technique_485",
                VisualPrimitive.MAGIC_ROPE, "黑青色绳索");
        assertEquals(1, blackGreenRope.copies());
        assertEquals(yin, blackGreenRope.primaryArgb());
        assertEquals(wood, blackGreenRope.secondaryArgb());
        VisualProgramLayer fiveFireRopes = figureLayer(catalog, "technique_538",
                VisualPrimitive.MAGIC_ROPE, "五根粗大火索");
        assertEquals(5, fiveFireRopes.copies());
        assertEquals(fire, fiveFireRopes.primaryArgb());
        assertEquals(VisualProgramLayer.Path.ORBIT, fiveFireRopes.path());
        VisualProgramLayer severalFireRopes = figureLayer(catalog, "technique_566",
                VisualPrimitive.MAGIC_ROPE, "数根粗大火索");
        assertEquals(5, severalFireRopes.copies());
        assertEquals(10, figureLayer(catalog, "technique_566",
                VisualPrimitive.BLOOD_THREAD, "十道红丝").copies());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_566").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FIRE_PLUME));
        VisualProgramLayer footRopes = figureLayer(catalog, "technique_612",
                VisualPrimitive.MAGIC_ROPE, "巨人双足");
        assertEquals(2, footRopes.copies());
        assertEquals(10, figureLayer(catalog, "technique_612",
                VisualPrimitive.BLOOD_THREAD, "十根淡淡红丝").copies());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_612").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLAME_BIRD));
        VisualProgramLayer silverFlameRope = figureLayer(catalog, "technique_1329",
                VisualPrimitive.MAGIC_ROPE, "银焰绳索");
        assertEquals(1, silverFlameRope.copies());
        assertEquals(qi, silverFlameRope.primaryArgb());
        assertEquals(fire, silverFlameRope.secondaryArgb());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_1329").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLAME_BIRD));
        assertEquals(fire, figureLayer(catalog, "technique_578",
                VisualPrimitive.MAGIC_ROPE, "红绳捆绑成一团").primaryArgb());
        for (String simile : List.of("technique_1071", "technique_825")) {
            assertFalse(catalog.find(VisualDomain.TECHNIQUE, simile).orElseThrow()
                    .visualProgram().layers().stream()
                    .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_ROPE));
        }

        assertEquals(yin, figureLayer(catalog, "technique_154",
                VisualPrimitive.RITUAL_BOWL, "聚魂钵").primaryArgb());
        VisualProgramLayer childCauldron = figureLayer(catalog, "technique_530",
                VisualPrimitive.CAULDRON_VESSEL, "鼎上则");
        assertEquals(1, childCauldron.copies());
        assertEquals(metal, childCauldron.primaryArgb());
        VisualProgramLayer copperCauldron = figureLayer(catalog, "technique_814",
                VisualPrimitive.CAULDRON_VESSEL, "铜鼎表面");
        assertEquals(metal, copperCauldron.primaryArgb());
        VisualProgramLayer blackGreenFlame = figureLayer(catalog, "technique_814",
                VisualPrimitive.FIRE_PLUME, "黑青色火焰");
        assertEquals(yin, blackGreenFlame.primaryArgb());
        assertEquals(wood, blackGreenFlame.secondaryArgb());
        VisualProgramLayer silverOuterFlame = figureLayer(catalog, "technique_921",
                VisualPrimitive.FIRE_PLUME, "鼎外的银色火焰");
        assertEquals(qi, silverOuterFlame.primaryArgb());
        assertEquals(fire, silverOuterFlame.secondaryArgb());
        assertEquals(metal, figureLayer(catalog, "technique_921",
                VisualPrimitive.CAULDRON_VESSEL, "鼎外").primaryArgb());

        VisualProgramLayer purpleFurnace = figureLayer(catalog, "technique_1429",
                VisualPrimitive.ALCHEMY_FURNACE, "紫色铜炉");
        assertEquals(yin, purpleFurnace.primaryArgb());
        assertEquals(fire, figureLayer(catalog, "technique_1429",
                VisualPrimitive.FIRE_PLUME, "一团婴火").primaryArgb());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1429").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("紫色铜炉"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.CLOUD_VORTEX));
        assertEquals(metal, figureLayer(catalog, "technique_1403",
                VisualPrimitive.MAGIC_BOX, "根本没有打开过").primaryArgb());
        VisualProgramLayer redLightBox = figureLayer(catalog, "technique_371",
                VisualPrimitive.MAGIC_BOX, "盒中爆发");
        assertEquals(qi, redLightBox.primaryArgb());
        assertEquals(water, redLightBox.secondaryArgb());
        assertEquals(fire, figureLayer(catalog, "technique_371",
                VisualPrimitive.IMPACT_ARCS, "盒中爆发").primaryArgb());
        VisualProgramLayer openedJadeBox = figureLayer(catalog, "technique_769",
                VisualPrimitive.MAGIC_BOX, "匣盖");
        assertEquals(qi, openedJadeBox.primaryArgb());
        assertEquals(water, openedJadeBox.secondaryArgb());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "technique_769").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("匣盖"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_ORBIT));

        VisualProfile soulBanner = catalog.find(
                VisualDomain.TECHNIQUE, "soul_banner_wave").orElseThrow();
        assertEquals(soulBanner.visualProgram().sourceQuoteCount(),
                soulBanner.visualProgram().layers().stream()
                        .filter(layer -> layer.primitive() == VisualPrimitive.BANNER_STREAMER)
                        .count());
        assertTrue(soulBanner.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.BANNER_STREAMER)
                .allMatch(layer -> layer.primaryArgb() == yin
                        && layer.secondaryArgb() == yin));
        assertTrue(soulBanner.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MIST_VEIL));
        VisualProgramLayer zither = figureLayer(catalog, "miaoyin_zither_domain",
                VisualPrimitive.SPIRIT_QIN, "琴域压神");
        assertEquals(yin, zither.primaryArgb());
        assertTrue(catalog.find(VisualDomain.TECHNIQUE, "miaoyin_zither_domain").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SOUND_WAVE
                        && layer.primaryArgb() == yin));
        VisualProgramLayer tower = figureLayer(catalog, "kunwu_tower_bind",
                VisualPrimitive.PAGODA_TOWER, "镇魔塔印");
        assertEquals(earth, tower.primaryArgb());
        assertEquals(earth, figureLayer(catalog, "kunwu_tower_bind",
                VisualPrimitive.RUNE_ORBIT, "镇魔塔印").primaryArgb());
    }

    @Test
    void puppetsBoatsAltarsRingsAndStarDiscsKeepAuthoredBodies() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long fire = catalog.palette("fire").orElseThrow().argb();
        long wood = catalog.palette("wood").orElseThrow().argb();
        long water = catalog.palette("water").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();
        long earth = catalog.palette("earth").orElseThrow().argb();
        List<VisualProgramLayer> allLayers = catalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .flatMap(profile -> profile.visualProgram().layers().stream())
                .toList();

        assertEquals(15, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.PUPPET_FIGURE).count());
        assertEquals(5, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.MAGIC_BOAT).count());
        assertEquals(9, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.RITUAL_ALTAR).count());

        VisualProgramLayer ironSoldiers = figureLayer(catalog, "technique_049",
                VisualPrimitive.PUPPET_FIGURE, "两只士兵打扮的玩偶");
        assertEquals(2, ironSoldiers.copies());
        assertEquals(metal, ironSoldiers.primaryArgb());
        VisualProgramLayer summonedSoldiers = figureLayer(catalog, "technique_049",
                VisualPrimitive.PUPPET_FIGURE, "数个真人大小");
        assertEquals(5, summonedSoldiers.copies());
        assertEquals(qi, summonedSoldiers.primaryArgb());
        VisualProfile puppetAmbush = catalog.find(
                VisualDomain.TECHNIQUE, "technique_049").orElseThrow();
        assertTrue(puppetAmbush.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_BOW));
        assertTrue(puppetAmbush.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.FLYING_BLADE));

        VisualProgramLayer woodenFigures = figureLayer(catalog, "technique_1024",
                VisualPrimitive.PUPPET_FIGURE, "八尊通体黑黝黝的木人");
        assertEquals(8, woodenFigures.copies());
        assertEquals(yin, woodenFigures.primaryArgb());
        assertEquals(wood, woodenFigures.secondaryArgb());
        VisualProgramLayer greenAltar = figureLayer(catalog, "technique_1024",
                VisualPrimitive.RITUAL_ALTAR, "碧绿石台");
        assertEquals(1, greenAltar.copies());
        assertEquals(wood, greenAltar.primaryArgb());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1024").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("黑黝黝的木人"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.EYE_GAZE));

        VisualProgramLayer jadePuppets = figureLayer(catalog, "technique_1302",
                VisualPrimitive.PUPPET_FIGURE, "人形白玉傀儡");
        assertEquals(4, jadePuppets.copies());
        assertEquals(qi, jadePuppets.primaryArgb());
        assertEquals(4, figureLayer(catalog, "technique_1302",
                VisualPrimitive.FORMATION_BANNER, "宽大白旗").copies());
        assertEquals(4, figureLayer(catalog, "technique_1302",
                VisualPrimitive.FORMATION_BANNER, "四具傀儡").copies());
        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_1302").orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("人形白玉傀儡"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPIRIT_AVATAR));

        VisualProgramLayer bluePuppets = figureLayer(catalog, "technique_833",
                VisualPrimitive.PUPPET_FIGURE, "青甲傀儡");
        assertEquals(3, bluePuppets.copies());
        assertEquals(water, bluePuppets.primaryArgb());
        assertEquals(metal, bluePuppets.secondaryArgb());
        VisualProgramLayer ironPuppet = figureLayer(catalog, "technique_884",
                VisualPrimitive.PUPPET_FIGURE, "黑乎乎的铁傀儡");
        assertEquals(yin, ironPuppet.primaryArgb());
        assertEquals(water, ironPuppet.secondaryArgb());
        VisualProgramLayer blueSpikes = figureLayer(catalog, "technique_884",
                VisualPrimitive.SPEAR_SPIKE, "蓝色细刺");
        assertEquals(12, blueSpikes.copies());
        assertEquals(water, blueSpikes.primaryArgb());
        VisualProfile nascentSoulPuppet = catalog.find(
                VisualDomain.TECHNIQUE, "technique_528").orElseThrow();
        assertTrue(nascentSoulPuppet.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("没入人形傀儡"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.PUPPET_FIGURE));
        assertTrue(nascentSoulPuppet.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("没入人形傀儡"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPIRIT_AVATAR));

        VisualProgramLayer windBoat = figureLayer(catalog, "technique_073",
                VisualPrimitive.MAGIC_BOAT, "脚下的神风舟");
        assertEquals(VisualProgramLayer.Path.DIRECT, windBoat.path());
        assertEquals(qi, windBoat.primaryArgb());
        assertEquals(VisualProgramLayer.Path.STATIC, figureLayer(catalog, "technique_086",
                VisualPrimitive.MAGIC_BOAT, "歪歪扭扭").path());
        assertEquals(metal, figureLayer(catalog, "technique_603",
                VisualPrimitive.MAGIC_BOAT, "金舟微微一颤").primaryArgb());
        assertEquals(yin, figureLayer(catalog, "technique_1253",
                VisualPrimitive.MAGIC_BOAT, "黑色巨舟顿时破空").primaryArgb());
        VisualProgramLayer bloodBoat = figureLayer(catalog, "technique_971",
                VisualPrimitive.MAGIC_BOAT, "血红小舟");
        assertEquals(VisualProgramLayer.Path.DIRECT, bloodBoat.path());
        assertEquals(fire, bloodBoat.primaryArgb());
        VisualProgramLayer bloodWake = figureLayer(catalog, "technique_971",
                VisualPrimitive.BLOOD_THREAD, "血红小舟");
        assertEquals(VisualProgramLayer.Path.DIRECT, bloodWake.path());
        assertEquals(fire, bloodWake.primaryArgb());
        assertEquals(fire, bloodWake.secondaryArgb());
        assertEquals(4, figureLayer(catalog, "technique_971",
                VisualPrimitive.WING_FAN, "血红晶翅").copies());
        for (String backgroundBoat : List.of("technique_077", "technique_762")) {
            assertFalse(catalog.find(VisualDomain.TECHNIQUE, backgroundBoat).orElseThrow()
                    .visualProgram().layers().stream()
                    .anyMatch(layer -> layer.primitive() == VisualPrimitive.MAGIC_BOAT));
        }

        VisualProgramLayer giantAltar = figureLayer(catalog, "technique_1270",
                VisualPrimitive.RITUAL_ALTAR, "巨大祭坛");
        assertEquals(earth, giantAltar.primaryArgb());
        VisualProgramLayer eightPillars = figureLayer(catalog, "technique_1270",
                VisualPrimitive.RUNE_PILLAR, "八只圆柱");
        assertEquals(8, eightPillars.copies());
        assertEquals(VisualProgramLayer.Path.STATIC, eightPillars.path());
        assertEquals(earth, eightPillars.primaryArgb());
        assertEquals(qi, figureLayer(catalog, "technique_1323",
                VisualPrimitive.RITUAL_ALTAR, "银浆浇筑").primaryArgb());
        assertEquals(metal, figureLayer(catalog, "technique_1474",
                VisualPrimitive.RITUAL_ALTAR, "金色高台").primaryArgb());
        assertEquals(8, figureLayer(catalog, "technique_726",
                VisualPrimitive.RITUAL_ALTAR, "八个高台").copies());

        VisualProgramLayer boneRings = figureLayer(catalog, "technique_626",
                VisualPrimitive.WHEEL_DISC, "五枚颜色各异的骨戒");
        assertEquals(5, boneRings.copies());
        assertEquals(qi, boneRings.primaryArgb());
        assertEquals(5, figureLayer(catalog, "technique_626",
                VisualPrimitive.GHOST_HEAD, "五枚颜色各异的骨戒").copies());
        VisualProgramLayer fiveColdFlames = figureLayer(catalog, "technique_626",
                VisualPrimitive.FIRE_PLUME, "五种颜色各异寒焰");
        assertEquals(5, fiveColdFlames.copies());
        assertEquals(qi, fiveColdFlames.primaryArgb());
        assertEquals(water, fiveColdFlames.secondaryArgb());
        assertEquals(5, figureLayer(catalog, "technique_627",
                VisualPrimitive.WHEEL_DISC, "戒指的变化").copies());

        VisualProfile starDiscs = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1407").orElseThrow();
        assertEquals(3, starDiscs.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.FORMATION_DISC).count());
        assertTrue(starDiscs.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("移星子母盘"))
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.SPATIAL_RIFT));
        VisualProgramLayer shatteredDisc = figureLayer(catalog, "technique_1407",
                VisualPrimitive.IMPACT_ARCS, "这块阵盘终于爆裂");
        assertEquals(1, shatteredDisc.copies());
    }

    @Test
    void sagesFormationRodsStelesAndCommandTokensKeepAuthoredContinuity() {
        AuthoredVisualCatalog.Snapshot catalog = AuthoredVisualCatalog.builtin();
        long qi = catalog.palette("qi").orElseThrow().argb();
        long wood = catalog.palette("wood").orElseThrow().argb();
        long metal = catalog.palette("metal").orElseThrow().argb();
        long yin = catalog.palette("yin").orElseThrow().argb();
        long earth = catalog.palette("earth").orElseThrow().argb();
        long thunder = catalog.palette("thunder").orElseThrow().argb();
        List<VisualProgramLayer> allLayers = catalog.profiles().values().stream()
                .filter(profile -> profile.domain() == VisualDomain.TECHNIQUE)
                .flatMap(profile -> profile.visualProgram().layers().stream())
                .toList();

        assertEquals(4, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.CONFUCIAN_SAGE).count());
        assertEquals(3, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.FORMATION_ROD).count());
        assertEquals(1, allLayers.stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.RUNE_STELE).count());

        VisualProfile sage = catalog.find(
                VisualDomain.TECHNIQUE, "technique_1134").orElseThrow();
        assertEquals(4, sage.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.CONFUCIAN_SAGE).count());
        assertTrue(sage.visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == VisualPrimitive.CONFUCIAN_SAGE)
                .allMatch(layer -> layer.copies() == 1
                        && layer.primaryArgb() == qi
                        && layer.secondaryArgb() == qi));
        VisualProgramLayer silverEyes = figureLayer(catalog, "technique_1134",
                VisualPrimitive.EYE_GAZE, "纯银般瞳孔");
        assertEquals(metal, silverEyes.primaryArgb());
        VisualProgramLayer fiveColorBrush = figureLayer(catalog, "technique_1134",
                VisualPrimitive.TALISMAN_BRUSH, "五色大笔");
        assertEquals(qi, fiveColorBrush.primaryArgb());
        assertEquals(thunder, fiveColorBrush.secondaryArgb());
        assertEquals(metal, figureLayer(catalog, "technique_1134",
                VisualPrimitive.SCRIPTURE_GLYPH, "银灿灿「儒」字").primaryArgb());

        VisualProgramLayer risingRods = figureLayer(catalog, "technique_1493",
                VisualPrimitive.FORMATION_ROD, "缓缓拉长");
        assertEquals(72, risingRods.copies());
        assertEquals(VisualProgramLayer.Path.RISE, risingRods.path());
        assertEquals(earth, risingRods.primaryArgb());
        assertEquals(metal, risingRods.secondaryArgb());
        VisualProgramLayer fixedRods = figureLayer(catalog, "technique_1493",
                VisualPrimitive.FORMATION_ROD, "黄色晶丝");
        assertEquals(72, fixedRods.copies());
        assertEquals(VisualProgramLayer.Path.STATIC, fixedRods.path());
        VisualProgramLayer rodRunes = figureLayer(catalog, "technique_1493",
                VisualPrimitive.RUNE_ORBIT, "黄色晶丝");
        assertEquals(earth, rodRunes.primaryArgb());
        assertEquals(metal, rodRunes.secondaryArgb());
        VisualProgramLayer thunderBallRunes = figureLayer(catalog, "technique_1493",
                VisualPrimitive.RUNE_ORBIT, "雷电法阵缩小");
        assertEquals(thunder, thunderBallRunes.primaryArgb());
        assertEquals(qi, thunderBallRunes.secondaryArgb());

        VisualProgramLayer stele = figureLayer(catalog, "technique_457",
                VisualPrimitive.RUNE_STELE, "石碑微颤");
        assertEquals(1, stele.copies());
        assertEquals(qi, stele.primaryArgb());
        assertEquals(thunder, stele.secondaryArgb());
        VisualProgramLayer twelvePillars = figureLayer(catalog, "technique_457",
                VisualPrimitive.RUNE_PILLAR, "巨大石柱");
        assertEquals(12, twelvePillars.copies());
        assertEquals(VisualProgramLayer.Path.STATIC, twelvePillars.path());
        VisualProgramLayer twelveRings = figureLayer(catalog, "technique_457",
                VisualPrimitive.HALO_RING, "粗大灵环");
        assertEquals(12, twelveRings.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, twelveRings.path());
        VisualProgramLayer twelveBeams = figureLayer(catalog, "technique_457",
                VisualPrimitive.BEAM_LANCE, "拔地而起");
        assertEquals(12, twelveBeams.copies());
        assertEquals(VisualProgramLayer.Path.RISE, twelveBeams.path());
        assertEquals(1, figureLayer(catalog, "technique_457",
                VisualPrimitive.BARRIER_PLANE, "晶莹异常的障壁").copies());

        VisualProgramLayer greenThreads = figureLayer(catalog, "technique_910",
                VisualPrimitive.CHAIN_NET, "五股青光");
        assertEquals(1, greenThreads.copies());
        assertEquals(wood, greenThreads.primaryArgb());
        assertEquals(qi, greenThreads.secondaryArgb());
        VisualProgramLayer fiveGreenBeams = figureLayer(catalog, "technique_910",
                VisualPrimitive.BEAM_LANCE, "五股青光");
        assertEquals(5, fiveGreenBeams.copies());
        assertEquals(wood, fiveGreenBeams.primaryArgb());
        VisualProgramLayer gatheringRods = figureLayer(catalog, "technique_910",
                VisualPrimitive.FORMATION_ROD, "十二根青色木棍");
        assertEquals(12, gatheringRods.copies());
        assertEquals(VisualProgramLayer.Path.ORBIT, gatheringRods.path());
        assertEquals(wood, gatheringRods.primaryArgb());
        assertEquals(qi, gatheringRods.secondaryArgb());

        VisualProgramLayer clawNet = figureLayer(catalog, "technique_1330",
                VisualPrimitive.CHAIN_NET, "青色大网");
        assertEquals(1, clawNet.copies());
        assertEquals(wood, clawNet.primaryArgb());
        assertEquals(wood, clawNet.secondaryArgb());

        VisualProgramLayer blackWoodTokens = figureLayer(catalog, "technique_098",
                VisualPrimitive.COMMAND_TOKEN, "漆黑的木牌");
        assertEquals(5, blackWoodTokens.copies());
        assertEquals(yin, blackWoodTokens.primaryArgb());
        assertEquals(wood, blackWoodTokens.secondaryArgb());
        VisualProfile mapTokens = catalog.find(
                VisualDomain.TECHNIQUE, "technique_605").orElseThrow();
        assertFalse(mapTokens.visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.GROUND_FIELD));
        assertEquals(metal, figureLayer(catalog, "technique_605",
                VisualPrimitive.COMMAND_TOKEN, "铁牌").primaryArgb());

        VisualProfile capturedSoul = catalog.find(
                VisualDomain.TECHNIQUE, "technique_670").orElseThrow();
        List<VisualPrimitive> captureLayers = capturedSoul.visualProgram().layers().stream()
                .filter(layer -> layer.sourceQuote().contains("绿牌翠芒"))
                .map(VisualProgramLayer::primitive)
                .toList();
        assertTrue(captureLayers.contains(VisualPrimitive.COMMAND_TOKEN));
        assertTrue(captureLayers.contains(VisualPrimitive.BEAST_PHANTOM));
        assertTrue(captureLayers.contains(VisualPrimitive.CHAIN_NET));
        assertEquals(wood, figureLayer(catalog, "technique_670",
                VisualPrimitive.CHAIN_NET, "绿牌翠芒").primaryArgb());

        assertFalse(catalog.find(VisualDomain.TECHNIQUE, "technique_340").orElseThrow()
                .visualProgram().layers().stream()
                .anyMatch(layer -> layer.primitive() == VisualPrimitive.RUNE_STELE));
        for (String id : List.of("technique_1344", "technique_965")) {
            assertEquals(VisualProgramLayer.Path.STATIC, figureLayer(
                    catalog, id, VisualPrimitive.COMMAND_TOKEN, "牌").path());
        }
    }

    private static VisualProgramLayer giantSwordLayer(
            AuthoredVisualCatalog.Snapshot catalog, String id, String sourceToken) {
        return figureLayer(catalog, id, VisualPrimitive.GIANT_SWORD, sourceToken);
    }

    private static VisualProgramLayer figureLayer(AuthoredVisualCatalog.Snapshot catalog,
                                                  String id, VisualPrimitive primitive,
                                                  String sourceToken) {
        return catalog.find(VisualDomain.TECHNIQUE, id).orElseThrow()
                .visualProgram().layers().stream()
                .filter(layer -> layer.primitive() == primitive)
                .filter(layer -> layer.sourceQuote().contains(sourceToken))
                .findFirst().orElseThrow();
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
