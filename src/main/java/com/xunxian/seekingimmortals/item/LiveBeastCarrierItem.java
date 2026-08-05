package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.LiveCaptureCarrierService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Y-B: single live-beast transport carrier (阴芝马活体载体).
 *
 * <p>Non-stackable by construction so one carrier is exactly one captured instance; the carrier is
 * inert on use — value is settled by the receiving station, not by right-clicking it.</p>
 */
public class LiveBeastCarrierItem extends Item {
    public LiveBeastCarrierItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!LiveCaptureCarrierService.isCarrier(stack)) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.live_carrier.empty"));
            return;
        }
        String beastId = LiveCaptureCarrierService.beastId(stack);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.live_carrier.holds",
                PlayerDisplayText.safeLiteral(displayOf(beastId), "text.seeking_immortals.unknown_item")));
        tooltip.add(LiveCaptureCarrierService.isLive(stack)
                ? Component.translatable("tooltip.seeking_immortals.live_carrier.state_live")
                : Component.translatable("tooltip.seeking_immortals.live_carrier.state_degraded"));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.live_carrier.transit_hint"));
    }

    private static String displayOf(String beastId) {
        return BeastBestiaryService.find(beastId)
                .map(BeastBestiaryService.BeastEntry::display)
                .filter(PlayerDisplayText::isSafe)
                .orElse("");
    }
}
