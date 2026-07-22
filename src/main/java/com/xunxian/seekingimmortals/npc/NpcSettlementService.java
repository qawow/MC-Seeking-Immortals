package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.entity.SpiritStoneBankerEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.worldpack.WorldpackDataService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import com.xunxian.seekingimmortals.worldpack.WorldpackSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Places the small set of service NPCs that have authored world anchors. */
public final class NpcSettlementService {
    private static final double BANKER_SEARCH_RADIUS = 16.0D;

    private NpcSettlementService() {}

    public static boolean ensureStarterHub(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return false;
        }
        ServerLevel overworld = player.getServer().overworld();
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(overworld);
        savedData.ensureStarterAnchor(overworld, snapshot);
        String anchorId = snapshot.findRegion(WorldpackGameplayService.DEFAULT_REGION_ID)
                .map(WorldpackDataService.RegionCard::travelAnchor)
                .orElse("");
        WorldpackSavedData.Anchor anchor = savedData.getAnchor(anchorId).orElse(null);
        if (anchor == null || !overworld.dimension().location().toString().equals(anchor.dimension())) {
            return false;
        }

        BlockPos column = BlockPos.containing(anchor.x() + 3.0D, anchor.y(), anchor.z() + 1.0D);
        BlockPos spawnPos = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        overworld.getChunkAt(spawnPos);
        AABB search = new AABB(spawnPos).inflate(BANKER_SEARCH_RADIUS, 8.0D, BANKER_SEARCH_RADIUS);
        if (!overworld.getEntitiesOfClass(SpiritStoneBankerEntity.class, search,
                SpiritStoneBankerEntity::isAlive).isEmpty()) {
            return true;
        }

        SpiritStoneBankerEntity banker = ModEntities.SPIRIT_STONE_BANKER.get().create(overworld);
        if (banker == null) {
            return false;
        }
        banker.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                anchor.yRot(), 0.0F);
        banker.setCustomName(Component.translatable("entity.seeking_immortals.spirit_stone_banker"));
        banker.setCustomNameVisible(true);
        if (!overworld.noCollision(banker)) {
            banker.moveTo(banker.getX(), banker.getY() + 1.0D, banker.getZ(), banker.getYRot(), 0.0F);
        }
        return overworld.noCollision(banker) && overworld.addFreshEntity(banker);
    }
}
