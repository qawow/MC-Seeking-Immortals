package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

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
 * M13 spirit_realm_interface + mortal_to_spirit_bridge rules.
 * Encodes open window / one-way main-body channel / loss notes for 凡界↔灵界.
 */
public final class SpiritRealmInterfaceService {
    private static final Snapshot SNAPSHOT = load();

    private SpiritRealmInterfaceService() {}

    public record GateDef(
            String id,
            String type,
            String from,
            String to,
            String fromDimension,
            String toDimension,
            String realmMin,
            boolean oneWay,
            int feeContribution,
            String faction,
            String nodeRef,
            String note) {}

    public record BridgeRules(
            String openTrigger,
            boolean mainBodyOneWay,
            String lossNote,
            String destinationDefault,
            List<String> helpItems,
            List<String> failureOutcomes) {}

    public record Snapshot(
            Map<String, GateDef> gates,
            BridgeRules bridge,
            double tianyuanCultivationMultiplier,
            String tianyuanResidenceRealmMin) {
        public int gateCount() {
            return gates.size();
        }

        public Optional<GateDef> findGate(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(gates.get(id.trim().toLowerCase(Locale.ROOT)));
        }

        public List<GateDef> allGates() {
            return List.copyOf(gates.values());
        }
    }

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int gateCount() {
        return SNAPSHOT.gateCount();
    }

    public static Optional<GateDef> findGate(String id) {
        return SNAPSHOT.findGate(id);
    }

    public static BridgeRules bridge() {
        return SNAPSHOT.bridge();
    }

    public static boolean isMainBodyOneWay(String gateId) {
        Optional<GateDef> gate = findGate(gateId);
        if (gate.isPresent() && gate.get().oneWay()) {
            return true;
        }
        return SNAPSHOT.bridge().mainBodyOneWay()
                && ("mortal_to_tianyuan".equalsIgnoreCase(gateId)
                || "ascension_channel".equalsIgnoreCase(gateId));
    }

    public static Optional<GateDef> gateBetween(String fromDimension, String toDimension) {
        String from = normalizeDim(fromDimension);
        String to = normalizeDim(toDimension);
        for (GateDef gate : SNAPSHOT.gates.values()) {
            if (normalizeDim(gate.fromDimension()).equals(from) && normalizeDim(gate.toDimension()).equals(to)) {
                return Optional.of(gate);
            }
        }
        return Optional.empty();
    }

    private static Snapshot load() {
        Map<String, GateDef> gates = new LinkedHashMap<>();
        JsonObject interfaceRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/spirit_realm_interface.json");
        JsonObject bridgeRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/mortal_to_spirit_bridge.json");

        if (interfaceRoot != null) {
            for (JsonElement element : array(interfaceRoot, "gates")) {
                ingestGate(gates, element);
            }
        }
        if (bridgeRoot != null) {
            for (JsonElement element : array(bridgeRoot, "gates")) {
                ingestGate(gates, element);
            }
        }
        if (gates.isEmpty()) {
            gates.put("mortal_to_tianyuan", new GateDef(
                    "mortal_to_tianyuan", "ascension", "mortal_realm", "tianyuan",
                    DimensionRegistryService.MORTAL_WORLD, DimensionRegistryService.TIANYUAN,
                    "DEITY_TRANSFORMATION", true, 0, "", "gate_mortal_to_tianyuan",
                    "飞升通道，本体单向"));
            gates.put("tianyuan_to_spirit_fengyuan", new GateDef(
                    "tianyuan_to_spirit_fengyuan", "regulated_portal", "tianyuan", "spirit_fengyuan",
                    DimensionRegistryService.TIANYUAN, DimensionRegistryService.SPIRIT_FENGYUAN,
                    "VOID_REFINEMENT", false, 500, "tianyuan_garrison", "",
                    "天渊界门"));
        }

        BridgeRules bridge = parseBridge(interfaceRoot, bridgeRoot);
        double mult = 2.0D;
        String residence = "DEITY_TRANSFORMATION";
        if (interfaceRoot != null && interfaceRoot.has("tianyuan_city") && interfaceRoot.get("tianyuan_city").isJsonObject()) {
            JsonObject city = interfaceRoot.getAsJsonObject("tianyuan_city");
            if (city.has("cultivation_multiplier")) {
                try {
                    mult = city.get("cultivation_multiplier").getAsDouble();
                } catch (Exception ignored) {
                }
            }
            residence = firstNonBlank(str(city, "realm_min_residence"), residence);
        }
        return new Snapshot(Collections.unmodifiableMap(gates), bridge, mult, residence);
    }

