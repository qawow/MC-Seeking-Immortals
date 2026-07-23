package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.network.VisualEventPacket;
import com.xunxian.seekingimmortals.visual.AuthoredVisualCatalog;
import com.xunxian.seekingimmortals.visual.VisualEventDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Rideable spirit boat / cloud vehicle (Wave49 vehicle depth).
 */
public class SpiritBoatEntity extends Entity implements GeoEntity {
    private static final String DEFAULT_VEHICLE_ID = "spirit_boat";
    private static final EntityDataAccessor<String> DATA_VEHICLE_ID =
            SynchedEntityData.defineId(SpiritBoatEntity.class, EntityDataSerializers.STRING);
    private int lifeTicks = 20 * 90;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public SpiritBoatEntity(EntityType<? extends SpiritBoatEntity> type, Level level) {
        super(type, level);
    }

    public SpiritBoatEntity(Level level, double x, double y, double z, String vehicleId, int lifeTicks) {
        this(ModEntities.SPIRIT_BOAT.get(), level);
        setPos(x, y, z);
        configure(vehicleId, lifeTicks);
    }

    public void configure(String vehicleId, int lifeTicks) {
        setVehicleId(vehicleId);
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
            if (tickCount % 40 == 0 && level() instanceof ServerLevel serverLevel
                    && AuthoredVisualCatalog.resolve("vehicle:" + vehicleId()).isPresent()) {
                VisualEventDispatcher.entity(serverLevel, "vehicle", vehicleId(),
                        VisualEventPacket.Lifecycle.UPDATE,
                        getPassengers().isEmpty() ? "DOCKED" : "CRUISE", this,
                        VisualEventDispatcher.entityKey("vehicle", this, vehicleId()),
                        80, 0, 1.0D, getPassengers().isEmpty() ? 8 : 18,
                        serverLevel.getGameTime() ^ getUUID().getLeastSignificantBits(), 1);
            }
        }
        // 权威移动只在服务端计算并应用，客户端依赖实体追踪插值，避免双端各自模拟导致失步/抖动
        if (!level().isClientSide) {
            Entity controller = getFirstPassenger();
            if (controller instanceof Player player) {
                setYRot(player.getYRot());
                Vec3 look = player.getLookAngle();
                double speed = vehicleId().contains("cloud") ? 0.55D : 0.42D;
                double dy = player.getXRot() < -15.0F ? 0.16D : (player.getXRot() > 25.0F ? -0.12D : 0.02D);
                setDeltaMovement(look.x * speed, dy, look.z * speed);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.9D));
            }
            move(MoverType.SELF, getDeltaMovement());
        }
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
        entityData.define(DATA_VEHICLE_ID, DEFAULT_VEHICLE_ID);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Life", Tag.TAG_INT)) {
            lifeTicks = Math.max(1, tag.getInt("Life"));
        }
        String savedVehicleId = tag.contains("VehicleId", Tag.TAG_STRING)
                ? tag.getString("VehicleId")
                : tag.contains("vehicleId", Tag.TAG_STRING)
                ? tag.getString("vehicleId")
                : tag.getString("vehicle_id");
        setVehicleId(savedVehicleId);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", lifeTicks);
        tag.putString("VehicleId", vehicleId());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public String vehicleId() {
        return SyncedVisualIdentity.boundedKey(entityData.get(DATA_VEHICLE_ID), DEFAULT_VEHICLE_ID);
    }

    public String getVehicleId() {
        return vehicleId();
    }

    public void setVehicleId(String vehicleId) {
        entityData.set(DATA_VEHICLE_ID,
                SyncedVisualIdentity.boundedKey(vehicleId, DEFAULT_VEHICLE_ID));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D || !getPassengers().isEmpty()) {
                state.setAnimation(RawAnimation.begin().thenLoop("fly"));
            } else {
                state.setAnimation(RawAnimation.begin().thenLoop("idle"));
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
