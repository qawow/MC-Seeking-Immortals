package com.xunxian.seekingimmortals.catalog;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightVehicleServiceTest {
    @Test
    void windFeatherBlueprintUsesAuthoredVehicleMaterials() {
        Map<String, Integer> costs = FlightVehicleService.windFeatherRaftRecipe().stream()
                .collect(Collectors.toMap(FlightVehicleService.CraftCost::itemId,
                        FlightVehicleService.CraftCost::count));
        assertEquals(4, costs.get("wind_feather"));
        assertEquals(4, costs.get("spirit_silk"));
        assertEquals(2, costs.get("kunwu_copper"));
        assertEquals(8, costs.get("spirit_stone_shard"));
    }
}
