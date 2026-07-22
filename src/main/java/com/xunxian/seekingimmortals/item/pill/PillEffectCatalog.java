package com.xunxian.seekingimmortals.item.pill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.alchemy.AlchemyDisplayTexts;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PillEffectCatalog {
    private static final String EFFECT_RESOURCE =
            "data/" + SeekingImmortalsMod.MODID + "/alchemy/pill_effect_catalog.json";
    private static final String DESIGN_RESOURCE =
            "data/" + SeekingImmortalsMod.MODID + "/text_material/pills_catalog.json";
    private static final Map<String, String> PILL_ALIASES = Map.of(
            "appearance_lock_pill", "dingyan_pill",
            "beast_taming_pill_low", "beast_taming_pill",
            "jiangying_pill", "jiangchen_pill",
            "marrow_drain_pill", "marrow_extract_pill",
            "qingxu_pill", "calm_spirit_pill"
    );
    private static final Map<String, Entry> BY_PILL_ID = load();
    private static final Map<String, Entry> BY_ITEM_PATH = indexByItemPath(BY_PILL_ID);

    private PillEffectCatalog() {}

    public record Entry(
            String pillId,
            String display,
            String category,
            String effect,
            String realmMin,
            String itemId,
            String realmMax,
            String realmTarget,
            int spiritGainFlat,
            Set<String> effectTags,
            String element,
            String risk,
            String school,
            int durationTicks,
            String note
    ) {
        public Entry {
            effectTags = effectTags == null ? Set.of() : Set.copyOf(effectTags);
        }
    }

    public static Optional<Entry> findByPillId(String pillId) {
        if (pillId == null || pillId.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(pillId);
        Entry direct = BY_PILL_ID.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }
        String canonical = canonicalPillId(key);
        Entry aliased = BY_PILL_ID.get(canonical);
        if (aliased != null) {
            return Optional.of(aliased);
        }
        for (String suffix : new String[]{"_mid", "_middle", "_high", "_supreme", "_perfect", "_low"}) {
            if (!key.endsWith(suffix)) {
                continue;
            }
            String baseKey = key.substring(0, key.length() - suffix.length());
            Entry base = BY_PILL_ID.get(baseKey);
            if (base == null) {
                base = BY_PILL_ID.get(canonicalPillId(baseKey));
            }
            if (base != null) {
                return Optional.of(base);
            }
        }
        return Optional.empty();
    }

    public static String canonicalPillId(String pillId) {
        String key = normalize(pillId);
        return PILL_ALIASES.getOrDefault(key, key);
    }

    public static Optional<Entry> findByItem(Item item) {
        if (item == null) {
            return Optional.empty();
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            return Optional.empty();
        }
        String path = normalize(key.getPath());
        Entry direct = BY_ITEM_PATH.get(path);
        return direct == null ? findByPillId(path) : Optional.of(direct);
    }

    public static int size() {
        return BY_PILL_ID.size();
    }

    public static Map<String, Entry> all() {
        return Collections.unmodifiableMap(BY_PILL_ID);
    }

    public static boolean tryConsume(ServerPlayer player, ItemStack stack, PillQuality quality) {
        return tryConsume(player, stack, "", quality);
    }

    public static boolean tryConsume(ServerPlayer player, ItemStack stack, String pillId, PillQuality quality) {
        Optional<Entry> optional = findByPillId(pillId);
        if (optional.isEmpty()) {
            optional = findByItem(stack.getItem());
        }
        if (optional.isEmpty()) {
            return false;
        }
        Entry entry = optional.get();
        PillQuality resolvedQuality = quality == null ? PillQuality.LOW : quality;
        return CultivationHelper.get(player)
                .map(cultivation -> apply(player, cultivation, stack, entry, resolvedQuality))
                .orElse(false);
    }

    private static boolean apply(ServerPlayer player, PlayerCultivation cultivation, ItemStack stack,
                                 Entry entry, PillQuality quality) {
        Realm minRealm = Realm.fromDesignId(entry.realmMin());
        if (minRealm == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.invalid_realm", entry.realmMin()), true);
            return false;
        }
        if (cultivation.getRealm().ordinal() < minRealm.ordinal()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.realm_too_low",
                    stack.getHoverName(), minRealm.getDisplayName()), true);
            return false;
        }
        if (!entry.realmMax().isBlank()) {
            Realm maxRealm = Realm.fromDesignId(entry.realmMax());
            if (maxRealm == null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.catalog_pill.invalid_realm", entry.realmMax()), true);
                return false;
            }
            if (cultivation.getRealm().ordinal() > maxRealm.ordinal()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.catalog_pill.realm_too_high",
                        stack.getHoverName(), maxRealm.getDisplayName()), true);
                return false;
            }
        }

        // Jiangchen Pill has its own lower-grade breakthrough profile. Letting the generic
        // hand-aid path claim it first would make it identical to a low Foundation Pill.
        if (!"jiangchen_breakthrough_aid".equals(entry.effect())) {
            BreakthroughService.HandBreakthroughAidResult aidResult =
                    BreakthroughService.tryApplyHandConsumedBreakthroughAid(player, cultivation, stack, true);
            if (aidResult == BreakthroughService.HandBreakthroughAidResult.APPLIED) {
                return true;
            }
            if (aidResult == BreakthroughService.HandBreakthroughAidResult.BLOCKED) {
                return false;
            }
        }

        double multiplier = quality.getEffectMultiplier() * cultivation.getPillAbsorptionMultiplier();
        boolean applied = applyEffect(player, cultivation, entry, multiplier, quality);
        if (!applied) {
            return false;
        }
        SyncCultivationDataPacket.send(player, cultivation);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_pill.success", stack.getHoverName()), true);
        return true;
    }

    private static boolean applyEffect(ServerPlayer player, PlayerCultivation cultivation,
                                       Entry entry, double multiplier, PillQuality quality) {
        String effect = entry.effect();
        return switch (effect) {
            case "no_food_24h" -> applyFasting(player, multiplier);
            case "restore_spirit_30pct", "restore_mana_50pct", "restore_mana" -> {
                int max = Math.max(1, cultivation.getMaxSpiritualPower());
                double percent = "restore_mana_50pct".equals(effect) ? 0.50D
                        : "restore_mana".equals(effect) ? 0.35D : 0.30D;
                yield addSpiritualPower(cultivation, Math.max(1, (int) Math.round(max * percent * multiplier)));
            }
            case "erase_memory_12h" -> applyForgetDust(player, multiplier);
            case "pet_mind_clarity" -> {
                boolean changed = BeastContractService.feedFromConsumable(player);
                changed |= player.removeEffect(MobEffects.CONFUSION);
                yield changed;
            }
            case "pet_growth" -> BeastContractService.feedFromConsumable(player);
            case "max_lifespan_plus" -> {
                cultivation.addLifespanYears(scale(500, multiplier));
                cultivation.addQiDeviationRisk(5);
                yield true;
            }
            case "lifespan_plus_decades" -> {
                cultivation.addLifespanYears(scale(40 + player.getRandom().nextInt(31), multiplier));
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
                cultivation.addLifespanYears(scale(300, multiplier));
                player.heal(player.getMaxHealth());
                yield true;
            }
            case "lifespan_small" -> {
                cultivation.addLifespanYears(scale(20, multiplier));
                yield true;
            }
            case "freeze_appearance" -> applyAppearanceFixing(player);
            case "power_now_lifespan_debt" -> {
                CompoundTag data = player.getPersistentData();
                data.putInt(CatalogPillItem.MARROW_ADDICTION_KEY,
                        data.getInt(CatalogPillItem.MARROW_ADDICTION_KEY) + 1);
                cultivation.addAgeYears(scale(2, multiplier));
                cultivation.addBodyRefinement(scale(10, multiplier));
                cultivation.addQiDeviationRisk(15);
                addOrUpgradeEffect(player, MobEffects.DAMAGE_BOOST, scaleTicks(1200, multiplier), 1);
                addOrUpgradeEffect(player, MobEffects.WITHER, scaleTicks(120, multiplier), 0);
                yield true;
            }
            case "lethal_silent" -> {
                addOrUpgradeEffect(player, MobEffects.POISON, scaleTicks(1200, multiplier), 2);
                addOrUpgradeEffect(player, MobEffects.WITHER, scaleTicks(600, multiplier), 1);
                player.hurt(player.damageSources().magic(), Math.max(1.0F, 8.0F * (float) multiplier));
                yield true;
            }
            case "force_power_side_effect" -> {
                cultivation.addQiDeviationRisk(25);
                addOrUpgradeEffect(player, MobEffects.DAMAGE_BOOST, scaleTicks(1800, multiplier), 2);
                addOrUpgradeEffect(player, MobEffects.POISON, scaleTicks(400, multiplier), 1);
                yield true;
            }
            case "save_life_self_damage" -> {
                boolean changed = heal(player, 12.0F * (float) multiplier);
                changed |= addOrUpgradeEffect(player, MobEffects.REGENERATION, scaleTicks(240, multiplier), 1);
                if (!changed) yield false;
                cultivation.addAgeYears(2);
                cultivation.addBodyRefinement(-2);
                yield true;
            }
            case "spirit_root_purify" -> {
                cultivation.retestLingGen(player.getRandom(), true);
                cultivation.addBodyRefinement(scale(5, multiplier));
                yield true;
            }
            case "physique_plus", "demon_body_temper" -> {
                cultivation.addBodyRefinement(scale(6, multiplier));
                cultivation.addCultivationExp(scale(25, multiplier));
                if ("demon_body_temper".equals(effect)) {
                    cultivation.addQiDeviationRisk(4);
                }
                yield true;
            }
            case "suppress_demon_qi_24h", "resist_demon_qi_corruption",
                 "reduce_demon_qi_stack", "purge_demon_qi" -> applyDemonPurge(player, cultivation, multiplier);
            case "clear_poison", "clear_cold_poison", "sea_poison_cure" -> applyDetox(player, multiplier);
            case "temp_mana_shield" -> {
                int duration = entry.durationTicks() > 0 ? scaleTicks(entry.durationTicks(), multiplier)
                        : scaleTicks(2400, multiplier);
                boolean changed = addOrUpgradeEffect(player, MobEffects.ABSORPTION, duration, 1);
                changed |= addSpiritualPower(cultivation, scale(40, multiplier));
                yield changed;
            }
            case "beast_contract_bonus" -> {
                if (!BeastContractService.feedFromConsumable(player)) yield false;
                addOrUpgradeEffect(player, MobEffects.DAMAGE_RESISTANCE, scaleTicks(3600, multiplier), 0);
                addDivineConsciousness(cultivation, scale(8, multiplier));
                yield true;
            }
            case "cultivation_speed_1h" -> {
                cultivation.addCultivationBoost(scaleTicks(72000, multiplier), 1.0D + 0.25D * multiplier);
                cultivation.addCultivationExp(scale(60, multiplier));
                yield true;
            }
            case "cultivation_speed_tianyuan" -> {
                cultivation.addCultivationBoost(scaleTicks(108000, multiplier), 1.0D + 0.45D * multiplier);
                cultivation.addCultivationExp(scale(180, multiplier));
                addSpiritualPower(cultivation, scale(80, multiplier));
                addDivineConsciousness(cultivation, scale(6, multiplier));
                yield true;
            }
            case "blood_cultivation_boost" -> {
                cultivation.addCultivationBoost(scaleTicks(72000, multiplier), 1.0D + 0.25D * multiplier);
                cultivation.addCultivationExp(scale(60, multiplier));
                cultivation.addQiDeviationRisk(3);
                yield true;
            }
            case "jiangchen_breakthrough_aid" ->
                    applyJiangchenBreakthroughAid(player, cultivation, quality);
            case "foundation_breakthrough_bonus", "stabilize_foundation" ->
                    applyTargetedBreakthrough(player, cultivation, entry, quality, false);
            case "dual_cultivation_compatible", "dual_cultivation_bonus", "demonic_dual_cultivation" -> {
                cultivation.addCultivationExp(scale(90, multiplier));
                cultivation.addCultivationBoost(scaleTicks(24000, multiplier), 1.0D + 0.20D * multiplier);
                if ("demonic_dual_cultivation".equals(effect)) {
                    cultivation.addQiDeviationRisk(6);
                }
                yield true;
            }
            case "calm_inner_demon", "heart_demon_resist" -> applyHeartDemonRelief(player, cultivation, multiplier, true);
            case "restore_soul_minor" -> applySoulHealing(player, cultivation, multiplier);
            case "sea_sickness_immune", "movement_speed" -> {
                boolean changed = addOrUpgradeEffect(player, MobEffects.MOVEMENT_SPEED, scaleTicks(6000, multiplier), 0);
                changed |= player.removeEffect(MobEffects.CONFUSION);
                yield changed;
            }
            case "yin_damage_resist_24h" -> {
                boolean changed = setMaxTicks(player.getPersistentData(), CatalogPillItem.YIN_PROTECTION_TICKS_KEY,
                        scaleTicks(24000, multiplier));
                changed |= addOrUpgradeEffect(player, MobEffects.DAMAGE_RESISTANCE, scaleTicks(1200, multiplier), 0);
                yield changed;
            }
            case "tribulation_lightning_reduce", "ascension_tribulation_aid" ->
                    applyTribulationGuard(player, cultivation, multiplier);
            case "killing_intent_control" -> {
                int before = cultivation.getQiDeviationRisk();
                cultivation.addQiDeviationRisk(-Math.min(20, scale(8, multiplier)));
                boolean changed = cultivation.getQiDeviationRisk() != before;
                changed |= addOrUpgradeEffect(player, MobEffects.DAMAGE_BOOST, scaleTicks(1800, multiplier), 0);
                yield changed;
            }
            case "spirit_gain_flat" -> addSpiritualPower(cultivation, scale(entry.spiritGainFlat(), multiplier));
            case "targeted_breakthrough_aid" -> applyTargetedBreakthrough(player, cultivation, entry, quality, false);
            case "tribulation_breakthrough_aid" -> applyTargetedBreakthrough(player, cultivation, entry, quality, true);
            case "soul_heal" -> applySoulHealing(player, cultivation, multiplier);
            case "hp_regen" -> {
                boolean changed = heal(player, 8.0F * (float) multiplier);
                changed |= addOrUpgradeEffect(player, MobEffects.REGENERATION, scaleTicks(300, multiplier), 1);
                yield changed;
            }
            case "heart_demon_reduce" -> applyHeartDemonRelief(player, cultivation, multiplier, false);
            case "death_substitute_once" -> {
                boolean granted = cultivation.grantDeathSubstitute();
                if (!granted) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.catalog_pill.death_substitute_exists"), true);
                }
                yield granted;
            }
            case "detox" -> applyDetox(player, multiplier);
            case "diyuan_adaptation" -> applyDiyuanAdaptation(player, multiplier);
            case "marrow_cleansing" -> applyMarrowCleansing(player, cultivation, entry, multiplier);
            case "elemental_cultivation" -> applyElementalCultivation(player, cultivation, entry, multiplier);
            case "high_tier_cultivation" -> {
                cultivation.addCultivationExp(scale(900, multiplier));
                cultivation.addDivineConsciousness(scale(30, multiplier));
                cultivation.addCultivationBoost(scaleTicks(36000, multiplier), 1.0D + 0.45D * multiplier);
                cultivation.addQiDeviationRisk(3);
                addOrUpgradeEffect(player, MobEffects.REGENERATION, scaleTicks(300, multiplier), 1);
                yield true;
            }
            case "cultivation_progress" -> applyCultivationProgress(player, cultivation, entry, multiplier);
            case "restorative_tonic" -> applyRestorativeTonic(player, cultivation, multiplier);
            case "body_tempering" -> {
                cultivation.addBodyRefinement(scale(5, multiplier));
                cultivation.addCultivationExp(scale(25, multiplier));
                yield true;
            }
            case "toxic_cultivation" -> applyToxicCultivation(player, cultivation, entry, multiplier);
            case "illusion_tonic" -> {
                boolean changed = addOrUpgradeEffect(player, MobEffects.INVISIBILITY, scaleTicks(1200, multiplier), 0);
                changed |= addOrUpgradeEffect(player, MobEffects.NIGHT_VISION, scaleTicks(1200, multiplier), 0);
                yield changed;
            }
            case "legendary_essence" -> {
                cultivation.addCultivationExp(scale(300, multiplier));
                cultivation.addLifespanYears(scale(50, multiplier));
                cultivation.addDivineConsciousness(scale(20, multiplier));
                yield true;
            }
            case "special_tonic" -> {
                int before = cultivation.getQiDeviationRisk();
                cultivation.addQiDeviationRisk(-scale(5, multiplier));
                boolean changed = before != cultivation.getQiDeviationRisk();
                changed |= addOrUpgradeEffect(player, MobEffects.ABSORPTION, scaleTicks(600, multiplier), 0);
                yield changed;
            }
            case "tribulation_guard" -> applyTribulationGuard(player, cultivation, multiplier);
            case "realm_cultivation_aid" -> applyRealmCultivationAid(player, cultivation, entry, multiplier);
            case "jade_spirit_tonic" -> applyJadeSpiritTonic(player, cultivation, multiplier);
            case "yuan_gathering" -> applyYuanGathering(player, cultivation, multiplier);
            case "dragon_tiger_temper" -> applyDragonTigerTemper(player, cultivation, multiplier);
            case "spirit_gather_tonic" -> applySpiritGatherTonic(player, cultivation, entry, multiplier);
            case "yin_yang_balance" -> applyYinYangBalance(player, cultivation, multiplier);
            case "spirit_seed_growth" -> applySpiritSeedGrowth(player, cultivation, multiplier);
            case "star_sea_voyage" -> applyStarSeaVoyage(player, cultivation, multiplier);
            case "sect_spirit_tonic" -> applySectSpiritTonic(player, cultivation, multiplier);
            case "merit_tonic" -> applyMeritTonic(player, cultivation, multiplier);
            default -> {
                SeekingImmortalsMod.LOGGER.error("Unsupported pill effect {} for {}", effect, entry.pillId());
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.catalog_pill.effect_unavailable",
                        AlchemyDisplayTexts.recipe(entry.pillId())), true);
                yield false;
            }
        };
    }

    private static boolean applyFasting(ServerPlayer player, double multiplier) {
        int duration = scaleTicks(24000, multiplier);
        boolean changed = setMaxTicks(player.getPersistentData(), CatalogPillItem.FASTING_TICKS_KEY, duration);
        if (player.getFoodData().getFoodLevel() < 18) {
            player.getFoodData().setFoodLevel(18);
            changed = true;
        }
        if (player.getFoodData().getSaturationLevel() < 5.0F) {
            player.getFoodData().setSaturation(5.0F);
            changed = true;
        }
        return changed;
    }

    private static boolean applyForgetDust(ServerPlayer player, double multiplier) {
        int duration = scaleTicks(2400, multiplier);
        boolean changed = setMaxTicks(player.getPersistentData(), CatalogPillItem.FORGET_DUST_TICKS_KEY, duration);
        changed |= addOrUpgradeEffect(player, MobEffects.CONFUSION, duration, 0);
        changed |= addOrUpgradeEffect(player, MobEffects.BLINDNESS, Math.min(duration, scaleTicks(200, multiplier)), 0);
        return changed;
    }

    private static boolean applyAppearanceFixing(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(CatalogPillItem.APPEARANCE_FIXED_KEY)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.appearance_fixed_exists"), true);
            return false;
        }
        data.putBoolean(CatalogPillItem.APPEARANCE_FIXED_KEY, true);
        return true;
    }

    private static boolean applyDemonPurge(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        boolean changed = cultivation.hasHeartDemon();
        cultivation.clearHeartDemon();
        changed |= setMaxTicks(player.getPersistentData(), CatalogPillItem.YIN_PROTECTION_TICKS_KEY,
                scaleTicks(24000, multiplier));
        changed |= removeEffects(player, MobEffects.WITHER, MobEffects.POISON, MobEffects.CONFUSION);
        return changed;
    }

    private static boolean applyDetox(ServerPlayer player, double multiplier) {
        boolean changed = removeEffects(player, MobEffects.POISON, MobEffects.WITHER,
                MobEffects.MOVEMENT_SLOWDOWN, MobEffects.HUNGER, MobEffects.CONFUSION);
        if (player.getHealth() < player.getMaxHealth()) {
            changed |= heal(player, 6.0F * (float) multiplier);
        }
        return changed;
    }

    private static boolean applyHeartDemonRelief(ServerPlayer player, PlayerCultivation cultivation,
                                                  double multiplier, boolean clearAll) {
        boolean changed;
        if (clearAll) {
            changed = cultivation.hasHeartDemon();
            cultivation.clearHeartDemon();
        } else {
            changed = cultivation.reduceHeartDemon(Math.max(1, scale(1, multiplier)));
        }
        changed |= player.removeEffect(MobEffects.CONFUSION);
        if (changed) {
            addDivineConsciousness(cultivation, scale(8, multiplier));
        }
        return changed;
    }

    private static boolean applySoulHealing(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        boolean changed = addDivineConsciousness(cultivation, scale(12, multiplier));
        changed |= removeEffects(player, MobEffects.BLINDNESS, MobEffects.CONFUSION, MobEffects.WEAKNESS);
        return changed;
    }

    private static boolean applyDiyuanAdaptation(ServerPlayer player, double multiplier) {
        boolean changed = setMaxTicks(player.getPersistentData(), CatalogPillItem.PRESSURE_RESIST_TICKS_KEY,
                scaleTicks(24000, multiplier));
        changed |= removeEffects(player, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.CONFUSION);
        changed |= addOrUpgradeEffect(player, MobEffects.DAMAGE_RESISTANCE, scaleTicks(1200, multiplier), 0);
        return changed;
    }

    private static boolean applyTribulationGuard(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        int before = cultivation.getTribulationResistance();
        cultivation.addTribulationResistance(Math.max(1, scale(5, multiplier)));
        boolean changed = before != cultivation.getTribulationResistance();
        changed |= setMaxTicks(player.getPersistentData(), CatalogPillItem.PRESSURE_RESIST_TICKS_KEY,
                scaleTicks(36000, multiplier));
        changed |= addOrUpgradeEffect(player, MobEffects.DAMAGE_RESISTANCE, scaleTicks(2400, multiplier), 1);
        return changed;
    }

    private static boolean applyTargetedBreakthrough(ServerPlayer player, PlayerCultivation cultivation,
                                                       Entry entry, PillQuality quality, boolean tribulationAid) {
        Realm target = entry.realmTarget().isBlank()
                ? cultivation.getNextBreakthroughRealm()
                : Realm.fromDesignId(entry.realmTarget());
        if (target == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.invalid_realm", entry.realmTarget()), true);
            return false;
        }
        if (!cultivation.isAtBreakthroughCap()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.not_at_breakthrough"), true);
            return false;
        }
        if (cultivation.getNextBreakthroughRealm() != target) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.wrong_target", target.getDisplayName()), true);
            return false;
        }
        if (cultivation.isBreakthroughAssisted()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.breakthrough_exists"), true);
            return false;
        }
        cultivation.setBreakthroughPillBonus(quality.getBreakthroughBonus());
        if (tribulationAid) {
            cultivation.addTribulationResistance(quality.getBreakthroughBonusPercent());
        }
        return true;
    }

    private static boolean applyJiangchenBreakthroughAid(ServerPlayer player, PlayerCultivation cultivation,
                                                          PillQuality quality) {
        if (!cultivation.isAtBreakthroughCap()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.not_at_breakthrough"), true);
            return false;
        }
        if (cultivation.isBreakthroughAssisted()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.breakthrough_exists"), true);
            return false;
        }
        // A low Jiangchen Pill is a 3% aid; formal Foundation Pills start at 5%.
        cultivation.setBreakthroughPillBonus(quality.getBreakthroughBonus() * 0.60D);
        return true;
    }

    private static boolean applyMarrowCleansing(ServerPlayer player, PlayerCultivation cultivation,
                                                 Entry entry, double multiplier) {
        cultivation.retestLingGen(player.getRandom(), true);
        cultivation.addBodyRefinement(scale(5, multiplier));
        if (entry.risk().contains("lifespan") && player.getRandom().nextFloat() < 0.25F) {
            cultivation.addAgeYears(Math.max(1, scale(2, multiplier)));
        }
        return true;
    }

    private static boolean applyElementalCultivation(ServerPlayer player, PlayerCultivation cultivation,
                                                       Entry entry, double multiplier) {
        Set<SpiritualRootAttribute> roots = cultivation.getSpiritualRootAttributes();
        boolean compatible = switch (normalize(entry.element())) {
            case "fire" -> roots.contains(SpiritualRootAttribute.FIRE);
            case "ice" -> roots.contains(SpiritualRootAttribute.ICE) || roots.contains(SpiritualRootAttribute.WATER);
            case "ice_fire" -> roots.contains(SpiritualRootAttribute.FIRE)
                    && (roots.contains(SpiritualRootAttribute.ICE) || roots.contains(SpiritualRootAttribute.WATER));
            case "mixed", "" -> !roots.isEmpty();
            default -> false;
        };
        int cultivationGain = compatible ? 120 : 40;
        cultivation.addCultivationExp(scale(cultivationGain, multiplier));
        if (!compatible) {
            cultivation.addQiDeviationRisk(entry.risk().contains("dual") ? 15 : 7);
            addOrUpgradeEffect(player, MobEffects.CONFUSION, scaleTicks(200, multiplier), 0);
        }
        return true;
    }

    private static boolean applyCultivationProgress(ServerPlayer player, PlayerCultivation cultivation,
                                                     Entry entry, double multiplier) {
        Realm target = entry.realmTarget().isBlank() ? null : Realm.fromDesignId(entry.realmTarget());
        if (target != null && cultivation.getRealm() != target) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.target_realm_only", target.getDisplayName()), true);
            return false;
        }
        int tier = target == null ? cultivation.getRealm().ordinal() : target.ordinal();
        int before = cultivation.getCultivationExp();
        int base = 40 + Math.max(0, tier) * 20;
        // Named leftover progress pills still get mild identity bonuses.
        String id = normalize(entry.pillId());
        if (id.contains("juyuan") || id.contains("juling") || id.contains("condense")) {
            base += 30;
            addSpiritualPower(cultivation, scale(25, multiplier));
        } else if (id.contains("longhu") || id.contains("body") || id.contains("temper")) {
            base += 15;
            cultivation.addBodyRefinement(scale(2, multiplier));
        } else if (id.contains("biyu") || id.contains("jade")) {
            base += 10;
            addOrUpgradeEffect(player, MobEffects.ABSORPTION, scaleTicks(400, multiplier), 0);
        }
        cultivation.addCultivationExp(scale(base, multiplier));
        boolean changed = before != cultivation.getCultivationExp();
        if (entry.school().contains("demonic") || entry.risk().contains("demonic")) {
            cultivation.addQiDeviationRisk(5);
            changed = true;
        }
        return changed;
    }

    private static boolean applyRealmCultivationAid(ServerPlayer player, PlayerCultivation cultivation,
                                                     Entry entry, double multiplier) {
        Realm target = entry.realmTarget().isBlank() ? null : Realm.fromDesignId(entry.realmTarget());
        if (target == null) {
            // Fall back to current realm band aid.
            target = cultivation.getRealm();
        }
        int delta = cultivation.getRealm().ordinal() - target.ordinal();
        if (delta > 1) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.target_realm_only", target.getDisplayName()), true);
            return false;
        }
        int tier = Math.max(0, target.ordinal());
        int exp;
        double boost;
        if (delta == 0) {
            exp = 80 + tier * 35;
            boost = 1.0D + 0.18D * multiplier + tier * 0.02D;
            cultivation.addCultivationBoost(scaleTicks(18000 + tier * 1200, multiplier), boost);
            addSpiritualPower(cultivation, scale(30 + tier * 8, multiplier));
        } else if (delta < 0) {
            // Below target: partial aid, no boost.
            exp = 35 + tier * 12;
            addSpiritualPower(cultivation, scale(15 + tier * 4, multiplier));
        } else {
            // One major realm above: residual aid only.
            exp = 20 + tier * 8;
        }
        int before = cultivation.getCultivationExp();
        cultivation.addCultivationExp(scale(exp, multiplier));
        return cultivation.getCultivationExp() != before;
    }

    private static boolean applyJadeSpiritTonic(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        boolean changed = addSpiritualPower(cultivation, scale(70, multiplier));
        changed |= addOrUpgradeEffect(player, MobEffects.ABSORPTION, scaleTicks(800, multiplier), 0);
        changed |= removeEffects(player, MobEffects.POISON, MobEffects.WEAKNESS);
        cultivation.addCultivationExp(scale(35, multiplier));
        return true;
    }

    private static boolean applyYuanGathering(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addCultivationExp(scale(90, multiplier));
        boolean changed = addSpiritualPower(cultivation, scale(55, multiplier));
        cultivation.addCultivationBoost(scaleTicks(12000, multiplier), 1.0D + 0.15D * multiplier);
        return true;
    }

    private static boolean applyDragonTigerTemper(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addBodyRefinement(scale(8, multiplier));
        cultivation.addCultivationExp(scale(70, multiplier));
        addOrUpgradeEffect(player, MobEffects.DAMAGE_BOOST, scaleTicks(1600, multiplier), 0);
        addOrUpgradeEffect(player, MobEffects.DAMAGE_RESISTANCE, scaleTicks(1200, multiplier), 0);
        return true;
    }

    private static boolean applySpiritGatherTonic(ServerPlayer player, PlayerCultivation cultivation,
                                                   Entry entry, double multiplier) {
        int base = entry.pillId().contains("minor") || entry.pillId().contains("condense_minor") ? 45 : 80;
        boolean changed = addSpiritualPower(cultivation, scale(base, multiplier));
        cultivation.addCultivationExp(scale(base / 2, multiplier));
        cultivation.addCultivationBoost(scaleTicks(9000, multiplier), 1.0D + 0.12D * multiplier);
        return true;
    }

    private static boolean applyYinYangBalance(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addCultivationExp(scale(75, multiplier));
        int before = cultivation.getQiDeviationRisk();
        cultivation.addQiDeviationRisk(-scale(6, multiplier));
        boolean changed = before != cultivation.getQiDeviationRisk();
        changed |= addOrUpgradeEffect(player, MobEffects.DAMAGE_RESISTANCE, scaleTicks(1400, multiplier), 0);
        changed |= addOrUpgradeEffect(player, MobEffects.NIGHT_VISION, scaleTicks(1400, multiplier), 0);
        addSpiritualPower(cultivation, scale(35, multiplier));
        return true;
    }

    private static boolean applySpiritSeedGrowth(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addCultivationExp(scale(55, multiplier));
        cultivation.addCultivationBoost(scaleTicks(24000, multiplier), 1.0D + 0.22D * multiplier);
        cultivation.addDivineConsciousness(scale(6, multiplier));
        return true;
    }

    private static boolean applyStarSeaVoyage(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        boolean changed = addOrUpgradeEffect(player, MobEffects.MOVEMENT_SPEED, scaleTicks(3600, multiplier), 0);
        changed |= addOrUpgradeEffect(player, MobEffects.NIGHT_VISION, scaleTicks(3600, multiplier), 0);
        changed |= addOrUpgradeEffect(player, MobEffects.WATER_BREATHING, scaleTicks(3600, multiplier), 0);
        changed |= player.removeEffect(MobEffects.CONFUSION);
        cultivation.addCultivationExp(scale(40, multiplier));
        addSpiritualPower(cultivation, scale(30, multiplier));
        return true;
    }

    private static boolean applySectSpiritTonic(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addCultivationExp(scale(65, multiplier));
        addSpiritualPower(cultivation, scale(45, multiplier));
        cultivation.addCultivationBoost(scaleTicks(10000, multiplier), 1.0D + 0.14D * multiplier);
        return true;
    }

    private static boolean applyMeritTonic(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        int before = cultivation.getQiDeviationRisk();
        cultivation.addQiDeviationRisk(-scale(8, multiplier));
        boolean changed = before != cultivation.getQiDeviationRisk();
        changed |= addOrUpgradeEffect(player, MobEffects.ABSORPTION, scaleTicks(1200, multiplier), 1);
        changed |= addOrUpgradeEffect(player, MobEffects.LUCK, scaleTicks(6000, multiplier), 0);
        cultivation.addCultivationExp(scale(50, multiplier));
        // Soft reputation toward tianyuan / merchant if available.
        try {
            com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, "tianyuan", scale(2, multiplier));
        } catch (Throwable ignored) {
            // Reputation faction may be absent in pure unit environments.
        }
        return true;
    }

    private static boolean applyRestorativeTonic(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        boolean changed = addSpiritualPower(cultivation, scale(60, multiplier));
        changed |= heal(player, 6.0F * (float) multiplier);
        changed |= removeEffects(player, MobEffects.CONFUSION, MobEffects.WEAKNESS);
        return changed;
    }

    private static boolean applyToxicCultivation(ServerPlayer player, PlayerCultivation cultivation,
                                                  Entry entry, double multiplier) {
        cultivation.addCultivationExp(scale(100, multiplier));
        cultivation.addQiDeviationRisk(entry.school().contains("demonic") ? 12 : 8);
        addOrUpgradeEffect(player, MobEffects.DAMAGE_BOOST, scaleTicks(1200, multiplier), 1);
        addOrUpgradeEffect(player, MobEffects.POISON, scaleTicks(160, multiplier), 0);
        return true;
    }

    private static boolean addSpiritualPower(PlayerCultivation cultivation, int amount) {
        int before = cultivation.getSpiritualPower();
        cultivation.addSpiritualPower(amount);
        return cultivation.getSpiritualPower() != before;
    }

    private static boolean addDivineConsciousness(PlayerCultivation cultivation, int amount) {
        int before = cultivation.getDivineConsciousness();
        cultivation.addDivineConsciousness(amount);
        return cultivation.getDivineConsciousness() != before;
    }

    private static boolean heal(ServerPlayer player, float amount) {
        float before = player.getHealth();
        player.heal(Math.max(1.0F, amount));
        return player.getHealth() > before;
    }

    private static boolean removeEffects(ServerPlayer player, MobEffect... effects) {
        boolean changed = false;
        for (MobEffect effect : effects) {
            changed |= player.removeEffect(effect);
        }
        return changed;
    }

    private static boolean addOrUpgradeEffect(ServerPlayer player, MobEffect effect, int duration, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing != null && existing.getAmplifier() > amplifier) {
            return false;
        }
        if (existing != null && existing.getAmplifier() == amplifier && existing.getDuration() >= duration) {
            return false;
        }
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
        return true;
    }

    private static boolean setMaxTicks(CompoundTag data, String key, int ticks) {
        int current = Math.max(0, data.getInt(key));
        if (current >= ticks) {
            return false;
        }
        data.putInt(key, ticks);
        return true;
    }

    private static int scale(double base, double multiplier) {
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    private static int scaleTicks(int base, double multiplier) {
        return Math.max(20, scale(base, multiplier));
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        JsonObject root = readJson(EFFECT_RESOURCE);
        if (root == null || !root.has("entries") || !root.get("entries").isJsonArray()) {
            return entries;
        }
        for (JsonElement element : root.getAsJsonArray("entries")) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String pillId = normalize(text(object, "pill_id"));
            if (pillId.isBlank()) continue;
            entries.put(pillId, new Entry(
                    pillId,
                    text(object, "display"),
                    firstNonBlank(text(object, "category"), "cultivation"),
                    firstNonBlank(text(object, "effect"), "generic_cultivation"),
                    firstNonBlank(text(object, "realm_min"), "QI_REFINING"),
                    text(object, "item"),
                    "", "", 0, Set.of(), "", "", "", 0, ""));
        }
        mergeDesignMetadata(entries);
        SeekingImmortalsMod.LOGGER.info("Loaded {} pill effect catalog entries.", entries.size());
        return entries;
    }

    private static void mergeDesignMetadata(Map<String, Entry> entries) {
        JsonObject root = readJson(DESIGN_RESOURCE);
        if (root == null || !root.has("pills") || !root.get("pills").isJsonArray()) {
            return;
        }
        for (JsonElement element : root.getAsJsonArray("pills")) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String pillId = normalize(text(object, "id"));
            Entry base = entries.get(pillId);
            if (base == null) continue;
            Set<String> tags = stringSet(object.get("effect_tags"));
            String category = firstNonBlank(text(object, "category"), base.category());
            String realmTarget = firstNonBlank(text(object, "realm_target"), inferredTarget(pillId));
            int spiritGain = integer(object, "spirit_gain_flat");
            String consumeRealm = nestedText(object, "learn_requirements", "consume", "realm_min");
            String effect = base.effect();
            if (effect.isBlank() || "generic_cultivation".equals(effect)) {
                effect = resolveGenericEffect(pillId, category, realmTarget, spiritGain, tags);
            }
            entries.put(pillId, new Entry(
                    pillId,
                    firstNonBlank(text(object, "display"), base.display()),
                    category,
                    effect,
                    firstNonBlank(consumeRealm, text(object, "realm_min"), base.realmMin()),
                    base.itemId(),
                    text(object, "realm_max"),
                    realmTarget,
                    spiritGain,
                    tags,
                    normalize(text(object, "element")),
                    normalize(text(object, "risk")),
                    normalize(text(object, "school")),
                    integer(object, "duration_ticks"),
                    text(object, "note")));
        }
    }

    private static String resolveGenericEffect(String pillId, String category, String realmTarget,
                                               int spiritGain, Set<String> tags) {
        if (tags.contains("death_substitute_once")) return "death_substitute_once";
        if (tags.contains("tribulation_aid")) return "tribulation_breakthrough_aid";
        if (tags.contains("diyuan_debuff_reduce")) return "diyuan_adaptation";
        if (tags.contains("soul_heal")) return "soul_heal";
        if (tags.contains("hp_regen")) return "hp_regen";
        if (tags.contains("heart_demon_reduce")) return "heart_demon_reduce";
        if (tags.contains("detox")) return "detox";
        if (spiritGain > 0) return "spirit_gain_flat";
        if ("breakthrough".equals(category) && !realmTarget.isBlank()) return "targeted_breakthrough_aid";
        // Realm-band aid pills always keep a dedicated scaled profile.
        if (pillId.startsWith("cultivation_aid_")) {
            return "realm_cultivation_aid";
        }
        return switch (pillId) {
            case "bone_marrow_pill", "xiyu_pill" -> "marrow_cleansing";
            case "ice_fire_pill", "xuanbing_pill", "lieyan_pill", "wuxing_pill" -> "elemental_cultivation";
            case "pressure_resist_pill", "diyuan_adapt_pill" -> "diyuan_adaptation";
            case "spirit_realm_condense_pill" -> "high_tier_cultivation";
            case "yangyuan_pill", "jingxin_pill", "jieqi_pill", "huiyuan_pill" -> "restorative_tonic";
            case "huanti_pill", "huanxue_pill", "body_refine_pill", "meridian_open_pill",
                 "barbarian_strength_pill" -> "body_tempering";
            case "wangchen_pill" -> "erase_memory_12h";
            case "ghost_cultivate_pill", "demonic_blood_pill", "xueying_pill", "ghost_gate_pill",
                 "poison_insect_pill", "blood_curse_pill" -> "toxic_cultivation";
            case "hehuan_pill" -> "dual_cultivation_bonus";
            case "illusion_pill", "fox_illusion_pill" -> "illusion_tonic";
            case "tribulation_guard_pill" -> "tribulation_guard";
            case "tianling_pill" -> "legendary_essence";
            case "biyu_pill" -> "jade_spirit_tonic";
            case "juyuan_pill" -> "yuan_gathering";
            case "longhu_pill" -> "dragon_tiger_temper";
            case "spirit_condense_minor", "spirit_condense_pill", "juling_pill" -> "spirit_gather_tonic";
            case "yin_yang_pill" -> "yin_yang_balance";
            case "spirit_seed_pill" -> "spirit_seed_growth";
            case "star_sea_pill" -> "star_sea_voyage";
            case "huanglong_pill", "luoyun_spirit_pill" -> "sect_spirit_tonic";
            case "ninghun_dan" -> "soul_heal";
            case "huichun_pill" -> "hp_regen";
            case "qingxin_pill" -> "heart_demon_reduce";
            case "anti_poison_pill" -> "detox";
            case "tianyuan_merit_pill" -> "merit_tonic";
            default -> switch (category) {
                case "breakthrough" -> "tribulation_guard";
                case "recovery" -> "restorative_tonic";
                case "poison" -> "toxic_cultivation";
                case "legendary" -> "legendary_essence";
                case "special" -> "special_tonic";
                default -> "cultivation_progress";
            };
        };
    }

    private static String inferredTarget(String pillId) {
        return switch (pillId) {
            case "foundation_pill" -> "FOUNDATION";
            case "condensation_pill", "jiedan_pill", "ningyuan_pill", "ningjin_pill", "golden_core_pill" ->
                    "CORE_FORMATION";
            case "nascent_soul_pill", "ningying_pill" -> "NASCENT_SOUL";
            case "huashen_pill", "spirit_severing_pill" -> "DEITY_TRANSFORMATION";
            case "void_condense_pill" -> "VOID_REFINEMENT";
            default -> "";
        };
    }

    private static JsonObject readJson(String resource) {
        try (InputStream stream = PillEffectCatalog.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Pill catalog missing: {}", resource);
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.error("Failed loading pill catalog {}", resource, exception);
            return null;
        }
    }

    private static Map<String, Entry> indexByItemPath(Map<String, Entry> byPill) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (Entry entry : byPill.values()) {
            entries.put(entry.pillId(), entry);
            if (!entry.itemId().isBlank()) {
                String path = entry.itemId().contains(":")
                        ? entry.itemId().substring(entry.itemId().indexOf(':') + 1)
                        : entry.itemId();
                entries.putIfAbsent(normalize(path), entry);
            }
        }
        return entries;
    }

    private static Set<String> stringSet(JsonElement element) {
        if (element == null || !element.isJsonArray()) return Set.of();
        Set<String> values = new LinkedHashSet<>();
        for (JsonElement value : element.getAsJsonArray()) {
            if (value.isJsonPrimitive()) {
                String normalized = normalize(value.getAsString());
                if (!normalized.isBlank()) values.add(normalized);
            }
        }
        return Set.copyOf(values);
    }

    private static int integer(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? Math.max(0, object.get(key).getAsInt()) : 0;
    }

    private static String nestedText(JsonObject object, String... path) {
        JsonObject current = object;
        for (int index = 0; index < path.length - 1; index++) {
            if (!current.has(path[index]) || !current.get(path[index]).isJsonObject()) return "";
            current = current.getAsJsonObject(path[index]);
        }
        return text(current, path[path.length - 1]);
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
