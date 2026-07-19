package com.xunxian.seekingimmortals.cultivation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads techniques from:
 * <ul>
 *   <li>datapack {@code cultivation/*.json} (legacy realm-bucket pack)</li>
 *   <li>classpath {@code text_material/techniques/*.json} (M02 author corpus, 747)</li>
 * </ul>
 * Text-material entries win on id collision so published corpus is authoritative.
 */
public final class TechniqueDataManager {
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

    private static final Map<String, TechniqueEntry> BUILTIN_TECHNIQUES = loadBuiltInTechniques();
    private static final Map<String, SourceSummary> BUILTIN_SOURCE_SUMMARIES = buildSourceSummaries(BUILTIN_TECHNIQUES);

    private TechniqueDataManager() {}

    public static List<TechniqueEntry> getTechniquesBySource(MinecraftServer server, String source) {
        Map<String, TechniqueEntry> techniques = loadTechniques(server);
        return techniques.values().stream()
                .filter(technique -> technique.source().equals(source))
                .sorted(Comparator.comparing(TechniqueEntry::id))
                .toList();
    }

    public static SourceSummary getSourceSummary(String source) {
        return BUILTIN_SOURCE_SUMMARIES.getOrDefault(source, SourceSummary.empty(source));
    }

    public static String describeConditions(String source) {
        SourceSummary summary = getSourceSummary(source);
        if (summary.attributes().isEmpty()) return "无特殊限制";
        return String.join("、", summary.attributes());
    }

    public static String describeTechniqueNames(String source) {
        SourceSummary summary = getSourceSummary(source);
        if (summary.names().isEmpty()) return "暂无收录术法";
        int max = 6;
        List<String> names = summary.names();
        String joined = String.join("、", names.subList(0, Math.min(max, names.size())));
        if (names.size() > max) {
            joined += " 等 " + names.size() + " 种";
        }
        return joined;
    }

    public static Optional<TechniqueEntry> getTechnique(MinecraftServer server, String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        TechniqueEntry builtin = BUILTIN_TECHNIQUES.get(id);
        if (builtin != null) {
            return Optional.of(builtin);
        }
        if (server == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadTechniques(server).get(id));
    }

    /** Classpath-only snapshot used by tests and client mirrors. */
    public static Map<String, TechniqueEntry> builtinTechniques() {
        return BUILTIN_TECHNIQUES;
    }

    public static int builtinTechniqueCount() {
        return BUILTIN_TECHNIQUES.size();
    }

    public static Map<String, TechniqueEntry> loadTechniques(MinecraftServer server) {
        Map<String, TechniqueEntry> result = new LinkedHashMap<>(BUILTIN_TECHNIQUES);
        if (server == null) {
            return result;
        }
        server.getResourceManager().listResources("cultivation", location -> location.getPath().endsWith(".json")).forEach((location, resource) -> {
            if (!SeekingImmortalsMod.MODID.equals(location.getNamespace())) {
                return;
            }
            try (BufferedReader reader = resource.openAsReader()) {
                loadCultivationEntries(reader, result, false);
            } catch (Exception exception) {
                SeekingImmortalsMod.LOGGER.warn("Failed to load cultivation technique data from {}", location, exception);
            }
        });
        // Text-material already baked into BUILTIN; re-apply so datapack overrides lose to corpus ids.
        result.putAll(BUILTIN_TECHNIQUES);
        return result;
    }

    public static boolean matchesAttributeCondition(PlayerCultivation cultivation, String attributeCondition) {
        if (attributeCondition == null || attributeCondition.isBlank()) return true;
        String normalized = attributeCondition.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "通用", "辅助", "秘术", "神识", "神念", "空间", "跨界", "封印", "保命", "傀儡", "炼体",
                "赶路", "身法", "符", "阵", "阵法", "无属性", "neutral", "common")) {
            return true;
        }
        for (SpiritualRootAttribute attribute : cultivation.getSpiritualRootAttributes()) {
            String displayName = attribute.getDisplayName();
            if (!displayName.isBlank() && attributeCondition.contains(displayName)) {
                return true;
            }
            if (normalized.contains(attribute.name().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        if (cultivation.getSpiritualRoot().getAttributeCount() == 1 && containsAny(normalized, "天灵根", "单属性")) {
            return true;
        }
        return false;
    }

    public static TechniqueAffinityCalculator.AffinityResult calculateAffinity(PlayerCultivation cultivation, TechniqueEntry technique) {
        return TechniqueAffinityCalculator.calculate(cultivation, technique);
    }

    public static TechniqueAffinityCalculator.AffinityResult calculateAffinity(PlayerCultivation cultivation, String attributeExpression) {
        return TechniqueAffinityCalculator.calculate(cultivation, attributeExpression);
    }

    public static double getAffinityMultiplier(PlayerCultivation cultivation, TechniqueEntry technique) {
        return calculateAffinity(cultivation, technique).multiplier();
    }

    public static double getAffinityMultiplier(PlayerCultivation cultivation, String attributeExpression) {
        return calculateAffinity(cultivation, attributeExpression).multiplier();
    }

    public static double getBreakthroughQualityBonus(TechniqueEntry technique) {
        if (technique.quality() > 0) return Math.min(0.10D, Math.max(0, technique.quality()) / 100.0D);
        String id = technique.id().toLowerCase(Locale.ROOT);
        String source = technique.source().toLowerCase(Locale.ROOT);
        if (containsAny(source, "天阶", "化神", "灵界", "古魔", "通天", "大衍", "元磁", "真魔") || containsAny(id, "spirit_transformation", "heaven", "void", "magnetic")) return 0.10D;
        if (containsAny(source, "元婴", "古宝", "高级", "真灵") || containsAny(id, "nascent", "soul")) return 0.08D;
        if (containsAny(source, "结丹", "金丹", "剑诀", "秘典") || containsAny(id, "core", "golden", "sword")) return 0.06D;
        if (containsAny(source, "筑基", "中阶", "阵法", "符宝") || containsAny(id, "foundation")) return 0.04D;
        if (containsAny(source, "长春功", "低阶", "炼气")) return 0.02D;
        return 0.0D;
    }

    private static Map<String, TechniqueEntry> loadBuiltInTechniques() {
        Map<String, TechniqueEntry> entries = new LinkedHashMap<>();
        ClassLoader loader = TechniqueDataManager.class.getClassLoader();
        for (String filename : BUILTIN_CULTIVATION_FILES) {
            String path = "data/" + SeekingImmortalsMod.MODID + "/cultivation/" + filename;
            try (InputStream stream = loader.getResourceAsStream(path)) {
                if (stream == null) continue;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    loadCultivationEntries(reader, entries, true);
                }
            } catch (Exception exception) {
                SeekingImmortalsMod.LOGGER.warn("Failed to load built-in technique data from {}", path, exception);
            }
        }
        for (String stem : TEXT_MATERIAL_TECHNIQUE_FILES) {
            String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/techniques/" + stem + ".json";
            try (InputStream stream = loader.getResourceAsStream(path)) {
                if (stream == null) continue;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    loadTextMaterialEntries(reader, entries, stem);
                }
            } catch (Exception exception) {
                SeekingImmortalsMod.LOGGER.warn("Failed to load text-material technique data from {}", path, exception);
            }
        }
        return Map.copyOf(entries);
    }

    private static Map<String, SourceSummary> buildSourceSummaries(Map<String, TechniqueEntry> entries) {
        Map<String, LinkedHashSet<String>> attributesBySource = new LinkedHashMap<>();
        Map<String, List<String>> namesBySource = new LinkedHashMap<>();
        entries.values().stream().sorted(Comparator.comparing(TechniqueEntry::id)).forEach(entry -> {
            if (entry.source().isBlank()) return;
            attributesBySource.computeIfAbsent(entry.source(), key -> new LinkedHashSet<>());
            if (!entry.attribute().isBlank()) {
                attributesBySource.get(entry.source()).add(entry.attribute());
            }
            namesBySource.computeIfAbsent(entry.source(), key -> new ArrayList<>());
            if (!entry.name().isBlank()) {
                namesBySource.get(entry.source()).add(entry.name());
            }
        });

        Map<String, SourceSummary> summaries = new LinkedHashMap<>();
        for (String source : namesBySource.keySet()) {
            summaries.put(source, new SourceSummary(source,
                    List.copyOf(attributesBySource.getOrDefault(source, new LinkedHashSet<>())),
                    List.copyOf(namesBySource.getOrDefault(source, List.of()))));
        }
        return Map.copyOf(summaries);
    }

    private static void loadCultivationEntries(BufferedReader reader, Map<String, TechniqueEntry> result, boolean overwrite) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray techniques = root.getAsJsonArray("techniques");
        if (techniques == null) return;
        for (JsonElement element : techniques) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String id = getString(object, "id");
            if (id.isBlank()) continue;
            if (!overwrite && result.containsKey(id)) continue;
            String name = getString(object, "name");
            if (name.isBlank()) name = getString(object, "display");
            result.put(id, new TechniqueEntry(
                    id,
                    name,
                    getString(object, "source"),
                    getString(object, "attribute"),
                    getInt(object, "quality"),
                    object.has("cost") ? object.get("cost").getAsInt() : 15,
                    parseRealm(firstNonBlank(getString(object, "required_realm"), getString(object, "realm_min"))),
                    getString(object, "requires_method"),
                    getString(object, "type"),
                    "",
                    "",
                    getInt(object, "cooldown_ticks")));
        }
    }

    private static void loadTextMaterialEntries(BufferedReader reader, Map<String, TechniqueEntry> result, String schoolFallback) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray techniques = root.getAsJsonArray("techniques");
        if (techniques == null) return;
        String schoolDefault = getString(root, "school");
        if (schoolDefault.isBlank()) schoolDefault = schoolFallback;
        for (JsonElement element : techniques) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String id = getString(object, "id");
            if (id.isBlank()) continue;
            String display = getString(object, "display");
            if (display.isBlank()) display = getString(object, "name");
            String school = getString(object, "school");
            if (school.isBlank()) school = schoolDefault;
            String elementAttr = getString(object, "element");
            if (elementAttr.isBlank()) elementAttr = school;
            String source = getString(object, "source");
            if (source.isBlank()) source = school;
            String requiresMethod = getString(object, "requires_method");
            if (requiresMethod.isBlank()) requiresMethod = getString(object, "source_method");
            String effectType = "";
            String effectElement = "";
            double damageBase = 0.0D;
            String effectKey = "";
            Set<String> tags = new LinkedHashSet<>(getStringSet(object.get("tags")));
            if (object.has("effect") && object.get("effect").isJsonObject()) {
                JsonObject effect = object.getAsJsonObject("effect");
                effectType = getString(effect, "type");
                effectElement = getString(effect, "element");
                damageBase = getDouble(effect, "damage_base");
                effectKey = getString(effect, "effect_key");
                tags.addAll(getStringSet(effect.get("tags")));
                if (elementAttr.isBlank()) elementAttr = effectElement;
            } else if (object.has("effect") && object.get("effect").isJsonPrimitive()) {
                effectType = object.get("effect").getAsString();
            }
            if (damageBase <= 0.0D) {
                damageBase = getDouble(object, "damage_base");
            }
            if (effectKey.isBlank()) {
                effectKey = getString(object, "effect_key");
            }
            JsonObject setting = object.has("setting") && object.get("setting").isJsonObject()
                    ? object.getAsJsonObject("setting")
                    : new JsonObject();
            String target = firstNonBlank(getString(setting, "target"), getString(object, "target"));
            String range = firstNonBlank(getString(setting, "range"), getString(object, "range"));
            int cost = object.has("spirit_cost_base") && !object.get("spirit_cost_base").isJsonNull()
                    ? object.get("spirit_cost_base").getAsInt()
                    : (object.has("cost") && !object.get("cost").isJsonNull() ? object.get("cost").getAsInt() : 15);
            Realm realm = parseRealm(firstNonBlank(
                    getString(object, "realm_min"),
                    learnRequirementRealm(object),
                    getString(object, "required_realm")));
            String tier = getString(object, "tier");
            // Text-material always overwrites legacy cultivation ids so corpus is authoritative.
            result.put(id, new TechniqueEntry(
                    id,
                    display,
                    source,
                    elementAttr,
                    getInt(object, "quality"),
                    Math.max(1, cost),
                    realm,
                    requiresMethod,
                    tier.isBlank() ? "spell" : tier,
                    effectType,
                    effectElement,
                    getInt(object, "cooldown_ticks"),
                    damageBase,
                    effectKey,
                    tags,
                    target,
                    range));
        }
    }

    private static String learnRequirementRealm(JsonObject object) {
        if (!object.has("learn_requirements") || !object.get("learn_requirements").isJsonObject()) {
            return "";
        }
        JsonObject learn = object.getAsJsonObject("learn_requirements");
        return getString(learn, "realm_min");
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }

    private static int getInt(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsInt() : 0;
    }

    private static double getDouble(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()
                || !object.get(key).isJsonPrimitive()) {
            return 0.0D;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException ignored) {
            return 0.0D;
        }
    }

    private static Set<String> getStringSet(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child == null || child.isJsonNull() || !child.isJsonPrimitive()
                    || !child.getAsJsonPrimitive().isString()) {
                continue;
            }
            String value = child.getAsString().trim().toLowerCase(Locale.ROOT);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return Set.copyOf(values);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Realm parseRealm(String id) {
        if (id == null || id.isBlank()) {
            return Realm.QI_REFINING;
        }
        Realm realm = Realm.fromDesignId(id);
        if (realm != null) {
            return realm;
        }
        String normalized = id.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        // Common corpus aliases
        return switch (normalized) {
            case "FOUNDATION", "FOUNDATION_ESTABLISHMENT" -> Realm.FOUNDATION_ESTABLISHMENT;
            case "CORE", "CORE_FORMATION", "GOLDEN_CORE", "JINDAN" -> Realm.CORE_FORMATION;
            case "NASCENT", "NASCENT_SOUL", "YUAN_YING" -> Realm.NASCENT_SOUL;
            case "SPIRIT_TRANSFORMATION", "SPIRIT_TRANSFORMATION_PLUS", "HUASHEN" -> Realm.SOUL_TRANSFORMATION;
            default -> null;
        };
    }

    public record TechniqueEntry(
            String id,
            String name,
            String source,
            String attribute,
            int quality,
            int cost,
            Realm requiredRealm,
            String requiresMethod,
            String tier,
            String effectType,
            String effectElement,
            int cooldownTicks,
            double damageBase,
            String effectKey,
            Set<String> tags,
            String target,
            String range) {
        /** Legacy 12-arg constructor used by datapack entries and older call sites. */
        public TechniqueEntry(String id, String name, String source, String attribute,
                              int quality, int cost, Realm requiredRealm, String requiresMethod,
                              String tier, String effectType, String effectElement, int cooldownTicks) {
            this(id, name, source, attribute, quality, cost, requiredRealm, requiresMethod,
                    tier, effectType, effectElement, cooldownTicks, 0.0D, "", Set.of(), "", "");
        }

        /** Legacy 7-arg constructor used by tests and older call sites. */
        public TechniqueEntry(String id, String name, String source, String attribute,
                              int quality, int cost, Realm requiredRealm) {
            this(id, name, source, attribute, quality, cost, requiredRealm, "", "", "", "", 0);
        }

        public TechniqueEntry {
            id = id == null ? "" : id;
            name = name == null ? "" : name;
            source = source == null ? "" : source;
            attribute = attribute == null ? "" : attribute;
            requiresMethod = requiresMethod == null ? "" : requiresMethod;
            tier = tier == null ? "" : tier;
            effectType = effectType == null ? "" : effectType;
            effectElement = effectElement == null ? "" : effectElement;
            damageBase = Math.max(0.0D, damageBase);
            effectKey = effectKey == null ? "" : effectKey.trim().toLowerCase(Locale.ROOT);
            tags = tags == null ? Set.of() : tags.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            target = target == null ? "" : target.trim().toLowerCase(Locale.ROOT);
            range = range == null ? "" : range.trim().toLowerCase(Locale.ROOT);
            cost = Math.max(0, cost);
            cooldownTicks = Math.max(0, cooldownTicks);
        }
    }

    public record SourceSummary(String source, List<String> attributes, List<String> names) {
        public static SourceSummary empty(String source) {
            return new SourceSummary(source, List.of(), List.of());
        }
    }
}
