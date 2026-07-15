package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.skill.LifeSkillService;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.SpecialSkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Wave491 protocol 18: client→server life/special skill tree practice intents.
 * Reuses LifeSkillService / SpecialSkillService; no parallel XP table.
 */
public record SkillTreeActionPacket(String action, String skillId) {
    public static final String ACTION_PRACTICE = "practice";
    public static final String ACTION_INFO = "info";

    private static final int MAX_ACTION = 32;
    private static final int MAX_ID = 64;

    public static void encode(SkillTreeActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action == null ? "" : packet.action, MAX_ACTION);
        buffer.writeUtf(packet.skillId == null ? "" : packet.skillId, MAX_ID);
    }

    public static SkillTreeActionPacket decode(FriendlyByteBuf buffer) {
        return new SkillTreeActionPacket(buffer.readUtf(MAX_ACTION), buffer.readUtf(MAX_ID));
    }

    public static void handle(SkillTreeActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String action = packet.action == null ? "" : packet.action.trim().toLowerCase(Locale.ROOT);
            SkillType type = resolve(packet.skillId);
            if (type == null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.skill_tree.unknown", packet.skillId()), true);
                return;
            }
            if (ACTION_INFO.equals(action)) {
                int level = LifeSkillService.level(player, type);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.skill_tree.info",
                        type.getDisplayName(), level, type.getRequiredRealm().getDisplayName()), false);
                return;
            }
            if (!ACTION_PRACTICE.equals(action)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.skill_tree.unknown_action", action), true);
                return;
            }
            // Small spiritual practice pulse; real gains still come from craft loops.
            if (type.getCategory() == com.xunxian.seekingimmortals.skill.SkillCategory.SPECIAL) {
                SpecialSkillService.practice(player, type, 8, 3);
            } else {
                LifeSkillService.grantPractice(player, type, 10, 4);
            }
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.skill_tree.practiced",
                    type.getDisplayName(), LifeSkillService.level(player, type)), true);
        });
        context.setPacketHandled(true);
    }

    private static SkillType resolve(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return null;
        }
        String id = skillId.trim().toLowerCase(Locale.ROOT);
        for (SkillType type : SkillType.values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(id)
                    || type.getTechniqueId() != null && type.getTechniqueId().equalsIgnoreCase(id)
                    || type.getDisplayName() != null && type.getDisplayName().equalsIgnoreCase(skillId)) {
                return type;
            }
        }
        return null;
    }
}
