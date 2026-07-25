package com.xunxian.seekingimmortals.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.registry.ModBulkItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M03 item-id authority: alias resolution + bulk carrier metadata.
 * Downstream modules should call {@link #resolveCatalogItem(String)} instead of hardcoding registry names.
 */
public final class ItemCatalogService {
    /** Unique story items that must never enter bulk/trade registration channels. */
    private static final Set<String> UNIQUE_FORBIDDEN = Set.of(
            "palm_heaven_bottle",
            "palm_sky_bottle",
            "heaven_palm_vase",
            "green_liquid",
            "lv_ye",
            "garden_liquid",
            "little_green_bottle",
            "mystic_green_liquid"
    );

    // Must initialize after UNIQUE_FORBIDDEN (loadBuiltin reads that set).
    private static final Snapshot BUILTIN = loadBuiltin();

    private ItemCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    /**
     * Resolve a catalog id or alias to a registered item.
     * Accepts bare ids ({@code yellow_essence}) or namespaced ids ({@code seeking_immortals:yellow_essence}).
     */
    @Nullable
    public static Item resolveCatalogItem(String idOrAlias) {
        String canonical = resolveId(idOrAlias);
        if (canonical == null || canonical.isBlank()) {
            return null;
        }
        if (isUniqueForbidden(canonical)) {
            return null;
        }

        // Entire registry lookup is guarded: pure unit tests and early bootstrap do not
        // have ForgeRegistries initialized, and even reading ForgeRegistries.ITEMS can
        // throw ExceptionInInitializerError / NoClassDefFoundError.
        try {
            RegistryObject<Item> bulk = ModBulkItems.byId().get(canonical);
            if (bulk != null && bulk.isPresent()) {
                return bulk.get();
            }
        } catch (Throwable ignored) {
            // fall through to direct registry lookup
        }

        try {
            ResourceLocation location = ResourceLocation.tryParse(SeekingImmortalsMod.MODID + ":" + canonical);
            if (location == null || ForgeRegistries.ITEMS == null) {
                return null;
            }
            Item item = ForgeRegistries.ITEMS.getValue(location);
            if (item == null || item == Items.AIR) {
                return null;
            }
            return item;
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * Canonical bare item path (no namespace) after alias collapse.
     */
    @Nullable
    public static String resolveId(String idOrAlias) {
        if (idOrAlias == null || idOrAlias.isBlank()) {
            return null;
        }
        String raw = idOrAlias.trim().toLowerCase(Locale.ROOT);
        if (raw.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(raw);
            if (parsed == null) {
                return null;
            }
            raw = parsed.getPath();
        }
        String current = raw;
        // Collapse short alias chains (A -> B -> C).
        for (int i = 0; i < 8; i++) {
            String next = BUILTIN.aliases().get(current);
            if (next == null || next.isBlank() || next.equals(current)) {
                break;
            }
            current = next.toLowerCase(Locale.ROOT);
        }
        return current;
    }

    public static Optional<CarrierMeta> findMeta(String idOrAlias) {
        String id = resolveId(idOrAlias);
        if (id == null) {
            return Optional.empty();
        }
        CarrierMeta meta = BUILTIN.carriers().get(id);
        return Optional.ofNullable(meta);
    }

    public static boolean isUniqueForbidden(String idOrAlias) {
        String id = resolveId(idOrAlias);
        return id != null && UNIQUE_FORBIDDEN.contains(id);
    }

    public static boolean isKnownAlias(String idOrAlias) {
        if (idOrAlias == null || idOrAlias.isBlank()) {
            return false;
        }
        String raw = idOrAlias.trim().toLowerCase(Locale.ROOT);
        if (raw.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(raw);
            raw = parsed == null ? raw : parsed.getPath();
        }
        return BUILTIN.aliases().containsKey(raw);
    }

    public record CarrierMeta(String id, String category, String rarity, String grade, String description) {
        public CarrierMeta {
            id = id == null ? "" : id;
            category = category == null ? "material" : category;
            rarity = rarity == null ? "common" : rarity;
            grade = grade == null ? "" : grade;
            description = description == null ? "" : description;
        }

        public boolean hasGrade() {
            return grade != null && !grade.isBlank();
        }
    }

    public record Snapshot(Map<String, String> aliases, Map<String, CarrierMeta> carriers) {
        public Snapshot {
            aliases = aliases == null ? Map.of() : Collections.unmodifiableMap(aliases);
            carriers = carriers == null ? Map.of() : Collections.unmodifiableMap(carriers);
        }

        public int aliasCount() {
            return aliases.size();
        }

        public int carrierCount() {
            return carriers.size();
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, String> aliases = new LinkedHashMap<>();
        Map<String, CarrierMeta> carriers = new LinkedHashMap<>();

        JsonObject aliasRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/item_id_aliases.json");
        if (aliasRoot != null) {
            JsonArray arr = aliasRoot.has("aliases") && aliasRoot.get("aliases").isJsonArray()
                    ? aliasRoot.getAsJsonArray("aliases") : new JsonArray();
            for (JsonElement element : arr) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String alias = str(o, "alias").toLowerCase(Locale.ROOT);
                String canonical = str(o, "canonical").toLowerCase(Locale.ROOT);
                if (!alias.isBlank() && !canonical.isBlank()) {
                    aliases.put(alias, canonical);
                }
            }
        }

        // Hard aliases for display block ids that map to existing functional blocks/items.
        putAlias(aliases, "alchemy_furnace_g1", "alchemy_furnace");
        putAlias(aliases, "refinement_forge_g1", "refinement_forge");
        putAlias(aliases, "yin_essence_ore_block", "yin_essence_ore");
        putAlias(aliases, "earth_fire_alchemy_room", "sect_earth_fire_room");
        // Multiblock/economy currency and component aliases.
        putAlias(aliases, "spirit_stone_low", "low_spirit_stone");
        putAlias(aliases, "spirit_stone_mid", "mid_spirit_stone");
        putAlias(aliases, "spirit_stone_high", "high_spirit_stone");
        putAlias(aliases, "spirit_stone", "spirit_stone_shard");
        putAlias(aliases, "low_spirit_stone_shard", "spirit_stone_shard");
        putAlias(aliases, "spirit_shard", "spirit_stone_shard");
        putAlias(aliases, "jade_immortal", "immortal_jade");
        putAlias(aliases, "xuan_iron", "black_iron");
        putAlias(aliases, "iron_wood", "ironwood");
        putAlias(aliases, "refinement_forge_g2", "refinement_forge");
        putAlias(aliases, "refinement_forge_g3", "refinement_forge");
        putAlias(aliases, "alchemy_furnace_g2", "alchemy_furnace");
        putAlias(aliases, "alchemy_furnace_g3", "alchemy_furnace");
        // Playable v141 quest rewards reuse registered compatibility carriers.
        putAlias(aliases, "blood_forbidden_token", "mortal_quest_token");
        putAlias(aliases, "black_jiao_sinew", "beast_tendon");
        putAlias(aliases, "jiao_pearl", "water_pearl");
        putAlias(aliases, "court_warrant_gray", "bounty_token");
        putAlias(aliases, "zhui_mo_ling", "fallen_demon_scout_report");
        putAlias(aliases, "lingzhu_fruit", "fire_spirit_fruit");
        putAlias(aliases, "yin_zhi_horse_live", "beast_contract_token");
        putAlias(aliases, "peiying_dan", "nascent_soul_pill");
        putAlias(aliases, "puppet_core_embryo_broken", "puppet_core_spirit");

        JsonObject bulkRoot = readJson("assets/" + SeekingImmortalsMod.MODID + "/catalog_bulk_items.json");
        if (bulkRoot != null) {
            JsonArray items = bulkRoot.has("items") && bulkRoot.get("items").isJsonArray()
                    ? bulkRoot.getAsJsonArray("items") : new JsonArray();
            for (JsonElement element : items) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id").toLowerCase(Locale.ROOT);
                if (id.isBlank() || UNIQUE_FORBIDDEN.contains(id)) {
                    continue;
                }
                carriers.put(id, new CarrierMeta(
                        id,
                        str(o, "category"),
                        str(o, "rarity"),
                        str(o, "grade"),
                        str(o, "description")
                ));
            }
        }

        return new Snapshot(aliases, carriers);
    }

    private static void putAlias(Map<String, String> aliases, String alias, String canonical) {
        if (alias == null || canonical == null || alias.isBlank() || canonical.isBlank()) {
            return;
        }
        aliases.putIfAbsent(alias.toLowerCase(Locale.ROOT), canonical.toLowerCase(Locale.ROOT));
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = ItemCatalogService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(o.get(key));
        }
    }
}
