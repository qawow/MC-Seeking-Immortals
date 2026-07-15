package com.xunxian.seekingimmortals.entity;

import com.xunxian.seekingimmortals.sect.SectContributionService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

import java.util.Locale;

public class SectStewardEntity extends Villager {
    public static final String NPC_TYPE_RECRUITER = "recruiter";

    private static final String TAG_SECT_ID = "SectId";
    private static final String TAG_NPC_TYPE = "SectNpcType";
    private static final String TAG_HOME = "SectHomePos";

    private String sectId = SectContributionService.SECT_ID;
    private String npcType = NPC_TYPE_RECRUITER;
    private net.minecraft.core.BlockPos homePos;

    public SectStewardEntity(EntityType<? extends SectStewardEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Wave490: light daily path AI — stay near home office and glance at nearby players.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 10.0F));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.55D) {
            @Override
            public boolean canUse() {
                return homePos != null && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (homePos == null) {
            homePos = blockPosition().immutable();
        }
        // Soft leash: if wandered too far from office, walk back.
        if (homePos != null && distanceToSqr(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D) > 64.0D) {
            getNavigation().moveTo(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D, 0.7D);
        }
    }

    public String getSectId() {
        return sectId;
    }

    public void setSectId(String sectId) {
        this.sectId = normalize(sectId, SectContributionService.SECT_ID);
    }

    public String getNpcType() {
        return npcType;
    }

    public void setNpcType(String npcType) {
        this.npcType = normalize(npcType, NPC_TYPE_RECRUITER);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_SECT_ID, sectId);
        tag.putString(TAG_NPC_TYPE, npcType);
        if (homePos != null) {
            tag.putLong(TAG_HOME, homePos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSectId(tag.getString(TAG_SECT_ID));
        setNpcType(tag.getString(TAG_NPC_TYPE));
        if (tag.contains(TAG_HOME)) {
            homePos = net.minecraft.core.BlockPos.of(tag.getLong(TAG_HOME));
        }
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? fallback : normalized;
    }
}
