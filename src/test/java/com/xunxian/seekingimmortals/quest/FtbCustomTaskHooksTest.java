package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbCustomTaskHooksTest {
    @Test
    void parseWarActiveTag() {
        FtbCustomTaskHooks.Spec spec = FtbCustomTaskHooks.parseTag("si_war_active");
        assertInstanceOf(FtbCustomTaskHooks.Spec.WarActive.class, spec);
    }

    @Test
    void parseReputationTag() {
        FtbCustomTaskHooks.Spec spec = FtbCustomTaskHooks.parseTag("si_rep_mulan_10");
        assertInstanceOf(FtbCustomTaskHooks.Spec.ReputationGate.class, spec);
        FtbCustomTaskHooks.Spec.ReputationGate gate = (FtbCustomTaskHooks.Spec.ReputationGate) spec;
        assertEquals("mulan", gate.faction());
        assertEquals(10, gate.min());
    }

    @Test
    void parseReputationTagWithUnderscoreFaction() {
        FtbCustomTaskHooks.Spec spec = FtbCustomTaskHooks.parseTag("si_rep_chaotic_sea_25");
        assertInstanceOf(FtbCustomTaskHooks.Spec.ReputationGate.class, spec);
        FtbCustomTaskHooks.Spec.ReputationGate gate = (FtbCustomTaskHooks.Spec.ReputationGate) spec;
        assertEquals("chaotic_sea", gate.faction());
        assertEquals(25, gate.min());
    }

    @Test
    void parseNativeQuestStageTag() {
        FtbCustomTaskHooks.Spec spec = FtbCustomTaskHooks.parseTag(
                "si_native_huangfeng_cultivation_path_5");
        assertInstanceOf(FtbCustomTaskHooks.Spec.NativeStage.class, spec);
        FtbCustomTaskHooks.Spec.NativeStage nativeStage = (FtbCustomTaskHooks.Spec.NativeStage) spec;
        assertEquals("huangfeng_cultivation_path", nativeStage.chainId());
        assertEquals(5, nativeStage.stage());
    }

    @Test
    void malformedOrWriteOnlyNativeTagFailsClosedAsCustomTask() {
        assertInstanceOf(FtbCustomTaskHooks.Spec.Unknown.class,
                FtbCustomTaskHooks.parseTag("si_native_huangfeng_cultivation_path_99"));
        assertInstanceOf(FtbCustomTaskHooks.Spec.Unknown.class,
                FtbCustomTaskHooks.parseTag("si_native_write_huangfeng_cultivation_path_1"));
    }

    @Test
    void unknownSiTagFailsClosed() {
        FtbCustomTaskHooks.Spec spec = FtbCustomTaskHooks.parseTag("si_not_a_real_rule");
        assertInstanceOf(FtbCustomTaskHooks.Spec.Unknown.class, spec);
        assertFalse(spec.matches(null));
    }

    @Test
    void ordinaryFtbTagsAreIgnored() {
        assertEquals(null, FtbCustomTaskHooks.parseTag("seeking_immortals"));
        assertEquals(null, FtbCustomTaskHooks.parseTag("mulan_tianlan_war"));
    }

    @Test
    void specsOfCollectsOnlySiRules() {
        List<FtbCustomTaskHooks.Spec> specs = FtbCustomTaskHooks.specsOf(Set.of(
                "seeking_immortals",
                "si_war_active",
                "si_rep_dajin_10",
                "si_native_qixuan_mortal_path_4",
                "optional"
        ));
        assertEquals(3, specs.size());
        assertTrue(specs.stream().anyMatch(s -> s instanceof FtbCustomTaskHooks.Spec.WarActive));
        assertTrue(specs.stream().anyMatch(s -> s instanceof FtbCustomTaskHooks.Spec.ReputationGate));
        assertTrue(specs.stream().anyMatch(s -> s instanceof FtbCustomTaskHooks.Spec.NativeStage));
    }
}
