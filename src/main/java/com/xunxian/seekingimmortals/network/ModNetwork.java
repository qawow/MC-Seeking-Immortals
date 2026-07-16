package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    // Wave491: auction live ladder + skill tree action packets.
    private static final String PROTOCOL_VERSION = "22";

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
        // Wave477: learned cultivation methods (protocol 14).
        CHANNEL.messageBuilder(SyncLearnedMethodsPacket.class, id++)
                .encoder(SyncLearnedMethodsPacket::encode)
                .decoder(SyncLearnedMethodsPacket::decode)
                .consumerMainThread(SyncLearnedMethodsPacket::handle)
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
        CHANNEL.messageBuilder(SetMovementSpeedScalePacket.class, id++)
                .encoder(SetMovementSpeedScalePacket::encode)
                .decoder(SetMovementSpeedScalePacket::decode)
                .consumerMainThread(SetMovementSpeedScalePacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSectDataPacket.class, id++)
                .encoder(SyncSectDataPacket::encode)
                .decoder(SyncSectDataPacket::decode)
                .consumerMainThread(SyncSectDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(SectActionPacket.class, id++)
                .encoder(SectActionPacket::encode)
                .decoder(SectActionPacket::decode)
                .consumerMainThread(SectActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncShopDataPacket.class, id++)
                .encoder(SyncShopDataPacket::encode)
                .decoder(SyncShopDataPacket::decode)
                .consumerMainThread(SyncShopDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShopActionPacket.class, id++)
                .encoder(ShopActionPacket::encode)
                .decoder(ShopActionPacket::decode)
                .consumerMainThread(ShopActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncWorldpackDataPacket.class, id++)
                .encoder(SyncWorldpackDataPacket::encode)
                .decoder(SyncWorldpackDataPacket::decode)
                .consumerMainThread(SyncWorldpackDataPacket::handle)
                .add();
        CHANNEL.messageBuilder(WorldpackActionPacket.class, id++)
                .encoder(WorldpackActionPacket::encode)
                .decoder(WorldpackActionPacket::decode)
                .consumerMainThread(WorldpackActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(AuctionActionPacket.class, id++)
                .encoder(AuctionActionPacket::encode)
                .decoder(AuctionActionPacket::decode)
                .consumerMainThread(AuctionActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenAuctionScreenPacket.class, id++)
                .encoder(OpenAuctionScreenPacket::encode)
                .decoder(OpenAuctionScreenPacket::decode)
                .consumerMainThread(OpenAuctionScreenPacket::handle)
                .add();
        // Wave47: dialogue GUI packets.
        CHANNEL.messageBuilder(OpenDialogueScreenPacket.class, id++)
                .encoder(OpenDialogueScreenPacket::encode)
                .decoder(OpenDialogueScreenPacket::decode)
                .consumerMainThread(OpenDialogueScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(DialogueActionPacket.class, id++)
                .encoder(DialogueActionPacket::encode)
                .decoder(DialogueActionPacket::decode)
                .consumerMainThread(DialogueActionPacket::handle)
                .add();
        // Wave49: quest tracker + shop rank lock wire changes (protocol 12).
        CHANNEL.messageBuilder(SyncQuestTrackerPacket.class, id++)
                .encoder(SyncQuestTrackerPacket::encode)
                .decoder(SyncQuestTrackerPacket::decode)
                .consumerMainThread(SyncQuestTrackerPacket::handle)
                .add();
        CHANNEL.messageBuilder(QuestTrackerActionPacket.class, id++)
                .encoder(QuestTrackerActionPacket::encode)
                .decoder(QuestTrackerActionPacket::decode)
                .consumerMainThread(QuestTrackerActionPacket::handle)
                .add();
        // Wave50: alchemy/storage/refine GUI open packets.
        CHANNEL.messageBuilder(OpenAlchemyStatusPacket.class, id++)
                .encoder(OpenAlchemyStatusPacket::encode)
                .decoder(OpenAlchemyStatusPacket::decode)
                .consumerMainThread(OpenAlchemyStatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenStoragePreviewPacket.class, id++)
                .encoder(OpenStoragePreviewPacket::encode)
                .decoder(OpenStoragePreviewPacket::decode)
                .consumerMainThread(OpenStoragePreviewPacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenRefinePlanPacket.class, id++)
                .encoder(OpenRefinePlanPacket::encode)
                .decoder(OpenRefinePlanPacket::decode)
                .consumerMainThread(OpenRefinePlanPacket::handle)
                .add();
        // Wave478: method-tree learn/sync intent (protocol 15) — appended after existing packets.
        CHANNEL.messageBuilder(MethodActionPacket.class, id++)
                .encoder(MethodActionPacket::encode)
                .decoder(MethodActionPacket::decode)
                .consumerMainThread(MethodActionPacket::handle)
                .add();
        // Wave486: freeform method-tree layout (protocol 17).
        CHANNEL.messageBuilder(SyncMethodLayoutPacket.class, id++)
                .encoder(SyncMethodLayoutPacket::encode)
                .decoder(SyncMethodLayoutPacket::decode)
                .consumerMainThread(SyncMethodLayoutPacket::handle)
                .add();
        CHANNEL.messageBuilder(MethodLayoutActionPacket.class, id++)
                .encoder(MethodLayoutActionPacket::encode)
                .decoder(MethodLayoutActionPacket::decode)
                .consumerMainThread(MethodLayoutActionPacket::handle)
                .add();
        // Wave491 protocol 18: auction live ladder + skill tree actions.
        CHANNEL.messageBuilder(SyncAuctionLadderPacket.class, id++)
                .encoder(SyncAuctionLadderPacket::encode)
                .decoder(SyncAuctionLadderPacket::decode)
                .consumerMainThread(SyncAuctionLadderPacket::handle)
                .add();
        CHANNEL.messageBuilder(SkillTreeActionPacket.class, id++)
                .encoder(SkillTreeActionPacket::encode)
                .decoder(SkillTreeActionPacket::decode)
                .consumerMainThread(SkillTreeActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSkillDataPacket.class, id++)
                .encoder(SyncSkillDataPacket::encode)
                .decoder(SyncSkillDataPacket::decode)
                .consumerMainThread(SyncSkillDataPacket::handle)
                .add();
    }
}
