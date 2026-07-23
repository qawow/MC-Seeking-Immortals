package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastBossService;
import com.xunxian.seekingimmortals.beast.BeastElementService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.BeastSpawnTableService;
import com.xunxian.seekingimmortals.worldpack.BossEncounterService;
import com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService;
import com.xunxian.seekingimmortals.worldpack.ServitorRegistrySavedData;
import com.xunxian.seekingimmortals.worldpack.TrialCombatShellService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Data-driven GeckoLib beast entity. Every bestiary id can be materialized by
 * this type while retaining authoritative tier, element, loot, and boss tags.
 */
public class CultivationBeastEntity extends Monster implements GeoEntity {
    public static final String TAG_ECOLOGY = "seeking_immortals_ecology_beast";
    public static final String TAG_BEAST_ID = "seeking_immortals_beast_id";
    public static final String TAG_BEAST_TIER = "seeking_immortals_beast_tier";
    /** Affinities with authored palettes and runtime status semantics. */
    public static final Set<String> SUPPORTED_ELEMENTS = BeastElementService.SUPPORTED_ELEMENTS;

    private static final String TAG_ELEMENT = "BeastElement";
    private static final String TAG_BODY_PLAN = "BeastBodyPlan";
    private static final String TAG_BOSS = "CatalogBoss";
    private static final String TAG_COMPANION = "ContractCompanion";
    private static final String TAG_OWNER = "CompanionOwner";
    private static final String TAG_LIFE = "CompanionLife";
    private static final String TAG_MAX_LIFE = "CompanionMaxLife";
    private static final String TAG_STANCE = "CompanionStance";
    private static final String TAG_GUARD_X = "CompanionGuardX";
    private static final String TAG_GUARD_Y = "CompanionGuardY";
    private static final String TAG_GUARD_Z = "CompanionGuardZ";
    private static final String TAG_CONFIGURED_HEALTH = "CompanionConfiguredHealth";
    private static final String TAG_CONFIGURED_DAMAGE = "CompanionConfiguredDamage";
    private static final String TAG_TERMINAL_GROWTH_CREDITED = "CompanionTerminalGrowthCredited";
    private static final String TAG_COMPANION_DAMAGE_OWNER = "SeekingImmortalsCompanionDamageOwner";
    private static final String TAG_COMPANION_DAMAGE_EXPIRY = "SeekingImmortalsCompanionDamageExpiry";

