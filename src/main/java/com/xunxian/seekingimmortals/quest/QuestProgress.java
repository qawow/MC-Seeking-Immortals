package com.xunxian.seekingimmortals.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class QuestProgress {
    private int stage;
    private final Set<String> flags = new HashSet<>();
    private String branchChoice = "";
    private String sectId = "";
    private String sectRole = "";
    private int sectQuestStage;
    private final Set<String> sectFlags = new HashSet<>();
    private int contribution;
    private int reputation;
    private boolean yueArrived;
    private String sectMissionId = "";
    private long sectMissionDay = Long.MIN_VALUE;
    private boolean sectMissionAccepted;
    private boolean sectMissionCompleted;
    @Nullable
    private BlockPos secretRoomMarker;
    @Nullable
    private BlockPos yuePortalMarker;

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = Math.max(0, Math.min(SevenMysteriesQuest.STAGE_COMPLETE, stage));
    }

    public boolean isStarted() {
        return stage > SevenMysteriesQuest.STAGE_NOT_STARTED;
    }

    public boolean isComplete() {
        return stage >= SevenMysteriesQuest.STAGE_COMPLETE;
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public boolean addFlag(String flag) {
        return flag != null && !flag.isBlank() && flags.add(flag);
    }

    public void removeFlag(String flag) {
        flags.remove(flag);
    }

    public Set<String> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    public String getBranchChoice() {
        return branchChoice;
    }

    public void setBranchChoice(String branchChoice) {
        this.branchChoice = branchChoice == null ? "" : branchChoice;
    }

    public String getSectId() {
        return sectId;
    }

    public void setSect(String sectId, String sectRole) {
        this.sectId = sectId == null ? "" : sectId;
        this.sectRole = sectRole == null ? "" : sectRole;
    }

    public String getSectRole() {
        return sectRole;
    }

    public int getSectQuestStage() {
        return sectQuestStage;
    }

    public void setSectQuestStage(int sectQuestStage) {
        this.sectQuestStage = Math.max(0, sectQuestStage);
    }

    public boolean hasSectFlag(String flag) {
        return sectFlags.contains(flag);
    }

    public boolean addSectFlag(String flag) {
        return flag != null && !flag.isBlank() && sectFlags.add(flag);
    }

    public void removeSectFlag(String flag) {
        sectFlags.remove(flag);
    }

    public Set<String> getSectFlags() {
        return Collections.unmodifiableSet(sectFlags);
    }

    public int getContribution() {
        return contribution;
    }

    public void addContribution(int amount) {
        contribution = Math.max(0, contribution + amount);
    }

    public boolean spendContribution(int amount) {
        if (amount <= 0 || contribution < amount) {
            return false;
        }
        contribution -= amount;
        return true;
    }

    public int getReputation() {
        return reputation;
    }

    public void addReputation(int amount) {
        reputation += amount;
    }

    public boolean hasYueArrived() {
        return yueArrived;
    }

    public void setYueArrived(boolean yueArrived) {
        this.yueArrived = yueArrived;
    }

    public String getSectMissionId() {
        return sectMissionId;
    }

    public long getSectMissionDay() {
        return sectMissionDay;
    }

    public boolean isSectMissionAccepted() {
        return sectMissionAccepted;
    }

    public boolean isSectMissionCompleted() {
        return sectMissionCompleted;
    }

    public void setSectMission(String missionId, long missionDay) {
        this.sectMissionId = missionId == null ? "" : missionId;
        this.sectMissionDay = missionDay;
        this.sectMissionAccepted = !this.sectMissionId.isBlank();
        this.sectMissionCompleted = false;
    }

    public void completeSectMission() {
        this.sectMissionCompleted = true;
    }

    public void clearSectMission() {
        this.sectMissionId = "";
        this.sectMissionDay = Long.MIN_VALUE;
        this.sectMissionAccepted = false;
        this.sectMissionCompleted = false;
    }

    @Nullable
    public BlockPos getSecretRoomMarker() {
        return secretRoomMarker;
    }

    public void setSecretRoomMarker(@Nullable BlockPos secretRoomMarker) {
        this.secretRoomMarker = secretRoomMarker == null ? null : secretRoomMarker.immutable();
    }

    @Nullable
    public BlockPos getYuePortalMarker() {
        return yuePortalMarker;
    }

    public void setYuePortalMarker(@Nullable BlockPos yuePortalMarker) {
        this.yuePortalMarker = yuePortalMarker == null ? null : yuePortalMarker.immutable();
    }

    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SevenMysteriesStage", stage);
        tag.putString("BranchChoice", branchChoice);
        tag.putString("SectId", sectId);
        tag.putString("SectRole", sectRole);
        tag.putInt("SectQuestStage", sectQuestStage);
        tag.putInt("Contribution", contribution);
        tag.putInt("SevenMysteriesReputation", reputation);
        tag.putBoolean("YueArrived", yueArrived);
        tag.putString("SectMissionId", sectMissionId);
        tag.putLong("SectMissionDay", sectMissionDay);
        tag.putBoolean("SectMissionAccepted", sectMissionAccepted);
        tag.putBoolean("SectMissionCompleted", sectMissionCompleted);
        if (secretRoomMarker != null) {
            tag.put("SecretRoomMarker", saveBlockPos(secretRoomMarker));
        }
        if (yuePortalMarker != null) {
            tag.put("YuePortalMarker", saveBlockPos(yuePortalMarker));
        }
        ListTag flagList = new ListTag();
        flags.stream().sorted().forEach(flag -> flagList.add(StringTag.valueOf(flag)));
        tag.put("Flags", flagList);
        ListTag sectFlagList = new ListTag();
        sectFlags.stream().sorted().forEach(flag -> sectFlagList.add(StringTag.valueOf(flag)));
        tag.put("SectFlags", sectFlagList);
        return tag;
    }

    public void loadNBT(CompoundTag tag) {
        stage = Math.max(0, Math.min(SevenMysteriesQuest.STAGE_COMPLETE, tag.getInt("SevenMysteriesStage")));
        branchChoice = tag.getString("BranchChoice");
        sectId = tag.getString("SectId");
        sectRole = tag.getString("SectRole");
        sectQuestStage = Math.max(0, tag.getInt("SectQuestStage"));
        if ("qinglan_sect".equals(sectId)
                && (sectQuestStage <= 0 || sectQuestStage > com.xunxian.seekingimmortals.sect.SectContributionService.STAGE_PHASE10_COMPLETE)) {
            sectQuestStage = 2;
        }
        contribution = Math.max(0, tag.getInt("Contribution"));
        reputation = tag.getInt("SevenMysteriesReputation");
        yueArrived = tag.getBoolean("YueArrived");
        sectMissionId = tag.getString("SectMissionId");
        sectMissionDay = tag.contains("SectMissionDay", Tag.TAG_LONG) ? tag.getLong("SectMissionDay") : Long.MIN_VALUE;
        sectMissionAccepted = tag.getBoolean("SectMissionAccepted") && !sectMissionId.isBlank();
        sectMissionCompleted = tag.getBoolean("SectMissionCompleted");
        secretRoomMarker = tag.contains("SecretRoomMarker", Tag.TAG_COMPOUND)
                ? loadBlockPos(tag.getCompound("SecretRoomMarker"))
                : null;
        yuePortalMarker = tag.contains("YuePortalMarker", Tag.TAG_COMPOUND)
                ? loadBlockPos(tag.getCompound("YuePortalMarker"))
                : null;
        flags.clear();
        ListTag flagList = tag.getList("Flags", 8);
        for (int i = 0; i < flagList.size(); i++) {
            String flag = flagList.getString(i);
            if (!flag.isBlank()) {
                flags.add(flag);
            }
        }
        sectFlags.clear();
        ListTag sectFlagList = tag.getList("SectFlags", 8);
        for (int i = 0; i < sectFlagList.size(); i++) {
            String flag = sectFlagList.getString(i);
            if (!flag.isBlank()) {
                sectFlags.add(flag);
            }
        }
    }

    private static CompoundTag saveBlockPos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    private static BlockPos loadBlockPos(CompoundTag tag) {
        return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
    }
}
