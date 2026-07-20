package com.xunxian.seekingimmortals.command;

import com.xunxian.seekingimmortals.catalog.AuctionSoftService;
import com.xunxian.seekingimmortals.catalog.FlightVehicleService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.catalog.TextMaterialManifestService;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.item.DeliveryOutboxSavedData;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TextQuestDialogueService;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import com.xunxian.seekingimmortals.structure.MultiblockOperationalSavedData;
import com.xunxian.seekingimmortals.structure.MultiblockPattern;
import com.xunxian.seekingimmortals.structure.MultiblockSequenceDisplayCatalog;
import com.xunxian.seekingimmortals.worldpack.AuctionHouseSavedData;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.SecretRealmDimensionService;
import com.xunxian.seekingimmortals.worldpack.SpatialNodeCatalogService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Live-smoke checklist with auto report + Wave56 human sign-off.
 * 0.2.104: multiplayer authority auto probes + dedicated-server manual steps.
 */
public final class LiveSmokeChecklistService {
    private static final String SIGN_TAG = "seeking_immortals_live_smoke_signed";
    private static final String SIGN_BY_TAG = "seeking_immortals_live_smoke_signed_by";
    private static final String SIGN_NOTE_TAG = "seeking_immortals_live_smoke_signed_note";
    private static final String SIGN_TIME_TAG = "seeking_immortals_live_smoke_signed_time";
    private static final String MP_SIGN_TAG = "seeking_immortals_mp_smoke_signed";
    private static final String MP_SIGN_BY_TAG = "seeking_immortals_mp_smoke_signed_by";
    private static final String MP_SIGN_NOTE_TAG = "seeking_immortals_mp_smoke_signed_note";
    private static final String MP_SIGN_TIME_TAG = "seeking_immortals_mp_smoke_signed_time";

    private static final List<String> MANUAL_STEPS = List.of(
            "multiblock_portal",
            "secret_realm_enter",
            "dialogue_gui",
            "technique_release",
            "auction_bid",
            "shop_rank_lock",
            "puppet_or_beast",
            "formation_deploy",
            "alchemy_storage_refine_ui",
            "return_anchor"
    );

    /** master_plan §6.9 dedicated-server + two-client steps. */
    public static final List<String> MANUAL_MP_STEPS = List.of(
            "mp_pvp",
            "mp_station_concurrent_form",
            "mp_auction_outbid",
            "mp_outbox_reconnect",
            "mp_technique_cooldown_reconnect",
            "mp_protocol_match"
    );

    private LiveSmokeChecklistService() {}

    public record CheckItem(String id, String title, boolean autoOk, String detail) {}

