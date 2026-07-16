package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

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
 * M13 server-side cross-dimension travel authority.
 * <p>Clients may only request a travel method/route id — never a free-form target dimension.
 * Cooldown + realm/gate checks live here.</p>
 */
public final class DimensionTravelService {
    public static final String METHOD_ASCENSION = "ascension_channel";
    public static final String METHOD_REGULATED = "regulated_portal";
    public static final String METHOD_TELEPORT_ARRAY = "fixed_teleport_array";
    public static final String METHOD_FLYING_BOAT = "flying_boat";
    public static final String METHOD_GHOST = "ghost_path_portal";
    public static final String METHOD_DEMON = "demon_rift_event";
    public static final String METHOD_AVATAR = "avatar_descent";

    private static final String COOLDOWN_ROOT = "seeking_immortals_dim_travel_cd";
    private static final long DEFAULT_COOLDOWN_TICKS = 20L * 30L;
    private static final Snapshot SNAPSHOT = load();

    private DimensionTravelService() {}

    public record MethodDef(String id, String display, String note) {}

    public record RouteDef(
            String id,
            String fromDimension,
            String toDimension,
            String method,
            String realmMin,
            boolean oneWay,
            String gateId,
            String allowed) {}

    public record CostDef(String routeId, String band, int spiritStoneMin, int spiritStoneMax, double failChance) {}

    public record Snapshot(
            Map<String, MethodDef> methods,
            Map<String, RouteDef> routes,
            Map<String, CostDef> costs,
            List<TravelMatrixEdge> matrix) {
        public int methodCount() { return methods.size(); }
        public int routeCount() { return routes.size(); }
    }

    public record TravelMatrixEdge(String from, String to, String allowed, String gate, String realmMin) {}

    public static Snapshot snapshot() {
        return SNAPSHOT;
    }

    public static int methodCount() {
        return SNAPSHOT.methodCount();
    }

    public static int routeCount() {
        return SNAPSHOT.routeCount();
    }

