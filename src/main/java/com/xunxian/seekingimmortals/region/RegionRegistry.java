package com.xunxian.seekingimmortals.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.worldpack.WorldpackDataService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.io.BufferedReader;
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
import java.util.Optional;
import java.util.Set;

/**
 * Authoritative region_id registry.
 * <p>Normalization plan (single source of truth):
 * <ul>
 *   <li>Runtime travel/aura/min_realm remain owned by {@code worldpack/regions.json}
 *       via {@link WorldpackDataService} (existing consumers keep working).</li>
 *   <li>Author cards under {@code text_material/region_cards/} enrich climate/factions/biomes
 *       and prove all 22 shipped cards are parseable.</li>
 *   <li>{@link RegionRegistry} is the only public region_id surface for downstream modules.
 *       Worldpack RegionCard stays as an internal travel DTO, not a second authority.</li>
 * </ul>
 */
public final class RegionRegistry {
    public static final String DEFAULT_REGION_ID = WorldpackDataService.builtin().findRegion("qinglan_mountains")
            .map(WorldpackDataService.RegionCard::id)
            .orElse("qinglan_mountains");

    private static final Snapshot BUILTIN = loadBuiltin();

    private RegionRegistry() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<RegionDefinition> find(String regionId) {
        return BUILTIN.find(regionId);
    }

    public static boolean isKnown(String regionId) {
        return BUILTIN.find(regionId).isPresent();
    }

    public static List<RegionDefinition> all() {
        return BUILTIN.all();
    }

    public static List<RegionDefinition> cards() {
        return BUILTIN.cards();
    }

    public static double auraMultiplier(String regionId) {
        return find(regionId).map(RegionDefinition::auraMultiplier).orElse(1.0D);
    }

    public static String resolveRegionId(Level level, BlockPos pos) {
        return resolveRegionId(level, pos, null);
    }

    public static String resolveRegionId(Level level, BlockPos pos, String preferredRegionId) {
        if (level == null) {
            return isKnown(preferredRegionId) ? preferredRegionId : DEFAULT_REGION_ID;
        }
        ResourceLocation dimension = level.dimension().location();
        Optional<String> byDimension = BUILTIN.regionForExactDimension(dimension.toString());
        Optional<String> byBiome = Optional.empty();
        if (pos != null) {
            Holder<Biome> biome = level.getBiome(pos);
            ResourceLocation biomeId = biome.unwrapKey().map(key -> key.location()).orElse(null);
            byBiome = RegionBiomeMap.builtin().regionForBiome(biomeId).filter(RegionRegistry::isKnown);
        }
        if (byDimension.isPresent()) {
            return reconcilePreferred(preferredRegionId, byDimension.get(), byBiome.orElse(""));
        }
        if ("minecraft".equals(dimension.getNamespace())
                && !"overworld".equals(dimension.getPath())) {
            return byBiome.orElse(DEFAULT_REGION_ID);
        }
        if (byBiome.isPresent()) {
            return byBiome.get();
        }
        return isKnown(preferredRegionId) ? preferredRegionId : DEFAULT_REGION_ID;
    }

