package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

public class CultivationFireballEntity extends Projectile {
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(CultivationFireballEntity.class, EntityDataSerializers.INT);
    private static final String DEFAULT_VISUAL_PROFILE = "technique:fireball";
    private static final String TAG_VISUAL_PROFILE = "VisualProfile";
    private static final String TAG_VISUAL_FAMILY = "VisualFamily";
    private static final String TAG_VISUAL_FAMILY_NAME = "VisualFamilyName";
    private static final String TAG_VISUAL_TRAIL = "VisualTrail";
    private static final String TAG_VISUAL_TRAIL_NAME = "VisualTrailName";
    private static final EntityDataAccessor<String> DATA_VISUAL_PROFILE_ID =
            SynchedEntityData.defineId(CultivationFireballEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_FAMILY =
            SynchedEntityData.defineId(CultivationFireballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_TRAIL =
            SynchedEntityData.defineId(CultivationFireballEntity.class, EntityDataSerializers.INT);
    private static final double SPEED = 1.15D;
    private static final int MAX_LIFE = 80;

    private double damage = 6.0D;
    private int life;
    private boolean terminalVfxSent;

    public CultivationFireballEntity(EntityType<? extends CultivationFireballEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public CultivationFireballEntity(Level level, LivingEntity owner, Vec3 direction, double damage) {
        this(level, owner, direction, damage, SPEED, SpellElement.FIRE);
    }

    public CultivationFireballEntity(Level level, LivingEntity owner, Vec3 direction, double damage, double speed) {
        this(level, owner, direction, damage, speed, SpellElement.FIRE);
    }

    public CultivationFireballEntity(Level level, LivingEntity owner, Vec3 direction, double damage, SpellElement element) {
        this(level, owner, direction, damage, SPEED, element);
    }

    public CultivationFireballEntity(Level level, LivingEntity owner, Vec3 direction, double damage, double speed, SpellElement element) {
        this(level, owner, direction, damage, speed, element,
                defaultVisualProfile(element), defaultVisualFamily(element),
                TechniqueVfxPacket.TrailStyle.SWORD_THIN);
    }

    public CultivationFireballEntity(Level level, LivingEntity owner, Vec3 direction, double damage,
                                     double speed, SpellElement element, String visualProfileId,
                                     TechniqueVfxPalette.Family visualFamily,
                                     TechniqueVfxPacket.TrailStyle visualTrailStyle) {
        this(ModEntities.CULTIVATION_FIREBALL.get(), level);
        Vec3 normalized = direction.normalize();
        setOwner(owner);
        this.damage = damage;
        setElement(element);
        setVisualIdentity(visualProfileId, visualFamily, visualTrailStyle);
        setPos(owner.getEyePosition().add(normalized.scale(0.8D)));
        setDeltaMovement(normalized.scale(speed));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_ELEMENT, SpellElement.FIRE.id);
        entityData.define(DATA_VISUAL_PROFILE_ID, DEFAULT_VISUAL_PROFILE);
        entityData.define(DATA_VISUAL_FAMILY, TechniqueVfxPalette.Family.FIRE.ordinal());
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
            }
            if (isRemoved()) {
                return;
            }
        }

        Vec3 movement = getDeltaMovement();
        move(MoverType.SELF, movement);
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);

