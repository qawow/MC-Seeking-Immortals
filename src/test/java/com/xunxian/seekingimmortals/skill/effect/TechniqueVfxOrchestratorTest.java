package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket.Kind;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket.Motif;
import com.xunxian.seekingimmortals.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechniqueVfxOrchestratorTest {
    private static final Set<Motif> BESPOKE_MOTIFS = Set.of(
            Motif.BLADE, Motif.SHIELD, Motif.DOMAIN, Motif.TELEPORT, Motif.SUMMON,
            Motif.WALL, Motif.CHAIN, Motif.CHANNEL, Motif.RAIN, Motif.HEAL,
            Motif.CLEANSE, Motif.SEAL, Motif.FORMATION, Motif.BUDDHIST,
            Motif.CONFUCIAN, Motif.DAO, Motif.GHOST, Motif.ILLUSION, Motif.MARTIAL);

    @Test
    void genericEffectTypesChooseDistinctShapes() {
        assertShape("projectile", "single", Motif.PROJECTILE, Kind.PATH);
        assertShape("beam", "single", Motif.PROJECTILE, Kind.BEAM);
        assertShape("cone", "area", Motif.PROJECTILE, Kind.CONE);
        assertShape("shield", "self", Motif.SHIELD, Kind.AURA);
        assertShape("domain", "area", Motif.DOMAIN, Kind.FORMATION);
        assertShape("teleport_short", "self", Motif.TELEPORT, Kind.PATH);
        assertShape("summon", "self", Motif.SUMMON, Kind.BURST);
        assertShape("wall", "area", Motif.WALL, Kind.FORMATION);
        assertShape("chain", "single", Motif.CHAIN, Kind.PATH);
        assertShape("scan", "self", Motif.CHANNEL, Kind.SCAN);
        assertShape("heal", "self", Motif.HEAL, Kind.AURA);
        assertShape("cleanse", "self", Motif.CLEANSE, Kind.AURA);
        assertShape("trap", "area", Motif.FORMATION, Kind.FORMATION);
    }

    @Test
    void explicitElementWinsOverNameBasedFamilyInference() {
        TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                technique("fire_sword", "melee", "fire", "", Set.of("blade"), "single", "short", "metal", ""),
                SkillType.PURE_YANG_SWORD,
                false);

        assertEquals(TechniqueVfxPalette.Family.FIRE, plan.family());
        assertEquals(Motif.BLADE, plan.motif());
        assertEquals(Kind.PATH, plan.kind());
        assertEquals(10.0D, plan.range());
    }

    @Test
    void authoredParticleAndTrailReferencesOverrideFallbackMotifs() {
        TechniqueVfxOrchestrator.VisualPlan sword = TechniqueVfxOrchestrator.plan(
                technique("qingyuan_sword_ray", "beam", "metal", "", Set.of(), "single", "long", "", ""),
                null, false);
        assertEquals(TechniqueVfxPacket.ParticleStyle.METAL_SPARK, sword.particleStyle());
        assertEquals(TechniqueVfxPacket.TrailStyle.SWORD_THIN, sword.trailStyle());
        assertEquals(Motif.BLADE, sword.motif());
        assertEquals(Kind.BEAM, sword.kind());

        TechniqueVfxOrchestrator.VisualPlan body = TechniqueVfxOrchestrator.plan(
                technique("palm_wind", "melee", "wind", "", Set.of(), "single", "short", "", ""),
                null, false);
        assertEquals(TechniqueVfxPacket.ParticleStyle.QI_SOFT, body.particleStyle());
        assertEquals(TechniqueVfxPacket.TrailStyle.HEAVY_WEAPON, body.trailStyle());
        assertEquals(Motif.MARTIAL, body.motif());
    }

    @Test
    void movementGeometryRemainsAPathWhenShapeUsesAfterimageLanguage() {
        assertMovementPath("earth_escape", "movement", Motif.ILLUSION);
        assertMovementPath("void_step", "movement", Motif.ILLUSION);
        assertMovementPath("blood_shadow_escape", "escape", Motif.GHOST);
        assertMovementPath("body_flash", "movement", Motif.MARTIAL);
    }

    @Test
    void authoredUltimateFrameExtendsTheClientTelegraph() {
        TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                technique("sword_formation_secret", "secret_art", "metal", "", Set.of(),
                        "area", "long", "", ""), null, false);

        assertTrue(plan.telegraphed());
    }

    @Test
    void secondaryCastKeepsGeometryButReducesDensity() {
        TechniqueDataManager.TechniqueEntry technique = technique(
                "thunder_domain", "ultimate", "thunder", "", Set.of("storm"), "battlefield", "long", "", "");
        TechniqueVfxOrchestrator.VisualPlan primary = TechniqueVfxOrchestrator.plan(technique, null, false);
        TechniqueVfxOrchestrator.VisualPlan secondary = TechniqueVfxOrchestrator.plan(technique, null, true);

        assertEquals(primary.family(), secondary.family());
        assertEquals(primary.motif(), secondary.motif());
        assertEquals(primary.kind(), secondary.kind());
        assertEquals(primary.range(), secondary.range());
        assertEquals(primary.radius(), secondary.radius());
        assertTrue(secondary.intensity() < primary.intensity());
        assertTrue(primary.intensity() <= 48);
        assertTrue(secondary.intensity() >= 8);
    }

    @Test
    void representativeBespokeNamesChooseSchoolsAndGeometry() {
        assertSkillMotif("BUDDHA_LIGHT", Motif.BUDDHIST);
        assertSkillMotif("RIGHTEOUS_QI", Motif.CONFUCIAN);
        assertSkillMotif("FIVE_THUNDER", Motif.DAO);
        assertSkillMotif("MYSTIC_SOUL_GHOST_FIRE", Motif.GHOST);
        assertSkillMotif("MIRROR_PHANTOM", Motif.ILLUSION);
        assertSkillMotif("SMALL_SWORD_ARRAY", Motif.FORMATION);
        assertSkillMotif("SWORD_SHIELD", Motif.SHIELD);
        assertSkillMotif("SWORD_ESCAPE", Motif.TELEPORT);
        assertSkillMotif("THOUSAND_SWORD_ARRAY", Motif.RAIN);
        assertSkillMotif("YIN_SOUL_CHAIN", Motif.CHAIN);
        assertSkillMotif("DAO_NATURE_BREATH", Motif.HEAL);
        assertSkillMotif("EARTH_MOUNTAIN_PRESS", Motif.WALL);
    }

    @Test
    void allBespokeMultiFormSkillNamesHaveNonGenericMotifs() {
        List<String> names = List.of(
                "BUDDHA_LIGHT", "SARIRA_SHIELD", "DEMON_SUBDUE_PALM", "ZEN_PULSE", "VAJRA_PALM",
                "DAJIN_BUDDHIST_VAJRA",
                "RIGHTEOUS_QI", "WORD_SUPPRESS", "SCROLL_STRIKE", "INK_SEA", "CONFUCIAN_RIGHTEOUS_QI",
                "FIVE_THUNDER", "PURE_YANG_SWORD", "TAOIST_SEAL", "CLOUD_WALK", "IMMORTAL_ROPE",
                "BAGUA_SEAL", "DAO_NATURE_BREATH",
                "BLOOD_SHADOW_ESCAPE", "SKY_SUPPORTING_DEMONIC_SKILL", "MYSTIC_SOUL_GHOST_FIRE",
                "MYSTIC_SOUL_BONE_CONDENSING_ART", "BLOOD_LUO_BARRIER", "YIN_DEMON_SLASH",
                "SENSE_SCAN", "SENSE_PRESSURE", "SENSE_NEEDLE", "SENSE_DOMAIN", "MIND_READ", "SENSE_LOCK",
                "DIVINE_SENSE_SCAN", "DIVINE_SENSE_LOCK", "SOUL_ATTACK_WAVE", "SOUL_CRY_SHOCK",
                "MIRROR_PHANTOM", "HUNDRED_ILLUSION", "MIND_CONFUSION", "VOID_STEP", "DREAM_SNARE",
                "CLONE_IMAGE", "YANYUE_PHANTOM_ARRAY", "VEIL_OF_MOON", "INVISIBILITY_BASIC", "ILLUSION_MIST",
                "INVERSE_STAR_VEIL", "YANYUE_MOON_ILLUSION", "WANHU_NINE_ILLUSION",
                "SOUL_DEVOURING_CLOUD", "YIN_SOUL_CHAIN", "UNDERWORLD_FLAME", "CORPSE_ARMOR",
                "SMALL_SWORD_ARRAY", "ILLUSION_FORMATION", "SPIRIT_GATHER_ARRAY", "THUNDER_TRAP_ARRAY",
                "SEAL_ARRAY", "KILL_SWORD_FORMATION", "DEFENSE_FORMATION", "SEA_LOCK_ARRAY",
                "STAR_PALACE_PATROL_BEACON", "FORMATION_TRAP_BASIC", "STAR_PALACE_SEAL",
                "KUNWU_SEAL_STRIKE", "STAR_PALACE_TIDAL_LOCK",
                "QINGYUAN_SWORD_RAY", "FLYING_SWORD_STRIKE", "GREEN_BAMBOO_SWORD_QI", "SWORD_SHIELD",
                "SWORD_ESCAPE", "THOUSAND_SWORD_ARRAY", "BLOOD_SWORD_SLASH", "SWORD_MERGE",
                "INVISIBLE_SWORD", "SWORD_DOMAIN", "DUAL_SWORD_DANCE",
                "PRIMORDIAL_MAGNET_SPHERE", "FLAME_SERPENT_STORM", "EARTH_MOUNTAIN_PRESS", "XUANTIAN_ICE_PRISON",
                "LIFE_FIRE", "LIEYAN_TRUE_FIRE_SECRET", "FIVE_ELEMENT_FUSION_BURST");

        assertTrue(names.size() >= 81);
        for (String name : names) {
            SkillType skillType = SkillType.valueOf(name);
            TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                    technique(name.toLowerCase(), "", "", "", Set.of(), "", "", "", ""),
                    skillType,
                    false);
            assertTrue(BESPOKE_MOTIFS.contains(plan.motif()), name + " -> " + plan.motif());
            assertNotEquals(Kind.CAST, plan.kind(), name);
        }
    }

    @Test
    void successfulCastEntryPointOwnsExactlyTwoPacketEmissions() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "effect", "TechniqueVfxOrchestrator.java"));

        assertEquals(2, occurrences(source, "TechniqueVfxPacket.send("));
        assertTrue(source.contains("Kind.CAST"));
        assertTrue(source.contains("beforeCast.add"));
        assertTrue(source.contains("player.position()"));
        assertTrue(source.contains("capturedIntents"));
        assertTrue(source.contains("selectSemantic"));
    }

    @Test
    void capturedServerGeometryWinsOverPlannedRange() {
        TechniqueVfxPacket cast = new TechniqueVfxPacket(
                Kind.CAST, TechniqueVfxPalette.Family.FIRE, Motif.GENERIC,
                0.0D, 65.0D, 0.0D, 1.0D, 65.0D, 0.0D,
                0.7F, 20, 1L);
        TechniqueVfxPacket blockedBeam = new TechniqueVfxPacket(
                Kind.BEAM, TechniqueVfxPalette.Family.FIRE, Motif.GENERIC,
                0.0D, 65.0D, 0.0D, 3.25D, 65.0D, 0.0D,
                0.2F, 28, 2L);

        TechniqueVfxPacket selected = TechniqueVfxOrchestrator.selectSemantic(
                List.of(cast, blockedBeam), Kind.BEAM);

        assertEquals(blockedBeam, selected);
        assertEquals(3.25D, selected.endX());
    }

    @Test
    void capturedFormationActivationKeepsItsMotif() {
        TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                "buff_zone", "buff_zone", "neutral", "", Set.of(),
                "area", "medium", "", "", "", false);
        TechniqueVfxPacket formation = new TechniqueVfxPacket(
                Kind.FORMATION, TechniqueVfxPalette.Family.FIRE, Motif.FORMATION,
                0.0D, 64.0D, 0.0D, 0.0D, 64.0D, 0.0D,
                4.0F, 48, 3L);

        assertEquals(Motif.FORMATION, TechniqueVfxOrchestrator.motif(plan, formation));
    }

    private static void assertShape(String effectType, String target, Motif motif, Kind kind) {
        TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                "shape_test", effectType, "neutral", "", Set.of(), target, "", "", "", "", false);
        assertEquals(motif, plan.motif(), effectType);
        assertEquals(kind, plan.kind(), effectType);
    }

    private static void assertSkillMotif(String name, Motif motif) {
        TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                technique(name.toLowerCase(), "", "", "", Set.of(), "", "", "", ""),
                SkillType.valueOf(name),
                false);
        assertEquals(motif, plan.motif(), name);
    }

    private static void assertMovementPath(String id, String type, Motif motif) {
        TechniqueVfxOrchestrator.VisualPlan plan = TechniqueVfxOrchestrator.plan(
                technique(id, type, "neutral", "", Set.of(), "self", "dash", "", ""),
                null, false);
        assertEquals(Kind.PATH, plan.kind(), id);
        assertEquals(motif, plan.motif(), id);
    }

    private static TechniqueDataManager.TechniqueEntry technique(String id,
                                                                  String effectType,
                                                                  String element,
                                                                  String effectKey,
                                                                  Set<String> tags,
                                                                  String target,
                                                                  String range,
                                                                  String attribute,
                                                                  String source) {
        return new TechniqueDataManager.TechniqueEntry(
                id, id, source, attribute, 1, 10, Realm.QI_REFINING, "", "",
                effectType, element, 100, 10.0D, effectKey, tags, target, range);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int cursor = 0;
        while ((cursor = source.indexOf(needle, cursor)) >= 0) {
            count++;
            cursor += needle.length();
        }
        return count;
    }
}
