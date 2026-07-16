package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.quest.TextQuestDialogueService;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server-to-client bounded dialogue view. */
public record OpenDialogueScreenPacket(
        String context,
        String sourceId,
        String npcId,
        String nodeId,
        Component speaker,
        List<Component> lines,
        List<Choice> choices
) {
    public static final int MAX_LINES = 16;
    public static final int MAX_CHOICES = 8;

    private static final int MAX_CONTEXT = 64;
    private static final int MAX_ID = 96;
    private static final int MAX_COMPONENT_JSON = 2_048;

    public OpenDialogueScreenPacket {
        context = context == null ? "" : context;
        sourceId = sourceId == null ? "" : sourceId;
        npcId = npcId == null ? "" : npcId;
        nodeId = nodeId == null ? "" : nodeId;
        speaker = speaker == null ? Component.empty() : speaker.copy();
        lines = lines == null ? List.of() : List.copyOf(lines);
        choices = choices == null ? List.of() : List.copyOf(choices);
    }

    /** Compatibility entry point used by text-quest commands and NPC hooks. */
    public static void send(ServerPlayer player, String chainId) {
        TextQuestDialogueService.openScreen(player, chainId);
    }

    public static void send(ServerPlayer player, OpenDialogueScreenPacket packet) {
        if (player == null || packet == null) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void encode(OpenDialogueScreenPacket packet, FriendlyByteBuf buffer) {
        write(buffer, packet.context(), MAX_CONTEXT);
        write(buffer, packet.sourceId(), MAX_ID);
        write(buffer, packet.npcId(), MAX_ID);
        write(buffer, packet.nodeId(), MAX_ID);
        writeComponent(buffer, packet.speaker());
        writeComponents(buffer, packet.lines());
        writeChoices(buffer, packet.choices());
    }

    public static OpenDialogueScreenPacket decode(FriendlyByteBuf buffer) {
        String context = buffer.readUtf(MAX_CONTEXT);
        String sourceId = buffer.readUtf(MAX_ID);
        String npcId = buffer.readUtf(MAX_ID);
        String nodeId = buffer.readUtf(MAX_ID);
        Component speaker = readComponent(buffer);
        int lineCount = readCount(buffer, MAX_LINES, "dialogue line");
        List<Component> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(readComponent(buffer));
        }
        int choiceCount = readCount(buffer, MAX_CHOICES, "dialogue choice");
        List<Choice> choices = new ArrayList<>(choiceCount);
        for (int i = 0; i < choiceCount; i++) {
            choices.add(new Choice(buffer.readUtf(MAX_ID), readComponent(buffer)));
        }
        return new OpenDialogueScreenPacket(context, sourceId, npcId, nodeId,
                speaker, List.copyOf(lines), List.copyOf(choices));
    }

    public static void handle(OpenDialogueScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.xunxian.seekingimmortals.client.DialogueScreen(packet))));
        context.setPacketHandled(true);
    }

    private static void writeComponents(FriendlyByteBuf buffer, List<Component> values) {
        List<Component> safe = values == null ? List.of() : values;
        requireCount(safe.size(), MAX_LINES, "dialogue line");
        buffer.writeVarInt(safe.size());
        for (Component value : safe) {
            writeComponent(buffer, value);
        }
    }

    private static void writeChoices(FriendlyByteBuf buffer, List<Choice> values) {
        List<Choice> safe = values == null ? List.of() : values;
        requireCount(safe.size(), MAX_CHOICES, "dialogue choice");
        buffer.writeVarInt(safe.size());
        for (Choice choice : safe) {
            Choice value = choice == null ? new Choice("", Component.empty()) : choice;
            write(buffer, value.id(), MAX_ID);
            writeComponent(buffer, value.label());
        }
    }

    private static void writeComponent(FriendlyByteBuf buffer, Component value) {
        String json = Component.Serializer.toJson(value == null ? Component.empty() : value);
        buffer.writeUtf(json, MAX_COMPONENT_JSON);
    }

    private static Component readComponent(FriendlyByteBuf buffer) {
        String json = buffer.readUtf(MAX_COMPONENT_JSON);
        try {
            Component component = Component.Serializer.fromJson(json);
            if (component == null) {
                throw new DecoderException("dialogue component is null");
            }
            return component;
        } catch (DecoderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DecoderException("invalid dialogue component", exception);
        }
    }

    private static int readCount(FriendlyByteBuf buffer, int max, String label) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException(label + " count " + count + " exceeds " + max);
        }
        return count;
    }

    private static void requireCount(int count, int max, String label) {
        if (count < 0 || count > max) {
            throw new IllegalArgumentException(label + " count " + count + " exceeds " + max);
        }
    }

    private static void write(FriendlyByteBuf buffer, String value, int maxLength) {
        buffer.writeUtf(value == null ? "" : value, maxLength);
    }

    public record Choice(String id, Component label) {
        public Choice {
            id = id == null ? "" : id;
            label = label == null ? Component.empty() : label.copy();
        }
    }
}