        if (!level().isClientSide && (++life > MAX_LIFE || !level().isLoaded(blockPosition()))) {
            sendDissipate();
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (!canDamageTarget(target)) {
            return;
        }

        boolean damaged = target.hurt(level().damageSources().indirectMagic(this, owner), (float) damage);
        if (damaged && target instanceof LivingEntity living) {
            getElement().applyDirectEffect(living);
        }
        applySplash(result.getLocation(), target);
        finishImpact(result.getLocation());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) {
            applySplash(result.getLocation(), null);
            finishImpact(result.getLocation());
        }
    }

    private void applySplash(Vec3 center, Entity directTarget) {
        Entity owner = getOwner();
        SpellElement element = getElement();
        AABB area = new AABB(center, center).inflate(element.splashRadius);
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != directTarget && canDamageTarget(entity));
        for (LivingEntity living : targets) {
            double distance = Math.sqrt(living.distanceToSqr(center));
            double falloff = Math.max(0.25D, 1.0D - distance / Math.max(0.1D, element.splashRadius));
            float splashDamage = (float) (damage * 0.35D * falloff);
            if (splashDamage > 0.0F) {
                if (living.hurt(level().damageSources().indirectMagic(this, owner), splashDamage)) {
                    element.applySplashEffect(living);
                }
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && canDamageTarget(target);
    }

    private boolean canDamageTarget(Entity target) {
        Entity owner = getOwner();
        if (target == null || target == owner || target.isSpectator()) {
            return false;
        }
        if (owner instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(target)) {
            return false;
        }
        if (owner instanceof Player caster && target instanceof SummonedServitorEntity servitor
                && servitor.getOwnerUUID().filter(caster.getUUID()::equals).isPresent()) {
            return false;
        }
        if (owner instanceof Player caster && target instanceof CultivationBeastEntity beast
                && beast.isCompanion()
                && beast.getOwnerUUID().filter(caster.getUUID()::equals).isPresent()) {
            return false;
        }
        return !(owner instanceof Player caster && target instanceof Player playerTarget)
                || caster.canHarmPlayer(playerTarget);
    }

    private void finishImpact(Vec3 position) {
        if (level() instanceof ServerLevel serverLevel) {
            SpellElement element = getElement();
            serverLevel.sendParticles(element.dust(1.25F),
                    position.x, position.y, position.z,
                    element.impactParticles, 0.45D, 0.35D, 0.45D, 0.03D);
            serverLevel.sendParticles(element.spark(0.75F),
                    position.x, position.y, position.z,
                    Math.max(6, element.impactParticles / 2), 0.28D, 0.22D, 0.28D, 0.02D);
            terminalVfxSent = true;
            TechniqueLifecycleVfxService.projectileImpact(
                    serverLevel,
                    getVisualFamily(),
                    TechniqueVfxPacket.Motif.PROJECTILE,
                    position,
                    Math.max(0.8D, element.splashRadius),
                    Math.min(72, element.impactParticles + 18),
                    getId() * 131L ^ element.id);
            serverLevel.playSound(null, blockPosition(), element.impactSound, SoundSource.PLAYERS, 0.55F, element.impactPitch);
        }
        discard();
    }

    private void sendDissipate() {
        if (terminalVfxSent || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        terminalVfxSent = true;
        SpellElement element = getElement();
        TechniqueLifecycleVfxService.projectileDissipate(
                serverLevel,
                getVisualFamily(),
                TechniqueVfxPacket.Motif.PROJECTILE,
                position(),
                Math.max(0.7D, element.splashRadius * 0.65D),
                getId() * 149L ^ element.id);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            sendDissipate();
        }
        super.remove(reason);
    }

    public SpellElement getElement() {
        return SpellElement.byId(entityData.get(DATA_ELEMENT));
    }

    public void setElement(SpellElement element) {
        entityData.set(DATA_ELEMENT, element == null ? SpellElement.FIRE.id : element.id);
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
                "technique", entityData.get(DATA_VISUAL_PROFILE_ID), defaultVisualProfile(getElement()));
    }

    public String visualProfileId() {
        return getVisualProfileId();
    }

    public String rawVisualProfileId() {
        return SyncedVisualIdentity.rawId(getVisualProfileId());
    }

    public void setVisualProfileId(String visualProfileId) {
        entityData.set(DATA_VISUAL_PROFILE_ID,
                SyncedVisualIdentity.qualified("technique", visualProfileId,
                        defaultVisualProfile(getElement())));
    }

    public TechniqueVfxPalette.Family getVisualFamily() {
        return SyncedVisualIdentity.byOrdinal(
                TechniqueVfxPalette.Family.values(), entityData.get(DATA_VISUAL_FAMILY),
                defaultVisualFamily(getElement()));
    }

    public TechniqueVfxPalette.Family visualFamily() {
        return getVisualFamily();
    }

    public void setVisualFamily(TechniqueVfxPalette.Family family) {
        TechniqueVfxPalette.Family safeFamily = family == null
                ? defaultVisualFamily(getElement()) : family;
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

    public static String defaultVisualProfile(SpellElement element) {
        return element == SpellElement.ICE_SPEAR ? "technique:ice_spear" : DEFAULT_VISUAL_PROFILE;
    }

    public static TechniqueVfxPalette.Family defaultVisualFamily(SpellElement element) {
        SpellElement safeElement = element == null ? SpellElement.FIRE : element;
        return TechniqueVfxPalette.familyOf(safeElement.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            damage = tag.getDouble("Damage");
        }
        if (tag.contains("Element")) {
            setElement(SpellElement.byId(tag.getInt("Element")));
        }
        setVisualIdentity(defaultVisualProfile(getElement()), defaultVisualFamily(getElement()),
                TechniqueVfxPacket.TrailStyle.SWORD_THIN);
        if (tag.contains("Life", Tag.TAG_INT)) {
            life = Math.max(0, tag.getInt("Life"));
        }
        if (tag.contains(TAG_VISUAL_PROFILE, Tag.TAG_STRING)) {
            setVisualProfileId(tag.getString(TAG_VISUAL_PROFILE));
        }
        if (tag.contains(TAG_VISUAL_FAMILY, Tag.TAG_INT)) {
            setVisualFamily(SyncedVisualIdentity.byOrdinal(
                    TechniqueVfxPalette.Family.values(), tag.getInt(TAG_VISUAL_FAMILY),
                    defaultVisualFamily(getElement())));
        } else if (tag.contains(TAG_VISUAL_FAMILY, Tag.TAG_STRING)) {
            setVisualFamily(SyncedVisualIdentity.byName(
                    TechniqueVfxPalette.Family.class, tag.getString(TAG_VISUAL_FAMILY),
                    defaultVisualFamily(getElement())));
        } else if (tag.contains(TAG_VISUAL_FAMILY_NAME, Tag.TAG_STRING)) {
            setVisualFamily(SyncedVisualIdentity.byName(
                    TechniqueVfxPalette.Family.class, tag.getString(TAG_VISUAL_FAMILY_NAME),
                    defaultVisualFamily(getElement())));
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
        tag.putInt("Element", getElement().id);
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

    public enum SpellElement {
        FIRE(0, 1.00F, 0.28F, 0.06F, 1.00F, 0.80F, 0.25F, 1.75D, 3, null, 0, 0, 28, SoundEvents.GENERIC_EXPLODE, 1.35F, 1.00F),
        WATER(1, 0.16F, 0.56F, 1.00F, 0.76F, 0.94F, 1.00F, 0.95D, 0, MobEffects.MOVEMENT_SLOWDOWN, 55, 0, 24, SoundEvents.TRIDENT_HIT, 1.45F, 0.92F),
        METAL(2, 0.78F, 0.86F, 0.98F, 1.00F, 1.00F, 1.00F, 0.45D, 0, MobEffects.WEAKNESS, 65, 0, 18, SoundEvents.ARROW_HIT_PLAYER, 1.75F, 0.72F),
        ICE(3, 0.52F, 0.90F, 1.00F, 0.92F, 1.00F, 1.00F, 1.05D, 0, MobEffects.MOVEMENT_SLOWDOWN, 85, 1, 26, SoundEvents.GLASS_BREAK, 1.65F, 0.88F),
        WIND(4, 0.74F, 0.95F, 0.88F, 0.94F, 1.00F, 0.96F, 0.70D, 0, MobEffects.MOVEMENT_SLOWDOWN, 35, 0, 22, SoundEvents.TRIDENT_THROW, 1.85F, 0.78F),
        WOOD(5, 0.24F, 0.74F, 0.28F, 0.66F, 1.00F, 0.52F, 0.90D, 0, MobEffects.MOVEMENT_SLOWDOWN, 75, 1, 24, SoundEvents.GRASS_BREAK, 1.35F, 0.86F),
        DARK(6, 0.12F, 0.02F, 0.20F, 0.78F, 0.12F, 1.00F, 1.20D, 2, MobEffects.WITHER, 65, 0, 30, SoundEvents.SOUL_ESCAPE, 1.22F, 0.96F),
        LIGHT(7, 1.00F, 0.86F, 0.30F, 1.00F, 1.00F, 0.92F, 1.05D, 0, MobEffects.GLOWING, 100, 0, 28, SoundEvents.AMETHYST_BLOCK_CHIME, 1.55F, 0.94F),
        EARTH(8, 0.62F, 0.45F, 0.22F, 0.94F, 0.78F, 0.42F, 1.35D, 0, MobEffects.MOVEMENT_SLOWDOWN, 80, 1, 30, SoundEvents.STONE_BREAK, 0.82F, 1.02F),
        THUNDER(9, 0.52F, 0.78F, 1.00F, 0.94F, 1.00F, 1.00F, 1.10D, 0, MobEffects.MOVEMENT_SLOWDOWN, 70, 1, 32, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.80F, 0.98F),
        ICE_SPEAR(10, 0.44F, 0.82F, 1.00F, 0.90F, 1.00F, 1.00F, 0.85D, 0, MobEffects.MOVEMENT_SLOWDOWN, 110, 2, 30, SoundEvents.GLASS_BREAK, 1.85F, 0.72F),
        FLAME_BURST(11, 1.00F, 0.24F, 0.04F, 1.00F, 0.78F, 0.18F, 2.15D, 4, MobEffects.WEAKNESS, 55, 0, 42, SoundEvents.GENERIC_EXPLODE, 1.18F, 1.10F),
        FIRE_SERPENT(12, 0.96F, 0.16F, 0.04F, 1.00F, 0.92F, 0.28F, 1.55D, 5, MobEffects.WEAKNESS, 60, 0, 36, SoundEvents.BLAZE_SHOOT, 1.28F, 0.92F);

        private final int id;
        public final float outerRed;
        public final float outerGreen;
        public final float outerBlue;
        public final float coreRed;
        public final float coreGreen;
        public final float coreBlue;
        public final double splashRadius;
        private final int burnSeconds;
        private final MobEffect directEffect;
        private final int effectDurationTicks;
        private final int effectAmplifier;
        private final int impactParticles;
        private final net.minecraft.sounds.SoundEvent impactSound;
        private final float impactPitch;
        public final float visualScale;

        SpellElement(int id, float outerRed, float outerGreen, float outerBlue,
                     float coreRed, float coreGreen, float coreBlue,
                     double splashRadius, int burnSeconds,
                     MobEffect directEffect, int effectDurationTicks, int effectAmplifier,
                     int impactParticles, net.minecraft.sounds.SoundEvent impactSound,
                     float impactPitch, float visualScale) {
            this.id = id;
            this.outerRed = outerRed;
            this.outerGreen = outerGreen;
            this.outerBlue = outerBlue;
            this.coreRed = coreRed;
            this.coreGreen = coreGreen;
            this.coreBlue = coreBlue;
            this.splashRadius = splashRadius;
            this.burnSeconds = burnSeconds;
            this.directEffect = directEffect;
            this.effectDurationTicks = effectDurationTicks;
            this.effectAmplifier = effectAmplifier;
            this.impactParticles = impactParticles;
            this.impactSound = impactSound;
            this.impactPitch = impactPitch;
            this.visualScale = visualScale;
        }

        private static SpellElement byId(int id) {
            for (SpellElement element : values()) {
                if (element.id == id) {
                    return element;
                }
            }
            return FIRE;
        }

        private DustParticleOptions dust(float scale) {
            return new DustParticleOptions(new Vector3f(outerRed, outerGreen, outerBlue), scale);
        }

        private DustParticleOptions spark(float scale) {
            return new DustParticleOptions(new Vector3f(coreRed, coreGreen, coreBlue), scale);
        }

        private void applyDirectEffect(LivingEntity target) {
            if (burnSeconds > 0) {
                target.setSecondsOnFire(burnSeconds);
            }
            if (directEffect != null && effectDurationTicks > 0) {
                target.addEffect(new MobEffectInstance(directEffect, effectDurationTicks, effectAmplifier, false, true));
            }
        }

        private void applySplashEffect(LivingEntity target) {
            if (burnSeconds > 1) {
                target.setSecondsOnFire(burnSeconds - 1);
            } else if (directEffect != null && effectDurationTicks > 0) {
                target.addEffect(new MobEffectInstance(directEffect, Math.max(20, effectDurationTicks / 2), effectAmplifier, false, true));
            }
        }
    }
}
