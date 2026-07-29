package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailedQuestCultivationRouteTest {
    @Test
    void typedCultivationEventsHaveNoClientSuppliedQuestCoordinates() {
        DetailedQuestProofEvent method = DetailedQuestProofEvent.methodLayerReached(
                "CHANGCHUN_GONG", 1);
        assertEquals(DetailedQuestProofEvent.Type.METHOD_LAYER_REACHED, method.type());
        assertEquals("cultivation", method.producer());
        assertEquals("changchun_gong", method.parameter("method"));
        assertEquals(1, method.observedLayer());
        assertEquals(DetailedQuestProofEvent.Source.NATURAL, method.source());

        DetailedQuestProofEvent realm = DetailedQuestProofEvent.realmReached(Realm.UNITY);
        assertEquals("body_integration", realm.parameter("realm"));
        assertFalse(realm.parameters().containsKey("chain_id"));
        assertFalse(realm.parameters().containsKey("step"));
    }

    @Test
    void cultivationRoutesResolveAgainstThePublishedCatalog() {
        DetailedQuestProofCatalog.Snapshot catalog = DetailedQuestRuntimeService.proofCatalog();
        for (DetailedQuestProofCatalog.Route route : catalog.routes()) {
            if ("METHOD_LAYER_REACHED".equals(route.proofType())) {
                assertTrue(route.minimumLayer() >= 1, route.eventId());
                assertTrue(com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService.builtin()
                        .findMethod(route.parameter("method")).isPresent(), route.eventId());
            }
            if ("REALM_REACHED".equals(route.proofType())) {
                assertTrue(Realm.fromDesignId(route.parameter("realm")) != null, route.eventId());
            }
        }
    }

    @Test
    void proofLedgerAndHistoryCloneAsIndependentDurableRoots() {
        CompoundTag source = new CompoundTag();
        CompoundTag ledger = new CompoundTag();
        ledger.putBoolean("realm_reached:test", true);
        CompoundTag history = new CompoundTag();
        history.putBoolean("realm_reached:test", true);
        source.put(DetailedQuestProofService.LEDGER_TAG, ledger);
        source.put(DetailedQuestProofService.HISTORY_TAG, history);
        CompoundTag target = new CompoundTag();

        DetailedQuestProofService.copyPersistentData(source, target);

        assertTrue(target.getCompound(DetailedQuestProofService.LEDGER_TAG)
                .getBoolean("realm_reached:test"));
        assertTrue(target.getCompound(DetailedQuestProofService.HISTORY_TAG)
                .getBoolean("realm_reached:test"));
        assertNotSame(source.get(DetailedQuestProofService.LEDGER_TAG),
                target.get(DetailedQuestProofService.LEDGER_TAG));
    }

    @Test
    void authoritativeProducerWiringAndAdminBoundaryAreExplicit() throws Exception {
        String manual = Files.readString(Path.of("src/main/java/com/xunxian/seekingimmortals/catalog/ManualCatalogService.java"));
        String breakthrough = Files.readString(Path.of("src/main/java/com/xunxian/seekingimmortals/cultivation/BreakthroughService.java"));
        String tribulation = Files.readString(Path.of("src/main/java/com/xunxian/seekingimmortals/cultivation/TribulationService.java"));
        String command = Files.readString(Path.of("src/main/java/com/xunxian/seekingimmortals/command/SeekingImmortalsCommand.java"));
        assertTrue(manual.contains("DetailedQuestProofService.recordMethodLayerReached(player, method.id())"));
        assertTrue(breakthrough.contains("DetailedQuestProofService.recordRealmReached(player, result.newRealm())"));
        assertTrue(tribulation.contains("DetailedQuestProofService.recordRealmReached(player, targetRealm)"));
        assertTrue(command.contains("DetailedQuestProofService.adminProve"));
        assertFalse(command.contains("String evidence = \"quest_step_\""));
    }
}
