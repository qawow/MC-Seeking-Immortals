package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
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
    private boolean terminalVfxSent;

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
            if (isRemoved()) {
                return;
            }
        }

        Vec3 movement = getDeltaMovement();
        move(MoverType.SELF, movement);
        setPos(getX(), getY(), getZ());
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);

        if (++life > 80 || !level().isLoaded(blockPosition())) {
            sendDissipate();
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner) return;
        boolean damaged = target.hurt(level().damageSources().indirectMagic(this, owner), (float) damage);
        if (damaged && slowsTarget && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        spawnImpactVisual(result.getLocation());
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) {
            spawnImpactVisual(result.getLocation());
            discard();
        }
    }

    private void spawnImpactVisual(Vec3 position) {
        if (level() instanceof ServerLevel serverLevel) {
            terminalVfxSent = true;
            TechniqueLifecycleVfxService.projectileImpact(
                    serverLevel,
                    TechniqueVfxPalette.Family.METAL,
                    TechniqueVfxPacket.Motif.BLADE,
                    position,
                    0.82D,
                    36,
                    getId() * 163L);
        }
    }

    private void sendDissipate() {
        if (terminalVfxSent || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        terminalVfxSent = true;
        TechniqueLifecycleVfxService.projectileDissipate(
                serverLevel,
                TechniqueVfxPalette.Family.METAL,
                TechniqueVfxPacket.Motif.BLADE,
                position(),
                0.78D,
                getId() * 173L);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            sendDissipate();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getDouble("Damage");
        slowsTarget = tag.getBoolean("SlowsTarget");
        life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("Damage", damage);
        tag.putBoolean("SlowsTarget", slowsTarget);
        tag.putInt("Life", life);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
