package com.xunxian.seekingimmortals.catalog;

import net.minecraft.nbt.CompoundTag;
import com.xunxian.seekingimmortals.cultivation.Realm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualCatalogServiceMethodLayerTest {
    @Test
    void cultivateCostsScaleWithLayer() {
        int sp1 = ManualCatalogService.cultivateSpiritualCost(1);
        int sp5 = ManualCatalogService.cultivateSpiritualCost(5);
        int exp1 = ManualCatalogService.cultivateCultivationCost(1);
        int exp5 = ManualCatalogService.cultivateCultivationCost(5);
        assertTrue(sp1 >= 20);
        assertTrue(sp5 > sp1);
        assertTrue(exp1 >= 40);
        assertTrue(exp5 > exp1);
        assertEquals(20 + 12, ManualCatalogService.cultivateSpiritualCost(1));
        assertEquals(40 + 30, ManualCatalogService.cultivateCultivationCost(1));
    }

    @Test
    void methodMaxLayersFollowCatalogAndMatrix() {
        assertEquals(13, ManualCatalogService.maxMethodLayer("changchun_gong"));
        assertEquals(13, ManualCatalogService.maxMethodLayer("qingyuan_sword_art"));
        assertEquals(12, ManualCatalogService.maxMethodLayer("treasure_appraisal_art"));
        assertEquals(1, ManualCatalogService.maxMethodLayer("huangfeng_alchemy_scripture"));
        assertEquals(1, ManualCatalogService.maxMethodLayer("unknown_method"));
    }

    @Test
    void learningGateHonorsRealmMaximumAndPrerequisiteLayers() {
        TextMaterialCatalogService.MethodEntry changchun = TextMaterialCatalogService.builtin()
                .findMethod("changchun_gong").orElseThrow();
        assertEquals(ManualCatalogService.MethodLearnFailure.REALM_TOO_HIGH,
                ManualCatalogService.evaluateLearnGate(changchun, Realm.FOUNDATION_ESTABLISHMENT,
                        ignored -> 0).failure());

        TextMaterialCatalogService.MethodEntry qingyuan = TextMaterialCatalogService.builtin()
                .findMethod("qingyuan_sword_art").orElseThrow();
        assertEquals(ManualCatalogService.MethodLearnFailure.PREREQUISITE_MISSING,
                ManualCatalogService.evaluateLearnGate(qingyuan, Realm.FOUNDATION_ESTABLISHMENT,
                        ignored -> 0).failure());
        assertEquals(ManualCatalogService.MethodLearnFailure.PREREQUISITE_LAYER,
                ManualCatalogService.evaluateLearnGate(qingyuan, Realm.FOUNDATION_ESTABLISHMENT,
                        ignored -> 12).failure());
        assertTrue(ManualCatalogService.evaluateLearnGate(qingyuan, Realm.FOUNDATION_ESTABLISHMENT,
                ignored -> 13).isAllowed());
    }

    @Test
    void deathCloneCopiesAllConsumedManualProgression() {
        CompoundTag original = new CompoundTag();
        CompoundTag studied = new CompoundTag();
        studied.putBoolean("manual_refinement_g2", true);
        original.put(ManualCatalogService.STUDIED_TAG, studied);
        CompoundTag learned = new CompoundTag();
        learned.putBoolean("mixed_method", true);
        original.put(ManualCatalogService.LEARNED_METHODS_TAG, learned);
        CompoundTag layers = new CompoundTag();
        layers.putInt("mixed_method", 4);
        original.put(ManualCatalogService.METHOD_LAYERS_TAG, layers);

        CompoundTag clone = new CompoundTag();
        ManualCatalogService.copyProgressionData(original, clone);

        assertTrue(clone.getCompound(ManualCatalogService.STUDIED_TAG)
                .getBoolean("manual_refinement_g2"));
        assertTrue(clone.getCompound(ManualCatalogService.LEARNED_METHODS_TAG)
                .getBoolean("mixed_method"));
        assertEquals(4, clone.getCompound(ManualCatalogService.METHOD_LAYERS_TAG)
                .getInt("mixed_method"));
    }
}
