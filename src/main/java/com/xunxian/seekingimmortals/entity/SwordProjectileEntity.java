package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class SwordProjectileEntity extends Projectile {
    private double damage = 8.0D;
    private int life;
    private boolean slowsTarget;

    public SwordProjectileEntity(EntityType<? extends SwordProjectileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public SwordProjectileEntity(Level level, LivingEntity owner, Vec3 direction, double damage, boolean slowsTarget) {
        this(ModEntities.SWORD_PROJECTILE.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.slowsTarget = slowsTarget;
        setPos(owner.getEyePosition().add(direction.normalize().scale(0.7D)));
        setDeltaMovement(direction.normalize().scale(1.25D));
        // H9: 标记弹射物来源，防止 onLivingHurt PvP 分支二次重算/吞伤害
        getPersistentData().putBoolean("SeekingImmortalsProjectileDamage", true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
        }

        Vec3 movement = getDeltaMovement();
        move(MoverType.SELF, movement);
        setPos(getX(), getY(), getZ());
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);

        if (++life > 80 || !level().isLoaded(blockPosition())) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner) return;
        target.hurt(level().damageSources().indirectMagic(this, owner), (float) damage);
        if (slowsTarget && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        damage = tag.getDouble("Damage");
        slowsTarget = tag.getBoolean("SlowsTarget");
        life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("Damage", damage);
        tag.putBoolean("SlowsTarget", slowsTarget);
        tag.putInt("Life", life);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
