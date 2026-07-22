package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * M11: Han Li timeline + chronicle record service (display UI owned by M16).
 * Records unlocked timeline phases / chronicle events on player NBT for later UI.
 */
public final class TimelineChronicleService {
    private static final String TIMELINE_TAG = "seeking_immortals_hanli_timeline";
    private static final Snapshot BUILTIN = loadBuiltin();

    private TimelineChronicleService() {}

    /** Preserve timeline unlocks across death/clone. */
    public static void copyPersistentData(CompoundTag originalData, CompoundTag clonedData) {
        if (originalData == null || clonedData == null || !originalData.contains(TIMELINE_TAG)) {
            return;
        }
        if (originalData.get(TIMELINE_TAG) != null) {
            clonedData.put(TIMELINE_TAG, originalData.get(TIMELINE_TAG).copy());
        }
    }

    public record TimelinePhase(String phase, String realm, int nodeCount) {}

    public record Snapshot(List<TimelinePhase> phases, int chronicleCount, List<String> mainlineOrder) {
        public int phaseCount() {
            return phases.size();
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static int phaseCount() {
        return BUILTIN.phaseCount();
    }

    public static int chronicleCount() {
        return Math.max(BUILTIN.chronicleCount(), FactionQuestCatalogService.builtin().chronicleEvents().size());
    }

    public static List<String> sampleTimeline(int limit) {
        List<String> out = new ArrayList<>();
        int i = 0;
        for (TimelinePhase phase : BUILTIN.phases()) {
            out.add(phase.phase() + " | realm=" + phase.realm() + " nodes=" + phase.nodeCount());
            if (++i >= Math.max(1, limit)) {
                break;
            }
        }
        return out;
    }

    public static boolean unlockPhase(ServerPlayer player, String phaseId) {
        if (player == null) {
            return false;
        }
        String id = normalize(phaseId);
        if (id.isBlank()) {
            return false;
        }
        Optional<TimelinePhase> phase = BUILTIN.phases().stream()
                .filter(p -> normalize(p.phase()).equals(id) || normalize(p.phase()).contains(id))
                .findFirst();
        if (phase.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.timeline.unknown",
                    Component.translatable("text.seeking_immortals.unknown_phase")), false);
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(TIMELINE_TAG).copy();
        String key = normalize(phase.get().phase());
        if (root.getBoolean(key)) {
            return false;
        }
        root.putBoolean(key, true);
        player.getPersistentData().put(TIMELINE_TAG, root);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.timeline.unlocked",
                PlayerDisplayText.safeLiteral(phase.get().phase(),
                        "text.seeking_immortals.unknown_phase")), true);
        return true;
    }

    public static boolean hasPhase(ServerPlayer player, String phaseId) {
        if (player == null) {
            return false;
        }
        return player.getPersistentData().getCompound(TIMELINE_TAG).getBoolean(normalize(phaseId));
    }

    public static int unlockedPhaseCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        CompoundTag root = player.getPersistentData().getCompound(TIMELINE_TAG);
        int count = 0;
        for (String key : root.getAllKeys()) {
            if (root.getBoolean(key)) {
                count++;
            }
        }
        return count;
    }

    /** Bridge: discovering a chronicle can unlock matching timeline phase by realm keyword. */
    public static void onChronicleDiscovered(ServerPlayer player, String eventId) {
        if (player == null) {
            return;
        }
        String id = normalize(eventId);
        if (id.contains("mortal") || id.startsWith("m1") || id.contains("qixuan")) {
            unlockPhase(player, "凡人");
        } else if (id.contains("huangfeng") || id.contains("qi_refin") || id.contains("blood")) {
            unlockPhase(player, "炼气");
        } else if (id.contains("foundation") || id.contains("tiannan")) {
            unlockPhase(player, "筑基");
        } else if (id.contains("core") || id.contains("chaotic") || id.contains("star")) {
            unlockPhase(player, "结丹");
        } else if (id.contains("nascent") || id.contains("dajin") || id.contains("void")) {
            unlockPhase(player, "元婴");
        } else if (id.contains("deity") || id.contains("spirit") || id.contains("tianyuan")) {
            unlockPhase(player, "化神");
        }
    }

    private static Snapshot loadBuiltin() {
        List<TimelinePhase> phases = new ArrayList<>();
        JsonObject index = readJson(path("catalog/hanli_timeline_index.json"));
        if (index != null) {
            for (JsonElement element : array(index, "phases")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                phases.add(new TimelinePhase(str(o, "phase"), str(o, "realm"), asInt(o, "node_count")));
            }
        }
        if (phases.isEmpty()) {
            JsonObject full = readJson(path("text_material/hanli_timeline_items_v100.json"));
            for (JsonElement element : array(full, "timeline")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                int nodes = o.has("nodes") && o.get("nodes").isJsonArray() ? o.getAsJsonArray("nodes").size() : 0;
                phases.add(new TimelinePhase(str(o, "phase"), str(o, "realm"), nodes));
            }
        }
        List<String> mainline = new ArrayList<>();
        JsonObject guide = readJson(path("text_material/timeline_guide_v115.json"));
        if (guide != null) {
            for (JsonElement element : array(guide, "mainline_order")) {
                try {
                    mainline.add(element.getAsString());
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        int chronicle = FactionQuestCatalogService.builtin().chronicleEvents().size();
        if (chronicle <= 0) {
            JsonObject ce = readJson(path("catalog/chronicle_events_index.json"));
            chronicle = array(ce, "entries").size();
        }
        return new Snapshot(List.copyOf(phases), chronicle, List.copyOf(mainline));
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = TimelineChronicleService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }

    private static int asInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
