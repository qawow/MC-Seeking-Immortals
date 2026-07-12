package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactRefinementServiceTest {
    @Test
    void resolvesExactFlyingSwordMaterialsFromSourceRecipe() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_flying_sword_low")
                .orElseThrow();

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe, Map.of(
                "low_spirit_iron", "seeking_immortals:low_spirit_iron",
                "spirit_stone_shard", "seeking_immortals:spirit_stone_shard"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:flying_sword_low", plan.outputItemId());
        assertTrue(plan.missingMappings().isEmpty());
        assertEquals(2, plan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "low_spirit_iron", "seeking_immortals:low_spirit_iron", 4), plan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "spirit_stone_shard", "seeking_immortals:spirit_stone_shard", 8), plan.materials().get(1));
    }

    @Test
    void resolvesExactQingyeFanMaterialsAfterIronwoodCarrier() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_qingye_fan")
                .orElseThrow();

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe, Map.of(
                "ironwood", "seeking_immortals:ironwood",
                "spirit_stone_shard", "seeking_immortals:spirit_stone_shard"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:qingye_leaf_fan", plan.outputItemId());
        assertTrue(plan.missingMappings().isEmpty());
        assertEquals(2, plan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "ironwood", "seeking_immortals:ironwood", 3), plan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "spirit_stone_shard", "seeking_immortals:spirit_stone_shard", 4), plan.materials().get(1));
    }

    @Test
    void resolvesExactCloudBootsMaterialsAfterSpiritSilkCarrier() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_cloud_boots")
                .orElseThrow();

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe, Map.of(
                "spirit_silk", "seeking_immortals:spirit_silk",
                "low_spirit_iron", "seeking_immortals:low_spirit_iron"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:cloud_boots", plan.outputItemId());
        assertTrue(plan.missingMappings().isEmpty());
        assertEquals(2, plan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "spirit_silk", "seeking_immortals:spirit_silk", 3), plan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "low_spirit_iron", "seeking_immortals:low_spirit_iron", 2), plan.materials().get(1));
    }

    @Test
    void resolvesExactSpiritGatheringBeadMaterialsAfterSoulStoneCarrier() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_spirit_gathering_bead")
                .orElseThrow();

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe, Map.of(
                "soul_gathering_stone", "seeking_immortals:soul_gathering_stone",
                "spirit_stone_shard", "seeking_immortals:spirit_stone_shard"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:spirit_gathering_bead", plan.outputItemId());
        assertTrue(plan.missingMappings().isEmpty());
        assertEquals(2, plan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "soul_gathering_stone", "seeking_immortals:soul_gathering_stone", 1), plan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "spirit_stone_shard", "seeking_immortals:spirit_stone_shard", 5), plan.materials().get(1));
    }

    @Test
    void resolvesExactQingningMirrorMaterialsAfterKunwuCopperCarrier() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_qingning_mirror")
                .orElseThrow();

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe, Map.of(
                "hundred_year_ice", "seeking_immortals:cold_jade",
                "kunwu_copper", "seeking_immortals:kunwu_copper"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:qingning_mirror", plan.outputItemId());
        assertTrue(plan.missingMappings().isEmpty());
        assertEquals(2, plan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "hundred_year_ice", "seeking_immortals:cold_jade", 2), plan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "kunwu_copper", "seeking_immortals:kunwu_copper", 1), plan.materials().get(1));
    }

    @Test
    void resolvesExactGoldDemonChainMaterialsAfterGoldSeamStoneCarrier() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_gold_demon_chain")
                .orElseThrow();

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe, Map.of(
                "gold_seam_stone", "seeking_immortals:gold_seam_stone",
                "demon_core_mid", "seeking_immortals:demon_core_mid",
                "kunwu_copper", "seeking_immortals:kunwu_copper"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:gold_demon_chain", plan.outputItemId());
        assertTrue(plan.missingMappings().isEmpty());
        assertEquals(3, plan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "gold_seam_stone", "seeking_immortals:gold_seam_stone", 3), plan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "demon_core_mid", "seeking_immortals:demon_core_mid", 2), plan.materials().get(1));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "kunwu_copper", "seeking_immortals:kunwu_copper", 2), plan.materials().get(2));
    }

    @Test
    void resolvesDemonCoreTierAliasesThroughCurrentBeastCoreCarrier() {
        ArtifactDataService.RefinementRecipe lowRecipe = ArtifactDataService.builtin()
                .findRecipe("refine_yellow_umbrella")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan lowPlan = ArtifactRefinementService.resolvePlan(lowRecipe, Map.of(
                "spirit_silk", "seeking_immortals:spirit_silk",
                "demon_core_low", "seeking_immortals:demon_core_low",
                "low_spirit_iron", "seeking_immortals:low_spirit_iron"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:yellow_umbrella", lowPlan.outputItemId());
        assertTrue(lowPlan.missingMappings().isEmpty());
        assertEquals(3, lowPlan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "demon_core_low", "seeking_immortals:demon_core_low", 2), lowPlan.materials().get(1));

        ArtifactDataService.RefinementRecipe highRecipe = ArtifactDataService.builtin()
                .findRecipe("refine_hunyuan_bowl")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan highPlan = ArtifactRefinementService.resolvePlan(highRecipe, Map.of(
                "kunwu_copper", "seeking_immortals:kunwu_copper",
                "space_crystal", "seeking_immortals:space_crystal",
                "demon_core_high", "seeking_immortals:demon_core_high"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:hunyuan_bowl", highPlan.outputItemId());
        assertTrue(highPlan.missingMappings().isEmpty());
        assertEquals(3, highPlan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "demon_core_high", "seeking_immortals:demon_core_high", 1), highPlan.materials().get(2));
    }

    @Test
    void resolvesTalismanPaperAliasThroughMortalPaperCarrier() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_talisman_soul_charm")
                .orElseThrow();

        ArtifactRefinementService.ResolvedPlan plan = ArtifactRefinementService.resolvePlan(recipe, Map.of(
                "talisman_paper", "seeking_immortals:talisman_paper_mortal",
                "soul_gathering_stone", "seeking_immortals:soul_gathering_stone"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:talisman_treasure_soul_charm", plan.outputItemId());
        assertTrue(plan.missingMappings().isEmpty());
        assertEquals(2, plan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "talisman_paper", "seeking_immortals:talisman_paper_mortal", 3), plan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "soul_gathering_stone", "seeking_immortals:soul_gathering_stone", 1), plan.materials().get(1));
    }

    @Test
    void resolvesExistingMaterialAliasesForAdditionalArtifactPlans() {
        ArtifactDataService.RefinementRecipe phoenixFan = ArtifactDataService.builtin()
                .findRecipe("refine_phoenix_feather_fan")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan phoenixPlan = ArtifactRefinementService.resolvePlan(phoenixFan, Map.of(
                "fire_feather", "seeking_immortals:fire_feather",
                "true_spirit_blood_drop", "seeking_immortals:true_spirit_blood_drop"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:phoenix_feather_fan", phoenixPlan.outputItemId());
        assertTrue(phoenixPlan.missingMappings().isEmpty());
        assertEquals(2, phoenixPlan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "fire_feather", "seeking_immortals:fire_feather", 6), phoenixPlan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "true_spirit_blood_drop", "seeking_immortals:true_spirit_blood_drop", 1), phoenixPlan.materials().get(1));

        ArtifactDataService.RefinementRecipe dragonArmor = ArtifactDataService.builtin()
                .findRecipe("refine_dragon_scale_armor")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan armorPlan = ArtifactRefinementService.resolvePlan(dragonArmor, Map.of(
                "dragon_scale", "seeking_immortals:dragon_scale",
                "spirit_silk", "seeking_immortals:spirit_silk"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:dragon_scale_armor", armorPlan.outputItemId());
        assertTrue(armorPlan.missingMappings().isEmpty());
        assertEquals(2, armorPlan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "dragon_scale", "seeking_immortals:dragon_scale", 5), armorPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe boneCart = ArtifactDataService.builtin()
                .findRecipe("refine_bone_wind_cart")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan cartPlan = ArtifactRefinementService.resolvePlan(boneCart, Map.of(
                "beast_bone_block", "seeking_immortals:beast_bone_block",
                "spirit_silk", "seeking_immortals:spirit_silk"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:bone_wind_cart", cartPlan.outputItemId());
        assertTrue(cartPlan.missingMappings().isEmpty());
        assertEquals(2, cartPlan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "beast_bone_block", "seeking_immortals:beast_bone_block", 12), cartPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe glazedGuard = ArtifactDataService.builtin()
                .findRecipe("refine_glazed_guard")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan guardPlan = ArtifactRefinementService.resolvePlan(glazedGuard, Map.of(
                "space_crystal_fragment", "seeking_immortals:space_crystal_fragment",
                "kunwu_copper", "seeking_immortals:kunwu_copper"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:glazed_guard_shield", guardPlan.outputItemId());
        assertTrue(guardPlan.missingMappings().isEmpty());
        assertEquals(2, guardPlan.materials().size());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "space_crystal_fragment", "seeking_immortals:space_crystal_fragment", 2), guardPlan.materials().get(0));
    }

    @Test
    void resolvesYinAndSpecialMaterialAliasesThroughExistingCarriers() {
        ArtifactDataService.RefinementRecipe snakePearl = ArtifactDataService.builtin()
                .findRecipe("refine_snake_pearl")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan snakePlan = ArtifactRefinementService.resolvePlan(snakePearl, Map.of(
                "demon_core_fragment", "seeking_immortals:demon_core_fragment",
                "beast_blood_vial", "seeking_immortals:beast_blood_vial"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:snake_pearl", snakePlan.outputItemId());
        assertTrue(snakePlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "beast_blood_vial", "seeking_immortals:beast_blood_vial", 2), snakePlan.materials().get(1));

        ArtifactDataService.RefinementRecipe bedrockShield = ArtifactDataService.builtin()
                .findRecipe("refine_bedrock_shield")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan shieldPlan = ArtifactRefinementService.resolvePlan(bedrockShield, Map.of(
                "kunwu_copper", "seeking_immortals:kunwu_copper",
                "earth_spine_root", "seeking_immortals:earth_spine_root"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:bedrock_shield", shieldPlan.outputItemId());
        assertTrue(shieldPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "earth_spine_root", "seeking_immortals:earth_spine_root", 1), shieldPlan.materials().get(1));

        ArtifactDataService.RefinementRecipe evilMirror = ArtifactDataService.builtin()
                .findRecipe("refine_evil_illusion_mirror")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan mirrorPlan = ArtifactRefinementService.resolvePlan(evilMirror, Map.of(
                "hundred_year_ice", "seeking_immortals:cold_jade",
                "demon_corruption_fungus", "seeking_immortals:demon_corruption_fungus",
                "soul_gathering_stone", "seeking_immortals:soul_gathering_stone"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:evil_illusion_mirror", mirrorPlan.outputItemId());
        assertTrue(mirrorPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "demon_corruption_fungus", "seeking_immortals:demon_corruption_fungus", 3), mirrorPlan.materials().get(1));

        ArtifactDataService.RefinementRecipe moonDisk = ArtifactDataService.builtin()
                .findRecipe("refine_moon_shadow_disk")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan moonPlan = ArtifactRefinementService.resolvePlan(moonDisk, Map.of(
                "yin_essence_ore", "seeking_immortals:yin_essence_ore",
                "low_spirit_iron", "seeking_immortals:low_spirit_iron"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:moon_shadow_disk", moonPlan.outputItemId());
        assertTrue(moonPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "yin_essence_ore", "seeking_immortals:yin_essence_ore", 2), moonPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe coldJadePendant = ArtifactDataService.builtin()
                .findRecipe("refine_void_cold_jade_pendant")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan pendantPlan = ArtifactRefinementService.resolvePlan(coldJadePendant, Map.of(
                "void_palace_cold_jade", "seeking_immortals:cold_jade",
                "hundred_year_ice", "seeking_immortals:cold_jade"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:void_palace_cold_jade_pendant", pendantPlan.outputItemId());
        assertTrue(pendantPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "void_palace_cold_jade", "seeking_immortals:cold_jade", 2), pendantPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe soulBell = ArtifactDataService.builtin()
                .findRecipe("refine_soul_summon_bell")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan bellPlan = ArtifactRefinementService.resolvePlan(soulBell, Map.of(
                "ghost_wood", "seeking_immortals:ghost_wood",
                "soul_gathering_stone", "seeking_immortals:soul_gathering_stone",
                "low_spirit_iron", "seeking_immortals:low_spirit_iron"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:soul_summon_bell", bellPlan.outputItemId());
        assertTrue(bellPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "ghost_wood", "seeking_immortals:ghost_wood", 3), bellPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe poluoBeads = ArtifactDataService.builtin()
                .findRecipe("refine_poluo_beads")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan poluoPlan = ArtifactRefinementService.resolvePlan(poluoBeads, Map.of(
                "soul_moss", "seeking_immortals:soul_moss",
                "demon_core_low", "seeking_immortals:demon_core_low"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:poluo_beads", poluoPlan.outputItemId());
        assertTrue(poluoPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "soul_moss", "seeking_immortals:soul_moss", 5), poluoPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe sevenStarDisk = ArtifactDataService.builtin()
                .findRecipe("refine_seven_star_disk")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan sevenStarPlan = ArtifactRefinementService.resolvePlan(sevenStarDisk, Map.of(
                "star_sand", "seeking_immortals:star_sand",
                "kunwu_copper", "seeking_immortals:kunwu_copper"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:seven_star_disk", sevenStarPlan.outputItemId());
        assertTrue(sevenStarPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "star_sand", "seeking_immortals:star_sand", 4), sevenStarPlan.materials().get(0));
    }

    @Test
    void resolvesBeastWindAndNeedleAliasesThroughCurrentCarriers() {
        ArtifactDataService.RefinementRecipe beastWhip = ArtifactDataService.builtin()
                .findRecipe("refine_beast_taming_whip")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan whipPlan = ArtifactRefinementService.resolvePlan(beastWhip, Map.of(
                "beast_hide", "seeking_immortals:beast_hide",
                "demon_core_low", "seeking_immortals:demon_core_low"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:beast_taming_whip", whipPlan.outputItemId());
        assertTrue(whipPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "beast_hide", "seeking_immortals:beast_hide", 4), whipPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe bridle = ArtifactDataService.builtin()
                .findRecipe("refine_spirit_beast_bridle")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan bridlePlan = ArtifactRefinementService.resolvePlan(bridle, Map.of(
                "beast_hide", "seeking_immortals:beast_hide",
                "demon_core_low", "seeking_immortals:demon_core_low",
                "spirit_silk", "seeking_immortals:spirit_silk"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:spirit_beast_bridle", bridlePlan.outputItemId());
        assertTrue(bridlePlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "beast_hide", "seeking_immortals:beast_hide", 3), bridlePlan.materials().get(0));

        ArtifactDataService.RefinementRecipe windSail = ArtifactDataService.builtin()
                .findRecipe("refine_wind_escape_sail")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan sailPlan = ArtifactRefinementService.resolvePlan(windSail, Map.of(
                "spirit_silk", "seeking_immortals:spirit_silk",
                "wind_feather", "seeking_immortals:wind_feather"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:wind_escape_sail", sailPlan.outputItemId());
        assertTrue(sailPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "wind_feather", "seeking_immortals:wind_feather", 3), sailPlan.materials().get(1));

        ArtifactDataService.RefinementRecipe invisibleNeedles = ArtifactDataService.builtin()
                .findRecipe("refine_invisible_needles")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan needlePlan = ArtifactRefinementService.resolvePlan(invisibleNeedles, Map.of(
                "invisible_needle_set", "seeking_immortals:flying_needle_set",
                "geng_gold_inlay", "seeking_immortals:geng_gold_inlay",
                "low_spirit_iron", "seeking_immortals:low_spirit_iron"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:flying_needle_set", needlePlan.outputItemId());
        assertTrue(needlePlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "geng_gold_inlay", "seeking_immortals:geng_gold_inlay", 1), needlePlan.materials().get(0));

        ArtifactDataService.RefinementRecipe redThreadReplica = ArtifactDataService.builtin()
                .findRecipe("refine_red_thread_replica")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan redThreadPlan = ArtifactRefinementService.resolvePlan(redThreadReplica, Map.of(
                "gold_seam_stone", "seeking_immortals:gold_seam_stone",
                "spirit_silk", "seeking_immortals:spirit_silk",
                "invisible_needle_set", "seeking_immortals:flying_needle_set"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:red_thread_needles_replica", redThreadPlan.outputItemId());
        assertTrue(redThreadPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "invisible_needle_set", "seeking_immortals:flying_needle_set", 1), redThreadPlan.materials().get(2));
    }

    @Test
    void resolvesTurtleAndPoisonAliasesThroughVanillaCarriers() {
        ArtifactDataService.RefinementRecipe turtleCore = ArtifactDataService.builtin()
                .findRecipe("refine_giant_turtle_core")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan turtlePlan = ArtifactRefinementService.resolvePlan(turtleCore, Map.of(
                "turtle_shell", "seeking_immortals:turtle_shell",
                "demon_core_mid", "seeking_immortals:demon_core_mid",
                "ironwood", "seeking_immortals:ironwood"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:giant_turtle_puppet_core", turtlePlan.outputItemId());
        assertTrue(turtlePlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "turtle_shell", "seeking_immortals:turtle_shell", 4), turtlePlan.materials().get(0));

        ArtifactDataService.RefinementRecipe thousandBeeNeedles = ArtifactDataService.builtin()
                .findRecipe("refine_thousand_bee_needles")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan poisonPlan = ArtifactRefinementService.resolvePlan(thousandBeeNeedles, Map.of(
                "poison_sac", "seeking_immortals:poison_sac",
                "gold_seam_stone", "seeking_immortals:gold_seam_stone",
                "low_spirit_iron", "seeking_immortals:low_spirit_iron"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:thousand_bee_needles", poisonPlan.outputItemId());
        assertTrue(poisonPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "poison_sac", "seeking_immortals:poison_sac", 3), poisonPlan.materials().get(0));
    }

    @Test
    void resolvesNewPhysicalMaterialCarriersForPuppetThunderIceAndVoidPlans() {
        ArtifactDataService.RefinementRecipe apeToken = ArtifactDataService.builtin()
                .findRecipe("refine_giant_ape_token")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan apePlan = ArtifactRefinementService.resolvePlan(apeToken, Map.of(
                "ironwood", "seeking_immortals:ironwood",
                "talisman_paper", "seeking_immortals:talisman_paper_mortal",
                "puppet_core_blank", "seeking_immortals:puppet_core_blank"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:giant_ape_puppet_token", apePlan.outputItemId());
        assertTrue(apePlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "puppet_core_blank", "seeking_immortals:puppet_core_blank", 1), apePlan.materials().get(2));

        ArtifactDataService.RefinementRecipe thunderRod = ArtifactDataService.builtin()
                .findRecipe("refine_talisman_thunder_rod")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan thunderRodPlan = ArtifactRefinementService.resolvePlan(thunderRod, Map.of(
                "talisman_paper", "seeking_immortals:talisman_paper_mortal",
                "thunder_bamboo", "seeking_immortals:thunder_bamboo",
                "gold_seam_stone", "seeking_immortals:gold_seam_stone"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:talisman_treasure_thunder_rod", thunderRodPlan.outputItemId());
        assertTrue(thunderRodPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "thunder_bamboo", "seeking_immortals:thunder_bamboo", 2), thunderRodPlan.materials().get(1));

        ArtifactDataService.RefinementRecipe iceFireOrb = ArtifactDataService.builtin()
                .findRecipe("refine_ice_fire_orb")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan iceFirePlan = ArtifactRefinementService.resolvePlan(iceFireOrb, Map.of(
                "ice_fire_crystal", "seeking_immortals:ice_fire_crystal",
                "demon_core_high", "seeking_immortals:demon_core_high"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:ice_fire_dual_orb", iceFirePlan.outputItemId());
        assertTrue(iceFirePlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "ice_fire_crystal", "seeking_immortals:ice_fire_crystal", 2), iceFirePlan.materials().get(0));

        ArtifactDataService.RefinementRecipe voidBell = ArtifactDataService.builtin()
                .findRecipe("refine_void_refining_bell")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan voidPlan = ArtifactRefinementService.resolvePlan(voidBell, Map.of(
                "void_bell_fragment", "seeking_immortals:void_bell_fragment",
                "void_marrow", "seeking_immortals:void_marrow",
                "space_crystal", "seeking_immortals:space_crystal"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:void_refining_bell", voidPlan.outputItemId());
        assertTrue(voidPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "void_bell_fragment", "seeking_immortals:void_bell_fragment", 2), voidPlan.materials().get(0));
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "void_marrow", "seeking_immortals:void_marrow", 3), voidPlan.materials().get(1));
    }

    @Test
    void resolvesRemainingAncientShardAndTalismanBlankCarriers() {
        ArtifactDataService.RefinementRecipe xuanguangMirror = ArtifactDataService.builtin()
                .findRecipe("refine_xuanguang_shard")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan xuanguangPlan = ArtifactRefinementService.resolvePlan(xuanguangMirror, Map.of(
                "xuanguang_mirror_shard", "seeking_immortals:xuanguang_mirror_shard",
                "demon_core_high", "seeking_immortals:demon_core_high",
                "true_spirit_blood_drop", "seeking_immortals:true_spirit_blood_drop"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:xuanguang_mirror", xuanguangPlan.outputItemId());
        assertTrue(xuanguangPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "xuanguang_mirror_shard", "seeking_immortals:xuanguang_mirror_shard", 3), xuanguangPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe xuanhuangMirror = ArtifactDataService.builtin()
                .findRecipe("refine_xuanhuang_shard")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan xuanhuangPlan = ArtifactRefinementService.resolvePlan(xuanhuangMirror, Map.of(
                "xuanhuang_mirror_shard", "seeking_immortals:xuanhuang_mirror_shard",
                "demon_core_high", "seeking_immortals:demon_core_high",
                "soul_gathering_stone", "seeking_immortals:soul_gathering_stone"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:xuanhuang_mirror", xuanhuangPlan.outputItemId());
        assertTrue(xuanhuangPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "xuanhuang_mirror_shard", "seeking_immortals:xuanhuang_mirror_shard", 3), xuanhuangPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe nineDragonReplica = ArtifactDataService.builtin()
                .findRecipe("refine_nine_dragon_replica")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan nineDragonPlan = ArtifactRefinementService.resolvePlan(nineDragonReplica, Map.of(
                "nine_dragon_cauldron_shard", "seeking_immortals:nine_dragon_cauldron_shard",
                "fire_feather", "seeking_immortals:fire_feather",
                "kunwu_copper", "seeking_immortals:kunwu_copper",
                "demon_core_high", "seeking_immortals:demon_core_high"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:nine_dragon_cauldron_replica", nineDragonPlan.outputItemId());
        assertTrue(nineDragonPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "nine_dragon_cauldron_shard", "seeking_immortals:nine_dragon_cauldron_shard", 2), nineDragonPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe demonSeal = ArtifactDataService.builtin()
                .findRecipe("refine_talisman_demon_seal")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan demonSealPlan = ArtifactRefinementService.resolvePlan(demonSeal, Map.of(
                "talisman_paper", "seeking_immortals:talisman_paper_mortal",
                "demon_suppress_talisman_blank", "seeking_immortals:demon_suppress_talisman_blank",
                "demon_core_mid", "seeking_immortals:demon_core_mid"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:talisman_treasure_demon_seal", demonSealPlan.outputItemId());
        assertTrue(demonSealPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "demon_suppress_talisman_blank", "seeking_immortals:demon_suppress_talisman_blank", 1), demonSealPlan.materials().get(1));
    }

    @Test
    void resolvesSpiritRealmReplicaAndNatalEmbryoCarriers() {
        ArtifactDataService.RefinementRecipe natalEmbryo = ArtifactDataService.builtin()
                .findRecipe("refine_natal_embryo")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan natalEmbryoPlan = ArtifactRefinementService.resolvePlan(natalEmbryo, Map.of(
                "kunwu_copper", "seeking_immortals:kunwu_copper",
                "demon_core_high", "seeking_immortals:demon_core_high",
                "true_spirit_blood_drop", "seeking_immortals:true_spirit_blood_drop"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:natal_artifact_embryo", natalEmbryoPlan.outputItemId());
        assertTrue(natalEmbryoPlan.missingMappings().isEmpty());

        ArtifactDataService.RefinementRecipe natalSwordEmbryo = ArtifactDataService.builtin()
                .findRecipe("refine_natal_sword_embryo")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan natalSwordPlan = ArtifactRefinementService.resolvePlan(natalSwordEmbryo, Map.of(
                "natal_artifact_embryo", "seeking_immortals:natal_artifact_embryo",
                "ironwood", "seeking_immortals:ironwood",
                "gold_seam_stone", "seeking_immortals:gold_seam_stone"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:natal_sword_embryo", natalSwordPlan.outputItemId());
        assertTrue(natalSwordPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "natal_artifact_embryo", "seeking_immortals:natal_artifact_embryo", 1), natalSwordPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe fourSymbolsRuler = ArtifactDataService.builtin()
                .findRecipe("refine_four_symbols_ruler")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan rulerPlan = ArtifactRefinementService.resolvePlan(fourSymbolsRuler, Map.of(
                "eight_spirit_ruler_shard", "seeking_immortals:eight_spirit_ruler_shard",
                "kunwu_copper", "seeking_immortals:kunwu_copper"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:four_symbols_ruler_replica", rulerPlan.outputItemId());
        assertTrue(rulerPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "eight_spirit_ruler_shard", "seeking_immortals:eight_spirit_ruler_shard", 1), rulerPlan.materials().get(0));

        ArtifactDataService.RefinementRecipe threeFlameFan = ArtifactDataService.builtin()
                .findRecipe("refine_three_flame_fan")
                .orElseThrow();
        ArtifactRefinementService.ResolvedPlan fanPlan = ArtifactRefinementService.resolvePlan(threeFlameFan, Map.of(
                "seven_flame_fan_replica", "seeking_immortals:seven_flame_fan_replica",
                "fire_feather", "seeking_immortals:fire_feather"
        ), "seeking_immortals");

        assertEquals("seeking_immortals:three_flame_fan_replica", fanPlan.outputItemId());
        assertTrue(fanPlan.missingMappings().isEmpty());
        assertEquals(new ArtifactRefinementService.ResolvedMaterial(
                "seven_flame_fan_replica", "seeking_immortals:seven_flame_fan_replica", 1), fanPlan.materials().get(0));
    }

    @Test
    void rollsUseSourceBaseSuccessRateAsExclusiveUpperBound() {
        assertTrue(ArtifactRefinementService.succeeds(0.69D, 0.7D));
        assertFalse(ArtifactRefinementService.succeeds(0.7D, 0.7D));
        assertFalse(ArtifactRefinementService.succeeds(0.0D, 0.0D));
        assertTrue(ArtifactRefinementService.succeeds(0.999D, 2.0D));
        assertFalse(ArtifactRefinementService.succeeds(-0.1D, 0.7D));
    }

    @Test
    void selectsFailureLootFromTierAndDefaultTables() {
        ArtifactDataService.RefinementRecipe lowRecipe = ArtifactDataService.builtin()
                .findRecipe("refine_flying_sword_low")
                .orElseThrow();
        ArtifactRefinementService.ResolvedFailureLoot lowLoot = ArtifactRefinementService.selectFailureLoot(
                lowRecipe, 0, 1, Map.of(
                        "low_spirit_iron", "seeking_immortals:low_spirit_iron"
                ), "seeking_immortals");

        assertEquals("low_spirit_iron", lowLoot.sourceId());
        assertEquals("seeking_immortals:low_spirit_iron", lowLoot.itemId());
        assertEquals(2, lowLoot.count());
        assertFalse(lowLoot.missingMapping());
        assertFalse(lowLoot.isEmpty());

        ArtifactDataService.RefinementRecipe unknownTierRecipe = new ArtifactDataService.RefinementRecipe(
                "test_unknown_tier", "test_artifact", "Test Artifact", "unknown_tier",
                "QI_REFINING", 1, 0.5D, List.of());
        ArtifactRefinementService.ResolvedFailureLoot defaultLoot = ArtifactRefinementService.selectFailureLoot(
                unknownTierRecipe, 0, 0, Map.of(
                        "scrap_spirit_iron", "seeking_immortals:scrap_spirit_iron"
                ), "seeking_immortals");

        assertEquals("scrap_spirit_iron", defaultLoot.sourceId());
        assertEquals("seeking_immortals:scrap_spirit_iron", defaultLoot.itemId());
        assertEquals(1, defaultLoot.count());
        assertFalse(defaultLoot.isEmpty());
    }

    @Test
    void failureLootMissingMappingsDoNotFallbackToFakeModIds() {
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin()
                .findRecipe("refine_flying_sword_low")
                .orElseThrow();

        ArtifactRefinementService.ResolvedFailureLoot loot = ArtifactRefinementService.selectFailureLoot(
                recipe, 50, 0, Map.of(), "seeking_immortals");

        assertEquals("scrap_spirit_iron", loot.sourceId());
        assertTrue(loot.missingMapping());
        assertTrue(loot.isEmpty());
    }

    @Test
    void mapsTextMaterialRealmAliasesToRuntimeRealms() {
        assertEquals(Realm.QI_REFINING, ArtifactRefinementService.realmFromDesignId("QI_REFINING"));
        assertEquals(Realm.FOUNDATION_ESTABLISHMENT, ArtifactRefinementService.realmFromDesignId("FOUNDATION"));
        assertEquals(Realm.SOUL_TRANSFORMATION, ArtifactRefinementService.realmFromDesignId("DEITY_TRANSFORMATION"));
        assertEquals(Realm.UNITY, ArtifactRefinementService.realmFromDesignId("BODY_INTEGRATION"));
        assertEquals(Realm.TRIBULATION, ArtifactRefinementService.realmFromDesignId("TRIBULATION_LAND"));
    }
}
