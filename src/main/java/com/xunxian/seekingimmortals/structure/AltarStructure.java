package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 祭坛结构验证（适用于兽契坛、魂幡祭坛、血色禁地试炼坛等祭祀类结构）。
 * 结构：3×3 基座 + 中心祭台 + 四周供台。
 */
public final class AltarStructure {
    public static final int BASE_RADIUS = 1;
    public static final int ALTAR_HEIGHT = 1;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> OFFERING_OFFSETS = buildOfferingOffsets();

    private AltarStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block offeringBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        // 中心祭台
        if (!level.getBlockState(center.above(ALTAR_HEIGHT)).is(baseBlock)) {
            missingBase++;
        }

        int missingOffering = 0;
        for (BlockPos offset : OFFERING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(offeringBlock)) {
                missingOffering++;
            }
        }
        return new CheckResult(missingBase, missingOffering);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildOfferingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四个方向的供台
        offsets.add(new BlockPos(-BASE_RADIUS, ALTAR_HEIGHT, 0));
        offsets.add(new BlockPos(BASE_RADIUS, ALTAR_HEIGHT, 0));
        offsets.add(new BlockPos(0, ALTAR_HEIGHT, -BASE_RADIUS));
        offsets.add(new BlockPos(0, ALTAR_HEIGHT, BASE_RADIUS));
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingOffering) {
        public boolean complete() {
            return missingBase <= 0 && missingOffering <= 0;
        }
    }
}
