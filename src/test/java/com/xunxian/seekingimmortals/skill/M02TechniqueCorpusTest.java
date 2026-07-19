package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M02 acceptance: published text_material techniques load into TechniqueDataManager,
 * structured effect fields load, unsupported families fail closed, and method matrices remain present.
 */
class M02TechniqueCorpusTest {
    @Test
    void loadsAuthoritativeTechniqueCorpus() {
        Map<String, TechniqueDataManager.TechniqueEntry> techniques = TechniqueDataManager.builtinTechniques();
        // Corpus 747 + residual jar-only cultivation ids that are not in corpus.
        assertTrue(techniques.size() >= 747, "expected >=747 loaded techniques, got " + techniques.size());
        assertTrue(TechniqueDataManager.builtinTechniqueCount() >= 747);
        // A known corpus id must be present with effect metadata.
        TechniqueDataManager.TechniqueEntry fireball = techniques.get("fireball");
        assertNotNull(fireball, "fireball from text_material elemental pack");
        assertEquals("projectile", fireball.effectType());
        assertTrue(fireball.cost() > 0);
        assertFalse(fireball.requiresMethod().isBlank());
        assertEquals(12.0D, fireball.damageBase(), 0.001D);
        assertEquals("single", fireball.target());
        assertEquals("medium", fireball.range());
        assertTrue(fireball.tags().contains("fire"));

        TechniqueDataManager.TechniqueEntry talisman = techniques.get("cast_fire_burst_talisman");
        assertNotNull(talisman);
        assertEquals("aoe_fire", talisman.effectKey());
        assertEquals(18.0D, talisman.damageBase(), 0.001D);
    }

    @Test
    void structuredRuntimeUsesAuthoredDamageTargetRangeAndTags() {
        Map<String, TechniqueDataManager.TechniqueEntry> techniques = TechniqueDataManager.builtinTechniques();
        var fireball = AbstractTechniqueEffectResolver.runtimeSpec(techniques.get("fireball"));
        assertEquals("projectile", fireball.type());
        assertEquals(12.0D, fireball.damage(), 0.001D);
        assertEquals(20.0D, fireball.range(), 0.001D);
        assertEquals("single", fireball.target());
        assertEquals("fire", fireball.element());
        assertTrue(fireball.tags().contains("fire"));

        var scan = AbstractTechniqueEffectResolver.runtimeSpec(techniques.get("divine_sense_scan"));
        assertEquals("scan", scan.type());
        assertEquals(32.0D, scan.range(), 0.001D);
        assertEquals("area", scan.target());
        assertEquals(0.0D, scan.damage(), 0.001D);
        assertTrue(AbstractTechniqueEffectResolver.isGenericRuntimeType(scan.type()));
        assertTrue(AbstractTechniqueEffectResolver.registeredAbstractTypeCount() >= 40);
    }

