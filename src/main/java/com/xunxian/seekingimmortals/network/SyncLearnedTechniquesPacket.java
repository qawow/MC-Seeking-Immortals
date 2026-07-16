package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import io.netty.handler.codec.DecoderException;
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
    public static final int MAX_TECHNIQUE_ID_LENGTH = 128;
    /** M02: raised for 747 corpus (learned set still bounded; not a full catalog dump). */
    public static final int MAX_LEARNED_TECHNIQUES = 768;
    public static final int MAX_COOLDOWNS = 768;
    public static final int MAX_TECHNIQUE_SLOTS = PlayerCultivation.TECHNIQUE_SLOT_COUNT;

    public static SyncLearnedTechniquesPacket from(ServerPlayer player, PlayerCultivation cultivation) {
        long gameTime = player.getServer().overworld().getGameTime();
        Map<String, Integer> remainingTicks = remainingCooldownTicks(cultivation.getTechniqueCooldownUntilTicks(), gameTime);
        return new SyncLearnedTechniquesPacket(
                cultivation.getLearnedTechniques().stream().sorted().toList(),
                cultivation.getTechniqueSlots(),
                remainingTicks);
    }

    static Map<String, Integer> remainingCooldownTicks(Map<String, Long> cooldownUntilTicks, long gameTime) {
        Map<String, Integer> remainingTicks = new HashMap<>();
        cooldownUntilTicks.forEach((techniqueId, untilTick) -> {
            long remainingLong = untilTick <= gameTime ? 0L : untilTick - gameTime;
            int remaining = remainingLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)remainingLong;
            if (remaining > 0) {
                remainingTicks.put(techniqueId, remaining);
            }
        });
        return Map.copyOf(remainingTicks);
    }

    public static void send(ServerPlayer player, PlayerCultivation cultivation) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(player, cultivation));
    }

    public static void encode(SyncLearnedTechniquesPacket packet, FriendlyByteBuf buffer) {
        requireSize(packet.learnedTechniques.size(), MAX_LEARNED_TECHNIQUES, "learned techniques");
        buffer.writeVarInt(packet.learnedTechniques.size());
        for (String techniqueId : packet.learnedTechniques) {
            writeTechniqueId(buffer, techniqueId);
        }
        requireSize(packet.techniqueSlots.size(), MAX_TECHNIQUE_SLOTS, "technique slots");
        buffer.writeVarInt(packet.techniqueSlots.size());
        for (String techniqueId : packet.techniqueSlots) {
            writeTechniqueId(buffer, techniqueId);
        }
        requireSize(packet.cooldownRemainingTicks.size(), MAX_COOLDOWNS, "technique cooldowns");
        buffer.writeVarInt(packet.cooldownRemainingTicks.size());
        packet.cooldownRemainingTicks.forEach((techniqueId, remainingTicks) -> {
            writeTechniqueId(buffer, techniqueId);
            buffer.writeVarInt(Math.max(0, remainingTicks));
        });
    }

    public static SyncLearnedTechniquesPacket decode(FriendlyByteBuf buffer) {
        int learnedSize = readBoundedSize(buffer, MAX_LEARNED_TECHNIQUES, "learned techniques");
        List<String> techniques = new ArrayList<>();
        for (int i = 0; i < learnedSize; i++) {
            techniques.add(buffer.readUtf(MAX_TECHNIQUE_ID_LENGTH));
        }
        int slotSize = readBoundedSize(buffer, MAX_TECHNIQUE_SLOTS, "technique slots");
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < slotSize; i++) {
            slots.add(buffer.readUtf(MAX_TECHNIQUE_ID_LENGTH));
        }
        int cooldownSize = readBoundedSize(buffer, MAX_COOLDOWNS, "technique cooldowns");
        Map<String, Integer> cooldowns = new HashMap<>();
        for (int i = 0; i < cooldownSize; i++) {
            String techniqueId = buffer.readUtf(MAX_TECHNIQUE_ID_LENGTH);
            int remainingTicks = buffer.readVarInt();
            if (!techniqueId.isBlank() && remainingTicks > 0) {
                cooldowns.put(techniqueId, remainingTicks);
            }
        }
        return new SyncLearnedTechniquesPacket(List.copyOf(techniques), List.copyOf(slots), Map.copyOf(cooldowns));
    }

    private static void writeTechniqueId(FriendlyByteBuf buffer, String techniqueId) {
        buffer.writeUtf(techniqueId == null ? "" : techniqueId, MAX_TECHNIQUE_ID_LENGTH);
    }

    private static void requireSize(int size, int max, String label) {
        if (size < 0 || size > max) {
            throw new IllegalArgumentException(label + " count " + size + " exceeds " + max);
        }
    }

    private static int readBoundedSize(FriendlyByteBuf buffer, int max, String label) {
        int size = buffer.readVarInt();
        if (size < 0 || size > max) {
            throw new DecoderException(label + " count " + size + " exceeds " + max);
        }
        return size;
    }

    public static void handle(SyncLearnedTechniquesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.xunxian.seekingimmortals.client.ClientTechniqueData.setTechniqueData(packet.learnedTechniques, packet.techniqueSlots, packet.cooldownRemainingTicks)));
        context.setPacketHandled(true);
    }
}
