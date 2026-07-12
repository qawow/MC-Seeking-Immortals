package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Locale;

/**
 * Lightweight multiplayer sect-war scoreboard (Wave49 Phase22 depth).
 * Server SavedData tracks two faction scores during an open window.
 */
public final class SectWarService {
    private SectWarService() {}

    public static final class WarData extends SavedData {
        private static final String NAME = "seeking_immortals_sect_war";
        private boolean active;
        private String factionA = "qinglan";
        private String factionB = "lingxiao";
        private int scoreA;
        private int scoreB;
        private long endsAtGameTime;

        public static WarData get(ServerLevel overworld) {
            return overworld.getDataStorage().computeIfAbsent(WarData::load, WarData::new, NAME);
        }

        public static WarData load(CompoundTag tag) {
            WarData data = new WarData();
            data.active = tag.getBoolean("Active");
            data.factionA = tag.getString("A");
            data.factionB = tag.getString("B");
            data.scoreA = tag.getInt("ScoreA");
            data.scoreB = tag.getInt("ScoreB");
            data.endsAtGameTime = tag.getLong("Ends");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Active", active);
            tag.putString("A", factionA);
            tag.putString("B", factionB);
            tag.putInt("ScoreA", scoreA);
            tag.putInt("ScoreB", scoreB);
            tag.putLong("Ends", endsAtGameTime);
            return tag;
        }
    }

    public static boolean start(MinecraftServer server, String factionA, String factionB, int minutes) {
        ServerLevel overworld = server.overworld();
        WarData data = WarData.get(overworld);
        data.active = true;
        data.factionA = normalize(factionA);
        data.factionB = normalize(factionB);
        data.scoreA = 0;
        data.scoreB = 0;
        data.endsAtGameTime = overworld.getGameTime() + Math.max(1, minutes) * 20L * 60L;
        data.setDirty();
        broadcast(server, Component.translatable("message.seeking_immortals.sect_war.started",
                data.factionA, data.factionB, minutes));
        return true;
    }

    public static boolean stop(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        WarData data = WarData.get(overworld);
        if (!data.active) {
            return false;
        }
        data.active = false;
        data.setDirty();
        String winner = data.scoreA == data.scoreB ? "draw"
                : (data.scoreA > data.scoreB ? data.factionA : data.factionB);
        broadcast(server, Component.translatable("message.seeking_immortals.sect_war.ended",
                data.factionA, data.scoreA, data.factionB, data.scoreB, winner));
        return true;
    }

    public static void onKill(ServerPlayer killer, ServerPlayer victim) {
        if (killer == null || victim == null || killer.level().isClientSide) {
            return;
        }
        MinecraftServer server = killer.server;
        WarData data = WarData.get(server.overworld());
        if (!data.active) {
            return;
        }
        if (server.overworld().getGameTime() > data.endsAtGameTime) {
            stop(server);
            return;
        }
        String side = sideOf(killer, data);
        if (side.isBlank()) {
            return;
        }
        if (side.equals("A")) {
            data.scoreA++;
        } else {
            data.scoreB++;
        }
        data.setDirty();
        ReputationService.add(killer, side.equals("A") ? data.factionA : data.factionB, 2);
        killer.displayClientMessage(Component.translatable("message.seeking_immortals.sect_war.score",
                data.factionA, data.scoreA, data.factionB, data.scoreB), true);
    }

    public static String status(MinecraftServer server) {
        WarData data = WarData.get(server.overworld());
        if (!data.active) {
            return "inactive";
        }
        long remain = Math.max(0L, data.endsAtGameTime - server.overworld().getGameTime());
        return data.factionA + "=" + data.scoreA + " vs " + data.factionB + "=" + data.scoreB
                + " | remainTicks=" + remain;
    }

    private static String sideOf(ServerPlayer player, WarData data) {
        String[] sect = {""};
        com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).ifPresent(cultivation ->
                sect[0] = cultivation.getSevenMysteriesQuest().getSectId());
        if (sect[0] == null || sect[0].isBlank()) {
            return "";
        }
        String s = sect[0].toLowerCase(Locale.ROOT);
        if (s.contains(data.factionA)) {
            return "A";
        }
        if (s.contains(data.factionB)) {
            return "B";
        }
        return "";
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }

    private static String normalize(String id) {
        return id == null ? "qinglan" : id.trim().toLowerCase(Locale.ROOT);
    }
}
