package com.xunxian.seekingimmortals.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Dedicated cultivator NPC base. It deliberately does not inherit villager
 * professions, gossip, breeding, trading offers, or zombie conversion.
 */
public abstract class CultivatorNpcEntity extends PathfinderMob implements GeoEntity {
    public enum VisualRole {
        STEWARD,
        TRADER,
        BANKER,
        QUEST
    }

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final VisualRole visualRole;

    protected CultivatorNpcEntity(EntityType<? extends PathfinderMob> type, Level level, VisualRole visualRole) {
        super(type, level);
        this.visualRole = visualRole == null ? VisualRole.QUEST : visualRole;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 7.0F, 0.75D, 1.0D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public final VisualRole getVisualRole() {
        return visualRole;
    }

    public String getNamedNpcId() {
        return "";
    }

    public String getRegionId() {
        return "";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop(state.isMoving() ? "walk" : "idle"));
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
