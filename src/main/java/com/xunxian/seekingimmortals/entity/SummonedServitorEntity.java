package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.beast.PuppetGrowthService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.skill.effect.TechniqueLifecycleVfxService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.ServitorRegistrySavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * Real combat servitor for text-material summon/puppet techniques.
 * Wave48: archetype-based AI / stats.
 * Wave56: GeckoLib GeoEntity.
 * Wave455: owner retaliate.
 * Wave458: stance modes, crafted flag, combat credit hooks, owner interact cycle.
 * Wave480: hostile trial mode for secret-realm typed combat shells (no owner, hunts players).
 */
public class SummonedServitorEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final String TAG_OWNER = "OwnerUUID";
    private static final String TAG_SUMMON_ID = "SummonId";
    private static final String TAG_LIFE = "LifeTicks";
    private static final String TAG_ARCHETYPE = "Archetype";
    private static final String TAG_STANCE = "Stance";
    private static final String TAG_CRAFTED = "Crafted";
    private static final String TAG_HOSTILE = "HostileTrial";
    private static final String TAG_GUARD_X = "GuardX";
    private static final String TAG_GUARD_Y = "GuardY";
    private static final String TAG_GUARD_Z = "GuardZ";
    private static final String TAG_MAX_LIFE = "MaxLifeTicks";
    private static final EntityDataAccessor<Integer> DATA_ARCHETYPE = SynchedEntityData.defineId(
            SummonedServitorEntity.class, EntityDataSerializers.INT);

    public enum Archetype {
        BEAST,
        PUPPET,
        GHOST,
        GENERIC
    }

    public enum Stance {
        FOLLOW,
        GUARD,
        AGGRESSIVE,
        STAY
    }

    private UUID ownerUUID;
    private String summonId = "summon";
    private int lifeTicks;
    private int maxLifeTicks;
    private Archetype archetype = Archetype.GENERIC;
    private Stance stance = Stance.FOLLOW;
    private boolean crafted;
    private boolean hostileTrial;
    private boolean summonVfxArmed;
    private boolean summonVfxSent;
    private boolean terminalVfxSent;
    private double guardX;
    private double guardY;
    private double guardZ;

    public SummonedServitorEntity(EntityType<? extends SummonedServitorEntity> type, Level level) {
        super(type, level);
        this.lifeTicks = 20 * 25;
        this.maxLifeTicks = this.lifeTicks;
        setPersistenceRequired();
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide && summonVfxArmed && !summonVfxSent) {
            summonVfxSent = true;
            TechniqueLifecycleVfxService.summon(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ARCHETYPE, Archetype.GENERIC.ordinal());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 3.0D);
    }

    public void configure(Player owner, String summonId, int lifeTicks, double health, double damage) {
        configure(owner, summonId, lifeTicks, health, damage, Archetype.GENERIC);
    }

    public void configure(Player owner, String summonId, int lifeTicks, double health, double damage, Archetype archetype) {
        this.hostileTrial = false;
        this.summonVfxArmed = true;
        this.ownerUUID = owner.getUUID();
        this.summonId = summonId == null || summonId.isBlank() ? "summon" : summonId;
        this.lifeTicks = Math.max(20 * 8, lifeTicks);
        this.maxLifeTicks = this.lifeTicks;
        setArchetype(archetype);
        this.stance = Stance.FOLLOW;
        this.guardX = owner.getX();
        this.guardY = owner.getY();
        this.guardZ = owner.getZ();
        applyArchetypeStats(health, damage);
        setCustomName(Component.translatable("entity.seeking_immortals.summoned_servitor.name",
                summonDisplay()));
        setCustomNameVisible(true);
        rebuildGoals();
    }

    /**
     * Wave480: secret-realm hostile shell. No owner leash; hunts players; long lifetime.
     */
    public void configureHostileTrial(String shellId, int lifeTicks, double health, double damage, Archetype archetype) {
        this.hostileTrial = true;
        this.summonVfxArmed = true;
        this.ownerUUID = null;
        this.crafted = false;
        this.summonId = shellId == null || shellId.isBlank() ? "trial_shell" : shellId;
        this.lifeTicks = Math.max(20 * 60 * 10, lifeTicks);
        this.maxLifeTicks = this.lifeTicks;
        setArchetype(archetype);
        this.stance = Stance.AGGRESSIVE;
        this.guardX = getX();
        this.guardY = getY();
        this.guardZ = getZ();
        applyArchetypeStats(health, damage);
        // Hostile shells get a bit more follow range so they stick to the player in arenas.
        if (getAttribute(Attributes.FOLLOW_RANGE) != null) {
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.max(28.0D,
                    getAttribute(Attributes.FOLLOW_RANGE).getBaseValue()));
        }
        setCustomName(Component.translatable("entity.seeking_immortals.trial_shell.name",
                archetypeDisplay()));
        setCustomNameVisible(true);
        rebuildGoals();
    }

    private void applyArchetypeStats(double health, double damage) {
        double speed = switch (this.archetype) {
            case BEAST -> 0.34D;
            case PUPPET -> 0.24D;
            case GHOST -> 0.30D;
            default -> 0.28D;
        };
        double armor = switch (this.archetype) {
            case BEAST -> 2.0D;
            case PUPPET -> 6.0D;
            case GHOST -> 1.0D;
            default -> 3.0D;
        };
        double follow = switch (this.archetype) {
            case BEAST -> 28.0D;
            case PUPPET -> 20.0D;
            case GHOST -> 26.0D;
            default -> 24.0D;
        };
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(8.0D, health));
            setHealth((float) Math.max(8.0D, health));
        }
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(2.0D, damage));
        }
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
        if (getAttribute(Attributes.ARMOR) != null) {
            getAttribute(Attributes.ARMOR).setBaseValue(armor);
        }
        if (getAttribute(Attributes.FOLLOW_RANGE) != null) {
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(follow);
        }
    }

    private void rebuildGoals() {
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        registerGoals();
    }

    public boolean isHostileTrial() {
        return hostileTrial;
    }

    public Optional<UUID> getOwnerUUID() {
        return Optional.ofNullable(ownerUUID);
    }

    public String getSummonId() {
        return summonId;
    }

    public Archetype getArchetype() {
        int ordinal = entityData.get(DATA_ARCHETYPE);
        Archetype[] values = Archetype.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Archetype.GENERIC;
    }

    private void setArchetype(Archetype archetype) {
        this.archetype = archetype == null ? Archetype.GENERIC : archetype;
        entityData.set(DATA_ARCHETYPE, this.archetype.ordinal());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ARCHETYPE.equals(key)) {
            this.archetype = getArchetype();
        }
    }

    public Stance getStance() {
        return stance;
    }

    public void setStance(Stance stance) {
        this.stance = stance == null ? Stance.FOLLOW : stance;
        if (this.stance == Stance.GUARD) {
            this.guardX = getX();
            this.guardY = getY();
            this.guardZ = getZ();
        }
        if (this.stance == Stance.STAY) {
            getNavigation().stop();
            setTarget(null);
        }
        if (!level().isClientSide && !hostileTrial && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            ServitorRegistrySavedData.get(serverLevel).setStance(getUUID(), this.stance.name());
        }
    }

    public Stance cycleStance() {
        Stance next = switch (stance) {
            case FOLLOW -> Stance.GUARD;
            case GUARD -> Stance.AGGRESSIVE;
            case AGGRESSIVE -> Stance.STAY;
            case STAY -> Stance.FOLLOW;
        };
        setStance(next);
        return this.stance;
    }

    public boolean isCrafted() {
        return crafted;
    }

    public void setCrafted(boolean crafted) {
        this.crafted = crafted;
    }

    public int getLifeTicksRemaining() {
        return lifeTicks;
    }

    public int getMaxLifeTicks() {
        return Math.max(lifeTicks, maxLifeTicks);
    }

    public void extendLife(int ticks) {
        int cap = Math.max(maxLifeTicks, 20 * 600);
        lifeTicks = Math.min(cap, lifeTicks + Math.max(0, ticks));
        maxLifeTicks = Math.max(maxLifeTicks, lifeTicks);
    }

    public void repair(float healAmount) {
        if (healAmount > 0.0F) {
            heal(healAmount);
        }
        extendLife(20 * 60);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        Archetype goalArchetype = archetype == null ? Archetype.GENERIC : archetype;
        double attackSpeed = switch (goalArchetype) {
            case BEAST -> 1.35D;
            case PUPPET -> 1.00D;
            case GHOST -> 1.20D;
            default -> 1.15D;
        };
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, attackSpeed, true));
        double stroll = switch (goalArchetype) {
            case BEAST -> 1.05D;
            case PUPPET -> 0.75D;
            case GHOST -> 0.95D;
            default -> 0.9D;
        };
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, stroll));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        if (hostileTrial) {
            // Wave480: secret-realm shells hunt players, not monsters.
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        } else {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            lifeTicks--;
            if (lifeTicks <= 0) {
                sendDissipate();
                discard();
                return;
            }
            if (!hostileTrial && ownerUUID != null && level() instanceof ServerLevel serverLevel
                    && applyRegistryState(serverLevel)) {
                return;
            }
            if (isAlive() && !terminalVfxSent && Math.floorMod(tickCount + getId(), 40) == 0) {
                TechniqueLifecycleVfxService.servitorStatus(this);
            }
            // Hostile trial shells do not require an owner and stay in arena AI.
            if (hostileTrial) {
                // M10: phased boss skills for secret-realm bosses.
                com.xunxian.seekingimmortals.beast.BeastBossService.tickBossSkills(this);
                return;
            }
            if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
                ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
                if (owner != null && owner.serverLevel() == serverLevel) {
                    applyStanceBehavior(owner);
                }
            }
        }
    }

    private boolean applyRegistryState(ServerLevel serverLevel) {
        ServitorRegistrySavedData registry = ServitorRegistrySavedData.get(serverLevel);
        ServitorRegistrySavedData.State state = registry.register(
                ownerUUID,
                getUUID(),
                serverLevel.dimension().location().toString(),
                stance.name(),
                SummonHonestMvpService.MAX_ACTIVE_SERVITORS);
        if (state == null) {
            return false;
        }
        if (state.dismissed()) {
            discard();
            return true;
        }
        try {
            Stance desired = Stance.valueOf(state.stance());
            if (desired != stance) {
                setStance(desired);
            }
        } catch (IllegalArgumentException ignored) {
            setStance(Stance.FOLLOW);
        }
        return false;
    }

    private void applyStanceBehavior(Player owner) {
        // Retaliate unless STAY.
        if (stance != Stance.STAY && (getTarget() == null || !getTarget().isAlive())) {
            LivingEntity ownerAttacker = owner.getLastHurtByMob();
            if (ownerAttacker != null && ownerAttacker.isAlive() && ownerAttacker != this && ownerAttacker != owner) {
                setTarget(ownerAttacker);
            } else {
                LivingEntity lastHurt = owner.getLastHurtMob();
                if (lastHurt != null && lastHurt.isAlive() && lastHurt != this && lastHurt != owner) {
                    setTarget(lastHurt);
                }
            }
        }

        switch (stance) {
            case STAY -> {
                getNavigation().stop();
                if (getTarget() != null) {
                    setTarget(null);
                }
            }
            case GUARD -> {
                double gx = guardX;
                double gy = guardY;
                double gz = guardZ;
                if (distanceToSqr(gx, gy, gz) > 4.0D * 4.0D && getTarget() == null) {
                    getNavigation().moveTo(gx, gy, gz, 1.0D);
                }
            }
            case AGGRESSIVE -> {
                // Keep hunting monsters even when idle; follow owner more loosely.
                if (getTarget() == null || !getTarget().isAlive()) {
                    // NearestAttackableTargetGoal handles pick; still leash if very far.
                    if (distanceToSqr(owner) > 24.0D * 24.0D) {
                        getNavigation().moveTo(owner, 1.25D);
                    }
                }
            }
            case FOLLOW -> {
                double leash = switch (archetype) {
                    case BEAST -> 14.0D;
                    case PUPPET -> 8.0D;
                    case GHOST -> 12.0D;
                    default -> 10.0D;
                };
                double followSpeed = switch (archetype) {
                    case BEAST -> 1.30D;
                    case PUPPET -> 1.00D;
                    case GHOST -> 1.20D;
                    default -> 1.15D;
                };
                if (getTarget() == null && distanceToSqr(owner) > leash * leash) {
                    getNavigation().moveTo(owner, followSpeed);
                }
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // Hostile trial shells are not controllable pets.
        if (hostileTrial || ownerUUID == null || !ownerUUID.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        Stance next = cycleStance();
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.summon.stance", summonDisplay(), stanceDisplay(next)), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Friendly fire immunity only for owned summons, not hostile trial shells.
        if (!hostileTrial && source.getEntity() instanceof Player player
                && ownerUUID != null && ownerUUID.equals(player.getUUID())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            float bonus = switch (archetype) {
                case BEAST -> 1.5F;
                case PUPPET -> 0.5F;
                case GHOST -> 2.0F;
                default -> 1.0F;
            };
            living.hurt(damageSources().magic(), bonus);
            if (archetype == Archetype.GHOST) {
                living.setDeltaMovement(living.getDeltaMovement().add(0.0D, 0.15D, 0.0D));
            }
            TechniqueLifecycleVfxService.servitorImpact(
                    this, living.position().add(0.0D, living.getBbHeight() * 0.55D, 0.0D));
            // Wave458: beast combat credit.
            if (archetype == Archetype.BEAST && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(ownerUUID);
                if (owner instanceof ServerPlayer serverPlayer) {
                    String beastId = BeastContractService.beastIdFromSummonId(summonId);
                    if (!beastId.isBlank()) {
                        if (!living.isAlive()) {
                            BeastContractService.recordCombatCredit(serverPlayer, beastId, BeastContractService.CreditKind.KILL);
                        } else {
                            BeastContractService.recordCombatCredit(serverPlayer, beastId, BeastContractService.CreditKind.HIT);
                        }
                    }
                }
            }
            if (archetype == Archetype.PUPPET && crafted && ownerUUID != null
                    && level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(ownerUUID);
                if (owner instanceof ServerPlayer serverPlayer) {
                    PuppetGrowthService.CreditKind kind = living.isAlive()
                            ? PuppetGrowthService.CreditKind.HIT : PuppetGrowthService.CreditKind.KILL;
                    PuppetGrowthService.GrowthResult growth = PuppetGrowthService.recordCombatCredit(
                            serverPlayer, summonId, kind);
                    if (kind == PuppetGrowthService.CreditKind.KILL) {
                        serverPlayer.displayClientMessage(Component.translatable(
                                "message.seeking_immortals.puppet.combat_growth",
                                displayForId(growth.puppetId()), growth.after().level(), growth.after().experience()), true);
                        if (growth.update().evolutionBlocked()) {
                            serverPlayer.displayClientMessage(Component.translatable(
                                    "message.seeking_immortals.puppet.core_forge_required"), false);
                        }
                    }
                }
            }
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !hostileTrial && archetype == Archetype.PUPPET
                && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerUUID);
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("message.seeking_immortals.puppet.core_cracked",
                        summonDisplay()), true);
            }
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            sendDissipate();
        }
        if (!level().isClientSide && reason.shouldDestroy() && level() instanceof ServerLevel serverLevel) {
            ServitorRegistrySavedData.get(serverLevel).remove(getUUID());
        }
        super.remove(reason);
    }

    private void sendDissipate() {
        if (terminalVfxSent || level().isClientSide) {
            return;
        }
        terminalVfxSent = true;
        TechniqueLifecycleVfxService.servitorDissipate(this);
    }

    private Component summonDisplay() {
        return displayForId(summonId);
    }

    private Component displayForId(String id) {
        String normalized = PlayerDisplayText.normalizeId(id);
        String itemKey = "item.seeking_immortals." + normalized;
        if (!normalized.isBlank() && PlayerDisplayText.hasTranslation(itemKey)) {
            return Component.translatable(itemKey);
        }
        return SummonHonestMvpService.findPuppet(id)
                .filter(entry -> PlayerDisplayText.isSafe(entry.display()))
                .<Component>map(entry -> Component.literal(entry.display().trim()))
                .orElseGet(this::archetypeDisplay);
    }

    private Component archetypeDisplay() {
        return Component.translatable("message.seeking_immortals.summon.archetype." +
                switch (archetype) {
                    case BEAST -> "beast";
                    case PUPPET -> "puppet";
                    case GHOST -> "ghost";
                    case GENERIC -> "generic";
                });
    }

    private static Component stanceDisplay(Stance value) {
        String key = switch (value == null ? Stance.FOLLOW : value) {
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
        if (ownerUUID != null) {
            tag.putUUID(TAG_OWNER, ownerUUID);
        }
        tag.putString(TAG_SUMMON_ID, summonId);
        tag.putInt(TAG_LIFE, lifeTicks);
        tag.putInt(TAG_MAX_LIFE, maxLifeTicks);
        tag.putString(TAG_ARCHETYPE, getArchetype().name());
        tag.putString(TAG_STANCE, stance.name());
        tag.putBoolean(TAG_CRAFTED, crafted);
        tag.putBoolean(TAG_HOSTILE, hostileTrial);
        tag.putDouble(TAG_GUARD_X, guardX);
        tag.putDouble(TAG_GUARD_Y, guardY);
        tag.putDouble(TAG_GUARD_Z, guardZ);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            ownerUUID = tag.getUUID(TAG_OWNER);
        } else {
            ownerUUID = null;
        }
        summonId = tag.getString(TAG_SUMMON_ID);
        if (summonId == null || summonId.isBlank()) {
            summonId = "summon";
        }
        lifeTicks = Math.max(0, tag.getInt(TAG_LIFE));
        maxLifeTicks = Math.max(lifeTicks, tag.getInt(TAG_MAX_LIFE));
        try {
            setArchetype(Archetype.valueOf(tag.getString(TAG_ARCHETYPE)));
        } catch (RuntimeException ignored) {
            setArchetype(Archetype.GENERIC);
        }
        try {
            stance = Stance.valueOf(tag.getString(TAG_STANCE));
        } catch (RuntimeException ignored) {
            stance = Stance.FOLLOW;
        }
        crafted = tag.getBoolean(TAG_CRAFTED);
        hostileTrial = tag.getBoolean(TAG_HOSTILE);
        guardX = tag.getDouble(TAG_GUARD_X);
        guardY = tag.getDouble(TAG_GUARD_Y);
        guardZ = tag.getDouble(TAG_GUARD_Z);
        // Rebuild AI after load so hostile shells keep hunting players.
        rebuildGoals();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getArchetype() == Archetype.GHOST && !state.isMoving()) {
                state.setAnimation(RawAnimation.begin().thenLoop("float_idle"));
                return PlayState.CONTINUE;
            }
            if (state.isMoving()) {
                state.setAnimation(RawAnimation.begin().thenLoop("walk"));
            } else {
                state.setAnimation(RawAnimation.begin().thenLoop("idle"));
            }
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "attack", 2, state -> {
            if (this.swinging || this.attackAnim > 0.0F || this.hurtTime > 0) {
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

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
