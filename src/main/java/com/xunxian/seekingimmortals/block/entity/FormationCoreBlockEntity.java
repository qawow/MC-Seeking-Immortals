package com.xunxian.seekingimmortals.block.entity;

import com.xunxian.seekingimmortals.entity.SyncedVisualIdentity;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent formation core BE (Wave54).
 * Stores legacy core metadata; FormationFieldService and its SavedData remain authoritative.
 * M07: also stores formationId for catalog-aligned field params.
 */
public class FormationCoreBlockEntity extends BlockEntity {
    private static final String DEFAULT_KIND = FormationFieldService.FieldKind.CATALOG_GENERIC.name();
    private String kind = FormationFieldService.FieldKind.CATALOG_GENERIC.name();
    private String formationId = "";
    private int remainingTicks;
    private boolean freeField;

    public FormationCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORMATION_CORE.get(), pos, state);
    }

    public void activate(FormationFieldService.FieldKind fieldKind, int durationTicks, boolean free) {
        activate(fieldKind, durationTicks, free, null);
    }

    public void activate(FormationFieldService.FieldKind fieldKind, int durationTicks, boolean free, String formationId) {
        this.kind = (fieldKind == null ? FormationFieldService.FieldKind.CATALOG_GENERIC : fieldKind).name();
        this.formationId = SyncedVisualIdentity.boundedKey(formationId, "");
        this.remainingTicks = Math.max(20, durationTicks);
        this.freeField = free;
        setChanged();
        syncClient();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FormationCoreBlockEntity be) {
        if (level.isClientSide || be.remainingTicks <= 0) {
            return;
        }
        be.remainingTicks--;
        if (be.remainingTicks % 20 == 0) {
            be.setChanged();
            be.syncClient();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Kind", kind);
        tag.putInt("Ticks", remainingTicks);
        tag.putBoolean("Free", freeField);
        tag.putString("FormationId", formationId == null ? "" : formationId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        kind = tag.getString("Kind");
        if (kind == null || kind.isBlank()) {
            kind = DEFAULT_KIND;
        }
        remainingTicks = Math.max(0, tag.getInt("Ticks"));
        freeField = tag.getBoolean("Free");
        formationId = SyncedVisualIdentity.boundedKey(
                tag.contains("FormationId") ? tag.getString("FormationId") : "", "");
    }

    public boolean isActive() {
        return remainingTicks > 0;
    }

    public int remainingTicks() {
        return Math.max(0, remainingTicks);
    }

    public String kind() {
        return kind;
    }

    public FormationFieldService.FieldKind fieldKind() {
        return SyncedVisualIdentity.byName(
                FormationFieldService.FieldKind.class, kind,
                FormationFieldService.FieldKind.CATALOG_GENERIC);
    }

    public String formationId() {
        return formationId == null ? "" : formationId;
    }

    public String visualProfileId() {
        String rawId = formationId().isBlank()
                ? fieldKind().name().toLowerCase(java.util.Locale.ROOT)
                : formationId();
        return SyncedVisualIdentity.qualified("formation", rawId, "formation:catalog_generic");
    }

    public boolean isFreeField() {
        return freeField;
    }

    private void syncClient() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }
}
