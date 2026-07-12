package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetMeditatingPacket(boolean meditating) {
    private static final int MEDITATION_HUNGER_MINIMUM = 6;
    private static final double MEDITATION_MONSTER_CHECK_RADIUS = 8.0D;

    public static void encode(SetMeditatingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.meditating);
    }

    public static SetMeditatingPacket decode(FriendlyByteBuf buffer) {
        return new SetMeditatingPacket(buffer.readBoolean());
    }

    public static void handle(SetMeditatingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            CultivationHelper.get(player).ifPresent(cultivation -> {
                if (packet.meditating) {
                    Component failure = validateStart(player);
                    if (failure != null) {
                        cultivation.setMeditating(false);
                        player.displayClientMessage(failure, true);
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                }
                if (!packet.meditating && player.getVehicle() instanceof CushionSeatEntity seat) {
                    net.minecraft.core.BlockPos cushionPos = seat.getCushionPos();
                    player.stopRiding();
                    player.setPos(cushionPos.getX() + 0.5D, cushionPos.getY() + 6.0D / 16.0D, cushionPos.getZ() + 0.5D);
                }
                cultivation.setMeditating(packet.meditating);
                player.displayClientMessage(Component.translatable(
                        packet.meditating ? "message.seeking_immortals.meditation.start" : "message.seeking_immortals.meditation.stop"), true);
                SyncCultivationDataPacket.send(player, cultivation);
            });
        });
        context.setPacketHandled(true);
    }

    private static Component validateStart(ServerPlayer player) {
        if (!isSittingOnValidMeditationCushion(player)) {
            return Component.translatable("message.seeking_immortals.meditation.start.not_cushion");
        }
        if (!player.getAbilities().instabuild && player.getFoodData().getFoodLevel() <= MEDITATION_HUNGER_MINIMUM) {
            return Component.translatable("message.seeking_immortals.meditation.start.hungry");
        }
        if (hasNearbyMonster(player)) {
            return Component.translatable("message.seeking_immortals.meditation.start.monster");
        }
        return null;
    }

    private static boolean isSittingOnValidMeditationCushion(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof CushionSeatEntity seat) {
            BlockPos cushionPos = seat.getCushionPos();
            return player.level().getBlockState(cushionPos).is(ModBlocks.MEDITATION_CUSHION.get());
        }
        return false;
    }

    private static boolean hasNearbyMonster(ServerPlayer player) {
        return !player.level().getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(MEDITATION_MONSTER_CHECK_RADIUS),
                monster -> monster.isAlive() && !monster.isSpectator()).isEmpty();
    }
}
