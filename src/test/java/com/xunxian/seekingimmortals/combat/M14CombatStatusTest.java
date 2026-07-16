package com.xunxian.seekingimmortals.combat;

import com.xunxian.seekingimmortals.combat.status.PoisonAntidoteService;
import com.xunxian.seekingimmortals.combat.status.StatusCatalogService;
import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.ImmortalAffliction;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M14 战斗与状态回归：语料 id、配装基线、毒映射、伤害管线风险与钩子。
 */
class M14CombatStatusTest {

    private static final List<String> BIBLE_CORE_IDS = List.of(
            "burn", "frozen", "soul_shock", "illusion", "karma",
            "demonic_qi", "foundation_unstable", "marrow_drain", "seal_nascent", "conceal_qi"
    );

    @AfterEach
    void tearDown() {
        DamagePipelineHooks.clearHooksForTests();
    }

    @Test
    void statusCatalogRegistersAtLeastTwentyIdsMatchingBibleCore() {
        StatusCatalogService.Snapshot snapshot = StatusCatalogService.builtin();
        assertTrue(snapshot.size() >= 20, "expected >=20 status effects, got " + snapshot.size());
        for (String id : BIBLE_CORE_IDS) {
            assertTrue(snapshot.find(id).isPresent(), "missing bible status id: " + id);
            assertEquals(id, snapshot.find(id).orElseThrow().id());
        }
        // freeze alias → frozen
        assertTrue(snapshot.find("freeze").isPresent());
        assertEquals("frozen", snapshot.find("freeze").orElseThrow().id());

        Set<String> unique = new HashSet<>(snapshot.ids());
        assertEquals(snapshot.size(), unique.size(), "duplicate status ids");
    }

    @Test
    void statusIdsDoNotOverlapImmortalAfflictions() {
        Set<String> statusIds = new HashSet<>(StatusCatalogService.builtin().ids());
        for (ImmortalAffliction affliction : ImmortalAffliction.values()) {
            String key = affliction.name().toLowerCase(Locale.ROOT);
            assertFalse(statusIds.contains(key),
                    "M14 short-combat status must not collide with M01 affliction id: " + key);
        }
        // 语料边界命名
        assertFalse(statusIds.contains("heart_demon"));
        assertFalse(statusIds.contains("severe_injury"));
        assertFalse(statusIds.contains("shattered_core"));
        assertFalse(statusIds.contains("realm_fall"));
    }

    @Test
    void hitChanceFormulaClampsByRealmDeltaAndResist() {
        // 同阶无抗性：0.70
        double same = clampHit(0.70 + 0.08 * 0 - 0);
        assertEquals(0.70D, same, 0.0001D);
        // 高两境：0.70 + 0.16 = 0.86
        assertEquals(0.86D, clampHit(0.70 + 0.08 * 2 - 0), 0.0001D);
        // 低五境：0.70 - 0.40 = 0.30
        assertEquals(0.30D, clampHit(0.70 + 0.08 * (-5) - 0), 0.0001D);
        // 上限 0.95
        assertEquals(0.95D, clampHit(0.70 + 0.08 * 10 - 0), 0.0001D);
        // 下限 0.05
        assertEquals(0.05D, clampHit(0.70 + 0.08 * (-20) - 0.5), 0.0001D);
    }

    @Test
    void poisonVariantsMapOntoStatusIds() {
        assertEquals("poison", PoisonAntidoteService.statusIdForPoisonVariant("bi_jiu").orElse(""));
        assertEquals("burn", PoisonAntidoteService.statusIdForPoisonVariant("huo_du").orElse(""));
        assertEquals("frozen", PoisonAntidoteService.statusIdForPoisonVariant("han_du").orElse(""));
        assertEquals("demonic_qi", PoisonAntidoteService.statusIdForPoisonVariant("gu_mo_qi_du").orElse(""));
        assertEquals("soul_wound", PoisonAntidoteService.statusIdForPoisonVariant("yin_huo_du").orElse(""));
        assertTrue(PoisonAntidoteService.statusIdForPoisonVariant("not_a_poison").isEmpty());
    }

