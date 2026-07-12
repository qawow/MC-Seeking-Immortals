package com.xunxian.seekingimmortals.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncSectDataPacket(String sectId, String sectDisplay, String currentSectDisplay, String role,
                                 int contribution, boolean yueArrived, boolean sevenMysteriesComplete,
                                 boolean member, boolean canJoin, int stage, String stageKey, String objectiveKey,
                                 List<CandidateData> candidates, DialogueNodeData dialogue, MissionData mission,
                                 List<ShopEntryData> shopEntries, boolean openScreen) {
    private static final int MAX_TEXT = 128;
    private static final int MAX_KEY = 192;
    private static final int MAX_CANDIDATES = 8;
    private static final int MAX_DIALOGUE_OPTIONS = 8;
    private static final int MAX_SHOP_ENTRIES = 64;

    public static void send(ServerPlayer player, SyncSectDataPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void encode(SyncSectDataPacket packet, FriendlyByteBuf buffer) {
        write(buffer, packet.sectId, MAX_TEXT);
        write(buffer, packet.sectDisplay, MAX_TEXT);
        write(buffer, packet.currentSectDisplay, MAX_TEXT);
        write(buffer, packet.role, MAX_TEXT);
        buffer.writeVarInt(Math.max(0, packet.contribution));
        buffer.writeBoolean(packet.yueArrived);
        buffer.writeBoolean(packet.sevenMysteriesComplete);
        buffer.writeBoolean(packet.member);
        buffer.writeBoolean(packet.canJoin);
        buffer.writeVarInt(Math.max(0, packet.stage));
        write(buffer, packet.stageKey, MAX_KEY);
        write(buffer, packet.objectiveKey, MAX_KEY);
        writeCandidates(packet.candidates, buffer);
        writeDialogue(packet.dialogue, buffer);
        writeMission(packet.mission, buffer);
        writeShopEntries(packet.shopEntries, buffer);
        buffer.writeBoolean(packet.openScreen);
    }

    public static SyncSectDataPacket decode(FriendlyByteBuf buffer) {
        String sectId = buffer.readUtf(MAX_TEXT);
        String sectDisplay = buffer.readUtf(MAX_TEXT);
        String currentSectDisplay = buffer.readUtf(MAX_TEXT);
        String role = buffer.readUtf(MAX_TEXT);
        int contribution = buffer.readVarInt();
        boolean yueArrived = buffer.readBoolean();
        boolean sevenMysteriesComplete = buffer.readBoolean();
        boolean member = buffer.readBoolean();
        boolean canJoin = buffer.readBoolean();
        int stage = buffer.readVarInt();
        String stageKey = buffer.readUtf(MAX_KEY);
        String objectiveKey = buffer.readUtf(MAX_KEY);
        List<CandidateData> candidates = readCandidates(buffer);
        DialogueNodeData dialogue = readDialogue(buffer);
        MissionData mission = readMission(buffer);
        List<ShopEntryData> entries = readShopEntries(buffer);
        boolean openScreen = buffer.readBoolean();
        return new SyncSectDataPacket(sectId, sectDisplay, currentSectDisplay, role, contribution,
                yueArrived, sevenMysteriesComplete, member, canJoin, stage, stageKey, objectiveKey,
                candidates, dialogue, mission, entries, openScreen);
    }

    public static void handle(SyncSectDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.xunxian.seekingimmortals.client.ClientSectData.set(packet);
            if (packet.openScreen()) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.xunxian.seekingimmortals.client.SectScreen());
            }
        }));
        context.setPacketHandled(true);
    }

    private static void writeCandidates(List<CandidateData> candidates, FriendlyByteBuf buffer) {
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalArgumentException("sect candidates exceed " + MAX_CANDIDATES);
        }
        buffer.writeVarInt(candidates.size());
        for (CandidateData candidate : candidates) {
            write(buffer, candidate.id(), MAX_TEXT);
            write(buffer, candidate.displayZh(), MAX_TEXT);
            write(buffer, candidate.displayEn(), MAX_TEXT);
            write(buffer, candidate.focusKey(), MAX_KEY);
            write(buffer, candidate.structureId(), MAX_TEXT);
            buffer.writeBoolean(candidate.canApply());
        }
    }

    private static List<CandidateData> readCandidates(FriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_CANDIDATES, "sect candidate");
        List<CandidateData> candidates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            candidates.add(new CandidateData(
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_KEY),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readBoolean()));
        }
        return List.copyOf(candidates);
    }

    private static void writeDialogue(DialogueNodeData dialogue, FriendlyByteBuf buffer) {
        DialogueNodeData safe = dialogue == null ? DialogueNodeData.empty() : dialogue;
        write(buffer, safe.id(), MAX_TEXT);
        write(buffer, safe.titleKey(), MAX_KEY);
        write(buffer, safe.textKey(), MAX_KEY);
        if (safe.options().size() > MAX_DIALOGUE_OPTIONS) {
            throw new IllegalArgumentException("sect dialogue options exceed " + MAX_DIALOGUE_OPTIONS);
        }
        buffer.writeVarInt(safe.options().size());
        for (DialogueOptionData option : safe.options()) {
            write(buffer, option.id(), MAX_TEXT);
            write(buffer, option.labelKey(), MAX_KEY);
            write(buffer, option.action(), MAX_TEXT);
        }
    }

    private static DialogueNodeData readDialogue(FriendlyByteBuf buffer) {
        String id = buffer.readUtf(MAX_TEXT);
        String titleKey = buffer.readUtf(MAX_KEY);
        String textKey = buffer.readUtf(MAX_KEY);
        int count = readCount(buffer, MAX_DIALOGUE_OPTIONS, "sect dialogue option");
        List<DialogueOptionData> options = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            options.add(new DialogueOptionData(
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_KEY),
                    buffer.readUtf(MAX_TEXT)));
        }
        return new DialogueNodeData(id, titleKey, textKey, List.copyOf(options));
    }

    private static void writeMission(MissionData mission, FriendlyByteBuf buffer) {
        MissionData safe = mission == null ? MissionData.empty() : mission;
        write(buffer, safe.id(), MAX_TEXT);
        write(buffer, safe.titleKey(), MAX_KEY);
        write(buffer, safe.objectiveKey(), MAX_KEY);
        write(buffer, safe.itemDescriptionId(), MAX_KEY);
        buffer.writeVarInt(Math.max(0, safe.target()));
        buffer.writeVarInt(Math.max(0, safe.rewardContribution()));
        buffer.writeBoolean(safe.accepted());
        buffer.writeBoolean(safe.completed());
        buffer.writeBoolean(safe.canTurnIn());
    }

    private static MissionData readMission(FriendlyByteBuf buffer) {
        return new MissionData(
                buffer.readUtf(MAX_TEXT),
                buffer.readUtf(MAX_KEY),
                buffer.readUtf(MAX_KEY),
                buffer.readUtf(MAX_KEY),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }

    private static void writeShopEntries(List<ShopEntryData> shopEntries, FriendlyByteBuf buffer) {
        if (shopEntries.size() > MAX_SHOP_ENTRIES) {
            throw new IllegalArgumentException("sect shop entries exceed " + MAX_SHOP_ENTRIES);
        }
        buffer.writeVarInt(shopEntries.size());
        for (ShopEntryData entry : shopEntries) {
            write(buffer, entry.id(), MAX_TEXT);
            write(buffer, entry.itemDescriptionId(), MAX_KEY);
            buffer.writeVarInt(Math.max(1, entry.count()));
            buffer.writeVarInt(Math.max(1, entry.cost()));
            write(buffer, entry.currency(), MAX_TEXT);
        }
    }

    private static List<ShopEntryData> readShopEntries(FriendlyByteBuf buffer) {
        int count = readCount(buffer, MAX_SHOP_ENTRIES, "sect shop entry");
        List<ShopEntryData> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new ShopEntryData(
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_KEY),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_TEXT)));
        }
        return List.copyOf(entries);
    }

    private static int readCount(FriendlyByteBuf buffer, int max, String label) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException(label + " count " + count + " exceeds " + max);
        }
        return count;
    }

    private static void write(FriendlyByteBuf buffer, String value, int maxLength) {
        buffer.writeUtf(value == null ? "" : value, maxLength);
    }

    public record CandidateData(String id, String displayZh, String displayEn, String focusKey,
                                String structureId, boolean canApply) {}

    public record DialogueNodeData(String id, String titleKey, String textKey, List<DialogueOptionData> options) {
        public static DialogueNodeData empty() {
            return new DialogueNodeData("", "", "", List.of());
        }
    }

    public record DialogueOptionData(String id, String labelKey, String action) {}

    public record MissionData(String id, String titleKey, String objectiveKey, String itemDescriptionId,
                              int target, int rewardContribution, boolean accepted, boolean completed,
                              boolean canTurnIn) {
        public static MissionData empty() {
            return new MissionData("", "", "", "", 0, 0, false, false, false);
        }
    }

    public record ShopEntryData(String id, String itemDescriptionId, int count, int cost, String currency) {}
}