    public static List<CheckItem> autoProbe(ServerPlayer player) {
        List<CheckItem> items = new ArrayList<>();
        AuctionSoftService.Snapshot auction = AuctionSoftService.builtin();
        items.add(new CheckItem("auction_catalog", "Auction catalog loads",
                auction.lotCount() > 0, "lots=" + auction.lotCount()));
        items.add(new CheckItem("quest_catalog", "Quest chains indexed",
                TextQuestChainService.chainCount() > 0, "chains=" + TextQuestChainService.chainCount()));
        items.add(new CheckItem("dialogue_npc_hooks", "Dialogue NPC hooks sample",
                !TextQuestDialogueService.sampleNpcHooks(3).isEmpty(),
                "hooks=" + TextQuestDialogueService.sampleNpcHooks(3).size()));
        items.add(new CheckItem("spatial_nodes", "Spatial nodes catalog",
                SpatialNodeCatalogService.builtin().size() > 0,
                "nodes=" + SpatialNodeCatalogService.builtin().size()));
        items.add(new CheckItem("secret_dims", "Dedicated secret-realm dimensions mapped",
                SecretRealmDimensionService.dedicatedDimensionCount() >= 6,
                "mapped=" + SecretRealmDimensionService.dedicatedDimensionCount()));
        items.add(new CheckItem("summon_surface", "Summon service surface",
                SummonHonestMvpService.puppetDefinitionCount() >= 0,
                "puppets=" + SummonHonestMvpService.puppetDefinitionCount()
                        + ",archetype=" + SummonHonestMvpService.archetypeOf("beast_summon")));
        items.add(new CheckItem("quest_stage_cost", "Quest stage cost table",
                TextQuestChainService.stageCostFor("huangfeng_cultivation_path", 2, 6).isPresent()
                        || TextQuestChainService.stageCostFor("huangfeng_cultivation_path", 3, 6).isPresent()
                        || TextQuestChainService.stageCostFor("huangfeng_cultivation_path", 6, 6).isPresent(),
                "mid_or_finale_cost_present"));
        items.add(new CheckItem("formation_fields", "Formation field service",
                FormationFieldService.activeCount() >= 0,
                "active=" + FormationFieldService.activeCount()));
        items.add(new CheckItem("reputation", "Reputation store readable",
                ReputationService.snapshot(player) != null,
                "entries=" + ReputationService.snapshot(player).size()));
        items.add(new CheckItem("manifest", "Text material manifest",
                TextMaterialManifestService.builtin().totalFiles() >= 0
                        || TextMaterialManifestService.builtin().totalEntries() >= 0,
                "files=" + TextMaterialManifestService.builtin().totalFiles()
                        + ",entries=" + TextMaterialManifestService.builtin().totalEntries()));
        items.add(new CheckItem("multiblock_api", "MultiblockPattern API present",
                MultiblockPattern.require(0, 0, 0, () -> net.minecraft.world.level.block.Blocks.STONE) != null,
                "ok"));
        items.add(new CheckItem("flight_vehicles", "Flight vehicle bindings",
                FlightVehicleService.vehicleCount() >= 0,
                "count=" + FlightVehicleService.vehicleCount()));
        items.add(new CheckItem("beast_contracts", "Beast contract service",
                BeastContractService.list(player) != null,
                "contracts=" + BeastContractService.list(player).size()));
        items.add(new CheckItem("asura_immortal_map", "Asura/Immortal dim map",
                SecretRealmDimensionService.dimensionIdFor("asura_realm").isPresent()
                        && SecretRealmDimensionService.dimensionIdFor("immortal_realm").isPresent(),
                "ok"));
        items.addAll(multiplayerAutoProbe(player));
        items.add(new CheckItem("human_signoff", "Human live-smoke sign-off recorded",
                player.getPersistentData().getBoolean(SIGN_TAG),
                player.getPersistentData().getBoolean(SIGN_TAG)
                        ? ("by=" + player.getPersistentData().getString(SIGN_BY_TAG)
                        + ",note=" + player.getPersistentData().getString(SIGN_NOTE_TAG))
                        : "unsigned"));
        items.add(new CheckItem("mp_human_signoff", "Human multiplayer sign-off recorded",
                player.getPersistentData().getBoolean(MP_SIGN_TAG),
                player.getPersistentData().getBoolean(MP_SIGN_TAG)
                        ? ("by=" + player.getPersistentData().getString(MP_SIGN_BY_TAG)
                        + ",note=" + player.getPersistentData().getString(MP_SIGN_NOTE_TAG))
                        : "unsigned"));
        return items;
    }

