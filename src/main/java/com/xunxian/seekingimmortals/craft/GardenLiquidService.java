package com.xunxian.seekingimmortals.craft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Server-authoritative 掌天瓶 / 绿液 year quota (M04 redline).
 * <p>Rules from {@code garden_liquid_calendar_v108.json}:
 * <ul>
 *   <li>bottle is unique + non-tradable (enforced on stack + inventory scan)</li>
 *   <li>year cap cannot be disabled (standard difficulty default = 6)</li>
 *   <li>催熟 cannot bypass the annual cap</li>
 * </ul>
 */
public final class GardenLiquidService {
    public static final String BOTTLE_ITEM_ID = "palm_heaven_bottle";
    public static final String LIQUID_ITEM_ID = "green_liquid_drop";
    public static final String UNIQUE_TAG = "SeekingImmortalsPalmBottle";
    public static final String OWNER_TAG = "OwnerUUID";
    private static final String ROOT = "seeking_immortals_garden_liquid";
    private static final String YEAR_KEY = "year";
    private static final String USED_KEY = "used";
    private static final String COOLDOWN_KEY = "cooldownUntilDay";

    private static final int DEFAULT_YEAR_CAP = 6;
    private static final int DEFAULT_COOLDOWN_DAYS = 20;

    private static volatile int yearCap = DEFAULT_YEAR_CAP;
    private static volatile int cooldownDays = DEFAULT_COOLDOWN_DAYS;
    private static volatile boolean loaded;

    private GardenLiquidService() {}

    public static int yearCap() {
        ensureLoaded();
        return yearCap;
    }

    public static int usedThisYear(ServerPlayer player) {
        CompoundTag tag = root(player);
        int year = currentYear();
        if (tag.getInt(YEAR_KEY) != year) {
            return 0;
        }
        return Math.max(0, tag.getInt(USED_KEY));
    }

    public static int remainingThisYear(ServerPlayer player) {
        return Math.max(0, yearCap() - usedThisYear(player));
    }

