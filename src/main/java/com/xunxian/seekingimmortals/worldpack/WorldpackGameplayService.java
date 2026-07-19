package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.block.PortalArrayStructure;
import com.xunxian.seekingimmortals.item.StarPalaceTaxReceiptItem;
import com.xunxian.seekingimmortals.network.SyncWorldpackDataPacket;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.shop.ShopService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class WorldpackGameplayService {
    public static final String DEFAULT_REGION_ID = "qinglan_mountains";
    public static final String ACTION_SYNC = "sync";
    public static final String ACTION_TRAVEL = "travel";
    public static final String ACTION_ENTER = "enter";
    public static final String ACTION_RETURN = "return";

    public static final String EFFECT_AURA_PLUS_5 = "aura_plus_5";
    public static final String EFFECT_SPIRIT_RAIN_BONUS = "spirit_rain_bonus";
    public static final String EFFECT_HERB_SHOP_BONUS = "herb_shop_bonus";
    public static final String EFFECT_SECRET_REALM_TICKET_HINT = "secret_realm_ticket_hint";
    public static final String EFFECT_TRADE_RISK_UP = "trade_risk_up";
    public static final String EFFECT_SECT_CONTRIBUTION_BONUS = "sect_contribution_bonus";
    public static final String ROUTE_HINT_PORTAL_ARRAY = "screen.seeking_immortals.worldpack.route.portal_array";
    public static final String ROUTE_HINT_PORTAL_ARRAY_FEE = "screen.seeking_immortals.worldpack.route.portal_array_fee";
    public static final String ROUTE_HINT_DEMON_RIFT_EVENT = "screen.seeking_immortals.worldpack.route.demon_rift_event";
    public static final String ROUTE_HINT_WIND_FEATHER_RAFT = "screen.seeking_immortals.worldpack.route.wind_feather_raft";
    public static final String ROUTE_HINT_NETHER_FERRY_FEE = "screen.seeking_immortals.worldpack.route.nether_ferry_fee";
    public static final String ROUTE_HINT_NETHER_FERRY_RETURN = "screen.seeking_immortals.worldpack.route.nether_ferry_return";
    private static final double STAR_PALACE_ISLAND_MARKET_TAX_MULTIPLIER = 1.10D;
    private static final String TIANYUAN_REGION_ID = "tianyuan";
    private static final String SPIRIT_FENGYUAN_REGION_ID = "spirit_fengyuan";
    private static final String NETHER_RIVER_REGION_ID = "nether_river";
    private static final String YINMING_REGION_ID = "yinming";
    private static final String FALLEN_DEMON_REGION_ID = "fallen_demon_valley";
    private static final String GREAT_JIN_CENTRAL_REGION_ID = "great_jin_central";
    private static final String DAJIN_REGION_ID = "dajin";
    private static final String ANCIENT_DEMON_SEAL_BREACH_EVENT_ID = "ancient_demon_seal_breach";
    private static final String WIND_FEATHER_RAFT_TICKET_ITEM = "seeking_immortals:wind_feather_raft_ticket";
    private static final String ALLIANCE_MERIT_TOKEN_ITEM = "seeking_immortals:alliance_merit_token";
    private static final String YIN_STONE_ITEM = "seeking_immortals:yin_stone";
    private static final String SECRET_REALM_TAG_NO_FLY = "no_fly";
    private static final int NETHER_RIVER_FERRY_YIN_STONE_FEE = 30;
    private static final int DEFAULT_PORTAL_PLATFORM_RADIUS = PortalArrayStructure.BASE_RADIUS;
    private static final int DEFAULT_PORTAL_APERTURE_RADIUS = PortalArrayStructure.APERTURE_RADIUS;
    private static final int DEFAULT_PORTAL_APERTURE_HEIGHT = PortalArrayStructure.APERTURE_HEIGHT;
    private static final int DEFAULT_PORTAL_FRAME_HEIGHT = PortalArrayStructure.FRAME_HEIGHT;
    private static final List<String> STAR_PALACE_ISLAND_MARKET_TAX_SHOPS = List.of(
            ShopService.CHAOTIC_SEA_ISLAND_GENERAL,
            ShopService.OUTER_SEA_PUBLIC_STALL);
    private static final List<DefaultDimensionAnchor> DEFAULT_DIMENSION_ANCHORS = List.of(
            new DefaultDimensionAnchor("tianyuan_anchor", "seeking_immortals:tianyuan", 0, 0),
            new DefaultDimensionAnchor("spirit_realm_border_anchor", "seeking_immortals:tianyuan", 160, 0),
            new DefaultDimensionAnchor("fengyuan_anchor", "seeking_immortals:spirit_fengyuan", 0, 0),
            new DefaultDimensionAnchor("barbarian_wasteland_anchor", "seeking_immortals:spirit_fengyuan", 160, 0),
            new DefaultDimensionAnchor("yinming_gate_anchor", "seeking_immortals:yin_ming_pocket", 0, 0),
            new DefaultDimensionAnchor("nether_ferry_anchor", "seeking_immortals:nether_river_pocket", 160, 0),
            new DefaultDimensionAnchor("fallen_demon_anchor", "seeking_immortals:demon_rift", 0, 0)
    );

    private WorldpackGameplayService() {}

    public record RouteRequirement(String translationKey, int amount, String itemId) {
        public static final RouteRequirement NONE = new RouteRequirement("", 0, "");

        public boolean isPresent() {
            return !translationKey.isBlank();
        }
    }

    public static void openScreen(ServerPlayer player) {
        sync(player, true);
    }

    public static void sync(ServerPlayer player, boolean openScreen) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
            WorldpackSavedData savedData = WorldpackSavedData.get(player.server.overworld());
            String regionId = normalizeRegion(cultivation, snapshot);
            WorldpackSavedData.EventRoll eventRoll = refreshDailyEvent(player, cultivation, savedData, snapshot, regionId);
            WorldpackSnapshot data = buildSnapshot(player, cultivation, savedData, snapshot, eventRoll, openScreen);
            SyncWorldpackDataPacket.send(player, toPacket(data));
            sendSummary(player, data);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
    }

    /**
     * Lifecycle mirror only: reads existing world/player worldpack state without creating anchors,
     * platforms, or rolling/refreshing daily events.
     */
    public static void syncSnapshot(ServerPlayer player) {
        if (player == null || player.server == null) {
            return;
        }
        CultivationHelper.get(player).ifPresent(cultivation -> {
            WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
            WorldpackSavedData savedData = WorldpackSavedData.get(player.server.overworld());
            String regionId = peekRegionId(cultivation, snapshot);
            WorldpackSavedData.EventRoll eventRoll = peekDailyEvent(player, cultivation, savedData, regionId);
            SyncWorldpackDataPacket.send(player, toPacket(
                    buildSnapshot(player, cultivation, savedData, snapshot, eventRoll, false)));
        });
    }

    /** Read-only region id for lifecycle mirrors; never mutates the current-region fallback. */
    private static String peekRegionId(PlayerCultivation cultivation, WorldpackDataService.Snapshot snapshot) {
        String regionId = cultivation.getWorldpackCurrentRegionId();
        if (snapshot.findRegion(regionId).isPresent()
                || com.xunxian.seekingimmortals.region.RegionRegistry.isKnown(regionId)) {
            return regionId;
        }
        return DEFAULT_REGION_ID;
    }

    /** Read-only daily-event mirror: prefer existing saved/player roll, never roll or dispatch hooks. */
    private static WorldpackSavedData.EventRoll peekDailyEvent(ServerPlayer player, PlayerCultivation cultivation,
                                                                WorldpackSavedData savedData, String regionId) {
        String resolved = regionId == null ? "" : regionId;
        long gameTime = player != null && player.server != null
                ? player.server.overworld().getGameTime()
                : 0L;
        Optional<WorldpackSavedData.EventRoll> saved = savedData == null
                ? Optional.empty()
                : savedData.peekDailyEvent(resolved);
        if (saved.isPresent() && (gameTime <= 0L || saved.get().isActive(gameTime))) {
            return saved.get();
        }
        String playerEvent = cultivation.getWorldpackActiveDailyEventId();
        long until = cultivation.getWorldpackActiveDailyEventUntilTick();
        if (playerEvent != null && !playerEvent.isBlank() && (gameTime <= 0L || until > gameTime)) {
            return new WorldpackSavedData.EventRoll(resolved, playerEvent, until);
        }
        return saved.orElseGet(() -> new WorldpackSavedData.EventRoll(resolved, "", 0L));
    }

    public static void handleClientAction(ServerPlayer player, String action, String targetId) {
        String normalizedAction = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        switch (normalizedAction) {
            case ACTION_SYNC -> syncSnapshot(player);
            case ACTION_TRAVEL -> travel(player, targetId);
            case ACTION_ENTER -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.gate_required"));
            case ACTION_RETURN -> returnFromSecretRealm(player);
            default -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.unknown_action", normalizedAction));
        }
    }

    public static boolean travel(ServerPlayer player, String regionId) {
        return travel(player, regionId, false);
    }

    private static boolean travel(ServerPlayer player, String regionId, boolean fromPortalArray) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        Optional<WorldpackDataService.RegionCard> regionOptional = snapshot.findRegion(regionId == null ? "" : regionId);
        if (regionOptional.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.unknown_region", regionId));
            return false;
        }
        WorldpackDataService.RegionCard region = regionOptional.get();
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            String currentRegion = normalizeRegion(cultivation, snapshot);
            if (!meetsMinRealm(cultivation.getRealm(), region.minRealm())) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.realm_too_low", region.minRealm()));
                return;
            }
            if (!cultivation.getWorldpackActiveSecretRealmId().isBlank()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.must_return_first"));
                return;
            }
            WorldpackSavedData savedData = prepareSavedData(player, snapshot);
            if (requiresDemonRiftEventGate(region)) {
                if (!fromPortalArray) {
                    player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.demon_rift_portal_required"));
                    return;
                }
                WorldpackSavedData.EventRoll roll = savedData.getOrRollDailyEvent(
                        region.id(), snapshot, gameTime(player), player.getRandom());
                if (!isAncientDemonSealBreach(snapshot, roll)) {
                    player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.demon_rift_seal_closed"));
                    return;
                }
            }
            Optional<WorldpackSavedData.Anchor> anchor = savedData.getAnchor(region.travelAnchor());
            if (anchor.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.missing_anchor", region.travelAnchor()));
                return;
            }
            if (!canTeleportToAnchor(player, anchor.get())) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.bad_anchor", region.travelAnchor()));
                return;
            }
            TravelCostReservation travelCosts = reserveTravelCosts(player, currentRegion, region, fromPortalArray);
            if (travelCosts == null) {
                return;
            }
            if (!teleportToAnchor(player, anchor.get())) {
                travelCosts.refund(player);
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.bad_anchor", region.travelAnchor()));
                return;
            }
            cultivation.setWorldpackCurrentRegionId(region.id());
            cultivation.setWorldpackActiveSecretRealmId("");
            cultivation.clearWorldpackReturnLocation();
            refreshDailyEvent(player, cultivation, savedData, snapshot, region.id());
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.travel_success", display(region)));
            ReputationService.onPortalTravel(player, region.id());
            sync(player, false);
            success[0] = true;
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return success[0];
    }

    public static boolean enterSecretRealm(ServerPlayer player, String realmId) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        Optional<WorldpackDataService.SecretRealm> realmOptional = snapshot.findSecretRealm(realmId == null ? "" : realmId);
        if (realmOptional.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.unknown_realm", realmId));
            return false;
        }
        WorldpackDataService.SecretRealm realm = realmOptional.get();
        Optional<WorldpackDataService.RegionCard> regionOptional = snapshot.findRegion(realm.regionId());
        if (regionOptional.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.unknown_region", realm.regionId()));
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            if (!cultivation.getWorldpackActiveSecretRealmId().isBlank()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.already_in_realm", cultivation.getWorldpackActiveSecretRealmId()));
                return;
            }
            if (!cultivation.getWorldpackCurrentRegionId().equals(realm.regionId())) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.wrong_region", realm.regionId()));
                return;
            }
            if (!meetsMinRealm(cultivation.getRealm(), realm.minRealm())
                    && !ProgressionGateApi.meetsRealm(player, realm.minRealm())) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.realm_too_low", realm.minRealm()));
                return;
            }
            Optional<String> openDenied = SecretRealmSessionService.validateOpen(player, realm.id());
            if (openDenied.isPresent()) {
                String reason = openDenied.get();
                if (reason.startsWith("realm_too_low:")) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.realm_too_low", reason.substring("realm_too_low:".length())));
                } else if (reason.startsWith("party_full:")) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.party_full", reason.substring("party_full:".length())));
                } else if (reason.startsWith("window_closed:")) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.window_closed", reason.substring("window_closed:".length())));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.enter_denied", reason));
                }
                return;
            }
            long now = gameTime(player);
            long cooldownUntil = cultivation.getWorldpackCooldownUntil(realm.id());
            if (cooldownUntil > now) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.cooldown", (cooldownUntil - now + 19L) / 20L));
                return;
            }
            boolean dedicatedDimension = SecretRealmDimensionService.hasDedicatedDimension(player, realm.id());
            WorldpackSavedData savedData = dedicatedDimension
                    ? WorldpackSavedData.get(player.server.overworld())
                    : prepareSavedData(player, snapshot);
            WorldpackDataService.RegionCard region = regionOptional.get();
            Optional<WorldpackSavedData.Anchor> anchor = Optional.empty();
            if (!dedicatedDimension) {
                anchor = savedData.getAnchor(region.travelAnchor());
                if (anchor.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.missing_anchor", region.travelAnchor()));
                    return;
                }
                if (!canTeleportToAnchor(player, anchor.get())) {
                    player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.bad_anchor", region.travelAnchor()));
                    return;
                }
            }
            WorldpackSavedData.EventRoll roll = refreshDailyEvent(player, cultivation, savedData, snapshot, realm.regionId());
            boolean ticketDiscount = activeEffects(snapshot, roll).contains(EFFECT_SECRET_REALM_TICKET_HINT);
            if (!hasTicket(player, realm.ticketItem())) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.missing_ticket", realm.ticketItem()));
                return;
            }
            InventoryReservation ticketReservation = InventoryReservation.none();
            if (!ticketDiscount) {
                Item ticket = resolveItem(realm.ticketItem());
                ticketReservation = ticket == null || ticket == Items.AIR
                        ? null
                        : InventoryReservation.consume(player, Map.of(ticket, 1));
                if (ticketReservation == null) {
                    player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.missing_ticket", realm.ticketItem()));
                    return;
                }
            }
            String returnDimension = player.level().dimension().location().toString();
            double returnX = player.getX();
            double returnY = player.getY();
            double returnZ = player.getZ();
            float returnYRot = player.getYRot();
            float returnXRot = player.getXRot();
            // Wave47: prefer dedicated secret-realm dimension pack when available.
            boolean entered = dedicatedDimension
                    ? SecretRealmDimensionService.teleportInto(player, realm.id())
                    : teleportToAnchor(player, anchor.orElseThrow());
            if (entered && dedicatedDimension) {
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.enter_dedicated_dimension",
                        SecretRealmDimensionService.dimensionIdFor(realm.id()).orElse("")));
            }
            if (!entered) {
                ticketReservation.refund(player);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.enter_teleport_failed", display(realm)));
                return;
            }
            cultivation.setWorldpackReturnLocation(returnDimension,
                    returnX, returnY, returnZ, returnYRot, returnXRot);
            cultivation.setWorldpackActiveSecretRealmId(realm.id());
            cultivation.setWorldpackCurrentRegionId(realm.regionId());
            if (realm.cooldownTicks() > 0) {
                cultivation.setWorldpackCooldownUntil(realm.id(), now + realm.cooldownTicks());
            }
            player.sendSystemMessage(Component.translatable(ticketDiscount
                    ? "message.seeking_immortals.worldpack.enter_discount"
                    : "message.seeking_immortals.worldpack.enter_success", display(realm)));
            TextMaterialCatalogService.builtin().findFlavor(realm.id()).ifPresent(flavor -> {
                if (!flavor.openCondition().isBlank()) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.realm_open_condition", flavor.openCondition()));
                }
                if (!flavor.environment().isBlank()) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.realm_environment", flavor.environment()));
                }
                if (!flavor.rareDrops().isEmpty()) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.realm_rare_drops",
                            String.join(", ", flavor.rareDrops())));
                }
            });
            SecretRealmSessionService.onEnter(player, realm.id());
            SecretRealmTrialService.onEnter(player, realm.id());
            sync(player, false);
            success[0] = true;
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return success[0];
    }

    public static boolean usePortalArray(ServerPlayer player) {
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            if (!cultivation.getWorldpackActiveSecretRealmId().isBlank()) {
                success[0] = returnFromSecretRealm(player);
                return;
            }
            WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
            String currentRegion = normalizeRegion(cultivation, snapshot);
            WorldpackSavedData savedData = prepareSavedData(player, snapshot);
            boolean demonRiftSealOpen = isDemonRiftSealOpen(player, savedData, snapshot);
            success[0] = travel(player, choosePortalArrayDestination(currentRegion, snapshot, demonRiftSealOpen), true);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return success[0];
    }

    /** pocket_gate / nether ferry multiblock activation. */
    public static boolean useNetherFerryGate(ServerPlayer player) {
        return useNetherFerryGate(player, true);
    }

    public static boolean useNetherFerryGate(ServerPlayer player, boolean enforceRequires) {
        Optional<Boolean> returnResult = returnIfSecretRealmActive(player);
        if (returnResult.isPresent()) {
            return returnResult.get();
        }
        SpatialNodeRequiresService.Reservation reservation = reserveSpatialNode(player, "pocket_gate", enforceRequires);
        if (reservation == null) {
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            success[0] = enterBoundRealmOr(player, "nether_ferry_gate", () -> {
                WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
                String current = normalizeRegion(cultivation, snapshot);
                String destination = NETHER_RIVER_REGION_ID.equals(current)
                        ? DEFAULT_REGION_ID
                        : NETHER_RIVER_REGION_ID;
                return travel(player, destination, true);
            });
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return completeSpatialNodeUse(player, reservation, success[0]);
    }

    /** ancient_rift multiblock activation (fallen demon valley route). */
    public static boolean useAncientRiftGate(ServerPlayer player) {
        return useAncientRiftGate(player, true);
    }

    public static boolean useAncientRiftGate(ServerPlayer player, boolean enforceRequires) {
        Optional<Boolean> returnResult = returnIfSecretRealmActive(player);
        if (returnResult.isPresent()) {
            return returnResult.get();
        }
        SpatialNodeRequiresService.Reservation reservation = reserveSpatialNode(player, "ancient_rift", enforceRequires);
        if (reservation == null) {
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            success[0] = enterBoundRealmOr(player, "ancient_rift_gate", () -> {
                WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
                String current = normalizeRegion(cultivation, snapshot);
                String destination = FALLEN_DEMON_REGION_ID.equals(current)
                        ? DEFAULT_REGION_ID
                        : FALLEN_DEMON_REGION_ID;
                return travel(player, destination, true);
            });
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return completeSpatialNodeUse(player, reservation, success[0]);
    }

    private static final String CHAOTIC_SEA_REGION_ID = "chaotic_sea";
    private static final String INVERSE_STAR_HIDEOUT_REGION_ID = "inverse_star_hideout";

    /** Wave462: ascension_gate with realm/tribulation requires, routes to Tianyuan. */
    public static boolean useAscensionGate(ServerPlayer player) {
        return useAscensionGate(player, true);
    }

    public static boolean useAscensionGate(ServerPlayer player, boolean enforceRequires) {
        Optional<Boolean> returnResult = returnIfSecretRealmActive(player);
        if (returnResult.isPresent()) {
            return returnResult.get();
        }
        SpatialNodeRequiresService.Reservation reservation = reserveSpatialNode(player, "ascension_gate", enforceRequires);
        if (reservation == null) {
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            // M09: prefer high-realm secret bindings; fallback is spirit-realm hub travel.
            success[0] = enterBoundRealmOr(player, "ascension_gate", () -> {
                boolean hopped = travel(player, TIANYUAN_REGION_ID, true);
                if (hopped) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.ascension_gate_travel"));
                }
                return hopped;
            });
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return completeSpatialNodeUse(player, reservation, success[0]);
    }

    /** Wave462: blood_forbidden_gate enters blood_forbidden secret realm (ticket gated). */
    public static boolean useBloodForbiddenGate(ServerPlayer player) {
        return useBloodForbiddenGate(player, true);
    }

    public static boolean useBloodForbiddenGate(ServerPlayer player, boolean enforceRequires) {
        Optional<Boolean> returnResult = returnIfSecretRealmActive(player);
        if (returnResult.isPresent()) {
            return returnResult.get();
        }
        SpatialNodeRequiresService.Reservation reservation = reserveSpatialNode(player, "blood_forbidden_gate", enforceRequires);
        if (reservation == null) {
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            success[0] = enterBoundRealmOr(player, "blood_forbidden_gate",
                    () -> {
                        if (enterSecretRealm(player, "blood_forbidden")) {
                            return true;
                        }
                        boolean corridor = travel(player, FALLEN_DEMON_REGION_ID, true);
                        if (corridor) {
                            player.sendSystemMessage(Component.translatable(
                                    "message.seeking_immortals.worldpack.blood_gate_corridor"));
                        }
                        return corridor;
                    });
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return completeSpatialNodeUse(player, reservation, success[0]);
    }

    /** cycle_gate multiblock activation (void palace via chaotic_sea). */
    public static boolean useCycleGate(ServerPlayer player) {
        return useCycleGate(player, true);
    }

    public static boolean useCycleGate(ServerPlayer player, boolean enforceRequires) {
        Optional<Boolean> returnResult = returnIfSecretRealmActive(player);
        if (returnResult.isPresent()) {
            return returnResult.get();
        }
        SpatialNodeRequiresService.Reservation reservation = reserveSpatialNode(player, "cycle_gate", enforceRequires);
        if (reservation == null) {
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            success[0] = enterBoundRealmOr(player, "cycle_gate", () -> {
                WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
                String current = normalizeRegion(cultivation, snapshot);
                if (!CHAOTIC_SEA_REGION_ID.equals(current)) {
                    boolean hopped = travel(player, CHAOTIC_SEA_REGION_ID, true);
                    if (hopped) {
                        player.sendSystemMessage(Component.translatable(
                                "message.seeking_immortals.worldpack.cycle_gate_to_sea"));
                    }
                    return hopped;
                }
                if (enterSecretRealm(player, "void_palace")) {
                    return true;
                }
                return travel(player, DEFAULT_REGION_ID, true);
            });
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return completeSpatialNodeUse(player, reservation, success[0]);
    }

    /** hidden_rift multiblock activation (inverse-star hideout). */
    public static boolean useHiddenRiftGate(ServerPlayer player) {
        return useHiddenRiftGate(player, true);
    }

    public static boolean useHiddenRiftGate(ServerPlayer player, boolean enforceRequires) {
        Optional<Boolean> returnResult = returnIfSecretRealmActive(player);
        if (returnResult.isPresent()) {
            return returnResult.get();
        }
        SpatialNodeRequiresService.Reservation reservation = reserveSpatialNode(player, "hidden_rift", enforceRequires);
        if (reservation == null) {
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            success[0] = enterBoundRealmOr(player, "hidden_rift_gate", () -> {
                WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
                String current = normalizeRegion(cultivation, snapshot);
                String destination = INVERSE_STAR_HIDEOUT_REGION_ID.equals(current)
                        ? CHAOTIC_SEA_REGION_ID
                        : INVERSE_STAR_HIDEOUT_REGION_ID;
                return travel(player, destination, true);
            });
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return completeSpatialNodeUse(player, reservation, success[0]);
    }

    private static final String BARBARIAN_WASTELAND_REGION_ID = "barbarian_wasteland";

    /** king_territory shared placeable for 7 catalog nodes. */
    public static boolean useKingTerritoryGate(ServerPlayer player) {
        return useKingTerritoryGate(player, true);
    }

    public static boolean useKingTerritoryGate(ServerPlayer player, boolean enforceRequires) {
        Optional<Boolean> returnResult = returnIfSecretRealmActive(player);
        if (returnResult.isPresent()) {
            return returnResult.get();
        }
        SpatialNodeRequiresService.Reservation reservation = reserveSpatialNode(player, "king_territory", enforceRequires);
        if (reservation == null) {
            return false;
        }
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            success[0] = enterBoundRealmOr(player, "king_territory_gate", () -> {
                WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
                String current = normalizeRegion(cultivation, snapshot);
                String destination = BARBARIAN_WASTELAND_REGION_ID.equals(current)
                        ? SPIRIT_FENGYUAN_REGION_ID
                        : BARBARIAN_WASTELAND_REGION_ID;
                boolean hopped = travel(player, destination, true);
                if (hopped && BARBARIAN_WASTELAND_REGION_ID.equals(destination)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.worldpack.king_territory_hint"));
                }
                return hopped;
            });
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return completeSpatialNodeUse(player, reservation, success[0]);
    }

    private static Optional<Boolean> returnIfSecretRealmActive(ServerPlayer player) {
        boolean[] active = { false };
        boolean[] returned = { false };
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!cultivation.getWorldpackActiveSecretRealmId().isBlank()) {
                active[0] = true;
                returned[0] = returnFromSecretRealm(player);
            }
        });
        return active[0] ? Optional.of(returned[0]) : Optional.empty();
    }

    private static SpatialNodeRequiresService.Reservation reserveSpatialNode(ServerPlayer player,
                                                                              String nodeType,
                                                                              boolean enforceRequires) {
        return enforceRequires
                ? SpatialNodeRequiresService.reserveByType(player, nodeType)
                : SpatialNodeRequiresService.Reservation.none();
    }

    private static boolean completeSpatialNodeUse(ServerPlayer player,
                                                  SpatialNodeRequiresService.Reservation reservation,
                                                  boolean success) {
        if (!success && reservation != null) {
            reservation.refund(player);
        }
        return success;
    }

    public static boolean returnFromSecretRealm(ServerPlayer player) {
        boolean[] success = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            if (cultivation.getWorldpackActiveSecretRealmId().isBlank()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.not_in_realm"));
                return;
            }
            if (!cultivation.hasWorldpackReturnLocation()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_return"));
                return;
            }
            ResourceLocation dimension = ResourceLocation.tryParse(cultivation.getWorldpackReturnDimension());
            if (dimension == null) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_return"));
                return;
            }
            ServerLevel targetLevel = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (targetLevel == null) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_return"));
                return;
            }
            player.teleportTo(targetLevel, cultivation.getWorldpackReturnX(), cultivation.getWorldpackReturnY(), cultivation.getWorldpackReturnZ(),
                    cultivation.getWorldpackReturnYRot(), cultivation.getWorldpackReturnXRot());
            if (player.serverLevel() != targetLevel
                    || player.distanceToSqr(cultivation.getWorldpackReturnX(), cultivation.getWorldpackReturnY(),
                    cultivation.getWorldpackReturnZ()) > 16.0D) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.return_teleport_failed"));
                return;
            }
            cultivation.setWorldpackActiveSecretRealmId("");
            cultivation.clearWorldpackReturnLocation();
            SecretRealmTrialService.onReturn(player);
            SecretRealmSessionService.onLeave(player);
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.return_success"));
            sync(player, false);
            success[0] = true;
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.no_data")));
        return success[0];
    }

    /**
     * M09: gate block → bound secret realm enter (ticket/realm/window validated server-side).
     * Falls back to the provided travel runnable when no catalog binding exists.
     */
    public static boolean enterBoundRealmOr(ServerPlayer player, String gateBlockId, java.util.function.Supplier<Boolean> fallback) {
        Optional<SecretRealmCatalogService.RealmDef> bound = SecretRealmCatalogService.primaryRealmForGate(gateBlockId);
        if (bound.isEmpty()) {
            return fallback != null && Boolean.TRUE.equals(fallback.get());
        }
        return enterSecretRealm(player, bound.get().id());
    }

    public static boolean setAnchor(ServerPlayer player, String anchorId) {
        if (anchorId == null || anchorId.isBlank()) {
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.bad_anchor_id"));
            return false;
        }
        WorldpackSavedData savedData = WorldpackSavedData.get(player.server.overworld());
        savedData.setAnchor(anchorId, player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.anchor_set", anchorId));
        sync(player, false);
        return true;
    }

    public static int applyAuraBonus(ServerPlayer player, PlayerCultivation cultivation, int adjustedGain) {
        return adjustAuraGainForEffects(adjustedGain, activeEffects(player, cultivation));
    }

    public static int adjustAuraGainForEffects(int adjustedGain, List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return adjustedGain;
        }
        int bonusPercent = 0;
        if (effects.contains(EFFECT_AURA_PLUS_5)) {
            bonusPercent += 5;
        }
        if (effects.contains(EFFECT_SPIRIT_RAIN_BONUS)) {
            bonusPercent += 10;
        }
        if (bonusPercent <= 0) {
            return adjustedGain;
        }
        long scaled = (long) adjustedGain * (100L + bonusPercent);
        int boosted = (int) Math.min(Integer.MAX_VALUE, (scaled + 99L) / 100L);
        return Math.max(adjustedGain, boosted);
    }

    public static ShopService.CostModifier marketCostModifier(ServerPlayer player) {
        return (shopId, entry, baseCost) -> {
            int effectAdjusted = CultivationHelper.get(player)
                    .map(cultivation -> adjustMarketCostForShop(shopId, entry, baseCost,
                            activeEffects(player, cultivation),
                            StarPalaceTaxReceiptItem.hasPaidIslandTradeTax(cultivation)))
                    .orElseGet(() -> adjustMarketCostForShop(shopId, entry, baseCost, List.of(), false));
            // Wave46: reputation discount table (friendly 5%, honored 12%, max 20%).
            double repDiscount = ReputationService.shopDiscountMultiplier(player, shopId);
            // M08: active faction conflict tax/blockade multiplies market prices.
            double conflictMul = com.xunxian.seekingimmortals.sect.FactionConflictEventService
                    .activePriceMultiplier(player);
            return Math.max(1, (int) Math.round(effectAdjusted * repDiscount * conflictMul));
        };
    }

    public static int adjustMarketCostForEffects(int baseCost, List<String> effects) {
        return adjustMarketCostForShop("", null, baseCost, effects, true);
    }

    public static int adjustMarketCostForShop(String shopId, ShopService.Entry entry, int baseCost,
                                              List<String> effects, boolean starPalaceIslandTaxPaid) {
        double multiplier = 1.0D;
        List<String> safeEffects = effects == null ? List.of() : effects;
        if (safeEffects.contains(EFFECT_HERB_SHOP_BONUS)) {
            multiplier *= 0.75D;
        }
        if (safeEffects.contains(EFFECT_TRADE_RISK_UP)) {
            multiplier *= 1.50D;
        }
        if (appliesStarPalaceIslandMarketTax(shopId, entry, starPalaceIslandTaxPaid)) {
            multiplier *= STAR_PALACE_ISLAND_MARKET_TAX_MULTIPLIER;
        }
        return Math.max(1, (int)Math.ceil(baseCost * multiplier));
    }

    private static boolean appliesStarPalaceIslandMarketTax(String shopId, ShopService.Entry entry,
                                                            boolean starPalaceIslandTaxPaid) {
        return !starPalaceIslandTaxPaid
                && STAR_PALACE_ISLAND_MARKET_TAX_SHOPS.contains(ShopService.canonicalMarketShopId(shopId))
                && entry != null
                && ShopService.CURRENCY_ITEM.equals(entry.currency())
                && !"star_palace_tax_receipt".equals(entry.id());
    }

    public static int applySectContributionBonus(ServerPlayer player, PlayerCultivation cultivation, int baseReward) {
        return adjustSectContributionReward(baseReward, activeEffects(player, cultivation));
    }

    public static int adjustSectContributionReward(int baseReward, List<String> effects) {
        if (baseReward <= 0) {
            return 0;
        }
        if (!effects.contains(EFFECT_SECT_CONTRIBUTION_BONUS)) {
            return baseReward;
        }
        return Math.max(baseReward, (int)Math.ceil(baseReward * 1.5D));
    }

    public static boolean meetsMinRealm(Realm currentRealm, String minRealmId) {
        if (currentRealm == null) {
            return false;
        }
        if (minRealmId == null || minRealmId.isBlank()) {
            return true;
        }
        Realm minRealm = parseRealm(minRealmId);
        return minRealm != null && currentRealm.ordinal() >= minRealm.ordinal();
    }

    public static boolean isFlightSuppressed(PlayerCultivation cultivation) {
        if (cultivation == null || cultivation.getWorldpackActiveSecretRealmId().isBlank()) {
            return false;
        }
        return WorldpackDataService.builtin()
                .findSecretRealm(cultivation.getWorldpackActiveSecretRealmId())
                .map(realm -> realm.tags().contains(SECRET_REALM_TAG_NO_FLY))
                .orElse(false);
    }

    static Realm parseRealm(String value) {
        return Realm.fromDesignId(value);
    }

    private static WorldpackSnapshot buildSnapshot(ServerPlayer player, PlayerCultivation cultivation,
                                                   WorldpackSavedData savedData, WorldpackDataService.Snapshot snapshot,
                                                   WorldpackSavedData.EventRoll eventRoll, boolean openScreen) {
        long now = gameTime(player);
        String currentRegion = normalizeRegion(cultivation, snapshot);
        List<RegionData> regions = snapshot.regions().stream()
                .map(region -> new RegionData(
                        region.id(),
                        display(region),
                        region.minRealm(),
                        region.auraMultiplier(),
                        savedData.hasAnchor(region.travelAnchor()),
                        currentRegion.equals(region.id())))
                .toList();
        List<RealmData> realms = snapshot.secretRealms().stream()
                .map(realm -> {
                    WorldpackDataService.RegionCard region = snapshot.findRegion(realm.regionId()).orElse(null);
                    boolean anchorReady = region != null && savedData.hasAnchor(region.travelAnchor());
                    long remaining = Math.max(0L, cultivation.getWorldpackCooldownUntil(realm.id()) - now);
                    return new RealmData(
                            realm.id(),
                            display(realm),
                            realm.regionId(),
                            realm.minRealm(),
                            itemDescriptionId(realm.ticketItem()),
                            remaining,
                            anchorReady,
                            currentRegion.equals(realm.regionId()),
                            cultivation.getWorldpackActiveSecretRealmId().equals(realm.id()));
                })
                .toList();
        String eventId = eventRoll.eventId();
        String eventDisplay = snapshot.findDailyEvent(eventId).map(WorldpackGameplayService::display).orElseGet(() -> {
            for (WorldpackDataService.DailyEvent event : com.xunxian.seekingimmortals.region.DailyEventScheduler
                    .expandedCandidates(currentRegion, snapshot)) {
                if (event.id().equals(eventId)) {
                    return display(event);
                }
            }
            return eventId;
        });
        long eventRemaining = Math.max(0L, eventRoll.untilTick() - now);
        List<String> effects = activeEffects(snapshot, eventRoll);
        return new WorldpackSnapshot(
                currentRegion,
                snapshot.findRegion(currentRegion).map(WorldpackGameplayService::display).orElse(currentRegion),
                cultivation.getWorldpackActiveSecretRealmId(),
                snapshot.findSecretRealm(cultivation.getWorldpackActiveSecretRealmId()).map(WorldpackGameplayService::display).orElse(""),
                eventId,
                eventDisplay,
                eventRemaining,
                effects,
                regions,
                realms,
                openScreen);
    }

    private static void sendSummary(ServerPlayer player, WorldpackSnapshot data) {
        player.sendSystemMessage(Component.translatable(
                "screen.seeking_immortals.worldpack.current_region",
                data.currentRegionDisplay()));
        if (!data.dailyEventId().isBlank()) {
            player.sendSystemMessage(Component.translatable(
                    "screen.seeking_immortals.worldpack.daily_event",
                    data.dailyEventDisplay(),
                    Math.max(0L, data.dailyEventRemainingTicks() / 20L)));
        }
        player.sendSystemMessage(Component.translatable(
                "command.seeking_immortals.worldpack.regions.header",
                data.regions().size()));
        player.sendSystemMessage(Component.translatable(
                "command.seeking_immortals.worldpack.realms.header",
                data.realms().size()));
    }

    private static SyncWorldpackDataPacket toPacket(WorldpackSnapshot data) {
        return new SyncWorldpackDataPacket(
                data.currentRegionId(),
                data.currentRegionDisplay(),
                data.activeSecretRealmId(),
                data.activeSecretRealmDisplay(),
                data.dailyEventId(),
                data.dailyEventDisplay(),
                data.dailyEventRemainingTicks(),
                data.dailyEventEffects(),
                data.regions().stream()
                        .map(region -> new SyncWorldpackDataPacket.RegionData(
                                region.id(),
                                region.display(),
                                region.minRealm(),
                                region.auraMultiplier(),
                                region.anchorReady(),
                                region.current()))
                        .toList(),
                data.realms().stream()
                        .map(realm -> new SyncWorldpackDataPacket.RealmData(
                                realm.id(),
                                realm.display(),
                                realm.regionId(),
                                realm.minRealm(),
                                realm.ticketDescriptionId(),
                                realm.remainingCooldownTicks(),
                                realm.anchorReady(),
                                realm.currentRegion(),
                                realm.active()))
                        .toList(),
                data.openScreen());
    }

    private static WorldpackSavedData.EventRoll refreshDailyEvent(ServerPlayer player, PlayerCultivation cultivation,
                                                                  WorldpackSavedData savedData, WorldpackDataService.Snapshot snapshot,
                                                                  String regionId) {
        // M06: expanded candidates + hook dispatch + encounter spawn live in DailyEventScheduler.
        WorldpackSavedData.EventRoll roll = com.xunxian.seekingimmortals.region.DailyEventScheduler
                .ensurePlayerEvent(player, regionId);
        cultivation.setWorldpackDailyEvent(roll.eventId(), roll.untilTick());
        // M08: player-scoped faction conflict authority (rep/price). Region-wide hooks fire inside scheduler.
        com.xunxian.seekingimmortals.sect.FactionConflictEventService.onDailyEvent(player, regionId, roll.eventId());
        return roll;
    }

    private static List<String> activeEffects(ServerPlayer player, PlayerCultivation cultivation) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = prepareSavedData(player, snapshot);
        WorldpackSavedData.EventRoll roll = refreshDailyEvent(player, cultivation, savedData, snapshot, normalizeRegion(cultivation, snapshot));
        return activeEffects(snapshot, roll);
    }

    private static List<String> activeEffects(WorldpackDataService.Snapshot snapshot, WorldpackSavedData.EventRoll roll) {
        if (roll == null || roll.eventId().isBlank()) {
            return List.of();
        }
        Optional<WorldpackDataService.DailyEvent> direct = snapshot.findDailyEvent(roll.eventId());
        if (direct.isPresent()) {
            return direct.get().effects();
        }
        // Expanded multi-region text events may only exist as synthetic candidates.
        for (WorldpackDataService.DailyEvent event : com.xunxian.seekingimmortals.region.DailyEventScheduler
                .expandedCandidates(roll.regionId(), snapshot)) {
            if (roll.eventId().equals(event.id())) {
                return event.effects();
            }
        }
        return List.of();
    }

    private static String normalizeRegion(PlayerCultivation cultivation, WorldpackDataService.Snapshot snapshot) {
        String regionId = cultivation.getWorldpackCurrentRegionId();
        if (snapshot.findRegion(regionId).isPresent()) {
            return regionId;
        }
        // Prefer unified registry before falling back to starter region.
        if (com.xunxian.seekingimmortals.region.RegionRegistry.isKnown(regionId)) {
            return regionId;
        }
        cultivation.setWorldpackCurrentRegionId(DEFAULT_REGION_ID);
        return DEFAULT_REGION_ID;
    }

    static String choosePortalArrayDestination(String currentRegionId, WorldpackDataService.Snapshot snapshot) {
        return choosePortalArrayDestination(currentRegionId, snapshot, false);
    }

    static String choosePortalArrayDestination(String currentRegionId, WorldpackDataService.Snapshot snapshot,
                                               boolean demonRiftSealOpen) {
        if (FALLEN_DEMON_REGION_ID.equals(currentRegionId)) {
            return GREAT_JIN_CENTRAL_REGION_ID;
        }
        if (demonRiftSealOpen && canUseDemonRiftPortalOrigin(currentRegionId, snapshot)) {
            return FALLEN_DEMON_REGION_ID;
        }
        if (TIANYUAN_REGION_ID.equals(currentRegionId)) {
            return SPIRIT_FENGYUAN_REGION_ID;
        }
        return snapshot.findRegion(currentRegionId)
                .filter(region -> region.tags().contains("spirit_realm"))
                .map(region -> TIANYUAN_REGION_ID)
                .orElse(TIANYUAN_REGION_ID);
    }

    static boolean requiresPortalArray(WorldpackDataService.RegionCard region) {
        return region.tags().contains("spirit_realm");
    }

    static boolean requiresDemonRiftEventGate(WorldpackDataService.RegionCard region) {
        return region != null && region.tags().contains("demon_rift");
    }

    static boolean isAncientDemonSealBreach(WorldpackDataService.Snapshot snapshot, WorldpackSavedData.EventRoll roll) {
        return roll != null
                && ANCIENT_DEMON_SEAL_BREACH_EVENT_ID.equals(roll.eventId())
                && snapshot.findDailyEvent(roll.eventId()).isPresent();
    }

    private static boolean isDemonRiftSealOpen(ServerPlayer player, WorldpackSavedData savedData,
                                               WorldpackDataService.Snapshot snapshot) {
        WorldpackSavedData.EventRoll roll = savedData.getOrRollDailyEvent(
                FALLEN_DEMON_REGION_ID, snapshot, gameTime(player), player.getRandom());
        return isAncientDemonSealBreach(snapshot, roll);
    }

    /** Public helper for spatial requires (Wave43). */
    public static boolean isDemonRiftSealOpenFor(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(player.server.overworld());
        return isDemonRiftSealOpen(player, savedData, snapshot);
    }

    static boolean canUseDemonRiftPortalOrigin(String currentRegionId, WorldpackDataService.Snapshot snapshot) {
        String current = currentRegionId == null ? "" : currentRegionId.trim();
        if (GREAT_JIN_CENTRAL_REGION_ID.equals(current) || DAJIN_REGION_ID.equals(current)) {
            return true;
        }
        return snapshot.findRegion(current)
                .map(region -> region.tags().contains("dajin") || region.tags().contains("ancient_seal"))
                .orElse(false);
    }

    static boolean canUseWindFeatherRaftRoute(String currentRegionId, WorldpackDataService.RegionCard targetRegion) {
        return targetRegion != null
                && TIANYUAN_REGION_ID.equals(targetRegion.id())
                && (GREAT_JIN_CENTRAL_REGION_ID.equals(currentRegionId)
                || DAJIN_REGION_ID.equals(currentRegionId));
    }

    static boolean requiresPortalArrayTravelFee(String currentRegionId, WorldpackDataService.RegionCard targetRegion) {
        return targetRegion != null
                && TIANYUAN_REGION_ID.equals(currentRegionId)
                && SPIRIT_FENGYUAN_REGION_ID.equals(targetRegion.id());
    }

    static boolean requiresNetherRiverFerryFee(String currentRegionId, WorldpackDataService.RegionCard targetRegion) {
        return targetRegion != null
                && NETHER_RIVER_REGION_ID.equals(currentRegionId)
                && YINMING_REGION_ID.equals(targetRegion.id());
    }

    static int netherRiverFerryYinStoneFee(String currentRegionId, WorldpackDataService.RegionCard targetRegion) {
        return requiresNetherRiverFerryFee(currentRegionId, targetRegion) ? NETHER_RIVER_FERRY_YIN_STONE_FEE : 0;
    }

    public static RouteRequirement routeRequirementForDisplay(String currentRegionId, String targetRegionId) {
        String current = currentRegionId == null ? "" : currentRegionId.trim();
        String targetId = targetRegionId == null ? "" : targetRegionId.trim();
        if (targetId.isBlank() || current.equals(targetId)) {
            return RouteRequirement.NONE;
        }
        Optional<WorldpackDataService.RegionCard> targetOptional = WorldpackDataService.builtin().findRegion(targetId);
        if (targetOptional.isEmpty()) {
            return RouteRequirement.NONE;
        }
        WorldpackDataService.RegionCard targetRegion = targetOptional.get();
        if (canUseWindFeatherRaftRoute(current, targetRegion)) {
            return new RouteRequirement(ROUTE_HINT_WIND_FEATHER_RAFT, 0, WIND_FEATHER_RAFT_TICKET_ITEM);
        }
        if (requiresDemonRiftEventGate(targetRegion)) {
            return new RouteRequirement(ROUTE_HINT_DEMON_RIFT_EVENT, 0, "");
        }
        if (requiresPortalArray(targetRegion)) {
            if (requiresPortalArrayTravelFee(current, targetRegion)) {
                return new RouteRequirement(ROUTE_HINT_PORTAL_ARRAY_FEE, 0, ALLIANCE_MERIT_TOKEN_ITEM);
            }
            return new RouteRequirement(ROUTE_HINT_PORTAL_ARRAY, 0, "");
        }
        int yinStoneFee = netherRiverFerryYinStoneFee(current, targetRegion);
        if (yinStoneFee > 0) {
            return new RouteRequirement(ROUTE_HINT_NETHER_FERRY_FEE, yinStoneFee, YIN_STONE_ITEM);
        }
        if (YINMING_REGION_ID.equals(current) && NETHER_RIVER_REGION_ID.equals(targetRegion.id())) {
            return new RouteRequirement(ROUTE_HINT_NETHER_FERRY_RETURN, 0, "");
        }
        return RouteRequirement.NONE;
    }

    private static TravelCostReservation reserveTravelCosts(ServerPlayer player, String currentRegionId,
                                                            WorldpackDataService.RegionCard targetRegion,
                                                            boolean fromPortalArray) {
        Map<Item, Integer> costs = new LinkedHashMap<>();
        if (!fromPortalArray && requiresPortalArray(targetRegion)
                && !planNonPortalTravelAccess(player, currentRegionId, targetRegion, costs)) {
            return null;
        }
        if (fromPortalArray && !planPortalArrayTravelFee(player, currentRegionId, targetRegion, costs)) {
            return null;
        }
        if (fromPortalArray && !planSpatialNodeFee(player, currentRegionId, targetRegion, costs)) {
            return null;
        }
        if (!planRegionalTravelFee(player, currentRegionId, targetRegion, costs)) {
            return null;
        }
        InventoryReservation inventory = InventoryReservation.consume(player, costs);
        if (inventory == null) {
            return null;
        }
        return new TravelCostReservation(inventory);
    }

    private static boolean planNonPortalTravelAccess(ServerPlayer player, String currentRegionId,
                                                     WorldpackDataService.RegionCard targetRegion,
                                                     Map<Item, Integer> costs) {
        if (!canUseWindFeatherRaftRoute(currentRegionId, targetRegion)) {
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.worldpack.portal_required"));
            return false;
        }
        return planTravelCost(player, costs, WIND_FEATHER_RAFT_TICKET_ITEM, 1,
                Component.translatable(
                        "message.seeking_immortals.worldpack.missing_travel_ticket",
                        Component.translatable(itemDescriptionId(WIND_FEATHER_RAFT_TICKET_ITEM))));
    }

    private static boolean planPortalArrayTravelFee(ServerPlayer player, String currentRegionId,
                                                    WorldpackDataService.RegionCard targetRegion,
                                                    Map<Item, Integer> costs) {
        if (!requiresPortalArrayTravelFee(currentRegionId, targetRegion)) {
            return true;
        }
        return planTravelCost(player, costs, ALLIANCE_MERIT_TOKEN_ITEM, 1,
                Component.translatable(
                        "message.seeking_immortals.worldpack.missing_portal_fee",
                        Component.translatable(itemDescriptionId(ALLIANCE_MERIT_TOKEN_ITEM))));
    }

    private static boolean planSpatialNodeFee(ServerPlayer player, String currentRegionId,
                                              WorldpackDataService.RegionCard targetRegion,
                                              Map<Item, Integer> costs) {
        SpatialNodeFeeRules.Fee fee = SpatialNodeFeeRules.portalDestinationFee(currentRegionId, targetRegion.id());
        if (!fee.present()) {
            return true;
        }
        return planTravelCost(player, costs, fee.itemId(), fee.count(),
                Component.translatable(
                    fee.messageKey(),
                    Component.translatable(itemDescriptionId(fee.itemId())),
                    fee.count()));
    }

    private static boolean planRegionalTravelFee(ServerPlayer player, String currentRegionId,
                                                 WorldpackDataService.RegionCard targetRegion,
                                                 Map<Item, Integer> costs) {
        int yinStoneFee = netherRiverFerryYinStoneFee(currentRegionId, targetRegion);
        if (yinStoneFee <= 0) {
            return true;
        }
        return planTravelCost(player, costs, YIN_STONE_ITEM, yinStoneFee,
                Component.translatable(
                        "message.seeking_immortals.worldpack.missing_nether_ferry_fee",
                        yinStoneFee,
                        Component.translatable(itemDescriptionId(YIN_STONE_ITEM))));
    }

    private static boolean planTravelCost(ServerPlayer player, Map<Item, Integer> costs,
                                          String itemId, int count, Component missingMessage) {
        Item item = resolveItem(itemId);
        if (item == null || item == Items.AIR || count <= 0) {
            player.sendSystemMessage(missingMessage);
            return false;
        }
        long required = (long) costs.getOrDefault(item, 0) + count;
        if (required > Integer.MAX_VALUE || !InventoryReservation.hasItems(player, item, (int) required)) {
            player.sendSystemMessage(missingMessage);
            return false;
        }
        costs.put(item, (int) required);
        return true;
    }

    private static WorldpackSavedData prepareSavedData(ServerPlayer player, WorldpackDataService.Snapshot snapshot) {
        WorldpackSavedData savedData = WorldpackSavedData.get(player.server.overworld());
        savedData.ensureStarterAnchor(player.server.overworld(), snapshot);
        ensureDefaultDimensionAnchors(player, savedData);
        return savedData;
    }

    private static void ensureDefaultDimensionAnchors(ServerPlayer player, WorldpackSavedData savedData) {
        for (DefaultDimensionAnchor anchor : DEFAULT_DIMENSION_ANCHORS) {
            ResourceLocation location = ResourceLocation.tryParse(anchor.dimension());
            if (location == null) {
                continue;
            }
            ServerLevel targetLevel = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
            if (targetLevel == null) {
                continue;
            }
            Optional<WorldpackSavedData.Anchor> existingAnchor = savedData.getAnchor(anchor.id());
            if (existingAnchor.isPresent()) {
                ensureDefaultPortalPlatform(targetLevel, existingAnchor.get());
                continue;
            }
            int y = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, anchor.x(), anchor.z()) + 1;
            y = Math.max(targetLevel.getMinBuildHeight() + 2, Math.min(targetLevel.getMaxBuildHeight() - 2, y));
            ensureDefaultPortalPlatform(targetLevel, new BlockPos(anchor.x(), y - 1, anchor.z()));
            savedData.setAnchor(anchor.id(), anchor.dimension(), anchor.x() + 0.5D, y, anchor.z() + 0.5D, 0.0F, 0.0F);
        }
    }

    private static boolean teleportToAnchor(ServerPlayer player, WorldpackSavedData.Anchor anchor) {
        ResourceLocation location = ResourceLocation.tryParse(anchor.dimension());
        if (location == null) {
            return false;
        }
        ServerLevel targetLevel = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location));
        if (targetLevel == null) {
            return false;
        }
        ensureDefaultPortalPlatform(targetLevel, anchor);
        player.teleportTo(targetLevel, anchor.x(), anchor.y(), anchor.z(), anchor.yRot(), anchor.xRot());
        return player.serverLevel() == targetLevel
                && player.distanceToSqr(anchor.x(), anchor.y(), anchor.z()) <= 16.0D;
    }

    static boolean usesDefaultPortalPlatform(WorldpackSavedData.Anchor anchor) {
        if (anchor == null) {
            return false;
        }
        return DEFAULT_DIMENSION_ANCHORS.stream().anyMatch(defaultAnchor ->
                defaultAnchor.id().equals(anchor.id())
                        && defaultAnchor.dimension().equals(anchor.dimension())
                        && Math.abs(anchor.x() - (defaultAnchor.x() + 0.5D)) < 0.001D
                        && Math.abs(anchor.z() - (defaultAnchor.z() + 0.5D)) < 0.001D);
    }

    static BlockPos defaultPortalPlatformBaseCenter(WorldpackSavedData.Anchor anchor) {
        return BlockPos.containing(anchor.x(), anchor.y() - 1.0D, anchor.z());
    }

    private static void ensureDefaultPortalPlatform(ServerLevel level, WorldpackSavedData.Anchor anchor) {
        if (!usesDefaultPortalPlatform(anchor)) {
            return;
        }
        if (!level.dimension().location().toString().equals(anchor.dimension())) {
            return;
        }
        ensureDefaultPortalPlatform(level, defaultPortalPlatformBaseCenter(anchor));
    }

    private static void ensureDefaultPortalPlatform(ServerLevel level, BlockPos baseCenter) {
        if (baseCenter.getY() < level.getMinBuildHeight()
                || baseCenter.getY() + Math.max(DEFAULT_PORTAL_APERTURE_HEIGHT, DEFAULT_PORTAL_FRAME_HEIGHT) >= level.getMaxBuildHeight()) {
            return;
        }
        BlockState arrayState = ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState();
        BlockState frameState = ModBlocks.SPIRIT_ORE.get().defaultBlockState();
        for (int x = -DEFAULT_PORTAL_PLATFORM_RADIUS; x <= DEFAULT_PORTAL_PLATFORM_RADIUS; x++) {
            for (int z = -DEFAULT_PORTAL_PLATFORM_RADIUS; z <= DEFAULT_PORTAL_PLATFORM_RADIUS; z++) {
                level.setBlock(baseCenter.offset(x, 0, z), arrayState, 3);
            }
        }
        for (BlockPos offset : PortalArrayStructure.frameOffsets()) {
            level.setBlock(baseCenter.offset(offset), frameState, 3);
        }
        for (int y = 1; y <= DEFAULT_PORTAL_APERTURE_HEIGHT; y++) {
            for (int x = -DEFAULT_PORTAL_APERTURE_RADIUS; x <= DEFAULT_PORTAL_APERTURE_RADIUS; x++) {
                for (int z = -DEFAULT_PORTAL_APERTURE_RADIUS; z <= DEFAULT_PORTAL_APERTURE_RADIUS; z++) {
                    level.setBlock(baseCenter.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean canTeleportToAnchor(ServerPlayer player, WorldpackSavedData.Anchor anchor) {
        ResourceLocation location = ResourceLocation.tryParse(anchor.dimension());
        return location != null && player.server.getLevel(ResourceKey.create(Registries.DIMENSION, location)) != null;
    }

    private static boolean hasTicket(ServerPlayer player, String itemId) {
        Item item = resolveItem(itemId);
        if (item == null || item == Items.AIR) {
            return false;
        }
        return InventoryReservation.hasItems(player, item, 1);
    }

    private static final class TravelCostReservation {
        private final InventoryReservation inventory;
        private boolean refunded;

        private TravelCostReservation(InventoryReservation inventory) {
            this.inventory = inventory == null ? InventoryReservation.none() : inventory;
        }

        private void refund(ServerPlayer player) {
            if (refunded) {
                return;
            }
            refunded = true;
            inventory.refund(player);
        }
    }

    private static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        // M03 catalog first (aliases / bulk carriers), then direct registry lookup.
        try {
            Item catalog = ItemCatalogService.resolveCatalogItem(itemId);
            if (catalog != null && catalog != Items.AIR) {
                return catalog;
            }
        } catch (Throwable ignored) {
            // unit tests / early bootstrap
        }
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) {
            String bare = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
            location = ResourceLocation.tryParse(com.xunxian.seekingimmortals.SeekingImmortalsMod.MODID + ":" + bare);
        }
        if (location == null) {
            return null;
        }
        try {
            return ForgeRegistries.ITEMS == null ? null : ForgeRegistries.ITEMS.getValue(location);
        } catch (ExceptionInInitializerError | NoClassDefFoundError | IllegalArgumentException ex) {
            return null;
        }
    }

    private static String itemDescriptionId(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId == null ? "" : itemId);
        return location == null ? (itemId == null ? "" : itemId) : "item." + location.getNamespace() + "." + location.getPath();
    }

    private static long gameTime(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static String display(WorldpackDataService.RegionCard region) {
        return !region.displayZh().isBlank() ? region.displayZh() : region.displayEn();
    }

    private static String display(WorldpackDataService.SecretRealm realm) {
        return !realm.displayZh().isBlank() ? realm.displayZh() : realm.displayEn();
    }

    private static String display(WorldpackDataService.DailyEvent event) {
        return !event.displayZh().isBlank() ? event.displayZh() : event.displayEn();
    }

    public record WorldpackSnapshot(String currentRegionId, String currentRegionDisplay,
                                    String activeSecretRealmId, String activeSecretRealmDisplay,
                                    String dailyEventId, String dailyEventDisplay, long dailyEventRemainingTicks,
                                    List<String> dailyEventEffects, List<RegionData> regions,
                                    List<RealmData> realms, boolean openScreen) {}

    public record RegionData(String id, String display, String minRealm, double auraMultiplier,
                             boolean anchorReady, boolean current) {}

    public record RealmData(String id, String display, String regionId, String minRealm,
                            String ticketDescriptionId, long remainingCooldownTicks,
                            boolean anchorReady, boolean currentRegion, boolean active) {}

    private record DefaultDimensionAnchor(String id, String dimension, int x, int z) {}
}
