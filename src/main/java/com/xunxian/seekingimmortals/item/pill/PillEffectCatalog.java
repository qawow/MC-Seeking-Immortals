package com.xunxian.seekingimmortals.item.pill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Data-driven pill effect map for catalog/bulk carriers (M04).
 * Loads {@code data/seeking_immortals/alchemy/pill_effect_catalog.json}.
 */
public final class PillEffectCatalog {
    private static final String RESOURCE = "data/" + SeekingImmortalsMod.MODID + "/alchemy/pill_effect_catalog.json";
    private static final Map<String, Entry> BY_PILL_ID = load();
    private static final Map<String, Entry> BY_ITEM_PATH = indexByItemPath(BY_PILL_ID);

    private PillEffectCatalog() {}

    public record Entry(String pillId, String display, String category, String effect, String realmMin, String itemId) {}

    public static Optional<Entry> findByPillId(String pillId) {
        if (pillId == null || pillId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_PILL_ID.get(pillId.trim().toLowerCase(Locale.ROOT)));
    }

    public static Optional<Entry> findByItem(Item item) {
        if (item == null) {
            return Optional.empty();
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            return Optional.empty();
        }
        Entry direct = BY_ITEM_PATH.get(key.getPath());
        if (direct != null) {
            return Optional.of(direct);
        }
        // quality suffix strip: foo_mid / foo_high / foo_supreme
        String path = key.getPath();
        for (String suffix : new String[]{"_mid", "_high", "_supreme", "_middle", "_perfect", "_low"}) {
            if (path.endsWith(suffix)) {
                Entry base = BY_ITEM_PATH.get(path.substring(0, path.length() - suffix.length()));
                if (base != null) {
                    return Optional.of(base);
                }
                Entry byId = BY_PILL_ID.get(path.substring(0, path.length() - suffix.length()));
                if (byId != null) {
                    return Optional.of(byId);
                }
            }
        }
        return Optional.ofNullable(BY_PILL_ID.get(path));
    }

    public static int size() {
        return BY_PILL_ID.size();
    }

    public static Map<String, Entry> all() {
        return Collections.unmodifiableMap(BY_PILL_ID);
    }

    /**
     * Apply catalog effect for a bulk/catalog pill item.
     * @return true if the pill was consumed successfully
     */
    public static boolean tryConsume(ServerPlayer player, ItemStack stack, PillQuality quality) {
        Optional<Entry> optional = findByItem(stack.getItem());
        if (optional.isEmpty()) {
            return false;
        }
        Entry entry = optional.get();
        return CultivationHelper.get(player)
                .map(cultivation -> apply(player, cultivation, stack, entry, quality == null ? PillQuality.LOW : quality))
                .orElse(false);
    }

