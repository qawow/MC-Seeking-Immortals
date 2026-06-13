package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
        if (!consumeBreakthroughResource(player, cultivation)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.need_resource"), true);
            return;
        }

        PlayerCultivation.BreakthroughChanceBreakdown preview = preview(player, cultivation);
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
                player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.qi_deviation"), false);
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
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ModItems.BREAKTHROUGH_PILL.get())) continue;
            stack.shrink(1);
            cultivation.setBreakthroughPillBonus(0.05D);
            return true;
        }
        return false;
    }

    private static int percent(double value) {
        return (int)Math.round(value * 100.0D);
    }
}
