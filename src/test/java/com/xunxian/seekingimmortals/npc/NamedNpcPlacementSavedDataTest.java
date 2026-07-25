package com.xunxian.seekingimmortals.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedNpcPlacementSavedDataTest {
    @Test
    void placementsRoundTripAndReplaceByNpcId() {
        NamedNpcPlacementSavedData data = new NamedNpcPlacementSavedData();
        UUID first = UUID.randomUUID();
        data.record("Npc_Trader", first, "minecraft:overworld", new BlockPos(4, 65, -8));
        data.record("npc_trader", first, "minecraft:overworld", new BlockPos(5, 65, -8));

        NamedNpcPlacementSavedData loaded = NamedNpcPlacementSavedData.load(data.save(new CompoundTag()));

        assertEquals(1, loaded.size());
        NamedNpcPlacementSavedData.Placement placement = loaded.find("NPC_TRADER").orElseThrow();
        assertEquals(first, placement.entityId());
        assertEquals(new BlockPos(5, 65, -8), placement.pos());
        assertTrue(loaded.contains(" npc_trader "));

        loaded.remove("NPC_TRADER");
        assertEquals(0, NamedNpcPlacementSavedData.load(loaded.save(new CompoundTag())).size());
    }
}
