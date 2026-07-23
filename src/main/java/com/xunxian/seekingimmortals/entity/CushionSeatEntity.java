package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class CushionSeatEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> DATA_CUSHION_POS =
            SynchedEntityData.defineId(CushionSeatEntity.class, EntityDataSerializers.BLOCK_POS);

    public CushionSeatEntity(EntityType<? extends CushionSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public CushionSeatEntity(Level level, BlockPos cushionPos) {
        this(ModEntities.CUSHION_SEAT.get(), level);
        setCushionPos(cushionPos);
        setPos(cushionPos.getX() + 0.5D, cushionPos.getY() + 0.28D, cushionPos.getZ() + 0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        if (!level().isClientSide) {
            BlockPos cushionPos = getCushionPos();
            if (cushionPos.equals(BlockPos.ZERO)) {
                setCushionPos(blockPosition());
                cushionPos = getCushionPos();
            }
            if (!level().getBlockState(cushionPos).is(ModBlocks.MEDITATION_CUSHION.get()) || getPassengers().isEmpty()) {
                getPassengers().forEach(passenger -> {
                    if (passenger instanceof Player player) {
                        CultivationHelper.get(player).ifPresent(cultivation -> cultivation.setMeditating(false));
                    }
                });
                discard();
            } else {
                setPos(cushionPos.getX() + 0.5D, cushionPos.getY() + 0.28D, cushionPos.getZ() + 0.5D);
            }
        }
    }

    public BlockPos getCushionPos() {
        return entityData.get(DATA_CUSHION_POS);
    }

    public void setCushionPos(BlockPos pos) {
        entityData.set(DATA_CUSHION_POS, pos == null ? BlockPos.ZERO : pos.immutable());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_CUSHION_POS, BlockPos.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setCushionPos(new BlockPos(tag.getInt("CushionX"), tag.getInt("CushionY"), tag.getInt("CushionZ")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        BlockPos cushionPos = getCushionPos();
        tag.putInt("CushionX", cushionPos.getX());
        tag.putInt("CushionY", cushionPos.getY());
        tag.putInt("CushionZ", cushionPos.getZ());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide) {
            if (passenger instanceof Player player) {
                CultivationHelper.get(player).ifPresent(cultivation -> cultivation.setMeditating(false));
            }
            discard();
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
