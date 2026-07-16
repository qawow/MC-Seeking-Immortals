package com.xunxian.seekingimmortals.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.skill.SkillType;
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

/**
 * Client mirror for learned techniques / slots / cooldowns.
 * M02: builtin summaries include the full text_material technique corpus (747).
 */
public final class ClientTechniqueData {
    public static final int SLOT_COUNT = 7;
    private static final List<String> BUILTIN_CULTIVATION_FILES = List.of(
            "qi_refining_techniques.json",
            "foundation_establishment_techniques.json",
            "core_formation_techniques.json",
            "nascent_soul_techniques.json",
            "spirit_transformation_plus_techniques.json",
            "special_common_techniques.json");
    private static final List<String> TEXT_MATERIAL_TECHNIQUE_FILES = List.of(
            "body", "buddhist", "confucian", "dao", "demon_path", "demonic", "divine_sense",
            "elemental", "fashi", "formation", "ghost", "illusion", "misc", "movement",
            "puppet", "recovery", "secret_arts", "sword", "talisman", "xuan_yin");
    private static final Map<String, TechniqueSummary> BUILTIN_SUMMARIES = loadBuiltInSummaries();
    private static List<String> learnedTechniques = List.of();
    private static List<String> techniqueSlots = emptySlots();
    private static Map<String, Integer> cooldownUntilClientTicks = Map.of();
    private static boolean synced;

    private ClientTechniqueData() {}

    public static void setLearnedTechniques(List<String> techniques) {
        List<String> learned = techniques.stream().sorted().toList();
        setTechniqueData(learned, retainValidSlots(techniqueSlots, learned), Map.of());
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

    public static double getCooldownFraction(String techniqueId) {
        int remaining = getCooldownRemainingTicks(techniqueId);
        if (remaining <= 0) return 0.0D;
        int total = Math.max(1, getTechniqueSummary(techniqueId).cooldownTicks());
        return Math.max(0.0D, Math.min(1.0D, remaining / (double) total));
    }

    public static TechniqueSummary getTechniqueSummary(String id) {
        return BUILTIN_SUMMARIES.getOrDefault(id, TechniqueSummary.fallback(id));
    }

    public static int builtinSummaryCount() {
        return BUILTIN_SUMMARIES.size();
    }

    public static boolean canRelease(String id, ClientCultivationData.Snapshot data) {
        TechniqueSummary summary = getTechniqueSummary(id);
        return !isCoolingDown(id) && data.spiritualPower() >= summary.cost() && !data.severeInjury() && !data.shatteredCore();
    }

    private static List<String> normalizeSlots(List<String> slots, List<String> learned) {
        if (slots == null || slots.isEmpty()) return retainValidSlots(techniqueSlots, learned);
        List<String> normalized = emptySlots();
        for (int i = 0; i < Math.min(SLOT_COUNT, slots.size()); i++) {
            String techniqueId = slots.get(i);
            normalized.set(i, techniqueId != null && learned.contains(techniqueId) ? techniqueId : "");
        }
        return List.copyOf(normalized);
    }

    private static List<String> retainValidSlots(List<String> current, List<String> learned) {
        List<String> out = emptySlots();
        for (int i = 0; i < SLOT_COUNT; i++) {
            String id = i < current.size() ? current.get(i) : "";
            out.set(i, !id.isBlank() && learned.contains(id) ? id : "");
        }
        return List.copyOf(out);
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
        for (String filename : BUILTIN_CULTIVATION_FILES) {
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
                                valueOrFallback(getString(object, "source"), "unknown_source"),
                                valueOrFallback(getString(object, "attribute"), "common"),
                                getInt(object, "cost", configuredCost(id, getString(object, "type"), getString(object, "attribute"))),
                                getInt(object, "cooldown_ticks", getInt(object, "cooldown", configuredCooldown(id)))));
                    }
                }
            } catch (Exception ignored) {
                // Client tooltip data is best-effort; fallback summaries keep the HUD usable.
            }
        }
        for (String stem : TEXT_MATERIAL_TECHNIQUE_FILES) {
            String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/techniques/" + stem + ".json";
            try (InputStream stream = loader.getResourceAsStream(path)) {
                if (stream == null) continue;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray techniques = root.getAsJsonArray("techniques");
                    if (techniques == null) continue;
                    for (JsonElement element : techniques) {
                        if (!element.isJsonObject()) continue;
                        JsonObject object = element.getAsJsonObject();
                        String id = getString(object, "id");
                        if (id.isBlank()) continue;
                        String display = getString(object, "display");
                        if (display.isBlank()) display = getString(object, "name");
                        String source = getString(object, "source");
                        if (source.isBlank()) source = getString(object, "school");
                        if (source.isBlank()) source = stem;
                        String attribute = getString(object, "element");
                        if (attribute.isBlank()) attribute = getString(object, "school");
                        int cost = getInt(object, "spirit_cost_base",
                                getInt(object, "cost", configuredCost(id, getString(object, "tier"), attribute)));
                        int cooldown = getInt(object, "cooldown_ticks", configuredCooldown(id));
                        // Corpus overwrites legacy cultivation summaries.
                        summaries.put(id, new TechniqueSummary(
                                id,
                                valueOrFallback(display, id),
                                valueOrFallback(source, stem),
                                valueOrFallback(attribute, "common"),
                                Math.max(1, cost),
                                Math.max(20, cooldown)));
                    }
                }
            } catch (Exception ignored) {
                // best-effort
            }
        }
        return Map.copyOf(summaries);
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).isJsonPrimitive()
                ? Math.max(0, object.get(key).getAsInt()) : fallback;
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int estimateCost(String type, String attribute) {
        String text = (type + " " + attribute).toLowerCase(Locale.ROOT);
        if (text.contains("formation") || text.contains("sword")) return 35;
        if (text.contains("secret") || text.contains("divine")) return 30;
        if (text.contains("talisman")) return 12;
        if (text.contains("utility")) return 8;
        return 15;
    }

    private static int configuredCost(String id, String type, String attribute) {
        SkillType skillType = skillTypeByTechniqueId(id);
        return skillType != null && skillType.getConfiguredSpiritualPowerCost() >= 0
                ? skillType.getConfiguredSpiritualPowerCost()
                : estimateCost(type, attribute);
    }

    private static int configuredCooldown(String id) {
        SkillType skillType = skillTypeByTechniqueId(id);
        return skillType != null && skillType.getConfiguredCooldownTicks() >= 0
                ? skillType.getConfiguredCooldownTicks()
                : 100;
    }

    private static SkillType skillTypeByTechniqueId(String id) {
        for (SkillType skillType : SkillType.values()) {
            if (skillType.getTechniqueId() != null && skillType.getTechniqueId().equals(id)) {
                return skillType;
            }
        }
        return null;
    }

    public record TechniqueSummary(String id, String name, String source, String attribute, int cost, int cooldownTicks) {
        public static TechniqueSummary fallback(String id) {
            String safeId = id == null || id.isBlank() ? "unknown" : id;
            return new TechniqueSummary(safeId, safeId, "unknown_source", "unknown", 15, 100);
        }
    }
}
