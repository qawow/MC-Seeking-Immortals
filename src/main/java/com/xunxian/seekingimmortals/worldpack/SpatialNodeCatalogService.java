package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

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
     * Soft browser + light travel routing for spatial_nodes_catalog.
     * Travel reuses WorldpackGameplayService portal/region paths (multiblock fees still apply
     * when using placeable gates in-world; command path is region-level soft travel).
     */
public final class SpatialNodeCatalogService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private SpatialNodeCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record Node(String id, String display, String type, String region, List<String> requires, int costSpiritStone,
                       String dimensionFrom, String dimensionTo, boolean oneWay) {}

    public record Snapshot(Map<String, Node> nodes) {
        public int size() { return nodes.size(); }
        public Optional<Node> find(String id) {
            return Optional.ofNullable(nodes.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT)));
        }
        public List<String> sample(int limit) {
            List<String> list = new ArrayList<>();
            int i = 0;
            for (Node node : nodes.values()) {
                list.add(node.id() + " | " + node.display() + " | " + node.type() + " | cost=" + node.costSpiritStone());
                if (++i >= Math.max(1, limit)) break;
            }
            return list;
        }
    }

    public static boolean preview(ServerPlayer player, String id) {
        Optional<Node> optional = builtin().find(id);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spatial_node.unknown",
                    Component.translatable("text.seeking_immortals.unknown_spatial_node")), false);
            return false;
        }
        Node node = optional.get();
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spatial_node.preview",
                nodeDisplay(node), typeDisplay(node.type()), regionDisplay(node.region()), node.costSpiritStone()), false);
        if (!node.requires().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spatial_node.requires",
                    requirementsDisplay(node.requires())), false);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spatial_node.travel_hint"), false);
        return true;
    }

    /**
     * Command-side soft travel by node type/region.
     * Does not replace placeable multiblock activation; reuses existing worldpack travel routes.
     */
    public static boolean travel(ServerPlayer player, String id) {
        Optional<Node> optional = builtin().find(id);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spatial_node.unknown",
                    Component.translatable("text.seeking_immortals.unknown_spatial_node")), false);
            return false;
        }
        Node node = optional.get();
        String currentDimension = player.serverLevel().dimension().location().toString();
        if (!sourceDimensionMatches(currentDimension, node)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spatial_node.wrong_dimension",
                    dimensionDisplay(node.dimensionFrom()), dimensionDisplay(currentDimension)), true);
            return false;
        }
        SpatialNodeRequiresService.Reservation reservation = SpatialNodeRequiresService.reserve(player, node);
        if (reservation == null) {
            return false;
        }
        String type = node.type() == null ? "" : node.type().trim().toLowerCase(Locale.ROOT);
        boolean ok = executeTravel(player, node, type);
        if (!ok) {
            reservation.refund(player);
            return false;
        }
        registerNetworkNode(player, node);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.spatial_node.travel_ok", nodeDisplay(node), typeDisplay(type)), true);
        return true;
    }

    private static boolean executeTravel(ServerPlayer player, Node node, String type) {
        // M13: cross-dimension nodes go through DimensionTravelService (server route authority).
        if (node.dimensionTo() != null && !node.dimensionTo().isBlank()
                && (type.contains("ascension") || isCrossDimension(node))) {
            if (type.contains("ascension")) {
                return AscensionService.attemptAscension(player, false);
            }
            return DimensionTravelService.travelToDimension(player, node.dimensionTo(), type);
        }
        return switch (type) {
            case "pocket_gate" -> WorldpackGameplayService.useNetherFerryGate(player, false);
            case "ancient_rift" -> WorldpackGameplayService.useAncientRiftGate(player, false);
            case "cycle_gate" -> WorldpackGameplayService.useCycleGate(player, false);
            case "hidden_rift" -> WorldpackGameplayService.useHiddenRiftGate(player, false);
            case "king_territory" -> WorldpackGameplayService.useKingTerritoryGate(player, false);
            case "ascension_gate" -> AscensionService.attemptAscension(player, false)
                    || WorldpackGameplayService.useAscensionGate(player, false);
            case "sect_gate", "fixed_teleport_array" -> travelToAuthoredRegion(player, node);
            default -> {
                yield travelToAuthoredRegion(player, node);
            }
        };
    }

    private static boolean travelToAuthoredRegion(ServerPlayer player, Node node) {
        String region = node.region() == null || node.region().isBlank()
                ? WorldpackGameplayService.DEFAULT_REGION_ID
                : node.region();
        return WorldpackGameplayService.travel(player, region);
    }

    static boolean sourceDimensionMatches(String currentDimension, Node node) {
        if (node == null || node.dimensionFrom() == null || node.dimensionFrom().isBlank()) {
            return true;
        }
        String expected = DimensionRegistryService.toMinecraftDimensionId(node.dimensionFrom());
        return expected.equals(currentDimension == null ? "" : currentDimension.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isCrossDimension(Node node) {
        if (node.dimensionFrom() == null || node.dimensionFrom().isBlank()
                || node.dimensionTo() == null || node.dimensionTo().isBlank()) {
            return false;
        }
        String from = DimensionRegistryService.toMinecraftDimensionId(node.dimensionFrom());
        String to = DimensionRegistryService.toMinecraftDimensionId(node.dimensionTo());
        return !from.equals(to);
    }

    private static void registerNetworkNode(ServerPlayer player, Node node) {
        if (player == null || node == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        SpatialNodeNetworkSavedData data = SpatialNodeNetworkSavedData.get(level);
        String id = node.id();
        data.upsert(new SpatialNodeNetworkSavedData.NetworkNode(
                id,
                node.id(),
                player.level().dimension().location().toString(),
                player.blockPosition().getX(),
                player.blockPosition().getY(),
                player.blockPosition().getZ(),
                node.type() == null ? "" : node.type(),
                true,
                player.level().getGameTime()));
        data.markUsed(id, player.level().getGameTime());
    }

    private static Snapshot loadBuiltin() {
        Map<String, Node> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/catalog/spatial_nodes_index.json");
        if (root == null) {
            return new Snapshot(Map.of());
        }
        JsonArray array = root.has("nodes") && root.get("nodes").isJsonArray() ? root.getAsJsonArray("nodes") : new JsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) continue;
            List<String> requires = new ArrayList<>();
            if (o.has("requires") && o.get("requires").isJsonArray()) {
                for (JsonElement r : o.getAsJsonArray("requires")) {
                    try { requires.add(r.getAsString()); } catch (Exception ignored) { requires.add(String.valueOf(r)); }
                }
            }
            int cost = 0;
            if (o.has("cost_spirit_stone") && o.get("cost_spirit_stone").isJsonPrimitive()) {
                try { cost = o.get("cost_spirit_stone").getAsInt(); } catch (Exception ignored) {}
            }
            map.put(id.toLowerCase(Locale.ROOT), new Node(id, str(o, "display"), str(o, "type"), str(o, "region"),
                    List.copyOf(requires), cost, str(o, "dimension_from"), str(o, "dimension_to"),
                    o.has("one_way") && o.get("one_way").isJsonPrimitive() && o.get("one_way").getAsBoolean()));
        }
        return new Snapshot(Collections.unmodifiableMap(map));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = SpatialNodeCatalogService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "";
        try { return o.get(key).getAsString(); } catch (Exception ignored) { return String.valueOf(o.get(key)); }
    }

    private static Component nodeDisplay(Node node) {
        return node == null
                ? Component.translatable("text.seeking_immortals.unknown_spatial_node")
                : PlayerDisplayText.safeLiteral(node.display(), "text.seeking_immortals.unknown_spatial_node");
    }

    private static Component typeDisplay(String rawType) {
        String type = rawType == null ? "" : rawType.trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "fixed_teleport_array" -> Component.translatable("text.seeking_immortals.spatial_type.fixed_array");
            case "ancient_rift" -> Component.translatable("text.seeking_immortals.spatial_type.ancient_rift");
            case "sect_gate" -> Component.translatable("text.seeking_immortals.spatial_type.sect_gate");
            case "ascension_gate" -> Component.translatable("text.seeking_immortals.spatial_type.ascension_gate");
            case "demon_rift_event" -> Component.translatable("text.seeking_immortals.spatial_type.demon_rift");
            case "pocket_gate" -> Component.translatable("text.seeking_immortals.spatial_type.pocket_gate");
            case "cycle_gate" -> Component.translatable("text.seeking_immortals.spatial_type.cycle_gate");
            case "hidden_rift" -> Component.translatable("text.seeking_immortals.spatial_type.hidden_rift");
            case "king_territory" -> Component.translatable("text.seeking_immortals.spatial_type.king_territory");
            default -> Component.translatable("text.seeking_immortals.unknown_spatial_type");
        };
    }

    private static Component regionDisplay(String rawRegion) {
        return RegionRegistry.find(rawRegion)
                .map(region -> PlayerDisplayText.safeLiteral(region.display(), "text.seeking_immortals.unknown_region"))
                .orElse(Component.translatable("text.seeking_immortals.unknown_region"));
    }

    private static Component requirementsDisplay(List<String> requirements) {
        MutableComponent result = Component.empty();
        boolean first = true;
        for (String requirement : requirements == null ? List.<String>of() : requirements) {
            if (!first) {
                result.append(", ");
            }
            result.append(requirementDisplay(requirement));
            first = false;
        }
        return result;
    }

    private static Component requirementDisplay(String rawRequirement) {
        String requirement = rawRequirement == null ? "" : rawRequirement.trim();
        String normalized = requirement.toLowerCase(Locale.ROOT);
        Component named = switch (normalized) {
            case "sect_permit_or_contribution" -> Component.translatable("text.seeking_immortals.requirement.sect_permit");
            case "auction_invite_or_reputation" -> Component.translatable("text.seeking_immortals.requirement.auction_access");
            case "realm_gate" -> Component.translatable("text.seeking_immortals.requirement.realm_gate");
            default -> null;
        };
        if (named != null) {
            return named;
        }
        String itemKey = "item.seeking_immortals." + PlayerDisplayText.normalizeId(requirement);
        if (PlayerDisplayText.hasTranslation(itemKey)) {
            return PlayerDisplayText.itemName(requirement);
        }
        Realm realm = Realm.fromDesignId(requirement);
        if (realm != null) {
            return ArtifactDisplayTexts.realm(requirement);
        }
        return PlayerDisplayText.safeLiteral(requirement, "text.seeking_immortals.unknown_requirement");
    }

    private static Component dimensionDisplay(String rawDimension) {
        String id = rawDimension == null ? "" : rawDimension.trim();
        if (id.isBlank() || "minecraft:overworld".equalsIgnoreCase(id)
                || "overworld".equalsIgnoreCase(id)) {
            return Component.translatable("text.seeking_immortals.dimension.mortal");
        }
        return DimensionRegistryService.find(id)
                .map(def -> PlayerDisplayText.safeLiteral(def.display(), "text.seeking_immortals.unknown_dimension"))
                .orElse(Component.translatable("text.seeking_immortals.unknown_dimension"));
    }
}
