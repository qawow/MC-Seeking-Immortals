package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionConflictEventServiceTest {
    @Test
    void loadsConflictEventsFromCorpus() {
        FactionConflictEventService.Snapshot snapshot = FactionConflictEventService.builtin();
        assertTrue(snapshot.count() >= 15, "expected faction_conflict_events volume, got " + snapshot.count());
        assertTrue(FactionConflictEventService.find("huangfeng_yanyue_rivalry").isPresent()
                || snapshot.events().values().stream().anyMatch(e -> e.id().contains("huangfeng")));
    }

    @Test
    void regionQueryReturnsEvents() {
        assertFalse(FactionConflictEventService.eventsForRegion("tiannan").isEmpty()
                || FactionConflictEventService.eventsForRegion("chaotic_sea").isEmpty()
                || FactionConflictEventService.builtin().count() == 0);
        assertTrue(FactionConflictEventService.eventsForRegion("tiannan").size()
                + FactionConflictEventService.eventsForRegion("mulan").size()
                + FactionConflictEventService.eventsForRegion("").size()
                >= 1);
    }

    @Test
    void authoredDailyFactionWarsResolveDeterministically() {
        assertEquals("mulan_tianlan_war_outbreak",
                FactionConflictEventService.authoredConflictId("mulan_scout_clash", "mulan_grassland"));
        assertEquals("mulan_tianlan_war_outbreak",
                FactionConflictEventService.authoredConflictId("mulan_border_patrol", "tiannan"));
        assertEquals("mulan_tianlan_war_outbreak",
                FactionConflictEventService.authoredConflictId("mulan_soul_array_supply", "mulan_grassland"));
        assertEquals("mulan_tianlan_war_outbreak",
                FactionConflictEventService.authoredConflictId("tiannan_war_merit_muster", "tiannan"));
        assertEquals("",
                FactionConflictEventService.authoredConflictId("spirit_rain", "mulan"));

        FactionConflictEventService.ConflictEvent outbreak =
                FactionConflictEventService.find("mulan_tianlan_war_outbreak").orElseThrow();
        assertEquals(List.of("tiannan_alliance", "mulan_fashi"), outbreak.factions());
        assertTrue(SectWarService.factionMatches("huangfeng_valley", outbreak.factions().get(0)));
        assertTrue(SectWarService.factionMatches("yanyue_sect", outbreak.factions().get(0)));
        assertTrue(SectWarService.factionMatches("mulan_fashi_council", outbreak.factions().get(1)));

        FactionConflictEventService.ConflictEvent legacyCandidate = FactionConflictEventService
                .resolveConflict("mulan_grassland", "mulan_tianlan_campaign")
                .orElseThrow();
        assertEquals("mulan_tianlan_war_outbreak", legacyCandidate.id());
        assertEquals(List.of("tiannan_alliance", "mulan_fashi"), legacyCandidate.factions());
        assertTrue(FactionConflictEventService
                .resolveConflict("chaotic_sea", "mulan_tianlan_campaign").isEmpty());
    }

    @Test
    void authoredNonFactionEventsNeverFallThroughToAmbientConflicts() {
        assertTrue(FactionConflictEventService.resolveConflict("tiannan", "spirit_rain").isEmpty());
        assertTrue(FactionConflictEventService.resolveConflict("tiannan", "ceasefire_envoy_rumor").isEmpty());
    }

    @Test
    void directAndPluralRegionDefinitionsAreScoped() {
        assertTrue(FactionConflictEventService.resolveConflict("chaotic_sea", "chaotic_sea_blockade").isPresent());
        assertTrue(FactionConflictEventService.resolveConflict("tiannan", "chaotic_sea_blockade").isEmpty());

        FactionConflictEventService.ConflictEvent multiRegion =
                FactionConflictEventService.find("ancient_demon_seal_breach").orElseThrow();
        assertTrue(multiRegion.matchesRegion("tiannan"));
        assertTrue(multiRegion.matchesRegion("fallen_demon_valley"));
        assertTrue(FactionConflictEventService.eventsForRegion("fallen_demon_valley").stream()
                .anyMatch(event -> event.id().equals("ancient_demon_seal_breach")));

        FactionConflictEventService.ConflictEvent gated =
                FactionConflictEventService.find("feiling_border_skirmish").orElseThrow();
        assertEquals("void_refinement", gated.realmMin());
        assertFalse(DailyEventEffectExecutor.meetsRealmMinimum(Realm.SOUL_TRANSFORMATION, gated.realmMin()));
        assertTrue(DailyEventEffectExecutor.meetsRealmMinimum(Realm.VOID_REFINEMENT, gated.realmMin()));
    }

    @Test
    void authoredWarPhaseIsReadableOnlyWhileActive() {
        CompoundTag root = new CompoundTag();
        root.putString("ActivePhase", "fashi_array_clash");
        root.putLong("ActiveUntil", 200L);

        assertEquals("fashi_array_clash",
                FactionConflictEventService.activePhase(root, 199L).orElseThrow());
        assertTrue(FactionConflictEventService.activePhase(root, 200L).isEmpty());
    }
}