    private static BridgeRules parseBridge(JsonObject interfaceRoot, JsonObject bridgeRoot) {
        String trigger = "DEITY_TRANSFORMATION_peak + tribulation_success";
        boolean oneWay = true;
        String loss = "通道损耗：本体飞升后不可逆回；分身下界另计";
        String dest = "tianyuan";
        List<String> help = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        JsonObject ascension = null;
        if (interfaceRoot != null && interfaceRoot.has("ascension") && interfaceRoot.get("ascension").isJsonObject()) {
            ascension = interfaceRoot.getAsJsonObject("ascension");
        } else if (bridgeRoot != null && bridgeRoot.has("ascension") && bridgeRoot.get("ascension").isJsonObject()) {
            ascension = bridgeRoot.getAsJsonObject("ascension");
        }
        if (ascension != null) {
            trigger = firstNonBlank(str(ascension, "trigger"), trigger);
            dest = firstNonBlank(str(ascension, "destination_default"), dest);
            for (JsonElement e : array(ascension, "items_help")) {
                try {
                    help.add(e.getAsString());
                } catch (Exception ignored) {
                }
            }
            for (JsonElement e : array(ascension, "failure_outcomes")) {
                try {
                    failures.add(e.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        if (bridgeRoot != null && bridgeRoot.has("window") && bridgeRoot.get("window").isJsonObject()) {
            JsonObject window = bridgeRoot.getAsJsonObject("window");
            oneWay = !window.has("main_body_one_way") || window.get("main_body_one_way").getAsBoolean();
            loss = firstNonBlank(str(window, "loss_note"), loss);
            trigger = firstNonBlank(str(window, "open_trigger"), trigger);
        }
        if (help.isEmpty()) {
            help = List.of("ascension_talisman", "space_anchor_talisman");
        }
        if (failures.isEmpty()) {
            failures = List.of("death", "cripple", "demonization");
        }
        return new BridgeRules(trigger, oneWay, loss, dest, List.copyOf(help), List.copyOf(failures));
    }

    private static void ingestGate(Map<String, GateDef> gates, JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject o = element.getAsJsonObject();
        String id = str(o, "id");
        if (id.isBlank()) {
            return;
        }
        int fee = 0;
        if (o.has("fee_contribution")) {
            try {
                fee = o.get("fee_contribution").getAsInt();
            } catch (Exception ignored) {
            }
        }
        gates.put(id.toLowerCase(Locale.ROOT), new GateDef(
                id,
                str(o, "type"),
                str(o, "from"),
                str(o, "to"),
                firstNonBlank(str(o, "from_dimension"), guessDim(str(o, "from"))),
                firstNonBlank(str(o, "to_dimension"), guessDim(str(o, "to"))),
                str(o, "realm_min"),
                o.has("one_way") && o.get("one_way").getAsBoolean(),
                fee,
                str(o, "faction"),
                str(o, "node_ref"),
                str(o, "note")));
    }

    private static String guessDim(String token) {
        String key = token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "mortal_realm", "mortal_world", "tiannan" -> DimensionRegistryService.MORTAL_WORLD;
            case "tianyuan" -> DimensionRegistryService.TIANYUAN;
            case "spirit_fengyuan", "fengyuan", "spirit_realm" -> DimensionRegistryService.SPIRIT_FENGYUAN;
            case "immortal_realm", "xianjie" -> DimensionRegistryService.IMMORTAL_REALM;
            case "yin_ming", "yinming", "nether_river" -> DimensionRegistryService.YIN_MING_POCKET;
            case "demon_rift" -> DimensionRegistryService.DEMON_RIFT;
            default -> key.contains(":") ? key : "";
        };
    }

    private static String normalizeDim(String id) {
        return DimensionRegistryService.toMinecraftDimensionId(id);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = SpiritRealmInterfaceService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            JsonElement element = object.get(key);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
            return element.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
