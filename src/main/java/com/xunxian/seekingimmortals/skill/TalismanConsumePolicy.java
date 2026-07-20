package com.xunxian.seekingimmortals.skill;

import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Soft talisman_consume policy for text-material CAST_* / *TALISMAN* techniques.
 * Survival reserves one mapped talisman before effect execution and refunds on failure;
 * creative/instabuild skips inventory mutation.
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
     * Legacy one-shot consume. Prefer {@link #tryReserve(ServerPlayer, String, SkillType)} so failed
     * casts can refund. Kept for callers that intentionally commit immediately.
     */
    public static boolean tryConsume(ServerPlayer player, String techniqueId, SkillType skillType) {
        Reservation reservation = tryReserve(player, techniqueId, skillType);
        if (!reservation.allowed()) {
            return false;
        }
        reservation.commit(player);
        return true;
    }

    /**
     * Reserve one talisman without finalizing the cast. On effect failure call {@link Reservation#refund};
     * on success call {@link Reservation#commit}.
     */
    public static Reservation tryReserve(ServerPlayer player, String techniqueId, SkillType skillType) {
        if (!requiresTalisman(techniqueId, skillType)) {
            return Reservation.notRequired();
        }
        if (player == null) {
            return Reservation.denied(ModItems.FIRE_TALISMAN.get());
        }
        if (player.getAbilities().instabuild) {
            return Reservation.creative(resolveItem(techniqueId, skillType));
        }
        Item item = resolveItem(techniqueId, skillType);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !matchesTechnique(stack, techniqueId, skillType)) {
                continue;
            }
            ItemStack taken = stack.split(1);
            if (taken.isEmpty()) {
                continue;
            }
            player.getInventory().setChanged();
            // Remember the actual item type for messaging.
            return Reservation.reserved(taken.getItem(), taken);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.talisman_consume.missing",
                item.getDescription()), true);
        return Reservation.denied(item);
    }

    static Item resolveItem(String techniqueId, SkillType skillType) {
        String id = (techniqueId == null ? "" : techniqueId + " " + (skillType == null ? "" : skillType.name()))
                .toLowerCase(Locale.ROOT);
        // Prefer a bulk catalog talisman that matches the technique id when present in inventory later;
        // resolveItem only picks the dedicated fallback family.
        if (id.contains("armor") || id.contains("shield") || id.contains("golden") || id.contains("protect")
                || id.contains("earth") || id.contains("wall") || id.contains("body_guard") || id.contains("defense")) {
            return ModItems.ARMOR_TALISMAN.get();
        }
        if (id.contains("wind") || id.contains("speed") || id.contains("escape") || id.contains("teleport")
                || id.contains("invis") || id.contains("hide") || id.contains("ghost_hide") || id.contains("void_escape")
                || id.contains("blood_escape")) {
            return ModItems.SPEED_TALISMAN.get();
        }
        if (id.contains("yin") || id.contains("soul") || id.contains("ghost") || id.contains("anti_demon")
                || id.contains("seal") || id.contains("demon") || id.contains("ward")) {
            return ModItems.YIN_BODY_PROTECTION_CHARM.get();
        }
        return ModItems.FIRE_TALISMAN.get();
    }

    /**
     * True when the stack is an acceptable talisman payment for the technique:
     * exact dedicated family item, or any bulk CatalogTalismanItem whose mode family matches.
     */
    public static boolean matchesTechnique(ItemStack stack, String techniqueId, SkillType skillType) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item expected = resolveItem(techniqueId, skillType);
        if (stack.is(expected)) {
            return true;
        }
        if (stack.getItem() instanceof com.xunxian.seekingimmortals.item.CatalogTalismanItem talisman) {
            String mode = com.xunxian.seekingimmortals.item.CatalogTalismanService.modeKey(
                    talisman.catalogId(), talisman.role());
            String expectedFamily = familyOf(expected);
            return expectedFamily.equals(familyOfMode(mode));
        }
        return false;
    }

    private static String familyOf(Item item) {
        if (item == ModItems.ARMOR_TALISMAN.get()) {
            return "armor";
        }
        if (item == ModItems.SPEED_TALISMAN.get()) {
            return "escape";
        }
        if (item == ModItems.YIN_BODY_PROTECTION_CHARM.get()) {
            return "ward";
        }
        return "projectile";
    }

    private static String familyOfMode(String mode) {
        if (mode == null) {
            return "projectile";
        }
        return switch (mode) {
            case "armor" -> "armor";
            case "escape", "speed", "invis" -> "escape";
            case "ward", "control", "heal", "utility", "contract" -> "ward";
            default -> "projectile";
        };
    }

    public static final class Reservation {
        private final boolean allowed;
        private final boolean required;
        private final Item item;
        private final ItemStack taken;
        private boolean finished;

        private Reservation(boolean allowed, boolean required, Item item, ItemStack taken) {
            this.allowed = allowed;
            this.required = required;
            this.item = item;
            // Avoid ItemStack.EMPTY in pure JVM unit tests (needs registry bootstrap).
            this.taken = (taken == null || taken.isEmpty()) ? null : taken.copy();
            this.finished = !required || !allowed || this.taken == null || this.taken.isEmpty();
        }

        public static Reservation notRequired() {
            return new Reservation(true, false, null, null);
        }

        public static Reservation creative(Item item) {
            return new Reservation(true, true, item, null);
        }

        public static Reservation reserved(Item item, ItemStack taken) {
            return new Reservation(true, true, item, taken);
        }

        public static Reservation denied(Item item) {
            return new Reservation(false, true, item, null);
        }

        public boolean allowed() {
            return allowed;
        }

        public boolean required() {
            return required;
        }

        public Item item() {
            return item;
        }

        /** Finalize consumption after a successful effect. Idempotent. */
        public void commit(ServerPlayer player) {
            if (finished || !allowed || !required) {
                finished = true;
                return;
            }
            finished = true;
            if (player != null && item != null && taken != null && !taken.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.talisman_consume.used",
                        item.getDescription()), true);
            }
            // Stack already removed during reserve; nothing else to mutate.
        }

        /** Restore the reserved talisman when effect execution fails. Idempotent. */
        public void refund(ServerPlayer player) {
            if (finished || !allowed || !required || taken == null || taken.isEmpty() || player == null) {
                finished = true;
                return;
            }
            finished = true;
            ItemStack give = taken.copy();
            com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                    player, give, "talisman_consume_refund");
            player.getInventory().setChanged();
        }
    }
}
