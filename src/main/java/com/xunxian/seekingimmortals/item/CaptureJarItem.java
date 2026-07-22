package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.artifact.ArtifactCaptureService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Capture container item for capture-family artifacts (Wave51/458). */
public class CaptureJarItem extends Item {
    public CaptureJarItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            boolean ok = ArtifactCaptureService.releaseOrCapture(serverPlayer, stack, serverPlayer.isShiftKeyDown());
            return ok ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String id = ArtifactCaptureService.storedId(stack);
        if (id == null || id.isBlank()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.capture_jar.empty"));
        } else {
            // Stored ids are persistence keys; resolve them before putting them in a tooltip.
            tooltip.add(Component.translatable("tooltip.seeking_immortals.capture_jar.stored",
                    PlayerDisplayText.safeLiteral(resolveStoredDisplay(id), "text.seeking_immortals.unknown_item")));
            tooltip.add(Component.translatable("tooltip.seeking_immortals.capture_jar.seal_hint"));
        }
    }

    private static String resolveStoredDisplay(String id) {
        return com.xunxian.seekingimmortals.beast.BeastBestiaryService.find(id)
                .map(com.xunxian.seekingimmortals.beast.BeastBestiaryService.BeastEntry::display)
                .filter(PlayerDisplayText::isSafe)
                .orElse("");
    }
}