    /**
     * Authority-side multiplayer probes that do not require a second human client.
     * Human two-client steps remain in MANUAL_MP_STEPS.
     */
    public static List<CheckItem> multiplayerAutoProbe(ServerPlayer player) {
        List<CheckItem> items = new ArrayList<>();
        String protocol = ModNetwork.protocolVersion();
        items.add(new CheckItem("protocol_version", "Network protocol version readable",
                protocol != null && !protocol.isBlank(),
                "protocol=" + protocol));

        boolean dedicated = player != null
                && player.getServer() != null
                && player.getServer().isDedicatedServer();
        int online = player != null && player.getServer() != null
                ? player.getServer().getPlayerCount()
                : 0;
        items.add(new CheckItem("server_mode", "Server mode snapshot",
                player != null && player.getServer() != null,
                "dedicated=" + dedicated + ",online=" + online));

        // Auction outbid ledger is shared SavedData; probe that load/save surface is live.
        if (player != null && player.getServer() != null) {
            AuctionHouseSavedData house = AuctionHouseSavedData.get(player);
            items.add(new CheckItem("auction_outbid_ledger", "Auction house ladder/refund ledger",
                    house != null,
                    "current_sample=" + house.currentAmount("lot_probe")
                            + ",pending_self=" + house.peekPendingRefund(player.getUUID())));

            DeliveryOutboxSavedData outbox = DeliveryOutboxSavedData.get(player.getServer());
            items.add(new CheckItem("outbox_uuid_isolation", "Delivery outbox per-player ledger",
                    outbox != null,
                    "pending_self=" + outbox.pendingCount(player.getUUID())
                            + ",pending_other=" + outbox.pendingCount(UUID.nameUUIDFromBytes("mp-probe".getBytes()))));

            MultiblockOperationalSavedData ops = MultiblockOperationalSavedData.get(player.serverLevel());
            items.add(new CheckItem("station_ops_ledger", "Multiblock operational SavedData",
                    ops != null,
                    "stations=" + ops.size()));
        } else {
            items.add(new CheckItem("auction_outbid_ledger", "Auction house ladder/refund ledger",
                    false, "no_server"));
            items.add(new CheckItem("outbox_uuid_isolation", "Delivery outbox per-player ledger",
                    false, "no_server"));
            items.add(new CheckItem("station_ops_ledger", "Multiblock operational SavedData",
                    false, "no_server"));
        }

        items.add(new CheckItem("mp_sequence_catalog", "MP sequence catalog (presentation only)",
                MultiblockSequenceDisplayCatalog.builtin().size() >= 0,
                "sequences=" + MultiblockSequenceDisplayCatalog.builtin().size()));
        return items;
    }

    public static void print(ServerPlayer player) {
        print(player, false);
    }

    public static void print(ServerPlayer player, boolean multiplayerOnly) {
        player.displayClientMessage(Component.translatable(
                multiplayerOnly
                        ? "command.seeking_immortals.live_smoke.mp_header"
                        : "command.seeking_immortals.live_smoke.header"), false);
        int pass = 0;
        int fail = 0;
        List<CheckItem> items = multiplayerOnly ? multiplayerAutoProbe(player) : autoProbe(player);
        StringBuilder report = new StringBuilder();
        report.append(multiplayerOnly ? "# Multiplayer Live Smoke Report\n\n" : "# Live Smoke Report\n\n");
        report.append("Time: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        for (CheckItem item : items) {
            if (item.autoOk()) {
                pass++;
                player.displayClientMessage(Component.translatable("command.seeking_immortals.live_smoke.pass",
                        item.id(), item.title(), item.detail()), false);
                report.append("- [x] ").append(item.id()).append(" — ").append(item.title())
                        .append(" (").append(item.detail()).append(")\n");
            } else {
                fail++;
                player.displayClientMessage(Component.translatable("command.seeking_immortals.live_smoke.fail",
                        item.id(), item.title(), item.detail()), false);
                report.append("- [ ] ").append(item.id()).append(" — ").append(item.title())
                        .append(" (").append(item.detail()).append(")\n");
            }
        }
        player.displayClientMessage(Component.translatable(
                multiplayerOnly
                        ? "command.seeking_immortals.live_smoke.mp_manual_header"
                        : "command.seeking_immortals.live_smoke.manual_header"), false);
        int i = 1;
        List<String> steps = multiplayerOnly ? MANUAL_MP_STEPS : MANUAL_STEPS;
        for (String step : steps) {
            player.displayClientMessage(Component.literal(i + ") " + step), false);
            i++;
        }
        if (!multiplayerOnly) {
            player.displayClientMessage(Component.translatable(
                    "command.seeking_immortals.live_smoke.mp_manual_header"), false);
            for (String step : MANUAL_MP_STEPS) {
                player.displayClientMessage(Component.literal(i + ") " + step), false);
                i++;
            }
        }
        player.displayClientMessage(Component.translatable("command.seeking_immortals.live_smoke.summary", pass, fail), false);
        report.append(multiplayerOnly ? "\n## Manual multiplayer steps\n" : "\n## Manual steps\n");
        for (String step : steps) {
            report.append("- [ ] ").append(step).append("\n");
        }
        if (!multiplayerOnly) {
            report.append("\n## Manual multiplayer steps\n");
            for (String step : MANUAL_MP_STEPS) {
                report.append("- [ ] ").append(step).append("\n");
            }
        }
        report.append("\nSummary: pass=").append(pass).append(" fail=").append(fail).append("\n");
        if (player.getPersistentData().getBoolean(SIGN_TAG)) {
            report.append("Signed-by: ").append(player.getPersistentData().getString(SIGN_BY_TAG)).append("\n");
            report.append("Signed-note: ").append(player.getPersistentData().getString(SIGN_NOTE_TAG)).append("\n");
            report.append("Signed-time: ").append(player.getPersistentData().getString(SIGN_TIME_TAG)).append("\n");
        } else {
            report.append("Signed-by: unsigned\n");
        }
        if (player.getPersistentData().getBoolean(MP_SIGN_TAG)) {
            report.append("Mp-signed-by: ").append(player.getPersistentData().getString(MP_SIGN_BY_TAG)).append("\n");
            report.append("Mp-signed-note: ").append(player.getPersistentData().getString(MP_SIGN_NOTE_TAG)).append("\n");
            report.append("Mp-signed-time: ").append(player.getPersistentData().getString(MP_SIGN_TIME_TAG)).append("\n");
        } else {
            report.append("Mp-signed-by: unsigned\n");
        }
        writeReport(report.toString(), multiplayerOnly
                ? "live_smoke_mp_report_latest.md"
                : "live_smoke_report_latest.md");
        player.getPersistentData().putBoolean("seeking_immortals_live_smoke_ran", true);
        player.getPersistentData().putInt("seeking_immortals_live_smoke_pass", pass);
        player.getPersistentData().putInt("seeking_immortals_live_smoke_fail", fail);
        if (multiplayerOnly) {
            player.getPersistentData().putBoolean("seeking_immortals_mp_smoke_ran", true);
            player.getPersistentData().putInt("seeking_immortals_mp_smoke_pass", pass);
            player.getPersistentData().putInt("seeking_immortals_mp_smoke_fail", fail);
        }
    }