    public static String resolveAndSync(ServerPlayer player) {
        if (player == null) {
            return DEFAULT_REGION_ID;
        }
        return com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).map(cultivation -> {
            String preferred = cultivation.getWorldpackCurrentRegionId();
            String resolved = resolveRegionId(player.level(), player.blockPosition(), preferred);
            if (!resolved.equals(preferred) && isKnown(resolved)) {
                cultivation.setWorldpackCurrentRegionId(resolved);
                com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor.expire(player);
                com.xunxian.seekingimmortals.sect.FactionConflictEventService.onDailyEvent(
                        player, resolved, "", 0L);
            }
            return resolved;
        }).orElse(DEFAULT_REGION_ID);
    }

    static String reconcilePreferred(String preferredRegionId, String dimensionRegionId, String biomeRegionId) {
        boolean preferredKnown = isKnown(preferredRegionId);
        boolean dimensionKnown = isKnown(dimensionRegionId);
        boolean biomeKnown = isKnown(biomeRegionId);
        if (dimensionKnown) {
            if (preferredKnown && sameDimensionFamily(preferredRegionId, dimensionRegionId)) {
                return preferredRegionId;
            }
            if (biomeKnown && sameDimensionFamily(biomeRegionId, dimensionRegionId)) {
                return biomeRegionId;
            }
            return dimensionRegionId;
        }
        if (biomeKnown) {
            return biomeRegionId;
        }
        return preferredKnown ? preferredRegionId : DEFAULT_REGION_ID;
    }

    public record Snapshot(Map<String, RegionDefinition> byId, List<String> cardIds) {
        public Snapshot {
            byId = byId == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(byId));
            cardIds = cardIds == null ? List.of() : List.copyOf(cardIds);
        }

        public Optional<RegionDefinition> find(String regionId) {
            if (regionId == null || regionId.isBlank()) {
                return Optional.empty();
            }
            RegionDefinition direct = byId.get(regionId);
            if (direct != null) {
                return Optional.of(direct);
            }
            for (Map.Entry<String, RegionDefinition> entry : byId.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(regionId.trim())) {
                    return Optional.of(entry.getValue());
                }
            }
            return Optional.empty();
        }

        public List<RegionDefinition> all() {
            return List.copyOf(byId.values());
        }

        public List<RegionDefinition> cards() {
            List<RegionDefinition> list = new ArrayList<>();
            for (String id : cardIds) {
                RegionDefinition definition = byId.get(id);
                if (definition != null) {
                    list.add(definition);
                }
            }
            return List.copyOf(list);
        }

        public int size() {
            return byId.size();
        }

        public int cardCount() {
            return cardIds.size();
        }

        public Optional<String> regionForDimension(String dimensionId) {
            if (dimensionId == null || dimensionId.isBlank()) {
                return Optional.empty();
            }
            String key = dimensionId.trim().toLowerCase(Locale.ROOT);
            for (RegionDefinition definition : byId.values()) {
                if (definition.dimensionId().equalsIgnoreCase(key)) {
                    return Optional.of(definition.id());
                }
            }
            // featured dimension path match
            for (RegionDefinition definition : byId.values()) {
                String dim = definition.dimensionId().toLowerCase(Locale.ROOT);
                if (dim.endsWith(":" + key) || key.endsWith(definition.id()) || dim.contains(definition.id())) {
                    return Optional.of(definition.id());
                }
            }
            return Optional.empty();
        }

        public Optional<String> regionForExactDimension(String dimensionId) {
            if (dimensionId == null || dimensionId.isBlank()) {
                return Optional.empty();
            }
            String key = dimensionId.trim().toLowerCase(Locale.ROOT);
            ResourceLocation location = ResourceLocation.tryParse(key);
            String path = location == null ? "" : location.getPath();
            String fallback = "";
            for (RegionDefinition definition : byId.values()) {
                if (!definition.dimensionId().equalsIgnoreCase(key)) {
                    continue;
                }
                if (definition.id().equalsIgnoreCase(path)) {
                    return Optional.of(definition.id());
                }
                if (fallback.isBlank()) {
                    fallback = definition.id();
                }
            }
            return fallback.isBlank() ? Optional.empty() : Optional.of(fallback);
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, MutableRegion> builders = new LinkedHashMap<>();
        List<String> cardIds = new ArrayList<>();

        // 1) worldpack runtime regions (travel/aura authority)
        for (WorldpackDataService.RegionCard card : WorldpackDataService.builtin().regions()) {
            MutableRegion mutable = builders.computeIfAbsent(card.id(), MutableRegion::new);
            mutable.displayZh = firstNonBlank(card.displayZh(), mutable.displayZh);
            mutable.displayEn = firstNonBlank(card.displayEn(), mutable.displayEn);
            mutable.auraMultiplier = card.auraMultiplier() > 0.0D ? card.auraMultiplier() : mutable.auraMultiplier;
            mutable.minRealm = firstNonBlank(card.minRealm(), mutable.minRealm);
            mutable.travelAnchor = firstNonBlank(card.travelAnchor(), mutable.travelAnchor);
            mutable.tags.addAll(card.tags());
            mutable.hasWorldpack = true;
            mutable.dimensionId = inferDimension(card.id(), card.tags(), mutable.dimensionId);
        }

        // 2) author region cards
        List<String> stems = loadCardStems();
        List<String> loadedCardIds = new ArrayList<>();
        List<String> loadedCardStems = new ArrayList<>();
        for (String stem : stems) {
            JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/region_cards/" + stem + ".json");
            if (root == null) {
                continue;
            }
            String id = firstNonBlank(str(root, "id"), stem);
            if (id.isBlank()) {
                continue;
            }
            if (!loadedCardStems.contains(stem)) {
                loadedCardStems.add(stem);
            }
            if (!loadedCardIds.contains(id)) {
                loadedCardIds.add(id);
            }
            applyCard(builders, id, root, true);
            // File stem may differ from declared id (e.g. extreme_west_thousand_bamboo -> extreme_west).
            // Keep both ids addressable for worldpack + author consumers.
            if (!stem.equals(id)) {
                applyCard(builders, stem, root, true);
                if (!loadedCardIds.contains(stem)) {
                    loadedCardIds.add(stem);
                }
            }
        }

        // 3) biome map enrichment
        RegionBiomeMap.Snapshot biomes = RegionBiomeMap.builtin();
        for (RegionBiomeMap.BiomeBinding binding : biomes.bindings()) {
            MutableRegion mutable = builders.computeIfAbsent(binding.regionId(), MutableRegion::new);
            if (!mutable.biomes.contains(binding.biomeId())) {
                mutable.biomes.add(binding.biomeId());
            }
            if (mutable.displayZh.isBlank()) {
                mutable.displayZh = binding.display();
            }
        }

        Map<String, RegionDefinition> byId = new LinkedHashMap<>();
        for (MutableRegion mutable : builders.values()) {
            if (mutable.id.isBlank()) {
                continue;
            }
            if (mutable.displayZh.isBlank()) {
                mutable.displayZh = mutable.id;
            }
            if (mutable.dimensionId.isBlank()) {
                mutable.dimensionId = inferDimension(mutable.id, mutable.tags, "minecraft:overworld");
            }
            byId.put(mutable.id, mutable.toDefinition());
        }
        // Prefer file stems for card count/identity (22 shipped files), keep declared ids addressable too.
        List<String> orderedCards = !loadedCardStems.isEmpty() ? loadedCardStems : loadedCardIds;
        return new Snapshot(byId, orderedCards);
    }

    private static void applyCard(Map<String, MutableRegion> builders, String id, JsonObject root, boolean hasCard) {
        MutableRegion mutable = builders.computeIfAbsent(id, MutableRegion::new);
        mutable.hasCard = mutable.hasCard || hasCard;
        mutable.displayZh = firstNonBlank(str(root, "display"), mutable.displayZh);
        mutable.climate = firstNonBlank(str(root, "climate"), mutable.climate);
        mutable.auraMultiplier = parseDensity(root.get("spirit_density"), mutable.auraMultiplier);
        mutable.minRealm = firstNonBlank(str(root, "realm_min"), firstNonBlank(str(root, "realm_typical"), mutable.minRealm));
        mutable.factions.addAll(strings(root, "factions"));
        mutable.biomes.addAll(strings(root, "minecraft_biomes_hint"));
        mutable.dailyEventIds.addAll(strings(root, "daily_events"));
        mutable.dailyEventIds.addAll(strings(root, "daily_events_ref"));
        String parent = firstNonBlank(str(root, "parent_region"), str(root, "dimension_parent"));
        if (!parent.isBlank()) {
            mutable.dimensionId = resolveDimensionHint(parent, mutable.dimensionId);
            if (!parent.contains(":")) {
                mutable.tags.add("parent:" + parent);
            }
        }
        String parentCluster = str(root, "parent_cluster");
        if (!parentCluster.isBlank()) {
            mutable.tags.add("cluster:" + parentCluster);
            mutable.dimensionId = resolveDimensionHint(parentCluster, mutable.dimensionId);
        }
        if (mutable.travelAnchor.isBlank()) {
            mutable.travelAnchor = id + "_anchor";
        }
    }

    private static List<String> loadCardStems() {
        // Prefer index.json cards[].id / file, then known directory listing via classpath is not enumerable;
        // so we use index + a fixed fallback of expected stems discovered at build time via worldpack overlap.
        LinkedHashSet<String> stems = new LinkedHashSet<>();
        JsonObject index = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/region_cards/index.json");
        if (index != null) {
            for (JsonElement element : array(index, "cards")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String file = str(object, "file");
                if (!file.isBlank()) {
                    stems.add(file.endsWith(".json") ? file.substring(0, file.length() - 5) : file);
                } else {
                    String id = str(object, "id");
                    if (!id.isBlank()) {
                        stems.add(id);
                    }
                }
            }
        }
        // Full shipped card set (22). Keep explicit so classpath-only environments still load all cards.
        for (String stem : List.of(
                "barbarian_wasteland", "chaotic_sea", "dajin", "extreme_west_thousand_bamboo",
                "fallen_demon_valley", "great_jin_central", "inverse_star_hideout", "kunwu",
                "mulan", "mulan_grassland", "nether_river", "outer_sea_market", "qixuan_village",
                "spirit_fengyuan", "spirit_realm_border", "star_palace_city", "tianlan", "tiannan",
                "tiannan_north_waste", "tianyuan", "wutu_border", "yinming")) {
            stems.add(stem);
        }
        return List.copyOf(stems);
    }

    private static String knownOrDefault(String id) {
        return isKnown(id) ? id : DEFAULT_REGION_ID;
    }

    private static boolean sameDimensionFamily(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equalsIgnoreCase(b)) {
            return true;
        }
        String da = find(a).map(RegionDefinition::dimensionId).orElse("");
        String db = find(b).map(RegionDefinition::dimensionId).orElse("");
        return !da.isBlank() && da.equalsIgnoreCase(db);
    }

    private static String inferDimension(String regionId, Iterable<String> tags, String fallback) {
        String id = regionId == null ? "" : regionId.toLowerCase(Locale.ROOT);
        if (id.contains("tianyuan") || id.contains("spirit_realm_border")) {
            return "seeking_immortals:tianyuan";
        }
        if (id.contains("spirit_fengyuan") || id.contains("barbarian_wasteland")) {
            return "seeking_immortals:spirit_fengyuan";
        }
        if (id.contains("yinming") || id.contains("yin_ming")) {
            return "seeking_immortals:yin_ming_pocket";
        }
        if (id.contains("nether_river")) {
            return "seeking_immortals:nether_river_pocket";
        }
        if (id.contains("fallen_demon") || id.contains("demon_rift")) {
            return "seeking_immortals:demon_rift";
        }
        if (tags != null) {
            for (String tag : tags) {
                if (tag == null) {
                    continue;
                }
                String t = tag.toLowerCase(Locale.ROOT);
                if (t.contains("spirit_realm")) {
                    return "seeking_immortals:tianyuan";
                }
                if (t.contains("demon_rift")) {
                    return "seeking_immortals:demon_rift";
                }
                if (t.contains("yin") || t.contains("underworld")) {
                    return "seeking_immortals:yin_ming_pocket";
                }
            }
        }
        return firstNonBlank(fallback, "minecraft:overworld");
    }

    private static String resolveDimensionHint(String hint, String fallback) {
        if (hint == null || hint.isBlank()) {
            return fallback;
        }
        if (hint.contains(":")) {
            return hint;
        }
        String key = hint.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "spirit_realm", "tianyuan" -> "seeking_immortals:tianyuan";
            case "spirit_fengyuan", "fengyuan" -> "seeking_immortals:spirit_fengyuan";
            case "yin_underworld_cluster", "yinming", "yin_ming" -> "seeking_immortals:yin_ming_pocket";
            case "nether_river", "nether_river_cluster" -> "seeking_immortals:nether_river_pocket";
            case "demon_rift", "fallen_demon_valley" -> "seeking_immortals:demon_rift";
            case "chaotic_sea", "mortal_world", "tiannan", "dajin", "mulan" -> "minecraft:overworld";
            default -> inferDimension(key, List.of(), fallback);
        };
    }

    private static double parseDensity(JsonElement element, double fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback > 0.0D ? fallback : 1.0D;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            double value = element.getAsDouble();
            return value > 0.0D ? value : fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String token = element.getAsString().trim().toLowerCase(Locale.ROOT);
            return switch (token) {
                case "low", "low_corrupt" -> 0.7D;
                case "low_mid", "yin" -> 0.85D;
                case "mid", "medium" -> 1.0D;
                case "mid_high" -> 1.15D;
                case "high" -> 1.4D;
                case "yin_heavy" -> 0.65D;
                case "very_high", "dense" -> 1.8D;
                default -> {
                    try {
                        double parsed = Double.parseDouble(token);
                        yield parsed > 0.0D ? parsed : fallback;
                    } catch (NumberFormatException ignored) {
                        yield fallback > 0.0D ? fallback : 1.0D;
                    }
                }
            };
        }
        return fallback > 0.0D ? fallback : 1.0D;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = RegionRegistry.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load region resource {}", path, exception);
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static List<String> strings(JsonObject object, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(object, key)) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static String str(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }

    private static final class MutableRegion {
        private final String id;
        private String displayZh = "";
        private String displayEn = "";
        private double auraMultiplier = 1.0D;
        private String minRealm = "qi_refining";
        private String travelAnchor = "";
        private String dimensionId = "minecraft:overworld";
        private String climate = "";
        private final LinkedHashSet<String> biomes = new LinkedHashSet<>();
        private final LinkedHashSet<String> factions = new LinkedHashSet<>();
        private final LinkedHashSet<String> tags = new LinkedHashSet<>();
        private final LinkedHashSet<String> dailyEventIds = new LinkedHashSet<>();
        private boolean hasCard;
        private boolean hasWorldpack;

        private MutableRegion(String id) {
            this.id = id == null ? "" : id;
            this.travelAnchor = this.id.isBlank() ? "" : this.id + "_anchor";
        }

        private RegionDefinition toDefinition() {
            return new RegionDefinition(
                    id,
                    displayZh,
                    displayEn,
                    auraMultiplier,
                    minRealm,
                    travelAnchor,
                    dimensionId,
                    climate,
                    List.copyOf(biomes),
                    List.copyOf(factions),
                    List.copyOf(tags),
                    List.copyOf(dailyEventIds),
                    hasCard,
                    hasWorldpack);
        }
    }
}