    @Test
    void dedicatedSpecialFamiliesResolveExecutableEffects() {
        Map<String, TechniqueDataManager.TechniqueEntry> techniques = TechniqueDataManager.builtinTechniques();

        TechniqueDataManager.TechniqueEntry ultimate = techniques.get("xuewu_grand_curse");
        assertNotNull(ultimate);
        assertEquals("ultimate", ultimate.effectType());
        assertTrue(AbstractTechniqueEffectResolver.requiresDedicatedImplementation(ultimate.effectType()));
        assertNotNull(AbstractTechniqueEffectResolver.resolve(ultimate));

        TechniqueDataManager.TechniqueEntry secretArt = techniques.get("tianmo_demon_body_secret");
        assertNotNull(secretArt);
        assertEquals("secret_art", secretArt.effectType());
        assertTrue(AbstractTechniqueEffectResolver.requiresDedicatedImplementation(secretArt.effectType()));
        assertNotNull(AbstractTechniqueEffectResolver.resolve(secretArt));

        TechniqueDataManager.TechniqueEntry wall = techniques.get("qingyan_earth_spike_wall");
        assertNotNull(wall);
        assertEquals("wall", wall.effectType());
        assertTrue(AbstractTechniqueEffectResolver.requiresDedicatedImplementation(wall.effectType()));
        assertNotNull(AbstractTechniqueEffectResolver.resolve(wall));

        TechniqueDataManager.TechniqueEntry buffZone = techniques.get("spirit_gather_array");
        assertNotNull(buffZone);
        assertEquals("buff_zone", buffZone.effectType());
        assertTrue(AbstractTechniqueEffectResolver.requiresDedicatedImplementation(buffZone.effectType()));
        assertNotNull(AbstractTechniqueEffectResolver.resolve(buffZone));

        TechniqueDataManager.TechniqueEntry command = techniques.get("qingyuan_bamboo_cloud_drive");
        assertNotNull(command);
        assertEquals("command", command.effectType());
        assertTrue(AbstractTechniqueEffectResolver.requiresDedicatedImplementation(command.effectType()));
        assertNotNull(AbstractTechniqueEffectResolver.resolve(command));

        TechniqueDataManager.TechniqueEntry craftGate = techniques.get("beast_soul_puppet_bind");
        assertNotNull(craftGate);
        assertEquals("craft_gate", craftGate.effectType());
        assertTrue(AbstractTechniqueEffectResolver.requiresDedicatedImplementation(craftGate.effectType()));
        assertNotNull(AbstractTechniqueEffectResolver.resolve(craftGate));

        TechniqueDataManager.TechniqueEntry mappedTalisman = techniques.get("cast_fire_burst_talisman");
        assertNotNull(mappedTalisman);
        assertEquals("talisman_consume", mappedTalisman.effectType());
        assertTrue(AbstractTechniqueEffectResolver.requiresDedicatedImplementation(
                mappedTalisman.effectType()));
        // SkillType registry mapping remains available for this corpus id.
        assertNotNull(AbstractTechniqueEffectResolver.resolveSkillType(mappedTalisman));
        assertNotNull(AbstractTechniqueEffectResolver.resolve(mappedTalisman));

        // Unknown high-risk / unregistered abstract type still fails closed.
        assertFalse(AbstractTechniqueEffectResolver.isAbstractTypeRegistered("unknown_effect"));
        assertFalse(AbstractTechniqueEffectResolver.isGenericRuntimeType("unknown_effect"));
        assertFalse(AbstractTechniqueEffectResolver.requiresDedicatedImplementation("unknown_effect"));
        assertNull(AbstractTechniqueEffectResolver.resolve(null));
    }

    @Test
    void methodsSkillTreesAndConflictMatrixAreWired() {
        assertEquals(136, ManualCatalogServiceBridge.methodCount());
        assertEquals(90, SkillTreeCatalogService.treeCount());
        assertTrue(ManualConflictMatrixService.pairCount() > 0,
                "conflict matrix should resolve at least some display-name pairs");
        assertTrue(MethodLayerTechniqueService.methodCount() >= 100,
                "layer matrix should cover most methods");
        assertFalse(MethodLayerTechniqueService.techniquesForLayer("changchun_gong", 1).isEmpty());
        assertEquals(13, MethodLayerTechniqueService.maxLayers("changchun_gong"));
        assertEquals(13, MethodLayerTechniqueService.maxLayers("qingyuan_sword_art"));
        assertEquals(1, MethodLayerTechniqueService.maxLayers("huangfeng_alchemy_scripture"));
        assertEquals("1-3层启蒙", MethodLayerTechniqueService.layerNameForLayer("changchun_gong", 3));
        assertEquals("4-6层", MethodLayerTechniqueService.layerNameForLayer("changchun_gong", 4));
        assertEquals(MethodLayerTechniqueService.techniquesForLayer("changchun_gong", 1),
                MethodLayerTechniqueService.techniquesForLayer("changchun_gong", 3));
        assertTrue(MethodLayerTechniqueService.techniquesForLayer("changchun_gong", 4).size()
                > MethodLayerTechniqueService.techniquesForLayer("changchun_gong", 3).size());
        assertEquals("FOUNDATION",
                MethodLayerTechniqueService.requiredRealmForLayer("qingyuan_sword_art", 6));
        assertEquals("CORE_FORMATION",
                MethodLayerTechniqueService.requiredRealmForLayer("qingyuan_sword_art", 7));
        assertEquals("NASCENT_SOUL",
                MethodLayerTechniqueService.requiredRealmForLayer("qingyuan_sword_art", 9));
        assertEquals("DEITY_TRANSFORMATION",
                MethodLayerTechniqueService.requiredRealmForLayer("qingyuan_sword_art", 13));
    }

    /** Avoid direct ManualCatalogService static init surprises in pure unit tests. */
    private static final class ManualCatalogServiceBridge {
        private static int methodCount() {
            return com.xunxian.seekingimmortals.catalog.ManualCatalogService.methodCount();
        }
    }
}
