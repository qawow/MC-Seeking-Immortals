package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.SpiritBoatEntity;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/**
 * Binds text-material flight_vehicles to existing item carriers and optional fuel costs.
 */
public final class FlightVehicleService {
    private FlightVehicleService() {}

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
            if (!player.getAbilities().instabuild && vehicle.fuelCount() > 0 && !vehicle.fuelItem().isBlank()) {
                Item fuel = resolve(vehicle.fuelItem());
                if (fuel == null || fuel == Items.AIR || !consume(player, fuel, vehicle.fuelCount())) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.flight_vehicle.missing_fuel",
                            vehicle.fuelItem(), vehicle.fuelCount()), false);
                    return;
                }
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            int life = 20 * (60 + Math.max(0, (int) Math.round(vehicle.speed() * 10.0D)));
            SpiritBoatEntity boat = new SpiritBoatEntity(level, player.getX(), player.getY() + 0.2D, player.getZ(),
                    vehicle.id(), life);
            if (!level.addFreshEntity(boat)) {
                return;
            }
            player.startRiding(boat);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.flight_vehicle.boarded",
                    vehicle.display(), vehicle.carrierItem(), String.valueOf(vehicle.speed())), true);
            ok[0] = true;
        });
        return ok[0];
    }

    private static Item resolve(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(location);
    }

    private static boolean consume(ServerPlayer player, Item item, int count) {
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
}
