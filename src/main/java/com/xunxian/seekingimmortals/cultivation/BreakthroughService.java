package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation.QiDeviationTier;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.item.pill.PillDeathSubstituteEvents;
import com.xunxian.seekingimmortals.item.pill.PillQuality;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.persistence.PlayerPersistentDataClonePolicy;
import com.xunxian.seekingimmortals.registry.ModBulkItems;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BreakthroughService {
    private static final String BREAKTHROUGH_REQUEST_GATE_UNTIL =
            "SeekingImmortalsBreakthroughRequestGateUntil";
    private static final long BREAKTHROUGH_REQUEST_GATE_TICKS = 10L;
    private static final Set<UUID> ACTIVE_EXTREME_DEATHS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> COMMITTED_EXTREME_DEATHS = ConcurrentHashMap.newKeySet();

    private BreakthroughService() {}

    public enum HandBreakthroughAidResult {
        NOT_APPLICABLE,
        APPLIED,
        BLOCKED
    }

    public static void attempt(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        long gateUntil = player.getPersistentData().getLong(BREAKTHROUGH_REQUEST_GATE_UNTIL);
        if (isBreakthroughRequestBlocked(now, gateUntil)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.breakthrough.request_pending",
                    Math.max(1L, gateUntil - now)), true);
            return;
        }
        player.getPersistentData().putLong(BREAKTHROUGH_REQUEST_GATE_UNTIL,
                nextBreakthroughRequestGate(now));
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> attempt(player, cultivation),
                () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.no_data"), true));
    }

    static boolean isBreakthroughRequestBlocked(long now, long gateUntil) {
        return gateUntil > now;
    }

    static long nextBreakthroughRequestGate(long now) {
        return now + BREAKTHROUGH_REQUEST_GATE_TICKS;
    }

    private static void attempt(ServerPlayer player, PlayerCultivation cultivation) {
        if (cultivation.isTribulationActive()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.tribulation.already_active",
                    cultivation.getTribulationTargetRealm().getDisplayName(),
                    cultivation.getTribulationCurrentStrike(),
                    cultivation.getTribulationTotalStrikes()), true);
            return;
        }
        if (cultivation.isAtFinalStage()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.final_stage"), true);
            return;
        }
        if (!cultivation.isAtBreakthroughCap()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.not_at_cap",
                    cultivation.getCurrentStageProgressExp(),
                    cultivation.getCurrentStageExpSpan()), true);
            return;
        }
        PlayerCultivation.BreakthroughChanceBreakdown preview = preview(player, cultivation);
        showPreBreakthroughPreview(player, cultivation, preview);
        if (!consumeBreakthroughResource(player, cultivation)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.need_resource"), true);
            return;
        }

        PlayerCultivation.BreakthroughAttemptResult result = cultivation.tryBreakthrough(player.getRandom(), new PlayerCultivation.BreakthroughChanceModifiers(
                preview.pillBonus(),
                preview.spiritEyeBonus(),
                preview.techniqueQualityBonus(),
                DailyEventEffectExecutor.activeBreakthroughChanceBonus(player)));
        if (result.success()) {
            formGoldCoreIfNeeded(player, cultivation, result);
            boolean tribulationStarted = TribulationService.onBreakthroughSuccess(player, cultivation, result);
            if (!tribulationStarted) {
                SyncCultivationDataPacket.send(player, cultivation);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.success",
                        result.newRealm().getDisplayName(),
                        result.newStage().getDisplayName(),
                        percent(result.chance())), false);
            }
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.bonus_detail",
                    percent(result.chanceBreakdown().pillBonus()),
                    percent(result.chanceBreakdown().spiritEyeBonus()),
                    percent(result.chanceBreakdown().techniqueQualityBonus()),
                    percent(result.chanceBreakdown().eventBonus()),
                    percent(result.chanceBreakdown().obsessionBonus()),
                    percent(result.chanceBreakdown().advancedBonus())), false);
            return;
        }
        SyncCultivationDataPacket.send(player, cultivation);
        if (result.status() == PlayerCultivation.BreakthroughAttemptStatus.FAILURE) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.failure",
                    percent(result.chance()),
                    result.qiDeviationRisk()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.bonus_detail",
                    percent(result.chanceBreakdown().pillBonus()),
                    percent(result.chanceBreakdown().spiritEyeBonus()),
                    percent(result.chanceBreakdown().techniqueQualityBonus()),
                    percent(result.chanceBreakdown().eventBonus()),
                    percent(result.chanceBreakdown().obsessionBonus()),
                    percent(result.chanceBreakdown().advancedBonus())), false);
            if (result.qiDeviationTriggered()) {
                applyQiDeviationEffect(player, cultivation, result.qiDeviationTier(), player.getRandom());
            }
        }
    }

    private static void showPreBreakthroughPreview(ServerPlayer player, PlayerCultivation cultivation, PlayerCultivation.BreakthroughChanceBreakdown preview) {
        ResourceRequirement requirement = getBreakthroughResourceRequirement(player, cultivation);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.preview", percent(preview.chance())), false);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.required_resource",
                requirement.name(), requirement.owned(), requirement.required(), requirement.assisted() ? 1 : 0), false);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.bonus_sources",
                percent(preview.baseChance()),
                percent(cultivation.getSpiritualRoot().getBreakthroughBonus()),
                percent(preview.pillBonus()),
                percent(preview.spiritEyeBonus()),
                percent(preview.techniqueQualityBonus()),
                percent(preview.eventBonus()),
                percent(preview.obsessionBonus()),
                percent(preview.advancedBonus()),
                percent(preview.chance())), false);
    }

    /**
     * 走火入魔分级效果：
     * 70~79% 轻微：损失 30% 当前修为，风险清零
     * 80~89% 中度：损失 50% 修为 + 昏迷 30 秒，风险清零
     * 90~99% 严重：掉落一境界 + 昏迷 3 分钟，装备随机损坏
     * 100%   极端：当场死亡，背包掉落 50%
     */
    public static boolean tryTriggerQiDeviation(ServerPlayer player, PlayerCultivation cultivation, String reasonKey) {
        QiDeviationTier tier = cultivation.rollQiDeviation(player.getRandom());
        if (tier == QiDeviationTier.NONE) {
            SyncCultivationDataPacket.send(player, cultivation);
            return false;
        }
        if (reasonKey != null && !reasonKey.isBlank()) {
            player.displayClientMessage(Component.translatable(reasonKey, cultivation.getQiDeviationRisk()), false);
        }
        applyQiDeviationEffect(player, cultivation, tier, player.getRandom());
        return true;
    }

    private static void applyQiDeviationEffect(ServerPlayer player, PlayerCultivation cultivation, QiDeviationTier tier, RandomSource random) {
        switch (tier) {
            case MINOR -> {
                int loss = (int)Math.floor(cultivation.getCurrentStageProgressExp() * 0.30D);
                cultivation.addCultivationExpRaw(-loss);
                cultivation.setQiDeviationRisk(0);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.qi_deviation.minor", loss), false);
            }
            case MODERATE -> {
                int loss = (int)Math.floor(cultivation.getCurrentStageProgressExp() * 0.50D);
                cultivation.addCultivationExpRaw(-loss);
                cultivation.setQiDeviationRisk(0);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 3, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30 * 20, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30 * 20, 2, false, false));
                player.displayClientMessage(Component.translatable("message.seeking_immortals.qi_deviation.moderate", loss), false);
            }
            case SEVERE -> {
                cultivation.fallOneStagePublic();
                cultivation.setQiDeviationRisk(0);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 60 * 20, 3, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 3 * 60 * 20, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3 * 60 * 20, 2, false, false));
                damageRandomEquipment(player, random);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.qi_deviation.severe"), false);
            }
            case EXTREME -> {
                cultivation.setQiDeviationRisk(0);
                if (!PillDeathSubstituteEvents.trySubstitute(player)) {
                    restorePreservedOnRespawn(player);
                    preserveHalfInventory(player, random);
                    forceExtremeDeath(player);
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.qi_deviation.extreme"), false);
                }
            }
            default -> {}
        }
        SyncCultivationDataPacket.send(player, cultivation);
    }

    public static final String PRESERVED_KEY = PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY;

    public static void markExtremeDeathCommitted(ServerPlayer player) {
        if (!ACTIVE_EXTREME_DEATHS.contains(player.getUUID())
                || !player.getPersistentData().contains(PRESERVED_KEY)) {
            return;
        }
        COMMITTED_EXTREME_DEATHS.add(player.getUUID());
    }

    private static void forceExtremeDeath(ServerPlayer player) {
        UUID playerId = player.getUUID();
        ACTIVE_EXTREME_DEATHS.add(playerId);
        COMMITTED_EXTREME_DEATHS.remove(playerId);
        boolean deathCommitted = false;
        try {
            player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
            deathCommitted = COMMITTED_EXTREME_DEATHS.remove(playerId);
            if (!deathCommitted) {
                if (player.getHealth() <= 0.0F) {
                    player.setHealth(1.0F);
                }
                player.kill();
                deathCommitted = COMMITTED_EXTREME_DEATHS.remove(playerId);
            }
        } finally {
            deathCommitted = deathCommitted || COMMITTED_EXTREME_DEATHS.remove(playerId);
            ACTIVE_EXTREME_DEATHS.remove(playerId);
            if (!deathCommitted && player.getPersistentData().contains(PRESERVED_KEY)) {
                if (player.getHealth() <= 0.0F) {
                    player.setHealth(1.0F);
                }
                restorePreservedOnRespawn(player);
            }
        }
    }

    /**
     * 走火极限：把背包 50% 物品序列化到 PersistentData 供重生归还（不掉世界），从背包移除后令死亡掉剩余 50%。
     * <p>keepInventory=false：地上 50% + 重生 50%；keepInventory=true：原版与本事务共同保留全部物品。
     */
    private static void preserveHalfInventory(ServerPlayer player, RandomSource random) {
        Inventory inv = player.getInventory();
        java.util.List<Integer> occupiedSlots = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) occupiedSlots.add(i);
        }
        java.util.Collections.shuffle(occupiedSlots, new java.util.Random(random.nextLong()));
        int preserveCount = occupiedSlots.size() / 2;
        net.minecraft.nbt.ListTag preserved = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < preserveCount; i++) {
            int slot = occupiedSlots.get(i);
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                preserved.add(stack.save(new net.minecraft.nbt.CompoundTag()));
                inv.setItem(slot, ItemStack.EMPTY);
            }
        }
        player.getPersistentData().put(PRESERVED_KEY, preserved);
    }

    /** 重生时归还走火极限保留的物品，用完即删 key。背包满则掉落地面。 */
    public static void restorePreservedOnRespawn(ServerPlayer player) {
        for (ItemStack stack : PlayerPersistentDataClonePolicy.takeExtremePreserved(
                player.getPersistentData())) {
            InventoryDeliveryService.giveOrEnqueue(player, stack, "breakthrough_preserve");
        }
    }

    private static void damageRandomEquipment(ServerPlayer player, RandomSource random) {
        Inventory inv = player.getInventory();
        java.util.List<ItemStack> damageable = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage()) {
                damageable.add(stack);
            }
        }
        if (damageable.isEmpty()) return;
        int count = Math.min(damageable.size(), Math.max(1, damageable.size() / 3));
        for (int i = 0; i < count; i++) {
            ItemStack target = damageable.get(random.nextInt(damageable.size()));
            int damageToAdd = target.getMaxDamage() / 4 + random.nextInt(target.getMaxDamage() / 4 + 1);
            target.setDamageValue(Math.min(target.getMaxDamage() - 1, target.getDamageValue() + damageToAdd));
        }
    }

    public static PlayerCultivation.BreakthroughChanceBreakdown preview(ServerPlayer player, PlayerCultivation cultivation) {
        ResourceRequirement requirement = getBreakthroughResourceRequirement(player, cultivation);
        PlayerCultivation.BreakthroughChanceModifiers modifiers = new PlayerCultivation.BreakthroughChanceModifiers(
                getPreviewPillBonus(player, cultivation, requirement),
                getSpiritEyeBonus(player),
                getTechniqueQualityBonus(player, cultivation),
                DailyEventEffectExecutor.activeBreakthroughChanceBonus(player));
        return cultivation.getBreakthroughChanceBreakdown(modifiers);
    }

    public static HandBreakthroughAidResult tryApplyHandConsumedBreakthroughAid(ServerPlayer player, PlayerCultivation cultivation,
                                                                               ItemStack stack, boolean requireBreakthroughCap) {
        if (stack == null || stack.isEmpty() || cultivation.isAtFinalStage()) {
            return HandBreakthroughAidResult.NOT_APPLICABLE;
        }
        if (requireBreakthroughCap && !cultivation.isAtBreakthroughCap()) {
            return HandBreakthroughAidResult.NOT_APPLICABLE;
        }

        ResourceRequirement requirement = getBreakthroughResourceRequirement(player, cultivation);
        Optional<PillOption> option = findMatchingOption(stack, requirement.options());
        if (option.isEmpty()) {
            return HandBreakthroughAidResult.NOT_APPLICABLE;
        }
        if (cultivation.isBreakthroughAssisted()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.aid_exists"), true);
            return HandBreakthroughAidResult.BLOCKED;
        }

        cultivation.setBreakthroughPillBonus(option.get().bonus());
        SyncCultivationDataPacket.send(player, cultivation);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.aid_applied",
                requirement.name(), percent(option.get().bonus())), true);
        return HandBreakthroughAidResult.APPLIED;
    }

    private static double getSpiritEyeBonus(ServerPlayer player) {
        SpiritualAuraManager.AuraInfo auraInfo = SpiritualAuraManager.getAuraInfo(player.level(), player.blockPosition());
        return auraInfo.leyline() ? 0.15D : 0.0D;
    }

    private static double getTechniqueQualityBonus(ServerPlayer player, PlayerCultivation cultivation) {
        if (player.getServer() == null || cultivation.getLearnedTechniques().isEmpty()) return 0.0D;
        return cultivation.getLearnedTechniques().stream()
                .map(id -> TechniqueDataManager.getTechnique(player.getServer(), id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .mapToDouble(TechniqueDataManager::getBreakthroughQualityBonus)
                .max()
                .orElse(0.0D);
    }

    private static boolean consumeBreakthroughResource(ServerPlayer player, PlayerCultivation cultivation) {
        if (cultivation.isBreakthroughAssisted()) {
            return true;
        }
        ResourceRequirement requirement = getBreakthroughResourceRequirement(player, cultivation);
        if (requirement.satisfiedWithoutItem()) {
            return true;
        }
        if (player.getAbilities().instabuild) {
            cultivation.setBreakthroughPillBonus(Math.max(0.05D, requirement.bestPossibleBonus()));
            return true;
        }
        Optional<PillOption> best = findBestAvailableOption(player, requirement.options());
        if (best.isPresent()) {
            consumeOne(player, best.get().item());
            cultivation.setBreakthroughPillBonus(best.get().bonus());
            return true;
        }
        return false;
    }

    private static ResourceRequirement getBreakthroughResourceRequirement(ServerPlayer player, PlayerCultivation cultivation) {
        boolean assisted = cultivation.isBreakthroughAssisted() || player.getAbilities().instabuild;
        Realm target = cultivation.getNextBreakthroughRealm();
        if (requiresFoundationBuildingPill(cultivation)) {
            return itemRequirement(player, "foundation", assisted, List.of(
                    option(ModItems.FOUNDATION_BUILDING_PILL_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.FOUNDATION_BUILDING_PILL_HIGH.get(), PillQuality.HIGH),
                    option(ModItems.FOUNDATION_BUILDING_PILL_MID.get(), PillQuality.MEDIUM),
                    option(ModItems.FOUNDATION_BUILDING_PILL_LOW.get(), PillQuality.LOW)));
        }
        if (target == Realm.CORE_FORMATION && cultivation.getRealm() == Realm.FOUNDATION_ESTABLISHMENT) {
            return itemRequirement(player, "essence_condensing", assisted, List.of(
                    option(ModItems.ESSENCE_CONDENSING_PILL_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.ESSENCE_CONDENSING_PILL_HIGH.get(), PillQuality.HIGH),
                    option(ModItems.ESSENCE_CONDENSING_PILL_MID.get(), PillQuality.MEDIUM),
                    option(ModItems.ESSENCE_CONDENSING_PILL.get(), PillQuality.LOW)));
        }
        if (target == Realm.NASCENT_SOUL && cultivation.getRealm() == Realm.CORE_FORMATION) {
            return itemRequirement(player, "soul_gathering", assisted, List.of(
                    option(ModItems.SOUL_GATHERING_PILL_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.SOUL_GATHERING_PILL_HIGH.get(), PillQuality.HIGH),
                    option(ModItems.SOUL_GATHERING_PILL_MID.get(), PillQuality.MEDIUM),
                    option(ModItems.SOUL_GATHERING_PILL.get(), PillQuality.LOW)));
        }
        if (target == Realm.SOUL_TRANSFORMATION && cultivation.getRealm() == Realm.NASCENT_SOUL) {
            return itemRequirement(player, "clear_void", assisted, List.of(
                    option(ModItems.CLEAR_VOID_PILL_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.CLEAR_VOID_PILL_HIGH.get(), PillQuality.HIGH),
                    option(ModItems.CLEAR_VOID_PILL_MID.get(), PillQuality.MEDIUM),
                    option(ModItems.CLEAR_VOID_PILL.get(), PillQuality.LOW)));
        }
        if (target == Realm.VOID_REFINEMENT && cultivation.getRealm() == Realm.SOUL_TRANSFORMATION) {
            if (cultivation.hasCompleteFiveElements()) {
                return new ResourceRequirement(resourceName("five_elements"), 1, 1, assisted, List.of(), true);
            }
            return itemRequirement(player, "marrow_cleansing_high", assisted, List.of(
                    option(ModItems.MARROW_CLEANSING_PILL_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.MARROW_CLEANSING_PILL_HIGH.get(), PillQuality.HIGH)));
        }
        if (target == Realm.UNITY && cultivation.getRealm() == Realm.VOID_REFINEMENT) {
            return itemRequirement(player, "longevity", assisted, List.of(
                    option(ModItems.LONGEVITY_PILL_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.LONGEVITY_PILL_HIGH.get(), PillQuality.HIGH),
                    option(ModItems.LONGEVITY_PILL_MID.get(), PillQuality.MEDIUM),
                    option(ModItems.LONGEVITY_PILL.get(), PillQuality.LOW)));
        }
        if (target == Realm.MAHAYANA && cultivation.getRealm() == Realm.UNITY) {
            return itemRequirement(player, "blood_qi_high", assisted, List.of(
                    option(ModItems.BLOOD_QI_PILL_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.BLOOD_QI_PILL_HIGH.get(), PillQuality.HIGH)));
        }
        if (target == Realm.TRIBULATION && cultivation.getRealm() == Realm.MAHAYANA) {
            return itemRequirement(player, "return_yang_high", assisted, List.of(
                    option(ModItems.RETURN_YANG_TRUE_WATER_SUPREME.get(), PillQuality.SUPREME),
                    option(ModItems.RETURN_YANG_TRUE_WATER_HIGH.get(), PillQuality.HIGH)));
        }
        if (target == Realm.TRUE_IMMORTAL && cultivation.getRealm() == Realm.TRIBULATION) {
            return itemRequirement(player, "poison_dragon_supreme", assisted, List.of(
                    option(ModItems.POISON_DRAGON_PEARL_SUPREME.get(), PillQuality.SUPREME)));
        }
        // M5: 批量目录物品可能缺失/未绑定，空指针防护——缺失时放行该需求并记录警告，不因数据缺陷阻断突破。
        var jiangchenPill = ModBulkItems.byId().get("jiangchen_pill");
        if (jiangchenPill == null || !jiangchenPill.isPresent()) {
            SeekingImmortalsMod.LOGGER.warn("Bulk item jiangchen_pill missing; breakthrough resource requirement waived");
            return new ResourceRequirement(resourceName("jiangchen"), 1, 1, assisted, List.of(), true);
        }
        return itemRequirement(player, "jiangchen", assisted, List.of(
                option(jiangchenPill.get(), PillQuality.LOW)));
    }

    private static boolean requiresFoundationBuildingPill(PlayerCultivation cultivation) {
        return cultivation.getRealm() == Realm.QI_REFINING && cultivation.getStage() == RealmStage.LAYER_13;
    }

    private static void formGoldCoreIfNeeded(ServerPlayer player, PlayerCultivation cultivation, PlayerCultivation.BreakthroughAttemptResult result) {
        if (result.oldRealm() != Realm.FOUNDATION_ESTABLISHMENT || result.newRealm() != Realm.CORE_FORMATION) return;
        int score = GoldCoreGrade.calculateScore(cultivation.getSpiritualRoot(),
                cultivation.getSpiritualRootPurity(),
                cultivation.getSpecialPhysique(),
                result.chanceBreakdown().pillBonus(),
                result.chanceBreakdown().techniqueQualityBonus(),
                result.chanceBreakdown().spiritEyeBonus() > 0.0D,
                cultivation.getBodyRef(),
                cultivation.getQiDevRisk());
        if (cultivation.formGoldCoreIfAbsent(score)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.gold_core.formed",
                    cultivation.getGoldCoreGradeName(), cultivation.getGoldCoreScore()), false);
        }
    }

    private static double getPreviewPillBonus(ServerPlayer player, PlayerCultivation cultivation, ResourceRequirement requirement) {
        if (cultivation.isBreakthroughAssisted()) return cultivation.getBreakthroughPillBonus();
        if (player.getAbilities().instabuild && !requirement.satisfiedWithoutItem()) {
            return Math.max(0.05D, requirement.bestPossibleBonus());
        }
        return findBestAvailableOption(player, requirement.options())
                .map(PillOption::bonus)
                .orElse(0.0D);
    }

    private static ResourceRequirement itemRequirement(ServerPlayer player, String key, boolean assisted, List<PillOption> options) {
        return new ResourceRequirement(resourceName(key), countOptions(player, options), 1, assisted, options, false);
    }

    private static Component resourceName(String key) {
        return Component.translatable("message.seeking_immortals.breakthrough.resource." + key);
    }

    private static PillOption option(Item item, PillQuality quality) {
        return new PillOption(item, quality.getBreakthroughBonus());
    }

    private static Optional<PillOption> findBestAvailableOption(ServerPlayer player, List<PillOption> options) {
        for (PillOption option : options) {
            if (countItem(player, option.item()) > 0) return Optional.of(option);
        }
        return Optional.empty();
    }

    private static Optional<PillOption> findMatchingOption(ItemStack stack, List<PillOption> options) {
        for (PillOption option : options) {
            if (stack.is(option.item())) return Optional.of(option);
        }
        return Optional.empty();
    }

    private static void consumeOne(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) continue;
            stack.shrink(1);
            return;
        }
    }

    private static int countOptions(ServerPlayer player, List<PillOption> options) {
        int count = 0;
        for (PillOption option : options) {
            count += countItem(player, option.item());
        }
        return count;
    }

    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static int percent(double value) {
        return (int)Math.round(value * 100.0D);
    }

    private record PillOption(Item item, double bonus) {}

    private record ResourceRequirement(Component name, int owned, int required, boolean assisted,
                                       List<PillOption> options, boolean satisfiedWithoutItem) {
        double bestPossibleBonus() {
            return options.stream().mapToDouble(PillOption::bonus).max().orElse(0.0D);
        }
    }
}
