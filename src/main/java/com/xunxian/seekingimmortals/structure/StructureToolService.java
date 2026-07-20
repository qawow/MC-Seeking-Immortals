package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Portable meta tools for M07 operational stations that ship only as bulk carriers:
 * structure_repair_bench (inspect/repair) and structure_blueprint_table (sequence preview/form).
 */
public final class StructureToolService {
    private static final int SCAN_RADIUS = 4;
    private static final int SCAN_Y = 2;

    private StructureToolService() {}

    public static Optional<InteractionResultHolder<ItemStack>> tryUse(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        String id = catalogId(stack);
        if (id.isBlank()) {
            return Optional.empty();
        }
        return switch (id) {
            case "structure_repair_bench" -> Optional.of(useRepairBench(player, stack));
            case "structure_blueprint_table" -> Optional.of(useBlueprintTable(player, stack));
            default -> Optional.empty();
        };
    }

    private static InteractionResultHolder<ItemStack> useRepairBench(ServerPlayer player, ItemStack stack) {
        NearbyStation nearby = findNearestFormed(player);
        if (nearby == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.structure_tool.no_station"), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean ok;
        if (player.isShiftKeyDown()) {
            // Sneak: repair if operational/damaged; otherwise attempt commission form.
            double efficiency = MultiblockOperationalService.efficiencyAt(
                    player.level(), nearby.stationId(), nearby.origin());
            if (efficiency <= 0.0D) {
                ok = MultiblockOperationalService.form(player, nearby.stationId(), nearby.origin());
            } else {
                ok = MultiblockOperationalService.repair(player, nearby.stationId(), nearby.origin());
            }
        } else {
            ok = MultiblockOperationalService.inspect(player, nearby.stationId(), nearby.origin());
        }
        return ok ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    private static InteractionResultHolder<ItemStack> useBlueprintTable(ServerPlayer player, ItemStack stack) {
        NearbyStation nearby = findNearestFormed(player);
        if (nearby == null) {
            // Still useful: list a few catalog stations the player can aim for.
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.structure_tool.blueprint_hint"), false);
            int shown = 0;
            for (MultiblockStructureCatalog.StructureEntry entry
                    : MultiblockStructureCatalog.builtin().structures().values()) {
                if (shown >= 6) {
                    break;
                }
                player.displayClientMessage(Component.literal(
                        " · " + entry.display() + " (" + entry.id() + ")"), false);
                shown++;
            }
            return InteractionResultHolder.success(stack);
        }
        if (player.isShiftKeyDown()) {
            boolean ok = MultiblockOperationalService.form(player, nearby.stationId(), nearby.origin());
            return ok ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        MultiblockStructureCatalog.StructureEntry entry = MultiblockStructureCatalog.builtin()
                .find(nearby.stationId()).orElse(null);
        String display = entry == null ? nearby.stationId() : entry.display();
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.structure_tool.blueprint_target",
                display, nearby.stationId()), false);
        List<MultiblockSequenceDisplayCatalog.SequenceEntry> sequences =
                MultiblockSequenceDisplayCatalog.builtin().forStructure(nearby.stationId());
        if (sequences.isEmpty()) {
            // Fallback inspect so the tool always surfaces operational state.
            MultiblockOperationalService.inspect(player, nearby.stationId(), nearby.origin());
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.structure_tool.blueprint_no_sequence"), false);
        } else {
            MultiblockSequenceDisplayCatalog.SequenceEntry seq = sequences.get(0);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.structure_tool.blueprint_sequence",
                    seq.display(), seq.steps().size()), false);
            int stepShown = 0;
            for (MultiblockSequenceDisplayCatalog.SequenceStep step : seq.steps()) {
                if (stepShown >= 8) {
                    break;
                }
                player.displayClientMessage(Component.literal(String.format(
                        Locale.ROOT, "  %d. [%s] %s — %s",
                        step.order(), step.actor(), step.action(), step.note())), false);
                stepShown++;
            }
        }
        return InteractionResultHolder.success(stack);
    }

    static NearbyStation findNearestFormed(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return null;
        }
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        NearbyStation best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -1; dy <= SCAN_Y; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    double dist = pos.distSqr(origin);
                    if (dist >= bestDist) {
                        continue;
                    }
                    for (MultiblockStructureCatalog.StructureEntry entry
                            : MultiblockStructureCatalog.builtin().structures().values()) {
                        if (MultiblockStationService.isStationFormed(level, entry.id(), pos)) {
                            best = new NearbyStation(entry.id(), pos);
                            bestDist = dist;
                            break;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static String catalogId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        if (stack.getItem() instanceof com.xunxian.seekingimmortals.item.CatalogCarrierItem carrier) {
            String id = carrier.catalogId();
            if (id != null && !id.isBlank()) {
                return id.trim().toLowerCase(Locale.ROOT);
            }
        }
        var key = stack.getItem().builtInRegistryHolder().key();
        if (key == null) {
            return "";
        }
        return key.location().getPath().toLowerCase(Locale.ROOT);
    }

    record NearbyStation(String stationId, BlockPos origin) {}
}