    private static final EntityDataAccessor<String> DATA_BEAST_ID = SynchedEntityData.defineId(
            CultivationBeastEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TIER = SynchedEntityData.defineId(
            CultivationBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_ELEMENT = SynchedEntityData.defineId(
            CultivationBeastEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_BODY_PLAN = SynchedEntityData.defineId(
            CultivationBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOSS = SynchedEntityData.defineId(
            CultivationBeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_COMPANION = SynchedEntityData.defineId(
            CultivationBeastEntity.class, EntityDataSerializers.BOOLEAN);

    public enum BodyPlan {
        QUADRUPED,
        SERPENT,
        INSECT,
        AVIAN,
        AQUATIC,
        HUMANOID
    }

    public record BeastSpawnGroupData(String beastId, int tier) implements SpawnGroupData {}

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID ownerUUID;
    private int lifeTicks;
    private int maxLifeTicks;
    private SummonedServitorEntity.Stance stance = SummonedServitorEntity.Stance.FOLLOW;
    private double guardX;
    private double guardY;
    private double guardZ;
    private boolean terminalGrowthCredited;

    public CultivationBeastEntity(EntityType<? extends CultivationBeastEntity> type, Level level) {
        super(type, level);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.25D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_BEAST_ID, "wild_beast");
        entityData.define(DATA_TIER, 1);
        entityData.define(DATA_ELEMENT, "neutral");
        entityData.define(DATA_BODY_PLAN, BodyPlan.QUADRUPED.ordinal());
        entityData.define(DATA_BOSS, false);
        entityData.define(DATA_COMPANION, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TryFindWaterGoal(this) {
            @Override
            public boolean canUse() {
                return getBodyPlan() == BodyPlan.AQUATIC && !isInWaterOrBubble() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35F) {
            @Override
            public boolean canUse() {
                return getBodyPlan() != BodyPlan.AQUATIC && super.canUse();
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true));
        goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1.0D, 24) {
            @Override
            public boolean canUse() {
                return getBodyPlan() == BodyPlan.AQUATIC && isInWaterOrBubble() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.05D) {
            @Override
            public boolean canUse() {
                return getBodyPlan() == BodyPlan.AVIAN && super.canUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9D) {
            @Override
            public boolean canUse() {
                return getBodyPlan() != BodyPlan.AQUATIC && getBodyPlan() != BodyPlan.AVIAN && super.canUse();
            }
        });
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.85D) {
            @Override
            public boolean canUse() {
                return getBodyPlan() == BodyPlan.AQUATIC && !isInWaterOrBubble() && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (isCompanion()) {
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
        } else {
            targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(CultivationBeastEntity.class));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    private void applyBodyPlanMobility(BodyPlan bodyPlan) {
        if (bodyPlan == BodyPlan.AVIAN) {
            if (!(moveControl instanceof FlyingMoveControl)) {
                moveControl = new FlyingMoveControl(this, 20, true);
            }
            if (!(navigation instanceof FlyingPathNavigation)) {
                navigation.stop();
                navigation = new FlyingPathNavigation(this, level());
            }
            setNoGravity(true);
            return;
        }
        if (moveControl instanceof FlyingMoveControl) {
            moveControl = new MoveControl(this);
        }
        if (navigation instanceof FlyingPathNavigation) {
            navigation.stop();
            navigation = new AmphibiousPathNavigation(this, level());
        }
        setNoGravity(false);
    }

    public static EntityDimensions dimensionsFor(BodyPlan bodyPlan) {
        return switch (bodyPlan == null ? BodyPlan.QUADRUPED : bodyPlan) {
            case QUADRUPED -> EntityDimensions.scalable(1.00F, 1.25F);
            case SERPENT -> EntityDimensions.scalable(0.80F, 0.60F);
            case INSECT -> EntityDimensions.scalable(0.72F, 0.55F);
            case AVIAN -> EntityDimensions.scalable(0.95F, 0.80F);
            case AQUATIC -> EntityDimensions.scalable(1.15F, 0.65F);
            case HUMANOID -> EntityDimensions.scalable(0.70F, 1.85F);
        };
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return dimensionsFor(getBodyPlan());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_BODY_PLAN.equals(key)) {
            refreshDimensions();
            applyBodyPlanMobility(getBodyPlan());
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return getBodyPlan() == BodyPlan.AQUATIC || super.canBreatheUnderwater();
    }

    @Override
    protected float getWaterSlowDown() {
        return getBodyPlan() == BodyPlan.AQUATIC ? 0.96F : super.getWaterSlowDown();
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return getBodyPlan() != BodyPlan.AVIAN
                && super.causeFallDamage(distance, damageMultiplier, source);
    }

    public void configureWild(String beastId, int tier) {
        configure(beastId, tier, false, 1.0D, 1.0D);
        getPersistentData().putBoolean(TAG_ECOLOGY, true);
    }

    public void configureBoss(String beastId, int tier, double health, double damage) {
        BeastTierService.ScaledStats base = BeastTierService.scaleStats(tier);
        configure(beastId, tier, true,
                Math.max(1.0D, health / Math.max(1.0D, base.health())),
                Math.max(1.0D, damage / Math.max(1.0D, base.damage())));
        getPersistentData().putBoolean(TAG_ECOLOGY, false);
    }

    public void configureCompanion(ServerPlayer owner, String beastId, int tier, int lifetimeTicks,
                                   double health, double damage) {
        if (owner == null) {
            throw new IllegalArgumentException("owner");
        }
        configure(beastId, tier, false, 1.0D, 1.0D);
        entityData.set(DATA_COMPANION, true);
        ownerUUID = owner.getUUID();
        lifeTicks = Math.max(20 * 8, lifetimeTicks);
        maxLifeTicks = lifeTicks;
        stance = SummonedServitorEntity.Stance.FOLLOW;
        guardX = owner.getX();
        guardY = owner.getY();
        guardZ = owner.getZ();
        getPersistentData().putBoolean(TAG_ECOLOGY, false);
        setBase(Attributes.MAX_HEALTH, Mth.clamp(health, 8.0D, 1024.0D));
        setBase(Attributes.ATTACK_DAMAGE, Mth.clamp(damage, 2.0D, 2048.0D));
        setHealth(getMaxHealth());
        setCustomName(displayNameForCurrentBeast());
        setCustomNameVisible(true);
        rebuildGoals();
    }

    private void rebuildGoals() {
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        registerGoals();
    }

    private void configure(String beastId, int tier, boolean boss, double healthMultiplier, double damageMultiplier) {
        entityData.set(DATA_COMPANION, false);
        ownerUUID = null;
        lifeTicks = 0;
        maxLifeTicks = 0;
        stance = SummonedServitorEntity.Stance.FOLLOW;
        String id = normalizeId(beastId);
        BeastBestiaryService.BeastEntry entry = BeastBestiaryService.find(id).orElse(null);
        if (entry != null) {
            id = entry.id();
        }
        int safeTier = BeastTierService.clampTier(tier > 0 ? tier : entry == null ? 1 : entry.tier());
        String element = normalizeElement(entry == null ? "" : entry.element(), id);
        BodyPlan bodyPlan = bodyPlanFor(id, entry);

        entityData.set(DATA_BEAST_ID, id.isBlank() ? "wild_beast" : id);
        entityData.set(DATA_TIER, safeTier);
        entityData.set(DATA_ELEMENT, element);
        entityData.set(DATA_BODY_PLAN, bodyPlan.ordinal());
        entityData.set(DATA_BOSS, boss);
        applyBodyPlanMobility(bodyPlan);
        refreshDimensions();
        applyCombatStats(safeTier, bodyPlan, healthMultiplier, damageMultiplier);

        if (boss) {
            setCustomName(displayNameForCurrentBeast());
            setCustomNameVisible(true);
        } else {
            // A custom name makes Mob persistent. Wild ecology entities must remain despawnable.
            setCustomName(null);
            setCustomNameVisible(false);
        }
        xpReward = 3 + safeTier * (boss ? 10 : 4);

        CompoundTag persistent = getPersistentData();
        persistent.putString(TAG_BEAST_ID, getBeastId());
        persistent.putInt(TAG_BEAST_TIER, safeTier);
    }

    private Component displayNameForCurrentBeast() {
        BeastBestiaryService.BeastEntry entry = BeastBestiaryService.find(getBeastId()).orElse(null);
        Component display = entry != null && PlayerDisplayText.isSafe(entry.display())
                ? Component.literal(entry.display().trim())
                : Component.translatable("entity.seeking_immortals.cultivation_beast");
        return Component.translatable("entity.seeking_immortals.cultivation_beast.named", display, getBeastTier());
    }

    @Override
    public Component getName() {
        Component custom = getCustomName();
        return custom != null ? custom : displayNameForCurrentBeast();
    }

    private void applyCombatStats(int tier, BodyPlan bodyPlan, double healthMultiplier, double damageMultiplier) {
        BeastTierService.ScaledStats stats = BeastTierService.scaleStats(tier);
        double speed = switch (bodyPlan) {
            case SERPENT -> 0.31D;
            case INSECT -> 0.34D;
            case AVIAN -> 0.36D;
            case AQUATIC -> 0.29D;
            case HUMANOID -> 0.28D;
            case QUADRUPED -> 0.33D;
        };
        setBase(Attributes.MAX_HEALTH, Math.min(1024.0D, stats.health() * healthMultiplier));
        setBase(Attributes.ATTACK_DAMAGE, Math.min(2048.0D, stats.damage() * damageMultiplier));
        setBase(Attributes.ARMOR, Math.min(30.0D, stats.armor() + (isCatalogBoss() ? 3.0D : 0.0D)));
        setBase(Attributes.MOVEMENT_SPEED, speed);
        setBase(Attributes.FOLLOW_RANGE, 24.0D + tier * 1.5D);
        setBase(Attributes.KNOCKBACK_RESISTANCE, Math.min(0.85D, 0.04D * tier));
        setBase(Attributes.ATTACK_KNOCKBACK, Math.min(2.0D, 0.18D + tier * 0.055D));
        setHealth(getMaxHealth());
    }

    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        if (getAttribute(attribute) != null) {
            getAttribute(attribute).setBaseValue(value);
        }
    }

    public String getBeastId() {
        return entityData.get(DATA_BEAST_ID);
    }

    public int getBeastTier() {
        return BeastTierService.clampTier(entityData.get(DATA_TIER));
    }

    public String getElement() {
        return entityData.get(DATA_ELEMENT);
    }

    public BodyPlan getBodyPlan() {
        int ordinal = entityData.get(DATA_BODY_PLAN);
        BodyPlan[] values = BodyPlan.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : BodyPlan.QUADRUPED;
    }

    public boolean isCatalogBoss() {
        return entityData.get(DATA_BOSS);
    }

    public boolean isCompanion() {
        return entityData.get(DATA_COMPANION);
    }

    public Optional<UUID> getOwnerUUID() {
        return Optional.ofNullable(ownerUUID);
    }

    public SummonedServitorEntity.Stance getStance() {
        return stance;
    }

    public void setStance(SummonedServitorEntity.Stance next) {
        stance = next == null ? SummonedServitorEntity.Stance.FOLLOW : next;
        if (stance == SummonedServitorEntity.Stance.GUARD) {
            guardX = getX();
            guardY = getY();
            guardZ = getZ();
        } else if (stance == SummonedServitorEntity.Stance.STAY) {
            getNavigation().stop();
            setTarget(null);
        }
        if (!level().isClientSide && isCompanion() && ownerUUID != null
                && level() instanceof ServerLevel serverLevel) {
            ServitorRegistrySavedData.get(serverLevel).setStance(getUUID(), stance.name());
        }
    }

    public SummonedServitorEntity.Stance cycleStance() {
        SummonedServitorEntity.Stance next = switch (stance) {
            case FOLLOW -> SummonedServitorEntity.Stance.GUARD;
            case GUARD -> SummonedServitorEntity.Stance.AGGRESSIVE;
            case AGGRESSIVE -> SummonedServitorEntity.Stance.STAY;
            case STAY -> SummonedServitorEntity.Stance.FOLLOW;
        };
        setStance(next);
        return stance;
    }

    public int getLifeTicksRemaining() {
        return lifeTicks;
    }

    public int getMaxLifeTicks() {
        return Math.max(lifeTicks, maxLifeTicks);
    }

    public boolean trySetCommandTarget(LivingEntity target) {
        if (!isCompanion() || target == null || !target.isAlive() || isFriendlyEntity(target)) {
            return false;
        }
        setTarget(target);
        return true;
    }

    public void recordDismissCredit() {
        if (terminalGrowthCredited || !isCompanion() || ownerUUID == null
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Player owner = serverLevel.getPlayerByUUID(ownerUUID);
        if (!(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        terminalGrowthCredited = true;
        float fraction = maxLifeTicks <= 0 ? 0.0F : (float) lifeTicks / (float) maxLifeTicks;
        BeastContractService.recordCombatCredit(serverPlayer, getBeastId(),
                fraction >= 0.5F ? BeastContractService.CreditKind.SURVIVE : BeastContractService.CreditKind.HIT);
    }

    public static BodyPlan bodyPlanFor(String beastId, @Nullable BeastBestiaryService.BeastEntry entry) {
        String source = (normalizeId(beastId) + " "
                + (entry == null ? "" : normalizeId(entry.category())) + " "
                + (entry == null ? "" : normalizeId(entry.entityIdHint())) + " "
                + (entry == null ? "" : normalizeId(entry.habitat()))).toLowerCase(Locale.ROOT);
        if (containsAny(source, "chong", "insect", "moth", "mantis", "spider", "scorpion", "beetle", "xie", "zhi_zhu", "yi_")) {
            return BodyPlan.INSECT;
        }
        if (containsAny(source, "serpent", "snake", "she_", "_she", "jiao", "mang", "dragon", "eel", "long_", "_long")) {
            return BodyPlan.SERPENT;
        }
        if (containsAny(source, "bird", "avian", "crow", "eagle", "hawk", "crane", "peng", "niao", "que", "jiu", "feng_")) {
            return BodyPlan.AVIAN;
        }
        if (containsAny(source, "aquatic", "ocean", "sea_", "hai_", "fish", "whale", "shark", "yu_", "_yu", "shui_mu", "octopus")) {
            return BodyPlan.AQUATIC;
        }
        if (containsAny(source, "humanoid", "puppet", "ghost", "corpse", "ren_", "_ren", "yecha", "moying", "canpo")) {
            return BodyPlan.HUMANOID;
        }
        return BodyPlan.QUADRUPED;
    }

    private static boolean containsAny(String source, String... tokens) {
        for (String token : tokens) {
            if (source.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeElement(String rawElement, String beastId) {
        return BeastElementService.normalize(rawElement, beastId);
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isCompanion() && isFriendlyEntity(target)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (!hit || !(target instanceof LivingEntity living)) {
            return hit;
        }
        int tier = getBeastTier();
        if (living.isAlive() && random.nextDouble() <= Math.min(0.48D, 0.10D + tier * 0.028D)) {
            applyElementalEffect(living, false);
        }
        if (isCompanion()) {
            recordCombatCredit(living.isAlive()
                    ? BeastContractService.CreditKind.HIT
                    : BeastContractService.CreditKind.KILL);
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (isCompanion()) {
            lifeTicks--;
            if (lifeTicks <= 0) {
                discard();
                return;
            }
            if (level() instanceof ServerLevel serverLevel && applyRegistryState(serverLevel)) {
                return;
            }
            if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(ownerUUID);
                if (owner instanceof ServerPlayer serverPlayer) {
                    applyStanceBehavior(serverPlayer);
                }
            }
        }
        int tier = getBeastTier();
        if (tier >= 7 && tickCount % Math.max(50, 130 - tier * 5) == 0 && getHealth() < getMaxHealth()) {
            heal(0.8F + tier * 0.12F);
        }
        if (tickCount % Math.max(45, 135 - tier * 6) == 0 && getTarget() != null
                && getTarget().isAlive() && distanceToSqr(getTarget()) >= 16.0D && distanceToSqr(getTarget()) <= 196.0D) {
            applyElementalEffect(getTarget(), true);
        }
        if (isCatalogBoss() || BossEncounterService.isBossMob(this)) {
            BeastBossService.tickBossSkills(this);
        }
    }

    private boolean applyRegistryState(ServerLevel serverLevel) {
        ServitorRegistrySavedData.State state = ServitorRegistrySavedData.get(serverLevel).register(
                ownerUUID, getUUID(), serverLevel.dimension().location().toString(), stance.name(),
                SummonHonestMvpService.MAX_ACTIVE_SERVITORS);
        if (state == null) {
            return false;
        }
        if (state.dismissed()) {
            recordDismissCredit();
            discard();
            return true;
        }
        try {
            SummonedServitorEntity.Stance desired = SummonedServitorEntity.Stance.valueOf(state.stance());
            if (desired != stance) {
                setStance(desired);
            }
        } catch (IllegalArgumentException ignored) {
            setStance(SummonedServitorEntity.Stance.FOLLOW);
        }
        return false;
    }

    private void applyStanceBehavior(ServerPlayer owner) {
        if (tickCount % 10 == 0) {
            level().getEntitiesOfClass(SummonedServitorEntity.class, getBoundingBox().inflate(24.0D),
                            servitor -> servitor.getTarget() == this
                                    && servitor.getOwnerUUID().filter(this::isOwnerAlly).isPresent())
                    .forEach(servitor -> servitor.setTarget(null));
        }
        if (stance != SummonedServitorEntity.Stance.STAY && (getTarget() == null || !getTarget().isAlive())) {
            LivingEntity retaliation = owner.getLastHurtByMob();
            if (retaliation == null || !retaliation.isAlive() || isFriendlyEntity(retaliation)) {
                retaliation = owner.getLastHurtMob();
            }
            if (retaliation != null && retaliation.isAlive() && !isFriendlyEntity(retaliation)) {
                setTarget(retaliation);
            }
        }
        switch (stance) {
            case STAY -> {
                getNavigation().stop();
                setTarget(null);
            }
            case GUARD -> {
                if (getTarget() == null && distanceToSqr(guardX, guardY, guardZ) > 16.0D) {
                    getNavigation().moveTo(guardX, guardY, guardZ, 1.05D);
                }
            }
            case AGGRESSIVE -> {
                if ((getTarget() == null || !getTarget().isAlive()) && tickCount % 10 == 0) {
                    level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(18.0D),
                                    target -> target != this && target.isAlive() && !isFriendlyEntity(target))
                            .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                            .ifPresent(this::setTarget);
                }
                if (getTarget() == null && distanceToSqr(owner) > 24.0D * 24.0D) {
                    getNavigation().moveTo(owner, 1.3D);
                }
            }
            case FOLLOW -> {
                if (getTarget() == null && distanceToSqr(owner) > 14.0D * 14.0D) {
                    getNavigation().moveTo(owner, 1.3D);
                }
            }
        }
    }

    private boolean isFriendlyEntity(@Nullable Entity entity) {
        if (!isCompanion() || ownerUUID == null || entity == null) {
            return false;
        }
        if (isOwnerAlly(entity.getUUID())) {
            return true;
        }
        if (entity instanceof CultivationBeastEntity beast
                && beast.isCompanion() && beast.getOwnerUUID().filter(this::isOwnerAlly).isPresent()) {
            return true;
        }
        if (entity instanceof SummonedServitorEntity servitor
                && servitor.getOwnerUUID().filter(this::isOwnerAlly).isPresent()) {
            return true;
        }
        if (level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerUUID);
            return owner != null && owner.isAlliedTo(entity);
        }
        return false;
    }

    private boolean isOwnerAlly(UUID candidateOwner) {
        if (ownerUUID == null || candidateOwner == null) {
            return false;
        }
        if (ownerUUID.equals(candidateOwner)) {
            return true;
        }
        if (level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerUUID);
            Player candidate = serverLevel.getPlayerByUUID(candidateOwner);
            return owner != null && candidate != null && owner.isAlliedTo(candidate);
        }
        return false;
    }

    private void recordCombatCredit(BeastContractService.CreditKind kind) {
        if (ownerUUID == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Player owner = serverLevel.getPlayerByUUID(ownerUUID);
        if (owner instanceof ServerPlayer serverPlayer) {
            BeastContractService.recordCombatCredit(serverPlayer, getBeastId(), kind);
        }
    }

    private void applyElementalEffect(net.minecraft.world.entity.LivingEntity target, boolean pulse) {
        int tier = getBeastTier();
        String status = switch (getElement()) {
            case "fire" -> "burn";
            case "ice", "water" -> "frozen";
            case "poison", "wood" -> "poison";
            case "thunder" -> "stun";
            case "soul" -> "soul_shock";
            case "blood" -> "bleed";
            case "illusion" -> "illusion";
            case "mixed", "void" -> "qi_disorder";
            case "earth", "metal" -> "array_bind";
            default -> "qi_disorder";
        };
        int duration = 35 + tier * 5;
        boolean applied = StatusRegistry.applyStatus(
                target, this, status, Math.min(3, tier / 4), duration, random);
        if (applied && isCompanion() && ownerUUID != null) {
            target.getPersistentData().putUUID(TAG_COMPANION_DAMAGE_OWNER, ownerUUID);
            target.getPersistentData().putLong(
                    TAG_COMPANION_DAMAGE_EXPIRY, target.level().getGameTime() + duration + 20L);
        }
        if (pulse) {
            target.hurt(damageSources().indirectMagic(this, this), (float) (1.0D + tier * 0.32D));
        }
        if ("wind".equals(getElement())) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.20D + tier * 0.01D, 0.0D));
        } else if ("earth".equals(getElement()) || "metal".equals(getElement())) {
            target.knockback(0.35D + tier * 0.025D, getX() - target.getX(), getZ() - target.getZ());
        }
        if (level() instanceof ServerLevel serverLevel) {
            ParticleOptions particle = switch (getElement()) {
                case "fire" -> ParticleTypes.FLAME;
                case "ice", "water" -> ParticleTypes.SNOWFLAKE;
                case "thunder" -> ParticleTypes.ELECTRIC_SPARK;
                case "poison", "wood" -> ParticleTypes.COMPOSTER;
                case "soul" -> ParticleTypes.SOUL;
                case "blood" -> ParticleTypes.DAMAGE_INDICATOR;
                case "illusion" -> ParticleTypes.ENCHANT;
                case "mixed" -> ParticleTypes.END_ROD;
                case "void" -> ParticleTypes.REVERSE_PORTAL;
                default -> ParticleTypes.ENCHANT;
            };
            serverLevel.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(), 6 + tier / 2, 0.25D, 0.3D, 0.25D, 0.02D);
        }
    }

    public static Optional<ServerPlayer> recentCompanionDamageOwner(LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        CompoundTag data = target.getPersistentData();
        if (!data.hasUUID(TAG_COMPANION_DAMAGE_OWNER)
                || data.getLong(TAG_COMPANION_DAMAGE_EXPIRY) < level.getGameTime()) {
            return Optional.empty();
        }
        Player owner = level.getPlayerByUUID(data.getUUID(TAG_COMPANION_DAMAGE_OWNER));
        return owner instanceof ServerPlayer serverPlayer ? Optional.of(serverPlayer) : Optional.empty();
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return isFriendlyEntity(entity) || super.isAlliedTo(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isCompanion() && (isFriendlyEntity(source.getEntity()) || isFriendlyEntity(source.getDirectEntity()))) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isCompanion()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (ownerUUID == null || !ownerUUID.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        SummonedServitorEntity.Stance next = cycleStance();
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.summon.stance", getDisplayName(), stanceDisplay(next)), true);
        return InteractionResult.CONSUME;
    }

    private static Component stanceDisplay(SummonedServitorEntity.Stance value) {
        String key = switch (value == null ? SummonedServitorEntity.Stance.FOLLOW : value) {
            case FOLLOW -> "follow";
            case GUARD -> "guard";
            case AGGRESSIVE -> "aggressive";
            case STAY -> "stay";
        };
        return Component.translatable("message.seeking_immortals.summon.stance." + key);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_BEAST_ID, getBeastId());
        tag.putInt(TAG_BEAST_TIER, getBeastTier());
        tag.putString(TAG_ELEMENT, getElement());
        tag.putString(TAG_BODY_PLAN, getBodyPlan().name());
        tag.putBoolean(TAG_BOSS, isCatalogBoss());
        tag.putBoolean(TAG_COMPANION, isCompanion());
        if (ownerUUID != null) {
            tag.putUUID(TAG_OWNER, ownerUUID);
        }
        tag.putInt(TAG_LIFE, lifeTicks);
        tag.putInt(TAG_MAX_LIFE, maxLifeTicks);
        tag.putString(TAG_STANCE, stance.name());
        tag.putDouble(TAG_GUARD_X, guardX);
        tag.putDouble(TAG_GUARD_Y, guardY);
        tag.putDouble(TAG_GUARD_Z, guardZ);
        net.minecraft.world.entity.ai.attributes.AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        net.minecraft.world.entity.ai.attributes.AttributeInstance attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);
        tag.putDouble(TAG_CONFIGURED_HEALTH,
                maxHealth == null ? getMaxHealth() : maxHealth.getBaseValue());
        tag.putDouble(TAG_CONFIGURED_DAMAGE,
                attackDamage == null ? getAttributeValue(Attributes.ATTACK_DAMAGE) : attackDamage.getBaseValue());
        tag.putBoolean(TAG_TERMINAL_GROWTH_CREDITED, terminalGrowthCredited);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float savedHealth = getHealth();
        Component savedCustomName = getCustomName();
        boolean savedCustomNameVisible = isCustomNameVisible();
        String id = normalizeId(tag.getString(TAG_BEAST_ID));
        int tier = BeastTierService.clampTier(tag.getInt(TAG_BEAST_TIER));
        boolean boss = tag.getBoolean(TAG_BOSS);
        configure(id, tier, boss, boss ? 2.4D : 1.0D, boss ? 1.6D : 1.0D);
        if (tag.contains(TAG_ELEMENT)) {
            entityData.set(DATA_ELEMENT, normalizeElement(tag.getString(TAG_ELEMENT), id));
        }
        if (tag.contains(TAG_BODY_PLAN)) {
            try {
                entityData.set(DATA_BODY_PLAN, BodyPlan.valueOf(tag.getString(TAG_BODY_PLAN)).ordinal());
            } catch (IllegalArgumentException ignored) {
                entityData.set(DATA_BODY_PLAN, BodyPlan.QUADRUPED.ordinal());
            }
        }
        boolean companion = tag.getBoolean(TAG_COMPANION);
        entityData.set(DATA_COMPANION, companion);
        ownerUUID = companion && tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        lifeTicks = companion ? Math.max(0, tag.getInt(TAG_LIFE)) : 0;
        maxLifeTicks = companion ? Math.max(lifeTicks, tag.getInt(TAG_MAX_LIFE)) : 0;
        try {
            stance = SummonedServitorEntity.Stance.valueOf(tag.getString(TAG_STANCE));
        } catch (RuntimeException ignored) {
            stance = SummonedServitorEntity.Stance.FOLLOW;
        }
        guardX = tag.getDouble(TAG_GUARD_X);
        guardY = tag.getDouble(TAG_GUARD_Y);
        guardZ = tag.getDouble(TAG_GUARD_Z);
        if (tag.contains(TAG_CONFIGURED_HEALTH)) {
            setBase(Attributes.MAX_HEALTH, Mth.clamp(tag.getDouble(TAG_CONFIGURED_HEALTH), 1.0D, 1024.0D));
        }
        if (tag.contains(TAG_CONFIGURED_DAMAGE)) {
            setBase(Attributes.ATTACK_DAMAGE, Mth.clamp(tag.getDouble(TAG_CONFIGURED_DAMAGE), 0.0D, 2048.0D));
        }
        terminalGrowthCredited = tag.getBoolean(TAG_TERMINAL_GROWTH_CREDITED);
        if (companion) {
            getPersistentData().putBoolean(TAG_ECOLOGY, false);
        }
        setHealth(Mth.clamp(savedHealth, 0.0F, getMaxHealth()));
        boolean ecology = getPersistentData().getBoolean(TAG_ECOLOGY);
        if (savedCustomName != null && (!ecology || boss || companion)) {
            setCustomName(savedCustomName);
        }
        setCustomNameVisible(savedCustomNameVisible || companion || boss);
        rebuildGoals();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        BeastSpawnGroupData group;
        if (spawnData instanceof BeastSpawnGroupData existing) {
            group = existing;
        } else {
            BeastSpawnTableService.Weight rolled = BeastSpawnTableService.rollFor(
                    level.getLevel(), blockPosition(), random).orElse(new BeastSpawnTableService.Weight("wild_beast", 1, 1));
            group = new BeastSpawnGroupData(rolled.beastId(), rolled.tier());
        }
        configureWild(group.beastId(), group.tier());
        return group;
    }

    public static boolean checkSpawnRules(EntityType<CultivationBeastEntity> type, ServerLevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || !Monster.isDarkEnoughToSpawn(level, pos, random)) {
            return false;
        }
        boolean waterColumn = level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
        boolean clearGroundColumn = level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && (spawnType == MobSpawnType.SPAWNER
                || level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type));
        if (!waterColumn && !clearGroundColumn) {
            return false;
        }
        AABB nearby = new AABB(pos).inflate(32.0D, 12.0D, 32.0D);
        return level.getLevel().getEntitiesOfClass(CultivationBeastEntity.class, nearby,
                        beast -> beast.getPersistentData().getBoolean(TAG_ECOLOGY)).size()
                < BeastSpawnTableService.MAX_ECOLOGY_NEAR;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && isCompanion() && reason.shouldDestroy()
                && level() instanceof ServerLevel serverLevel) {
            ServitorRegistrySavedData.get(serverLevel).remove(getUUID());
        }
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !isCompanion() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return !isCompanion()
                && !isCatalogBoss()
                && !BossEncounterService.isBossMob(this)
                && !TrialCombatShellService.isHostileShell(this)
                && !SecretRealmTrialService.isTrialMob(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop(state.isMoving() ? "walk" : "idle"));
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "attack", 2, state -> {
            if (swinging || attackAnim > 0.0F || hurtTime > 0) {
                state.setAnimation(RawAnimation.begin().thenPlay("attack"));
                return PlayState.CONTINUE;
            }
            state.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
