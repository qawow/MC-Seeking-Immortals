package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Rideable spirit boat / cloud vehicle (Wave49 vehicle depth).
 */
public class SpiritBoatEntity extends Entity {
    private int lifeTicks = 20 * 90;
    private String vehicleId = "spirit_boat";

    public SpiritBoatEntity(EntityType<? extends SpiritBoatEntity> type, Level level) {
        super(type, level);
    }

    public SpiritBoatEntity(Level level, double x, double y, double z, String vehicleId, int lifeTicks) {
        this(ModEntities.SPIRIT_BOAT.get(), level);
        setPos(x, y, z);
        configure(vehicleId, lifeTicks);
    }

    public void configure(String vehicleId, int lifeTicks) {
        this.vehicleId = vehicleId == null || vehicleId.isBlank() ? "spirit_boat" : vehicleId;
        this.lifeTicks = Math.max(20 * 20, lifeTicks);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            lifeTicks--;
            if (lifeTicks <= 0) {
                ejectPassengers();
                discard();
                return;
            }
        }
        Entity controller = getFirstPassenger();
        if (controller instanceof Player player) {
            setYRot(player.getYRot());
            Vec3 look = player.getLookAngle();
            double speed = vehicleId.contains("cloud") ? 0.55D : 0.42D;
            double dy = player.getXRot() < -15.0F ? 0.16D : (player.getXRot() > 25.0F ? -0.12D : 0.02D);
            setDeltaMovement(look.x * speed, dy, look.z * speed);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.9D));
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.35D;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        lifeTicks = tag.getInt("Life");
        vehicleId = tag.getString("VehicleId");
        if (vehicleId == null || vehicleId.isBlank()) {
            vehicleId = "spirit_boat";
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", lifeTicks);
        tag.putString("VehicleId", vehicleId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
