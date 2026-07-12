package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Soft talisman_consume policy for text-material CAST_* / *TALISMAN* techniques.
 * Survival requires and consumes a mapped talisman item; creative/instabuild skips.
 */
public final class TalismanConsumePolicy {
    private TalismanConsumePolicy() {}

    public static boolean requiresTalisman(String techniqueId, SkillType skillType) {
        String id = techniqueId == null ? "" : techniqueId.toLowerCase(Locale.ROOT);
        if (id.startsWith("cast_") && id.contains("talisman")) {
            return true;
        }
        if (id.endsWith("_talisman") || id.contains("talisman_")) {
            return true;
        }
        if (skillType != null) {
            String name = skillType.name().toLowerCase(Locale.ROOT);
            if (name.contains("talisman") || name.startsWith("cast_")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if allowed to cast (creative or consumed successfully)
     */
    public static boolean tryConsume(ServerPlayer player, String techniqueId, SkillType skillType) {
        if (!requiresTalisman(techniqueId, skillType)) {
            return true;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        Item item = resolveItem(techniqueId, skillType);
        if (consumeOne(player, item)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.talisman_consume.used",
                    item.getDescription()), true);
            return true;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.talisman_consume.missing",
                item.getDescription()), true);
        return false;
    }

    private static Item resolveItem(String techniqueId, SkillType skillType) {
        String id = (techniqueId == null ? "" : techniqueId + " " + (skillType == null ? "" : skillType.name())).toLowerCase(Locale.ROOT);
        if (id.contains("armor") || id.contains("shield") || id.contains("golden") || id.contains("protect") || id.contains("earth") || id.contains("wall")) {
            return ModItems.ARMOR_TALISMAN.get();
        }
        if (id.contains("wind") || id.contains("speed") || id.contains("escape") || id.contains("teleport") || id.contains("invis") || id.contains("hide") || id.contains("ghost_hide")) {
            return ModItems.SPEED_TALISMAN.get();
        }
        if (id.contains("yin") || id.contains("soul") || id.contains("ghost") || id.contains("anti_demon") || id.contains("seal")) {
            return ModItems.YIN_BODY_PROTECTION_CHARM.get();
        }
        return ModItems.FIRE_TALISMAN.get();
    }

    private static boolean consumeOne(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
