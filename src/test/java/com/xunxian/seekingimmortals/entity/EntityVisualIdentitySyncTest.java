package com.xunxian.seekingimmortals.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityVisualIdentitySyncTest {
    private static final Path JAVA = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void visualKeysAreNormalizedBoundedAndFormatChecked() {
        assertEquals("technique:fireball",
                SyncedVisualIdentity.boundedKey(" Technique:Fireball ", "fallback"));
        assertEquals("fallback", SyncedVisualIdentity.boundedKey("bad key", "fallback"));
        assertEquals("fallback", SyncedVisualIdentity.boundedKey("a:b:c", "fallback"));
        assertEquals("fallback", SyncedVisualIdentity.boundedKey(
                "x".repeat(SyncedVisualIdentity.MAX_KEY_LENGTH + 1), "fallback"));
        assertEquals("x".repeat(SyncedVisualIdentity.MAX_KEY_LENGTH),
                SyncedVisualIdentity.boundedKey(
                        "x".repeat(SyncedVisualIdentity.MAX_KEY_LENGTH), "fallback"));
    }

    @Test
    void qualificationPreservesDomainAndLegacyRawLookup() {
        assertEquals("technique:fireball",
                SyncedVisualIdentity.qualified("technique", "fireball", "technique:fallback"));
        assertEquals("artifact:fire_cloud_fan",
                SyncedVisualIdentity.qualified(
                        "technique", "artifact:fire_cloud_fan", "technique:fallback"));
        assertEquals("fire_cloud_fan",
                SyncedVisualIdentity.rawId("artifact:fire_cloud_fan"));
    }

    @Test
    void enumOrdinalsAndNamesFailClosed() {
        assertEquals(VisualKind.FIRE,
                SyncedVisualIdentity.byOrdinal(VisualKind.values(), 0, VisualKind.NEUTRAL));
        assertEquals(VisualKind.NEUTRAL,
                SyncedVisualIdentity.byOrdinal(VisualKind.values(), -1, VisualKind.NEUTRAL));
        assertEquals(VisualKind.NEUTRAL,
                SyncedVisualIdentity.byOrdinal(VisualKind.values(), 99, VisualKind.NEUTRAL));
        assertEquals(VisualKind.WATER,
                SyncedVisualIdentity.byName(VisualKind.class, "water", VisualKind.NEUTRAL));
        assertEquals(VisualKind.NEUTRAL,
                SyncedVisualIdentity.byName(VisualKind.class, "unknown", VisualKind.NEUTRAL));
    }

    @Test
    void boatVehicleIdentityUsesSynchedDataAndLegacyNbt() throws Exception {
        String boat = Files.readString(JAVA.resolve("entity/SpiritBoatEntity.java"));
        String renderer = Files.readString(JAVA.resolve("client/SpiritBoatRenderer.java"));

        assertTrue(boat.contains("EntityDataAccessor<String> DATA_VEHICLE_ID"));
        assertTrue(boat.contains("entityData.define(DATA_VEHICLE_ID, DEFAULT_VEHICLE_ID)"));
        assertTrue(boat.contains("entityData.set(DATA_VEHICLE_ID"));
        assertTrue(boat.contains("entityData.get(DATA_VEHICLE_ID)"));
        assertTrue(boat.contains("tag.putString(\"VehicleId\", vehicleId())"));
        assertTrue(boat.contains("tag.contains(\"VehicleId\", Tag.TAG_STRING)"));
        assertTrue(renderer.contains("entity.vehicleId()"));
    }

    @Test
    void projectilesSynchronizeAndPersistAuthoredVisualIdentity() throws Exception {
        for (String file : new String[]{"SwordProjectileEntity.java", "CultivationFireballEntity.java"}) {
            String projectile = Files.readString(JAVA.resolve("entity").resolve(file));
            assertTrue(projectile.contains("DATA_VISUAL_PROFILE_ID"), file);
            assertTrue(projectile.contains("DATA_VISUAL_FAMILY"), file);
            assertTrue(projectile.contains("DATA_VISUAL_TRAIL"), file);
            assertTrue(projectile.contains("EntityDataSerializers.STRING"), file);
            assertTrue(projectile.contains("EntityDataSerializers.INT"), file);
            assertTrue(projectile.contains("setVisualIdentity("), file);
            assertTrue(projectile.contains("getVisualProfileId()"), file);
            assertTrue(projectile.contains("getVisualFamily()"), file);
            assertTrue(projectile.contains("getVisualTrailStyle()"), file);
            assertTrue(projectile.contains("tag.putString(TAG_VISUAL_PROFILE"), file);
            assertTrue(projectile.contains("tag.putInt(TAG_VISUAL_FAMILY"), file);
            assertTrue(projectile.contains("tag.putInt(TAG_VISUAL_TRAIL"), file);
            assertTrue(projectile.contains("SyncedVisualIdentity.byOrdinal("), file);
            assertTrue(projectile.contains("TechniqueLifecycleVfxService.projectileImpact("), file);
        }
    }

    @Test
    void formationCorePublishesActiveStateThroughVanillaUpdatePackets() throws Exception {
        String core = Files.readString(JAVA.resolve("block/entity/FormationCoreBlockEntity.java"));

        assertTrue(core.contains("public boolean isActive()"));
        assertTrue(core.contains("public int remainingTicks()"));
        assertTrue(core.contains("public String visualProfileId()"));
        assertTrue(core.contains("public CompoundTag getUpdateTag()"));
        assertTrue(core.contains("ClientboundBlockEntityDataPacket getUpdatePacket()"));
        assertTrue(core.contains("ClientboundBlockEntityDataPacket.create(this)"));
        assertTrue(core.contains("level.sendBlockUpdated("));
        assertTrue(core.contains("Block.UPDATE_CLIENTS"));
    }

    private enum VisualKind {
        FIRE,
        WATER,
        NEUTRAL
    }
}
