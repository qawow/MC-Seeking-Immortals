package com.xunxian.seekingimmortals.beast;

import java.util.ArrayList;
import java.util.List;

/** Shared, registry-free progression math for contracted beasts and crafted puppets. */
public final class CompanionGrowthService {
    public static final int MAX_LEVEL = 20;
    private static final int BASE_EXPERIENCE = 20;
    private static final int EXPERIENCE_PER_LEVEL = 10;

    private CompanionGrowthService() {}

    public record Progress(int level, int experience, int evolutionStage) {
        public Progress {
            level = Math.max(0, Math.min(MAX_LEVEL, level));
            experience = level >= MAX_LEVEL ? 0 : Math.max(0, experience);
            evolutionStage = Math.max(0, evolutionStage);
        }
    }

    public record Update(Progress progress, int levelsGained, int evolutionsGained,
                         boolean evolutionBlocked) {}

    public static int experienceToNextLevel(int level) {
        int clean = Math.max(0, Math.min(MAX_LEVEL - 1, level));
        return BASE_EXPERIENCE + clean * EXPERIENCE_PER_LEVEL;
    }

    /** Maps authored stage count across levels 0..20, preserving the final stage as a real capstone. */
    public static List<Integer> evolutionThresholds(int stageCount) {
        int stages = Math.max(1, Math.min(MAX_LEVEL + 1, stageCount));
        if (stages <= 1) {
            return List.of();
        }
        List<Integer> thresholds = new ArrayList<>();
        for (int stage = 1; stage < stages; stage++) {
            int threshold = (int) Math.ceil((double) MAX_LEVEL * stage / (stages - 1));
            if (thresholds.isEmpty() || thresholds.get(thresholds.size() - 1) != threshold) {
                thresholds.add(threshold);
            }
        }
        return List.copyOf(thresholds);
    }

    public static Progress legacyProgress(int oldGrowth, int stageCount) {
        int level = Math.max(0, Math.min(MAX_LEVEL, oldGrowth));
        int evolution = 0;
        for (int threshold : evolutionThresholds(stageCount)) {
            if (level < threshold) {
                break;
            }
            evolution++;
        }
        return new Progress(level, 0, evolution);
    }

    public static Update grant(Progress current, int gainedExperience, int stageCount,
                               boolean evolutionStationReady) {
        Progress safe = current == null ? new Progress(0, 0, 0) : current;
        int level = safe.level();
        int experience = safe.experience() + Math.max(0, gainedExperience);
        int evolution = Math.min(safe.evolutionStage(), Math.max(0, stageCount - 1));
        int levelsGained = 0;
        int evolutionsGained = 0;
        boolean blocked = false;
        List<Integer> thresholds = evolutionThresholds(stageCount);

        while (level < MAX_LEVEL) {
            int needed = experienceToNextLevel(level);
            if (experience < needed) {
                break;
            }
            int nextLevel = level + 1;
            int requiredEvolution = thresholds.indexOf(nextLevel) + 1;
            if (requiredEvolution > evolution && !evolutionStationReady) {
                experience = needed;
                blocked = true;
                break;
            }
            experience -= needed;
            level = nextLevel;
            levelsGained++;
            if (requiredEvolution > evolution) {
                evolutionsGained += requiredEvolution - evolution;
                evolution = requiredEvolution;
            }
        }
        return new Update(new Progress(level, experience, evolution), levelsGained,
                evolutionsGained, blocked);
    }

    public static double statMultiplier(Progress progress) {
        Progress safe = progress == null ? new Progress(0, 0, 0) : progress;
        return 1.0D + safe.level() * 0.04D + safe.evolutionStage() * 0.18D;
    }
}
