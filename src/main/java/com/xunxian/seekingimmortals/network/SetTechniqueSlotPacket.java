package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetTechniqueSlotPacket(int slot, String techniqueId) {
    public static void encode(SetTechniqueSlotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
        buffer.writeUtf(packet.techniqueId == null ? "" : packet.techniqueId);
    }

    public static SetTechniqueSlotPacket decode(FriendlyByteBuf buffer) {
        return new SetTechniqueSlotPacket(buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(SetTechniqueSlotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (packet.slot < 0 || packet.slot >= PlayerCultivation.TECHNIQUE_SLOT_COUNT) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.invalid_slot"), true);
                return;
            }
            CultivationHelper.get(player).ifPresent(cultivation -> {
                String techniqueId = packet.techniqueId == null ? "" : packet.techniqueId.trim();
                if (!techniqueId.isBlank() && !cultivation.hasLearnedTechnique(techniqueId)) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.not_learned"), true);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    return;
                }
                if (cultivation.setTechniqueSlot(packet.slot, techniqueId)) {
                    player.displayClientMessage(Component.translatable(
                            techniqueId.isBlank() ? "message.seeking_immortals.technique_slot.cleared" : "message.seeking_immortals.technique_slot.bound",
                            packet.slot + 1), true);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                }
            });
        });
        context.setPacketHandled(true);
    }
}
