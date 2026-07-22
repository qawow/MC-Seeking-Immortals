package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import com.xunxian.seekingimmortals.menu.MenuAccessContext;
import com.xunxian.seekingimmortals.menu.SectHallMenu;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.network.SyncSectDataPacket;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.shop.ShopService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
                            "message.seeking_immortals.sect.apply_ghost_ban", sectDisplay(sectId)));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.apply_entry_denied",
                            sectDisplay(sectId),
                            Component.translatable("text.seeking_immortals.unknown_requirement")));
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
                case UNKNOWN_SECT -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.apply_unknown", sectDisplay(sectId)));
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
                        sectDisplay(sectId),
                        Component.translatable("text.seeking_immortals.unknown_requirement")));
            }
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return applied[0];
    }

    public static boolean advanceSectQuest(ServerPlayer player) {
        return advanceSectQuest(player, true);
    }

    private static boolean advanceSectQuest(ServerPlayer player, boolean openScreen) {
        boolean[] advanced = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.not_member", SECT_DISPLAY));
                syncSect(player, cultivation, openScreen);
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
                            completionDisplay(combat),
                            completionDisplay(gather),
                            completionDisplay(quiz)));
                    syncSect(player, cultivation, openScreen);
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
                // Specialty map is the authority for rank-bound sect method grants.
                try {
                    com.xunxian.seekingimmortals.catalog.ManualCatalogService.grantSectSpecialtyMethods(
                            player, sid, STAGE_OUTER_DISCIPLE);
                } catch (Throwable ignored) {
                    // optional in tests
                }
                syncAll(player, cultivation, openScreen, sid);
                advanced[0] = true;
                return;
            }
            if (stage == STAGE_OUTER_DISCIPLE) {
                if (cultivation.getRealm().ordinal() < Realm.FOUNDATION_ESTABLISHMENT.ordinal()) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.advance_need_foundation",
                            definition.get().displayZh()));
                    syncSect(player, cultivation, openScreen);
                    return;
                }
                progress.addSectFlag(definition.get().id() + "_foundation_dilemma");
                progress.setSectQuestStage(STAGE_FOUNDATION_DILEMMA);
                com.xunxian.seekingimmortals.catalog.ManualCatalogService.grantSectSpecialtyMethods(
                        player, definition.get().id(), STAGE_FOUNDATION_DILEMMA);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.foundation_dilemma",
                        definition.get().displayZh()));
                syncAll(player, cultivation, openScreen, definition.get().id());
                advanced[0] = true;
                return;
            }
            if (stage == STAGE_FOUNDATION_DILEMMA) {
                if (progress.getContribution() < INNER_PROMOTION_REQUIRED_CONTRIBUTION) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.advance_need_contribution",
                            INNER_PROMOTION_REQUIRED_CONTRIBUTION,
                            progress.getContribution()));
                    syncSect(player, cultivation, openScreen);
                    return;
                }
                progress.addSectFlag(definition.get().id() + "_inner_promotion");
                progress.setSect(definition.get().id(), SectDefinitionService.INNER_DISCIPLE_ROLE);
                progress.setSectQuestStage(STAGE_INNER_DISCIPLE);
                com.xunxian.seekingimmortals.catalog.ManualCatalogService.grantSectSpecialtyMethods(
                        player, definition.get().id(), STAGE_INNER_DISCIPLE);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.inner_promoted",
                        definition.get().displayZh(),
                        SectDefinitionService.INNER_DISCIPLE_ROLE));
                syncAll(player, cultivation, openScreen, definition.get().id());
                advanced[0] = true;
                return;
            }
            if (stage == STAGE_INNER_DISCIPLE) {
                if (progress.getContribution() < PHASE10_COMPLETE_REQUIRED_CONTRIBUTION) {
                    player.sendSystemMessage(Component.translatable(
                            "message.seeking_immortals.sect.complete_need_contribution",
                            PHASE10_COMPLETE_REQUIRED_CONTRIBUTION,
                            progress.getContribution()));
                    syncSect(player, cultivation, openScreen);
                    return;
                }
                progress.addSectFlag(definition.get().id() + "_phase10_complete");
                progress.setSectQuestStage(STAGE_PHASE10_COMPLETE);
                com.xunxian.seekingimmortals.catalog.ManualCatalogService.grantSectSpecialtyMethods(
                        player, definition.get().id(), STAGE_PHASE10_COMPLETE);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.phase10_complete",
                        definition.get().displayZh()));
                syncAll(player, cultivation, openScreen, definition.get().id());
                advanced[0] = true;
                return;
            }
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.inner_complete"));
            syncSect(player, cultivation, openScreen);
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
                int cost = SectSpecialtyGameplayService.contributionCost(
                        definition.get().id(), definition.get().shopId(), progress.getSectQuestStage(), entry.cost());
                player.sendSystemMessage(Component.translatable(
                        "command.seeking_immortals.sect.shop.entry",
                        ShopService.itemName(entry),
                        ShopService.itemName(entry),
                        cost));
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
                case UNKNOWN_ENTRY -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.unknown_entry", shopEntryDisplay(result)));
                case UNSUPPORTED_CURRENCY -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.unsupported_currency", shopEntryDisplay(result)));
                case BAD_ITEM -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.bad_shop_item", shopEntryDisplay(result)));
                case BAD_CURRENCY_ITEM -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.bad_currency_item", shopEntryDisplay(result)));
                case OUT_OF_STOCK -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.out_of_stock", shopEntryDisplay(result)));
                case NOT_ENOUGH_CURRENCY -> player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.not_enough_contribution",
                        result.paidCost(),
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
                            result.paidCost(),
                            progress.getContribution()));
                    bought[0] = true;
                }
            }
            syncSect(player, cultivation, false);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return bought[0];
    }

    public static boolean acceptMission(ServerPlayer player, boolean openScreen) {
        boolean[] accepted = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty() || progress.getSectQuestStage() < STAGE_OUTER_DISCIPLE) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_not_member"));
                syncSect(player, cultivation, openScreen);
                return;
            }
            long day = currentDay(player);
            if (progress.isSectMissionAccepted() && progress.getSectMissionDay() == day) {
                String message = progress.isSectMissionCompleted()
                        ? "message.seeking_immortals.sect.mission_already_done"
                        : "message.seeking_immortals.sect.mission_already_active";
                player.sendSystemMessage(Component.translatable(message));
                syncSect(player, cultivation, openScreen);
                return;
            }
            // Wave490: alternate kill/escort/beast/formation generator missions with item dailies.
            if ((day & 1L) == 1L) {
                SectMissionGenerator.Mission generated = SectMissionGenerator.generate(definition.get().id());
                if (!SectMissionGenerator.acceptGenerated(player, generated)) {
                    syncSect(player, cultivation, openScreen);
                    return;
                }
                progress.setSectMission(generated.id(), day);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_accepted_generated",
                        SectMissionGenerator.displayName(generated), generatedTargetDisplay(generated), generated.count(),
                        SectSpecialtyGameplayService.missionContributionReward(
                                definition.get().id(), progress.getSectQuestStage(), generated.rewardContribution())));
            } else {
                SectContentService.MissionDefinition mission = SectContentService.missionForDay(
                        definition.get().id(), progress.getSectQuestStage(), day);
                progress.setSectMission(mission.id(), day);
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_accepted",
                        Component.translatable(mission.titleKey())));
            }
            syncAll(player, cultivation, openScreen, definition.get().id());
            accepted[0] = true;
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return accepted[0];
    }

    public static boolean acceptMission(ServerPlayer player) {
        return acceptMission(player, true);
    }

    public static boolean turnInMission(ServerPlayer player) {
        return turnInMission(player, true);
    }

    private static boolean turnInMission(ServerPlayer player, boolean openScreen) {
        boolean[] turnedIn = { false };
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty() || !progress.isSectMissionAccepted()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_none"));
                syncSect(player, cultivation, openScreen);
                return;
            }
            if (progress.isSectMissionCompleted()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_already_done"));
                syncSect(player, cultivation, openScreen);
                return;
            }

            // Wave490: prefer generated mission authority when payload is active.
            SectMissionGenerator.Mission generated = SectMissionGenerator.activeGenerated(player);
            if (generated != null && generated.id().equals(progress.getSectMissionId())) {
                if (!SectMissionGenerator.turnIn(player, generated)) {
                    syncSect(player, cultivation, openScreen);
                    return;
                }
                int specialtyReward = SectSpecialtyGameplayService.missionContributionReward(
                        definition.get().id(), progress.getSectQuestStage(), generated.rewardContribution());
                int rewardContribution = WorldpackGameplayService.applySectContributionBonus(
                        player, cultivation, specialtyReward);
                progress.addContribution(rewardContribution);
                SectSpecialtyGameplayService.grantMissionPractice(
                        player, definition.get().id(), progress.getSectQuestStage());
                progress.completeSectMission();
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_completed",
                        SectMissionGenerator.displayName(generated),
                        rewardContribution,
                        progress.getContribution()));
                syncAll(player, cultivation, openScreen, definition.get().id());
                turnedIn[0] = true;
                return;
            }

            Optional<SectContentService.MissionDefinition> missionOptional = SectContentService.missionById(definition.get().id(), progress.getSectMissionId());
            if (missionOptional.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.mission_missing_definition"));
                syncSect(player, cultivation, openScreen);
                return;
            }
            SectContentService.MissionDefinition mission = missionOptional.get();
            Item item = resolveItem(mission.itemId());
            if (item == null || item == Items.AIR || !consumeItems(player, item, mission.target())) {
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.sect.mission_missing_items",
                        mission.target(),
                        item == null || item == Items.AIR
                                ? PlayerDisplayText.itemName(mission.itemId())
                                : PlayerDisplayText.itemName(item)));
                syncSect(player, cultivation, openScreen);
                return;
            }
            int specialtyReward = SectSpecialtyGameplayService.missionContributionReward(
                    definition.get().id(), progress.getSectQuestStage(), mission.rewardContribution());
            int rewardContribution = WorldpackGameplayService.applySectContributionBonus(
                    player, cultivation, specialtyReward);
            progress.addContribution(rewardContribution);
            SectSpecialtyGameplayService.grantMissionPractice(
                    player, definition.get().id(), progress.getSectQuestStage());
            progress.completeSectMission();
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.sect.mission_completed",
                    Component.translatable(mission.titleKey()),
                    rewardContribution,
                    progress.getContribution()));
            syncAll(player, cultivation, openScreen, definition.get().id());
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
        openScreen(player, focusSectId, null);
    }

    public static void openScreen(ServerPlayer player, String focusSectId, Entity source) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
                    normalizeSectState(cultivation.getSevenMysteriesQuest());
                    syncSect(player, cultivation, false, focusSectId);
                    openSectHall(player, focusSectId, source);
                },
                () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
    }

    /** Wave490: productized sect hall MenuType open path. */
    public static void openSectHall(ServerPlayer player, String focusSectId) {
        openSectHall(player, focusSectId, null);
    }

    public static void openSectHall(ServerPlayer player, String focusSectId, Entity source) {
        if (player == null) {
            return;
        }
        final String focus = focusSectId == null ? "" : focusSectId;
        MenuAccessContext access = source == null
                ? MenuAccessContext.atPlayer(player)
                : MenuAccessContext.atEntity(player, source);
        net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.seeking_immortals.sect.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new SectHallMenu(id, inv, focus, access);
            }
        }, buf -> {
            buf.writeUtf(focus, 128);
            buf.writeLong(access.token());
        });
    }

    public static void handleClientAction(ServerPlayer player, String action, String targetId, String extra,
                                          long accessToken) {
        String normalizedAction = normalize(action);
        String normalizedTarget = normalize(targetId);
        if (!(player.containerMenu instanceof SectHallMenu menu)
                || !menu.authorizes(player, accessToken)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.menu.invalid_context"), true);
            return;
        }
        if ((ACTION_OPEN.equals(normalizedAction)
                || ACTION_APPLY.equals(normalizedAction)
                || ACTION_JOIN.equals(normalizedAction))
                && !menu.authorizesSect(normalizedTarget.isBlank() ? SECT_ID : normalizedTarget)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.menu.invalid_context"), true);
            return;
        }
        if (requiresFocusedSect(normalizedAction)
                && !isCurrentSect(player, menu.focusSectId())) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.menu.invalid_context"), true);
            return;
        }
        switch (normalizedAction) {
            case ACTION_OPEN -> CultivationHelper.get(player).ifPresent(
                    cultivation -> syncSect(player, cultivation, false, menu.focusSectId()));
            case ACTION_APPLY -> applySect(player, normalizedTarget, false);
            case ACTION_JOIN -> applySect(
                    player, normalizedTarget.isBlank() ? SECT_ID : normalizedTarget, false);
            case ACTION_DIALOGUE -> handleDialogue(player, normalizedTarget, false);
            case ACTION_ADVANCE -> advanceSectQuest(player, false);
            case ACTION_ACCEPT_MISSION -> acceptMission(player, false);
            case ACTION_TURN_IN_MISSION -> turnInMission(player, false);
            case ACTION_BUY -> buy(player, normalizedTarget);
            case ACTION_DONATE_SPIRIT_GRASS -> donateSpiritGrass(player);
            default -> CultivationHelper.get(player).ifPresent(cultivation -> syncSect(player, cultivation, false));
        }
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
        return handleStewardInteraction(player, steward.getSectId(), steward);
    }

    public static boolean handleStewardInteraction(ServerPlayer player, String sectId) {
        return handleStewardInteraction(player, sectId, null);
    }

    public static boolean authorizeStewardInteraction(ServerPlayer player, String sectId) {
        if (player == null) {
            return false;
        }
        SectDefinitionService.SectDefinition definition = definitionOrQinglan(sectId);
        Optional<PlayerCultivation> cultivation = CultivationHelper.get(player);
        if (cultivation.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data"));
            return false;
        }
        QuestProgress progress = cultivation.get().getSevenMysteriesQuest();
        normalizeSectState(progress);
        if (!progress.getSectId().isBlank() && !definition.id().equals(normalize(progress.getSectId()))) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.sect.other_sect", displaySect(progress)));
            return false;
        }
        return true;
    }

    private static boolean handleStewardInteraction(ServerPlayer player, String sectId, Entity source) {
        SectDefinitionService.SectDefinition definition = definitionOrQinglan(sectId);
        if (!authorizeStewardInteraction(player, definition.id())) {
            return true;
        }
        openScreen(player, definition.id(), source);
        return true;
    }

    private static boolean requiresFocusedSect(String action) {
        return ACTION_DIALOGUE.equals(action)
                || ACTION_ADVANCE.equals(action)
                || ACTION_ACCEPT_MISSION.equals(action)
                || ACTION_TURN_IN_MISSION.equals(action)
                || ACTION_BUY.equals(action)
                || ACTION_DONATE_SPIRIT_GRASS.equals(action);
    }

    private static boolean isCurrentSect(ServerPlayer player, String focusSectId) {
        String focus = normalize(focusSectId);
        if (focus.isBlank()) {
            return false;
        }
        return CultivationHelper.get(player).map(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            return focus.equals(normalize(progress.getSectId()));
        }).orElse(false);
    }

    public static void syncSect(ServerPlayer player, PlayerCultivation cultivation, boolean openScreen) {
        syncSect(player, cultivation, openScreen, "");
    }

    public static void syncSect(ServerPlayer player, PlayerCultivation cultivation, boolean openScreen, String focusSectId) {
        QuestProgress progress = cultivation.getSevenMysteriesQuest();
        normalizeSectState(progress);
        currentDefinition(progress).ifPresent(definition ->
                com.xunxian.seekingimmortals.catalog.ManualCatalogService.grantSectSpecialtyMethods(
                        player, definition.id(), progress.getSectQuestStage()));
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
                                SectSpecialtyGameplayService.contributionCost(
                                        definition.id(), definition.shopId(), stage, entry.cost()),
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
        String itemDescriptionId = item == null || item == Items.AIR
                || !PlayerDisplayText.hasTranslation(item.getDescriptionId())
                ? "text.seeking_immortals.unknown_item" : item.getDescriptionId();
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
                SectSpecialtyGameplayService.missionContributionReward(
                        definition.get().id(), stage, mission.rewardContribution()),
                progress.isSectMissionAccepted(),
                progress.isSectMissionCompleted(),
                canTurnIn);
    }

    private static void handleDialogue(ServerPlayer player, String optionId, boolean openScreen) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            normalizeSectState(progress);
            Optional<SectDefinitionService.SectDefinition> definition = currentDefinition(progress);
            if (definition.isEmpty()) {
                syncSect(player, cultivation, openScreen);
                return;
            }
            Optional<SectContentService.DialogueOption> option = SectContentService.optionForStage(
                    definition.get().id(), progress.getSectQuestStage(), optionId);
            String action = option.map(SectContentService.DialogueOption::action).orElse("");
            switch (action) {
                case ACTION_ADVANCE -> advanceSectQuest(player, openScreen);
                case ACTION_ACCEPT_MISSION -> acceptMission(player, openScreen);
                case ACTION_TURN_IN_MISSION -> turnInMission(player, openScreen);
                default -> syncSect(player, cultivation, openScreen);
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
        Component stewardName = PlayerDisplayText.safeCatalogLiteral(
                definition.stewardName(), "宗门执事");
        steward.setCustomName(stewardName);
        steward.setCustomNameVisible(true);
        steward.setPersistenceRequired();
        level.addFreshEntity(steward);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.sect.spawned_steward", stewardName));
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
                .orElse("-");
    }

    private static Component sectDisplay(String sectId) {
        return SectDefinitionService.find(sectId)
                .map(definition -> PlayerDisplayText.safeLiteral(
                        definition.displayZh(), "text.seeking_immortals.unknown_faction"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_faction"));
    }

    private static Component completionDisplay(boolean complete) {
        return Component.translatable(complete
                ? "message.seeking_immortals.sect.yes"
                : "message.seeking_immortals.sect.no");
    }

    private static Component shopEntryDisplay(ShopService.PurchaseResult result) {
        return result == null || result.entry() == null
                ? Component.translatable("text.seeking_immortals.unknown_market_entry")
                : ShopService.itemName(result.entry());
    }

    private static Component generatedTargetDisplay(SectMissionGenerator.Mission mission) {
        if (mission == null) {
            return Component.translatable("text.seeking_immortals.unknown_requirement");
        }
        return switch (normalize(mission.type())) {
            case "gather" -> PlayerDisplayText.itemName(mission.target());
            case "kill" -> Component.literal("目标妖邪");
            case "escort" -> Component.literal("宗门执事");
            case "beast" -> Component.literal("灵兽契约");
            case "formation" -> Component.literal("宗门阵法");
            default -> Component.translatable("text.seeking_immortals.unknown_requirement");
        };
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
