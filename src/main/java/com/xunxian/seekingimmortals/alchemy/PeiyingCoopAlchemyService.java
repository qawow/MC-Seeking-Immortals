package com.xunxian.seekingimmortals.alchemy;

import com.xunxian.seekingimmortals.beast.LiveCaptureCarrierService;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.quest.DetailedQuestProofService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Y-C: 窟外协作炼丹 — the consumer that finally closes {@code peiying_material_hunt}.
 *
 * <p>Authored rules this enforces:</p>
 * <ul>
 *   <li>「活的才是丹。死的，只是一堆打折的肉」— a live carrier always brews better than a
 *       degraded one, and degraded material carries its own lower ceiling.</li>
 *   <li>{@code peiying_dan_craft_chance: [0.15, 0.35]} — a hard band. Station tier, skill,
 *       aura and operational damage all move the odds inside it, never outside.</li>
 *   <li>「协作炼丹是『富成气质』的可玩化，不强制任何原著身份」— on-site partners raise the
 *       odds and share the step credit, but never duplicate the pill.</li>
 * </ul>
 *
 * <p>The success maths is a pure function so the authored band is testable without a server.</p>
 */
public final class PeiyingCoopAlchemyService {
    /** The authored coop recipe id; the band below applies to this recipe only. */
    public static final String COOP_RECIPE_ID = "peiying_dan";
    /** Authored floor from {@code peiying_dan_craft_chance}. */
    public static final double AUTHORED_MIN_CHANCE = 0.15D;
    /** Authored ceiling from {@code peiying_dan_craft_chance}. */
    public static final double AUTHORED_MAX_CHANCE = 0.35D;
    /**
     * 「设定约两成上下，活捕有加成」— a live carrier starts at the authored two-tenths.
     * This sits strictly above {@link #DEGRADED_MAX_CHANCE} so no amount of coop help can make
     * 打折的肉 brew as well as a live beast.
     */
    public static final double LIVE_MIN_CHANCE = 0.20D;
    /** 「死的，只是一堆打折的肉」— degraded material is capped below the live floor. */
    public static final double DEGRADED_MAX_CHANCE = 0.18D;
    /** Bonus per confirmed on-site partner. */
    public static final double COOP_BONUS_PER_PARTNER = 0.03D;
    /** Beyond this, extra bodies stop helping. */
    public static final int MAX_COOP_PARTNERS = 4;
    /** How close a partner must stand to count as on-site. */
    public static final double COOP_RADIUS = 6.0D;

    private PeiyingCoopAlchemyService() {}

    public static boolean isCoopRecipe(AlchemyRecipe recipe) {
        return recipe != null && COOP_RECIPE_ID.equals(recipe.id());
    }

    /**
     * Clamps the furnace's computed rate into the authored band, then applies the live/degraded
     * split and the coop bonus. Pure function: same inputs always give the same rate.
     *
     * @param baseRate    the furnace's own rate (skill, station, aura, operational damage)
     * @param liveCarrier whether the consumed carrier was still {@code LIVE}
     * @param partners    confirmed on-site partners, excluding the crafter
     */
    public static double resolveSuccessRate(double baseRate, boolean liveCarrier, int partners) {
        // Live and degraded occupy disjoint sub-bands, so the 「活的才是丹」 gap cannot be
        // closed by a better station, a higher skill or more helpers.
        double floor = liveCarrier ? LIVE_MIN_CHANCE : AUTHORED_MIN_CHANCE;
        double ceiling = liveCarrier ? AUTHORED_MAX_CHANCE : DEGRADED_MAX_CHANCE;
        double anchored = clamp(baseRate, floor, ceiling);
        double bonus = COOP_BONUS_PER_PARTNER * Math.min(MAX_COOP_PARTNERS, Math.max(0, partners));
        return clamp(anchored + bonus, floor, ceiling);
    }

    /**
     * Reads the life state of the carrier that {@link AlchemyRecipeService} will actually
     * consume. That path walks {@code inventory.items} in order and takes the first match, so
     * this snapshot mirrors the same order — otherwise the applied bonus could describe a
     * different carrier than the one burned.
     */
    public static boolean snapshotCarrierLive(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (LiveCaptureCarrierService.isCarrier(stack)) {
                return LiveCaptureCarrierService.isLive(stack);
            }
        }
        return false;
    }

    /**
     * Snapshots the partners standing at the furnace, excluding the crafter. Presence is read
     * server-side from the level; the client never supplies the roster.
     */
    public static List<UUID> snapshotParticipants(ServerLevel level, net.minecraft.core.BlockPos pos,
                                                  ServerPlayer crafter) {
        if (level == null || pos == null) {
            return List.of();
        }
        Set<UUID> found = new LinkedHashSet<>();
        for (ServerPlayer nearby : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(pos).inflate(COOP_RADIUS))) {
            if (crafter != null && nearby.getUUID().equals(crafter.getUUID())) {
                continue;
            }
            found.add(nearby.getUUID());
            if (found.size() >= MAX_COOP_PARTNERS) {
                break;
            }
        }
        return List.copyOf(found);
    }

    /**
     * Re-confirms the snapshot at settlement time and credits whoever is still on site.
     * A partner who walked away earns nothing; a passer-by who never invested the cook time is
     * not in the snapshot and so cannot harvest the proof either.
     *
     * @return the confirmed partners, for messaging
     */
    public static List<ServerPlayer> creditCoopParticipants(ServerLevel level,
                                                            net.minecraft.core.BlockPos pos,
                                                            ServerPlayer crafter,
                                                            List<UUID> snapshot,
                                                            String stationId,
                                                            boolean succeeded) {
        if (level == null || pos == null || snapshot == null || snapshot.isEmpty()) {
            return List.of();
        }
        Set<UUID> stillPresent = new LinkedHashSet<>();
        for (ServerPlayer nearby : level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(pos).inflate(COOP_RADIUS))) {
            stillPresent.add(nearby.getUUID());
        }
        List<ServerPlayer> credited = new ArrayList<>();
        for (UUID id : snapshot) {
            if (!stillPresent.contains(id)) {
                continue;
            }
            ServerPlayer partner = level.getServer().getPlayerList().getPlayer(id);
            if (partner == null || (crafter != null && partner.getUUID().equals(crafter.getUUID()))) {
                continue;
            }
            // Step 3 is authored SOLO_OR_PARTY, so every confirmed partner really did the work.
            DetailedQuestProofService.recordAlchemyCompleted(partner, stationId);
            // The pill itself is never duplicated; partners take a share of the by-product.
            ItemStack share = new ItemStack(succeeded
                    ? ModItems.SPIRIT_STONE_SHARD.get()
                    : ModItems.WASTE_PILL.get(), 1);
            InventoryDeliveryService.giveOrEnqueue(partner, share, "peiying_coop_share");
            partner.displayClientMessage(Component.translatable(succeeded
                    ? "message.seeking_immortals.peiying_coop.partner_success"
                    : "message.seeking_immortals.peiying_coop.partner_failed"), true);
            credited.add(partner);
        }
        return List.copyOf(credited);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