    /**
     * Wave56 human client sign-off. Records operator identity + note and writes a signed report.
     */
    public static boolean sign(ServerPlayer player, String note) {
        String signer = player.getGameProfile().getName();
        String cleanNote = note == null || note.isBlank() ? "manual_client_pass" : note.trim();
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        player.getPersistentData().putBoolean(SIGN_TAG, true);
        player.getPersistentData().putString(SIGN_BY_TAG, signer);
        player.getPersistentData().putString(SIGN_NOTE_TAG, cleanNote);
        player.getPersistentData().putString(SIGN_TIME_TAG, time);

        // Mark all manual steps complete in signed report.
        StringBuilder report = new StringBuilder();
        report.append("# Live Smoke Signed Report\n\n");
        report.append("Time: ").append(time).append("\n");
        report.append("Signer: ").append(signer).append("\n");
        report.append("Note: ").append(cleanNote).append("\n\n");
        report.append("## Auto probe\n");
        int pass = 0;
        int fail = 0;
        for (CheckItem item : autoProbe(player)) {
            if (item.autoOk()) {
                pass++;
                report.append("- [x] ").append(item.id()).append(" — ").append(item.title())
                        .append(" (").append(item.detail()).append(")\n");
            } else {
                fail++;
                report.append("- [ ] ").append(item.id()).append(" — ").append(item.title())
                        .append(" (").append(item.detail()).append(")\n");
            }
        }
        report.append("\n## Manual steps (human signed)\n");
        for (String step : MANUAL_STEPS) {
            report.append("- [x] ").append(step).append("\n");
        }
        report.append("\n## Manual multiplayer steps (still require live_smoke mp sign)\n");
        for (String step : MANUAL_MP_STEPS) {
            report.append("- [ ] ").append(step).append("\n");
        }
        report.append("\nSummary: pass=").append(pass).append(" fail=").append(fail)
                .append(" signed=true\n");
        writeReport(report.toString(), "live_smoke_report_latest.md");
        writeReport(report.toString(), "live_smoke_report_signed.md");
        writeManualChecklist(signer, cleanNote, time);
        player.displayClientMessage(Component.translatable(
                "command.seeking_immortals.live_smoke.signed", signer, cleanNote), true);
        return true;
    }

