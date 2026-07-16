package com.xunxian.seekingimmortals.lore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M16: name alias glossary (v103) — display-name / alias lookup with ItemCatalog consistency checks.
 */
public final class NameAliasGlossaryService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private NameAliasGlossaryService() {}

    public record GlossaryEntry(String id, String primary, List<String> aliases, String type) {
        public GlossaryEntry {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }

    public record Snapshot(Map<String, GlossaryEntry> byId,
                           Map<String, String> aliasToId,
                           List<String> searchTips) {
        public int size() {
            return byId.size();
        }

        public Optional<GlossaryEntry> find(String idOrAlias) {
            if (idOrAlias == null || idOrAlias.isBlank()) {
                return Optional.empty();
            }
            String key = idOrAlias.trim();
            GlossaryEntry direct = byId.get(key.toLowerCase(Locale.ROOT));
            if (direct != null) {
                return Optional.of(direct);
            }
            String mapped = aliasToId.get(key);
            if (mapped == null) {
                mapped = aliasToId.get(key.toLowerCase(Locale.ROOT));
            }
            if (mapped != null) {
                return Optional.ofNullable(byId.get(mapped));
            }
            for (GlossaryEntry entry : byId.values()) {
                if (entry.primary().equalsIgnoreCase(key)) {
                    return Optional.of(entry);
                }
            }
            return Optional.empty();
        }
    }

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static int size() {
        return BUILTIN.size();
    }

    public static Optional<GlossaryEntry> find(String idOrAlias) {
        return BUILTIN.find(idOrAlias);
    }

    public static List<GlossaryEntry> all() {
        return List.copyOf(BUILTIN.byId().values());
    }

    public static List<String> sampleLines(int limit) {
        List<String> out = new ArrayList<>();
        int n = 0;
        for (GlossaryEntry entry : BUILTIN.byId().values()) {
            String aliases = entry.aliases().isEmpty() ? "-" : String.join("/", entry.aliases());
            out.add(entry.primary() + " [" + entry.id() + "] " + aliases);
            if (++n >= Math.max(1, limit)) {
                break;
            }
        }
        return out;
    }

    /**
     * Cross-check glossary ids that look like item ids against ItemCatalogService alias resolution.
     * Returns ids whose catalog resolve disagrees with the glossary primary id (soft report only).
     */
    public static List<String> catalogConsistencyMismatches() {
        List<String> mismatches = new ArrayList<>();
        for (GlossaryEntry entry : BUILTIN.byId().values()) {
            if (!isItemLike(entry.type())) {
                continue;
            }
            String resolved = ItemCatalogService.resolveId(entry.id());
            if (resolved == null || resolved.isBlank()) {
                continue;
            }
            String bare = resolved.contains(":") ? resolved.substring(resolved.indexOf(':') + 1) : resolved;
            if (!bare.equalsIgnoreCase(entry.id()) && ItemCatalogService.isKnownAlias(entry.id())) {
                // alias chain is fine; only flag when known alias maps away without glossary awareness
                if (!entry.aliases().stream().anyMatch(a -> a.equalsIgnoreCase(bare))) {
                    mismatches.add(entry.id() + "->" + bare);
                }
            }
        }
        return List.copyOf(mismatches);
    }

    private static boolean isItemLike(String type) {
        if (type == null) {
            return false;
        }
        String t = type.toLowerCase(Locale.ROOT);
        return t.contains("pill") || t.contains("herb") || t.contains("material") || t.contains("fabao")
                || t.contains("consumable") || t.contains("currency") || t.contains("manual")
                || t.contains("unique") || t.contains("catalyst") || t.contains("liquid")
                || t.contains("treasure") || t.contains("token");
    }

    private static Snapshot loadBuiltin() {
        Map<String, GlossaryEntry> byId = new LinkedHashMap<>();
        Map<String, String> aliasToId = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/name_alias_glossary_v103.json");
        if (root == null) {
            return new Snapshot(Map.of(), Map.of(), List.of());
        }
        JsonArray entries = root.has("entries") && root.get("entries").isJsonArray()
                ? root.getAsJsonArray("entries") : new JsonArray();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id").toLowerCase(Locale.ROOT);
            if (id.isBlank()) {
                continue;
            }
            List<String> aliases = new ArrayList<>();
            if (o.has("aliases") && o.get("aliases").isJsonArray()) {
                for (JsonElement a : o.getAsJsonArray("aliases")) {
                    try {
                        String alias = a.getAsString();
                        if (alias != null && !alias.isBlank()) {
                            aliases.add(alias);
                            aliasToId.putIfAbsent(alias, id);
                            aliasToId.putIfAbsent(alias.toLowerCase(Locale.ROOT), id);
                        }
                    } catch (Exception ignored) {
                        // skip
                    }
                }
            }
            String primary = str(o, "primary");
            if (!primary.isBlank()) {
                aliasToId.putIfAbsent(primary, id);
                aliasToId.putIfAbsent(primary.toLowerCase(Locale.ROOT), id);
            }
            byId.put(id, new GlossaryEntry(id, primary, aliases, str(o, "type")));
        }
        if (root.has("search_tips") && root.get("search_tips").isJsonArray()) {
            for (JsonElement t : root.getAsJsonArray("search_tips")) {
                try {
                    tips.add(t.getAsString());
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        return new Snapshot(Collections.unmodifiableMap(byId), Collections.unmodifiableMap(aliasToId), List.copyOf(tips));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = NameAliasGlossaryService.class.getClassLoader().getResourceAsStream(path)) {
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
}
