package com.xunxian.seekingimmortals.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.item.CatalogCarrierItem;
import com.xunxian.seekingimmortals.item.material.MaterialCategory;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Data-driven bulk carriers for text-material catalog ids that lack dedicated Java fields.
 * Loaded from assets/seeking_immortals/catalog_bulk_items.json at class init.
 * <p>
 * M03: sole bulk registration pipeline for catalog item-id authority.
 * Unique story items (掌天瓶/绿液) are never registered here.
 */
public final class ModBulkItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SeekingImmortalsMod.MODID);

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

    private static final Map<String, RegistryObject<Item>> BY_ID = new LinkedHashMap<>();
    private static final List<String> IDS = new ArrayList<>();
    private static final Map<String, String> GRADES = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();

    static {
        loadAndRegister();
    }

    private ModBulkItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static Map<String, RegistryObject<Item>> byId() {
        return Collections.unmodifiableMap(BY_ID);
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(IDS);
    }

    public static int size() {
        return IDS.size();
    }

    public static String gradeOf(String id) {
        if (id == null) {
            return "";
        }
        return GRADES.getOrDefault(id.trim().toLowerCase(Locale.ROOT), "");
    }

    public static String categoryOf(String id) {
        if (id == null) {
            return "";
        }
        return CATEGORIES.getOrDefault(id.trim().toLowerCase(Locale.ROOT), "");
    }

    private static void loadAndRegister() {
        String path = "/assets/" + SeekingImmortalsMod.MODID + "/catalog_bulk_items.json";
        try (InputStream in = ModBulkItems.class.getResourceAsStream(path)) {
            if (in == null) {
                SeekingImmortalsMod.LOGGER.warn("Bulk item catalog missing: {}", path);
                return;
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                return;
            }
            JsonArray arr = root.getAsJsonObject().getAsJsonArray("items");
            if (arr == null) {
                return;
            }
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String id = o.has("id") ? o.get("id").getAsString() : "";
                if (id == null || id.isBlank()) continue;
                id = id.trim().toLowerCase(Locale.ROOT);
                if (UNIQUE_FORBIDDEN.contains(id)) {
                    SeekingImmortalsMod.LOGGER.warn("Skipped unique forbidden bulk id: {}", id);
                    continue;
                }
                if (BY_ID.containsKey(id)) continue;
                String category = o.has("category") ? o.get("category").getAsString() : "material";
                String rarity = o.has("rarity") ? o.get("rarity").getAsString() : "common";
                String desc = o.has("description") ? o.get("description").getAsString() : ("目录载体：" + id);
                String grade = o.has("grade") ? o.get("grade").getAsString() : "";
                final String rid = id;
                final String cat = category;
                final String rar = rarity;
                final String description = desc;
                final String itemGrade = grade == null ? "" : grade;
                RegistryObject<Item> reg = ITEMS.register(rid,
                        () -> createItem(rid, cat, rar, description, itemGrade));
                BY_ID.put(rid, reg);
                IDS.add(rid);
                CATEGORIES.put(rid, cat == null ? "material" : cat.toLowerCase(Locale.ROOT));
                if (itemGrade != null && !itemGrade.isBlank()) {
                    GRADES.put(rid, itemGrade.toLowerCase(Locale.ROOT));
                }
            }
            SeekingImmortalsMod.LOGGER.info("Registered {} bulk catalog item carriers", IDS.size());
        } catch (Exception e) {
            SeekingImmortalsMod.LOGGER.error("Failed loading bulk catalog items", e);
        }
    }

    private static Item createItem(String catalogId, String category, String rarity, String description, String grade) {
        String cat = category == null ? "material" : category.toLowerCase(Locale.ROOT);
        MaterialCategory materialCategory = switch (cat) {
            case "pill", "consumable" -> MaterialCategory.SPIRITUAL_HERB;
            case "talisman", "manual", "formula", "technique" -> MaterialCategory.SPECIAL;
            case "artifact", "equipment" -> MaterialCategory.SPECIAL;
            case "currency", "access_item" -> MaterialCategory.SPECIAL;
            case "mineral", "ore" -> MaterialCategory.MINERAL;
            case "beast", "beast_material" -> MaterialCategory.BEAST_MATERIAL;
            case "herb" -> MaterialCategory.SPIRITUAL_HERB;
            default -> MaterialCategory.SPECIAL;
        };
        MaterialRarity materialRarity = switch ((rarity == null ? "common" : rarity).toLowerCase(Locale.ROOT)) {
            case "uncommon" -> MaterialRarity.UNCOMMON;
            case "rare" -> MaterialRarity.RARE;
            case "epic" -> MaterialRarity.EPIC;
            case "legendary" -> MaterialRarity.LEGENDARY;
            default -> MaterialRarity.COMMON;
        };
        Item.Properties props = new Item.Properties();
        if (materialRarity == MaterialRarity.UNCOMMON) props = props.rarity(Rarity.UNCOMMON);
        if (materialRarity == MaterialRarity.RARE) props = props.rarity(Rarity.RARE);
        if (materialRarity == MaterialRarity.EPIC || materialRarity == MaterialRarity.LEGENDARY) {
            props = props.rarity(Rarity.EPIC);
        }
        if ("artifact".equals(cat) || "equipment".equals(cat)) {
            props = props.stacksTo(1);
        } else if ("pill".equals(cat) || "consumable".equals(cat) || "talisman".equals(cat)) {
            props = props.stacksTo(16);
        }
        return new CatalogCarrierItem(props, materialCategory, materialRarity, description, catalogId, grade);
    }
}
