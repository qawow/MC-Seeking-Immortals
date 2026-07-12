package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncSectDataPacket;

import java.util.List;

public final class ClientSectData {
    private static Snapshot snapshot = Snapshot.empty();

    private ClientSectData() {}

    public static void set(SyncSectDataPacket packet) {
        snapshot = new Snapshot(
                packet.sectId(),
                packet.sectDisplay(),
                packet.currentSectDisplay(),
                packet.role(),
                packet.contribution(),
                packet.yueArrived(),
                packet.sevenMysteriesComplete(),
                packet.member(),
                packet.canJoin(),
                packet.stage(),
                packet.stageKey(),
                packet.objectiveKey(),
                packet.candidates().stream()
                        .map(candidate -> new Candidate(
                                candidate.id(),
                                candidate.displayZh(),
                                candidate.displayEn(),
                                candidate.focusKey(),
                                candidate.structureId(),
                                candidate.canApply()))
                        .toList(),
                new DialogueNode(
                        packet.dialogue().id(),
                        packet.dialogue().titleKey(),
                        packet.dialogue().textKey(),
                        packet.dialogue().options().stream()
                                .map(option -> new DialogueOption(option.id(), option.labelKey(), option.action()))
                                .toList()),
                new Mission(
                        packet.mission().id(),
                        packet.mission().titleKey(),
                        packet.mission().objectiveKey(),
                        packet.mission().itemDescriptionId(),
                        packet.mission().target(),
                        packet.mission().rewardContribution(),
                        packet.mission().accepted(),
                        packet.mission().completed(),
                        packet.mission().canTurnIn()),
                packet.shopEntries().stream()
                        .map(entry -> new ShopEntry(entry.id(), entry.itemDescriptionId(), entry.count(), entry.cost(), entry.currency()))
                        .toList(),
                true);
    }

    public static void reset() {
        snapshot = Snapshot.empty();
    }

    public static Snapshot get() {
        return snapshot;
    }

    public record Snapshot(String sectId, String sectDisplay, String currentSectDisplay, String role,
                           int contribution, boolean yueArrived, boolean sevenMysteriesComplete,
                           boolean member, boolean canJoin, int stage, String stageKey, String objectiveKey,
                           List<Candidate> candidates, DialogueNode dialogue, Mission mission,
                           List<ShopEntry> shopEntries, boolean synced) {
        private static Snapshot empty() {
            return new Snapshot("", "", "-", "", 0, false, false, false, false, 0,
                    "screen.seeking_immortals.sect.stage.locked",
                    "screen.seeking_immortals.sect.objective.waiting",
                    List.of(),
                    DialogueNode.empty(),
                    Mission.empty(),
                    List.of(),
                    false);
        }
    }

    public record Candidate(String id, String displayZh, String displayEn, String focusKey,
                            String structureId, boolean canApply) {}

    public record DialogueNode(String id, String titleKey, String textKey, List<DialogueOption> options) {
        public static DialogueNode empty() {
            return new DialogueNode("", "", "", List.of());
        }
    }

    public record DialogueOption(String id, String labelKey, String action) {}

    public record Mission(String id, String titleKey, String objectiveKey, String itemDescriptionId,
                          int target, int rewardContribution, boolean accepted, boolean completed,
                          boolean canTurnIn) {
        public static Mission empty() {
            return new Mission("", "", "", "", 0, 0, false, false, false);
        }

        public boolean available() {
            return !id.isBlank();
        }
    }

    public record ShopEntry(String id, String itemDescriptionId, int count, int cost, String currency) {}
}
