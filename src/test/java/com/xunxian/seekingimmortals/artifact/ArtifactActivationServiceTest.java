package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.Realm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactActivationServiceTest {
    @Test
    void classifiesPriorityArtifactActivations() {
        ArtifactActivationService.ActivationInfo cloudBoots =
                ArtifactActivationService.activationInfo("cloud_boots").orElseThrow();
        assertEquals("movement", cloudBoots.kind());
        assertEquals(Realm.QI_REFINING, cloudBoots.minRealm());
        assertTrue(cloudBoots.spiritualPowerCost() > 0);
        assertTrue(cloudBoots.cooldownTicks() > 0);

        ArtifactActivationService.ActivationInfo qingyeFan =
                ArtifactActivationService.activationInfo("qingye_leaf_fan").orElseThrow();
        assertEquals("offense", qingyeFan.kind());
        assertEquals(Realm.QI_REFINING, qingyeFan.minRealm());

        ArtifactActivationService.ActivationInfo yellowUmbrella =
                ArtifactActivationService.activationInfo("yellow_umbrella").orElseThrow();
        assertEquals("defense", yellowUmbrella.kind());
        assertEquals(Realm.FOUNDATION_ESTABLISHMENT, yellowUmbrella.minRealm());
    }

    @Test
    void exposesTalismanTreasureUsesAndRealmGate() {
        ArtifactActivationService.ActivationInfo goldBrick =
                ArtifactActivationService.activationInfo("gold_light_brick").orElseThrow();
        assertEquals("talisman", goldBrick.kind());
        assertEquals(3, goldBrick.maxUses());
        assertEquals(Realm.FOUNDATION_ESTABLISHMENT, goldBrick.minRealm());

        ArtifactActivationService.ActivationInfo soulCharm =
                ArtifactActivationService.activationInfo("talisman_treasure_soul_charm").orElseThrow();
        assertEquals("talisman", soulCharm.kind());
        assertEquals(3, soulCharm.maxUses());
    }

    @Test
    void supportsHighTierArtifactActivationFollowUps() {
        ArtifactActivationService.ActivationInfo xuanhuangMirror =
                ArtifactActivationService.activationInfo("xuanhuang_mirror").orElseThrow();
        assertEquals("soul_destroy", xuanhuangMirror.kind());
        assertEquals(Realm.NASCENT_SOUL, xuanhuangMirror.minRealm());
        assertTrue(xuanhuangMirror.integrityCost() > 0);

        ArtifactActivationService.ActivationInfo voidBell =
                ArtifactActivationService.activationInfo("void_refining_bell").orElseThrow();
        assertEquals("space_control", voidBell.kind());
        assertEquals(Realm.VOID_REFINEMENT, voidBell.minRealm());
        assertTrue(voidBell.cooldownTicks() > xuanhuangMirror.cooldownTicks());

        ArtifactActivationService.ActivationInfo demonSeal =
                ArtifactActivationService.activationInfo("talisman_treasure_demon_seal").orElseThrow();
        assertEquals("talisman", demonSeal.kind());
        assertEquals(Realm.CORE_FORMATION, demonSeal.minRealm());
        assertEquals(3, demonSeal.maxUses());

        assertFalse(ArtifactActivationService.hasActivation("natal_sword_embryo"));
    }

    @Test
    void supportsArtifactFamilyActivationFollowUps() {
        ArtifactActivationService.ActivationInfo ruler =
                ArtifactActivationService.activationInfo("eight_spirit_ruler_replica").orElseThrow();
        assertEquals("ruler", ruler.kind());
        assertEquals(Realm.VOID_REFINEMENT, ruler.minRealm());
        assertTrue(ruler.integrityCost() > 0);

        ArtifactActivationService.ActivationInfo mirror =
                ArtifactActivationService.activationInfo("flat_mirror_treasure").orElseThrow();
        assertEquals("mirror", mirror.kind());
        assertEquals(Realm.NASCENT_SOUL, mirror.minRealm());

        ArtifactActivationService.ActivationInfo soundBell =
                ArtifactActivationService.activationInfo("sound_attack_bell").orElseThrow();
        assertEquals("sound", soundBell.kind());
        assertEquals(Realm.CORE_FORMATION, soundBell.minRealm());

        ArtifactActivationService.ActivationInfo swarm =
                ArtifactActivationService.activationInfo("gold_devour_beetle_nest").orElseThrow();
        assertEquals("swarm", swarm.kind());
        assertEquals(Realm.NASCENT_SOUL, swarm.minRealm());

        ArtifactActivationService.ActivationInfo soulSummonBell =
                ArtifactActivationService.activationInfo("soul_summon_bell").orElseThrow();
        assertEquals("beast_control", soulSummonBell.kind());
        assertEquals(Realm.QI_REFINING, soulSummonBell.minRealm());

        ArtifactActivationService.ActivationInfo silverMoonWolf =
                ArtifactActivationService.activationInfo("silver_moon_wolf").orElseThrow();
        assertEquals("beast_control", silverMoonWolf.kind());
        assertEquals(Realm.CORE_FORMATION, silverMoonWolf.minRealm());
    }

    @Test
    void supportsUtilityArtifactActivationFollowUps() {
        ArtifactActivationService.ActivationInfo greatShiftToken =
                ArtifactActivationService.activationInfo("great_shift_token").orElseThrow();
        assertEquals("teleport_protection", greatShiftToken.kind());
        assertEquals(Realm.CORE_FORMATION, greatShiftToken.minRealm());
        assertTrue(greatShiftToken.integrityCost() > 0);

        ArtifactActivationService.ActivationInfo magnetShard =
                ArtifactActivationService.activationInfo("primordial_magnet_peak_shard").orElseThrow();
        assertEquals("magnet", magnetShard.kind());
        assertEquals(Realm.VOID_REFINEMENT, magnetShard.minRealm());
        assertTrue(magnetShard.spiritualPowerCost() > greatShiftToken.spiritualPowerCost());

        ArtifactActivationService.ActivationInfo mountainsPearl =
                ArtifactActivationService.activationInfo("rivers_mountains_pearl_replica").orElseThrow();
        assertEquals("world", mountainsPearl.kind());
        assertEquals(Realm.VOID_REFINEMENT, mountainsPearl.minRealm());

        ArtifactActivationService.ActivationInfo barrierToken =
                ArtifactActivationService.activationInfo("nine_dragon_barrier_token").orElseThrow();
        assertEquals("formation", barrierToken.kind());
        assertEquals(Realm.CORE_FORMATION, barrierToken.minRealm());

        ArtifactActivationService.ActivationInfo yinYangDisk =
                ArtifactActivationService.activationInfo("yin_yang_disk").orElseThrow();
        assertEquals("formation", yinYangDisk.kind());
        assertEquals(Realm.FOUNDATION_ESTABLISHMENT, yinYangDisk.minRealm());
    }

    @Test
    void supportsSupportArtifactActivationFollowUps() {
        ArtifactActivationService.ActivationInfo concealCloth =
                ArtifactActivationService.activationInfo("aura_conceal_cloth").orElseThrow();
        assertEquals("utility", concealCloth.kind());
        assertEquals(Realm.QI_REFINING, concealCloth.minRealm());

        ArtifactActivationService.ActivationInfo riftCompass =
                ArtifactActivationService.activationInfo("space_rift_compass").orElseThrow();
        assertEquals("utility", riftCompass.kind());
        assertEquals(Realm.NASCENT_SOUL, riftCompass.minRealm());
        assertTrue(riftCompass.integrityCost() > 0);

        ArtifactActivationService.ActivationInfo purpleGoldBowl =
                ArtifactActivationService.activationInfo("purple_gold_bowl").orElseThrow();
        assertEquals("capture", purpleGoldBowl.kind());
        assertEquals(Realm.CORE_FORMATION, purpleGoldBowl.minRealm());

        ArtifactActivationService.ActivationInfo voidRefiningPot =
                ArtifactActivationService.activationInfo("void_refining_pot").orElseThrow();
        assertEquals("refinement", voidRefiningPot.kind());
        assertEquals(Realm.NASCENT_SOUL, voidRefiningPot.minRealm());

        ArtifactActivationService.ActivationInfo greenVase =
                ArtifactActivationService.activationInfo("spirit_nurture_green_vase").orElseThrow();
        assertEquals("spirit_liquid", greenVase.kind());
        assertEquals(Realm.VOID_REFINEMENT, greenVase.minRealm());

        ArtifactActivationService.ActivationInfo starBoat =
                ArtifactActivationService.activationInfo("star_chasing_boat").orElseThrow();
        assertEquals("vehicle", starBoat.kind());
        assertEquals(Realm.CORE_FORMATION, starBoat.minRealm());

        ArtifactActivationService.ActivationInfo spiritBoatModel =
                ArtifactActivationService.activationInfo("spirit_boat_model").orElseThrow();
        assertEquals("vehicle", spiritBoatModel.kind());
        assertEquals(Realm.FOUNDATION_ESTABLISHMENT, spiritBoatModel.minRealm());

        assertTrue(ArtifactActivationService.hasActivation("generic_treasure_grade_5"));
    }

    @Test
    void keepsStorageOutsideCombatActivationButExposesCapacity() {
        assertTrue(ArtifactActivationService.hasActivation("storage_bracelet_low"));
        assertTrue(ArtifactStorageService.supports("storage_bracelet_low"));
        assertEquals(8, ArtifactStorageService.storageSlots("storage_bracelet_low"));
    }

    @Test
    void exposesRepairKitAndIntegrityRules() {
        ArtifactActivationService.ActivationInfo repairKit =
                ArtifactActivationService.activationInfo("artifact_repair_kit").orElseThrow();
        assertEquals("repair", repairKit.kind());
        assertEquals(Realm.QI_REFINING, repairKit.minRealm());
        assertEquals(0, repairKit.integrityCost());
        assertTrue(repairKit.repairAmount() > 0);

        ArtifactDataService.ArtifactDefinition cloudBoots = ArtifactDataService.builtin()
                .findArtifact("cloud_boots")
                .orElseThrow();
        ArtifactActivationService.ActivationInfo cloudBootsActivation =
                ArtifactActivationService.activationInfo("cloud_boots").orElseThrow();

        assertEquals(100, ArtifactActivationService.maxIntegrity(cloudBoots));
        assertTrue(cloudBootsActivation.integrityCost() > 0);
    }
}
