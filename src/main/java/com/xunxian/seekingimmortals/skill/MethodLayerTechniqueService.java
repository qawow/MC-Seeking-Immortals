package com.xunxian.seekingimmortals.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M02 method-layer matrix authority. Catalog layers are the playable scale while matrix rows may
 * represent either exact layers or wider stage bands such as Changchun Gong's 1-3/4-6 bands.
 */
public final class MethodLayerTechniqueService {
    private static final Pattern LEADING_LAYER = Pattern.compile("^(\\d+)\\s*(?:[-—–~至]\\s*\\d+)?\\s*层");
    private static final Map<String, MethodProgression> BY_METHOD = loadBuiltin();

    private MethodLayerTechniqueService() {}

    public record LayerUnlock(int matrixLayer, String layerName, String realmBand,
                              List<String> techniqueIds) {
        public LayerUnlock {
            layerName = layerName == null ? "" : layerName;
            realmBand = realmBand == null ? "" : realmBand;
            techniqueIds = techniqueIds == null ? List.of() : List.copyOf(techniqueIds);
        }
    }

    public record MethodProgression(int matrixTotalLayers, List<LayerUnlock> layers,
                                    List<String> prerequisiteMethods) {
        public MethodProgression {
            matrixTotalLayers = Math.max(0, matrixTotalLayers);
            layers = layers == null ? List.of() : List.copyOf(layers);
            prerequisiteMethods = prerequisiteMethods == null ? List.of() : List.copyOf(prerequisiteMethods);
        }
    }

    public static int methodCount() {
        return BY_METHOD.size();
    }

    public static int matrixTotalLayers(String methodId) {
        MethodProgression progression = progression(methodId);
        return progression == null ? 0 : progression.matrixTotalLayers();
    }

    /** Catalog explicit layers win; positive matrix totals are fallback; non-progressive methods stay at layer 1. */
    public static int maxLayers(String methodId) {
        TextMaterialCatalogService.MethodEntry method = TextMaterialCatalogService.builtin()
                .findMethod(methodId).orElse(null);
        if (method != null && method.explicitMaxLayers() > 0) {
            return method.explicitMaxLayers();
        }
        int matrixLayers = matrixTotalLayers(methodId);
        return matrixLayers > 0 ? matrixLayers : 1;
    }

    public static String requiredRealmForLayer(String methodId, int actualLayer) {
        LayerUnlock unlock = mappedLayer(methodId, actualLayer);
        return unlock == null ? "" : unlock.realmBand();
    }

    public static String layerNameForLayer(String methodId, int actualLayer) {
        LayerUnlock unlock = mappedLayer(methodId, actualLayer);
        return unlock == null ? "" : unlock.layerName();
    }

    public static List<String> techniquesForLayer(String methodId, int actualLayer) {
        if (methodId == null || methodId.isBlank() || actualLayer <= 0) {
            return List.of();
        }
        MethodProgression progression = progression(methodId);
        if (progression == null || progression.layers().isEmpty()) {
            return List.of();
        }
        int maxLayers = maxLayers(methodId);
        int clampedLayer = Math.min(maxLayers, actualLayer);
        LinkedHashSet<String> techniques = new LinkedHashSet<>();
        for (int index = 0; index < progression.layers().size(); index++) {
            LayerUnlock unlock = progression.layers().get(index);
            if (actualThreshold(unlock, index, progression.layers().size(), maxLayers) <= clampedLayer) {
                techniques.addAll(unlock.techniqueIds());
            }
        }
        return List.copyOf(techniques);
    }

