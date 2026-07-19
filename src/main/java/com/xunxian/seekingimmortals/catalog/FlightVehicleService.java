package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.SpiritBoatEntity;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.structure.FlyingBoatDockStructure;
import com.xunxian.seekingimmortals.worldpack.FlyingAuthorityPolicy;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Binds text-material flight_vehicles to existing item carriers and optional fuel costs.
 */
public final class FlightVehicleService {
    private FlightVehicleService() {}

    public record CraftCost(String itemId, int count) {}

    public static int vehicleCount() {
        return TextMaterialCatalogService.builtin().flightBindings().size();
    }

    public static Optional<TextMaterialCatalogService.FlightBinding> find(String id) {
        return TextMaterialCatalogService.builtin().findFlight(id);
    }

    /**
     * Attempts to board a vehicle by id: realm gate + optional fuel consume, then spawns a rideable SpiritBoatEntity.
     */
    public static boolean board(ServerPlayer player, String vehicleId) {
        Optional<TextMaterialCatalogService.FlightBinding> optional = find(vehicleId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.flight_vehicle.unknown", vehicleId), false);
            return false;
        }
        TextMaterialCatalogService.FlightBinding vehicle = optional.get();
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!vehicle.realmMin().isBlank()
                    && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), vehicle.realmMin())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.flight_vehicle.realm_too_low",
                        vehicle.display(), vehicle.realmMin()), false);
                return;
            }
            if (!FlyingAuthorityPolicy.allowsVehicle(player, vehicle.id())) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.flight_vehicle.dimension_denied", vehicle.display()), false);
                return;
            }
            // Optional M07 dock formed bonus: when standing on a complete flying_boat_dock, extend life.
            boolean docked = isNearFormedDock(player);
            boolean requiresFuel = !player.getAbilities().instabuild
                    && vehicle.fuelCount() > 0 && !vehicle.fuelItem().isBlank();
            Item fuel = requiresFuel ? resolve(vehicle.fuelItem()) : null;
            if (requiresFuel && (fuel == null || fuel == Items.AIR || !has(player, fuel, vehicle.fuelCount()))) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.flight_vehicle.missing_fuel",
                            vehicle.fuelItem(), vehicle.fuelCount()), false);
                    return;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            int life = 20 * (60 + Math.max(0, (int) Math.round(vehicle.speed() * 10.0D)));
            if (docked) {
                life += 20 * 30;
            }
            SpiritBoatEntity boat = new SpiritBoatEntity(level, player.getX(), player.getY() + 0.2D, player.getZ(),
                    vehicle.id(), life);
            if (!level.addFreshEntity(boat)) {
                return;
            }
            if (!player.startRiding(boat)) {
                boat.discard();
                return;
            }
            if (requiresFuel && !consume(player, fuel, vehicle.fuelCount())) {
                player.stopRiding();
                boat.discard();
                return;
            }
            player.displayClientMessage(Component.translatable("message.seeking_immortals.flight_vehicle.boarded",
                    vehicle.display(), vehicle.carrierItem(), String.valueOf(vehicle.speed())), true);
            ok[0] = true;
        });
        return ok[0];
    }

    public static List<CraftCost> windFeatherRaftRecipe() {
        return List.of(
                new CraftCost("wind_feather", 4),
                new CraftCost("spirit_silk", 4),
                new CraftCost("kunwu_copper", 2),
                new CraftCost("spirit_stone_shard", 8));
    }

    public static boolean craftWindFeatherRaftTicket(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel)) {
            return false;
        }
        if (!isNearFormedDock(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.flight_vehicle.blueprint_requires_dock"), true);
            return false;
        }
        Map<Item, Integer> costs = new LinkedHashMap<>();
        costs.put(ModItems.WIND_FEATHER.get(), 4);
        costs.put(ModItems.SPIRIT_SILK.get(), 4);
        costs.put(ModItems.KUNWU_COPPER.get(), 2);
        costs.put(ModItems.SPIRIT_STONE_SHARD.get(), 8);
        if (!player.getAbilities().instabuild && !hasAll(player, costs)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.flight_vehicle.blueprint_missing_materials"), true);
            return false;
        }
        if (!player.getAbilities().instabuild && !consumeAll(player, costs)) {
            return false;
        }
        InventoryDeliveryService.giveOrDrop(player, new ItemStack(ModItems.WIND_FEATHER_RAFT_TICKET.get()));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.flight_vehicle.blueprint_crafted"), true);
        return true;
    }

    private static Item resolve(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(location);
    }

    private static boolean consume(ServerPlayer player, Item item, int count) {
        if (!has(player, item, count)) {
            return false;
        }
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining <= 0;
    }

    private static boolean has(ServerPlayer player, Item item, int count) {
        int total = 0;
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (stack.is(item)) {
                total += stack.getCount();
                if (total >= count) {
                    return true;
                }
            }
        }
        return count <= 0;
    }

    private static boolean hasAll(ServerPlayer player, Map<Item, Integer> costs) {
        return costs.entrySet().stream().allMatch(entry -> has(player, entry.getKey(), entry.getValue()));
    }

    private static boolean consumeAll(ServerPlayer player, Map<Item, Integer> costs) {
        if (!hasAll(player, costs)) {
            return false;
        }
        for (Map.Entry<Item, Integer> entry : costs.entrySet()) {
            if (!consume(player, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * M07 flying_boat_dock formed check: treat SPIRIT_GATHERING_ARRAY ring + SPIRIT_ORE masts near player
     * as a valid dock (bulk item flying_boat_dock is catalog-only; structure reuses placeable blocks).
     */
    public static boolean isNearFormedDock(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos origin = player.blockPosition();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos center = origin.offset(dx, dy, dz);
                    FlyingBoatDockStructure.CheckResult check = FlyingBoatDockStructure.validate(
                            level,
                            center,
                            ModBlocks.SPIRIT_GATHERING_ARRAY.get(),
                            ModBlocks.SPIRIT_ORE.get());
                    if (check.complete()) {
                        return true;
                    }
                    // also accept long-range array as a "grand dock" variant
                    if (com.xunxian.seekingimmortals.structure.ImmortalTeleportGrandArrayStructure.isFormed(
                            level, center, ModBlocks.LONG_RANGE_TELEPORT_ARRAY.get(), ModBlocks.SPIRIT_ORE.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
