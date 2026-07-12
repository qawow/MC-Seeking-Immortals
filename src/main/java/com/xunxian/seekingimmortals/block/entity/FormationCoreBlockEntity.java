package com.xunxian.seekingimmortals.block.entity;

import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent formation core BE (Wave54).
 * Stores kind + remaining ticks and rehydrates FormationFieldService on load/tick.
 */
public class FormationCoreBlockEntity extends BlockEntity {
    private String kind = FormationFieldService.FieldKind.CATALOG_GENERIC.name();
    private int remainingTicks;
    private boolean freeField;

    public FormationCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORMATION_CORE.get(), pos, state);
    }

    public void activate(FormationFieldService.FieldKind fieldKind, int durationTicks, boolean free) {
        this.kind = fieldKind.name();
        this.remainingTicks = Math.max(20, durationTicks);
        this.freeField = free;
        setChanged();
        if (level != null && !level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (free) {
                FormationFieldService.activateFreeField(serverLevel, worldPosition, fieldKind, remainingTicks);
            } else {
                FormationFieldService.activate(serverLevel, worldPosition, fieldKind);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FormationCoreBlockEntity be) {
        if (level.isClientSide || be.remainingTicks <= 0) {
            return;
        }
        be.remainingTicks--;
        if (be.remainingTicks % 20 == 0) {
            be.setChanged();
        }
        if (be.remainingTicks <= 0) {
            be.setChanged();
        } else if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && be.remainingTicks % 100 == 0) {
            // periodic rehydrate in case memory map was cleared
            try {
                FormationFieldService.FieldKind fieldKind = FormationFieldService.FieldKind.valueOf(be.kind);
                if (be.freeField) {
                    FormationFieldService.activateFreeField(serverLevel, pos, fieldKind, be.remainingTicks);
                } else {
                    FormationFieldService.activate(serverLevel, pos, fieldKind);
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Kind", kind);
        tag.putInt("Ticks", remainingTicks);
        tag.putBoolean("Free", freeField);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        kind = tag.getString("Kind");
        if (kind == null || kind.isBlank()) {
            kind = FormationFieldService.FieldKind.CATALOG_GENERIC.name();
        }
        remainingTicks = tag.getInt("Ticks");
        freeField = tag.getBoolean("Free");
    }
}
