package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Server-authoritative execution helpers for authored daily-event semantics. */
public final class DailyEventEffectExecutor {
    private static final String ROOT = "seeking_immortals_daily_event_semantics";
    private static final String ACTIVE_ID = "ActiveId";
    private static final String ACTIVE_REGION = "ActiveRegion";
    private static final String ACTIVE_UNTIL = "ActiveUntil";
    private static final String APPLIED_ID = "AppliedId";
    private static final String APPLIED_UNTIL = "AppliedUntil";
    private static final String BREAKTHROUGH_BONUS = "BreakthroughBonus";
    private static final String BREAKTHROUGH_EVENT = "BreakthroughEvent";
    private static final String BREAKTHROUGH_UNTIL = "BreakthroughUntil";
    private static final String APPLIED_FLAGS = "AppliedFlags";
    private static final String SMUGGLE_CHANCE = "SmuggleChance";

    private static final Set<String> QUEST_HINT_TOKENS = Set.of(
            "quest_hint_blood_forbidden",
            "clan_quest_offer",
            "faction_conflict_minor",
            "quest_hint_demon_embassy",
            "quest_merit_convoy",
            "quest_hint_diyuan_permit");

    private DailyEventEffectExecutor() {}

    /** Preserves the complete temporary ownership ledger across player clones. */
    public static void copyPersistentData(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        if (source.contains(ROOT, Tag.TAG_COMPOUND)) {
            target.put(ROOT, source.getCompound(ROOT).copy());
        } else {
            target.remove(ROOT);
        }
    }