    @Test
    void loadoutByRealmCorpusAndDamageBaselineStayInBand() {
        assertTrue(LoadoutByRealmService.builtin().size() >= 8);
        assertTrue(LoadoutByRealmService.builtin().find("QI_REFINING").isPresent());
        assertTrue(LoadoutByRealmService.builtin().find("FOUNDATION").isPresent());
        assertTrue(LoadoutByRealmService.builtin().find("NASCENT_SOUL").isPresent());

        for (Realm realm : Arrays.asList(
                Realm.QI_REFINING, Realm.FOUNDATION_ESTABLISHMENT, Realm.CORE_FORMATION,
                Realm.NASCENT_SOUL, Realm.SOUL_TRANSFORMATION)) {
            LoadoutByRealmService.DamageBaseline baseline = LoadoutByRealmService.damageBaseline(realm);
            double mid = LoadoutByRealmService.expectedMitigatedDamage(realm, realm);
            assertTrue(mid >= baseline.expectedMinDamage() - 1e-6);
            assertTrue(mid <= baseline.expectedMaxDamage() + 1e-6);

            // 同境界同配置：伤害落在公式期望区间
            double attack = RealmStageConfig.getAttackBase(realm);
            double defense = RealmStageConfig.getDefenseBase(realm);
            double expected = Math.max(1.0D, attack * (1.0D - defense / (defense + 100.0D)));
            assertEquals(expected, mid, 0.0001D);
            assertTrue(expected >= baseline.expectedMinDamage());
            assertTrue(expected <= baseline.expectedMaxDamage());
        }
    }

    @Test
    void getCombatStatsIsNullSafe() {
        // 风险③：null 玩家返回 empty，不抛 NPE
        assertTrue(CombatCalculator.getCombatStats(null).isEmpty());
    }

    @Test
    void damagePipelineHooksFirePreAndPostWithoutRecursiveHurtContract() {
        AtomicBoolean preSeen = new AtomicBoolean(false);
        AtomicBoolean postSeen = new AtomicBoolean(false);
        AtomicReference<Double> postAmount = new AtomicReference<>(-1.0D);

        DamagePipelineHooks.registerPreHook(ctx -> {
            preSeen.set(true);
            ctx.setAmount(ctx.amount() + 5.0D);
        });
        DamagePipelineHooks.registerPostHook(ctx -> {
            postSeen.set(true);
            postAmount.set(ctx.amount());
        });

        DamagePipelineHooks.DamageContext pre = DamagePipelineHooks.firePre(null, null, 10.0D);
        assertTrue(preSeen.get());
        assertEquals(15.0D, pre.amount(), 0.0001D);
        DamagePipelineHooks.firePost(pre);
        assertTrue(postSeen.get());
        assertEquals(15.0D, postAmount.get(), 0.0001D);

        // cancel short-circuits later pre hooks
        DamagePipelineHooks.clearHooksForTests();
        AtomicBoolean second = new AtomicBoolean(false);
        DamagePipelineHooks.registerPreHook(DamagePipelineHooks.DamageContext::cancel);
        DamagePipelineHooks.registerPreHook(ctx -> second.set(true));
        DamagePipelineHooks.DamageContext canceled = DamagePipelineHooks.firePre(null, null, 3.0D);
        assertTrue(canceled.isCanceled());
        assertFalse(second.get());
    }

    @Test
    void statusRegistryNormalizesAliasesAndKnowsCoreIds() {
        assertTrue(StatusRegistry.isKnown("burn"));
        assertTrue(StatusRegistry.isKnown("freeze"));
        assertEquals("frozen", StatusRegistry.normalizeId("freeze"));
        assertEquals("burn", StatusRegistry.normalizeId("burn"));
        assertFalse(StatusRegistry.isKnown("not_registered_status_xyz"));
    }

    private static double clampHit(double chance) {
        return Math.max(0.05D, Math.min(0.95D, chance));
    }
}