    public static boolean isBottle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isId(stack.getItem(), BOTTLE_ITEM_ID) || stack.getOrCreateTag().getBoolean(UNIQUE_TAG);
    }

    public static boolean isLiquid(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isId(stack.getItem(), LIQUID_ITEM_ID);
    }

    public static Item bottleItem() {
        Item item = ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation(SeekingImmortalsMod.MODID, BOTTLE_ITEM_ID));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            // fallback vase carrier if dedicated item missing
            item = ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation(SeekingImmortalsMod.MODID, "spirit_nurture_green_vase"));
        }
        return item;
    }

    public static Item liquidItem() {
        return ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation(SeekingImmortalsMod.MODID, LIQUID_ITEM_ID));
    }

    /** Bind unique owner on first possession; strip extras. */
    public static void enforceUniqueBottle(ServerPlayer player) {
        Item bottle = bottleItem();
        if (bottle == null || bottle == net.minecraft.world.item.Items.AIR) {
            return;
        }
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(bottle) && !isBottle(stack)) {
                continue;
            }
            found++;
            if (found == 1) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putBoolean(UNIQUE_TAG, true);
                if (!tag.hasUUID(OWNER_TAG)) {
                    tag.putUUID(OWNER_TAG, player.getUUID());
                } else if (!player.getUUID().equals(tag.getUUID(OWNER_TAG))
                        && !player.getAbilities().instabuild) {
                    // Not owner: confiscate
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.garden_liquid.bottle_not_owner"), true);
                    found--;
                }
                if (stack.getCount() > 1) {
                    stack.setCount(1);
                }
            } else {
                // Extra bottles are destroyed (unique redline).
                player.getInventory().setItem(i, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.garden_liquid.bottle_unique"), true);
            }
        }
    }

    /**
     * Spend one green-liquid charge for herb acceleration.
     * @return true if quota consumed
     */
    public static boolean tryConsumeLiquidCharge(ServerPlayer player, boolean forceMessage) {
        ensureLoaded();
        if (player.getAbilities().instabuild) {
            return true;
        }
        CompoundTag tag = root(player);
        int year = currentYear();
        if (tag.getInt(YEAR_KEY) != year) {
            tag.putInt(YEAR_KEY, year);
            tag.putInt(USED_KEY, 0);
            tag.putInt(COOLDOWN_KEY, 0);
        }
        int day = currentDayOfYear();
        if (tag.getInt(COOLDOWN_KEY) > day) {
            if (forceMessage) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.garden_liquid.cooldown",
                        tag.getInt(COOLDOWN_KEY) - day), true);
            }
            return false;
        }
        int used = tag.getInt(USED_KEY);
        if (used >= yearCap) {
            if (forceMessage) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.garden_liquid.year_cap", yearCap), true);
            }
            return false;
        }
        // Prefer consuming a liquid drop item if present; otherwise bottle-only charge.
        if (!consumeOneLiquidItem(player) && !hasBoundBottle(player)) {
            if (forceMessage) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.garden_liquid.missing"), true);
            }
            return false;
        }
        tag.putInt(USED_KEY, used + 1);
        tag.putInt(COOLDOWN_KEY, day + cooldownDays);
        player.getPersistentData().put(ROOT, tag);
        if (forceMessage) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.garden_liquid.used",
                    used + 1, yearCap), true);
        }
        return true;
    }

    public static boolean hasBoundBottle(ServerPlayer player) {
        Item bottle = bottleItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if ((bottle != null && stack.is(bottle)) || isBottle(stack)) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.hasUUID(OWNER_TAG) && player.getUUID().equals(tag.getUUID(OWNER_TAG))) {
                    return true;
                }
                if (tag != null && tag.getBoolean(UNIQUE_TAG)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean consumeOneLiquidItem(ServerPlayer player) {
        Item liquid = liquidItem();
        if (liquid == null || liquid == net.minecraft.world.item.Items.AIR) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(liquid)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag tag = data.getCompound(ROOT).copy();
        data.put(ROOT, tag);
        return tag;
    }

    private static int currentYear() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return cal.get(Calendar.YEAR);
    }

    private static int currentDayOfYear() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isId(Item item, String path) {
        var key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && SeekingImmortalsMod.MODID.equals(key.getNamespace()) && path.equals(key.getPath());
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (GardenLiquidService.class) {
            if (loaded) {
                return;
            }
            String path = "data/" + SeekingImmortalsMod.MODID + "/text_material/garden_liquid_calendar_v108.json";
            try (InputStream in = GardenLiquidService.class.getClassLoader().getResourceAsStream(path)) {
                if (in != null) {
                    JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (root.has("bottle_rules") && root.get("bottle_rules").isJsonObject()) {
                        JsonObject rules = root.getAsJsonObject("bottle_rules");
                        if (rules.has("year_caps") && rules.get("year_caps").isJsonObject()) {
                            JsonObject caps = rules.getAsJsonObject("year_caps");
                            if (caps.has("standard")) {
                                yearCap = Math.max(1, caps.get("standard").getAsInt());
                            }
                        }
                        if (rules.has("cooldown_days_structure") && rules.get("cooldown_days_structure").isJsonArray()
                                && rules.getAsJsonArray("cooldown_days_structure").size() > 0) {
                            cooldownDays = Math.max(1, rules.getAsJsonArray("cooldown_days_structure").get(0).getAsInt());
                        }
                    }
                    if (root.has("year_calendar_standard") && root.get("year_calendar_standard").isJsonObject()) {
                        JsonObject cal = root.getAsJsonObject("year_calendar_standard");
                        if (cal.has("liquid_per_year")) {
                            yearCap = Math.max(1, cal.get("liquid_per_year").getAsInt());
                        }
                    }
                }
            } catch (Exception ex) {
                SeekingImmortalsMod.LOGGER.warn("Failed loading garden liquid calendar; using defaults", ex);
            }
            loaded = true;
            SeekingImmortalsMod.LOGGER.info("Garden liquid year cap={}, cooldownDays={}", yearCap, cooldownDays);
        }
    }
}
