package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class FireTalismanItem extends Item {
    private static final double BASE_DAMAGE = 5.0D;
    private static final double BASE_SPEED = 1.05D;
    private static final double MAX_SPEED = 1.8D;
    private static final int QI_COST = 10;
    private static final Realm MIN_REALM = Realm.QI_REFINING;

    public FireTalismanItem(Properties properties) {
        super(properties);
    }

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
                double affinity = TechniqueDataManager.getAffinityMultiplier(cultivation, "\u706b/\u96f7/\u9690\u96f7");
                double damage = BASE_DAMAGE * affinity;
                double speed = Math.min(MAX_SPEED, Math.max(BASE_SPEED, BASE_SPEED + (affinity - 1.0D) * 0.25D));
                Vec3 look = player.getLookAngle();
                CultivationFireballEntity fireball = new CultivationFireballEntity(level, player, look, damage, speed);
                level.addFreshEntity(fireball);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.fire_talisman.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.talisman.qi_cost", QI_COST).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.talisman.affinity", "\u706b/\u96f7/\u9690\u96f7").withStyle(ChatFormatting.GOLD));
        ItemUsageGateService.ItemRequirement requirement = ItemUsageGateService.ItemRequirement.realm(MIN_REALM);
        ItemUsageGateService.appendRequirementTooltip(stack, tooltip, requirement);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.talisman.use").withStyle(ChatFormatting.GREEN));
    }
}