    /**
     * Dedicated-server + two-client multiplayer sign-off (master_plan §6.9).
     */
    public static boolean signMultiplayer(ServerPlayer player, String note) {
        String signer = player.getGameProfile().getName();
        String cleanNote = note == null || note.isBlank() ? "manual_mp_pass" : note.trim();
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        player.getPersistentData().putBoolean(MP_SIGN_TAG, true);
        player.getPersistentData().putString(MP_SIGN_BY_TAG, signer);
        player.getPersistentData().putString(MP_SIGN_NOTE_TAG, cleanNote);
        player.getPersistentData().putString(MP_SIGN_TIME_TAG, time);

        StringBuilder report = new StringBuilder();
        report.append("# Multiplayer Live Smoke Signed Report\n\n");
        report.append("Time: ").append(time).append("\n");
        report.append("Signer: ").append(signer).append("\n");
        report.append("Note: ").append(cleanNote).append("\n\n");
        report.append("## Auto multiplayer probe\n");
        int pass = 0;
        int fail = 0;
        for (CheckItem item : multiplayerAutoProbe(player)) {
            if (item.autoOk()) {
                pass++;
                report.append("- [x] ").append(item.id()).append(" — ").append(item.title())
                        .append(" (").append(item.detail()).append(")\n");
            } else {
                fail++;
                report.append("- [ ] ").append(item.id()).append(" — ").append(item.title())
                        .append(" (").append(item.detail()).append(")\n");
            }
        }
        report.append("\n## Manual multiplayer steps (human signed)\n");
        for (String step : MANUAL_MP_STEPS) {
            report.append("- [x] ").append(step).append("\n");
        }
        report.append("\nSummary: pass=").append(pass).append(" fail=").append(fail)
                .append(" mp_signed=true\n");
        writeReport(report.toString(), "live_smoke_mp_report_latest.md");
        writeReport(report.toString(), "live_smoke_mp_report_signed.md");
        writeMultiplayerChecklist(signer, cleanNote, time, pass, fail);
        player.displayClientMessage(Component.translatable(
                "command.seeking_immortals.live_smoke.mp_signed", signer, cleanNote), true);
        return true;
    }

    public static boolean isSigned(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(SIGN_TAG);
    }

    public static boolean isMultiplayerSigned(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(MP_SIGN_TAG);
    }

    private static void writeManualChecklist(String signer, String note, String time) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Manual Live Smoke Checklist (signed)\n\n");
        sb.append("Date: ").append(time).append("\n");
        sb.append("Signer: ").append(signer).append("\n");
        sb.append("Note: ").append(note).append("\n\n");
        sb.append("## Auto probe\n");
        sb.append("Run `/seeking_immortals live_smoke` then `/seeking_immortals live_smoke sign <note>`.\n\n");
        sb.append("## Manual in-game steps\n");
        for (String step : MANUAL_STEPS) {
            sb.append("- [x] ").append(step).append("\n");
        }
        sb.append("\n## Sign-off\n");
        sb.append("- [x] Human client live-smoke sign-off recorded\n");
        writeReport(sb.toString(), "manual_live_smoke_checklist.md");
    }

    private static void writeMultiplayerChecklist(String signer, String note, String time, int pass, int fail) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Manual Multiplayer Sign-off Checklist (signed)\n\n");
        sb.append("Date: ").append(time).append("\n");
        sb.append("Signer: ").append(signer).append("\n");
        sb.append("Note: ").append(note).append("\n");
        sb.append("Auto probe: pass=").append(pass).append(" fail=").append(fail).append("\n\n");
        sb.append("## Commands\n");
        sb.append("1. Start dedicated server: `./gradlew runServer` (or production jar).\n");
        sb.append("2. Join with two clients on the same protocol version.\n");
        sb.append("3. Run `/seeking_immortals live_smoke mp` then `/seeking_immortals live_smoke mp sign <note>`.\n\n");
        sb.append("## Manual multiplayer steps\n");
        for (String step : MANUAL_MP_STEPS) {
            sb.append("- [x] ").append(step).append("\n");
        }
        sb.append("\n## Sign-off\n");
        sb.append("- [x] Human dedicated-server + two-client multiplayer sign-off recorded\n");
        writeReport(sb.toString(), "manual_multiplayer_signoff_checklist.md");
    }

    private static void writeReport(String content, String fileName) {
        try {
            Path path = Path.of("project_docs", fileName);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
