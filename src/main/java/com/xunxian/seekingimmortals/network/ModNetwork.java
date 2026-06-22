package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "5";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SeekingImmortalsMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SetMeditatingPacket.class, id++)
                .encoder(SetMeditatingPacket::encode)
                .decoder(SetMeditatingPacket::decode)
                .consumerMainThread(SetMeditatingPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncLearnedTechniquesPacket.class, id++)
                .encoder(SyncLearnedTechniquesPacket::encode)
                .decoder(SyncLearnedTechniquesPacket::decode)
                .consumerMainThread(SyncLearnedTechniquesPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncCultivationDataPacket.class, id++)
                .encoder(SyncCultivationDataPacket::encode)
                .decoder(SyncCultivationDataPacket::decode)
                .consumerMainThread(SyncCultivationDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReleaseTechniquePacket.class, id++)
                .encoder(ReleaseTechniquePacket::encode)
                .decoder(ReleaseTechniquePacket::decode)
                .consumerMainThread(ReleaseTechniquePacket::handle)
                .add();
        CHANNEL.messageBuilder(SetTechniqueSlotPacket.class, id++)
                .encoder(SetTechniqueSlotPacket::encode)
                .decoder(SetTechniqueSlotPacket::decode)
                .consumerMainThread(SetTechniqueSlotPacket::handle)
                .add();
        CHANNEL.messageBuilder(AttemptBreakthroughPacket.class, id++)
                .encoder(AttemptBreakthroughPacket::encode)
                .decoder(AttemptBreakthroughPacket::decode)
                .consumerMainThread(AttemptBreakthroughPacket::handle)
                .add();
    }
}
