package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ReleaseTechniquePacket(int slot) {
    private static final int SLOT_COUNT = 7;
    private static final int DEFAULT_COOLDOWN_TICKS = 5 * 20;

    public static void encode(ReleaseTechniquePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
    }

    public static ReleaseTechniquePacket decode(FriendlyByteBuf buffer) {
        return new ReleaseTechniquePacket(buffer.readVarInt());
    }

    public static void handle(ReleaseTechniquePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (packet.slot < 0 || packet.slot >= SLOT_COUNT) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.invalid_slot"), true);
                return;
            }

            CultivationHelper.get(player).ifPresent(cultivation -> {
                String techniqueId = cultivation.getTechniqueSlot(packet.slot);
                if (techniqueId.isBlank()) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.empty_slot", packet.slot + 1), true);
                    return;
                }

                if (!cultivation.hasLearnedTechnique(techniqueId)) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.not_learned"), true);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    return;
                }

                long gameTime = player.serverLevel().getGameTime();
                long cooldownUntilTick = cultivation.getTechniqueCooldownUntilTick(techniqueId);
                if (cooldownUntilTick > gameTime) {
                    int remainingSeconds = (int)Math.ceil((cooldownUntilTick - gameTime) / 20.0D);
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.cooldown", remainingSeconds), true);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    return;
                }

                int cost = estimateCost(player, techniqueId);
                if (!cultivation.consumeSpiritualPower(cost)) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                    SyncCultivationDataPacket.send(player, cultivation);
                    return;
                }

                cultivation.setTechniqueCooldown(techniqueId, gameTime + DEFAULT_COOLDOWN_TICKS);
                SyncCultivationDataPacket.send(player, cultivation);
                SyncLearnedTechniquesPacket.send(player, cultivation);
                TechniqueDataManager.getTechnique(player.getServer(), techniqueId).ifPresentOrElse(technique ->
                                player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.success",
                                        packet.slot + 1,
                                        technique.name().isBlank() ? technique.id() : technique.name(),
                                        cost), false),
                        () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.success",
                                packet.slot + 1,
                                techniqueId,
                                cost), false));
            });
        });
        context.setPacketHandled(true);
    }

    private static int estimateCost(ServerPlayer player, String techniqueId) {
        return TechniqueDataManager.getTechnique(player.getServer(), techniqueId)
                .map(technique -> {
                    String text = (technique.id() + " " + technique.source() + " " + technique.attribute()).toLowerCase(java.util.Locale.ROOT);
                    if (text.contains("formation") || text.contains("sword") || text.contains("阵") || text.contains("剑")) return 35;
                    if (text.contains("secret") || text.contains("divine") || text.contains("秘") || text.contains("神通")) return 30;
                    if (text.contains("talisman") || text.contains("符")) return 12;
                    if (text.contains("utility") || text.contains("通用")) return 8;
                    return 15;
                })
                .orElse(15);
    }
}
