package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record SyncLearnedTechniquesPacket(List<String> learnedTechniques, List<String> techniqueSlots, Map<String, Integer> cooldownRemainingTicks) {
    public static SyncLearnedTechniquesPacket from(ServerPlayer player, PlayerCultivation cultivation) {
        long gameTime = player.serverLevel().getGameTime();
        Map<String, Integer> remainingTicks = new HashMap<>();
        cultivation.getTechniqueCooldownUntilTicks().forEach((techniqueId, untilTick) -> {
            int remaining = (int)Math.max(0L, untilTick - gameTime);
            if (remaining > 0) {
                remainingTicks.put(techniqueId, remaining);
            }
        });
        return new SyncLearnedTechniquesPacket(
                cultivation.getLearnedTechniques().stream().sorted().toList(),
                cultivation.getTechniqueSlots(),
                Map.copyOf(remainingTicks));
    }

    public static void send(ServerPlayer player, PlayerCultivation cultivation) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(player, cultivation));
    }

    public static void encode(SyncLearnedTechniquesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.learnedTechniques.size());
        for (String techniqueId : packet.learnedTechniques) {
            buffer.writeUtf(techniqueId);
        }
        buffer.writeVarInt(packet.techniqueSlots.size());
        for (String techniqueId : packet.techniqueSlots) {
            buffer.writeUtf(techniqueId == null ? "" : techniqueId);
        }
        buffer.writeVarInt(packet.cooldownRemainingTicks.size());
        packet.cooldownRemainingTicks.forEach((techniqueId, remainingTicks) -> {
            buffer.writeUtf(techniqueId);
            buffer.writeVarInt(Math.max(0, remainingTicks));
        });
    }

    public static SyncLearnedTechniquesPacket decode(FriendlyByteBuf buffer) {
        int learnedSize = buffer.readVarInt();
        List<String> techniques = new ArrayList<>();
        for (int i = 0; i < learnedSize; i++) {
            techniques.add(buffer.readUtf());
        }
        int slotSize = buffer.readVarInt();
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < slotSize; i++) {
            slots.add(buffer.readUtf());
        }
        int cooldownSize = buffer.readVarInt();
        Map<String, Integer> cooldowns = new HashMap<>();
        for (int i = 0; i < cooldownSize; i++) {
            String techniqueId = buffer.readUtf();
            int remainingTicks = buffer.readVarInt();
            if (!techniqueId.isBlank() && remainingTicks > 0) {
                cooldowns.put(techniqueId, remainingTicks);
            }
        }
        return new SyncLearnedTechniquesPacket(List.copyOf(techniques), List.copyOf(slots), Map.copyOf(cooldowns));
    }

    public static void handle(SyncLearnedTechniquesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.xunxian.seekingimmortals.client.ClientTechniqueData.setTechniqueData(packet.learnedTechniques, packet.techniqueSlots, packet.cooldownRemainingTicks)));
        context.setPacketHandled(true);
    }
}
