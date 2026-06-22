package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation.QiDeviationTier;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class BreakthroughService {
    private BreakthroughService() {}

    public static void attempt(ServerPlayer player) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> attempt(player, cultivation),
                () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.no_data"), true));
    }

    private static void attempt(ServerPlayer player, PlayerCultivation cultivation) {
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
                preview.techniqueQualityBonus()));
        SyncCultivationDataPacket.send(player, cultivation);
        if (result.success()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.success",
                    result.newRealm().getDisplayName(),
                    result.newStage().getDisplayName(),
                    percent(result.chance())), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.bonus_detail",
                    percent(result.chanceBreakdown().pillBonus()),
                    percent(result.chanceBreakdown().spiritEyeBonus()),
                    percent(result.chanceBreakdown().techniqueQualityBonus()),
                    percent(result.chanceBreakdown().obsessionBonus())), false);
            return;
        }
        if (result.status() == PlayerCultivation.BreakthroughAttemptStatus.FAILURE) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.failure",
                    percent(result.chance()),
                    result.qiDeviationRisk()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.bonus_detail",
                    percent(result.chanceBreakdown().pillBonus()),
                    percent(result.chanceBreakdown().spiritEyeBonus()),
                    percent(result.chanceBreakdown().techniqueQualityBonus()),
                    percent(result.chanceBreakdown().obsessionBonus())), false);
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
                percent(preview.obsessionBonus()),
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
                dropHalfInventory(player, random);
                player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.qi_deviation.extreme"), false);
            }
            default -> {}
        }
        SyncCultivationDataPacket.send(player, cultivation);
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

    private static void dropHalfInventory(ServerPlayer player, RandomSource random) {
        Inventory inv = player.getInventory();
        java.util.List<Integer> occupiedSlots = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) occupiedSlots.add(i);
        }
        java.util.Collections.shuffle(occupiedSlots, new java.util.Random(random.nextLong()));
        int dropCount = occupiedSlots.size() / 2;
        for (int i = 0; i < dropCount; i++) {
            int slot = occupiedSlots.get(i);
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) {
                player.drop(stack.copy(), true, false);
                inv.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    public static PlayerCultivation.BreakthroughChanceBreakdown preview(ServerPlayer player, PlayerCultivation cultivation) {
        PlayerCultivation.BreakthroughChanceModifiers modifiers = new PlayerCultivation.BreakthroughChanceModifiers(
                cultivation.getBreakthroughPillBonus(),
                getSpiritEyeBonus(player),
                getTechniqueQualityBonus(player, cultivation));
        return cultivation.getBreakthroughChanceBreakdown(modifiers);
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
        if (player.getAbilities().instabuild) {
            cultivation.setBreakthroughPillBonus(0.05D);
            return true;
        }
        if (requiresFoundationBuildingPill(cultivation)) {
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.is(ModItems.FOUNDATION_BUILDING_PILL_LOW.get())) continue;
                stack.shrink(1);
                cultivation.setBreakthroughPillBonus(0.05D);
                return true;
            }
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ModItems.BREAKTHROUGH_PILL.get())) continue;
            stack.shrink(1);
            cultivation.setBreakthroughPillBonus(0.05D);
            return true;
        }
        return false;
    }

    private static ResourceRequirement getBreakthroughResourceRequirement(ServerPlayer player, PlayerCultivation cultivation) {
        boolean assisted = cultivation.isBreakthroughAssisted() || player.getAbilities().instabuild;
        if (requiresFoundationBuildingPill(cultivation)) {
            return new ResourceRequirement(Component.translatable("item.seeking_immortals.foundation_building_pill_low"), countItem(player, ModItems.FOUNDATION_BUILDING_PILL_LOW.get()), 1, assisted);
        }
        return new ResourceRequirement(Component.translatable("item.seeking_immortals.breakthrough_pill"), countItem(player, ModItems.BREAKTHROUGH_PILL.get()), 1, assisted);
    }

    private static boolean requiresFoundationBuildingPill(PlayerCultivation cultivation) {
        return cultivation.getRealm() == Realm.QI_REFINING && cultivation.getStage() == RealmStage.LAYER_13;
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

    private record ResourceRequirement(Component name, int owned, int required, boolean assisted) {}
}
