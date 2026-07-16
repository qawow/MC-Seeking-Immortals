package com.xunxian.seekingimmortals.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * M02: method_layer_technique_matrix_v130 — when a method reaches layer N, unlock the
 * techniques listed for that layer band.
 */
public final class MethodLayerTechniqueService {
    private static final Map<String, List<LayerUnlock>> BY_METHOD = loadBuiltin();

    private MethodLayerTechniqueService() {}

    public record LayerUnlock(int layer, List<String> techniqueIds) {
        public LayerUnlock {
            techniqueIds = techniqueIds == null ? List.of() : List.copyOf(techniqueIds);
        }
    }

    public static int methodCount() {
        return BY_METHOD.size();
    }

    public static List<String> techniquesForLayer(String methodId, int layer) {
        if (methodId == null || methodId.isBlank() || layer <= 0) {
            return List.of();
        }
        List<LayerUnlock> unlocks = BY_METHOD.get(methodId.trim().toLowerCase(Locale.ROOT));
        if (unlocks == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (LayerUnlock unlock : unlocks) {
            if (unlock.layer() <= layer) {
                out.addAll(unlock.techniqueIds());
            }
        }
        return List.copyOf(out);
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
                    if (skillType != null) {
                        if (!cultivation.hasSkill(skillType)) {
                            cultivation.unlockSkillForQuest(skillType);
                        }
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

    private static Map<String, List<LayerUnlock>> loadBuiltin() {
        Map<String, List<LayerUnlock>> map = new LinkedHashMap<>();
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
            List<LayerUnlock> unlocks = new ArrayList<>();
            JsonArray layers = table.has("layers") && table.get("layers").isJsonArray()
                    ? table.getAsJsonArray("layers") : new JsonArray();
            for (JsonElement layerEl : layers) {
                if (!layerEl.isJsonObject()) {
                    continue;
                }
                JsonObject layerObj = layerEl.getAsJsonObject();
                int layer = layerObj.has("layer") && layerObj.get("layer").isJsonPrimitive()
                        ? layerObj.get("layer").getAsInt() : 0;
                List<String> ids = stringList(layerObj.get("unlock_technique_ids"));
                if (ids.isEmpty()) {
                    ids = stringList(layerObj.get("unlock_new_this_layer"));
                }
                if (layer > 0 && !ids.isEmpty()) {
                    unlocks.add(new LayerUnlock(layer, ids));
                }
            }
            if (!unlocks.isEmpty()) {
                map.put(methodId.toLowerCase(Locale.ROOT), List.copyOf(unlocks));
            }
        }
        return Collections.unmodifiableMap(map);
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
        return out;
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
