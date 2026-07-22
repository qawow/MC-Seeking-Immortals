package com.xunxian.seekingimmortals.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Original spirit-stone banker NPC (M05). Handles ladder upgrades; not a vanilla villager.
 */
public class SpiritStoneBankerEntity extends CultivatorNpcEntity {
    private BlockPos deskPos;

    public SpiritStoneBankerEntity(EntityType<? extends SpiritStoneBankerEntity> type, Level level) {
        super(type, level, VisualRole.BANKER);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.4D));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (deskPos == null) {
            deskPos = blockPosition().immutable();
        }
        double dist = distanceToSqr(deskPos.getX() + 0.5D, deskPos.getY(), deskPos.getZ() + 0.5D);
        if (dist > 36.0D && tickCount % 20 == 0 && getNavigation().isDone()) {
            getNavigation().moveTo(deskPos.getX() + 0.5D, deskPos.getY(), deskPos.getZ() + 0.5D, 0.55D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (deskPos != null) {
            tag.putLong("SpiritBankDeskPos", deskPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpiritBankDeskPos")) {
            deskPos = BlockPos.of(tag.getLong("SpiritBankDeskPos"));
        }
    }
}
