package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M02 acceptance: published text_material techniques load into TechniqueDataManager,
 * every effect.type resolves, methods/skill trees/conflict matrix are present.
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
    }

    @Test
    void everyLoadedTechniqueResolvesAnEffect() {
        Map<String, TechniqueDataManager.TechniqueEntry> techniques = TechniqueDataManager.builtinTechniques();
        int unresolved = 0;
        Set<String> missingTypes = new HashSet<>();
        for (TechniqueDataManager.TechniqueEntry technique : techniques.values()) {
            SkillEffect effect = AbstractTechniqueEffectResolver.resolve(technique);
            if (effect == null) {
                unresolved++;
                missingTypes.add(technique.effectType().isBlank() ? "<blank>" : technique.effectType());
            }
        }
        assertEquals(0, unresolved, "unresolved effect types: " + missingTypes);
        assertTrue(AbstractTechniqueEffectResolver.registeredAbstractTypeCount() >= 30);
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
