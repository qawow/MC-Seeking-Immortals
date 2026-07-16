package com.xunxian.seekingimmortals.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import java.util.List;

/**
 * M07 public facade for downstream modules (M04/M06/M08/M09/M13/M14).
 * Conservative, stable signatures over MultiblockStationService + FormationFieldService.
 */
public final class FormationApi {
    private FormationApi() {}

    /**
     * Multiblock workstation / station formed check.
     * stationId uses multiblock structure index ids.
     */
    public static boolean isStationFormed(LevelReader level, String stationId, BlockPos pos) {
        return MultiblockStationService.isStationFormed(level, stationId, pos);
    }

    /**
     * Active formation field effects covering pos (server-authoritative).
     */
    public static List<FormationFieldService.FieldEffect> getActiveFieldEffects(Level level, BlockPos pos) {
        return FormationFieldService.getActiveFieldEffects(level, pos);
    }

    public static int structureIndexSize() {
        return MultiblockStructureCatalog.builtin().size();
    }

    public static int formationFieldCatalogSize() {
        return FormationFieldCatalog.builtin().size();
    }

    public static int formationItemBehaviorSize() {
        return FormationItemService.builtin().size();
    }

    public static int mpSequenceDisplaySize() {
        return MultiblockSequenceDisplayCatalog.builtin().size();
    }
}
