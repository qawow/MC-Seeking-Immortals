package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 机关廊道结构验证（适用于虚天殿机关廊 3×15×3 等长廊类结构）。
 * 结构：长廊地板 + 双侧墙壁 + 机关触发点 + 顶部陷阱。
 */
public final class TrapCorridorStructure {
    public static final int CORRIDOR_WIDTH = 3;
    public static final int CORRIDOR_LENGTH = 15;
    public static final int CORRIDOR_HEIGHT = 3;

    private TrapCorridorStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block floorBlock, Block wallBlock, Block trapBlock) {
        int missingFloor = 0;
        int missingWalls = 0;
        int missingTraps = 0;

        // 验证地板（3×15）
        for (int z = 0; z < CORRIDOR_LENGTH; z++) {
            for (int x = -1; x <= 1; x++) {
                BlockPos pos = center.offset(x, 0, z);
                if (!level.getBlockState(pos).is(floorBlock)) {
                    missingFloor++;
                }
            }
        }

        // 验证墙壁（双侧）
        for (int z = 0; z < CORRIDOR_LENGTH; z++) {
            for (int y = 1; y <= 2; y++) {
                // 左墙
                if (!level.getBlockState(center.offset(-1, y, z)).is(wallBlock)) {
                    missingWalls++;
                }
                // 右墙
                if (!level.getBlockState(center.offset(1, y, z)).is(wallBlock)) {
                    missingWalls++;
                }
            }
        }

        // 验证机关陷阱（每隔3格一个，顶部）
        for (int z = 2; z < CORRIDOR_LENGTH; z += 3) {
            if (!level.getBlockState(center.offset(0, CORRIDOR_HEIGHT - 1, z)).is(trapBlock)) {
                missingTraps++;
            }
        }

        return new CheckResult(missingFloor, missingWalls, missingTraps);
    }

    public record CheckResult(int missingFloor, int missingWalls, int missingTraps) {
        public boolean complete() {
            return missingFloor <= 0 && missingWalls <= 0 && missingTraps <= 0;
        }
    }
}
