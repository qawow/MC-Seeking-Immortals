package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Hard-gate evaluator for spatial_nodes requires / cost matrices.
 * AI default Wave41: item + realm hard gates; pure reputation OR tags soft-warn only.
 * Creative/instabuild bypasses item consumption and presence checks.
 */
public final class SpatialNodeRequiresService {
    private SpatialNodeRequiresService() {}

    /**
     * Enforce requires for the first catalog node matching a spatial node type.
     * Used by placeable multiblock gates that map 1:N to catalog nodes.
     */
    public static boolean enforceByType(ServerPlayer player, String nodeType) {
        return reserveByType(player, nodeType) != null;
    }

    public static Reservation reserveByType(ServerPlayer player, String nodeType) {
        if (player == null) {
            return null;
        }
        if (nodeType == null || nodeType.isBlank()) {
            return Reservation.none();
        }
        String type = nodeType.trim().toLowerCase(Locale.ROOT);
        for (SpatialNodeCatalogService.Node node : SpatialNodeCatalogService.builtin().nodes().values()) {
            if (node.type() != null && type.equals(node.type().trim().toLowerCase(Locale.ROOT))) {
                return reserve(player, node);
            }
        }
        return Reservation.none();
    }

    public static boolean enforce(ServerPlayer player, SpatialNodeCatalogService.Node node) {
        return reserve(player, node) != null;
    }

