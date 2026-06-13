package com.xunxian.seekingimmortals.skill;

import net.minecraft.nbt.CompoundTag;

public class CultivationSkill {
    private final SkillType skillType;
    private int level;
    private int experience;
    private int proficiency;
    private boolean unlocked;

    public CultivationSkill(SkillType skillType) {
        this.skillType = skillType;
        this.level = 0;
        this.experience = 0;
        this.proficiency = 0;
        this.unlocked = false;
    }

    public SkillType getSkillType() { return skillType; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getProficiency() { return proficiency; }
    public boolean isUnlocked() { return unlocked; }

    public void unlock() {
        this.unlocked = true;
        this.level = 1;
    }

    public void addExperience(int amount) {
        if (!unlocked) return;
        experience += amount;
        while (experience >= getExpForNextLevel() && level < getMaxLevel()) {
            experience -= getExpForNextLevel();
            level++;
        }
    }

    public void addProficiency(int amount) {
        if (!unlocked) return;
        proficiency = Math.max(0, Math.min(10000, proficiency + amount));
    }

    public int getExpForNextLevel() {
        return 100 + level * 50;
    }

    public int getMaxLevel() {
        return switch (skillType.getCategory()) {
            case CULTIVATION_METHOD -> 10;
            case SPELL -> 9;
            case CRAFTING -> 10;
            case SPECIAL -> 5;
        };
    }

    public double getProficiencyMultiplier() {
        return 1.0D + proficiency / 10000.0D;
    }

    public double getEffectivenessMultiplier() {
        return (1.0D + level * 0.15D) * getProficiencyMultiplier();
    }

    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("SkillType", skillType.name());
        tag.putInt("Level", level);
        tag.putInt("Experience", experience);
        tag.putInt("Proficiency", proficiency);
        tag.putBoolean("Unlocked", unlocked);
        return tag;
    }

    public static CultivationSkill loadNBT(CompoundTag tag) {
        SkillType skillType;
        try {
            skillType = SkillType.valueOf(tag.getString("SkillType"));
        } catch (Exception e) {
            return null;
        }
        CultivationSkill skill = new CultivationSkill(skillType);
        skill.level = tag.getInt("Level");
        skill.experience = tag.getInt("Experience");
        skill.proficiency = tag.getInt("Proficiency");
        skill.unlocked = tag.getBoolean("Unlocked");
        return skill;
    }
}
