package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueAffinityCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class SpeedTalismanItem extends Item {
    private static final int QI_COST = 6;
    private static final Realm MIN_REALM = Realm.QI_REFINING;
    private static final String AFFINITY = "风/雷/隐雷";

    public SpeedTalismanItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ItemUsageGateService.GateResult gateCheck = CultivationHelper.get(player)
                    .map(cultivation -> {
                        ItemUsageGateService.ItemRequirement requirement =
                                ItemUsageGateService.ItemRequirement.realm(MIN_REALM);
                        return ItemUsageGateService.canUse(player, requirement);
                    })
                    .orElse(ItemUsageGateService.GateResult.deny("message.seeking_immortals.item_gate.no_cultivation"));

            if (!gateCheck.allowed()) {
                player.displayClientMessage(gateCheck.message(), true);
                return InteractionResultHolder.fail(stack);
            }

            CultivationHelper.get(player).ifPresent(cultivation -> {
                if (!cultivation.consumeQi(QI_COST)) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                    return;
                }
                int duration = 20 * 30 + TechniqueAffinityCalculator.getDurationBonusTicks(cultivation, 20 * 30, AFFINITY);
                int amplifier = 1 + TechniqueAffinityCalculator.getEffectAmplifierBonus(cultivation, AFFINITY);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier));
                if (!player.getAbilities().instabuild) stack.shrink(1);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.speed_talisman.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.talisman.qi_cost", QI_COST).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.talisman.affinity", AFFINITY).withStyle(ChatFormatting.GOLD));
        ItemUsageGateService.ItemRequirement requirement = ItemUsageGateService.ItemRequirement.realm(MIN_REALM);
        ItemUsageGateService.appendRequirementTooltip(stack, tooltip, requirement);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.talisman.use").withStyle(ChatFormatting.GREEN));
    }
}
