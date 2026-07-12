package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SetMovementSpeedScalePacket(double scale) {
    private static final UUID CULTIVATION_MOVEMENT_SPEED_UUID = UUID.fromString("275d4c23-2678-4f45-8445-2525d5896053");

    public static void encode(SetMovementSpeedScalePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.scale);
    }

    public static SetMovementSpeedScalePacket decode(FriendlyByteBuf buffer) {
        return new SetMovementSpeedScalePacket(buffer.readDouble());
    }

    public static void handle(SetMovementSpeedScalePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            CultivationHelper.get(player).ifPresent(cultivation -> {
                double scale = Double.isFinite(packet.scale) ? packet.scale : 1.0D;
                cultivation.setMovementSpeedScale(scale);
                refreshMovementSpeed(player, cultivation.getEffectiveMovementSpeedBonus());
                SyncCultivationDataPacket.send(player, cultivation);
            });
        });
        context.setPacketHandled(true);
    }

    private static void refreshMovementSpeed(ServerPlayer player, double amount) {
        var instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) return;
        AttributeModifier oldModifier = instance.getModifier(CULTIVATION_MOVEMENT_SPEED_UUID);
        if (oldModifier != null) {
            instance.removeModifier(CULTIVATION_MOVEMENT_SPEED_UUID);
        }
        if (amount > 0.0001D) {
            instance.addTransientModifier(new AttributeModifier(
                    CULTIVATION_MOVEMENT_SPEED_UUID,
                    "seeking_immortals_cultivation_movement_speed",
                    amount,
                    AttributeModifier.Operation.ADDITION));
        }
    }
}
