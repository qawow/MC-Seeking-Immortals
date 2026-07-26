package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import com.xunxian.seekingimmortals.skill.effect.spell.AuthoredSpellEffect;
import com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.SpellEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

        // When the effect can be materialised (full game or bootstrap), SkillType hardcodes must
        // not beat corpus damage_base (FireballSpell 6.0 vs authored 12.0). Pure JVM may return
        // null because ElementalProjectileSpell loads entity classes that need bootstrap.
        SkillEffect resolvedFireball = AbstractTechniqueEffectResolver.resolve(techniques.get("fireball"));
        if (resolvedFireball != null) {
            SpellEffect fireballSpell = assertInstanceOf(SpellEffect.class, resolvedFireball);
            assertEquals(12.0D, fireballSpell.getBaseDamage(), 0.001D,
                    "authored fireball damage_base must win over SkillType library hardcode");
        }

        TechniqueDataManager.TechniqueEntry talisman = techniques.get("cast_fire_burst_talisman");
        SkillEffect resolvedTalisman = AbstractTechniqueEffectResolver.resolve(talisman);
        if (resolvedTalisman instanceof SpellEffect spell) {
            assertEquals(18.0D, spell.getBaseDamage(), 0.001D);
        }

        var scan = AbstractTechniqueEffectResolver.runtimeSpec(techniques.get("divine_sense_scan"));
        assertEquals("scan", scan.type());
        assertEquals(32.0D, scan.range(), 0.001D);
        assertEquals("area", scan.target());
        assertEquals(0.0D, scan.damage(), 0.001D);
        assertTrue(AbstractTechniqueEffectResolver.isGenericRuntimeType(scan.type()));
        assertTrue(AbstractTechniqueEffectResolver.registeredAbstractTypeCount() >= 40);
    }

    @Test
    void genericRegistryAliasesCannotOverrideCorpusSemantics() {
        Map<String, TechniqueDataManager.TechniqueEntry> techniques = TechniqueDataManager.builtinTechniques();

        SkillEffect command = AbstractTechniqueEffectResolver.resolve(required(techniques, "beast_tame_bond"));
        assertInstanceOf(AuthoredSpellEffect.class, command,
                "command corpus entry must use the authored data-driven executor");
        var commandSpec = AbstractTechniqueEffectResolver.runtimeSpec(required(techniques, "beast_tame_bond"));
        assertEquals("command", commandSpec.type());
        assertTrue(commandSpec.tags().contains("beast"));

        SkillEffect wall = AbstractTechniqueEffectResolver.resolve(required(techniques, "tianfu_paper_shield_wall"));
        assertInstanceOf(AuthoredSpellEffect.class, wall,
                "wall corpus entry must use the authored data-driven executor");
        var wallSpec = AbstractTechniqueEffectResolver.runtimeSpec(required(techniques, "tianfu_paper_shield_wall"));
        assertEquals("wall", wallSpec.type());
        assertTrue(wallSpec.tags().contains("defense"));
        assertTrue(wallSpec.tags().contains("talisman"));

        Map<String, String> authoredTypes = Map.of(
                "jingzhe_partial_change", "transform",
                "kunwu_absolute_zero_guard", "buff_self",
                "time_haste_self", "buff_self",
                "wood_spirit_shield_basic", "buff_self",
                "wuxing_water_mirror", "buff_self");
        for (Map.Entry<String, String> authored : authoredTypes.entrySet()) {
            TechniqueDataManager.TechniqueEntry entry = required(techniques, authored.getKey());
            var spec = AbstractTechniqueEffectResolver.runtimeSpec(entry);
            assertEquals(authored.getValue(), spec.type(), authored.getKey());
            SkillEffect resolved = AbstractTechniqueEffectResolver.resolve(entry);
            assertFalse(resolved instanceof ElementalProjectileSpell,
                    authored.getKey() + " type=" + entry.effectType()
                            + " must fail closed instead of becoming a projectile");
            if (resolved != null) {
                assertInstanceOf(AuthoredSpellEffect.class, resolved,
                        authored.getKey() + " must execute through its authored plan");
            }
        }

        TechniqueDataManager.TechniqueEntry beam = required(techniques, "wuxing_metal_edge");
        assertEquals("beam", beam.effectType());
        assertInstanceOf(AuthoredSpellEffect.class, AbstractTechniqueEffectResolver.resolve(beam),
                "authored beam must use the data-driven server executor");

        TechniqueDataManager.TechniqueEntry cone = required(techniques, "luoyun_spirit_flame_combat");
        assertEquals("cone", cone.effectType());
        assertInstanceOf(AuthoredSpellEffect.class, AbstractTechniqueEffectResolver.resolve(cone),
                "authored cone must use the data-driven server executor");

        TechniqueDataManager.TechniqueEntry buff = required(techniques, "inverse_star_veil_trace");
        assertEquals("buff_self", buff.effectType());
        assertInstanceOf(AuthoredSpellEffect.class, AbstractTechniqueEffectResolver.resolve(buff),
                "authored buff must use the data-driven server executor");
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
    void cultOnlyHighLevelTypesMapToExecutableRuntimeEffects() {
        Map<String, TechniqueDataManager.TechniqueEntry> techniques = TechniqueDataManager.builtinTechniques();

        // Cultivation-pack-only ids (not overwritten by text_material) used to load with blank
        // effectType and fail closed at release. They must now map into abstract runtime types.
        TechniqueDataManager.TechniqueEntry seal = techniques.get("spirit_sealing_great_art");
        assertNotNull(seal);
        assertEquals("seal", seal.tier());
        assertEquals("control", seal.effectType());
        assertTrue(AbstractTechniqueEffectResolver.isAbstractTypeRegistered(seal.effectType()));

        TechniqueDataManager.TechniqueEntry detection = techniques.get("heavenly_eye_art");
        assertNotNull(detection);
        assertEquals("scan", detection.effectType());
        assertTrue(AbstractTechniqueEffectResolver.isGenericRuntimeType(detection.effectType()));

        TechniqueDataManager.TechniqueEntry armor = techniques.get("frost_armor");
        if (armor != null && "defense".equals(armor.tier())) {
            assertEquals("shield", armor.effectType());
        }

        TechniqueDataManager.TechniqueEntry spark = techniques.get("spark_art");
        assertNotNull(spark);
        assertEquals("projectile", spark.effectType());
        assertTrue(spark.damageBase() > 0.0D);

        TechniqueDataManager.TechniqueEntry ghost = techniques.get("soul_condensing_art");
        assertNotNull(ghost);
        assertTrue(Set.of("soul_attack", "debuff", "buff_self").contains(ghost.effectType()),
                ghost.effectType());

        TechniqueDataManager.TechniqueEntry forbidden = techniques.get("mind_control_art");
        assertNotNull(forbidden);
        assertEquals("debuff", forbidden.effectType());

        // Pure mapper unit checks — keep independent of load order.
        assertEquals("shield", TechniqueDataManager.inferEffectType("defense", "frost_armor", "寒冰护体", "冰"));
        assertEquals("scan", TechniqueDataManager.inferEffectType("detection", "aura_detection_art", "灵气探测", "通用"));
        assertEquals("movement", TechniqueDataManager.inferEffectType("movement", "wind_riding_formula", "御风诀", "风"));
        assertEquals("summon", TechniqueDataManager.inferEffectType("puppet_art", "puppet_mechanism_art", "傀儡机关术", "傀儡"));
        assertEquals("field", TechniqueDataManager.inferEffectType("sword_formation", "great_geng_sword_formation_level_10_13", "大庚剑阵", "剑法"));
        assertEquals("buff_self", TechniqueDataManager.inferEffectType("cultivation", "qi_guiding_art", "引气入体", "通用"));
        assertEquals("fire", TechniqueDataManager.inferElement("火", "spark_art", "火花术"));

        int blankEffect = 0;
        int resolvableRuntime = 0;
        for (TechniqueDataManager.TechniqueEntry entry : techniques.values()) {
            if (entry.effectType() == null || entry.effectType().isBlank()) {
                blankEffect++;
            } else if (AbstractTechniqueEffectResolver.isAbstractTypeRegistered(entry.effectType())
                    || AbstractTechniqueEffectResolver.isGenericRuntimeType(entry.effectType())
                    || AbstractTechniqueEffectResolver.requiresDedicatedImplementation(entry.effectType())) {
                resolvableRuntime++;
            }
        }
        assertEquals(0, blankEffect, "no builtin technique should keep a blank effectType");
        assertTrue(resolvableRuntime >= 747, "resolvableRuntime=" + resolvableRuntime);
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
        assertEquals(5, MethodLayerTechniqueService.maxLayers("lieyan_gong"));
        assertEquals("QI_REFINING", MethodLayerTechniqueService.requiredRealmForLayer("lieyan_gong", 2));
        assertEquals("FOUNDATION", MethodLayerTechniqueService.requiredRealmForLayer("lieyan_gong", 3));
        assertEquals("NASCENT_SOUL", MethodLayerTechniqueService.requiredRealmForLayer("lieyan_gong", 5));
        assertEquals("真火大成", MethodLayerTechniqueService.layerNameForLayer("lieyan_gong", 5));
        assertTrue(MethodLayerTechniqueService.techniquesForLayer("lieyan_gong", 1).contains("spark_art"));
        assertFalse(MethodLayerTechniqueService.techniquesForLayer("lieyan_gong", 1)
                .contains("lieyan_true_fire_secret"));
        assertTrue(MethodLayerTechniqueService.techniquesForLayer("lieyan_gong", 5)
                .containsAll(List.of("spark_art", "fire_bullet_art", "flame_ring", "fire_talisman",
                        "fire_rain", "elemental_burst_fire", "fire_escape", "lieyan_true_fire_secret")));
    }

    /** Avoid direct ManualCatalogService static init surprises in pure unit tests. */
    private static final class ManualCatalogServiceBridge {
        private static int methodCount() {
            return com.xunxian.seekingimmortals.catalog.ManualCatalogService.methodCount();
        }
    }

    private static TechniqueDataManager.TechniqueEntry required(
            Map<String, TechniqueDataManager.TechniqueEntry> techniques, String id) {
        TechniqueDataManager.TechniqueEntry entry = techniques.get(id);
        assertNotNull(entry, id);
        return entry;
    }
}
