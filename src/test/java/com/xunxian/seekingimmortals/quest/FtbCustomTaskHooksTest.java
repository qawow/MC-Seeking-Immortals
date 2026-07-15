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
                "optional"
        ));
        assertEquals(2, specs.size());
        assertTrue(specs.stream().anyMatch(s -> s instanceof FtbCustomTaskHooks.Spec.WarActive));
        assertTrue(specs.stream().anyMatch(s -> s instanceof FtbCustomTaskHooks.Spec.ReputationGate));
    }
}
