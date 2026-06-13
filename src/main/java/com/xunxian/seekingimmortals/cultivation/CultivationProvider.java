package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

@AutoRegisterCapability
public class CultivationProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<PlayerCultivation> CULTIVATION = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(SeekingImmortalsMod.MODID, "cultivation");

    private final PlayerCultivation cultivation = new PlayerCultivation();
    private final LazyOptional<PlayerCultivation> optional = LazyOptional.of(() -> cultivation);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CULTIVATION ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() { return cultivation.saveNBTData(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { cultivation.loadNBTData(nbt); }
}
