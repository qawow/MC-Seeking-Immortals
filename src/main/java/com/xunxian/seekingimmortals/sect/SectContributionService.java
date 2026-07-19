package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.network.SyncSectDataPacket;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.shop.ShopService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SectContributionService {
    public static final String SECT_ID = "qinglan_sect";
    public static final String SECT_DISPLAY = "\u9752\u5c9a\u5b97";
    public static final String SECT_ROLE_OUTER = "\u5916\u95e8\u5f1f\u5b50";
    public static final String SECT_ROLE_INNER = "\u5185\u95e8\u5f1f\u5b50";
    public static final String NPC_STEWARD = "\u9752\u5c9a\u5b97\u6267\u4e8b";

    public static final int STAGE_LOCKED = 0;
    public static final int STAGE_KNOCKING = 1;
    public static final int STAGE_OUTER_DISCIPLE = 2;
    public static final int STAGE_FOUNDATION_DILEMMA = 3;
    public static final int STAGE_INNER_DISCIPLE = 4;
    public static final int STAGE_PHASE10_COMPLETE = 5;

    public static final String ACTION_SYNC = "sync";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_APPLY = "apply";
    public static final String ACTION_JOIN = "join";
    public static final String ACTION_DIALOGUE = "dialogue";
    public static final String ACTION_ADVANCE = "advance";
    public static final String ACTION_ACCEPT_MISSION = "accept_mission";
    public static final String ACTION_TURN_IN_MISSION = "turn_in_mission";
    public static final String ACTION_BUY = "buy";
    public static final String ACTION_DONATE_SPIRIT_GRASS = "donate_spirit_grass";

    private static final int SPIRIT_GRASS_DONATION_COUNT = 10;
    private static final int SPIRIT_GRASS_DONATION_REWARD = 20;
    private static final int INNER_PROMOTION_REQUIRED_CONTRIBUTION = 300;
    private static final int PHASE10_COMPLETE_REQUIRED_CONTRIBUTION = 600;

    private SectContributionService() {}

    public static void showStatus(ServerPlayer player) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            player.sendSystemMessage(Component.translatable(
                    "command.seeking_immortals.sect.status",
                    displaySect(progress),
                    progress.getSectRole().isBlank() ? "-" : progress.getSectRole(),
                    progress.getContribution(),
                    Component.translatable(progress.hasYueArrived()
                            ? "message.seeking_immortals.sect.yes"
                            : "message.seeking_immortals.sect.no")));
            syncSect(player, cultivation, false);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
    }

    public static boolean join(ServerPlayer player) {
        return applySect(player, SECT_ID, false);
    }

    public static boolean applySect(ServerPlayer player, String sectId, boolean openScreen) {
        boolean[] applied = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            // M08: corpus + M01 ProgressionGateApi + ghost ban before mutating quest progress.
            SectMasterDataService.EntryCheck entryCheck = SectMasterDataService.canEnter(player, sectId);
            if (!entryCheck.allowed()) {
                if (entryCheck.reason() != null && entryCheck.reason().startsWith("ghost_ban")) {
                    GhostSectBanService.markDetected(player, "apply:" + sectId);
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.apply_ghost_ban", sectId));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.apply_entry_denied",
                            sectId,
                            entryCheck.reason() == null ? "" : entryCheck.reason()));
                }
                syncSect(player, cultivation, openScreen);
                return;
            }
            SectDefinitionService.ApplyResult result = SectDefinitionService.apply(progress, sectId);
            switch (result.status()) {
                case SUCCESS -> {
                    syncAll(player, cultivation, openScreen, result.definition().id());
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.apply_success",
                            result.definition().displayZh(),
                            SectDefinitionService.CANDIDATE_ROLE));
                    applied[0] = true;
                }
                case UNKNOWN_SECT -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.apply_unknown", sectId));
                case LOCKED -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.candidates_locked"));
                case ALREADY_MEMBER -> {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.apply_already_member",
                            result.definition().displayZh()));
                    syncAll(player, cultivation, openScreen, result.definition().id());
                }
                case OTHER_SECT -> {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.apply_other_sect",
                            displaySect(progress)));
                    syncSect(player, cultivation, openScreen);
                }
                case ENTRY_DENIED, NOT_PLAYABLE -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.apply_entry_denied",
                        sectId,
                        result.status().name().toLowerCase(Locale.ROOT)));
            }
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return applied[0];
    }

    public static boolean advanceSectQuest(ServerPlayer player) {
        boolean[] advanced = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.not_member", SECT_DISPLAY));
                syncSect(player, cultivation, true);
                return;
            }
            normalizeSectState(progress);
            int stage = progress.getSectQuestStage();
            if (stage == STAGE_KNOCKING) {
                // Wave50: three-gate knocking checklist (combat / gather / quiz flags).
                String sid = definition.get().id();
                ensureKnockingObjectives(player, progress, sid);
                boolean combat = progress.hasSectFlag(sid + "_knock_combat");
                boolean gather = progress.hasSectFlag(sid + "_knock_gather");
                boolean quiz = progress.hasSectFlag(sid + "_knock_quiz");
                if (!(combat && gather && quiz)) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.knock_progress",
                            combat ? "OK" : "X",
                            gather ? "OK" : "X",
                            quiz ? "OK" : "X"));
                    syncSect(player, cultivation, true);
                    return;
                }
                progress.addSectFlag(sid + "_knocking_passed");
                progress.setSect(sid, SectDefinitionService.OUTER_DISCIPLE_ROLE);
                progress.setSectQuestStage(STAGE_OUTER_DISCIPLE);
                progress.addContribution(WorldpackGameplayService.applySectContributionBonus(player, cultivation, 20));
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.exam_passed",
                        definition.get().displayZh(),
                        SectDefinitionService.OUTER_DISCIPLE_ROLE));
                // Wave474: outer promotion grants a starter cultivation method when available.
                try {
                    com.xunxian.seekingimmortals.catalog.ManualCatalogService.grantSectStarterMethod(player, sid);
                } catch (Throwable ignored) {
                    // optional in tests
                }
                syncAll(player, cultivation, true, sid);
                advanced[0] = true;
                return;
            }
            if (stage == STAGE_OUTER_DISCIPLE) {
                if (cultivation.getRealm().ordinal() < Realm.FOUNDATION_ESTABLISHMENT.ordinal()) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.advance_need_foundation",
                            definition.get().displayZh()));
                    syncSect(player, cultivation, true);
                    return;
                }
                progress.addSectFlag(definition.get().id() + "_foundation_dilemma");
                progress.setSectQuestStage(STAGE_FOUNDATION_DILEMMA);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.foundation_dilemma",
                        definition.get().displayZh()));
                syncAll(player, cultivation, true, definition.get().id());
                advanced[0] = true;
                return;
            }
            if (stage == STAGE_FOUNDATION_DILEMMA) {
                if (progress.getContribution() < INNER_PROMOTION_REQUIRED_CONTRIBUTION) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.advance_need_contribution",
                            INNER_PROMOTION_REQUIRED_CONTRIBUTION,
                            progress.getContribution()));
                    syncSect(player, cultivation, true);
                    return;
                }
                progress.addSectFlag(definition.get().id() + "_inner_promotion");
                progress.setSect(definition.get().id(), SectDefinitionService.INNER_DISCIPLE_ROLE);
                progress.setSectQuestStage(STAGE_INNER_DISCIPLE);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.inner_promoted",
                        definition.get().displayZh(),
                        SectDefinitionService.INNER_DISCIPLE_ROLE));
                syncAll(player, cultivation, true, definition.get().id());
                advanced[0] = true;
                return;
            }
            if (stage == STAGE_INNER_DISCIPLE) {
                if (progress.getContribution() < PHASE10_COMPLETE_REQUIRED_CONTRIBUTION) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.complete_need_contribution",
                            PHASE10_COMPLETE_REQUIRED_CONTRIBUTION,
                            progress.getContribution()));
                    syncSect(player, cultivation, true);
                    return;
                }
                progress.addSectFlag(definition.get().id() + "_phase10_complete");
                progress.setSectQuestStage(STAGE_PHASE10_COMPLETE);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.phase10_complete",
                        definition.get().displayZh()));
                syncAll(player, cultivation, true, definition.get().id());
                advanced[0] = true;
                return;
            }
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.inner_complete"));
            syncSect(player, cultivation, true);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return advanced[0];
    }

    public static void showShop(ServerPlayer player) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.not_member", SECT_DISPLAY));
                syncSect(player, cultivation, false);
                return;
            }
            player.sendSystemMessage(Component.translatable(
                    "command.seeking_immortals.sect.shop.header",
                    definition.get().displayZh(),
                    progress.getContribution()));
            for (ShopService.Entry entry : ShopService.entries(definition.get().shopId())) {
                player.sendSystemMessage(Component.translatable(
                        "command.seeking_immortals.sect.shop.entry",
                        entry.id(),
                        ShopService.itemName(entry),
                        entry.cost()));
            }
            syncSect(player, cultivation, false);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
    }

    public static boolean buy(ServerPlayer player, String entryId) {
        boolean[] bought = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty() || progress.getSectQuestStage() < STAGE_OUTER_DISCIPLE) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.not_member", SECT_DISPLAY));
                syncSect(player, cultivation, false);
                return;
            }
            ShopService.PurchaseResult result = ShopService.buyWithSectContribution(player, progress, definition.get().shopId(), entryId);
            switch (result.status()) {
                case UNKNOWN_ENTRY -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.unknown_entry", entryId));
                case UNSUPPORTED_CURRENCY -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.unsupported_currency", entryId));
                case BAD_ITEM -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.bad_shop_item", entryId));
                case BAD_CURRENCY_ITEM -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.bad_currency_item", entryId));
                case OUT_OF_STOCK -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.out_of_stock", entryId));
                case NOT_ENOUGH_CURRENCY -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.not_enough_contribution",
                        result.entry().cost(),
                        progress.getContribution()));
                case RANK_TOO_LOW -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.rank_too_low",
                        ShopService.itemName(result.entry()),
                        Component.translatable(ShopService.rankDescriptionId(result.entry().rankMin()))));
                case SUCCESS -> {
                    syncAll(player, cultivation, false, definition.get().id());
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.buy_success",
                            ShopService.itemName(result.entry()),
                            result.entry().cost(),
                            progress.getContribution()));
                    bought[0] = true;
                }
            }
            syncSect(player, cultivation, false);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return bought[0];
    }

    public static boolean acceptMission(ServerPlayer player) {
        boolean[] accepted = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty() || progress.getSectQuestStage() < STAGE_OUTER_DISCIPLE) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_not_member"));
                syncSect(player, cultivation, true);
                return;
            }
            long day = currentDay(player);
            if (progress.isSectMissionAccepted() && progress.getSectMissionDay() == day) {
                String message = progress.isSectMissionCompleted()
                        ? "message.seeking_immortals.sect.mission_already_done"
                        : "message.seeking_immortals.sect.mission_already_active";
                player.sendSystemMessage(Component.translatable(message));
                syncSect(player, cultivation, true);
                return;
            }
            // Wave490: alternate kill/escort/beast/formation generator missions with item dailies.
            if ((day & 1L) == 1L) {
                SectMissionGenerator.Mission generated = SectMissionGenerator.generate(definition.get().id());
                if (!SectMissionGenerator.acceptGenerated(player, generated)) {
                    syncSect(player, cultivation, true);
                    return;
                }
                progress.setSectMission(generated.id(), day);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_accepted_generated",
                        generated.id(), generated.type(), generated.count(), generated.rewardContribution()));
            } else {
                SectContentService.MissionDefinition mission = SectContentService.missionForDay(
                        definition.get().id(), progress.getSectQuestStage(), day);
                progress.setSectMission(mission.id(), day);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_accepted",
                        Component.translatable(mission.titleKey())));
            }
            syncAll(player, cultivation, true, definition.get().id());
            accepted[0] = true;
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return accepted[0];
    }

    public static boolean turnInMission(ServerPlayer player) {
        boolean[] turnedIn = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty() || !progress.isSectMissionAccepted()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_none"));
                syncSect(player, cultivation, true);
                return;
            }
            if (progress.isSectMissionCompleted()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_already_done"));
                syncSect(player, cultivation, true);
                return;
            }

            // Wave490: prefer generated mission authority when payload is active.
            SectMissionGenerator.Mission generated = SectMissionGenerator.activeGenerated(player);
            if (generated != null && generated.id().equals(progress.getSectMissionId())) {
                if (!SectMissionGenerator.turnIn(player, generated)) {
                    syncSect(player, cultivation, true);
                    return;
                }
                int rewardContribution = WorldpackGameplayService.applySectContributionBonus(
                        player, cultivation, generated.rewardContribution());
                progress.addContribution(rewardContribution);
                progress.completeSectMission();
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_completed",
                        Component.literal(generated.id()),
                        rewardContribution,
                        progress.getContribution()));
                syncAll(player, cultivation, true, definition.get().id());
                turnedIn[0] = true;
                return;
            }

            Optional<SectContentService.MissionDefinition> missionOptional = SectContentService.missionById(definition.get().id(), progress.getSectMissionId());
            if (missionOptional.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_missing_definition"));
                syncSect(player, cultivation, true);
                return;
            }
            SectContentService.MissionDefinition mission = missionOptional.get();
            Item item = resolveItem(mission.itemId());
            if (item == null || item == Items.AIR || !consumeItems(player, item, mission.target())) {
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_missing_items",
                        mission.target(),
                        mission.itemId()));
                syncSect(player, cultivation, true);
                return;
            }
            int rewardContribution = WorldpackGameplayService.applySectContributionBonus(
                    player, cultivation, mission.rewardContribution());
            progress.addContribution(rewardContribution);
            progress.completeSectMission();
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.sect.mission_completed",
                    Component.translatable(mission.titleKey()),
                    rewardContribution,
                    progress.getContribution()));
            syncAll(player, cultivation, true, definition.get().id());
            turnedIn[0] = true;
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return turnedIn[0];
    }

    public static boolean donateSpiritGrass(ServerPlayer player) {
        boolean[] donated = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            if (currentDefinition(progress).isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.not_member", SECT_DISPLAY));
                syncSect(player, cultivation, false);
                return;
            }
            if (!consumeItems(player, ModItems.SPIRIT_GRASS.get(), SPIRIT_GRASS_DONATION_COUNT)) {
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.donate_missing",
                        SPIRIT_GRASS_DONATION_COUNT,
                        new ItemStack(ModItems.SPIRIT_GRASS.get()).getHoverName()));
                syncSect(player, cultivation, false);
                return;
            }
            int rewardContribution = WorldpackGameplayService.applySectContributionBonus(
                    player, cultivation, SPIRIT_GRASS_DONATION_REWARD);
            progress.addContribution(rewardContribution);
            syncAll(player, cultivation, false, progress.getSectId());
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.sect.donate_success",
                    SPIRIT_GRASS_DONATION_COUNT,
                    new ItemStack(ModItems.SPIRIT_GRASS.get()).getHoverName(),
                    rewardContribution,
                    progress.getContribution()));
            donated[0] = true;
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return donated[0];
    }

    public static void addDebugContribution(ServerPlayer player, int amount) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            progress.addContribution(amount);
            syncAll(player, cultivation, false, progress.getSectId());
            player.sendSystemMessage(Component.translatable(
                    "command.seeking_immortals.sect.debug.add_contribution",
                    amount,
                    progress.getContribution()));
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
    }

    public static void openScreen(ServerPlayer player) {
        openScreen(player, "");
    }

    public static void openScreen(ServerPlayer player, String focusSectId) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
                    normalizeSectState(cultivation.getSevenMysteriesQuest());
                    // Wave490: sync data then open productized SectHall MenuType.
                    syncSect(player, cultivation, true, focusSectId);
                },
                () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
    }

    /** Wave490: productized sect hall MenuType open path. */
    public static void openSectHall(ServerPlayer player, String focusSectId) {
        if (player == null) {
            return;
        }
        final String focus = focusSectId == null ? "" : focusSectId;
        net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.seeking_immortals.sect.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new com.xunxian.seekingimmortals.menu.SectHallMenu(id, inv, focus);
            }
        }, buf -> buf.writeUtf(focus, 128));
    }

    public static void handleClientAction(ServerPlayer player, String action, String targetId, String extra) {
        String normalizedAction = normalize(action);
        String normalizedTarget = normalize(targetId);
        switch (normalizedAction) {
            case ACTION_OPEN -> openScreen(player, normalizedTarget);
            case ACTION_APPLY -> {
                applySect(player, normalizedTarget, true);
                openScreen(player, normalizedTarget);
            }
            case ACTION_JOIN -> {
                applySect(player, normalizedTarget.isBlank() ? SECT_ID : normalizedTarget, true);
                openScreen(player, normalizedTarget);
            }
            case ACTION_DIALOGUE -> handleDialogue(player, normalizedTarget);
            case ACTION_ADVANCE -> advanceSectQuest(player);
            case ACTION_ACCEPT_MISSION -> acceptMission(player);
            case ACTION_TURN_IN_MISSION -> turnInMission(player);
            case ACTION_BUY -> {
                buy(player, normalizedTarget);
                openScreen(player);
            }
            case ACTION_DONATE_SPIRIT_GRASS -> {
                donateSpiritGrass(player);
                openScreen(player);
            }
            default -> CultivationHelper.get(player).ifPresent(cultivation -> syncSect(player, cultivation, false));
        }
    }

    public static void handleClientAction(ServerPlayer player, String action, String entryId) {
        handleClientAction(player, action, entryId, "");
    }

    public static void spawnSteward(ServerPlayer player) {
        spawnSteward(player, SECT_ID);
    }

    public static void spawnSteward(ServerPlayer player, String sectId) {
        spawnStewardAt(player, player.serverLevel(), player.blockPosition().offset(1, 0, 1), sectId);
    }

    public static void placeQinglanOutpost(ServerPlayer player) {
        placeSectOutpost(player, SECT_ID);
    }

    public static void placeSectOutpost(ServerPlayer player, String sectId) {
        ServerLevel level = player.serverLevel();
        SectDefinitionService.SectDefinition definition = definitionOrQinglan(sectId);
        BlockPos origin = player.blockPosition().offset(4, 0, 0);
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                if (Math.abs(x) == 4 || Math.abs(z) == 3) {
                    level.setBlock(origin.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            level.setBlock(origin.offset(x, 0, -2), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
            level.setBlock(origin.offset(x, 0, 2), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
        }
        level.setBlock(origin, Blocks.LECTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(0, 1, 0), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(origin.offset(-3, 0, 0), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        spawnStewardAt(player, level, origin.offset(0, 0, 2), definition.id());
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.sect.outpost_placed",
                definition.displayZh(),
                origin.getX(),
                origin.getY(),
                origin.getZ()));
    }

    public static boolean handleStewardInteraction(ServerPlayer player) {
        return handleStewardInteraction(player, SECT_ID);
    }

    public static boolean handleStewardInteraction(ServerPlayer player, SectStewardEntity steward) {
        return handleStewardInteraction(player, steward.getSectId());
    }

    public static boolean handleStewardInteraction(ServerPlayer player, String sectId) {
        SectDefinitionService.SectDefinition definition = definitionOrQinglan(sectId);
        CultivationHelper.get(player).ifPresent(cultivation -> {
            normalizeSectState(cultivation.getSevenMysteriesQuest());
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (!progress.getSectId().isBlank()
                    && !SectDefinitionService.LEGACY_SEVEN_MYSTERIES_SECT_ID.equals(progress.getSectId())
                    && !definition.id().equals(progress.getSectId())) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.other_sect", displaySect(progress)));
            }
        });
        openScreen(player, definition.id());
        return true;
    }

    public static void syncSect(ServerPlayer player, PlayerCultivation cultivation, boolean openScreen) {
        syncSect(player, cultivation, openScreen, "");
    }

    public static void syncSect(ServerPlayer player, PlayerCultivation cultivation, boolean openScreen, String focusSectId) {
        // Wave490: never open legacy SectScreen via packet; MenuType is opened separately.
        SyncSectDataPacket.send(player, createPacket(player, cultivation, false, focusSectId));
        if (openScreen) {
            openSectHall(player, focusSectId);
        }
    }

    private static SyncSectDataPacket createPacket(ServerPlayer player, PlayerCultivation cultivation, boolean openScreen, String focusSectId) {
        QuestProgress progress = cultivation.getSevenMysteriesQuest();
        normalizeSectState(progress);
        SectDefinitionService.SectDefinition focused = focusedDefinition(progress, focusSectId);
        Optional<SectDefinitionService.SectDefinition> current = currentDefinition(progress);
        int stage = current.isPresent() ? progress.getSectQuestStage() : STAGE_LOCKED;

        List<SyncSectDataPacket.CandidateData> candidates = SectDefinitionService.candidates(progress).stream()
                .map(definition -> new SyncSectDataPacket.CandidateData(
                        definition.id(),
                        definition.displayZh(),
                        definition.displayEn(),
                        definition.focusZh(),
                        definition.structureId(),
                        canApplyTo(progress, definition.id())))
                .toList();

        SectContentService.DialogueNode node = current
                .map(definition -> SectContentService.nodeForStage(definition.id(), stage))
                .orElseGet(() -> SectContentService.nodeForStage(focused.id(), STAGE_KNOCKING));
        SyncSectDataPacket.DialogueNodeData dialogue = new SyncSectDataPacket.DialogueNodeData(
                node.id(),
                node.titleKey(),
                node.textKey(),
                node.options().stream()
                        .map(option -> new SyncSectDataPacket.DialogueOptionData(option.id(), option.labelKey(), option.action()))
                        .toList());

        SyncSectDataPacket.MissionData mission = createMissionData(player, progress, current, stage);

        List<SyncSectDataPacket.ShopEntryData> shopEntries = current
                .filter(definition -> stage >= STAGE_OUTER_DISCIPLE)
                .map(definition -> ShopService.entries(definition.shopId()).stream()
                        .map(entry -> new SyncSectDataPacket.ShopEntryData(
                                entry.id(),
                                ShopService.itemDescriptionId(entry),
                                entry.count(),
                                entry.cost(),
                                entry.currency()))
                        .toList())
                .orElse(List.of());

        return new SyncSectDataPacket(
                focused.id(),
                focused.displayZh(),
                displaySect(progress),
                progress.getSectRole(),
                progress.getContribution(),
                progress.hasYueArrived(),
                progress.isComplete(),
                current.isPresent(),
                canApplyTo(progress, focused.id()),
                stage,
                stageKey(stage, current.isPresent()),
                objectiveKey(cultivation, progress, focused),
                candidates,
                dialogue,
                mission,
                shopEntries,
                openScreen);
    }

    private static SyncSectDataPacket.MissionData createMissionData(ServerPlayer player, QuestProgress progress,
                                                                    Optional<SectDefinitionService.SectDefinition> definition,
                                                                    int stage) {
        if (definition.isEmpty() || stage < STAGE_OUTER_DISCIPLE) {
            return SyncSectDataPacket.MissionData.empty();
        }
        SectContentService.MissionDefinition mission = progress.isSectMissionAccepted()
                ? SectContentService.missionById(definition.get().id(), progress.getSectMissionId())
                        .orElseGet(() -> SectContentService.missionForDay(definition.get().id(), stage, currentDay(player)))
                : SectContentService.missionForDay(definition.get().id(), stage, currentDay(player));
        Item item = resolveItem(mission.itemId());
        String itemDescriptionId = item == null || item == Items.AIR ? mission.itemId() : item.getDescriptionId();
        boolean canTurnIn = progress.isSectMissionAccepted()
                && !progress.isSectMissionCompleted()
                && item != null
                && item != Items.AIR
                && hasItem(player, item, mission.target());
        return new SyncSectDataPacket.MissionData(
                mission.id(),
                mission.titleKey(),
                mission.objectiveKey(),
                itemDescriptionId,
                mission.target(),
                mission.rewardContribution(),
                progress.isSectMissionAccepted(),
                progress.isSectMissionCompleted(),
                canTurnIn);
    }

    private static void handleDialogue(ServerPlayer player, String optionId) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty()) {
                syncSect(player, cultivation, true);
                return;
            }
            Optional<SectContentService.DialogueOption> option = SectContentService.optionForStage(
                    definition.get().id(), progress.getSectQuestStage(), optionId);
            String action = option.map(SectContentService.DialogueOption::action).orElse("");
            switch (action) {
                case ACTION_ADVANCE -> advanceSectQuest(player);
                case ACTION_ACCEPT_MISSION -> acceptMission(player);
                case ACTION_TURN_IN_MISSION -> turnInMission(player);
                default -> syncSect(player, cultivation, true);
            }
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
    }

    private static void spawnStewardAt(ServerPlayer player, ServerLevel level, BlockPos pos, String sectId) {
        SectDefinitionService.SectDefinition definition = definitionOrQinglan(sectId);
        SectStewardEntity steward = ModEntities.SECT_STEWARD.get().create(level);
        if (steward == null) {
            return;
        }
        steward.setSectId(definition.id());
        steward.setNpcType(SectStewardEntity.NPC_TYPE_RECRUITER);
        steward.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        steward.setCustomName(Component.literal(definition.stewardName()));
        steward.setCustomNameVisible(true);
        steward.setPersistenceRequired();
        level.addFreshEntity(steward);
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.spawned_steward", definition.stewardName()));
    }

    private static boolean canApplyTo(QuestProgress progress, String sectId) {
        if (!SectDefinitionService.entryGateOpen(progress) || SectDefinitionService.find(sectId).isEmpty()) {
            return false;
        }
        String currentSect = progress.getSectId();
        return currentSect.isBlank()
                || SectDefinitionService.LEGACY_SEVEN_MYSTERIES_SECT_ID.equals(currentSect)
                || normalize(currentSect).equals(normalize(sectId));
    }

    /**
     * Wave50: auto-progress knocking three-gate objectives using inventory/realm heuristics.
     * combat: any learned technique or foundation realm
     * gather: holds spirit grass / spirit stone / beast core
     * quiz: answered via prior talk/flag or contribution > 0 / manual present
     */
    private static void ensureKnockingObjectives(ServerPlayer player, QuestProgress progress, String sid) {
        if (progress.hasSectFlag(sid + "_knock_combat")
                && progress.hasSectFlag(sid + "_knock_gather")
                && progress.hasSectFlag(sid + "_knock_quiz")) {
            return;
        }
        com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).ifPresent(cultivation -> {
            if (cultivation.getRealm().ordinal() >= com.xunxian.seekingimmortals.cultivation.Realm.QI_REFINING.ordinal()
                    || cultivation.getLearnedTechniques().size() > 0) {
                progress.addSectFlag(sid + "_knock_combat");
            }
        });
        boolean hasGather = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            String key = stack.getDescriptionId();
            if (key.contains("spirit_grass") || key.contains("spirit_stone") || key.contains("beast_core")
                    || key.contains("cloud_mushroom") || key.contains("immortal_ginseng")) {
                hasGather = true;
                break;
            }
        }
        if (hasGather) {
            progress.addSectFlag(sid + "_knock_gather");
        }
        if (progress.getContribution() > 0 || progress.hasSectFlag(sid + "_knock_quiz")) {
            progress.addSectFlag(sid + "_knock_quiz");
        } else {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(com.xunxian.seekingimmortals.registry.ModItems.JADE_SLIP_BLANK.get())
                        || stack.is(com.xunxian.seekingimmortals.registry.ModItems.TECHNIQUE_MANUAL_COMMON.get())) {
                    progress.addSectFlag(sid + "_knock_quiz");
                    break;
                }
            }
        }
        // Allow creative / OP to complete all three instantly for smoke tests.
        if (player.getAbilities().instabuild) {
            progress.addSectFlag(sid + "_knock_combat");
            progress.addSectFlag(sid + "_knock_gather");
            progress.addSectFlag(sid + "_knock_quiz");
        }
    }

    private static void normalizeSectState(QuestProgress progress) {
        Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
        if (definition.isEmpty()) {
            return;
        }
        int stage = progress.getSectQuestStage();
        if (SECT_ID.equals(definition.get().id()) && stage <= STAGE_LOCKED) {
            stage = STAGE_OUTER_DISCIPLE;
        } else if (stage < STAGE_KNOCKING) {
            stage = STAGE_KNOCKING;
        } else if (stage > STAGE_PHASE10_COMPLETE) {
            stage = STAGE_PHASE10_COMPLETE;
        }
        progress.setSectQuestStage(stage);
        if (stage >= STAGE_INNER_DISCIPLE) {
            progress.setSect(definition.get().id(), SectDefinitionService.INNER_DISCIPLE_ROLE);
        } else if (stage >= STAGE_OUTER_DISCIPLE) {
            progress.setSect(definition.get().id(), SectDefinitionService.OUTER_DISCIPLE_ROLE);
        } else {
            progress.setSect(definition.get().id(), SectDefinitionService.CANDIDATE_ROLE);
        }
    }

    private static Optional<SectDefinitionService.SectDefinition> currentDefinition(QuestProgress progress) {
        return SectDefinitionService.find(progress.getSectId());
    }

    private static SectDefinitionService.SectDefinition focusedDefinition(QuestProgress progress, String focusSectId) {
        Optional<SectDefinitionService.SectDefinition> current = currentDefinition(progress);
        if (current.isPresent()) {
            return current.get();
        }
        return definitionOrQinglan(focusSectId);
    }

    private static SectDefinitionService.SectDefinition definitionOrQinglan(String sectId) {
        return SectDefinitionService.find(sectId)
                .or(() -> SectDefinitionService.find(SECT_ID))
                .orElse(SectDefinitionService.definitions().get(0));
    }

    private static String displaySect(QuestProgress progress) {
        return currentDefinition(progress)
                .map(SectDefinitionService.SectDefinition::displayZh)
                .orElse(progress.getSectId().isBlank() ? "-" : progress.getSectId());
    }

    private static String stageKey(int stage, boolean member) {
        if (!member) {
            return "screen.seeking_immortals.sect.stage.locked";
        }
        return switch (stage) {
            case STAGE_KNOCKING -> "screen.seeking_immortals.sect.stage.knocking";
            case STAGE_OUTER_DISCIPLE -> "screen.seeking_immortals.sect.stage.outer";
            case STAGE_FOUNDATION_DILEMMA -> "screen.seeking_immortals.sect.stage.foundation";
            case STAGE_INNER_DISCIPLE -> "screen.seeking_immortals.sect.stage.inner";
            case STAGE_PHASE10_COMPLETE -> "screen.seeking_immortals.sect.stage.complete";
            default -> "screen.seeking_immortals.sect.stage.locked";
        };
    }

    private static String objectiveKey(PlayerCultivation cultivation, QuestProgress progress, SectDefinitionService.SectDefinition focused) {
        if (!progress.isComplete() || !progress.hasYueArrived()) {
            return "screen.seeking_immortals.sect.objective.locked";
        }
        Optional<SectDefinitionService.SectDefinition> current = currentDefinition(progress);
        if (current.isEmpty()) {
            return "screen.seeking_immortals.sect.objective.choose";
        }
        return switch (progress.getSectQuestStage()) {
            case STAGE_KNOCKING -> "screen.seeking_immortals.sect.objective.knocking";
            case STAGE_OUTER_DISCIPLE -> cultivation.getRealm().ordinal() >= Realm.FOUNDATION_ESTABLISHMENT.ordinal()
                    ? "screen.seeking_immortals.sect.objective.foundation_available"
                    : "screen.seeking_immortals.sect.objective.outer";
            case STAGE_FOUNDATION_DILEMMA -> "screen.seeking_immortals.sect.objective.inner";
            case STAGE_INNER_DISCIPLE -> "screen.seeking_immortals.sect.objective.inner_competition";
            case STAGE_PHASE10_COMPLETE -> "screen.seeking_immortals.sect.objective.complete";
            default -> "screen.seeking_immortals.sect.objective.join";
        };
    }

    private static long currentDay(ServerPlayer player) {
        return player.serverLevel().getDayTime() / 24000L;
    }

    private static Item resolveItem(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId == null || itemId.isBlank()
                ? ""
                : itemId.indexOf(':') >= 0 ? itemId : SeekingImmortalsMod.MODID + ":" + itemId);
        return location == null ? null : ForgeRegistries.ITEMS.getValue(location);
    }

    private static boolean hasItem(ServerPlayer player, Item item, int count) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total >= count;
    }

    private static boolean consumeItems(ServerPlayer player, Item item, int count) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (!hasItem(player, item, count)) {
            return false;
        }
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) {
                continue;
            }
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (remaining <= 0) {
                return true;
            }
        }
        return true;
    }

    private static void syncAll(ServerPlayer player, PlayerCultivation cultivation, boolean openScreen, String focusSectId) {
        SyncCultivationDataPacket.send(player, cultivation);
        SyncLearnedTechniquesPacket.send(player, cultivation);
        syncSect(player, cultivation, openScreen, focusSectId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
