package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.network.SyncSkillDataPacket;
import com.xunxian.seekingimmortals.skill.SkillType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Client-only mirror of the server-authoritative skill snapshot. */
public final class ClientSkillData {
    private static Map<SkillType, SkillSnapshot> skills = emptySkills();
    private static boolean synced;

    private ClientSkillData() {}

    public static void set(SyncSkillDataPacket packet) {
        if (packet == null) {
            reset();
            return;
        }
        setSkillData(packet.skills());
    }

    public static void setSkillData(List<SyncSkillDataPacket.SkillData> entries) {
        EnumMap<SkillType, SkillSnapshot> normalized = mutableEmptySkills();
        if (entries != null) {
            for (SyncSkillDataPacket.SkillData entry : entries) {
                SkillType type = resolve(entry == null ? null : entry.skillType());
                if (type == null) {
                    continue;
                }
                boolean unlocked = entry.unlocked();
                int level = unlocked ? Math.max(1, Math.min(maxLevel(type), entry.level())) : 0;
                int experience = unlocked ? Math.max(0, entry.experience()) : 0;
                int proficiency = unlocked ? Math.max(0, Math.min(10000, entry.proficiency())) : 0;
                normalized.put(type, new SkillSnapshot(unlocked, level, experience, proficiency));
            }
        }
        skills = Collections.unmodifiableMap(normalized);
        synced = true;
    }

    public static SkillSnapshot get(SkillType type) {
        return type == null ? SkillSnapshot.locked() : skills.getOrDefault(type, SkillSnapshot.locked());
    }

    public static Map<SkillType, SkillSnapshot> all() {
        return skills;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static void reset() {
        skills = emptySkills();
        synced = false;
    }

    private static SkillType resolve(String skillType) {
        if (skillType == null || skillType.isBlank()) {
            return null;
        }
        try {
            return SkillType.valueOf(skillType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int maxLevel(SkillType type) {
        return switch (type.getCategory()) {
            case CULTIVATION_METHOD, CRAFTING -> 10;
            case SPELL -> 9;
            case SPECIAL -> 5;
        };
    }

    private static Map<SkillType, SkillSnapshot> emptySkills() {
        return Collections.unmodifiableMap(mutableEmptySkills());
    }

    private static EnumMap<SkillType, SkillSnapshot> mutableEmptySkills() {
        EnumMap<SkillType, SkillSnapshot> values = new EnumMap<>(SkillType.class);
        for (SkillType type : SkillType.values()) {
            values.put(type, SkillSnapshot.locked());
        }
        return values;
    }

    public record SkillSnapshot(boolean unlocked, int level, int experience, int proficiency) {
        public static SkillSnapshot locked() {
            return new SkillSnapshot(false, 0, 0, 0);
        }
    }
}
