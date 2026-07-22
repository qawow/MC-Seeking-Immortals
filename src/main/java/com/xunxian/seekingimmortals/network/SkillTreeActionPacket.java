package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.skill.LifeSkillService;
import com.xunxian.seekingimmortals.skill.SkillTreeCatalogService;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wave491 protocol 18: client→server life/special skill tree practice intents.
 * M02: also resolves skill_trees.json tree ids for info queries.
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
            if (ACTION_PRACTICE.equals(action)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.skill_tree.practice_disabled"), true);
                return;
            }
            if (ACTION_INFO.equals(action)) {
                Optional<SkillTreeCatalogService.Tree> tree = SkillTreeCatalogService.builtin().find(packet.skillId);
                if (tree.isPresent()) {
                    SkillTreeCatalogService.Tree t = tree.get();
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.skill_tree.tree_info",
                            displayName(t.display(), "未知技能树"),
                            methodDisplay(t.methodRoot()),
                            t.spells().size(),
                            t.secrets().size()), false);
                    return;
                }
                SkillType type = resolve(packet.skillId);
                if (type == null) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.skill_tree.unknown",
                            displayName(packet.skillId(), "未知技能")), true);
                    return;
                }
                int level = LifeSkillService.level(player, type);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.skill_tree.info",
                        skillDisplay(type), level, realmDisplay(type)), false);
                return;
            }
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.skill_tree.unknown_action",
                    actionDisplay(action)), true);
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

    private static Component displayName(String value, String fallback) {
        return PlayerDisplayText.safeCatalogLiteral(value, fallback);
    }

    private static Component methodDisplay(String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return Component.literal("未设主功法");
        }
        return TextMaterialCatalogService.builtin().findMethod(methodId)
                .map(TextMaterialCatalogService.MethodEntry::display)
                .map(value -> PlayerDisplayText.safeCatalogLiteral(value, "未知功法"))
                .orElseGet(() -> Component.literal("未知功法"));
    }

    private static Component skillDisplay(SkillType type) {
        return PlayerDisplayText.safeCatalogLiteral(
                type == null ? "" : type.getDisplayName(), "未知技能");
    }

    private static Component realmDisplay(SkillType type) {
        return PlayerDisplayText.safeCatalogLiteral(
                type == null || type.getRequiredRealm() == null
                        ? "" : type.getRequiredRealm().getDisplayName(),
                "未知境界");
    }

    private static String actionDisplay(String action) {
        return switch (action == null ? "" : action) {
            case ACTION_PRACTICE -> "修习";
            case ACTION_INFO -> "查看详情";
            default -> "未识别操作";
        };
    }
}
