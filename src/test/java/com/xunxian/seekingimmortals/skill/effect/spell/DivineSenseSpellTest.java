package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.combat.status.StatusRegistryTestSupport;
import com.xunxian.seekingimmortals.cultivation.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DivineSenseSpellTest {

    @Test
    void concealedRealmReadDoesNotCarryTheRealRealm() {
        DivineSenseSpell.RealmReadResult result = DivineSenseSpell.realmReadResult(
                StatusRegistryTestSupport.hidesRealm("conceal_qi"), Realm.CORE_FORMATION);

        assertEquals("message.seeking_immortals.spell.realm_read.hidden", result.messageKey());
        assertTrue(result.args().isEmpty());
        assertFalse(result.args().contains(Realm.CORE_FORMATION.getDisplayName()));
    }

    @Test
    void visibleRealmReadReturnsTheRealRealm() {
        DivineSenseSpell.RealmReadResult result = DivineSenseSpell.realmReadResult(
                StatusRegistryTestSupport.hidesRealm(), Realm.CORE_FORMATION);

        assertEquals("message.seeking_immortals.spell.realm_read.visible", result.messageKey());
        assertEquals(List.of(Realm.CORE_FORMATION.getDisplayName()), result.args());
    }

    @Test
    void missingCultivationCapabilityDoesNotFallBackToMortal() {
        DivineSenseSpell.RealmReadResult result = DivineSenseSpell.realmReadResult(false, null);

        assertEquals("message.seeking_immortals.spell.realm_read.unknown", result.messageKey());
        assertTrue(result.args().isEmpty());
    }
}