    /**
     * Grants all techniques unlocked up to {@code layer} for {@code methodId}.
     * @return number of newly learned techniques
     */
    public static int grantForMethodLayer(ServerPlayer player, String methodId, int layer) {
        if (player == null || methodId == null || methodId.isBlank() || layer <= 0) {
            return 0;
        }
        List<String> techniques = techniquesForLayer(methodId, layer);
        if (techniques.isEmpty()) {
            return 0;
        }
        int[] granted = {0};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            for (String techniqueId : techniques) {
                if (cultivation.learnTechnique(techniqueId)) {
                    granted[0]++;
                    SkillType skillType = SkillEffectRegistrySafe.byTechniqueId(techniqueId);
                    if (skillType != null && !cultivation.hasSkill(skillType)) {
                        cultivation.unlockSkillForQuest(skillType);
                    }
                }
            }
            if (granted[0] > 0) {
                SyncLearnedTechniquesPacket.send(player, cultivation);
                SyncCultivationDataPacket.send(player, cultivation);
            }
        });
        return granted[0];
    }

    private static LayerUnlock mappedLayer(String methodId, int actualLayer) {
        if (actualLayer <= 0) {
            return null;
        }
        MethodProgression progression = progression(methodId);
        if (progression == null || progression.layers().isEmpty()) {
            return null;
        }
        int maxLayers = maxLayers(methodId);
        int clampedLayer = Math.min(maxLayers, actualLayer);
        LayerUnlock mapped = progression.layers().get(0);
        for (int index = 0; index < progression.layers().size(); index++) {
            LayerUnlock candidate = progression.layers().get(index);
            if (actualThreshold(candidate, index, progression.layers().size(), maxLayers) > clampedLayer) {
                break;
            }
            mapped = candidate;
        }
        return mapped;
    }

    static int actualThreshold(LayerUnlock unlock, int index, int stageCount, int maxLayers) {
        if (maxLayers <= 1 || index <= 0) {
            return 1;
        }
        Matcher matcher = LEADING_LAYER.matcher(unlock.layerName().trim());
        if (matcher.find()) {
            try {
                return Math.max(1, Math.min(maxLayers, Integer.parseInt(matcher.group(1))));
            } catch (NumberFormatException ignored) {
                // Fall through to proportional stage mapping.
            }
        }
        if (stageCount == maxLayers) {
            return Math.min(maxLayers, index + 1);
        }
        int bandSize = Math.max(1, (maxLayers + Math.max(1, stageCount) - 1) / Math.max(1, stageCount));
        return Math.min(maxLayers, 1 + index * bandSize);
    }

    private static MethodProgression progression(String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return null;
        }
        return BY_METHOD.get(methodId.trim().toLowerCase(Locale.ROOT));
    }

    private static Map<String, MethodProgression> loadBuiltin() {
        Map<String, MethodProgression> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID
                + "/text_material/method_layer_technique_matrix_v130.json");
        if (root == null) {
            return Map.of();
        }
        JsonArray tables = root.has("method_layer_tables") && root.get("method_layer_tables").isJsonArray()
                ? root.getAsJsonArray("method_layer_tables") : new JsonArray();
        for (JsonElement element : tables) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject table = element.getAsJsonObject();
            String methodId = str(table, "method_id");
            if (methodId.isBlank()) {
                continue;
            }
            int totalLayers = positiveInt(table, "total_layers");
            List<LayerUnlock> unlocks = new ArrayList<>();
            JsonArray layers = table.has("layers") && table.get("layers").isJsonArray()
                    ? table.getAsJsonArray("layers") : new JsonArray();
            for (JsonElement layerEl : layers) {
                if (!layerEl.isJsonObject()) {
                    continue;
                }
                JsonObject layerObj = layerEl.getAsJsonObject();
                int layer = positiveInt(layerObj, "layer");
                List<String> ids = stringList(layerObj.get("unlock_technique_ids"));
                if (ids.isEmpty()) {
                    ids = stringList(layerObj.get("unlock_new_this_layer"));
                }
                if (layer > 0) {
                    unlocks.add(new LayerUnlock(layer, str(layerObj, "layer_name"),
                            str(layerObj, "realm_band"), ids));
                }
            }
            map.put(methodId.toLowerCase(Locale.ROOT), new MethodProgression(totalLayers, unlocks,
                    stringList(table.get("prerequisite_methods"))));
        }
        return Collections.unmodifiableMap(map);
    }

    private static int positiveInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return Math.max(0, object.get(key).getAsInt());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child != null && child.isJsonPrimitive()) {
                String value = child.getAsString();
                if (value != null && !value.isBlank()) {
                    out.add(value);
                }
            }
        }
        return List.copyOf(out);
    }

    private static String str(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static JsonObject readJson(String path) {
        ClassLoader loader = MethodLayerTechniqueService.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load method layer matrix from {}", path, exception);
            return null;
        }
    }

    /** Avoid hard dependency cycles in static init of SkillEffectRegistry. */
    private static final class SkillEffectRegistrySafe {
        private static SkillType byTechniqueId(String id) {
            try {
                return com.xunxian.seekingimmortals.skill.effect.SkillEffectRegistry.byTechniqueId(id);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
