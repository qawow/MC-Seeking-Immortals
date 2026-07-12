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

    private String sectId = SectContributionService.SECT_ID;
    private String npcType = NPC_TYPE_RECRUITER;

    public SectStewardEntity(EntityType<? extends SectStewardEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSectId(tag.getString(TAG_SECT_ID));
        setNpcType(tag.getString(TAG_NPC_TYPE));
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? fallback : normalized;
    }
}
