package com.xunxian.seekingimmortals.skill.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SkillContext {
    private final Level level;
    private final Vec3 position;
    private final Vec3 lookDirection;
    private final Entity targetEntity;
    private final BlockPos targetBlock;

    private SkillContext(Builder builder) {
        this.level = builder.level;
        this.position = builder.position;
        this.lookDirection = builder.lookDirection;
        this.targetEntity = builder.targetEntity;
        this.targetBlock = builder.targetBlock;
    }

    public Level getLevel() { return level; }
    public Vec3 getPosition() { return position; }
    public Vec3 getLookDirection() { return lookDirection; }
    public Entity getTargetEntity() { return targetEntity; }
    public BlockPos getTargetBlock() { return targetBlock; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Level level;
        private Vec3 position;
        private Vec3 lookDirection;
        private Entity targetEntity;
        private BlockPos targetBlock;

        public Builder level(Level level) { this.level = level; return this; }
        public Builder position(Vec3 position) { this.position = position; return this; }
        public Builder lookDirection(Vec3 lookDirection) { this.lookDirection = lookDirection; return this; }
        public Builder targetEntity(Entity targetEntity) { this.targetEntity = targetEntity; return this; }
        public Builder targetBlock(BlockPos targetBlock) { this.targetBlock = targetBlock; return this; }

        public SkillContext build() { return new SkillContext(this); }
    }
}
