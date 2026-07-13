package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServitorRegistrySavedDataTest {
    @Test
    void capAndDeferredCommandsSurviveSerialization() {
        ServitorRegistrySavedData data = new ServitorRegistrySavedData();
        UUID owner = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        data.register(owner, first, "minecraft:overworld", "FOLLOW", 3);
        data.register(owner, UUID.randomUUID(), "minecraft:the_nether", "GUARD", 3);
        data.register(owner, UUID.randomUUID(), "minecraft:overworld", "STAY", 3);
        ServitorRegistrySavedData.State rejected = data.register(
                owner, UUID.randomUUID(), "minecraft:the_end", "FOLLOW", 3);

        assertTrue(rejected.dismissed());
        assertEquals(3, data.countActive(owner));
        assertEquals(3, data.setStanceAll(owner, "AGGRESSIVE"));
        assertTrue(data.activeFor(owner).stream().allMatch(s -> "AGGRESSIVE".equals(s.stance())));
        ServitorRegistrySavedData.State reloadedFirst = data.register(
                owner, first, "minecraft:overworld", "FOLLOW", 3);
        assertEquals("AGGRESSIVE", reloadedFirst.stance(),
                "entity NBT must not overwrite a deferred owner command when its chunk reloads");

        data.dismissOldest(owner, 1);
        assertEquals(2, data.countActive(owner));

        ServitorRegistrySavedData loaded = ServitorRegistrySavedData.load(data.save(new CompoundTag()));
        assertEquals(2, loaded.countActive(owner));
        assertTrue(loaded.state(first).orElseThrow().dismissed());
    }
}
