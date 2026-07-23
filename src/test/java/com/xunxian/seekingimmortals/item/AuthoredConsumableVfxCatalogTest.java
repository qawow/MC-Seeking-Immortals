package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.item.pill.PillEffectCatalog;
import com.xunxian.seekingimmortals.item.pill.PillType;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthoredConsumableVfxCatalogTest {
    @Test
    void authoredProfilesCoverBothCatalogsWithoutCollapsingDuplicateIds() {
        assertEquals(114, AuthoredConsumableVfxCatalog.pills().size());
        assertEquals(57, AuthoredConsumableVfxCatalog.consumables().size());
        assertEquals(171, AuthoredConsumableVfxCatalog.profiles().size());
        assertTrue(AuthoredConsumableVfxCatalog.pills().keySet()
                .containsAll(PillEffectCatalog.all().keySet()));
        PillEffectCatalog.all().forEach((id, entry) -> {
            AuthoredConsumableVfxCatalog.Profile profile =
                    AuthoredConsumableVfxCatalog.pills().get(id);
            assertEquals(entry.category(), profile.category(), id + " category");
            assertEquals(entry.effect(), profile.effect(), id + " effect");
        });

        AuthoredConsumableVfxCatalog.Profile pill =
                AuthoredConsumableVfxCatalog.findPill("bigu_pill").orElseThrow();
        AuthoredConsumableVfxCatalog.Profile consumable =
                AuthoredConsumableVfxCatalog.findConsumable("bigu_pill").orElseThrow();
        assertEquals(AuthoredConsumableVfxCatalog.Profile.Type.PILL, pill.type());
        assertEquals(AuthoredConsumableVfxCatalog.Profile.Type.CONSUMABLE, consumable.type());
        assertNotEquals(pill.sources(), consumable.sources());

        AuthoredConsumableVfxCatalog.profiles().values().forEach(profile -> {
            assertNotEquals(TechniqueVfxPacket.ParticleStyle.DEFAULT, profile.particle(), profile.id());
            assertNotEquals(TechniqueVfxPacket.TrailStyle.DEFAULT, profile.trail(), profile.id());
            assertTrue(profile.hasAuthoredText(), profile.id());
            assertFalse(profile.sources().isEmpty(), profile.id());
            if (profile.type() == AuthoredConsumableVfxCatalog.Profile.Type.PILL) {
                assertFalse(profile.frames().isEmpty(), profile.id());
            }
        });
    }

    @Test
    void pillResolutionKeepsExactIdsBeforeAliasesAndQualityFallbacks() {
        assertEquals("spirit_recovery_pill_high",
                AuthoredConsumableVfxCatalog.findPill("spirit_recovery_pill_high").orElseThrow().id());
        assertEquals("spirit_recovery_pill",
                AuthoredConsumableVfxCatalog.findPill("spirit_recovery_pill_mid").orElseThrow().id());
        assertEquals("jiangchen_pill",
                AuthoredConsumableVfxCatalog.findPill("jiangying_pill").orElseThrow().id());
        assertEquals("dingyan_pill",
                AuthoredConsumableVfxCatalog.findPill("appearance_lock_pill").orElseThrow().id());

        assertTrue(AuthoredConsumableVfxCatalog.pills().containsKey("jiangying_pill"));
        assertEquals(AuthoredConsumableVfxCatalog.pills().get("jiangchen_pill").effect(),
                AuthoredConsumableVfxCatalog.pills().get("jiangying_pill").effect());
        assertTrue(AuthoredConsumableVfxCatalog.findPill("not_in_author_corpus").isEmpty());
    }

    @Test
    void runtimeOnlyPillIdsAndLegacyItemsResolveToAuthoredProfiles() {
        assertEquals("juling_pill", ConsumableVfxOrchestrator.visualPillId("spirit_gathering_pill"));
        assertEquals("huoyuan_pill", ConsumableVfxOrchestrator.visualPillId("fire_origin_pill"));
        assertEquals("condensation_pill", ConsumableVfxOrchestrator.visualPillId("essence_condensing_pill"));
        assertEquals("huiyang_true_water", ConsumableVfxOrchestrator.visualPillId("return_yang_true_water"));
        assertEquals("soul_break_pill", ConsumableVfxOrchestrator.visualPillId("soul_breaking_pill"));

        for (PillType type : PillType.values()) {
            String id = ConsumableVfxOrchestrator.legacyPillId(type);
            assertFalse(id.isBlank(), type.name());
            assertTrue(AuthoredConsumableVfxCatalog.findPill(id).isPresent(), type.name() + " -> " + id);
        }
    }

    @Test
    void storageProfilesAreIdentifiedForUiOnlyUse() {
        assertTrue(AuthoredConsumableVfxCatalog.findConsumable("storage_pouch_low")
                .orElseThrow().storageLike());
        assertTrue(AuthoredConsumableVfxCatalog.findConsumable("storage_bag_high")
                .orElseThrow().storageLike());
        assertFalse(AuthoredConsumableVfxCatalog.findConsumable("fireball_talisman")
                .orElseThrow().storageLike());
    }

    @Test
    void everyHookEmitsOnlyAfterItsAuthoritativeSuccessBranch() throws Exception {
        String basePill = source("item/pill/BasePillItem.java");
        String catalogPill = source("item/pill/CatalogPillItem.java");
        String pillEffects = source("item/pill/PillEffectCatalog.java");
        String consumables = source("item/CatalogConsumableService.java");
        String coffinNail = source("item/YinCoffinNailItem.java");
        String taxReceipt = source("item/StarPalaceTaxReceiptItem.java");
        String yinCharm = source("item/YinProtectionCharmItem.java");

        assertTrue(basePill.indexOf("emitLegacyPill(serverPlayer, pillType, quality)")
                > basePill.indexOf("if (consumePill(serverPlayer))"));
        assertTrue(catalogPill.indexOf("emitPill(serverPlayer, type.id(), quality)")
                > catalogPill.indexOf("if (consumed)"));
        assertTrue(pillEffects.indexOf("emitPill(player, entry.pillId(), resolvedQuality)")
                > pillEffects.indexOf("if (consumed)"));
        assertTrue(consumables.indexOf("emitConsumable(player, id, action)")
                > consumables.indexOf("if (success)"));
        assertTrue(coffinNail.indexOf("emitConsumable(player, \"yin_coffin_nail\", \"corpse_control\")")
                > coffinNail.indexOf("if (success)"));
        assertTrue(taxReceipt.indexOf("emitConsumable(serverPlayer,")
                > taxReceipt.indexOf("if (!applied)"));
        assertTrue(yinCharm.indexOf("emitConsumable(serverPlayer,")
                > yinCharm.indexOf("if (activated)"));
    }

    @Test
    void twoAuthoredPillSpecialsRemainInTheGeneratedProvenance() {
        assertEquals("vis_ultra_foundation_pill",
                AuthoredConsumableVfxCatalog.findPill("foundation_pill")
                        .orElseThrow().sources().get("v118_ultra"));
        assertEquals("vis_ultra_jiangchen",
                AuthoredConsumableVfxCatalog.findPill("jiangchen_pill")
                        .orElseThrow().sources().get("v118_ultra"));
    }

    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals")
                .resolve(relative));
    }
}
