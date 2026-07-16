package com.xunxian.seekingimmortals.npc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the Forge EVENT_BUS when a dialogue node is reached/selected for a player.
 * M11 quest system consumes this to advance story hooks without owning dialogue runtime.
 */
public final class DialogueNodeReachedEvent extends Event {
    private final ServerPlayer player;
    private final String npcId;
    private final String treeId;
    private final String nodeId;

    public DialogueNodeReachedEvent(ServerPlayer player, String npcId, String treeId, String nodeId) {
        this.player = player;
        this.npcId = npcId == null ? "" : npcId;
        this.treeId = treeId == null ? "" : treeId;
        this.nodeId = nodeId == null ? "" : nodeId;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getNpcId() {
        return npcId;
    }

    public String getTreeId() {
        return treeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
