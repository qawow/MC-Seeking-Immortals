package com.xunxian.seekingimmortals.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * Custom PathfinderMob (not vanilla projectile attack core). Owner-bound, timed despawn.
 * Wave48: archetype-based AI / stats (beast / puppet / ghost / generic).
 * Wave56: GeckoLib GeoEntity for high-fidelity skeletal rendering.
 */
public class SummonedServitorEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final String TAG_OWNER = "OwnerUUID";
    private static final String TAG_SUMMON_ID = "SummonId";
    private static final String TAG_LIFE = "LifeTicks";
    private static final String TAG_ARCHETYPE = "Archetype";

    public enum Archetype {
        BEAST,
        PUPPET,
        GHOST,
        GENERIC
    }

    private UUID ownerUUID;
    private String summonId = "summon";
    private int lifeTicks;
    private Archetype archetype = Archetype.GENERIC;

    public SummonedServitorEntity(EntityType<? extends SummonedServitorEntity> type, Level level) {
        super(type, level);
        this.lifeTicks = 20 * 25;
        setPersistenceRequired();
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
        this.ownerUUID = owner.getUUID();
        this.summonId = summonId == null || summonId.isBlank() ? "summon" : summonId;
        this.lifeTicks = Math.max(20 * 8, lifeTicks);
        this.archetype = archetype == null ? Archetype.GENERIC : archetype;
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
        setCustomName(Component.translatable("entity.seeking_immortals.summoned_servitor.name", this.summonId));
        setCustomNameVisible(true);
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        registerGoals();
    }

    public Optional<UUID> getOwnerUUID() {
        return Optional.ofNullable(ownerUUID);
    }

    public String getSummonId() {
        return summonId;
    }

    public Archetype getArchetype() {
        return archetype;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        double attackSpeed = switch (archetype) {
            case BEAST -> 1.35D;
            case PUPPET -> 1.00D;
            case GHOST -> 1.20D;
            default -> 1.15D;
        };
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, attackSpeed, true));
        double stroll = switch (archetype) {
            case BEAST -> 1.05D;
            case PUPPET -> 0.75D;
            case GHOST -> 0.95D;
            default -> 0.9D;
        };
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, stroll));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            lifeTicks--;
            if (lifeTicks <= 0) {
                discard();
                return;
            }
            if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
                Player owner = serverLevel.getPlayerByUUID(ownerUUID);
                if (owner == null) {
                    discard();
                    return;
                }
                // Keep servitor near owner when idle; custom follow (not vanilla wolf/tameable).
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
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && ownerUUID != null && ownerUUID.equals(player.getUUID())) {
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
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) {
            tag.putUUID(TAG_OWNER, ownerUUID);
        }
        tag.putString(TAG_SUMMON_ID, summonId);
        tag.putInt(TAG_LIFE, lifeTicks);
        tag.putString(TAG_ARCHETYPE, archetype.name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            ownerUUID = tag.getUUID(TAG_OWNER);
        }
        summonId = tag.getString(TAG_SUMMON_ID);
        if (summonId == null || summonId.isBlank()) {
            summonId = "summon";
        }
        lifeTicks = Math.max(0, tag.getInt(TAG_LIFE));
        try {
            archetype = Archetype.valueOf(tag.getString(TAG_ARCHETYPE));
        } catch (RuntimeException ignored) {
            archetype = Archetype.GENERIC;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (state.isMoving()) {
                state.setAnimation(RawAnimation.begin().thenLoop("walk"));
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

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