    /** Returns whether this executor or its capability mirror still owns state. */
    public static boolean hasState(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!player.getPersistentData().getCompound(ROOT).isEmpty()) {
            return true;
        }
        return CultivationHelper.get(player).map(cultivation ->
                !cultivation.getWorldpackActiveDailyEventId().isBlank()
                        || cultivation.getWorldpackActiveDailyEventUntilTick() > 0L
                        || Math.abs(cultivation.getWorldpackDailyCultivationMultiplier() - 1.0D) > 0.000001D)
                .orElse(false);
    }

    /**
     * Applies one-shot and renewable effects from a server-selected event definition.
     * The changed hint is informational; per-player NBT remains the duplicate guard.
     */
    public static void apply(ServerPlayer player, String regionId, DailyEventEffectCatalog.Event event,
                             long untilTick, boolean changed) {
        if (player == null || event == null || event.id().isBlank() || untilTick <= 0L
                || player.level().isClientSide) {
            return;
        }
        long now = player.level().getGameTime();
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        clearExpiredState(player, root, event.id(), now);
        if (untilTick <= now) {
            expire(player);
            return;
        }

        root.putString(ACTIVE_ID, event.id());
        root.putString(ACTIVE_REGION, normalize(regionId));
        root.putLong(ACTIVE_UNTIL, untilTick);
        boolean realmAllowed = isRealmAllowed(player, event);
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (realmAllowed) {
                cultivation.setWorldpackDailyEvent(event.id(), untilTick);
                cultivation.setWorldpackDailyCultivationMultiplier(cultivationMultiplier(event));
            } else {
                cultivation.clearWorldpackDailyEvent();
            }
        });
        if (!realmAllowed) {
            clearStoredFlags(player, root);
            clearBreakthrough(root);
            root.remove(SMUGGLE_CHANCE);
            root.remove(APPLIED_ID);
            root.remove(APPLIED_UNTIL);
            player.getPersistentData().put(ROOT, root);
            return;
        }
        boolean firstForRoll = !event.id().equals(root.getString(APPLIED_ID))
                || untilTick != root.getLong(APPLIED_UNTIL);
        if (firstForRoll) {
            clearStoredFlags(player, root);
            applyOneShot(player, event, untilTick, root);
            root.putString(APPLIED_ID, event.id());
            root.putLong(APPLIED_UNTIL, untilTick);
        }
        applyRenewableDebuffs(player, event);
        player.getPersistentData().put(ROOT, root);
    }

    private static void applyOneShot(ServerPlayer player, DailyEventEffectCatalog.Event event,
                                     long untilTick, CompoundTag root) {
        CultivationHelper.get(player).ifPresent(cultivation ->
                cultivation.setWorldpackDailyCultivationMultiplier(cultivationMultiplier(event)));

        Set<String> flags = new LinkedHashSet<>(semanticFlags(event));
        flags.add("daily_event_" + event.id());
        for (String flag : flags) {
            NpcDialogueFlags.setFlag(player, flag);
        }
        putFlags(root, flags);

        double breakthroughBonus = breakthroughChanceBonus(event);
        if (breakthroughBonus > 0.0D) {
            root.putDouble(BREAKTHROUGH_BONUS, breakthroughBonus);
            root.putString(BREAKTHROUGH_EVENT, event.id());
            root.putLong(BREAKTHROUGH_UNTIL, untilTick);
        } else {
            clearBreakthrough(root);
        }

        double smuggleChance = tokenNumber(event, "inverse_star_smuggle_chance", 0.0D);
        if (smuggleChance > 0.0D) {
            root.putDouble(SMUGGLE_CHANCE, Math.min(1.0D, smuggleChance));
        } else {
            root.remove(SMUGGLE_CHANCE);
        }
    }

    private static void clearExpiredState(ServerPlayer player, CompoundTag root,
                                          String nextEventId, long now) {
        String previousEvent = root.getString(ACTIVE_ID);
        boolean expired = root.contains(ACTIVE_ID) && now >= root.getLong(ACTIVE_UNTIL);
        boolean replaced = !normalize(nextEventId).equals(previousEvent);
        boolean breakthroughExpired = root.contains(BREAKTHROUGH_EVENT)
                && (now >= root.getLong(BREAKTHROUGH_UNTIL)
                || !normalize(nextEventId).equals(root.getString(BREAKTHROUGH_EVENT)));
        if (!expired && !replaced && !breakthroughExpired) {
            return;
        }
        clearStoredFlags(player, root);
        if (expired || replaced) {
            clearDailyCultivationMultiplier(player);
        }
        if (replaced || breakthroughExpired) {
            clearBreakthrough(root);
        }
        if (expired || replaced) {
            root.remove(APPLIED_ID);
            root.remove(APPLIED_UNTIL);
        }
    }

    /** Clears event-owned temporary state when no active event is available. */
    public static void expire(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        clearStoredFlags(player, root);
        clearBreakthrough(root);
        clearActive(root);
        root.remove(APPLIED_ID);
        root.remove(APPLIED_UNTIL);
        clearDailyEventMirror(player);
        player.getPersistentData().put(ROOT, root);
    }

    /** Performs the cheap boundary check before gameplay gains are calculated. */
    public static void expireIfNeeded(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        String eventId = normalize(root.getString(ACTIVE_ID));
        long now = player.level().getGameTime();
        if ((!eventId.isBlank() && (root.getLong(ACTIVE_UNTIL) <= now
                || !matchesCurrentRegion(player, root.getString(ACTIVE_REGION))))
                || (eventId.isBlank() && !root.isEmpty())) {
            expire(player);
            return;
        }
        if (eventId.isBlank()) {
            CultivationHelper.get(player).ifPresent(cultivation -> {
                long mirrorUntil = cultivation.getWorldpackActiveDailyEventUntilTick();
                boolean malformedMirror = mirrorUntil <= 0L
                        && (!cultivation.getWorldpackActiveDailyEventId().isBlank()
                        || Math.abs(cultivation.getWorldpackDailyCultivationMultiplier() - 1.0D) > 0.000001D);
                if ((mirrorUntil > 0L && mirrorUntil <= now) || malformedMirror) {
                    cultivation.clearWorldpackDailyEvent();
                }
            });
        }
    }

    /** Re-applies renewable event effects and removes them exactly at expiry. */
    public static void tick(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        long now = player.level().getGameTime();
        String eventId = normalize(root.getString(ACTIVE_ID));
        long until = root.getLong(ACTIVE_UNTIL);
        boolean hasOwnedState = !root.isEmpty();
        if (eventId.isBlank()) {
            if (hasOwnedState) {
                clearStoredFlags(player, root);
                clearBreakthrough(root);
                clearActive(root);
                root.remove(APPLIED_ID);
                root.remove(APPLIED_UNTIL);
                player.getPersistentData().put(ROOT, root);
                clearDailyEventMirror(player);
            }
            return;
        }
        if (until <= now || !matchesCurrentRegion(player, root.getString(ACTIVE_REGION))) {
            expire(player);
            return;
        }
        DailyEventEffectCatalog.builtin().find(eventId).ifPresentOrElse(
                event -> apply(player, root.getString(ACTIVE_REGION), event, until, false),
                () -> expire(player));
    }

    /** Returns the active authored breakthrough bonus without touching pill-assistance state. */
    public static double activeBreakthroughChanceBonus(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return 0.0D;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        DailyEventEffectCatalog.Event event = DailyEventEffectCatalog.builtin()
                .find(root.getString(ACTIVE_ID)).orElse(null);
        Realm realm = CultivationHelper.get(player)
                .map(PlayerCultivation::getRealm).orElse(null);
        String currentRegion = CultivationHelper.get(player)
                .map(PlayerCultivation::getWorldpackCurrentRegionId).orElse("");
        return breakthroughBonusForState(root, event, realm, currentRegion,
                player.level().getGameTime());
    }

    static double breakthroughBonusForState(CompoundTag root, DailyEventEffectCatalog.Event event,
                                            Realm realm, String currentRegion, long now) {
        if (root == null || event == null) {
            return 0.0D;
        }
        String eventId = normalize(event.id());
        long activeUntil = root.getLong(ACTIVE_UNTIL);
        long breakthroughUntil = root.getLong(BREAKTHROUGH_UNTIL);
        if (eventId.isBlank() || !eventId.equals(normalize(root.getString(ACTIVE_ID)))
                || !eventId.equals(normalize(root.getString(BREAKTHROUGH_EVENT)))
                || activeUntil <= now || breakthroughUntil <= now || breakthroughUntil > activeUntil) {
            return 0.0D;
        }
        String activeRegion = normalize(root.getString(ACTIVE_REGION));
        if (activeRegion.isBlank() || !activeRegion.equals(normalize(currentRegion))) {
            return 0.0D;
        }
        if (!event.realmMin().isBlank() && !meetsRealmMinimum(realm, event.realmMin())) {
            return 0.0D;
        }
        double authoredBonus = breakthroughChanceBonus(event);
        double storedBonus = clampChanceBonus(root.getDouble(BREAKTHROUGH_BONUS));
        return authoredBonus > 0.0D && storedBonus > 0.0D
                ? Math.min(authoredBonus, storedBonus) : 0.0D;
    }

    private static void putFlags(CompoundTag root, Set<String> flags) {
        ListTag list = new ListTag();
        for (String flag : flags) {
            if (flag != null && !flag.isBlank()) {
                list.add(net.minecraft.nbt.StringTag.valueOf(flag));
            }
        }
        root.put(APPLIED_FLAGS, list);
    }

    private static void clearStoredFlags(ServerPlayer player, CompoundTag root) {
        ListTag list = root.getList(APPLIED_FLAGS, 8);
        for (int i = 0; i < list.size(); i++) {
            String flag = list.getString(i);
            // Never clear a general quest hook that may have been written after
            // an older event build. New event-owned flags use this namespace.
            if (flag.startsWith("daily_event_") || flag.startsWith("daily_herb_growth_boost")) {
                NpcDialogueFlags.setFlag(player, flag, false);
            }
        }
    }

    private static void clearBreakthrough(CompoundTag root) {
        root.remove(BREAKTHROUGH_BONUS);
        root.remove(BREAKTHROUGH_EVENT);
        root.remove(BREAKTHROUGH_UNTIL);
    }

    private static double clampChanceBonus(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(0.20D, value)) : 0.0D;
    }

    private static void applyRenewableDebuffs(ServerPlayer player, DailyEventEffectCatalog.Event event) {
        boolean outdoorSlow = event.hasToken("movement_debuff_outdoor")
                && player.level().canSeeSky(player.blockPosition());
        if (outdoorSlow || event.hasToken("movement_debuff")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
        }
        if (event.hasToken("demon_qi_tick") || event.hasToken("demonization_risk_tag")) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
        }
        if (event.hasToken("yin_damage_ambient") || event.hasToken("ghost_hunt_risk")) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
        }
        if (event.hasToken("spatial_damage_risk")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, false, true));
        }
        if (event.hasToken("tribulation_pressure") || event.hasToken("tribulation_prep_event")) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
        }
    }

    public static double cultivationMultiplier(DailyEventEffectCatalog.Event event) {
        if (event == null) {
            return 1.0D;
        }
        if (event.hasToken("cultivation_speed_1.2_3day")) {
            return 1.20D;
        }
        if (event.hasToken("cultivation_speed_1.1_1day")
                || event.hasToken("cultivation_buff_minor")
                || event.hasToken("cultivation_buff_tag")) {
            return 1.10D;
        }
        return 1.0D;
    }

    public static double breakthroughChanceBonus(DailyEventEffectCatalog.Event event) {
        return event != null && event.hasToken("breakthrough_chance_small") ? 0.05D : 0.0D;
    }

    public static double contributionMultiplier(DailyEventEffectCatalog.Event event) {
        if (event == null) {
            return 1.0D;
        }
        if (event.rawFields().containsKey("contribution_multiplier")) {
            return Math.max(1.0D, Math.min(5.0D,
                    event.authoredNumber("contribution_multiplier", 1.0D)));
        }
        if (event.hasToken("contribution_gain_1.5_1day")) {
            return 1.5D;
        }
        return 1.0D;
    }

    public static double meritMultiplier(DailyEventEffectCatalog.Event event) {
        if (event == null) {
            return 1.0D;
        }
        if (event.rawFields().containsKey("merit_multiplier")) {
            return Math.max(1.0D, Math.min(5.0D,
                    event.authoredNumber("merit_multiplier", 1.0D)));
        }
        if (event.hasToken("merit_mult_2")) {
            return 2.0D;
        }
        return 1.0D;
    }

    public static int adjustContributionReward(int baseReward, DailyEventEffectCatalog.Event event) {
        if (baseReward <= 0) {
            return 0;
        }
        return Math.max(baseReward, (int) Math.ceil(baseReward * contributionMultiplier(event)));
    }

    public static boolean definesContributionReward(DailyEventEffectCatalog.Event event) {
        return event != null && event.definesContributionReward();
    }

    public static boolean definesMeritReward(DailyEventEffectCatalog.Event event) {
        return event != null && event.definesMeritReward();
    }

    public static int adjustMeritReward(int baseReward, DailyEventEffectCatalog.Event event) {
        if (baseReward <= 0) {
            return 0;
        }
        return Math.max(baseReward, (int) Math.ceil(baseReward * meritMultiplier(event)));
    }

    public static boolean definesMarketPricing(DailyEventEffectCatalog.Event event) {
        return event != null && (event.hasToken("herb_shop_price_x1.3")
                || event.hasToken("shop_herb_discount") || event.hasToken("tax_mult"));
    }

    public static double marketPriceMultiplier(DailyEventEffectCatalog.Event event,
                                               String shopId, String itemId) {
        if (event == null) {
            return 1.0D;
        }
        double multiplier = Math.max(0.1D, tokenNumber(event, "tax_mult", 1.0D));
        if (isHerbContext(shopId, itemId)) {
            if (event.hasToken("herb_shop_price_x1.3")) {
                multiplier *= 1.30D;
            }
            if (event.hasToken("shop_herb_discount")) {
                multiplier *= 0.75D;
            }
        }
        return Math.max(0.1D, Math.min(5.0D, multiplier));
    }

    public static int adjustMarketCost(int baseCost, DailyEventEffectCatalog.Event event,
                                       String shopId, String itemId) {
        return Math.max(1, (int) Math.ceil(Math.max(1, baseCost)
                * marketPriceMultiplier(event, shopId, itemId)));
    }

    public static int adjustFerryCost(int baseCost, DailyEventEffectCatalog.Event event) {
        if (baseCost <= 0) {
            return 0;
        }
        int authoredBase = event == null ? 0 : event.costYinStone();
        long cost = Math.max(baseCost, authoredBase);
        if (event != null && event.hasToken("ferry_cost_double")) {
            cost *= 2L;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, cost));
    }

    public static boolean isFerryDelayed(DailyEventEffectCatalog.Event event) {
        if (event == null || !event.hasToken("ferry_delay")) {
            return false;
        }
        String value = event.tokenValue("ferry_delay").orElse("");
        String decoded = decodeString(value);
        // Array tokens are presence-only and arrive as a quoted token value;
        // only an explicit false authored value disables the gate.
        return !"false".equalsIgnoreCase(decoded.isBlank() ? value : decoded);
    }

    public static boolean hasMovementDebuff(DailyEventEffectCatalog.Event event) {
        return event != null && (event.hasToken("movement_debuff_outdoor")
                || event.hasToken("movement_debuff"));
    }

    public static Set<String> semanticFlags(DailyEventEffectCatalog.Event event) {
        if (event == null) {
            return Set.of();
        }
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        for (DailyEventEffectCatalog.EffectToken token : event.tokens()) {
            String id = token.token();
            if (token.source() == DailyEventEffectCatalog.Source.HOOK
                    || QUEST_HINT_TOKENS.contains(id)
                    || id.startsWith("quest_") || id.startsWith("auction_")) {
                flags.add("daily_event_hook_" + id);
                flags.add("daily_event_" + id);
            }
            if ("quest_hint".equals(id)) {
                String value = decodeString(token.value());
                if (!value.isBlank()) {
                    flags.add("daily_event_hook_" + normalize(value));
                    flags.add("daily_event_quest_hint_" + normalize(value));
                }
            }
            if ("herb_growth_boost".equals(id)) {
                flags.add("daily_event_herb_growth_boost");
            }
        }
        if (!event.questHook().isBlank()) {
            flags.add("daily_event_hook_" + event.questHook());
        }
        if (!event.factionTrigger().isBlank()) {
            flags.add("daily_event_faction_" + event.factionTrigger());
        }
        return Collections.unmodifiableSet(flags);
    }

    public static double activeSmuggleChance(ServerPlayer player) {
        if (player == null) {
            return 0.0D;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        if (player.level().getGameTime() >= root.getLong(ACTIVE_UNTIL)
                || !matchesCurrentRegion(player, root.getString(ACTIVE_REGION))) {
            return 0.0D;
        }
        return DailyEventEffectCatalog.builtin().find(root.getString(ACTIVE_ID))
                .filter(event -> isRealmAllowed(player, event))
                .map(event -> Math.max(0.0D, Math.min(1.0D, root.getDouble(SMUGGLE_CHANCE))))
                .orElse(0.0D);
    }

    public static boolean hasActiveToken(ServerPlayer player, String token) {
        return activeEvent(player).map(event -> event.hasToken(normalize(token))).orElse(false);
    }

    public static ItemStack adjustMeritStack(ServerPlayer player, ItemStack original) {
        if (original == null || original.isEmpty()
                || !original.is(com.xunxian.seekingimmortals.registry.ModItems.ALLIANCE_MERIT_TOKEN.get())) {
            return original == null ? ItemStack.EMPTY : original;
        }
        ItemStack adjusted = original.copy();
        int count = activeEvent(player)
                .map(event -> adjustMeritReward(original.getCount(), event))
                .orElse(original.getCount());
        adjusted.setCount(count);
        return adjusted;
    }

    public static List<ItemStack> adjustMeritStacks(ServerPlayer player, List<ItemStack> originals) {
        if (originals == null || originals.isEmpty()) {
            return List.of();
        }
        return originals.stream().map(stack -> adjustMeritStack(player, stack)).toList();
    }

    public static boolean isPvpAllowed(ServerPlayer attacker, ServerPlayer defender) {
        Optional<DailyEventEffectCatalog.Event> active = activeEvent(defender);
        if (active.isEmpty()) {
            return true;
        }
        DailyEventEffectCatalog.Event event = active.get();
        if (event.hasToken("pvp_local")) {
            String attackerRegion = com.xunxian.seekingimmortals.region.RegionRegistry.resolveRegionId(
                    attacker.level(), attacker.blockPosition());
            String defenderRegion = com.xunxian.seekingimmortals.region.RegionRegistry.resolveRegionId(
                    defender.level(), defender.blockPosition());
            if (!attackerRegion.equals(defenderRegion)) {
                return false;
            }
        }
        if (!event.hasToken("pvp_disabled_factions")) {
            return true;
        }
        Set<String> truceFactions = jsonStrings(event.tokenValue("pvp_disabled_factions").orElse(""));
        String attackerFaction = playerFaction(attacker);
        String defenderFaction = playerFaction(defender);
        return attackerFaction.isBlank() || defenderFaction.isBlank()
                || !truceFactions.contains(attackerFaction)
                || !truceFactions.contains(defenderFaction);
    }

    /** Returns the currently active, realm-eligible authored event for cross-service gates. */
    public static Optional<DailyEventEffectCatalog.Event> activeEvent(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return Optional.empty();
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        long until = root.getLong(ACTIVE_UNTIL);
        if (until <= player.level().getGameTime()
                || !matchesCurrentRegion(player, root.getString(ACTIVE_REGION))) {
            return Optional.empty();
        }
        return DailyEventEffectCatalog.builtin().find(root.getString(ACTIVE_ID))
                .filter(event -> isRealmAllowed(player, event));
    }

    private static boolean isHerbContext(String shopId, String itemId) {
        String value = normalize(shopId) + " " + normalize(itemId);
        return containsAny(value, "herb", "grass", "root", "flower", "mushroom", "fruit", "leaf", "seed", "alchemy");
    }

    private static double tokenNumber(DailyEventEffectCatalog.Event event, String token, double fallback) {
        if (event == null) {
            return fallback;
        }
        return event.tokenValue(token).map(value -> parseNumber(value, fallback)).orElse(fallback);
    }

    private static double parseNumber(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String decodeString(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            JsonElement parsed = JsonParser.parseString(value);
            return parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isString()
                    ? parsed.getAsString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Set<String> jsonStrings(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        try {
            JsonElement parsed = JsonParser.parseString(value);
            if (!parsed.isJsonArray()) {
                return Set.of();
            }
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (JsonElement element : parsed.getAsJsonArray()) {
                values.add(normalize(element.getAsString()));
            }
            return Set.copyOf(values);
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    private static String playerFaction(ServerPlayer player) {
        return CultivationHelper.get(player)
                .map(cultivation -> normalize(cultivation.getSevenMysteriesQuest().getSectId()))
                .orElse("");
    }

    private static void clearActive(CompoundTag root) {
        root.remove(ACTIVE_ID);
        root.remove(ACTIVE_REGION);
        root.remove(ACTIVE_UNTIL);
        root.remove(SMUGGLE_CHANCE);
        root.remove(APPLIED_FLAGS);
    }

    private static void clearDailyCultivationMultiplier(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(PlayerCultivation::clearWorldpackDailyCultivationMultiplier);
    }

    private static void clearDailyEventMirror(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(PlayerCultivation::clearWorldpackDailyEvent);
    }

    private static boolean matchesCurrentRegion(ServerPlayer player, String activeRegion) {
        String expected = normalize(activeRegion);
        if (expected.isBlank()) {
            return true;
        }
        return CultivationHelper.get(player)
                .map(cultivation -> expected.equals(normalize(cultivation.getWorldpackCurrentRegionId())))
                .orElse(false);
    }

    public static boolean isRealmAllowed(ServerPlayer player, DailyEventEffectCatalog.Event event) {
        if (event == null || event.realmMin().isBlank()) {
            return true;
        }
        return CultivationHelper.get(player)
                .map(cultivation -> meetsRealmMinimum(cultivation.getRealm(), event.realmMin()))
                .orElse(false);
    }

    public static boolean meetsRealmMinimum(Realm current, String minimum) {
        if (current == null) {
            return false;
        }
        if (minimum == null || minimum.isBlank()) {
            return true;
        }
        Realm required = Realm.fromDesignId(minimum);
        return required != null && current.ordinal() >= required.ordinal();
    }

    private static boolean booleanToken(DailyEventEffectCatalog.Event event, String token) {
        return event != null && event.tokenValue(token)
                .map(value -> {
                    String decoded = decodeString(value);
                    return "true".equalsIgnoreCase(decoded.isBlank() ? value : decoded);
                })
                .orElse(false);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
