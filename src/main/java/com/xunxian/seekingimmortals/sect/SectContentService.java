package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SectContentService {
    private static final Map<String, DialogueTree> DIALOGUE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, MissionPool> MISSION_CACHE = new ConcurrentHashMap<>();

    private SectContentService() {}

    public static DialogueNode nodeForStage(String sectId, int stage) {
        DialogueTree tree = dialogueTree(sectId);
        DialogueNode node = tree.nodes().get(stageNodeId(stage));
        return node == null ? fallbackNode(stage) : node;
    }

    public static Optional<DialogueOption> optionForStage(String sectId, int stage, String optionId) {
        String normalized = normalize(optionId);
        return nodeForStage(sectId, stage).options().stream()
                .filter(option -> option.id().equals(normalized))
                .findFirst();
    }

    public static MissionDefinition missionForDay(String sectId, int stage, long day) {
        List<MissionDefinition> eligible = missionPool(sectId).missions().stream()
                .filter(mission -> stage >= mission.minStage())
                .toList();
        if (eligible.isEmpty()) {
            return fallbackMission(sectId);
        }
        int index = Math.floorMod((int)day + normalize(sectId).hashCode(), eligible.size());
        return eligible.get(index);
    }

    public static Optional<MissionDefinition> missionById(String sectId, String missionId) {
        String normalized = normalize(missionId);
        return missionPool(sectId).missions().stream()
                .filter(mission -> mission.id().equals(normalized))
                .findFirst();
    }

    public static List<MissionDefinition> missionsForTest(String sectId) {
        return missionPool(sectId).missions();
    }

    public static DialogueTree dialogueForTest(String sectId) {
        return dialogueTree(sectId);
    }

    private static DialogueTree dialogueTree(String sectId) {
        return DIALOGUE_CACHE.computeIfAbsent(normalize(sectId), SectContentService::loadDialogue);
    }

    private static MissionPool missionPool(String sectId) {
        return MISSION_CACHE.computeIfAbsent(normalize(sectId), SectContentService::loadMissions);
    }

    private static DialogueTree loadDialogue(String sectId) {
        String path = "data/" + SeekingImmortalsMod.MODID + "/sects/dialogues/" + sectId + ".json";
        try (InputStream stream = SectContentService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    Map<String, DialogueNode> nodes = new HashMap<>();
                    JsonArray array = root.getAsJsonArray("nodes");
                    if (array != null) {
                        for (JsonElement element : array) {
                            DialogueNode node = parseNode(element.getAsJsonObject());
                            nodes.put(node.id(), node);
                        }
                    }
                    return new DialogueTree(sectId, Map.copyOf(nodes));
                }
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load sect dialogue {}", path, exception);
        }
        return new DialogueTree(sectId, Map.of(
                "knocking", fallbackNode(SectContributionService.STAGE_KNOCKING),
                "outer", fallbackNode(SectContributionService.STAGE_OUTER_DISCIPLE),
                "foundation", fallbackNode(SectContributionService.STAGE_FOUNDATION_DILEMMA),
                "inner", fallbackNode(SectContributionService.STAGE_INNER_DISCIPLE),
                "complete", fallbackNode(SectContributionService.STAGE_PHASE10_COMPLETE)));
    }

    private static DialogueNode parseNode(JsonObject object) {
        String id = normalize(string(object, "id", "outer"));
        String titleKey = string(object, "title_key", "screen.seeking_immortals.sect.dialogue." + id + ".title");
        String textKey = string(object, "text_key", "screen.seeking_immortals.sect.dialogue." + id + ".text");
        List<DialogueOption> options = new ArrayList<>();
        JsonArray array = object.getAsJsonArray("options");
        if (array != null) {
            for (JsonElement element : array) {
                JsonObject option = element.getAsJsonObject();
                options.add(new DialogueOption(
                        normalize(string(option, "id", "")),
                        string(option, "label_key", "screen.seeking_immortals.sect.dialogue.option.continue"),
                        normalize(string(option, "action", ""))));
            }
        }
        return new DialogueNode(id, titleKey, textKey, List.copyOf(options));
    }

    private static MissionPool loadMissions(String sectId) {
        String path = "data/" + SeekingImmortalsMod.MODID + "/sects/missions/" + sectId + ".json";
        try (InputStream stream = SectContentService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    List<MissionDefinition> missions = new ArrayList<>();
                    JsonArray array = root.getAsJsonArray("missions");
                    if (array != null) {
                        for (JsonElement element : array) {
                            missions.add(parseMission(element.getAsJsonObject()));
                        }
                    }
                    return new MissionPool(sectId, List.copyOf(missions));
                }
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load sect missions {}", path, exception);
        }
        return new MissionPool(sectId, List.of(fallbackMission(sectId)));
    }

    private static MissionDefinition parseMission(JsonObject object) {
        String id = normalize(string(object, "id", ""));
        return new MissionDefinition(
                id,
                string(object, "title_key", "screen.seeking_immortals.sect.mission." + id + ".title"),
                string(object, "objective_key", "screen.seeking_immortals.sect.mission." + id + ".objective"),
                normalizeItemId(string(object, "item", "seeking_immortals:spirit_grass")),
                positiveInt(object, "target", 10),
                positiveInt(object, "reward_contribution", 20),
                Math.max(SectContributionService.STAGE_OUTER_DISCIPLE, positiveInt(object, "min_stage", SectContributionService.STAGE_OUTER_DISCIPLE)));
    }

    private static DialogueNode fallbackNode(int stage) {
        String id = stageNodeId(stage);
        return new DialogueNode(
                id,
                "screen.seeking_immortals.sect.dialogue." + id + ".title",
                "screen.seeking_immortals.sect.dialogue." + id + ".text",
                switch (stage) {
                    case SectContributionService.STAGE_KNOCKING -> List.of(new DialogueOption("exam", "screen.seeking_immortals.sect.dialogue.option.exam", "advance"));
                    case SectContributionService.STAGE_OUTER_DISCIPLE -> List.of(new DialogueOption("mission", "screen.seeking_immortals.sect.dialogue.option.mission", "accept_mission"));
                    case SectContributionService.STAGE_FOUNDATION_DILEMMA -> List.of(new DialogueOption("promotion", "screen.seeking_immortals.sect.dialogue.option.promotion", "advance"));
                    case SectContributionService.STAGE_INNER_DISCIPLE -> List.of(new DialogueOption("competition", "screen.seeking_immortals.sect.dialogue.option.competition", "advance"));
                    default -> List.of();
                });
    }

    private static MissionDefinition fallbackMission(String sectId) {
        return new MissionDefinition(
                normalize(sectId) + "_spirit_grass",
                "screen.seeking_immortals.sect.mission.spirit_grass.title",
                "screen.seeking_immortals.sect.mission.spirit_grass.objective",
                "seeking_immortals:spirit_grass",
                10,
                20,
                SectContributionService.STAGE_OUTER_DISCIPLE);
    }

    private static String stageNodeId(int stage) {
        return switch (stage) {
            case SectContributionService.STAGE_KNOCKING -> "knocking";
            case SectContributionService.STAGE_OUTER_DISCIPLE -> "outer";
            case SectContributionService.STAGE_FOUNDATION_DILEMMA -> "foundation";
            case SectContributionService.STAGE_INNER_DISCIPLE -> "inner";
            case SectContributionService.STAGE_PHASE10_COMPLETE -> "complete";
            default -> "locked";
        };
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        int value = object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        return Math.max(1, value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeItemId(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.indexOf(':') >= 0 ? normalized : SeekingImmortalsMod.MODID + ":" + normalized;
    }

    public record DialogueTree(String sectId, Map<String, DialogueNode> nodes) {}

    public record DialogueNode(String id, String titleKey, String textKey, List<DialogueOption> options) {}

    public record DialogueOption(String id, String labelKey, String action) {}

    public record MissionPool(String sectId, List<MissionDefinition> missions) {}

    public record MissionDefinition(String id, String titleKey, String objectiveKey, String itemId,
                                    int target, int rewardContribution, int minStage) {}
}
