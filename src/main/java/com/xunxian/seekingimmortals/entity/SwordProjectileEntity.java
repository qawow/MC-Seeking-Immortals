package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
    private static final String DEFAULT_VISUAL_PROFILE = "technique:flying_sword_strike";
    private static final String TAG_VISUAL_PROFILE = "VisualProfile";
    private static final String TAG_VISUAL_FAMILY = "VisualFamily";
    private static final String TAG_VISUAL_FAMILY_NAME = "VisualFamilyName";
    private static final String TAG_VISUAL_TRAIL = "VisualTrail";
    private static final String TAG_VISUAL_TRAIL_NAME = "VisualTrailName";
    private static final EntityDataAccessor<String> DATA_VISUAL_PROFILE_ID =
            SynchedEntityData.defineId(SwordProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_FAMILY =
            SynchedEntityData.defineId(SwordProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_TRAIL =
            SynchedEntityData.defineId(SwordProjectileEntity.class, EntityDataSerializers.INT);

    private double damage = 8.0D;
    private int life;
    private boolean slowsTarget;
    private boolean terminalVfxSent;

    public SwordProjectileEntity(EntityType<? extends SwordProjectileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public SwordProjectileEntity(Level level, LivingEntity owner, Vec3 direction, double damage, boolean slowsTarget) {
        this(level, owner, direction, damage, slowsTarget, DEFAULT_VISUAL_PROFILE,
                TechniqueVfxPalette.Family.METAL, TechniqueVfxPacket.TrailStyle.SWORD_THIN);
    }

    public SwordProjectileEntity(Level level, LivingEntity owner, Vec3 direction, double damage,
                                 boolean slowsTarget, String visualProfileId,
                                 TechniqueVfxPalette.Family visualFamily,
                                 TechniqueVfxPacket.TrailStyle visualTrailStyle) {
        this(ModEntities.SWORD_PROJECTILE.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.slowsTarget = slowsTarget;
        setVisualIdentity(visualProfileId, visualFamily, visualTrailStyle);
        setPos(owner.getEyePosition().add(direction.normalize().scale(0.7D)));
        setDeltaMovement(direction.normalize().scale(1.25D));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_VISUAL_PROFILE_ID, DEFAULT_VISUAL_PROFILE);
        entityData.define(DATA_VISUAL_FAMILY, TechniqueVfxPalette.Family.METAL.ordinal());
        entityData.define(DATA_VISUAL_TRAIL, TechniqueVfxPacket.TrailStyle.SWORD_THIN.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        // 权威命中/伤害/移除只在服务端执行，客户端仅做插值，避免客户端篡改本地血量或提前丢弃弹射物
        if (!level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit);
                if (isRemoved()) {
                    return;
                }
            }
        }

        Vec3 movement = getDeltaMovement();
        move(MoverType.SELF, movement);
        setPos(getX(), getY(), getZ());
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);

        if (!level().isClientSide && (++life > 80 || !level().isLoaded(blockPosition()))) {
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
                    getVisualFamily(),
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
                getVisualFamily(),
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
        setVisualIdentity(DEFAULT_VISUAL_PROFILE, TechniqueVfxPalette.Family.METAL,
                TechniqueVfxPacket.TrailStyle.SWORD_THIN);
        if (tag.contains(TAG_VISUAL_PROFILE, Tag.TAG_STRING)) {
            setVisualProfileId(tag.getString(TAG_VISUAL_PROFILE));
        }
        if (tag.contains(TAG_VISUAL_FAMILY, Tag.TAG_INT)) {
            setVisualFamily(SyncedVisualIdentity.byOrdinal(
                    TechniqueVfxPalette.Family.values(), tag.getInt(TAG_VISUAL_FAMILY),
                    TechniqueVfxPalette.Family.METAL));
        } else if (tag.contains(TAG_VISUAL_FAMILY, Tag.TAG_STRING)) {
            setVisualFamily(SyncedVisualIdentity.byName(
                    TechniqueVfxPalette.Family.class, tag.getString(TAG_VISUAL_FAMILY),
                    TechniqueVfxPalette.Family.METAL));
        } else if (tag.contains(TAG_VISUAL_FAMILY_NAME, Tag.TAG_STRING)) {
            setVisualFamily(SyncedVisualIdentity.byName(
                    TechniqueVfxPalette.Family.class, tag.getString(TAG_VISUAL_FAMILY_NAME),
                    TechniqueVfxPalette.Family.METAL));
        }
        if (tag.contains(TAG_VISUAL_TRAIL, Tag.TAG_INT)) {
            setVisualTrailStyle(SyncedVisualIdentity.byOrdinal(
                    TechniqueVfxPacket.TrailStyle.values(), tag.getInt(TAG_VISUAL_TRAIL),
                    TechniqueVfxPacket.TrailStyle.SWORD_THIN));
        } else if (tag.contains(TAG_VISUAL_TRAIL, Tag.TAG_STRING)) {
            setVisualTrailStyle(SyncedVisualIdentity.byName(
                    TechniqueVfxPacket.TrailStyle.class, tag.getString(TAG_VISUAL_TRAIL),
                    TechniqueVfxPacket.TrailStyle.SWORD_THIN));
        } else if (tag.contains(TAG_VISUAL_TRAIL_NAME, Tag.TAG_STRING)) {
            setVisualTrailStyle(SyncedVisualIdentity.byName(
                    TechniqueVfxPacket.TrailStyle.class, tag.getString(TAG_VISUAL_TRAIL_NAME),
                    TechniqueVfxPacket.TrailStyle.SWORD_THIN));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("Damage", damage);
        tag.putBoolean("SlowsTarget", slowsTarget);
        tag.putInt("Life", life);
        tag.putString(TAG_VISUAL_PROFILE, getVisualProfileId());
        tag.putInt(TAG_VISUAL_FAMILY, getVisualFamily().ordinal());
        tag.putString(TAG_VISUAL_FAMILY_NAME, getVisualFamily().name());
        tag.putInt(TAG_VISUAL_TRAIL, getVisualTrailStyle().ordinal());
        tag.putString(TAG_VISUAL_TRAIL_NAME, getVisualTrailStyle().name());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void setVisualIdentity(String visualProfileId,
                                  TechniqueVfxPalette.Family family,
                                  TechniqueVfxPacket.TrailStyle trailStyle) {
        setVisualProfileId(visualProfileId);
        setVisualFamily(family);
        setVisualTrailStyle(trailStyle);
    }

    public String getVisualProfileId() {
        return SyncedVisualIdentity.qualified(
                "technique", entityData.get(DATA_VISUAL_PROFILE_ID), DEFAULT_VISUAL_PROFILE);
    }

    public String visualProfileId() {
        return getVisualProfileId();
    }

    public String rawVisualProfileId() {
        return SyncedVisualIdentity.rawId(getVisualProfileId());
    }

    public void setVisualProfileId(String visualProfileId) {
        entityData.set(DATA_VISUAL_PROFILE_ID,
                SyncedVisualIdentity.qualified("technique", visualProfileId, DEFAULT_VISUAL_PROFILE));
    }

    public TechniqueVfxPalette.Family getVisualFamily() {
        return SyncedVisualIdentity.byOrdinal(
                TechniqueVfxPalette.Family.values(), entityData.get(DATA_VISUAL_FAMILY),
                TechniqueVfxPalette.Family.METAL);
    }

    public TechniqueVfxPalette.Family visualFamily() {
        return getVisualFamily();
    }

    public void setVisualFamily(TechniqueVfxPalette.Family family) {
        TechniqueVfxPalette.Family safeFamily = family == null
                ? TechniqueVfxPalette.Family.METAL : family;
        entityData.set(DATA_VISUAL_FAMILY, safeFamily.ordinal());
    }

    public TechniqueVfxPacket.TrailStyle getVisualTrailStyle() {
        return SyncedVisualIdentity.byOrdinal(
                TechniqueVfxPacket.TrailStyle.values(), entityData.get(DATA_VISUAL_TRAIL),
                TechniqueVfxPacket.TrailStyle.SWORD_THIN);
    }

    public TechniqueVfxPacket.TrailStyle visualTrailStyle() {
        return getVisualTrailStyle();
    }

    public void setVisualTrailStyle(TechniqueVfxPacket.TrailStyle trailStyle) {
        TechniqueVfxPacket.TrailStyle safeTrail = trailStyle == null
                ? TechniqueVfxPacket.TrailStyle.SWORD_THIN : trailStyle;
        entityData.set(DATA_VISUAL_TRAIL, safeTrail.ordinal());
    }
}