    private static boolean apply(ServerPlayer player, PlayerCultivation cultivation, ItemStack stack,
                                 Entry entry, PillQuality quality) {
        Realm minRealm = Realm.fromDesignIdOrMortal(entry.realmMin());
        if (cultivation.getRealm().ordinal() < minRealm.ordinal()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.realm_too_low",
                    stack.getHoverName(), minRealm.getDisplayName()), true);
            return false;
        }

        BreakthroughService.HandBreakthroughAidResult aidResult =
                BreakthroughService.tryApplyHandConsumedBreakthroughAid(player, cultivation, stack, true);
        if (aidResult == BreakthroughService.HandBreakthroughAidResult.APPLIED) {
            return true;
        }
        if (aidResult == BreakthroughService.HandBreakthroughAidResult.BLOCKED) {
            return false;
        }

        double mult = quality.getEffectMultiplier() * cultivation.getPillAbsorptionMultiplier();
        String effect = entry.effect() == null ? "generic_cultivation" : entry.effect();
        boolean ok = applyEffect(player, cultivation, effect, entry.category(), mult, quality);
        if (ok) {
            SyncCultivationDataPacket.send(player, cultivation);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.success", stack.getHoverName()), true);
        }
        return ok;
    }

    private static boolean applyEffect(ServerPlayer player, PlayerCultivation cultivation,
                                       String effect, String category, double mult, PillQuality quality) {
        return switch (effect) {
            case "no_food_24h" -> {
                player.getFoodData().eat(scale(10, mult), (float) (5.0D * mult));
                player.getPersistentData().putInt(CatalogPillItem.FASTING_TICKS_KEY, scaleTicks(24000, mult));
                yield true;
            }
            case "restore_spirit_30pct", "restore_mana_50pct", "restore_mana" -> {
                int max = Math.max(1, cultivation.getMaxSpiritualPower());
                double pct = "restore_mana_50pct".equals(effect) ? 0.50D : "restore_mana".equals(effect) ? 0.35D : 0.30D;
                cultivation.addSpiritualPower(Math.max(1, (int) Math.round(max * pct * mult)));
                yield true;
            }
            case "erase_memory_12h" -> {
                player.getPersistentData().putInt(CatalogPillItem.FORGET_DUST_TICKS_KEY, scaleTicks(2400, mult));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, scaleTicks(2400, mult), 0));
                yield true;
            }
            case "pet_mind_clarity", "pet_growth" -> {
                player.getPersistentData().putInt(CatalogPillItem.CLEAR_VOID_PET_CLARITY_KEY, scaleTicks(6000, mult));
                player.removeEffect(MobEffects.CONFUSION);
                yield true;
            }
            case "max_lifespan_plus" -> {
                cultivation.addLifespanYears(scale(500, mult));
                cultivation.addQiDeviationRisk(5);
                yield true;
            }
            case "lifespan_plus_decades" -> {
                cultivation.addLifespanYears(scale(40 + player.getRandom().nextInt(31), mult));
                cultivation.addQiDeviationRisk(6);
                yield true;
            }
            case "lifespan_quarter_once" -> {
                if (cultivation.hasUsedReturnYangTrueWater()) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.catalog_pill.return_yang_used"), true);
                    yield false;
                }
                cultivation.setUsedReturnYangTrueWater(true);
                cultivation.addLifespanYears(scale(300, mult));
                player.heal(player.getMaxHealth());
                yield true;
            }
            case "lifespan_small" -> {
                cultivation.addLifespanYears(scale(20, mult));
                yield true;
            }
            case "freeze_appearance" -> {
                if (player.getPersistentData().getBoolean(CatalogPillItem.APPEARANCE_FIXED_KEY)) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.catalog_pill.appearance_fixed_exists"), true);
                    yield false;
                }
                player.getPersistentData().putBoolean(CatalogPillItem.APPEARANCE_FIXED_KEY, true);
                yield true;
            }
            case "power_now_lifespan_debt" -> {
                player.getPersistentData().putInt(CatalogPillItem.MARROW_ADDICTION_KEY,
                        player.getPersistentData().getInt(CatalogPillItem.MARROW_ADDICTION_KEY) + 1);
                cultivation.addBodyRefinement(scale(10, mult));
                cultivation.addQiDeviationRisk(15);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, scaleTicks(1200, mult), 1));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, scaleTicks(120, mult), 0));
                yield true;
            }
            case "lethal_silent" -> {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, scaleTicks(1200, mult), 2));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, scaleTicks(600, mult), 1));
                player.hurt(player.damageSources().magic(), Math.max(1.0F, 8.0F * (float) mult));
                yield true;
            }
            case "force_power_side_effect" -> {
                cultivation.addQiDeviationRisk(25);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, scaleTicks(1800, mult), 2));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, scaleTicks(400, mult), 1));
                yield true;
            }
            case "save_life_self_damage" -> {
                player.heal(Math.max(1.0F, 12.0F * (float) mult));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, scaleTicks(240, mult), 1));
                cultivation.addAgeYears(2);
                cultivation.addBodyRefinement(-2);
                yield true;
            }
            case "spirit_root_purify" -> {
                cultivation.retestLingGen(player.getRandom(), true);
                cultivation.addBodyRefinement(scale(5, mult));
                yield true;
            }
            case "physique_plus", "demon_body_temper" -> {
                cultivation.addBodyRefinement(scale(6, mult));
                cultivation.addCultivationExp(scale(25, mult));
                yield true;
            }
            case "suppress_demon_qi_24h", "resist_demon_qi_corruption",
                 "reduce_demon_qi_stack", "purge_demon_qi" -> {
                cultivation.clearHeartDemon();
                player.getPersistentData().putInt(CatalogPillItem.YIN_PROTECTION_TICKS_KEY, scaleTicks(24000, mult));
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.POISON);
                yield true;
            }
            case "clear_poison", "clear_cold_poison", "sea_poison_cure" -> {
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.heal(Math.max(1.0F, 6.0F * (float) mult));
                yield true;
            }
            case "temp_mana_shield" -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, scaleTicks(2400, mult), 1));
                cultivation.addSpiritualPower(scale(40, mult));
                yield true;
            }
            case "beast_contract_bonus" -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, scaleTicks(3600, mult), 0));
                cultivation.addDivineConsciousness(scale(8, mult));
                yield true;
            }
            case "cultivation_speed_1h", "cultivation_speed_tianyuan", "blood_cultivation_boost" -> {
                double boost = 1.0D + 0.25D * mult;
                cultivation.addCultivationBoost(scaleTicks(72000, mult), boost);
                cultivation.addCultivationExp(scale(60, mult));
                yield true;
            }
            case "foundation_breakthrough_bonus", "stabilize_foundation" -> {
                if (cultivation.isBreakthroughAssisted()) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.catalog_pill.breakthrough_exists"), true);
                    yield false;
                }
                cultivation.setBreakthroughPillBonus(quality.getBreakthroughBonus());
                yield true;
            }
            case "dual_cultivation_compatible", "dual_cultivation_bonus", "demonic_dual_cultivation" -> {
                cultivation.addCultivationExp(scale(90, mult));
                cultivation.addCultivationBoost(scaleTicks(24000, mult), 1.0D + 0.20D * mult);
                yield true;
            }
            case "calm_inner_demon", "heart_demon_resist" -> {
                cultivation.clearHeartDemon();
                player.removeEffect(MobEffects.CONFUSION);
                cultivation.addDivineConsciousness(scale(10, mult));
                yield true;
            }
            case "restore_soul_minor" -> {
                cultivation.addDivineConsciousness(scale(12, mult));
                player.removeEffect(MobEffects.BLINDNESS);
                yield true;
            }
            case "sea_sickness_immune", "movement_speed" -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, scaleTicks(6000, mult), 0));
                player.removeEffect(MobEffects.CONFUSION);
                yield true;
            }
            case "yin_damage_resist_24h" -> {
                int duration = scaleTicks(24000, mult);
                player.getPersistentData().putInt(CatalogPillItem.YIN_PROTECTION_TICKS_KEY,
                        Math.max(player.getPersistentData().getInt(CatalogPillItem.YIN_PROTECTION_TICKS_KEY), duration));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, scaleTicks(1200, mult), 0));
                yield true;
            }
            case "tribulation_lightning_reduce", "ascension_tribulation_aid" -> {
                player.getPersistentData().putInt(CatalogPillItem.PRESSURE_RESIST_TICKS_KEY, scaleTicks(36000, mult));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, scaleTicks(2400, mult), 1));
                yield true;
            }
            case "killing_intent_control" -> {
                cultivation.addQiDeviationRisk(-Math.min(20, scale(8, mult)));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, scaleTicks(1800, mult), 0));
                yield true;
            }
            default -> applyByCategory(player, cultivation, category, mult, quality);
        };
    }

    private static boolean applyByCategory(ServerPlayer player, PlayerCultivation cultivation,
                                           String category, double mult, PillQuality quality) {
        String cat = category == null ? "cultivation" : category.toLowerCase(Locale.ROOT);
        return switch (cat) {
            case "breakthrough" -> {
                if (cultivation.isBreakthroughAssisted()) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.catalog_pill.breakthrough_exists"), true);
                    yield false;
                }
                cultivation.setBreakthroughPillBonus(quality.getBreakthroughBonus());
                cultivation.addCultivationExp(scale(40, mult));
                yield true;
            }
            case "recovery" -> {
                cultivation.addSpiritualPower(scale(80, mult));
                player.heal(Math.max(1.0F, 6.0F * (float) mult));
                yield true;
            }
            case "poison" -> {
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, scaleTicks(200, mult), 0));
                yield true;
            }
            case "legendary" -> {
                cultivation.addCultivationExp(scale(200, mult));
                cultivation.addLifespanYears(scale(50, mult));
                cultivation.addDivineConsciousness(scale(20, mult));
                yield true;
            }
            case "special" -> {
                cultivation.addCultivationExp(scale(50, mult));
                cultivation.addDivineConsciousness(scale(6, mult));
                yield true;
            }
            default -> {
                // generic_cultivation / cultivation
                cultivation.addSpiritualPower(scale(60, mult));
                cultivation.addCultivationExp(scale(40, mult));
                yield true;
            }
        };
    }

    private static int scale(double base, double mult) {
        return Math.max(1, (int) Math.round(base * mult));
    }

    private static int scaleTicks(int base, double mult) {
        return Math.max(20, scale(base, mult));
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> map = new LinkedHashMap<>();
        try (InputStream in = PillEffectCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                SeekingImmortalsMod.LOGGER.warn("Pill effect catalog missing: {}", RESOURCE);
                return map;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("entries");
            if (arr == null) {
                return map;
            }
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject o = el.getAsJsonObject();
                String pillId = text(o, "pill_id");
                if (pillId.isBlank()) {
                    continue;
                }
                pillId = pillId.toLowerCase(Locale.ROOT);
                map.put(pillId, new Entry(
                        pillId,
                        text(o, "display"),
                        text(o, "category"),
                        text(o, "effect").isBlank() ? "generic_cultivation" : text(o, "effect"),
                        text(o, "realm_min").isBlank() ? "QI_REFINING" : text(o, "realm_min"),
                        text(o, "item")));
            }
            SeekingImmortalsMod.LOGGER.info("Loaded {} pill effect catalog entries.", map.size());
        } catch (Exception ex) {
            SeekingImmortalsMod.LOGGER.error("Failed loading pill effect catalog", ex);
        }
        return map;
    }

    private static Map<String, Entry> indexByItemPath(Map<String, Entry> byPill) {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (Entry entry : byPill.values()) {
            map.put(entry.pillId(), entry);
            String item = entry.itemId();
            if (item != null && !item.isBlank()) {
                String path = item.contains(":") ? item.substring(item.indexOf(':') + 1) : item;
                map.putIfAbsent(path.toLowerCase(Locale.ROOT), entry);
            }
        }
        return map;
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }
}
