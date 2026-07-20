package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.catalog.FlightVehicleService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/**
 * Right-click authority for bulk equipment carriers.
 */
public final class CatalogEquipmentService {
    private CatalogEquipmentService() {}

    public enum Mode {
        VEHICLE("vehicle"),
        PUPPET("puppet"),
        FURNACE("furnace"),
        UNKNOWN("unknown");

        final String key;

        Mode(String key) {
            this.key = key;
        }
    }

    public record Result(boolean success, boolean consume) {
        public InteractionResultHolder<ItemStack> holder(ItemStack stack) {
            if (!success) {
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.success(stack);
        }

        public InteractionResult interaction() {
            return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
    }

    public static String modeKey(String catalogId) {
        return resolveMode(catalogId).key;
    }

    public static Mode resolveMode(String catalogId) {
        String id = normalize(catalogId);
        if (id.contains("puppet")) {
            return Mode.PUPPET;
        }
        if (id.startsWith("alchemy_furnace") || id.contains("furnace_g")) {
            return Mode.FURNACE;
        }
        if (id.contains("boat") || id.contains("raft") || id.contains("ferry")
                || id.contains("sedan") || id.contains("cart") || id.contains("vehicle")) {
            return Mode.VEHICLE;
        }
        return Mode.UNKNOWN;
    }

    public static Result use(ServerPlayer player, ItemStack stack, String catalogId, BlockPos placeAt) {
        if (player == null || stack == null || stack.isEmpty()) {
            return new Result(false, false);
        }
        Mode mode = resolveMode(catalogId);
        boolean ok = switch (mode) {
            case VEHICLE -> FlightVehicleService.board(player, normalize(catalogId));
            case PUPPET -> SummonHonestMvpService.summonProxy(player, normalize(catalogId));
            case FURNACE -> placeFurnace(player, placeAt, catalogId);
            case UNKNOWN -> {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.catalog_equipment.unknown", normalize(catalogId)), true);
                yield false;
            }
        };
        if (ok && mode == Mode.FURNACE && !player.getAbilities().instabuild) {
            stack.shrink(1);
            return new Result(true, true);
        }
        // Vehicles and puppets keep the equipment token as a reusable key / command token.
        return new Result(ok, false);
    }

    private static boolean placeFurnace(ServerPlayer player, BlockPos placeAt, String catalogId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos target = placeAt;
        if (target == null) {
            target = player.blockPosition().relative(player.getDirection());
        }
        if (!level.getBlockState(target).canBeReplaced()) {
            target = player.blockPosition();
        }
        if (!level.getBlockState(target).canBeReplaced()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_equipment.no_space"), true);
            return false;
        }
        Block block = furnaceBlock(catalogId);
        BlockState state = block.defaultBlockState();
        level.setBlock(target, state, 3);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_equipment.furnace_placed",
                Component.translatable("item.seeking_immortals." + normalize(catalogId))), true);
        return true;
    }

    private static Block furnaceBlock(String catalogId) {
        String id = normalize(catalogId);
        if (id.contains("g3") || id.contains("tier_3") || id.endsWith("_3")) {
            return ModBlocks.ALCHEMY_FURNACE_TIER_3.get();
        }
        if (id.contains("g2") || id.contains("tier_2") || id.endsWith("_2")) {
            return ModBlocks.ALCHEMY_FURNACE_TIER_2.get();
        }
        if (id.contains("g4") || id.contains("tier_4")) {
            return ModBlocks.ALCHEMY_FURNACE_TIER_4.get();
        }
        if (id.contains("g5") || id.contains("tier_5")) {
            return ModBlocks.ALCHEMY_FURNACE_TIER_5.get();
        }
        return ModBlocks.ALCHEMY_FURNACE.get();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
