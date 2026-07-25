package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.SpatialNodeCatalogService;
import com.xunxian.seekingimmortals.worldpack.SpatialNodeNetworkSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/** Artifact compass that resolves the nearest formed spatial-network node. */
public final class SpaceRiftCompassItem extends ArtifactCatalogItem {
    private static final double SCAN_RADIUS = 2048.0D;

    public SpaceRiftCompassItem(Properties properties) {
        super(properties, "space_rift_compass");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (!level.isClientSide && !player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer
                && result.getResult().consumesAction()) {
            scan(serverPlayer);
        }
        return result;
    }

    private static void scan(ServerPlayer player) {
        String dimension = player.serverLevel().dimension().location().toString();
        double maximumDistanceSqr = SCAN_RADIUS * SCAN_RADIUS;
        SpatialNodeNetworkSavedData.NetworkNode nearest = SpatialNodeNetworkSavedData.get(player.serverLevel())
                .all().stream()
                .filter(SpatialNodeNetworkSavedData.NetworkNode::formed)
                .filter(node -> dimension.equals(node.dimensionId()))
                .filter(node -> distanceSqr(player, node) <= maximumDistanceSqr)
                .min(Comparator.comparingDouble(node -> distanceSqr(player, node)))
                .orElse(null);
        if (nearest == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.space_rift_compass.not_found"), true);
            return;
        }
        double dx = nearest.x() + 0.5D - player.getX();
        double dz = nearest.z() + 0.5D - player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        Component name = SpatialNodeCatalogService.builtin().find(nearest.catalogNodeId())
                .map(SpatialNodeCatalogService.Node::display)
                .filter(PlayerDisplayText::isSafe)
                .map(value -> Component.literal(value.trim()))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_spatial_node"));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.space_rift_compass.found",
                name, Component.translatable(SpaceRiftCompassDirection.key(dx, dz)), distance), true);
    }

    private static double distanceSqr(ServerPlayer player, SpatialNodeNetworkSavedData.NetworkNode node) {
        double dx = node.x() + 0.5D - player.getX();
        double dy = node.y() - player.getY();
        double dz = node.z() + 0.5D - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.space_rift_compass")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
