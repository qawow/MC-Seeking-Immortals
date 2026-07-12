package com.xunxian.seekingimmortals.spiritual;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpiritualAuraManagerTest {
    private static final double DELTA = 0.0001D;

    @Test
    void spiritRealmDimensionsUseSettingAuraProfiles() {
        ResourceLocation tianyuan = new ResourceLocation("seeking_immortals", "tianyuan");
        ResourceLocation spiritFengyuan = new ResourceLocation("seeking_immortals", "spirit_fengyuan");

        assertEquals(2.0D, SpiritualAuraManager.getDimensionMultiplier(tianyuan), DELTA);
        assertEquals(1.8D, SpiritualAuraManager.getDimensionMultiplier(spiritFengyuan), DELTA);
        assertEquals(SpiritualAuraManager.AuraNature.SPIRIT_REALM, SpiritualAuraManager.getAuraNature(tianyuan));
        assertEquals(SpiritualAuraManager.AuraNature.SPIRIT_REALM, SpiritualAuraManager.getAuraNature(spiritFengyuan));
    }

    @Test
    void yinPocketDimensionsUseUnderworldAuraProfiles() {
        ResourceLocation yinming = new ResourceLocation("seeking_immortals", "yin_ming_pocket");
        ResourceLocation netherRiver = new ResourceLocation("seeking_immortals", "nether_river_pocket");

        assertEquals(0.7D, SpiritualAuraManager.getDimensionMultiplier(yinming), DELTA);
        assertEquals(0.85D, SpiritualAuraManager.getDimensionMultiplier(netherRiver), DELTA);
        assertEquals(SpiritualAuraManager.AuraNature.YIN_UNDERWORLD, SpiritualAuraManager.getAuraNature(yinming));
        assertEquals(SpiritualAuraManager.AuraNature.YIN_UNDERWORLD, SpiritualAuraManager.getAuraNature(netherRiver));
    }

    @Test
    void demonRiftUsesDemonicAuraProfile() {
        ResourceLocation demonRift = new ResourceLocation("seeking_immortals", "demon_rift");

        assertEquals(1.1D, SpiritualAuraManager.getDimensionMultiplier(demonRift), DELTA);
        assertEquals(SpiritualAuraManager.AuraNature.BODY_REFINING_FIRE_DEMONIC,
                SpiritualAuraManager.getAuraNature(demonRift));
    }

    @Test
    void existingDimensionAuraProfilesStayStable() {
        ResourceLocation overworld = new ResourceLocation("minecraft", "overworld");
        ResourceLocation nether = new ResourceLocation("minecraft", "the_nether");
        ResourceLocation end = new ResourceLocation("minecraft", "the_end");
        ResourceLocation secretRealm = new ResourceLocation("seeking_immortals", "secret_realm_instance");

        assertEquals(1.0D, SpiritualAuraManager.getDimensionMultiplier(overworld), DELTA);
        assertEquals(1.2D, SpiritualAuraManager.getDimensionMultiplier(nether), DELTA);
        assertEquals(1.5D, SpiritualAuraManager.getDimensionMultiplier(end), DELTA);
        assertEquals(10.0D, SpiritualAuraManager.getDimensionMultiplier(secretRealm), DELTA);
        assertEquals(SpiritualAuraManager.AuraNature.NORMAL, SpiritualAuraManager.getAuraNature(overworld));
        assertEquals(SpiritualAuraManager.AuraNature.BODY_REFINING_FIRE_DEMONIC, SpiritualAuraManager.getAuraNature(nether));
        assertEquals(SpiritualAuraManager.AuraNature.LAW_VOID, SpiritualAuraManager.getAuraNature(end));
        assertEquals(SpiritualAuraManager.AuraNature.SECRET_REALM, SpiritualAuraManager.getAuraNature(secretRealm));
    }
}
