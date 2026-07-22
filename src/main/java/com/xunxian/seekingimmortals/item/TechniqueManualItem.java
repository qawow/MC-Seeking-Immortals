package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.skill.TechniqueGateService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class TechniqueManualItem extends Item {
    private final String source;

    public TechniqueManualItem(Properties properties, String source) {
        super(properties);
        this.source = source;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return InteractionResultHolder.fail(stack);
        }
        Component manualName = PlayerDisplayText.itemName(stack.getItem());

        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            List<TechniqueDataManager.TechniqueEntry> techniques = TechniqueDataManager.getTechniquesBySource(serverPlayer.getServer(), source);
            int learned = 0;
            int blocked = 0;
            for (TechniqueDataManager.TechniqueEntry technique : techniques) {
                if (!TechniqueDataManager.matchesAttributeCondition(cultivation, technique.attribute())) {
                    blocked++;
                    continue;
                }
                TechniqueGateService.GateResult gate = TechniqueGateService.canLearn(serverPlayer, cultivation, technique);
                if (!gate.allowed()) {
                    blocked++;
                    if (gate.messageKey() != null && !gate.messageKey().isBlank()) {
                        player.displayClientMessage(Component.translatable(gate.messageKey(), gate.args()), true);
                    }
                    continue;
                }
                if (cultivation.learnTechnique(technique.id())) {
                    learned++;
                }
            }
            if (techniques.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.technique_manual.empty", manualName), false);
                return;
            }
            // Source methods are a consequence of a successful technique learn, never a gate bypass.
            int methodsGranted = learned > 0
                    ? ManualCatalogService.grantMethodsFromTechniqueSource(serverPlayer, source)
                    : 0;
            if (learned > 0 || methodsGranted > 0) {
                if (learned > 0) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.technique_manual.learned", manualName, learned), false);
                }
                if (methodsGranted > 0) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.technique_manual.methods_granted", methodsGranted), false);
                }
                SyncLearnedTechniquesPacket.send(serverPlayer, cultivation);
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
                if (learned > 0 && !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            } else if (blocked > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.technique_manual.condition_failed", manualName), false);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.technique_manual.already_known", manualName), false);
            }
        }, () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_manual.no_data"), false));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.seeking_immortals.technique_manual.source",
                PlayerDisplayText.itemName(stack.getItem()))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.technique_manual.condition", TechniqueDataManager.describeConditions(source)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.technique_manual.learn", TechniqueDataManager.describeTechniqueNames(source)).withStyle(ChatFormatting.DARK_AQUA));

        // 使用 ItemUsageGateService 显示功法要求提示
        // TechniqueGateService 已经处理了更详细的前置检查，这里只显示基本提示
        tooltip.add(Component.translatable("tooltip.seeking_immortals.technique_manual.use").withStyle(ChatFormatting.GREEN));
    }

    public String getSource() {
        return source;
    }
}
