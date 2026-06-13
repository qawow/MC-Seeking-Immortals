package com.xunxian.seekingimmortals.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClientTechniqueData {
    public static final int SLOT_COUNT = 7;
    private static final List<String> BUILTIN_FILES = List.of(
            "qi_refining_techniques.json",
            "foundation_establishment_techniques.json",
            "core_formation_techniques.json",
            "nascent_soul_techniques.json",
            "spirit_transformation_plus_techniques.json",
            "special_common_techniques.json");
    private static final Map<String, TechniqueSummary> BUILTIN_SUMMARIES = loadBuiltInSummaries();
    private static List<String> learnedTechniques = List.of();
    private static List<String> techniqueSlots = emptySlots();
    private static Map<String, Integer> cooldownUntilClientTicks = Map.of();
    private static boolean synced;

    private ClientTechniqueData() {}

    public static void setLearnedTechniques(List<String> techniques) {
        setTechniqueData(techniques, defaultSlots(techniques), Map.of());
    }

    public static void setTechniqueData(List<String> techniques, List<String> slots, Map<String, Integer> cooldownRemainingTicks) {
        learnedTechniques = techniques.stream().sorted().toList();
        techniqueSlots = normalizeSlots(slots, learnedTechniques);
        int now = currentClientTick();
        Map<String, Integer> cooldowns = new HashMap<>();
        cooldownRemainingTicks.forEach((techniqueId, remainingTicks) -> {
            if (learnedTechniques.contains(techniqueId) && remainingTicks > 0) {
                cooldowns.put(techniqueId, now + remainingTicks);
            }
        });
        cooldownUntilClientTicks = Map.copyOf(cooldowns);
        synced = true;
    }

    public static void reset() {
        learnedTechniques = List.of();
        techniqueSlots = emptySlots();
        cooldownUntilClientTicks = Map.of();
        synced = false;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static List<String> getLearnedTechniques() {
        return learnedTechniques;
    }

    public static List<String> getTechniqueSlots() {
        return techniqueSlots;
    }

    public static String getTechniqueInSlot(int slot) {
        return slot >= 0 && slot < techniqueSlots.size() ? techniqueSlots.get(slot) : "";
    }

    public static int getCooldownRemainingTicks(String techniqueId) {
        int remaining = cooldownUntilClientTicks.getOrDefault(techniqueId, 0) - currentClientTick();
        return Math.max(0, remaining);
    }

    public static boolean isCoolingDown(String techniqueId) {
        return getCooldownRemainingTicks(techniqueId) > 0;
    }

    public static TechniqueSummary getTechniqueSummary(String id) {
        return BUILTIN_SUMMARIES.getOrDefault(id, TechniqueSummary.fallback(id));
    }

    public static boolean canRelease(String id, ClientCultivationData.Snapshot data) {
        TechniqueSummary summary = getTechniqueSummary(id);
        return !isCoolingDown(id) && data.spiritualPower() >= summary.cost() && !data.severeInjury() && !data.shatteredCore();
    }

    private static List<String> normalizeSlots(List<String> slots, List<String> learned) {
        List<String> normalized = emptySlots();
        if (slots == null || slots.isEmpty()) return defaultSlots(learned);
        for (int i = 0; i < Math.min(SLOT_COUNT, slots.size()); i++) {
            String techniqueId = slots.get(i);
            normalized.set(i, techniqueId != null && learned.contains(techniqueId) ? techniqueId : "");
        }
        return List.copyOf(normalized);
    }

    private static List<String> defaultSlots(List<String> techniques) {
        List<String> slots = emptySlots();
        List<String> sorted = techniques.stream().sorted().toList();
        for (int i = 0; i < Math.min(SLOT_COUNT, sorted.size()); i++) {
            slots.set(i, sorted.get(i));
        }
        return List.copyOf(slots);
    }

    private static List<String> emptySlots() {
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.add("");
        }
        return slots;
    }

    private static int currentClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0 : minecraft.player.tickCount;
    }

    private static Map<String, TechniqueSummary> loadBuiltInSummaries() {
        Map<String, TechniqueSummary> summaries = new HashMap<>();
        ClassLoader loader = ClientTechniqueData.class.getClassLoader();
        for (String filename : BUILTIN_FILES) {
            String path = "data/" + SeekingImmortalsMod.MODID + "/cultivation/" + filename;
            try (InputStream stream = loader.getResourceAsStream(path)) {
                if (stream == null) continue;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray techniques = root.getAsJsonArray("techniques");
                    if (techniques == null) continue;
                    for (JsonElement element : techniques) {
                        JsonObject object = element.getAsJsonObject();
                        String id = getString(object, "id");
                        if (id.isBlank()) continue;
                        summaries.put(id, new TechniqueSummary(
                                id,
                                valueOrFallback(getString(object, "name"), id),
                                valueOrFallback(getString(object, "source"), "未记录功法"),
                                valueOrFallback(getString(object, "attribute"), "通用"),
                                estimateCost(getString(object, "type"), getString(object, "attribute"))));
                    }
                }
            } catch (Exception ignored) {
                // Client tooltip data is best-effort; fallback summaries keep the HUD usable.
            }
        }
        return Map.copyOf(summaries);
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int estimateCost(String type, String attribute) {
        String text = (type + " " + attribute).toLowerCase(Locale.ROOT);
        if (text.contains("formation") || text.contains("sword") || text.contains("阵")) return 35;
        if (text.contains("secret") || text.contains("divine") || text.contains("秘") || text.contains("神通")) return 30;
        if (text.contains("talisman") || text.contains("符")) return 12;
        if (text.contains("utility") || text.contains("通用")) return 8;
        return 15;
    }

    public record TechniqueSummary(String id, String name, String source, String attribute, int cost) {
        public static TechniqueSummary fallback(String id) {
            String safeId = id == null || id.isBlank() ? "unknown" : id;
            return new TechniqueSummary(safeId, safeId, "未记录功法", "未知", 15);
        }
    }
}