    public static Reservation reserve(ServerPlayer player, SpatialNodeCatalogService.Node node) {
        if (player == null || node == null) {
            return null;
        }
        boolean creative = player.getAbilities().instabuild;
        Map<Item, Integer> costs = new LinkedHashMap<>();

        if (node.costSpiritStone() > 0 && !creative) {
            Item shard = ModItems.SPIRIT_STONE_SHARD.get();
            if (!canAddCost(player, costs, shard, node.costSpiritStone())) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.spatial_node.missing_cost",
                        node.costSpiritStone()), true);
                return null;
            }
            addCost(costs, shard, node.costSpiritStone());
        }

        List<String> softWarnings = new ArrayList<>();
        for (String raw : node.requires()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String req = raw.trim();
            // JSON-object-looking strings from some catalog rows: treat as soft for now if unparsable.
            if (req.startsWith("{")) {
                softWarnings.add(req);
                continue;
            }
            String lower = req.toLowerCase(Locale.ROOT);

            // Realm gates: realm_NASCENT_SOUL / DEITY_TRANSFORMATION_peak / VOID_REFINEMENT_permit etc.
            Optional<Realm> realmGate = parseRealmGate(lower);
            if (realmGate.isPresent()) {
                boolean[] ok = {false};
                CultivationHelper.get(player).ifPresent(c -> ok[0] = c.getRealm().ordinal() >= realmGate.get().ordinal());
                if (!ok[0]) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.spatial_node.realm_gate",
                            realmGate.get().getDisplayName()), true);
                    return null;
                }
                continue;
            }

            // OR groups: try any mapped alternative item.
            if (lower.contains("_or_") || lower.contains("|")) {
                String[] parts = lower.split("_or_|\\|");
                boolean anyMapped = false;
                Item selected = null;
                for (String part : parts) {
                    Item item = mapRequireToItem(part.trim());
                    if (item == null) {
                        continue;
                    }
                    anyMapped = true;
                    if (creative || canAddCost(player, costs, item, 1)) {
                        selected = item;
                        break;
                    }
                }
                if (anyMapped && selected == null) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.spatial_node.missing_require", req), true);
                    return null;
                }
                if (!creative && selected != null) {
                    addCost(costs, selected, 1);
                }
                if (!anyMapped) {
                    softWarnings.add(req);
                }
                continue;
            }

            Item item = mapRequireToItem(lower);
            if (item != null) {
                if (!creative && !canAddCost(player, costs, item, 1)) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.spatial_node.missing_require", req), true);
                    return null;
                }
                if (!creative) {
                    addCost(costs, item, 1);
                }
                continue;
            }

            // Reputation tags: hard-gate against lightweight ReputationService.
            if (ReputationService.parse(lower) != null || lower.startsWith("rep_") || lower.contains("reputation")) {
                if (!ReputationService.meets(player, lower)) {
                    ReputationService.ParsedRep parsed = ReputationService.parse(lower);
                    String faction = parsed == null ? lower : parsed.faction();
                    int need = parsed == null ? ReputationService.NEUTRAL_THRESHOLD : parsed.minValue();
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.spatial_node.missing_reputation",
                            faction, need, ReputationService.get(player, faction)), true);
                    return null;
                }
                continue;
            }

            // Remaining event/lore tags: hard-gate known worldpack events when possible.
            if (isKnownEventRequire(lower)) {
                if (!checkEventRequire(player, lower)) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.spatial_node.missing_event", req), true);
                    return null;
                }
                continue;
            }

            softWarnings.add(req);
        }

        for (String soft : softWarnings) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spatial_node.soft_require", soft), false);
        }
        if (creative || costs.isEmpty()) {
            return Reservation.none();
        }
        InventoryReservation inventory = InventoryReservation.consume(player, costs);
        if (inventory == null) {
            return null;
        }
        return new Reservation(costs, inventory);
    }

    private static boolean canAddCost(ServerPlayer player, Map<Item, Integer> costs, Item item, int count) {
        long required = (long) costs.getOrDefault(item, 0) + Math.max(0, count);
        return item != null && required <= Integer.MAX_VALUE && countItem(player, item) >= required;
    }

    private static void addCost(Map<Item, Integer> costs, Item item, int count) {
        if (item != null && count > 0) {
            costs.merge(item, count, Integer::sum);
        }
    }

    public static final class Reservation {
        private final Map<Item, Integer> costs;
        private final InventoryReservation inventory;
        private boolean refunded;

        private Reservation(Map<Item, Integer> costs, InventoryReservation inventory) {
            this.costs = costs == null ? Map.of() : Map.copyOf(costs);
            this.inventory = inventory == null ? InventoryReservation.none() : inventory;
        }

        public static Reservation none() {
            return new Reservation(Map.of(), InventoryReservation.none());
        }

        public Map<Item, Integer> costs() {
            return costs;
        }

        public boolean isEmpty() {
            return costs.isEmpty();
        }

        public void refund(ServerPlayer player) {
            if (refunded) {
                return;
            }
            refunded = true;
            inventory.refund(player);
        }
    }

    private static boolean isKnownEventRequire(String lower) {
        return lower.contains("seal_weak")
                || lower.contains("ancient_demon_seal")
                || lower.contains("tribulation_success")
                || lower.contains("cycle_void_palace_open");
    }

    private static boolean checkEventRequire(ServerPlayer player, String lower) {
        if (lower.contains("tribulation_success")) {
            return com.xunxian.seekingimmortals.cultivation.TribulationService.hasPassedTribulation(player);
        }
        if (lower.contains("seal_weak") || lower.contains("ancient_demon_seal")) {
            return WorldpackGameplayService.isDemonRiftSealOpenFor(player);
        }
        if (lower.contains("cycle_void_palace_open")) {
            // Treat void palace key fragment possession already handled as item; event itself soft-pass if no active event system.
            return true;
        }
        return false;
    }

    private static Optional<Realm> parseRealmGate(String lower) {
        if (lower.contains("nascent_soul") || lower.contains("yuanying")) {
            return Optional.of(Realm.NASCENT_SOUL);
        }
        if (lower.contains("deity_transformation") || lower.contains("huashen") || lower.contains("soul_transform")) {
            return Optional.of(Realm.SOUL_TRANSFORMATION);
        }
        if (lower.contains("void_refin") || lower.contains("lianxu")) {
            return Optional.of(Realm.VOID_REFINEMENT);
        }
        if (lower.contains("foundation") || lower.contains("zhuji")) {
            return Optional.of(Realm.FOUNDATION_ESTABLISHMENT);
        }
        if (lower.contains("core_formation") || lower.contains("jindan") || lower.contains("gold_core")) {
            return Optional.of(Realm.CORE_FORMATION);
        }
        if (lower.startsWith("realm_")) {
            String key = lower.substring("realm_".length());
            for (Realm realm : Realm.values()) {
                if (realm.name().equalsIgnoreCase(key) || realm.name().toLowerCase(Locale.ROOT).contains(key)) {
                    return Optional.of(realm);
                }
            }
        }
        return Optional.empty();
    }

    private static Item mapRequireToItem(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String t = token.toLowerCase(Locale.ROOT)
                .replace("seeking_immortals:", "")
                .replace(' ', '_');
        return switch (t) {
            case "yin_stone" -> ModItems.YIN_STONE.get();
            case "ferry_pass" -> ModItems.FERRY_PASS.get();
            case "border_merit_token" -> ModItems.BORDER_MERIT_TOKEN.get();
            case "alliance_merit_token" -> ModItems.ALLIANCE_MERIT_TOKEN.get();
            case "sect_permit", "sect_permit_or_contribution" -> ModItems.SECT_PERMIT.get();
            case "war_contribution_token" -> ModItems.WAR_CONTRIBUTION_TOKEN.get();
            case "mulan_pass", "mulan_pass_or_war_truce" -> ModItems.MULAN_PASS.get();
            case "star_palace_tax_receipt" -> ModItems.STAR_PALACE_TAX_RECEIPT.get();
            case "chaotic_sea_teleport_permit" -> ModItems.CHAOTIC_SEA_TELEPORT_PERMIT.get();
            case "diyuan_permit", "void_refinement_permit" -> ModItems.DIYUAN_PERMIT.get();
            case "immortal_jade" -> ModItems.IMMORTAL_JADE.get();
            case "auction_invite", "auction_invite_or_reputation" -> ModItems.AUCTION_INVITE.get();
            case "void_crystal" -> ModItems.VOID_CRYSTAL.get();
            case "void_palace_key_fragment" -> ModItems.VOID_PALACE_KEY_FRAGMENT.get();
            case "void_bell_fragment", "cycle_void_palace_open" -> ModItems.VOID_BELL_FRAGMENT.get();
            case "space_rift_compass" -> ModItems.SPACE_RIFT_COMPASS.get();
            case "leyline_compass" -> ModItems.LEYLINE_COMPASS.get();
            case "spirit_stone", "spirit_stone_shard", "cost_spirit_stone" ->
                    ModItems.SPIRIT_STONE_SHARD.get();
            case "inverse_star_contact", "inverse_star_contact_or_smuggle" ->
                    ModItems.INVERSE_STAR_CONTACT.get();
            case "sect_yanyue", "sect_qingxu" -> ModItems.SECT_PERMIT.get();
            default -> {
                // Direct registry id attempt.
                ResourceLocation loc = ResourceLocation.tryParse(
                        t.contains(":") ? t : "seeking_immortals:" + t);
                if (loc == null || ForgeRegistries.ITEMS == null) {
                    yield null;
                }
                Item item = ForgeRegistries.ITEMS.getValue(loc);
                yield item == null || item == Items.AIR ? null : item;
            }
        };
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

}
