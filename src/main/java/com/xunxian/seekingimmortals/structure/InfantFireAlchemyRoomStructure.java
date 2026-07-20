package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 婴火炼丹室结构验证（7×7×5 大型炼丹室）。
 * 结构：外围基座 + 内部炉台 + 四周火柱 + 顶部聚火阵。
 */
public final class InfantFireAlchemyRoomStructure {
    public static final int BASE_RADIUS = 3;
    public static final int ROOM_HEIGHT = 5;

    private static final List<BlockPos> BASE_OFFSETS = buildBaseOffsets();
    private static final List<BlockPos> FIRE_PILLAR_OFFSETS = buildFirePillarOffsets();
    private static final List<BlockPos> FURNACE_OFFSETS = buildFurnaceOffsets();
    private static final List<BlockPos> CEILING_OFFSETS = buildCeilingOffsets();

    private InfantFireAlchemyRoomStructure() {}

    public static CheckResult validate(Level level, BlockPos center, Block baseBlock, Block pillarBlock, Block furnaceBlock) {
        int missingBase = 0;
        for (BlockPos offset : BASE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingBase++;
            }
        }

        int missingPillars = 0;
        for (BlockPos offset : FIRE_PILLAR_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(pillarBlock)) {
                missingPillars++;
            }
        }

        int missingFurnace = 0;
        for (BlockPos offset : FURNACE_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(furnaceBlock)) {
                missingFurnace++;
            }
        }

        int missingCeiling = 0;
        for (BlockPos offset : CEILING_OFFSETS) {
            if (!level.getBlockState(center.offset(offset)).is(baseBlock)) {
                missingCeiling++;
            }
        }

        return new CheckResult(missingBase, missingPillars, missingFurnace, missingCeiling);
    }

    private static List<BlockPos> buildBaseOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 7×7 基座地面
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                offsets.add(new BlockPos(x, 0, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildFirePillarOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 四角火柱 + 四边中点火柱
        int[] corners = { -BASE_RADIUS, BASE_RADIUS };
        for (int y = 1; y <= 3; y++) {
            // 四角
            for (int x : corners) {
                for (int z : corners) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
            // 四边中点
            offsets.add(new BlockPos(0, y, -BASE_RADIUS));
            offsets.add(new BlockPos(0, y, BASE_RADIUS));
            offsets.add(new BlockPos(-BASE_RADIUS, y, 0));
            offsets.add(new BlockPos(BASE_RADIUS, y, 0));
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildFurnaceOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 中心 3×3 炉台区域
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(x, 1, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> buildCeilingOffsets() {
        List<BlockPos> offsets = new ArrayList<>();
        // 顶部聚火阵：5×5 天花板
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                offsets.add(new BlockPos(x, ROOM_HEIGHT - 1, z));
            }
        }
        return List.copyOf(offsets);
    }

    public record CheckResult(int missingBase, int missingPillars, int missingFurnace, int missingCeiling) {
        public boolean complete() {
            return missingBase <= 0 && missingPillars <= 0 && missingFurnace <= 0 && missingCeiling <= 0;
        }
    }
}
