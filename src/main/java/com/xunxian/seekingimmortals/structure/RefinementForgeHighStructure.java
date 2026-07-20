package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * High-tier refinement forge arrays (G4–G6).
 * Ring radius = grade-1 (G4:3, G5:4, G6:5), corner frame pillars, clear aperture above center.
 */
public final class RefinementForgeHighStructure {
    private RefinementForgeHighStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block forgeBlock, Block frameBlock, int grade) {
        int ringRadius = Math.max(2, Math.min(6, grade - 1));
        int apertureRadius = Math.max(1, ringRadius - 1);
        int apertureHeight = Math.max(3, grade);
        int frameHeight = Math.max(3, grade);
        int missingRing = 0;
        for (BlockPos offset : ringOffsets(ringRadius)) {
            if (!level.getBlockState(center.offset(offset)).is(forgeBlock)) {
                missingRing++;
            }
        }
        int missingFrame = 0;
        for (BlockPos offset : frameOffsets(ringRadius, frameHeight)) {
            if (!level.getBlockState(center.offset(offset)).is(frameBlock)) {
                missingFrame++;
            }
        }
        int blockedAperture = 0;
        for (BlockPos offset : apertureOffsets(apertureRadius, apertureHeight)) {
            if (!level.getBlockState(center.offset(offset)).isAir()) {
                blockedAperture++;
            }
        }
        return new CheckResult(missingRing, missingFrame, blockedAperture, grade);
    }

    public static List<BlockPos> ringOffsets(int ringRadius) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -ringRadius; x <= ringRadius; x++) {
            for (int z = -ringRadius; z <= ringRadius; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                if (Math.abs(x) == ringRadius || Math.abs(z) == ringRadius) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public static List<BlockPos> frameOffsets(int ringRadius, int frameHeight) {
        List<BlockPos> offsets = new ArrayList<>();
        int[] corners = {-ringRadius, ringRadius};
        for (int y = 1; y <= frameHeight; y++) {
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public static List<BlockPos> apertureOffsets(int apertureRadius, int apertureHeight) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int y = 1; y <= apertureHeight; y++) {
            for (int x = -apertureRadius; x <= apertureRadius; x++) {
                for (int z = -apertureRadius; z <= apertureRadius; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingRing, int missingFrame, int blockedAperture, int grade) {
        public boolean complete() {
            return missingRing <= 0 && missingFrame <= 0 && blockedAperture <= 0;
        }
    }
}
