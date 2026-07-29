package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    // 0.2.243: remove the unreachable legacy auction screen packet.
    private static final String PROTOCOL_VERSION = "31";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SeekingImmortalsMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {}

    /** Exposed for live-smoke / multiplayer protocol match probes. */
    public static String protocolVersion() {
        return PROTOCOL_VERSION;
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SetMeditatingPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetMeditatingPacket::encode)
                .decoder(SetMeditatingPacket::decode)
                .consumerMainThread(SetMeditatingPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncLearnedTechniquesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLearnedTechniquesPacket::encode)
                .decoder(SyncLearnedTechniquesPacket::decode)
                .consumerMainThread(SyncLearnedTechniquesPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncCultivationDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCultivationDataPacket::encode)
                .decoder(SyncCultivationDataPacket::decode)
                .consumerMainThread(SyncCultivationDataPacket::handle)
                .add();
        // Wave477: learned cultivation methods (protocol 14).
        CHANNEL.messageBuilder(SyncLearnedMethodsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLearnedMethodsPacket::encode)
                .decoder(SyncLearnedMethodsPacket::decode)
                .consumerMainThread(SyncLearnedMethodsPacket::handle)
                .add();
        CHANNEL.messageBuilder(ReleaseTechniquePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReleaseTechniquePacket::encode)
                .decoder(ReleaseTechniquePacket::decode)
                .consumerMainThread(ReleaseTechniquePacket::handle)
                .add();
        CHANNEL.messageBuilder(SetTechniqueSlotPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetTechniqueSlotPacket::encode)
                .decoder(SetTechniqueSlotPacket::decode)
                .consumerMainThread(SetTechniqueSlotPacket::handle)
                .add();
        CHANNEL.messageBuilder(AttemptBreakthroughPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AttemptBreakthroughPacket::encode)
                .decoder(AttemptBreakthroughPacket::decode)
                .consumerMainThread(AttemptBreakthroughPacket::handle)
                .add();
        CHANNEL.messageBuilder(SetMovementSpeedScalePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetMovementSpeedScalePacket::encode)
                .decoder(SetMovementSpeedScalePacket::decode)
                .consumerMainThread(SetMovementSpeedScalePacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSectDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSectDataPacket::encode)
                .decoder(SyncSectDataPacket::decode)
                .consumerMainThread(SyncSectDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(SectActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SectActionPacket::encode)
                .decoder(SectActionPacket::decode)
                .consumerMainThread(SectActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShopDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncShopDataPacket::encode)
                .decoder(SyncShopDataPacket::decode)
                .consumerMainThread(SyncShopDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShopActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShopActionPacket::encode)
                .decoder(ShopActionPacket::decode)
                .consumerMainThread(ShopActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncWorldpackDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncWorldpackDataPacket::encode)
                .decoder(SyncWorldpackDataPacket::decode)
                .consumerMainThread(SyncWorldpackDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(WorldpackActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WorldpackActionPacket::encode)
                .decoder(WorldpackActionPacket::decode)
                .consumerMainThread(WorldpackActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(AuctionActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AuctionActionPacket::encode)
                .decoder(AuctionActionPacket::decode)
                .consumerMainThread(AuctionActionPacket::handle)
                .add();
        // Wave47: dialogue GUI packets.
        CHANNEL.messageBuilder(OpenDialogueScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDialogueScreenPacket::encode)
                .decoder(OpenDialogueScreenPacket::decode)
                .consumerMainThread(OpenDialogueScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(DialogueActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueActionPacket::encode)
                .decoder(DialogueActionPacket::decode)
                .consumerMainThread(DialogueActionPacket::handle)
                .add();
        // Wave49: quest tracker + shop rank lock wire changes (protocol 12).
        CHANNEL.messageBuilder(SyncQuestTrackerPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQuestTrackerPacket::encode)
                .decoder(SyncQuestTrackerPacket::decode)
                .consumerMainThread(SyncQuestTrackerPacket::handle)
                .add();
        CHANNEL.messageBuilder(QuestTrackerActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(QuestTrackerActionPacket::encode)
                .decoder(QuestTrackerActionPacket::decode)
                .consumerMainThread(QuestTrackerActionPacket::handle)
                .add();
        // Wave50: alchemy/storage/refine GUI open packets.
        CHANNEL.messageBuilder(OpenAlchemyStatusPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAlchemyStatusPacket::encode)
                .decoder(OpenAlchemyStatusPacket::decode)
                .consumerMainThread(OpenAlchemyStatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenStoragePreviewPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenStoragePreviewPacket::encode)
                .decoder(OpenStoragePreviewPacket::decode)
                .consumerMainThread(OpenStoragePreviewPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenRefinePlanPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenRefinePlanPacket::encode)
                .decoder(OpenRefinePlanPacket::decode)
                .consumerMainThread(OpenRefinePlanPacket::handle)
                .add();
        // Wave478: method-tree learn/sync intent (protocol 15) — appended after existing packets.
        CHANNEL.messageBuilder(MethodActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MethodActionPacket::encode)
                .decoder(MethodActionPacket::decode)
                .consumerMainThread(MethodActionPacket::handle)
                .add();
        // Wave486: freeform method-tree layout (protocol 17).
        CHANNEL.messageBuilder(SyncMethodLayoutPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMethodLayoutPacket::encode)
                .decoder(SyncMethodLayoutPacket::decode)
                .consumerMainThread(SyncMethodLayoutPacket::handle)
                .add();
        CHANNEL.messageBuilder(MethodLayoutActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MethodLayoutActionPacket::encode)
                .decoder(MethodLayoutActionPacket::decode)
                .consumerMainThread(MethodLayoutActionPacket::handle)
                .add();
        // Wave491 protocol 18: auction live ladder + skill tree actions.
        CHANNEL.messageBuilder(SyncAuctionLadderPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAuctionLadderPacket::encode)
                .decoder(SyncAuctionLadderPacket::decode)
                .consumerMainThread(SyncAuctionLadderPacket::handle)
                .add();
        CHANNEL.messageBuilder(SkillTreeActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SkillTreeActionPacket::encode)
                .decoder(SkillTreeActionPacket::decode)
                .consumerMainThread(SkillTreeActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSkillDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillDataPacket::encode)
                .decoder(SyncSkillDataPacket::decode)
                .consumerMainThread(SyncSkillDataPacket::handle)
                .add();
        // M16: lore unlock sync + encyclopedia open intent (appended; no existing packet field change).
        CHANNEL.messageBuilder(SyncLoreUnlockPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLoreUnlockPacket::encode)
                .decoder(SyncLoreUnlockPacket::decode)
                .consumerMainThread(SyncLoreUnlockPacket::handle)
                .add();
        CHANNEL.messageBuilder(LoreScreenActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LoreScreenActionPacket::encode)
                .decoder(LoreScreenActionPacket::decode)
                .consumerMainThread(LoreScreenActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(TechniqueVfxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TechniqueVfxPacket::encode)
                .decoder(TechniqueVfxPacket::decode)
                .consumerMainThread(TechniqueVfxPacket::handle)
                .add();
        // Lifecycle transport is appended so every pre-existing packet id remains stable.
        CHANNEL.messageBuilder(VisualEventPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VisualEventPacket::encode)
                .decoder(VisualEventPacket::decode)
                .consumerMainThread(VisualEventPacket::handle)
                .add();
    }
}
