package com.xunxian.seekingimmortals.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public class MarketTraderEntity extends Villager {
    private net.minecraft.core.BlockPos stallPos;

    public MarketTraderEntity(EntityType<? extends MarketTraderEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Wave490: market hall NPC daily path — look at customers and keep stall vicinity.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(
                this, net.minecraft.world.entity.player.Player.class, 8.0F));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.45D));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (stallPos == null) {
            stallPos = blockPosition().immutable();
        }
        if (stallPos != null && distanceToSqr(stallPos.getX() + 0.5D, stallPos.getY(), stallPos.getZ() + 0.5D) > 36.0D) {
            getNavigation().moveTo(stallPos.getX() + 0.5D, stallPos.getY(), stallPos.getZ() + 0.5D, 0.6D);
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (stallPos != null) {
            tag.putLong("MarketStallPos", stallPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MarketStallPos")) {
            stallPos = net.minecraft.core.BlockPos.of(tag.getLong("MarketStallPos"));
        }
    }
}
