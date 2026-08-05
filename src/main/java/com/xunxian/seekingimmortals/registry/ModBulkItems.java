package com.xunxian.seekingimmortals.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.item.CatalogCarrierItem;
import com.xunxian.seekingimmortals.item.CatalogConsumableItem;
import com.xunxian.seekingimmortals.item.CatalogEquipmentItem;
import com.xunxian.seekingimmortals.item.CatalogManualItem;
import com.xunxian.seekingimmortals.item.CatalogTalismanItem;
import com.xunxian.seekingimmortals.item.CatalogTalismanService;
import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyFormulaItem;
import com.xunxian.seekingimmortals.item.material.MaterialCategory;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
import com.xunxian.seekingimmortals.item.pill.BulkPillItem;
import com.xunxian.seekingimmortals.item.pill.PillQuality;
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
 * Unique story items are excluded from generic catalog loading; M04 registers
 * the palm bottle and green-liquid carriers explicitly below.
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

    /**
     * 0.2.267: ids that {@code ModItems} now owns as BlockItems for the authored single_core
     * stations. They were removed from {@code catalog_bulk_items.json}; this set keeps a catalog
     * regeneration that reintroduces one from crashing registration on a duplicate id.
     */
    private static final Set<String> BLOCK_OWNED_IDS = Set.of(
            "kunwu_copper_ore",
            "contribution_stele",
            "ownership_stele",
            "spirit_vein_tap",
            "ice_crystal_cooler",
            "scripture_pavilion_shelf",
            "pill_cabinet",
            "weapon_rack_artifact",
            "market_stall_counter",
            "inner_sect_task_board"
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
                if (BLOCK_OWNED_IDS.contains(id)) {
                    SeekingImmortalsMod.LOGGER.warn("Skipped bulk id owned by a registered block: {}", id);
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
            // Explicit carriers for M04's server-authoritative unique/quota service.
            registerGardenSpecial("palm_heaven_bottle", "artifact", "legendary", "掌天瓶（唯一，不可交易）");
            registerGardenSpecial("green_liquid_drop", "consumable", "epic", "绿液（年配额催熟）");
            SeekingImmortalsMod.LOGGER.info("Registered {} bulk catalog item carriers", IDS.size());
        } catch (Exception e) {
            SeekingImmortalsMod.LOGGER.error("Failed loading bulk catalog items", e);
        }
    }

    private static void registerGardenSpecial(String id, String category, String rarity, String description) {
        String rid = id.toLowerCase(Locale.ROOT);
        if (BY_ID.containsKey(rid)) {
            return;
        }
        RegistryObject<Item> reg = ITEMS.register(rid,
                () -> createItem(rid, category, rarity, description, ""));
        BY_ID.put(rid, reg);
        IDS.add(rid);
        CATEGORIES.put(rid, category.toLowerCase(Locale.ROOT));
    }

    private static Item createItem(String id, String category, String rarity, String description, String grade) {
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
        if ("artifact".equals(cat) || "equipment".equals(cat) || "palm_heaven_bottle".equals(id)) {
            props = props.stacksTo(1);
        } else if ("pill".equals(cat) || "consumable".equals(cat) || "talisman".equals(cat)) {
            props = props.stacksTo(16);
        }
        BulkItemKind kind = BulkItemClassifier.classify(id, cat);
        return switch (kind) {
            case FORMULA -> new AlchemyFormulaItem(props.stacksTo(1),
                    BulkItemClassifier.recipeOutput(id).orElse(id), BulkItemClassifier.formulaSource(id));
            case ARTIFACT -> new ArtifactCatalogItem(props.stacksTo(1), id);
            case CONSUMABLE -> {
                BulkItemClassifier.ConsumableDefinition definition =
                        BulkItemClassifier.consumable(id).orElseThrow();
                yield new CatalogConsumableItem(
                        definition.storageSlots() > 0 ? props.stacksTo(1) : props,
                        materialCategory,
                        materialRarity,
                        description,
                        definition);
            }
            case PILL -> {
                PillQuality quality = qualityFromId(id);
                yield new BulkPillItem(props, materialCategory, materialRarity, description,
                        id, quality);
            }
            case MANUAL -> new CatalogManualItem(props.stacksTo(16), id);
            case TALISMAN -> new CatalogTalismanItem(props.stacksTo(16), materialCategory, materialRarity, description,
                    id, grade, CatalogTalismanService.roleOf(id));
            case EQUIPMENT -> new CatalogEquipmentItem(props.stacksTo(1), materialCategory, materialRarity, description, id);
            case CARRIER -> new CatalogCarrierItem(props, materialCategory, materialRarity, description, id, grade);
        };
    }

    private static PillQuality qualityFromId(String id) {
        String path = id == null ? "" : id.toLowerCase(Locale.ROOT);
        if (path.endsWith("_supreme") || path.endsWith("_perfect")) {
            return PillQuality.PERFECT;
        }
        if (path.endsWith("_high")) {
            return PillQuality.HIGH;
        }
        if (path.endsWith("_mid") || path.endsWith("_middle")) {
            return PillQuality.MIDDLE;
        }
        return PillQuality.LOW;
    }
}