    public static Optional<MethodDef> findMethod(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SNAPSHOT.methods.get(id.trim().toLowerCase(Locale.ROOT)));
    }

    public static Optional<RouteDef> findRoute(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SNAPSHOT.routes.get(id.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * Server-authoritative travel by route id. Target dimension comes from route data, not client.
     */
    public static boolean travelByRoute(ServerPlayer player, String routeId) {
        if (player == null) {
            return false;
        }
        Optional<RouteDef> optional = findRoute(routeId);
        if (optional.isEmpty()) {
            // also accept method+matrix style: method id alone is rejected without explicit route
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.unknown_route", routeId == null ? "" : routeId), false);
            return false;
        }
        RouteDef route = optional.get();
        return travelInternal(player, route.fromDimension(), route.toDimension(), route.method(),
                route.realmMin(), route.oneWay(), route.gateId(), route.id());
    }

    /**
     * Server-authoritative travel by matrix edge (from current dim to catalog target).
     * Target is validated against travel_matrix — client cannot invent destinations.
     */
    public static boolean travelToDimension(ServerPlayer player, String targetDimensionId, String methodHint) {
        if (player == null) {
            return false;
        }
        String from = player.level().dimension().location().toString();
        String to = DimensionRegistryService.toMinecraftDimensionId(targetDimensionId);
        // forbid free client target unless matrix allows
        Optional<TravelMatrixEdge> edge = findMatrixEdge(from, targetDimensionId);
        if (edge.isEmpty()) {
            // allow if a known route exists
            Optional<RouteDef> route = findRouteFor(from, targetDimensionId, methodHint);
            if (route.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.dim_travel.not_allowed", to), false);
                return false;
            }
            RouteDef r = route.get();
            return travelInternal(player, r.fromDimension(), r.toDimension(), r.method(),
                    r.realmMin(), r.oneWay(), r.gateId(), r.id());
        }
        TravelMatrixEdge matrix = edge.get();
        if (isForbidden(matrix.allowed())) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.forbidden", matrix.allowed()), false);
            return false;
        }
        String method = firstNonBlank(methodHint, methodFromAllowed(matrix.allowed()), matrix.gate());
        return travelInternal(player, matrix.from(), matrix.to(), method, matrix.realmMin(),
                matrix.allowed().contains("ascension") || matrix.allowed().contains("one_way"),
                matrix.gate(), matrix.from() + "->" + matrix.to());
    }

    public static boolean isOnCooldown(ServerPlayer player, String routeKey) {
        if (player == null) {
            return false;
        }
        String key = normalizeKey(routeKey);
        long until = player.getPersistentData().getCompound(COOLDOWN_ROOT).getLong(key);
        return until > player.level().getGameTime();
    }

    public static long remainingCooldownSeconds(ServerPlayer player, String routeKey) {
        if (player == null) {
            return 0L;
        }
        String key = normalizeKey(routeKey);
        long until = player.getPersistentData().getCompound(COOLDOWN_ROOT).getLong(key);
        long now = player.level().getGameTime();
        return until > now ? (until - now + 19L) / 20L : 0L;
    }

    private static boolean travelInternal(ServerPlayer player, String fromDim, String toDim, String method,
                                          String realmMin, boolean oneWay, String gateId, String routeKey) {
        if (isOnCooldown(player, routeKey)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.cooldown", remainingCooldownSeconds(player, routeKey)), true);
            return false;
        }
        String targetId = DimensionRegistryService.toMinecraftDimensionId(toDim);
        Optional<DimensionRegistryService.DimensionDef> def = DimensionRegistryService.find(toDim);
        if (def.isPresent() && def.get().isDeferred()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.deferred", def.get().display(), def.get().note()), false);
            return false;
        }
        String minRealm = firstNonBlank(realmMin, DimensionRegistryService.minRealmOf(toDim));
        if (!minRealm.isBlank() && !ProgressionGateApi.meetsRealm(player, minRealm)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.realm_too_low", minRealm), false);
            return false;
        }
        if (METHOD_ASCENSION.equalsIgnoreCase(method) || "ascension".equalsIgnoreCase(method)
                || (gateId != null && gateId.contains("mortal_to_tianyuan"))) {
            // full ascension path has its own checks/loadout
            return AscensionService.attemptAscension(player, false);
        }
        if (!DimensionRegistryService.meetsEntryRealm(player, toDim)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.realm_too_low",
                    DimensionRegistryService.minRealmOf(toDim)), false);
            return false;
        }
        // yin cluster path rules
        if (YinUnderworldClusterService.isYinDimension(toDim)
                && !YinUnderworldClusterService.canEnter(player, toDim)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.yin_denied"), false);
            return false;
        }

        Optional<ServerLevel> targetOpt = DimensionRegistryService.resolveLevel(player, toDim);
        if (targetOpt.isEmpty()) {
            // fallback: region travel for known spirit hubs
            if (targetId.endsWith("tianyuan")) {
                return WorldpackGameplayService.travel(player, "tianyuan");
            }
            if (targetId.endsWith("spirit_fengyuan")) {
                return WorldpackGameplayService.travel(player, "spirit_fengyuan");
            }
            if (targetId.endsWith("yin_ming_pocket")) {
                return WorldpackGameplayService.travel(player, "yinming");
            }
            if (targetId.endsWith("nether_river_pocket")) {
                return WorldpackGameplayService.travel(player, "nether_river");
            }
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.dim_travel.missing_level", targetId), false);
            return false;
        }
        ServerLevel target = targetOpt.get();
        int x = 0;
        int z = 0;
        int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        y = Math.max(target.getMinBuildHeight() + 2, Math.min(target.getMaxBuildHeight() - 2, y));
        ensurePlatform(target, new BlockPos(x, y - 1, z));

        if (oneWay) {
            CultivationHelper.get(player).ifPresent(c -> c.clearWorldpackReturnLocation());
        } else {
            CultivationHelper.get(player).ifPresent(c -> c.setWorldpackReturnLocation(
                    player.level().dimension().location().toString(),
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        }

        player.teleportTo(target, x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        setCooldown(player, routeKey, cooldownFor(method));
        FlyingAuthorityPolicy.onDimensionChanged(player, targetId);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.dim_travel.success", targetId, method == null ? "" : method), true);
        return true;
    }

    private static long cooldownFor(String method) {
        if (method == null) {
            return DEFAULT_COOLDOWN_TICKS;
        }
        String key = method.toLowerCase(Locale.ROOT);
        if (key.contains("ascension")) {
            return 20L * 60L;
        }
        if (key.contains("teleport") || key.contains("array")) {
            return 20L * 20L;
        }
        if (key.contains("boat") || key.contains("ferry")) {
            return 20L * 15L;
        }
        return DEFAULT_COOLDOWN_TICKS;
    }

    private static void setCooldown(ServerPlayer player, String routeKey, long ticks) {
        var root = player.getPersistentData().getCompound(COOLDOWN_ROOT).copy();
        root.putLong(normalizeKey(routeKey), player.level().getGameTime() + Math.max(20L, ticks));
        player.getPersistentData().put(COOLDOWN_ROOT, root);
    }

    private static Optional<TravelMatrixEdge> findMatrixEdge(String fromDim, String toDim) {
        String from = normalizeDimToken(fromDim);
        String to = normalizeDimToken(toDim);
        for (TravelMatrixEdge edge : SNAPSHOT.matrix) {
            if (normalizeDimToken(edge.from()).equals(from) && normalizeDimToken(edge.to()).equals(to)) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
    }

    private static Optional<RouteDef> findRouteFor(String fromDim, String toDim, String methodHint) {
        String from = normalizeDimToken(fromDim);
        String to = normalizeDimToken(toDim);
        String method = methodHint == null ? "" : methodHint.trim().toLowerCase(Locale.ROOT);
        RouteDef fallback = null;
        for (RouteDef route : SNAPSHOT.routes.values()) {
            if (!normalizeDimToken(route.fromDimension()).equals(from)) {
                continue;
            }
            if (!normalizeDimToken(route.toDimension()).equals(to)) {
                continue;
            }
            if (!method.isBlank() && method.equals(route.method().toLowerCase(Locale.ROOT))) {
                return Optional.of(route);
            }
            if (fallback == null) {
                fallback = route;
            }
        }
        return Optional.ofNullable(fallback);
    }

    private static boolean isForbidden(String allowed) {
        if (allowed == null) {
            return false;
        }
        String key = allowed.toLowerCase(Locale.ROOT);
        return key.contains("forbidden") || key.contains("story_incarnation_only");
    }

    private static String methodFromAllowed(String allowed) {
        if (allowed == null) {
            return METHOD_TELEPORT_ARRAY;
        }
        String key = allowed.toLowerCase(Locale.ROOT);
        if (key.contains("ascension")) return METHOD_ASCENSION;
        if (key.contains("avatar")) return METHOD_AVATAR;
        if (key.contains("portal")) return METHOD_REGULATED;
        if (key.contains("ghost")) return METHOD_GHOST;
        if (key.contains("demon") || key.contains("event")) return METHOD_DEMON;
        if (key.contains("boat")) return METHOD_FLYING_BOAT;
        return METHOD_TELEPORT_ARRAY;
    }

    private static void ensurePlatform(ServerLevel level, BlockPos base) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(base.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(base.offset(dx, 1, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(base.offset(dx, 2, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static Snapshot load() {
        Map<String, MethodDef> methods = new LinkedHashMap<>();
        Map<String, RouteDef> routes = new LinkedHashMap<>();
        Map<String, CostDef> costs = new LinkedHashMap<>();
        List<TravelMatrixEdge> matrix = new ArrayList<>();

        JsonObject methodsRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/dimension_travel_methods_v136.json");
        if (methodsRoot != null) {
            for (JsonElement element : array(methodsRoot, "transport_methods_catalog")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                methods.put(id.toLowerCase(Locale.ROOT), new MethodDef(id, firstNonBlank(str(o, "display"), id), str(o, "note")));
            }
            for (JsonElement element : array(methodsRoot, "access_routes")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = firstNonBlank(str(o, "id"), str(o, "route_id"));
                if (id.isBlank()) continue;
                routes.put(id.toLowerCase(Locale.ROOT), new RouteDef(
                        id,
                        firstNonBlank(str(o, "from_dimension"), str(o, "from"), DimensionRegistryService.MORTAL_WORLD),
                        firstNonBlank(str(o, "to_dimension"), str(o, "to"), ""),
                        firstNonBlank(str(o, "method"), str(o, "transport"), METHOD_TELEPORT_ARRAY),
                        firstNonBlank(str(o, "realm_min"), str(o, "min_realm")),
                        o.has("one_way") && o.get("one_way").getAsBoolean(),
                        str(o, "gate"),
                        str(o, "allowed")));
            }
        }

        JsonObject costsRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/dimension_travel_costs_v137.json");
        if (costsRoot != null) {
            for (JsonElement element : array(costsRoot, "entries")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = firstNonBlank(str(o, "id"), str(o, "route"), str(o, "route_id"));
                if (id.isBlank()) continue;
                int min = intOf(o, "spirit_stone_min", intOf(o, "cost_min", 0));
                int max = intOf(o, "spirit_stone_max", intOf(o, "cost_max", min));
                double fail = 0.0D;
                if (o.has("fail") || o.has("fail_chance")) {
                    try {
                        fail = o.has("fail_chance") ? o.get("fail_chance").getAsDouble() : o.get("fail").getAsDouble();
                    } catch (Exception ignored) {
                    }
                }
                costs.put(id.toLowerCase(Locale.ROOT), new CostDef(id, str(o, "band"), min, max, fail));
            }
        }

        JsonObject catalog = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/dimensions_catalog.json");
        if (catalog != null) {
            for (JsonElement element : array(catalog, "travel_matrix")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                matrix.add(new TravelMatrixEdge(
                        str(o, "from"),
                        str(o, "to"),
                        str(o, "allowed"),
                        str(o, "gate"),
                        str(o, "realm_min")));
            }
        }

        if (methods.isEmpty()) {
            methods.put(METHOD_ASCENSION, new MethodDef(METHOD_ASCENSION, "飞升通道", "本体单向"));
            methods.put(METHOD_TELEPORT_ARRAY, new MethodDef(METHOD_TELEPORT_ARRAY, "固定传送阵", ""));
            methods.put(METHOD_FLYING_BOAT, new MethodDef(METHOD_FLYING_BOAT, "灵舟", ""));
            methods.put(METHOD_REGULATED, new MethodDef(METHOD_REGULATED, "管制界门", ""));
        }
        if (routes.isEmpty()) {
            routes.put("mortal_to_tianyuan", new RouteDef(
                    "mortal_to_tianyuan", DimensionRegistryService.MORTAL_WORLD, DimensionRegistryService.TIANYUAN,
                    METHOD_ASCENSION, "DEITY_TRANSFORMATION", true, "mortal_to_tianyuan", "ascension_only"));
            routes.put("tianyuan_to_fengyuan", new RouteDef(
                    "tianyuan_to_fengyuan", DimensionRegistryService.TIANYUAN, DimensionRegistryService.SPIRIT_FENGYUAN,
                    METHOD_REGULATED, "VOID_REFINEMENT", false, "tianyuan_to_spirit_fengyuan", "portal_fee"));
        }
        if (matrix.isEmpty()) {
            matrix.add(new TravelMatrixEdge(DimensionRegistryService.MORTAL_WORLD, DimensionRegistryService.TIANYUAN,
                    "ascension_only", "mortal_to_tianyuan", "DEITY_TRANSFORMATION"));
            matrix.add(new TravelMatrixEdge(DimensionRegistryService.TIANYUAN, DimensionRegistryService.SPIRIT_FENGYUAN,
                    "portal_fee", "tianyuan_to_spirit_fengyuan", "VOID_REFINEMENT"));
        }

        return new Snapshot(
                Collections.unmodifiableMap(methods),
                Collections.unmodifiableMap(routes),
                Collections.unmodifiableMap(costs),
                List.copyOf(matrix));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = DimensionTravelService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
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
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive()) return e.getAsString();
            return e.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int intOf(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) return fallback;
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private static String normalizeKey(String key) {
        return key == null ? "default" : key.trim().toLowerCase(Locale.ROOT).replace(':', '_');
    }

    private static String normalizeDimToken(String id) {
        String mc = DimensionRegistryService.toMinecraftDimensionId(id);
        if (DimensionRegistryService.OVERWORLD.equals(mc)) {
            return DimensionRegistryService.MORTAL_WORLD;
        }
        return mc;
    }
}
