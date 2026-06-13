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

public final class TechniqueDataManager {
    private static final List<String> BUILTIN_FILES = List.of(
            "qi_refining_techniques.json",
            "foundation_establishment_techniques.json",
            "core_formation_techniques.json",
            "nascent_soul_techniques.json",
            "spirit_transformation_plus_techniques.json",
            "special_common_techniques.json");
    private static final Map<String, SourceSummary> BUILTIN_SOURCE_SUMMARIES = loadBuiltInSourceSummaries();

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
        return Optional.ofNullable(loadTechniques(server).get(id));
    }

    public static Map<String, TechniqueEntry> loadTechniques(MinecraftServer server) {
        Map<String, TechniqueEntry> result = new LinkedHashMap<>();
        server.getResourceManager().listResources("cultivation", location -> location.getPath().endsWith(".json")).forEach((location, resource) -> {
            if (!SeekingImmortalsMod.MODID.equals(location.getNamespace()) || location.getPath().endsWith("special_common_techniques.json")) {
                return;
            }
            try (BufferedReader reader = resource.openAsReader()) {
                loadEntries(reader, result);
            } catch (Exception exception) {
                SeekingImmortalsMod.LOGGER.warn("Failed to load cultivation technique data from {}", location, exception);
            }
        });
        return result;
    }

    public static boolean matchesAttributeCondition(PlayerCultivation cultivation, String attributeCondition) {
        if (attributeCondition == null || attributeCondition.isBlank()) return true;
        String normalized = attributeCondition.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "通用", "辅助", "秘术", "神识", "神念", "空间", "跨界", "封印", "保命", "傀儡", "炼体",
                "赶路", "身法", "符", "阵", "阵法", "无属性")) {
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

    private static Map<String, SourceSummary> loadBuiltInSourceSummaries() {
        Map<String, TechniqueEntry> entries = new LinkedHashMap<>();
        ClassLoader loader = TechniqueDataManager.class.getClassLoader();
        for (String filename : BUILTIN_FILES) {
            String path = "data/" + SeekingImmortalsMod.MODID + "/cultivation/" + filename;
            try (InputStream stream = loader.getResourceAsStream(path)) {
                if (stream == null) continue;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    loadEntries(reader, entries);
                }
            } catch (Exception exception) {
                SeekingImmortalsMod.LOGGER.warn("Failed to load built-in technique tooltip data from {}", path, exception);
            }
        }

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

    private static void loadEntries(BufferedReader reader, Map<String, TechniqueEntry> result) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray techniques = root.getAsJsonArray("techniques");
        if (techniques == null) return;
        for (JsonElement element : techniques) {
            JsonObject object = element.getAsJsonObject();
            String id = getString(object, "id");
            if (id.isBlank()) continue;
            result.put(id, new TechniqueEntry(
                    id,
                    getString(object, "name"),
                    getString(object, "source"),
                    getString(object, "attribute"),
                    getInt(object, "quality")));
        }
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private static int getInt(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
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

    public record TechniqueEntry(String id, String name, String source, String attribute, int quality) {}
    public record SourceSummary(String source, List<String> attributes, List<String> names) {
        public static SourceSummary empty(String source) {
            return new SourceSummary(source, List.of(), List.of());
        }
    }
}
