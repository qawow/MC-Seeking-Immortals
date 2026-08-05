package com.xunxian.seekingimmortals.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts;
import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import com.xunxian.seekingimmortals.catalog.AuctionInterestService;
import com.xunxian.seekingimmortals.catalog.AuctionSoftService;
import com.xunxian.seekingimmortals.catalog.BulkCatalogIndexService;
import com.xunxian.seekingimmortals.catalog.ChronicleTradeSoftService;
import com.xunxian.seekingimmortals.catalog.CraftWorldSoftService;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.catalog.FactionConflictSoftService;
import com.xunxian.seekingimmortals.catalog.FactionQuestCatalogService;
import com.xunxian.seekingimmortals.catalog.FlightVehicleService;
import com.xunxian.seekingimmortals.catalog.LoreCatalogService;
import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialManifestService;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TribulationService;
import com.xunxian.seekingimmortals.entity.MarketTraderEntity;
import com.xunxian.seekingimmortals.entity.SpiritStoneBankerEntity;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.npc.DialogueBranchService;
import com.xunxian.seekingimmortals.npc.DialogueTemplateService;
import com.xunxian.seekingimmortals.npc.NamedNpcRegistry;
import com.xunxian.seekingimmortals.npc.NamedNpcRewardService;
import com.xunxian.seekingimmortals.npc.NpcDialogueApi;
import com.xunxian.seekingimmortals.npc.NpcFavorService;
import com.xunxian.seekingimmortals.npc.NpcSpawnService;
import com.xunxian.seekingimmortals.phase.SoftPhaseShellService;
import com.xunxian.seekingimmortals.quest.MainStorySoftService;
import com.xunxian.seekingimmortals.quest.DetailedQuestRuntimeService;
import com.xunxian.seekingimmortals.quest.QuestHookSoftService;
import com.xunxian.seekingimmortals.quest.QuestService;
import com.xunxian.seekingimmortals.quest.SevenMysteriesQuest;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TextQuestDialogueService;
import com.xunxian.seekingimmortals.quest.TextQuestNpcHookService;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.shop.ShopService;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.region.DailyEventScheduler;
import com.xunxian.seekingimmortals.region.RegionEventConfig;
import com.xunxian.seekingimmortals.region.RegionItemsService;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.region.TravelRouteGraph;
import com.xunxian.seekingimmortals.worldpack.SpatialNodeCatalogService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.WorldpackDataService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SeekingImmortalsCommand {
    private SeekingImmortalsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seeking_immortals")
                .then(Commands.literal("lingli").executes(ctx -> showSpiritualPower(ctx.getSource())))
                .then(Commands.literal("qi").executes(ctx -> showSpiritualPower(ctx.getSource())))
                .then(Commands.literal("realm").executes(ctx -> showRealm(ctx.getSource())))
                .then(Commands.literal("root").executes(ctx -> showRoot(ctx.getSource())))
                .then(Commands.literal("artifact")
                        .executes(ctx -> artifactPriority(ctx.getSource(), "P0_launch"))
                        .then(Commands.literal("p0").executes(ctx -> artifactPriority(ctx.getSource(), "P0_launch")))
                        .then(Commands.literal("list").executes(ctx -> artifactCatalog(ctx.getSource())))
                        .then(Commands.literal("files").executes(ctx -> artifactFiles(ctx.getSource())))
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> artifactInfo(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("recipe")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> artifactRecipe(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("refine").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> artifactRefine(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("plan")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> artifactPlan(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("natal")
                                .executes(ctx -> natalStatus(ctx.getSource()))
                                .then(Commands.literal("bind").requires(source -> source.hasPermission(2))
                                        .executes(ctx -> natalBind(ctx.getSource())))
                                .then(Commands.literal("grow").requires(source -> source.hasPermission(2))
                                        .executes(ctx -> natalGrow(ctx.getSource())))))
                .then(Commands.literal("quest")
                        .executes(ctx -> questShow(ctx.getSource()))
                        .then(Commands.literal("start").executes(ctx -> questStart(ctx.getSource())))
                        .then(Commands.literal("check").executes(ctx -> questCheck(ctx.getSource())))
                        .then(Commands.literal("choose")
                                .then(Commands.literal(QuestService.BRANCH_REPORT).executes(ctx -> questChoose(ctx.getSource(), QuestService.BRANCH_REPORT)))
                                .then(Commands.literal(QuestService.BRANCH_SILENT).executes(ctx -> questChoose(ctx.getSource(), QuestService.BRANCH_SILENT)))
                                .then(Commands.literal(QuestService.BRANCH_BLACKMAIL).executes(ctx -> questChoose(ctx.getSource(), QuestService.BRANCH_BLACKMAIL))))
                        .then(Commands.literal("reset").requires(source -> source.hasPermission(2)).executes(ctx -> questReset(ctx.getSource())))
                        .then(Commands.literal("advance").requires(source -> source.hasPermission(2)).executes(ctx -> questAdvance(ctx.getSource())))
                        .then(Commands.literal("spawn_mo_lao").requires(source -> source.hasPermission(2)).executes(ctx -> questSpawn(ctx.getSource(), SevenMysteriesQuest.NPC_MO_LAO)))
                        .then(Commands.literal("spawn_steward").requires(source -> source.hasPermission(2)).executes(ctx -> questSpawn(ctx.getSource(), SevenMysteriesQuest.NPC_STEWARD)))
                        .then(Commands.literal("place_secret_room").requires(source -> source.hasPermission(2)).executes(ctx -> questPlaceSecretRoom(ctx.getSource())))
                        .then(Commands.literal("place_yue_portal").requires(source -> source.hasPermission(2)).executes(ctx -> questPlaceYuePortal(ctx.getSource())))
                        .then(Commands.literal("give_evidence").requires(source -> source.hasPermission(2)).executes(ctx -> questGiveEvidence(ctx.getSource())))
                        .then(Commands.literal("trigger_attack").requires(source -> source.hasPermission(2)).executes(ctx -> questTriggerAttack(ctx.getSource())))
                        .then(Commands.literal("text")
                                .executes(ctx -> textQuestList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> textQuestList(ctx.getSource())))
                                .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestStart(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("advance").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestAdvance(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("status")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestStatus(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("cost")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestCost(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("branch").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("choice", StringArgumentType.word())
                                                        .executes(ctx -> textQuestBranch(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "choice"))))))
                                .then(Commands.literal("talk").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestTalk(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                                .then(Commands.argument("choice", StringArgumentType.word())
                                                        .executes(ctx -> textQuestTalkAct(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "choice"))))))
                                .then(Commands.literal("gui").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestGui(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("hooks")
                                        .executes(ctx -> textQuestHooks(ctx.getSource()))
                                        .then(Commands.literal("accept").requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> textQuestHookAccept(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestHookPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("playable")
                                        .executes(ctx -> detailedQuestList(ctx.getSource()))
                                        .then(Commands.literal("list")
                                                .executes(ctx -> detailedQuestList(ctx.getSource())))
                                        .then(Commands.literal("status")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> detailedQuestStatus(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("claim")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> detailedQuestClaim(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> detailedQuestStart(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("prove").requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .then(Commands.argument("step", IntegerArgumentType.integer(1, 95))
                                                                .executes(ctx -> detailedQuestProve(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "id"),
                                                                        IntegerArgumentType.getInteger(ctx, "step")))))))
                                .then(Commands.literal("spawn_npc").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestSpawnNpc(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("interact").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("npc", StringArgumentType.word())
                                                .executes(ctx -> textQuestInteract(ctx.getSource(), StringArgumentType.getString(ctx, "npc")))))
                                .then(Commands.literal("story")
                                        .executes(ctx -> mainStoryList(ctx.getSource()))
                                        .then(Commands.literal("list").executes(ctx -> mainStoryList(ctx.getSource())))
                                        .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> mainStoryStart(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("complete").requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> mainStoryComplete(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))))
                .then(Commands.literal("npc")
                        .executes(ctx -> npcSummary(ctx.getSource()))
                        .then(Commands.literal("list")
                                .executes(ctx -> npcSummary(ctx.getSource()))
                                .then(Commands.argument("region", StringArgumentType.word())
                                        .executes(ctx -> npcListRegion(ctx.getSource(), StringArgumentType.getString(ctx, "region")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> npcInfo(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("talk").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> npcTalk(ctx.getSource(), StringArgumentType.getString(ctx, "id"), ""))
                                        .then(Commands.argument("tree", StringArgumentType.word())
                                                .executes(ctx -> npcTalk(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "tree"))))))
                        .then(Commands.literal("act").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("choice", StringArgumentType.word())
                                        .executes(ctx -> npcAct(ctx.getSource(), StringArgumentType.getString(ctx, "choice")))))
                        .then(Commands.literal("favor")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> npcFavor(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("spawn").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> npcSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("ensure_region").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("region", StringArgumentType.word())
                                        .executes(ctx -> npcEnsureRegion(ctx.getSource(), StringArgumentType.getString(ctx, "region"), 3))
                                        .then(Commands.argument("limit", IntegerArgumentType.integer(1, 16))
                                                .executes(ctx -> npcEnsureRegion(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "region"),
                                                        IntegerArgumentType.getInteger(ctx, "limit")))))))
                .then(Commands.literal("market")
                        .executes(ctx -> marketList(ctx.getSource()))
                        .then(Commands.literal("open").requires(source -> source.hasPermission(2))
                                .executes(ctx -> marketOpen(ctx.getSource()))
                                .then(Commands.argument("shopId", StringArgumentType.word())
                                        .executes(ctx -> marketOpen(ctx.getSource(), StringArgumentType.getString(ctx, "shopId")))))
                        .then(Commands.literal("list")
                                .executes(ctx -> marketList(ctx.getSource()))
                                .then(Commands.argument("shopId", StringArgumentType.word())
                                        .executes(ctx -> marketList(ctx.getSource(), StringArgumentType.getString(ctx, "shopId")))))
                        .then(Commands.literal("buy").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("entryOrShop", StringArgumentType.word())
                                        .executes(ctx -> marketBuy(ctx.getSource(), ShopService.MARKET_HERBAL_STALL,
                                                StringArgumentType.getString(ctx, "entryOrShop")))
                                        .then(Commands.argument("entry", StringArgumentType.word())
                                                .executes(ctx -> marketBuy(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "entryOrShop"),
                                                        StringArgumentType.getString(ctx, "entry"))))))
                        .then(Commands.literal("spawn_trader").requires(source -> source.hasPermission(2))
                                .executes(ctx -> marketSpawnTrader(ctx.getSource())))
                        .then(Commands.literal("spawn_banker").requires(source -> source.hasPermission(2))
                                .executes(ctx -> marketSpawnBanker(ctx.getSource()))))
                .then(Commands.literal("worldpack")
                        .executes(ctx -> worldpackOpen(ctx.getSource()))
                        .then(Commands.literal("open").executes(ctx -> worldpackOpen(ctx.getSource())))
                        .then(Commands.literal("travel")
                                .then(Commands.argument("region", StringArgumentType.word())
                                        .executes(ctx -> worldpackTravel(ctx.getSource(), StringArgumentType.getString(ctx, "region")))))
                        .then(Commands.literal("enter").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("realm", StringArgumentType.word())
                                        .executes(ctx -> worldpackEnter(ctx.getSource(), StringArgumentType.getString(ctx, "realm")))))
                        .then(Commands.literal("return").executes(ctx -> worldpackReturn(ctx.getSource())))
                        .then(Commands.literal("set_anchor").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("anchor", StringArgumentType.word())
                                        .executes(ctx -> worldpackSetAnchor(ctx.getSource(), StringArgumentType.getString(ctx, "anchor")))))
                        .then(Commands.literal("regions").executes(ctx -> worldpackRegions(ctx.getSource())))
                        .then(Commands.literal("realms").executes(ctx -> worldpackRealms(ctx.getSource())))
                        .then(Commands.literal("events").executes(ctx -> worldpackEvents(ctx.getSource())))
                        .then(Commands.literal("daily_events")
                                .executes(ctx -> regionDailyEventsStatus(ctx.getSource()))
                                .then(Commands.literal("status").executes(ctx -> regionDailyEventsStatus(ctx.getSource())))
                                .then(Commands.literal("enable").requires(source -> source.hasPermission(2))
                                        .executes(ctx -> regionDailyEventsToggle(ctx.getSource(), true)))
                                .then(Commands.literal("disable").requires(source -> source.hasPermission(2))
                                        .executes(ctx -> regionDailyEventsToggle(ctx.getSource(), false)))
                                .then(Commands.literal("claim")
                                        .executes(ctx -> regionDailyEventsClaim(ctx.getSource())))
                                .then(Commands.literal("roll").requires(source -> source.hasPermission(2))
                                        .executes(ctx -> regionDailyEventsRoll(ctx.getSource())))))
                .then(Commands.literal("region")
                        .executes(ctx -> regionHere(ctx.getSource()))
                        .then(Commands.literal("here").executes(ctx -> regionHere(ctx.getSource())))
                        .then(Commands.literal("list").executes(ctx -> regionList(ctx.getSource())))
                        .then(Commands.literal("items")
                                .then(Commands.argument("region", StringArgumentType.word())
                                        .executes(ctx -> regionItems(ctx.getSource(), StringArgumentType.getString(ctx, "region")))))
                        .then(Commands.literal("routes")
                                .then(Commands.argument("from", StringArgumentType.word())
                                        .then(Commands.argument("to", StringArgumentType.word())
                                                .executes(ctx -> regionRoutes(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "from"),
                                                        StringArgumentType.getString(ctx, "to")))))))
                .then(Commands.literal("catalog")
                        .executes(ctx -> catalogSummary(ctx.getSource()))
                        .then(Commands.literal("summary").executes(ctx -> catalogSummary(ctx.getSource())))
                        .then(Commands.literal("manual").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogStudyManual(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("flight")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogBoardFlight(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("methods")
                                .executes(ctx -> catalogMethods(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogMethods(ctx.getSource())))
                                .then(Commands.literal("learn").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogLearnMethod(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("studied").executes(ctx -> catalogStudiedMethods(ctx.getSource()))))
                        .then(Commands.literal("realms").executes(ctx -> catalogRealms(ctx.getSource())))
                        .then(Commands.literal("quests").executes(ctx -> catalogQuests(ctx.getSource())))
                        .then(Commands.literal("sects").executes(ctx -> catalogSects(ctx.getSource())))
                        .then(Commands.literal("bands").executes(ctx -> catalogBands(ctx.getSource())))
                        .then(Commands.literal("chapters").executes(ctx -> catalogChapters(ctx.getSource())))
                        .then(Commands.literal("manifest").executes(ctx -> catalogManifest(ctx.getSource())))
                        .then(Commands.literal("lore").executes(ctx -> catalogLore(ctx.getSource())))
                        .then(Commands.literal("factions").executes(ctx -> catalogFactions(ctx.getSource())))
                        .then(Commands.literal("auction")
                                .executes(ctx -> catalogAuctionList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogAuctionList(ctx.getSource())))
                                .then(Commands.literal("open").requires(source -> source.hasPermission(2))
                                        .executes(ctx -> catalogAuctionOpen(ctx.getSource())))
                                .then(Commands.literal("interest").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogAuctionInterest(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("bid").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogAuctionBid(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("settle").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogAuctionSettle(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogAuctionPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("spatial")
                                .executes(ctx -> catalogSpatialList(ctx.getSource()))
                                .then(Commands.literal("travel").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogSpatialTravel(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogSpatialPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("dimensions")
                                .executes(ctx -> catalogDimensions(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogDimensions(ctx.getSource())))
                                .then(Commands.literal("get")
                                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                                .executes(ctx -> catalogDimensionGet(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("travel")
                                        .then(Commands.argument("route", StringArgumentType.word())
                                                .executes(ctx -> catalogDimensionTravel(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "route"))))))
                        .then(Commands.literal("ascension")
                                .executes(ctx -> catalogAscensionStatus(ctx.getSource()))
                                .then(Commands.literal("status").executes(ctx -> catalogAscensionStatus(ctx.getSource())))
                                .then(Commands.literal("attempt").executes(ctx -> catalogAscensionAttempt(ctx.getSource(), false)))
                                .then(Commands.literal("confirm").executes(ctx -> catalogAscensionAttempt(ctx.getSource(), true)))
                                .then(Commands.literal("cancel").executes(ctx -> catalogAscensionCancel(ctx.getSource())))
                                // Admin-only diagnostic: successful ascension no longer keeps a reclaim snapshot.
                                .then(Commands.literal("restore").requires(source -> source.hasPermission(2))
                                        .executes(ctx -> catalogAscensionRestore(ctx.getSource()))))
                        .then(Commands.literal("reputation")
                                .executes(ctx -> catalogReputationList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogReputationList(ctx.getSource())))
                                .then(Commands.literal("discount")
                                        .then(Commands.argument("shopId", StringArgumentType.word())
                                                .executes(ctx -> catalogReputationDiscount(ctx.getSource(), StringArgumentType.getString(ctx, "shopId")))))
                                .then(Commands.literal("get")
                                        .then(Commands.argument("faction", StringArgumentType.word())
                                                .executes(ctx -> catalogReputationGet(ctx.getSource(), StringArgumentType.getString(ctx, "faction")))))
                                .then(Commands.literal("add").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("faction", StringArgumentType.word())
                                                .then(Commands.argument("delta", IntegerArgumentType.integer(-1000, 1000))
                                                        .executes(ctx -> catalogReputationAdd(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "faction"),
                                                                IntegerArgumentType.getInteger(ctx, "delta")))))))
                        .then(Commands.literal("conflicts")
                                .executes(ctx -> catalogConflictsList(ctx.getSource()))
                                .then(Commands.literal("accept")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogConflictsAccept(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("side")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("side", StringArgumentType.word())
                                                        .executes(ctx -> catalogConflictsSide(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "side"))))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogConflictsPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("bulk")
                                .executes(ctx -> catalogBulkList(ctx.getSource()))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> catalogBulkShow(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("refine")
                                .executes(ctx -> catalogRefineList(ctx.getSource()))
                                .then(Commands.literal("craft").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogRefineCraft(ctx.getSource(), StringArgumentType.getString(ctx, "id"), 1))
                                                .then(Commands.argument("grade", IntegerArgumentType.integer(1, 3))
                                                        .executes(ctx -> catalogRefineCraft(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                IntegerArgumentType.getInteger(ctx, "grade"))))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogRefinePreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("formations")
                                .executes(ctx -> catalogFormationsList(ctx.getSource()))
                                .then(Commands.literal("deploy").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogFormationsDeploy(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogFormationsPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("station")
                                .then(Commands.literal("inspect")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> stationInspect(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("form")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> stationForm(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("repair")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> stationRepair(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("dismantle").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> stationDismantle(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("overhaul")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> stationOverhaul(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("talisman")
                                .executes(ctx -> catalogTalismanList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogTalismanList(ctx.getSource())))
                                .then(Commands.literal("craft").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogTalismanCraft(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("puppet")
                                .executes(ctx -> catalogPuppetList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogPuppetList(ctx.getSource())))
                                .then(Commands.literal("craft").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogPuppetCraft(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("repair").executes(ctx -> catalogPuppetRepair(ctx.getSource()))))
                        .then(Commands.literal("chronicle")
                                .executes(ctx -> catalogChronicleList(ctx.getSource()))
                                .then(Commands.literal("discover").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogChronicleDiscover(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogChroniclePreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("trade")
                                .executes(ctx -> catalogTradeList(ctx.getSource()))
                                .then(Commands.literal("embark")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogTradeEmbark(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogTradePreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("summon")
                                .executes(ctx -> catalogSummonList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogSummonList(ctx.getSource())))
                                .then(Commands.literal("stance")
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .executes(ctx -> catalogSummonStance(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                                .then(Commands.literal("dismiss").executes(ctx -> catalogSummonDismiss(ctx.getSource())))
                                .then(Commands.literal("repair").executes(ctx -> catalogPuppetRepair(ctx.getSource())))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogSummon(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("beast")
                                .executes(ctx -> beastList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> beastList(ctx.getSource())))
                                .then(Commands.literal("contract").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> beastContract(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("feed")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> beastFeed(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("summon")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> beastSummon(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("has")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogHas(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("boss").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> bossSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("phase")
                        .executes(ctx -> phaseStatus(ctx.getSource()))
                        .then(Commands.literal("mark").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> phaseMark(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("enter").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> phaseEnter(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("mission").requires(source -> source.hasPermission(2))
                        .executes(ctx -> missionGenerate(ctx.getSource()))
                        .then(Commands.literal("gen").executes(ctx -> missionGenerate(ctx.getSource()))))
                .then(Commands.literal("war")
                        .executes(ctx -> warStatus(ctx.getSource()))
                        .then(Commands.literal("status").executes(ctx -> warStatus(ctx.getSource())))
                        .then(Commands.literal("start").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("factionA", StringArgumentType.word())
                                        .then(Commands.argument("factionB", StringArgumentType.word())
                                                .executes(ctx -> warStart(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "factionA"),
                                                        StringArgumentType.getString(ctx, "factionB"),
                                                        "", 10))
                                                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 120))
                                                        .executes(ctx -> warStart(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "factionA"),
                                                                StringArgumentType.getString(ctx, "factionB"),
                                                                "",
                                                                IntegerArgumentType.getInteger(ctx, "minutes"))))
                                                // Wave487: optional third army for multi-army battlefield simulation.
                                                .then(Commands.argument("factionC", StringArgumentType.word())
                                                        .executes(ctx -> warStart(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "factionA"),
                                                                StringArgumentType.getString(ctx, "factionB"),
                                                                StringArgumentType.getString(ctx, "factionC"),
                                                                10))
                                                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 120))
                                                                .executes(ctx -> warStart(ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "factionA"),
                                                                        StringArgumentType.getString(ctx, "factionB"),
                                                                        StringArgumentType.getString(ctx, "factionC"),
                                                                        IntegerArgumentType.getInteger(ctx, "minutes"))))))))
                        .then(Commands.literal("stop").requires(source -> source.hasPermission(2))
                                .executes(ctx -> warStop(ctx.getSource()))))
                .then(Commands.literal("sect")
                        .executes(ctx -> sectStatus(ctx.getSource()))
                        .then(Commands.literal("status").executes(ctx -> sectStatus(ctx.getSource())))
                        .then(Commands.literal("open").requires(source -> source.hasPermission(2))
                                .executes(ctx -> sectOpen(ctx.getSource())))
                        .then(Commands.literal("join").requires(source -> source.hasPermission(2))
                                .executes(ctx -> sectJoin(ctx.getSource())))
                        .then(Commands.literal("candidates").executes(ctx -> sectCandidates(ctx.getSource())))
                        .then(Commands.literal("apply").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("sectId", StringArgumentType.word())
                                        .executes(ctx -> sectApply(ctx.getSource(), StringArgumentType.getString(ctx, "sectId")))))
                        .then(Commands.literal("advance").requires(source -> source.hasPermission(2))
                                .executes(ctx -> sectAdvance(ctx.getSource())))
                        .then(Commands.literal("shop").executes(ctx -> sectShop(ctx.getSource())))
                        .then(Commands.literal("buy").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("entry", StringArgumentType.word())
                                        .executes(ctx -> sectBuy(ctx.getSource(), StringArgumentType.getString(ctx, "entry")))))
                        .then(Commands.literal("donate").requires(source -> source.hasPermission(2))
                                .then(Commands.literal("spirit_grass").executes(ctx -> sectDonateSpiritGrass(ctx.getSource()))))
                        .then(Commands.literal("spawn_steward").requires(source -> source.hasPermission(2))
                                .executes(ctx -> sectSpawnSteward(ctx.getSource(), SectContributionService.SECT_ID))
                                .then(Commands.argument("sectId", StringArgumentType.word())
                                        .executes(ctx -> sectSpawnSteward(ctx.getSource(), StringArgumentType.getString(ctx, "sectId")))))
                        .then(Commands.literal("place_outpost").requires(source -> source.hasPermission(2))
                                .executes(ctx -> sectPlaceOutpost(ctx.getSource(), SectContributionService.SECT_ID))
                                .then(Commands.argument("sectId", StringArgumentType.word())
                                        .executes(ctx -> sectPlaceOutpost(ctx.getSource(), StringArgumentType.getString(ctx, "sectId"))))))
                .then(Commands.literal("affliction").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("severe_injury").executes(ctx -> applySevereInjury(ctx.getSource())))
                        .then(Commands.literal("heart_demon").executes(ctx -> applyHeartDemon(ctx.getSource())))
                        .then(Commands.literal("realm_fall").executes(ctx -> applyRealmFall(ctx.getSource())))
                        .then(Commands.literal("shattered_core").executes(ctx -> applyShatteredCore(ctx.getSource()))))
                .then(Commands.literal("debug").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set_cultivation")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> debugSetCultivation(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("set_core_attrs")
                                .then(Commands.argument("divSense", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("bodyRef", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("qiDevRisk", IntegerArgumentType.integer(0, 100))
                                                        .then(Commands.argument("tribRes", IntegerArgumentType.integer(0, 90))
                                                                .executes(ctx -> debugSetCoreAttrs(ctx.getSource(),
                                                                        IntegerArgumentType.getInteger(ctx, "divSense"),
                                                                        IntegerArgumentType.getInteger(ctx, "bodyRef"),
                                                                        IntegerArgumentType.getInteger(ctx, "qiDevRisk"),
                                                                        IntegerArgumentType.getInteger(ctx, "tribRes"))))))))
                        .then(Commands.literal("start_tribulation")
                                .then(Commands.argument("target_realm", StringArgumentType.word())
                                        .executes(ctx -> debugStartTribulation(ctx.getSource(), StringArgumentType.getString(ctx, "target_realm")))))
                        .then(Commands.literal("add_contribution")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> debugAddContribution(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("fill_mana").executes(ctx -> debugFillMana(ctx.getSource())))
                        .then(Commands.literal("unlock_skills").executes(ctx -> debugUnlockSkills(ctx.getSource()))))
                .then(Commands.literal("live_smoke")
                        .executes(ctx -> liveSmoke(ctx.getSource()))
                        .then(Commands.literal("run").executes(ctx -> liveSmoke(ctx.getSource())))
                        .then(Commands.literal("sign")
                                .executes(ctx -> liveSmokeSign(ctx.getSource(), "manual_client_pass"))
                                .then(Commands.argument("note", StringArgumentType.greedyString())
                                        .executes(ctx -> liveSmokeSign(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "note")))))
                        .then(Commands.literal("mp")
                                .executes(ctx -> liveSmokeMp(ctx.getSource()))
                                .then(Commands.literal("run").executes(ctx -> liveSmokeMp(ctx.getSource())))
                                .then(Commands.literal("sign")
                                        .executes(ctx -> liveSmokeMpSign(ctx.getSource(), "manual_mp_pass"))
                                        .then(Commands.argument("note", StringArgumentType.greedyString())
                                                .executes(ctx -> liveSmokeMpSign(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "note")))))))
                .then(Commands.literal("breakthrough").executes(ctx -> breakthrough(ctx.getSource())))
                .then(Commands.literal("lore")
                        .executes(ctx -> loreHub(ctx.getSource()))
                        .then(Commands.literal("hub").executes(ctx -> loreOpen(ctx.getSource(), "compendium")))
                        .then(Commands.literal("compendium").executes(ctx -> loreOpen(ctx.getSource(), "compendium")))
                        .then(Commands.literal("bestiary").executes(ctx -> loreOpen(ctx.getSource(), "bestiary")))
                        .then(Commands.literal("chronicle").executes(ctx -> loreOpen(ctx.getSource(), "chronicle")))
                        .then(Commands.literal("glossary")
                                .executes(ctx -> loreGlossary(ctx.getSource(), ""))
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(ctx -> loreGlossary(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "query")))))
                        .then(Commands.literal("numeric").executes(ctx -> loreNumeric(ctx.getSource())))
                        .then(Commands.literal("visual").executes(ctx -> loreVisual(ctx.getSource())))
                        .then(Commands.literal("summary").executes(ctx -> loreHub(ctx.getSource())))
                        .then(Commands.literal("lang").executes(ctx -> loreLangAudit(ctx.getSource())))
                        .then(Commands.literal("patchouli").executes(ctx -> lorePatchouliStatus(ctx.getSource())))));
    }

    private static int liveSmoke(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LiveSmokeChecklistService.print(player);
        return 1;
    }

    private static int liveSmokeSign(CommandSourceStack source, String note) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return LiveSmokeChecklistService.sign(player, note) ? 1 : 0;
    }

    private static int liveSmokeMp(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LiveSmokeChecklistService.print(player, true);
        return 1;
    }

    private static int liveSmokeMpSign(CommandSourceStack source, String note) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return LiveSmokeChecklistService.signMultiplayer(player, note) ? 1 : 0;
    }

    private static int showSpiritualPower(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.qi",
                cultivation.getMana(),
                cultivation.getManaMax(),
                cultivation.getCultivation(),
                cultivation.getDivSense(),
                cultivation.getBodyRef(),
                cultivation.getQiDevRisk(),
                cultivation.getTribRes(),
                tribulationStatus(cultivation)), false));
        return 1;
    }

    private static int questShow(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.show(source.getPlayerOrException());
        return 1;
    }

    private static int textQuestList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.header",
                TextQuestChainService.chainCount()), false);
        int shown = 0;
        for (TextQuestChainService.ChainProgress progress : TextQuestChainService.listProgress(player)) {
            MutableComponent line = Component.empty()
                    .append(questDisplay(progress.id()))
                    .append("｜进度 ")
                    .append(Integer.toString(progress.stage()))
                    .append("/")
                    .append(Integer.toString(progress.stepCount()))
                    .append(progress.complete() ? "｜已完成" : "｜进行中");
            source.sendSuccess(() -> line, false);
            if (++shown >= 20) {
                int remaining = TextQuestChainService.chainCount() - shown;
                source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.truncated", remaining), false);
                break;
            }
        }
        return 1;
    }

    private static int textQuestStart(CommandSourceStack source, String id) throws CommandSyntaxException {
        return TextQuestChainService.start(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int detailedQuestList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.detailed_quest.header",
                DetailedQuestRuntimeService.chainCount(), DetailedQuestRuntimeService.stepCount()), false);
        for (DetailedQuestRuntimeService.Progress progress : DetailedQuestRuntimeService.listProgress(player)) {
            DetailedQuestRuntimeService.Chain chain = DetailedQuestRuntimeService.find(progress.id()).orElse(null);
            if (chain == null) {
                continue;
            }
            MutableComponent line;
            if (progress.complete()) {
                line = Component.translatable("command.seeking_immortals.detailed_quest.line.complete",
                        chain.display(), progress.stepCount());
            } else if (progress.started()) {
                line = Component.translatable("command.seeking_immortals.detailed_quest.line.active",
                        chain.display(), progress.stage(), progress.stepCount());
            } else {
                line = Component.translatable("command.seeking_immortals.detailed_quest.line.unclaimed",
                        chain.display(), progress.stepCount());
            }
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int detailedQuestStatus(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (DetailedQuestRuntimeService.find(id).isEmpty()) {
            source.sendFailure(Component.translatable("message.seeking_immortals.detailed_quest.unknown", id));
            return 0;
        }
        return DetailedQuestRuntimeService.showCurrentStep(player, id) ? 1 : 0;
    }

    private static int detailedQuestClaim(CommandSourceStack source, String id) throws CommandSyntaxException {
        DetailedQuestRuntimeService.Progress progress = DetailedQuestRuntimeService.progressOf(
                source.getPlayerOrException(), id);
        return detailedQuestProve(source, id, progress.stage());
    }

    private static int detailedQuestStart(CommandSourceStack source, String id) throws CommandSyntaxException {
        return DetailedQuestRuntimeService.start(source.getPlayerOrException(), id,
                DetailedQuestRuntimeService.Evidence.of()) ? 1 : 0;
    }

    private static int detailedQuestProve(CommandSourceStack source, String id, int step)
            throws CommandSyntaxException {
        com.xunxian.seekingimmortals.quest.DetailedQuestProofService.Result result =
                com.xunxian.seekingimmortals.quest.DetailedQuestProofService.adminProve(
                        source.getPlayerOrException(), id, step);
        if (result.status() == com.xunxian.seekingimmortals.quest.DetailedQuestProofService.Status.ACCEPTED) {
            source.sendSuccess(() -> Component.translatable(
                    "command.seeking_immortals.detailed_quest.admin_proof", id, step), false);
            return 1;
        }
        source.sendFailure(Component.translatable(
                "command.seeking_immortals.detailed_quest.admin_proof_failed", result.reason()));
        return 0;
    }

    private static int textQuestAdvance(CommandSourceStack source, String id) throws CommandSyntaxException {
        return TextQuestChainService.advance(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int textQuestStatus(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, id);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.status",
                questDisplay(progress.id()), progress.stage(), progress.stepCount(),
                Component.literal(progress.complete() ? "已完成" : "进行中")), false);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.branch_status",
                branchDisplay(TextQuestChainService.getBranch(player, id)),
                npcDisplay(TextQuestChainService.getNpc(player, id))), false);
        previewQuestCost(source, player, progress);
        return 1;
    }

    private static int textQuestCost(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, id);
        previewQuestCost(source, player, progress);
        return 1;
    }

    private static void previewQuestCost(CommandSourceStack source, ServerPlayer player,
                                         TextQuestChainService.ChainProgress progress) {
        if (progress.complete() || progress.stepCount() <= 0) {
            source.sendSuccess(() -> Component.translatable("message.seeking_immortals.text_quest.cost_free"), false);
            return;
        }
        int next = Math.max(1, progress.stage()) + (progress.stage() <= 0 ? 0 : 1);
        if (progress.stage() <= 0) {
            next = 1;
        }
        var cost = TextQuestChainService.stageCostFor(progress.id(), next, progress.stepCount());
        if (cost.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.seeking_immortals.text_quest.cost_free"), false);
            return;
        }
        TextQuestChainService.StageCost stageCost = cost.get();
        int owned = TextQuestChainService.countOwned(player, stageCost);
        source.sendSuccess(() -> Component.translatable("message.seeking_immortals.text_quest.cost_preview",
                itemDisplay(stageCost.itemId()), stageCost.count(), owned), false);
    }

    private static int textQuestBranch(CommandSourceStack source, String id, String choice) throws CommandSyntaxException {
        return TextQuestChainService.chooseBranch(source.getPlayerOrException(), id, choice) ? 1 : 0;
    }


    private static int natalStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String id = com.xunxian.seekingimmortals.artifact.NatalBindingService.boundId(player);
        int growth = com.xunxian.seekingimmortals.artifact.NatalBindingService.growth(player);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.natal.status",
                id.isBlank() ? Component.literal("未绑定") : artifactDisplay(id), growth), false);
        return 1;
    }

    private static int natalBind(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return com.xunxian.seekingimmortals.artifact.NatalBindingService.bind(player, player.getMainHandItem()) ? 1 : 0;
    }

    private static int natalGrow(CommandSourceStack source) throws CommandSyntaxException {
        return com.xunxian.seekingimmortals.artifact.NatalBindingService.grow(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int artifactPlan(CommandSourceStack source, String recipeId) throws CommandSyntaxException {
        source.getPlayerOrException();
        ArtifactDataService.RefinementRecipe recipe = ArtifactDataService.builtin().findRecipe(recipeId).orElse(null);
        if (recipe == null) {
            source.sendFailure(Component.translatable("message.seeking_immortals.artifact.refine.unknown_recipe",
                    Component.literal("未收录配方")));
            return 0;
        }
        MutableComponent materials = Component.empty();
        for (int i = 0; i < recipe.materials().size(); i++) {
            ArtifactDataService.MaterialRequirement material = recipe.materials().get(i);
            if (i > 0) {
                materials.append("、");
            }
            materials.append(itemDisplay(material.id())).append(" x").append(Integer.toString(material.count()));
        }
        source.sendSuccess(() -> Component.translatable("message.seeking_immortals.artifact.refine.plan",
                safeText(recipe.display(), "text.seeking_immortals.unknown_item"),
                realmDisplay(recipe.realmMin()),
                String.format(Locale.ROOT, "%.0f%%", Math.max(0.0D, Math.min(1.0D, recipe.baseSuccessRate())) * 100.0D),
                recipe.materials().isEmpty() ? Component.literal("无") : materials), false);
        source.sendSuccess(() -> Component.literal("材料齐备后，可在对应炼器工位继续炼制。"), false);
        return 1;
    }

    private static int textQuestTalk(CommandSourceStack source, String id) throws CommandSyntaxException {
        return TextQuestDialogueService.talk(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int npcSummary(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("命名角色：" + NamedNpcRegistry.count()
                + "，对话树：" + DialogueBranchService.treeCount()
                + "，角色模板：" + DialogueTemplateService.archetypeCount()
                + "，奖励条目：" + NamedNpcRewardService.entryCount()), false);
        int shown = 0;
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
            if (shown++ >= 8) {
                break;
            }
            MutableComponent line = Component.empty()
                    .append(npcDisplay(npc.id()))
                    .append("｜")
                    .append(regionDisplay(npc.regionId()));
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int npcListRegion(CommandSourceStack source, String region) {
        List<NamedNpcRegistry.NamedNpc> list = NamedNpcRegistry.byRegion(region);
        source.sendSuccess(() -> Component.literal("区域 ").append(regionDisplay(region))
                .append("，角色 ").append(Integer.toString(list.size())), false);
        int shown = 0;
        for (NamedNpcRegistry.NamedNpc npc : list) {
            if (shown++ >= 12) {
                break;
            }
            source.sendSuccess(() -> npcDisplay(npc.id()), false);
        }
        return list.isEmpty() ? 0 : 1;
    }

    private static int npcInfo(CommandSourceStack source, String id) {
        return NamedNpcRegistry.find(id).map(npc -> {
            source.sendSuccess(() -> Component.literal("角色：").append(npcDisplay(npc.id())), false);
            source.sendSuccess(() -> Component.literal("所属宗门：").append(factionDisplay(npc.sectId()))
                    .append("｜势力：").append(factionDisplay(npc.factionId()))
                    .append("｜区域：").append(regionDisplay(npc.regionId())), false);
            if (PlayerDisplayText.isSafe(npc.description())) {
                source.sendSuccess(() -> Component.literal("简介：").append(npc.description()), false);
            }
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.translatable("text.seeking_immortals.unknown_affiliation"));
            return 0;
        });
    }

    private static int npcTalk(CommandSourceStack source, String id, String tree) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = NpcDialogueApi.startDialogue(player, id, tree);
        source.sendSuccess(() -> Component.literal(ok ? "已开始对话：" : "无法开始对话：")
                .append(npcDisplay(id)), false);
        return ok ? 1 : 0;
    }

    private static int npcAct(CommandSourceStack source, String choice) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = NpcDialogueApi.selectNext(player, choice);
        source.sendSuccess(() -> Component.literal(ok ? "对话选项已执行。" : "当前对话选项不可用。"), false);
        return ok ? 1 : 0;
    }

    private static int npcFavor(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int value = NpcFavorService.get(player, id);
        source.sendSuccess(() -> Component.literal("对").append(npcDisplay(id))
                .append("的好感：").append(Integer.toString(value)), false);
        return 1;
    }

    private static int npcSpawn(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var spawned = NpcSpawnService.spawnNamed(player.serverLevel(), player.blockPosition(), id);
        if (spawned.isEmpty()) {
            source.sendFailure(Component.literal("无法生成该角色。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已生成角色：").append(npcDisplay(id)), true);
        return 1;
    }

    private static int npcEnsureRegion(CommandSourceStack source, String region, int limit) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<String> spawned = NpcSpawnService.ensureRegionNpcs(player.serverLevel(), player.blockPosition(), region, limit);
        source.sendSuccess(() -> Component.literal("已在 ").append(regionDisplay(region))
                .append("安排角色：").append(Integer.toString(spawned.size())), true);
        return spawned.size();
    }

    private static int textQuestTalkAct(CommandSourceStack source, String id, String choice) throws CommandSyntaxException {
        return TextQuestDialogueService.act(source.getPlayerOrException(), id, choice) ? 1 : 0;
    }

    private static int textQuestGui(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket.send(player, id);
        // Also push current dialogue lines to chat for immediate feedback.
        TextQuestDialogueService.talk(player, id);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.gui_opened",
                questDisplay(id)), false);
        return 1;
    }

    private static int textQuestHooks(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.hooks.header",
                QuestHookSoftService.hookCount()), false);
        int shown = 0;
        for (var entry : FactionQuestCatalogService.builtin().questHooks().values()) {
            MutableComponent line = safeText(entry.display(), "text.seeking_immortals.unknown_quest").copy();
            QuestHookSoftService.mappedChainId(entry.id()).ifPresent(chain ->
                    line.append("｜对应任务：").append(questDisplay(chain)));
            source.sendSuccess(() -> line, false);
            if (++shown >= 20) {
                break;
            }
        }
        return 1;
    }

    private static int textQuestHookPreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        var entry = FactionQuestCatalogService.builtin().questHooks()
                .get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT));
        if (entry == null) {
            source.sendFailure(Component.literal("未找到该任务线索。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("任务线索：")
                .append(safeText(entry.display(), "text.seeking_immortals.unknown_quest")), false);
        QuestHookSoftService.mappedChainId(entry.id()).ifPresent(chain ->
                source.sendSuccess(() -> Component.literal("对应任务：").append(questDisplay(chain)), false));
        return 1;
    }

    private static int textQuestHookAccept(CommandSourceStack source, String id) throws CommandSyntaxException {
        return QuestHookSoftService.accept(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int textQuestSpawnNpc(CommandSourceStack source, String chainId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String npcId = TextQuestChainService.npcFor(chainId);
        String display = TextQuestNpcHookService.displayNameForNpc(npcId);
        if (!QuestService.spawnQuestNpc(player, display, npcId)) {
            source.sendFailure(Component.literal("任务角色生成失败。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已生成任务角色：")
                .append(safeText(display, "text.seeking_immortals.quest_guide"))
                .append("｜对应任务：").append(questDisplay(chainId)), true);
        return 1;
    }

    private static int textQuestInteract(CommandSourceStack source, String npc) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return TextQuestNpcHookService.chainForNpcId(player, npc)
                .map(chainId -> TextQuestNpcHookService.openDialogue(player, chainId, true) ? 1 : 0)
                .orElseGet(() -> {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.unknown",
                            npcDisplay(npc)), false);
                    return 0;
                });
    }

    private static int mainStoryList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.main_story.header",
                MainStorySoftService.chapterCount(), MainStorySoftService.completedCount(player)), false);
        for (ExtendedCatalogService.StoryChapter chapter : ExtendedCatalogService.builtin().chapters().values()) {
            source.sendSuccess(() -> Component.literal("章节：")
                    .append(safeText(chapter.display(), "text.seeking_immortals.unknown_chapter"))
                    .append(MainStorySoftService.isComplete(player, chapter.id()) ? "｜已完成" : "｜未完成"), false);
        }
        return 1;
    }

    private static int mainStoryComplete(CommandSourceStack source, String id) throws CommandSyntaxException {
        return MainStorySoftService.complete(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int mainStoryStart(CommandSourceStack source, String id) throws CommandSyntaxException {
        return MainStorySoftService.startChapter(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int questStart(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.start(source.getPlayerOrException());
        return 1;
    }

    private static int questCheck(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.check(source.getPlayerOrException());
        return 1;
    }

    private static int questChoose(CommandSourceStack source, String choice) throws CommandSyntaxException {
        return QuestService.chooseBranch(source.getPlayerOrException(), choice) ? 1 : 0;
    }

    private static int questReset(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.reset(source.getPlayerOrException());
        return 1;
    }

    private static int questAdvance(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.forceAdvance(source.getPlayerOrException());
        return 1;
    }

    private static int questSpawn(CommandSourceStack source, String npcName) throws CommandSyntaxException {
        return QuestService.spawnQuestNpc(source.getPlayerOrException(), npcName) ? 1 : 0;
    }

    private static int questPlaceSecretRoom(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.placeSecretRoomMarker(source.getPlayerOrException());
        return 1;
    }

    private static int questPlaceYuePortal(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.placeYuePortalMarker(source.getPlayerOrException());
        return 1;
    }

    private static int questGiveEvidence(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.giveEvidence(source.getPlayerOrException());
        return 1;
    }

    private static int questTriggerAttack(CommandSourceStack source) throws CommandSyntaxException {
        QuestService.triggerAttack(source.getPlayerOrException());
        return 1;
    }

    private static int artifactCatalog(CommandSourceStack source) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "法宝目录：%d 件法宝、%d 条炼器配方、%d 种飞行载具、%d 种符宝模板。",
                snapshot.artifacts().size(),
                snapshot.refinementRecipes().size(),
                snapshot.flightVehicles().size(),
                snapshot.talismanTreasureTemplates().size())), false);
        source.sendSuccess(() -> Component.literal("可查看首批法宝、法宝详情或炼器材料计划。"), false);
        return artifactPriority(source, "P0_launch");
    }

    private static int artifactPriority(CommandSourceStack source, String priorityTier) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        List<ArtifactDataService.ArtifactDefinition> artifacts = snapshot.priorityArtifacts(priorityTier);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "首批法宝：%d 件（目录共 %d 件）。",
                artifacts.size(),
                snapshot.priorityIds(priorityTier).size())), false);
        for (ArtifactDataService.ArtifactDefinition artifact : artifacts) {
            source.sendSuccess(() -> artifactLine(snapshot, artifact), false);
        }
        return artifacts.isEmpty() ? 0 : 1;
    }

    private static int artifactInfo(CommandSourceStack source, String artifactId) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        ArtifactDataService.ArtifactDefinition artifact = snapshot.findArtifact(artifactId).orElse(null);
        if (artifact == null) {
            source.sendFailure(Component.translatable("text.seeking_immortals.unknown_item"));
            return 0;
        }
        source.sendSuccess(() -> artifactLine(snapshot, artifact), false);
        if (!artifact.tags().isEmpty()) {
            source.sendSuccess(() -> Component.literal("标签：").append(ArtifactDisplayTexts.tagsJoined(artifact.tags())), false);
        }
        snapshot.findRecipeByArtifact(artifact.id())
                .ifPresent(recipe -> source.sendSuccess(() -> artifactRecipeLine(snapshot, recipe), false));
        return 1;
    }

    private static int artifactRecipe(CommandSourceStack source, String artifactOrRecipeId) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        var recipe = snapshot.findRecipeByArtifact(artifactOrRecipeId);
        if (recipe.isEmpty()) {
            recipe = snapshot.findRecipe(artifactOrRecipeId);
        }
        if (recipe.isEmpty()) {
            source.sendFailure(Component.literal("未找到对应的法宝或炼器配方。"));
            return 0;
        }
        ArtifactDataService.RefinementRecipe foundRecipe = recipe.get();
        source.sendSuccess(() -> artifactRecipeLine(snapshot, foundRecipe), false);
        return 1;
    }

    private static int artifactRefine(CommandSourceStack source, String recipeId) throws CommandSyntaxException {
        return ArtifactRefinementService.refine(source.getPlayerOrException(), recipeId) ? 1 : 0;
    }

    private static int artifactFiles(CommandSourceStack source) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "法宝数据文件：%d 个。", ArtifactDataService.sourceFiles().size())), false);
        if (source.hasPermission(2)) {
            snapshot.sourceFileEntryCounts().forEach((file, count) ->
                    source.sendSuccess(() -> Component.literal("内部数据文件：" + file + "，条目 " + count), false));
        }
        return 1;
    }

    private static Component artifactLine(ArtifactDataService.Snapshot snapshot,
            ArtifactDataService.ArtifactDefinition artifact) {
        return Component.literal("法宝：")
                .append(safeText(artifact.display(), "text.seeking_immortals.unknown_item"))
                .append("｜品阶：")
                .append(safeText(snapshot.tierDisplay(artifact.tier()), "text.seeking_immortals.unknown_item"))
                .append("｜境界：")
                .append(realmDisplay(artifact.realmMin()))
                .append("｜类型：")
                .append(ArtifactDisplayTexts.type(artifact.type()))
                .append("｜等级：")
                .append(Integer.toString(artifact.gameTier()));
    }

    private static Component artifactRecipeLine(ArtifactDataService.Snapshot snapshot,
            ArtifactDataService.RefinementRecipe recipe) {
        return Component.literal("炼器配方：")
                .append(safeText(recipe.display(), "text.seeking_immortals.unknown_item"))
                .append("｜产出：")
                .append(artifactDisplay(recipe.artifactId()))
                .append("｜品阶：")
                .append(safeText(snapshot.tierDisplay(recipe.tier()), "text.seeking_immortals.unknown_item"))
                .append("｜境界：")
                .append(realmDisplay(recipe.realmMin()))
                .append("｜工艺等级：")
                .append(Integer.toString(recipe.forgeGrade()))
                .append("｜成功率：")
                .append(successPercent(recipe.baseSuccessRate()));
    }

    private static String successPercent(double rate) {
        return String.format(Locale.ROOT, "%.0f%%", rate * 100.0D);
    }

    private static int marketOpen(CommandSourceStack source) throws CommandSyntaxException {
        return marketOpen(source, ShopService.MARKET_HERBAL_STALL);
    }

    private static int marketOpen(CommandSourceStack source, String shopId) throws CommandSyntaxException {
        String normalizedShopId = ShopService.canonicalMarketShopId(shopId);
        if (normalizedShopId.isBlank()) {
            source.sendFailure(Component.translatable("message.seeking_immortals.market.unknown_shop_generic"));
            return 0;
        }
        ShopService.openMarket(source.getPlayerOrException(), normalizedShopId);
        return 1;
    }

    private static int marketList(CommandSourceStack source) {
        return marketList(source, ShopService.MARKET_HERBAL_STALL);
    }

    private static int marketList(CommandSourceStack source, String shopId) {
        String normalizedShopId = ShopService.canonicalMarketShopId(shopId);
        if (normalizedShopId.isBlank()) {
            source.sendFailure(Component.translatable("message.seeking_immortals.market.unknown_shop_generic"));
            return 0;
        }
        ShopService.CostModifier modifier = source.getEntity() instanceof ServerPlayer player
                ? WorldpackGameplayService.marketCostModifier(player)
                : ShopService.CostModifier.NONE;
        source.sendSuccess(() -> Component.literal("坊市店铺：").append(shopDisplay(normalizedShopId)), false);
        for (ShopService.Entry entry : ShopService.entries(normalizedShopId)) {
            int cost = ShopService.adjustedCost(normalizedShopId, entry, modifier);
            source.sendSuccess(() -> Component.literal("- ")
                    .append(ShopService.itemName(entry))
                    .append(" x").append(Integer.toString(entry.count()))
                    .append("，消耗 ").append(Integer.toString(cost)).append(" x ")
                    .append(currencyDisplay(entry))
                    .append("，库存 ").append(stockText(entry)), false);
        }
        return 1;
    }

    private static int marketBuy(CommandSourceStack source, String shopId, String entry) throws CommandSyntaxException {
        String normalizedShopId = ShopService.canonicalMarketShopId(shopId);
        if (normalizedShopId.isBlank()) {
            source.sendFailure(Component.translatable("message.seeking_immortals.market.unknown_shop_generic"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ShopService.PurchaseResult result = ShopService.buyWithItemCurrency(player, normalizedShopId, entry,
                WorldpackGameplayService.marketCostModifier(player));
        switch (result.status()) {
            case SUCCESS -> {
                player.sendSystemMessage(Component.translatable(
                        "message.seeking_immortals.market.buy_success",
                        result.entry().count(),
                        ShopService.itemName(result.entry()),
                        result.paidCost(),
                        currencyDisplay(result.entry()),
                        stockText(result.remainingStock())));
                return 1;
            }
            case UNKNOWN_ENTRY -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.market.unknown_entry_generic"));
            case UNSUPPORTED_CURRENCY -> player.sendSystemMessage(Component.literal("该商品暂不支持当前货币。"));
            case BAD_ITEM -> player.sendSystemMessage(Component.translatable("text.seeking_immortals.unknown_item"));
            case BAD_CURRENCY_ITEM -> player.sendSystemMessage(Component.translatable("text.seeking_immortals.unknown_currency"));
            case NOT_ENOUGH_CURRENCY -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.not_enough_currency",
                    result.paidCost(),
                    result.entry() == null ? Component.translatable("text.seeking_immortals.unknown_currency") : currencyDisplay(result.entry())));
            case OUT_OF_STOCK -> player.sendSystemMessage(Component.literal("该商品已经售罄。"));
        }
        return 0;
    }

    private static int marketSpawnTrader(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MarketTraderEntity trader = ModEntities.MARKET_TRADER.get().create(player.serverLevel());
        if (trader == null) {
            source.sendFailure(Component.translatable("message.seeking_immortals.market.spawn_failed"));
            return 0;
        }
        trader.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        trader.setCustomName(Component.translatable("entity.seeking_immortals.market_trader"));
        trader.setCustomNameVisible(true);
        trader.setPersistenceRequired();
        player.serverLevel().addFreshEntity(trader);
        source.sendSuccess(() -> Component.translatable("message.seeking_immortals.market.spawned"), true);
        return 1;
    }

    private static int marketSpawnBanker(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SpiritStoneBankerEntity banker = ModEntities.SPIRIT_STONE_BANKER.get().create(player.serverLevel());
        if (banker == null) {
            source.sendFailure(Component.translatable("message.seeking_immortals.exchange.spawn_failed"));
            return 0;
        }
        banker.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        banker.setCustomName(Component.translatable("entity.seeking_immortals.spirit_stone_banker"));
        banker.setCustomNameVisible(true);
        banker.setPersistenceRequired();
        player.serverLevel().addFreshEntity(banker);
        source.sendSuccess(() -> Component.translatable("message.seeking_immortals.exchange.spawned"), true);
        return 1;
    }

    private static int worldpackOpen(CommandSourceStack source) throws CommandSyntaxException {
        WorldpackGameplayService.openScreen(source.getPlayerOrException());
        return 1;
    }

    private static int worldpackTravel(CommandSourceStack source, String regionId) throws CommandSyntaxException {
        return WorldpackGameplayService.travel(source.getPlayerOrException(), regionId) ? 1 : 0;
    }

    private static int worldpackEnter(CommandSourceStack source, String realmId) throws CommandSyntaxException {
        return WorldpackGameplayService.enterSecretRealm(source.getPlayerOrException(), realmId) ? 1 : 0;
    }

    private static int worldpackReturn(CommandSourceStack source) throws CommandSyntaxException {
        return WorldpackGameplayService.returnFromSecretRealm(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int worldpackSetAnchor(CommandSourceStack source, String anchorId) throws CommandSyntaxException {
        return WorldpackGameplayService.setAnchor(source.getPlayerOrException(), anchorId) ? 1 : 0;
    }

    private static int worldpackRegions(CommandSourceStack source) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.worldpack.regions.header", snapshot.regions().size()), false);
        for (WorldpackDataService.RegionCard region : snapshot.regions()) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(safeText(region.displayZh(), "text.seeking_immortals.unknown_region"))
                    .append("｜最低境界 ").append(realmDisplay(region.minRealm()))
                    .append("｜灵气倍率 ")
                    .append(String.format(Locale.ROOT, "%.2f", region.auraMultiplier())), false);
        }
        return 1;
    }

    private static int worldpackRealms(CommandSourceStack source) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.worldpack.realms.header", snapshot.secretRealms().size()), false);
        for (WorldpackDataService.SecretRealm realm : snapshot.secretRealms()) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(safeText(realm.displayZh(), "text.seeking_immortals.unknown_secret_realm"))
                    .append("｜区域 ").append(regionDisplay(realm.regionId()))
                    .append("｜最低境界 ").append(realmDisplay(realm.minRealm()))
                    .append("｜凭证 ").append(itemDisplay(realm.ticketItem()))
                    .append("｜冷却 ").append(Integer.toString(Math.max(0, realm.cooldownTicks() / 20)))
                    .append(" 秒"), false);
        }
        return 1;
    }

    private static int worldpackEvents(CommandSourceStack source) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.worldpack.events.header", snapshot.dailyEvents().size()), false);
        for (WorldpackDataService.DailyEvent event : snapshot.dailyEvents()) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(safeText(event.displayZh(), "text.seeking_immortals.unknown_event"))
                    .append("｜区域 ").append(regionDisplay(event.regionId()))
                    .append("｜权重 ").append(Integer.toString(event.weight()))
                    .append("｜持续 ").append(Integer.toString(Math.max(0, event.durationTicks() / 20)))
                    .append(" 秒"), false);
        }
        return 1;
    }

    private static int regionDailyEventsStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.region.daily_events.status",
                Component.literal(RegionEventConfig.isDailyEventsEnabled() ? "启用" : "停用"),
                DailyEventScheduler.expandedEventCount(),
                DailyEventScheduler.hookCount()), false);
        return 1;
    }

    private static int regionDailyEventsToggle(CommandSourceStack source, boolean enabled) {
        RegionEventConfig.setDailyEventsEnabled(enabled);
        source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.region.daily_events.toggle",
                Component.literal(enabled ? "启用" : "停用")), true);
        return 1;
    }

    private static int regionDailyEventsRoll(CommandSourceStack source) {
        net.minecraft.server.MinecraftServer server = source.getServer();
        if (server == null) {
            source.sendFailure(Component.translatable("command.seeking_immortals.region.daily_events.need_server"));
            return 0;
        }
        DailyEventScheduler.rollAllRegions(server.overworld(), true);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.region.daily_events.rolled"), true);
        return 1;
    }

    private static int regionDailyEventsClaim(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var result = com.xunxian.seekingimmortals.worldpack.DailyEventRewardService.claimActive(player);
        com.xunxian.seekingimmortals.worldpack.DailyEventRewardService.sendResult(player, result);
        return result == com.xunxian.seekingimmortals.worldpack.DailyEventRewardService.ClaimResult.CLAIMED ? 1 : 0;
    }

    private static int regionHere(CommandSourceStack source) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        String regionId = RegionRegistry.resolveAndSync(player);
        var definition = RegionRegistry.find(regionId);
        source.sendSuccess(() -> Component.literal("当前区域：")
                .append(regionDisplay(regionId))
                .append("｜灵气倍率 ")
                .append(definition.map(region -> String.format(Locale.ROOT, "%.2f", region.auraMultiplier())).orElse("1.00"))
                .append("｜维度 ")
                .append(definition.map(region -> dimensionDisplay(region.dimensionId()))
                        .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_dimension"))), false);
        return 1;
    }

    private static int regionList(CommandSourceStack source) {
        var snapshot = RegionRegistry.builtin();
        source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.region.list.header",
                snapshot.size(),
                snapshot.cardCount()), false);
        for (var region : snapshot.cards()) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(regionDisplay(region.id()))
                    .append("｜灵气 ")
                    .append(String.format(Locale.ROOT, "%.2f", region.auraMultiplier()))
                    .append("｜维度 ").append(dimensionDisplay(region.dimensionId()))
                    .append("｜世界包 ").append(region.hasWorldpack() ? "有" : "无"), false);
        }
        return 1;
    }

    private static int regionItems(CommandSourceStack source, String regionId) {
        var items = RegionItemsService.itemsForRegion(regionId);
        source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.region.items.header", regionDisplay(regionId), items.size()), false);
        int shown = 0;
        for (var item : items) {
            if (shown++ >= 20) {
                break;
            }
            source.sendSuccess(() -> Component.literal("- ")
                    .append(safeText(item.display(), "text.seeking_immortals.unknown_item"))
                    .append("｜").append(categoryDisplay(item.category()))
                    .append("｜").append(rarityDisplay(item.rarity())), false);
        }
        return 1;
    }

    private static int regionRoutes(CommandSourceStack source, String from, String to) {
        var routes = TravelRouteGraph.builtin().routesBetween(from, to);
        boolean connected = TravelRouteGraph.builtin().isConnected(from, to);
        source.sendSuccess(() -> Component.literal("路线 ").append(regionDisplay(from)).append(" → ")
                .append(regionDisplay(to)).append("：直接路线 ").append(Integer.toString(routes.size()))
                .append("，连通 ").append(connected ? "是" : "否"), false);
        for (var route : routes) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(regionDisplay(route.from())).append(" → ").append(regionDisplay(route.to()))
                    .append("｜耗时 ").append(Integer.toString(route.minDays())).append("-")
                    .append(Integer.toString(route.maxDays())).append(" 天｜费用 ")
                    .append(Integer.toString(route.feeLowStone())), false);
        }
        return 1;
    }

    private static int catalogSummary(CommandSourceStack source) {
        TextMaterialCatalogService.Snapshot snapshot = TextMaterialCatalogService.builtin();
        ExtendedCatalogService.Snapshot extended = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.summary",
                snapshot.secretRealmFlavors().size(),
                snapshot.manuals().size(),
                snapshot.methods().size(),
                snapshot.flightBindings().size()), false);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.summary_extended",
                extended.questChains().size(),
                extended.sects().size(),
                extended.priceBands().size(),
                extended.consumables().size(),
                extended.chapters().size(),
                extended.dailyEvents().size()), false);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.summary_indexes",
                extended.alchemyRecipes().size(),
                extended.spatialNodes().size(),
                extended.materials().size(),
                extended.pills().size(),
                extended.artifacts().size(),
                extended.totalIndexedEntries()), false);
        TextMaterialManifestService.Snapshot manifest = TextMaterialManifestService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.summary_manifest",
                manifest.catalogFiles(),
                manifest.techniqueFiles(),
                manifest.totalFiles(),
                manifest.totalEntries()), false);
        return 1;
    }

    private static int catalogManifest(CommandSourceStack source) {
        TextMaterialManifestService.Snapshot manifest = TextMaterialManifestService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.manifest.header",
                manifest.totalFiles(), manifest.totalEntries()), false);
        int shown = 0;
        for (TextMaterialManifestService.FileEntry entry : manifest.files().values()) {
            MutableComponent line = Component.literal("目录文件 ").append(Integer.toString(shown + 1))
                    .append("｜条目 ").append(Integer.toString(entry.entries()));
            if (source.hasPermission(2)) {
                line.append("｜").append(adminId(entry.id()));
            }
            source.sendSuccess(() -> line, false);
            if (++shown >= 30) {
                int remaining = manifest.totalFiles() - shown;
                source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.truncated", remaining), false);
                break;
            }
        }
        return 1;
    }

    private static int loreHub(CommandSourceStack source) {
        com.xunxian.seekingimmortals.lore.LoreCompendiumService.HubSummary hub =
                com.xunxian.seekingimmortals.lore.LoreCompendiumService.hub();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.lore.hub",
                hub.glossary(), hub.bestiaryTotal(), hub.chronicleTotal(), hub.timelinePhases(),
                hub.loreCatalogTotal()), false);
        for (String line : hub.lines()) {
            String copy = line;
            source.sendSuccess(() -> safeText(copy, "text.seeking_immortals.unknown_requirement"), false);
        }
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            for (String line : com.xunxian.seekingimmortals.lore.LoreCompendiumService.playerProgressLines(player)) {
                String copy = line;
                source.sendSuccess(() -> safeText(copy, "text.seeking_immortals.unknown_requirement"), false);
            }
        }
        return 1;
    }

    private static int loreOpen(CommandSourceStack source, String screen) {
        if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.seeking_immortals.lore.players_only"));
            return 0;
        }
        com.xunxian.seekingimmortals.lore.LoreSyncService.syncAndOpen(player, screen);
        String screenName = switch (screen == null ? "" : screen.trim().toLowerCase(Locale.ROOT)) {
            case "bestiary" -> "妖兽图鉴";
            case "chronicle" -> "编年见闻";
            default -> "百科总览";
        };
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.lore.opened", screenName), false);
        return 1;
    }

    private static int loreGlossary(CommandSourceStack source, String query) {
        if (query == null || query.isBlank()) {
            source.sendSuccess(() -> Component.translatable("command.seeking_immortals.lore.glossary.header",
                    com.xunxian.seekingimmortals.lore.NameAliasGlossaryService.size()), false);
            for (String line : com.xunxian.seekingimmortals.lore.NameAliasGlossaryService.sampleLines(12)) {
                String copy = line;
                source.sendSuccess(() -> safeText(copy, "text.seeking_immortals.unknown_requirement"), false);
            }
            return 1;
        }
        return com.xunxian.seekingimmortals.lore.NameAliasGlossaryService.find(query)
                .map(entry -> {
                    String primary = com.xunxian.seekingimmortals.lore.NameAliasGlossaryService
                            .playerDisplayName(entry);
                    List<String> visibleAliases = com.xunxian.seekingimmortals.lore.NameAliasGlossaryService
                            .playerDisplayAliases(entry);
                    String aliases = visibleAliases.isEmpty() ? "无" : String.join("、", visibleAliases);
                    source.sendSuccess(() -> Component.literal("术语：").append(safeText(primary,
                                    "text.seeking_immortals.unknown_requirement"))
                            .append("｜别名：").append(safeText(aliases, "text.seeking_immortals.unknown_requirement")), false);
                    return 1;
                })
                .orElseGet(() -> {
                    source.sendFailure(Component.literal("未找到该术语。"));
                    return 0;
                });
    }

    private static int loreNumeric(CommandSourceStack source) {
        if (!com.xunxian.seekingimmortals.lore.NumericOverviewService.present()) {
            source.sendFailure(Component.translatable("command.seeking_immortals.lore.numeric.missing"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.lore.numeric.header"), false);
        for (String line : com.xunxian.seekingimmortals.lore.NumericOverviewService.sampleLines(20)) {
            String copy = line;
            source.sendSuccess(() -> safeText(copy, "text.seeking_immortals.unknown_requirement"), false);
        }
        return 1;
    }

    private static int loreVisual(CommandSourceStack source) {
        com.xunxian.seekingimmortals.lore.VisualStyleService.Snapshot snap =
                com.xunxian.seekingimmortals.lore.VisualStyleService.builtin();
        if (!snap.present()) {
            source.sendFailure(Component.translatable("command.seeking_immortals.lore.visual.missing"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.lore.visual.header",
                Component.literal("视觉规范")), false);
        for (String line : snap.paletteLines()) {
            String copy = line;
            source.sendSuccess(() -> safeText(copy, "text.seeking_immortals.unknown_requirement"), false);
        }
        return 1;
    }

    private static int loreLangAudit(CommandSourceStack source) {
        // Lightweight shipped audit: report known M16 key presence via translation fallback pattern.
        String[] required = {
                "screen.seeking_immortals.bestiary.title",
                "screen.seeking_immortals.chronicle.title",
                "screen.seeking_immortals.compendium.title",
                "key.seeking_immortals.open_lore_compendium",
                "key.seeking_immortals.open_bestiary",
                "key.seeking_immortals.open_chronicle",
                "command.seeking_immortals.lore.hub"
        };
        int ok = 0;
        for (String key : required) {
            Component c = Component.translatable(key);
            boolean missing = c.getString().equals(key);
            if (!missing) {
                ok++;
            }
            String status = missing ? "缺失" : "正常";
            source.sendSuccess(() -> Component.literal(status), false);
        }
        int finalOk = ok;
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.lore.lang.summary",
                finalOk, required.length), false);
        return ok == required.length ? 1 : 0;
    }

    private static int lorePatchouliStatus(CommandSourceStack source) {
        boolean loaded = com.xunxian.seekingimmortals.compat.ModCompat.PATCHOULI_LOADED;
        source.sendSuccess(() -> Component.translatable(
                loaded ? "command.seeking_immortals.lore.patchouli.loaded"
                        : "command.seeking_immortals.lore.patchouli.absent"), false);
        return loaded ? 1 : 0;
    }

    private static int catalogLore(CommandSourceStack source) {
        LoreCatalogService.Snapshot lore = LoreCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.lore.summary",
                lore.npcArchetypes().size(),
                lore.dimensions().size(),
                lore.skillTrees().size(),
                lore.factionNodes().size(),
                lore.puppetDefinitions().size(),
                lore.totalEntries()), false);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.lore.summary_extra",
                lore.beasts().size(),
                lore.lootTables().size(),
                lore.constitutions().size(),
                lore.spiritRootGrades().size(),
                lore.ghostStages().size(),
                lore.ascensionStages().size()), false);
        return 1;
    }

    private static int catalogFactions(CommandSourceStack source) {
        FactionQuestCatalogService.Snapshot snapshot = FactionQuestCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.factions.summary",
                snapshot.chronicleEvents().size(),
                snapshot.factionConflicts().size(),
                snapshot.questHooks().size(),
                snapshot.merchantShops().size(),
                snapshot.tradeRoutes().size(),
                snapshot.totalEntries()), false);
        return 1;
    }

    private static int catalogAuctionList(CommandSourceStack source) {
        AuctionSoftService.Snapshot snapshot = AuctionSoftService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.auction.summary",
                snapshot.venueCount(), snapshot.lotCount(), snapshot.minIncrementPct()), false);
        for (AuctionSoftService.Venue venue : snapshot.venues()) {
            Component visible = Component.translatable("message.seeking_immortals.auction.venue",
                    AuctionSoftService.playerVenueDisplay(venue),
                    AuctionSoftService.playerRegionDisplay(venue.region()),
                    AuctionSoftService.playerFactionDisplay(venue.faction()));
            if (source.hasPermission(2)) {
                visible = Component.literal("内部标识「" + venue.id() + "」｜").append(visible);
            }
            Component line = visible;
            source.sendSuccess(() -> line, false);
        }
        int shown = 0;
        for (AuctionSoftService.Lot lot : snapshot.lots()) {
            Component visible = Component.translatable("message.seeking_immortals.auction.lot",
                    AuctionSoftService.playerLotDisplay(lot), lot.minEquiv(), lot.maxEquiv());
            if (source.hasPermission(2)) {
                visible = Component.literal("内部标识「" + lot.id() + "」｜").append(visible);
            }
            Component line = visible;
            source.sendSuccess(() -> line, false);
            if (++shown >= 10) break;
        }
        return 1;
    }

    private static int catalogAuctionOpen(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        // Wave490: open productized AuctionHall MenuType (authority path).
        com.xunxian.seekingimmortals.catalog.AuctionSoftService.openHall(player);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.auction.opened"), false);
        return 1;
    }

    private static int catalogAuctionPreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return AuctionSoftService.preview(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogAuctionInterest(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = AuctionInterestService.markInterest(player, id);
        if (ok) {
            source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.auction.interest_count",
                    AuctionInterestService.interestCount(player)), false);
        }
        return ok ? 1 : 0;
    }

    private static int catalogAuctionBid(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = AuctionSoftService.bid(player, id);
        if (ok) {
            source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.auction.bid_ok", id), false);
        }
        return ok ? 1 : 0;
    }

    private static int catalogAuctionSettle(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = AuctionSoftService.settle(player, id);
        if (ok) {
            source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.auction.settle_ok", id), false);
        }
        return ok ? 1 : 0;
    }

    private static int catalogDimensions(CommandSourceStack source) {
        var snap = com.xunxian.seekingimmortals.worldpack.DimensionRegistryService.snapshot();
        source.sendSuccess(() -> Component.literal("维度目录：共 ").append(Integer.toString(snap.size()))
                .append(" 个，可进入 ").append(Integer.toString(snap.playable().size()))
                .append(" 个，待实现 ").append(Integer.toString(snap.deferredIds().size()))
                .append(" 个"), false);
        int shown = 0;
        for (var def : snap.playable()) {
            MutableComponent line = Component.literal("- ").append(dimensionDisplay(def.id()))
                    .append("｜最低境界 ").append(realmDisplay(def.minRealm()))
                    .append("｜状态 ").append(dimensionClassLabel(def.dimensionClass()));
            if (source.hasPermission(2)) {
                line.append("｜").append(adminId(def.id()));
            }
            source.sendSuccess(() -> line, false);
            if (++shown >= 16) break;
        }
        // M-A: report the three non-playable classes separately; a template or a logical
        // cluster id is honest architecture, not pending work.
        if (source.hasPermission(2)) {
            for (var dimensionClass : java.util.List.of(
                    com.xunxian.seekingimmortals.worldpack.DimensionRegistryService.DimensionClass.PREVIEW_LOCKED,
                    com.xunxian.seekingimmortals.worldpack.DimensionRegistryService.DimensionClass.ABSTRACT_TEMPLATE,
                    com.xunxian.seekingimmortals.worldpack.DimensionRegistryService.DimensionClass.LOGICAL_CLUSTER)) {
                for (var def : snap.inClass(dimensionClass)) {
                    MutableComponent line = Component.empty()
                            .append(dimensionClassLabel(dimensionClass))
                            .append("：").append(adminId(def.id()));
                    source.sendSuccess(() -> line, false);
                }
            }
        }
        return 1;
    }

    /** M-A: the four honest dimension states, replacing the old deferred/enterable guess. */
    private static Component dimensionClassLabel(
            com.xunxian.seekingimmortals.worldpack.DimensionRegistryService.DimensionClass dimensionClass) {
        if (dimensionClass == null) {
            return Component.translatable("text.seeking_immortals.dimension_class.preview_locked");
        }
        return switch (dimensionClass) {
            case PLAYABLE -> Component.translatable("text.seeking_immortals.dimension_class.playable");
            case PREVIEW_LOCKED -> Component.translatable("text.seeking_immortals.dimension_class.preview_locked");
            case ABSTRACT_TEMPLATE -> Component.translatable("text.seeking_immortals.dimension_class.abstract_template");
            case LOGICAL_CLUSTER -> Component.translatable("text.seeking_immortals.dimension_class.logical_cluster");
        };
    }

    private static int catalogDimensionGet(CommandSourceStack source, String id) {
        var optional = com.xunxian.seekingimmortals.worldpack.DimensionRegistryService.find(id);
        if (optional.isEmpty()) {
            source.sendFailure(Component.translatable("text.seeking_immortals.unknown_dimension"));
            return 0;
        }
        var def = optional.get();
        MutableComponent line = Component.literal("维度：").append(dimensionDisplay(def.id()))
                .append("｜最低境界 ").append(realmDisplay(def.minRealm()))
                .append("｜境界上限 ").append(realmDisplay(def.realmCap()))
                .append("｜状态 ").append(dimensionClassLabel(def.dimensionClass()));
        if (PlayerDisplayText.isSafe(def.note()) && !def.note().isBlank()) {
            line.append("｜说明 ").append(def.note());
        }
        if (source.hasPermission(2)) {
            line.append("｜").append(adminId(def.id()));
        }
        source.sendSuccess(() -> line, false);
        return 1;
    }

    private static int catalogDimensionTravel(CommandSourceStack source, String route) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = com.xunxian.seekingimmortals.worldpack.DimensionTravelService.travelByRoute(player, route);
        source.sendSuccess(() -> Component.literal(ok ? "跨界传送已完成。" : "跨界传送未完成。"), false);
        return ok ? 1 : 0;
    }

    private static int catalogAscensionStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var missing = com.xunxian.seekingimmortals.worldpack.AscensionService.missingRequirements(player);
        boolean can = com.xunxian.seekingimmortals.worldpack.AscensionService.canAscend(player);
        boolean pending = com.xunxian.seekingimmortals.worldpack.AscensionService.hasPendingConfirmation(player);
        MutableComponent line = Component.literal("飞升资格：").append(can ? "满足" : "不足")
                .append("｜待确认：").append(pending ? "是" : "否")
                .append("｜当前阶段：").append(ascensionStageDisplay(
                        com.xunxian.seekingimmortals.worldpack.AscensionService.currentStage(player)))
                .append("｜缺少条件：");
        if (missing.isEmpty()) {
            line.append("无");
        } else {
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) line.append("、");
                line.append(ascensionRequirementDisplay(missing.get(i)));
            }
        }
        source.sendSuccess(() -> line, false);
        return 1;
    }

    private static int catalogAscensionAttempt(CommandSourceStack source, boolean confirm) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = confirm
                ? com.xunxian.seekingimmortals.worldpack.AscensionService.confirmLoadoutAndAscend(player)
                : com.xunxian.seekingimmortals.worldpack.AscensionService.attemptAscension(player, false);
        source.sendSuccess(() -> Component.literal(ok ? "飞升已执行。" : "飞升尚未完成。"), false);
        return ok ? 1 : 0;
    }

    private static int catalogAscensionCancel(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean ok = com.xunxian.seekingimmortals.worldpack.AscensionService.cancelPending(player);
        return ok ? 1 : 0;
    }

    /**
     * Admin diagnostic only (permission 2). Not a player reclaim path —
     * successful ascension clears the temporary teleport-failure snapshot.
     */
    private static int catalogAscensionRestore(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal("管理员回滚诊断：临时备份 "
                + (com.xunxian.seekingimmortals.worldpack.AscensionService.hasBackup(player) ? "存在" : "不存在")), false);
        boolean ok = com.xunxian.seekingimmortals.worldpack.AscensionService.restoreBackup(player);
        source.sendSuccess(() -> Component.literal(ok ? "管理员回滚已应用。" : "没有可回滚的备份。"), false);
        return ok ? 1 : 0;
    }

    private static int catalogSpatialList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.spatial.header",
                SpatialNodeCatalogService.builtin().size()), false);
        int shown = 0;
        for (SpatialNodeCatalogService.Node node : SpatialNodeCatalogService.builtin().nodes().values()) {
            MutableComponent line = Component.literal("- ")
                    .append(safeText(node.display(), "text.seeking_immortals.unknown_spatial_node"))
                    .append("｜类型 ").append(spatialTypeDisplay(node.type()))
                    .append("｜区域 ").append(regionDisplay(node.region()))
                    .append("｜费用 ").append(Integer.toString(node.costSpiritStone()));
            if (source.hasPermission(2)) {
                line.append("｜").append(adminId(node.id()));
            }
            source.sendSuccess(() -> line, false);
            if (++shown >= 20) break;
        }
        return 1;
    }

    private static int catalogSpatialPreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return SpatialNodeCatalogService.preview(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogSpatialTravel(CommandSourceStack source, String id) throws CommandSyntaxException {
        boolean ok = SpatialNodeCatalogService.travel(source.getPlayerOrException(), id);
        if (ok) {
            source.sendSuccess(() -> Component.literal("空间节点传送完成。"), false);
        }
        return ok ? 1 : 0;
    }

    private static int catalogReputationList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var snapshot = ReputationService.snapshot(player);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.header",
                snapshot.size()), false);
        if (snapshot.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.empty"), false);
            return 1;
        }
        for (var entry : snapshot.entrySet()) {
            source.sendSuccess(() -> Component.literal("声望·").append(factionDisplay(entry.getKey()))
                    .append("：").append(Integer.toString(entry.getValue())), false);
        }
        return 1;
    }

    private static int catalogReputationGet(CommandSourceStack source, String faction) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int value = ReputationService.get(player, faction);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.value",
                factionDisplay(faction), value), false);
        return 1;
    }

    private static int catalogReputationDiscount(CommandSourceStack source, String shopId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        double mult = ReputationService.shopDiscountMultiplier(player, shopId);
        String label = ReputationService.discountLabel(player, shopId);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.discount",
                shopDisplay(shopId), discountDisplay(label), mult), false);
        return 1;
    }

    private static int catalogReputationAdd(CommandSourceStack source, String faction, int delta) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int next = ReputationService.add(player, faction, delta);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.added",
                factionDisplay(faction), delta, next), true);
        return 1;
    }

    private static int catalogConflictsList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.conflicts.header",
                FactionConflictSoftService.count()), false);
        for (String line : FactionConflictSoftService.sample(20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogConflictsPreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return FactionConflictSoftService.preview(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogConflictsAccept(CommandSourceStack source, String id) throws CommandSyntaxException {
        return FactionConflictSoftService.accept(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogConflictsSide(CommandSourceStack source, String id, String side) throws CommandSyntaxException {
        return FactionConflictSoftService.chooseSide(source.getPlayerOrException(), id, side) ? 1 : 0;
    }

    private static int catalogBulkList(CommandSourceStack source) {
        BulkCatalogIndexService.Snapshot snapshot = BulkCatalogIndexService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.bulk.header",
                snapshot.fileCount(), snapshot.totalEntries()), false);
        int shown = 0;
        for (BulkCatalogIndexService.IndexFile file : snapshot.indexes().values()) {
            MutableComponent line = Component.literal("目录文件 ").append(Integer.toString(shown + 1))
                    .append("｜条目 ").append(Integer.toString(file.size()));
            if (source.hasPermission(2)) {
                line.append("｜").append(adminId(file.file()));
            }
            source.sendSuccess(() -> line, false);
            if (++shown >= 30) break;
        }
        return 1;
    }

    private static int catalogBulkShow(CommandSourceStack source, String name) {
        var optional = BulkCatalogIndexService.builtin().find(name);
        if (optional.isEmpty()) {
            source.sendFailure(Component.literal("未找到该目录。"));
            return 0;
        }
        BulkCatalogIndexService.IndexFile file = optional.get();
        MutableComponent header = Component.literal("目录条目：").append(Integer.toString(file.size()));
        if (source.hasPermission(2)) {
            header.append("｜").append(adminId(file.file()));
        }
        source.sendSuccess(() -> header, false);
        int shown = 0;
        for (BulkCatalogIndexService.Entry entry : file.entries().values()) {
            MutableComponent line = Component.literal("- ")
                    .append(safeText(entry.display(), "text.seeking_immortals.unknown_requirement"));
            if (source.hasPermission(2)) {
                line.append("｜").append(adminId(entry.id()));
            }
            source.sendSuccess(() -> line, false);
            if (++shown >= 20) {
                int remaining = file.size() - shown;
                source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.truncated", remaining), false);
                break;
            }
        }
        return 1;
    }

    private static int catalogRefineList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.refine.header",
                CraftWorldSoftService.refinementRecipeCount()), false);
        for (String line : CraftWorldSoftService.sample("refinement_recipes_index", 20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogRefinePreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return CraftWorldSoftService.preview(source.getPlayerOrException(), "refinement_recipes_index", id,
                "message.seeking_immortals.refine.unknown",
                "message.seeking_immortals.refine.preview",
                "message.seeking_immortals.refine.soft_only") ? 1 : 0;
    }

    private static int catalogRefineCraft(CommandSourceStack source, String id, int grade) throws CommandSyntaxException {
        return CraftWorldSoftService.craft(source.getPlayerOrException(), id, grade) ? 1 : 0;
    }

    private static int catalogFormationsList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.formations.header",
                CraftWorldSoftService.formationCount()), false);
        for (String line : CraftWorldSoftService.sample("formation_catalog_index", 20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }


    private static int stationInspect(CommandSourceStack source, String stationId) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        boolean ok = com.xunxian.seekingimmortals.structure.MultiblockOperationalService.inspect(
                player, stationId, player.blockPosition());
        return ok ? 1 : 0;
    }

    private static int stationForm(CommandSourceStack source, String stationId) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        boolean ok = com.xunxian.seekingimmortals.structure.MultiblockOperationalService.form(
                player, stationId, player.blockPosition());
        return ok ? 1 : 0;
    }

    private static int stationRepair(CommandSourceStack source, String stationId) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        boolean ok = com.xunxian.seekingimmortals.structure.MultiblockOperationalService.repair(
                player, stationId, player.blockPosition());
        return ok ? 1 : 0;
    }

    private static int stationDismantle(CommandSourceStack source, String stationId) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        boolean ok = com.xunxian.seekingimmortals.structure.MultiblockOperationalService.dismantle(
                player, stationId, player.blockPosition());
        return ok ? 1 : 0;
    }

    private static int stationOverhaul(CommandSourceStack source, String stationId) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();
        boolean ok = com.xunxian.seekingimmortals.structure.MultiblockOperationalService.overhaul(
                player, stationId, player.blockPosition());
        return ok ? 1 : 0;
    }


    private static int catalogFormationsPreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return CraftWorldSoftService.preview(source.getPlayerOrException(), "formation_catalog_index", id,
                "message.seeking_immortals.formation_catalog.unknown",
                "message.seeking_immortals.formation_catalog.preview",
                "message.seeking_immortals.formation_catalog.soft_only") ? 1 : 0;
    }

    private static int catalogFormationsDeploy(CommandSourceStack source, String id) throws CommandSyntaxException {
        return CraftWorldSoftService.deploy(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogTalismanList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.talisman.header",
                CraftWorldSoftService.talismanRecipeCount()), false);
        for (String line : CraftWorldSoftService.sample("talisman_recipes_index", 20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogTalismanCraft(CommandSourceStack source, String id) throws CommandSyntaxException {
        return CraftWorldSoftService.craftTalisman(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogPuppetList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.puppet.header",
                CraftWorldSoftService.puppetRecipeCount()), false);
        for (String line : CraftWorldSoftService.sample("puppet_craft_recipes_index", 20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogPuppetCraft(CommandSourceStack source, String id) throws CommandSyntaxException {
        return CraftWorldSoftService.craftPuppet(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogPuppetRepair(CommandSourceStack source) throws CommandSyntaxException {
        int repaired = SummonHonestMvpService.repairOwnedPuppets(source.getPlayerOrException());
        return repaired > 0 ? 1 : 0;
    }

    private static int catalogChronicleList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.chronicle.header",
                ChronicleTradeSoftService.chronicleCount()), false);
        for (String line : ChronicleTradeSoftService.sampleChronicle(20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogChroniclePreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return ChronicleTradeSoftService.previewChronicle(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogChronicleDiscover(CommandSourceStack source, String id) throws CommandSyntaxException {
        return ChronicleTradeSoftService.discoverChronicle(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogTradeList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.trade.header",
                ChronicleTradeSoftService.tradeRouteCount()), false);
        for (String line : ChronicleTradeSoftService.sampleTradeRoutes(20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogTradePreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return ChronicleTradeSoftService.previewTradeRoute(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogTradeEmbark(CommandSourceStack source, String id) throws CommandSyntaxException {
        return ChronicleTradeSoftService.embark(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogSummon(CommandSourceStack source, String id) throws CommandSyntaxException {
        return SummonHonestMvpService.summonProxy(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogSummonList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int count = SummonHonestMvpService.countOwnedServitors(player);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.summon.header", count), false);
        SummonHonestMvpService.listOwnedServitors(player).forEach(servitor ->
                source.sendSuccess(() -> Component.literal("侍灵：")
                        .append(summonArchetypeDisplay(servitor.getArchetype()))
                        .append("｜姿态：").append(summonStanceDisplay(servitor.getStance())), false));
        return 1;
    }

    private static int catalogSummonStance(CommandSourceStack source, String mode) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String token = mode == null ? "" : mode.trim().toLowerCase();
        com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Stance stance = switch (token) {
            case "guard", "defend" -> com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Stance.GUARD;
            case "aggressive", "attack", "aggro" -> com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Stance.AGGRESSIVE;
            case "stay", "hold" -> com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Stance.STAY;
            default -> com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Stance.FOLLOW;
        };
        int count = SummonHonestMvpService.setStanceAll(player, stance);
        return count > 0 ? 1 : 0;
    }

    private static int catalogSummonDismiss(CommandSourceStack source) throws CommandSyntaxException {
        int count = SummonHonestMvpService.dismissOwned(source.getPlayerOrException());
        return count > 0 ? 1 : 0;
    }

    private static int beastList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var lines = com.xunxian.seekingimmortals.cultivation.BeastContractService.snapshotLines(player);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.beast.header", lines.size()), false);
        lines.forEach((id, info) -> source.sendSuccess(() -> Component.literal("灵兽：")
                .append(beastDisplay(id)).append("｜").append(safeText(info.replace("affinity=", "亲和=")
                        .replace(",level=", "，等级=").replace(",experience=", "，经验=")
                        .replace(",evolution=", "，进化="), "text.seeking_immortals.unknown_beast")), false));
        return 1;
    }

    private static int beastContract(CommandSourceStack source, String id) throws CommandSyntaxException {
        return com.xunxian.seekingimmortals.cultivation.BeastContractService.contract(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int beastFeed(CommandSourceStack source, String id) throws CommandSyntaxException {
        return com.xunxian.seekingimmortals.cultivation.BeastContractService.feed(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int beastSummon(CommandSourceStack source, String id) throws CommandSyntaxException {
        return com.xunxian.seekingimmortals.cultivation.BeastContractService.summon(source.getPlayerOrException(), id) ? 1 : 0;
    }


    private static int bossSpawn(CommandSourceStack source, String id) throws CommandSyntaxException {
        return com.xunxian.seekingimmortals.worldpack.BossEncounterService.spawnIfNeeded(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int phaseStatus(CommandSourceStack source) throws CommandSyntaxException {
        com.xunxian.seekingimmortals.phase.SoftPhaseShellService.status(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("阶段进度已读取。"), false);
        return 1;
    }

    private static int phaseMark(CommandSourceStack source, String id) throws CommandSyntaxException {
        return com.xunxian.seekingimmortals.phase.SoftPhaseShellService.mark(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int phaseEnter(CommandSourceStack source, String id) throws CommandSyntaxException {
        return SoftPhaseShellService.enter(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int missionGenerate(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String sect = CultivationHelper.get(player).map(c -> c.getSevenMysteriesQuest().getSectId()).orElse("qinglan");
        var mission = com.xunxian.seekingimmortals.sect.SectMissionGenerator.generate(sect);
        player.getPersistentData().putString("seeking_immortals_active_mission", mission.id());
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.mission.generated",
                mission.id(), mission.type(), mission.target(), mission.count(), mission.rewardContribution()), false);
        return 1;
    }

    private static int warStatus(CommandSourceStack source) {
        String status = com.xunxian.seekingimmortals.sect.SectWarService.status(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.war.status",
                safeText(status, "text.seeking_immortals.unknown_event")), false);
        return 1;
    }

    private static int warStart(CommandSourceStack source, String a, String b, String c, int minutes) {
        return com.xunxian.seekingimmortals.sect.SectWarService.start(source.getServer(), a, b, c, minutes) ? 1 : 0;
    }

    private static int warStop(CommandSourceStack source) {
        return com.xunxian.seekingimmortals.sect.SectWarService.stop(source.getServer()) ? 1 : 0;
    }

    private static int catalogHas(CommandSourceStack source, String id) {
        TextMaterialManifestService.Snapshot manifest = TextMaterialManifestService.builtin();
        boolean present = manifest.contains(id);
        source.sendSuccess(() -> Component.literal(present ? "目录已收录该内容。" : "目录未收录该内容。"), false);
        return present ? 1 : 0;
    }

    private static int catalogStudyManual(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return ManualCatalogService.study(player, id) ? 1 : 0;
    }

    private static int catalogBoardFlight(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return FlightVehicleService.board(player, id) ? 1 : 0;
    }

    private static int catalogMethods(CommandSourceStack source) {
        TextMaterialCatalogService.Snapshot snapshot = TextMaterialCatalogService.builtin();
        int total = snapshot.methods().size();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.header", total), false);
        int shown = 0;
        for (TextMaterialCatalogService.MethodEntry method : snapshot.methods().values()) {
            MutableComponent line = Component.literal("- ")
                    .append(safeText(method.display(), "text.seeking_immortals.unknown_technique"))
                    .append("｜最低境界 ").append(realmDisplay(method.realmMin()));
            if (PlayerDisplayText.isSafe(method.school()) && !method.school().isBlank()) {
                line.append("｜门类 ").append(method.school());
            }
            if (source.hasPermission(2)) {
                line.append("｜").append(adminId(method.id()));
            }
            source.sendSuccess(() -> line, false);
            if (++shown >= 20) {
                int remaining = total - shown;
                source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.truncated", remaining), false);
                break;
            }
        }
        return 1;
    }

    private static int catalogLearnMethod(CommandSourceStack source, String id) throws CommandSyntaxException {
        return ManualCatalogService.learnMethod(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogStudiedMethods(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int count = ManualCatalogService.learnedMethodCount(player);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.studied_header", count), false);
        var tag = player.getPersistentData().getCompound(ManualCatalogService.LEARNED_METHODS_TAG);
        int shown = 0;
        for (String key : tag.getAllKeys()) {
            if (!tag.getBoolean(key)) {
                continue;
            }
            Component line = Component.translatable("text.seeking_immortals.unknown_technique");
            var optional = TextMaterialCatalogService.builtin().findMethod(key);
            if (optional.isPresent()) {
                line = safeText(optional.get().display(), "text.seeking_immortals.unknown_technique");
            }
            Component finalLine = line;
            source.sendSuccess(() -> finalLine, false);
            if (++shown >= 30) {
                break;
            }
        }
        return 1;
    }

    private static int catalogRealms(CommandSourceStack source) {
        TextMaterialCatalogService.Snapshot snapshot = TextMaterialCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.realms.header",
                snapshot.secretRealmFlavors().size()), false);
        for (TextMaterialCatalogService.SecretRealmFlavor flavor : snapshot.secretRealmFlavors().values()) {
            MutableComponent line = Component.literal("秘境风貌：")
                    .append(safeText(flavor.environment(), "text.seeking_immortals.unknown_secret_realm"));
            if (PlayerDisplayText.isSafe(flavor.openCondition()) && !flavor.openCondition().isBlank()) {
                line.append("｜开启条件 ").append(flavor.openCondition());
            }
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int catalogQuests(CommandSourceStack source) {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.quests.header",
                snapshot.questChains().size()), false);
        int shown = 0;
        for (ExtendedCatalogService.QuestChain chain : snapshot.questChains().values()) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(questDisplay(chain.id()))
                    .append("｜步骤 ").append(Integer.toString(chain.stepCount()))
                    .append("｜区域 ").append(regionDisplay(chain.region())), false);
            if (++shown >= 20) {
                int remaining = snapshot.questChains().size() - shown;
                source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.truncated", remaining), false);
                break;
            }
        }
        return 1;
    }

    private static int catalogSects(CommandSourceStack source) {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.sects.header",
                snapshot.sects().size()), false);
        for (ExtendedCatalogService.SectEntry sect : snapshot.sects().values()) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(factionDisplay(sect.id()))
                    .append("｜区域 ").append(regionDisplay(sect.region()))
                    .append("｜立场 ").append(alignmentDisplay(sect.alignment())), false);
        }
        return 1;
    }

    private static int catalogBands(CommandSourceStack source) {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.bands.header",
                snapshot.priceBands().size()), false);
        for (ExtendedCatalogService.PriceBand band : snapshot.priceBands().values()) {
            source.sendSuccess(() -> Component.literal("价格范围：")
                    .append(Integer.toString(band.min())).append("-").append(Integer.toString(band.max()))
                    .append("，建议 ").append(Integer.toString(band.suggested())), false);
        }
        return 1;
    }

    private static int catalogChapters(CommandSourceStack source) {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.chapters.header",
                snapshot.chapters().size()), false);
        for (ExtendedCatalogService.StoryChapter chapter : snapshot.chapters().values()) {
            source.sendSuccess(() -> Component.literal("- ")
                    .append(safeText(chapter.display(), "text.seeking_immortals.unknown_chapter"))
                    .append("｜区域 ").append(regionDisplay(chapter.region())), false);
        }
        return 1;
    }

    private static int sectStatus(CommandSourceStack source) throws CommandSyntaxException {
        SectContributionService.showStatus(source.getPlayerOrException());
        return 1;
    }

    private static int sectOpen(CommandSourceStack source) throws CommandSyntaxException {
        SectContributionService.openScreen(source.getPlayerOrException());
        return 1;
    }

    private static int sectJoin(CommandSourceStack source) throws CommandSyntaxException {
        return SectContributionService.join(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int sectCandidates(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            var progress = cultivation.getSevenMysteriesQuest();
            List<SectDefinitionService.SectDefinition> candidates = SectDefinitionService.candidates(progress);
            if (!SectDefinitionService.entryGateOpen(progress)) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.candidates_locked"));
                return;
            }
            player.sendSystemMessage(Component.translatable("command.seeking_immortals.sect.candidates.header", candidates.size()));
            for (SectDefinitionService.SectDefinition definition : candidates) {
                player.sendSystemMessage(Component.literal("- ")
                        .append(safeText(definition.displayZh(), "text.seeking_immortals.unknown_faction"))
                        .append("：")
                        .append(safeText(definition.focusZh(), "text.seeking_immortals.unknown_requirement")));
            }
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.sect.no_data")));
        return 1;
    }

    private static int sectApply(CommandSourceStack source, String sectId) throws CommandSyntaxException {
        return SectContributionService.applySect(source.getPlayerOrException(), sectId, false) ? 1 : 0;
    }

    private static int sectAdvance(CommandSourceStack source) throws CommandSyntaxException {
        return SectContributionService.advanceSectQuest(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int sectShop(CommandSourceStack source) throws CommandSyntaxException {
        SectContributionService.showShop(source.getPlayerOrException());
        return 1;
    }

    private static int sectBuy(CommandSourceStack source, String entry) throws CommandSyntaxException {
        return SectContributionService.buy(source.getPlayerOrException(), entry) ? 1 : 0;
    }

    private static int sectDonateSpiritGrass(CommandSourceStack source) throws CommandSyntaxException {
        return SectContributionService.donateSpiritGrass(source.getPlayerOrException()) ? 1 : 0;
    }

    private static int sectSpawnSteward(CommandSourceStack source, String sectId) throws CommandSyntaxException {
        SectContributionService.spawnSteward(source.getPlayerOrException(), sectId);
        return 1;
    }

    private static int sectPlaceOutpost(CommandSourceStack source, String sectId) throws CommandSyntaxException {
        SectContributionService.placeSectOutpost(source.getPlayerOrException(), sectId);
        return 1;
    }

    private static int showRealm(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            double chance = BreakthroughService.preview(player, cultivation).chance();
            source.sendSuccess(() -> Component.translatable(
                    "command.seeking_immortals.realm",
                    cultivation.getRealm().getDisplayName(),
                    cultivation.getStage().getDisplayName(),
                    cultivation.getAgeYears(),
                    cultivation.getLifespanYears(),
                    cultivation.getRemainingLifespanYears(),
                    Math.round(chance * 10000.0D) / 100.0D), false);

            // Phase 1: 显示衍生属性
            source.sendSuccess(() -> Component.literal(String.format(
                    "衍生属性 | 最大HP: %d | 灵力回速: %.2f/s | 修为回速: %.2f/s | 飞行速度: %.1f格/s",
                    cultivation.getMaxHealthPoints(),
                    cultivation.getManaRecoveryPerSecond(),
                    cultivation.getCultivationGainPerSecond(),
                    cultivation.getFlyingSpeed()
            )), false);
        });
        return 1;
    }

    private static int showRoot(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.root",
                cultivation.getSpiritualRoot().getDisplayName(),
                cultivation.getSpiritualRoot().getCategoryName(),
                cultivation.getSpiritualRootAttributeNames(),
                String.format(java.util.Locale.ROOT, "%.2f", cultivation.getCultivationSpeedMultiplier()),
                String.format(java.util.Locale.ROOT, "%.2f", cultivation.getSpiritualRoot().getQiRecoveryMultiplier()),
                String.format(java.util.Locale.ROOT, "%.0f", cultivation.getSpiritualRoot().getBreakthroughBonus() * 100),
                String.format(java.util.Locale.ROOT, "%.2f", cultivation.getPillAbsorptionMultiplier()),
                cultivation.getSpecialPhysique().getDisplayName()), false));
        return 1;
    }

    private static int applySevereInjury(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.applySevereInjury();
            source.sendSuccess(() -> Component.literal("已施加重伤：最大生命上限降低80%，灵力恢复降低40%。"), false);
        });
        return 1;
    }

    private static int applyHeartDemon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.applyHeartDemon(player.getRandom());
            source.sendSuccess(() -> Component.literal("已施加心魔：基础层数/随机触发计时已保存。"), false);
        });
        return 1;
    }

    private static int applyRealmFall(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.applyRealmFall(player.getRandom());
            source.sendSuccess(() -> Component.literal("已触发跌境：境界进度已回退。"), false);
        });
        return 1;
    }

    private static int applyShatteredCore(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.applyShatteredCore();
            source.sendSuccess(() -> Component.literal("已施加碎丹：造成伤害降低30%。"), false);
        });
        return 1;
    }

    private static int debugSetCultivation(CommandSourceStack source, int amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.setAbsoluteCultivationForDebug(amount);
            SyncCultivationDataPacket.send(player, cultivation);
            SyncLearnedTechniquesPacket.send(player, cultivation);
            source.sendSuccess(() -> Component.literal(String.format(
                    "修为已设为%d → %s %s，阶段进度%d/%d，灵力上限%d。",
                    cultivation.getCultivation(),
                    cultivation.getRealm().getDisplayName(),
                    cultivation.getStage().getDisplayName(),
                    cultivation.getCurrentStageProgressExp(),
                    cultivation.getCurrentStageExpSpan(),
                    cultivation.getManaMax())), true);
        });
        return 1;
    }

    private static int debugFillMana(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.setMana(cultivation.getManaMax());
            SyncCultivationDataPacket.send(player, cultivation);
            source.sendSuccess(() -> Component.literal(String.format(
                    "灵力已充满：%d/%d。",
                    cultivation.getMana(),
                    cultivation.getManaMax())), true);
        });
        return 1;
    }

    private static int debugSetCoreAttrs(CommandSourceStack source, int divSense, int bodyRef, int qiDevRisk, int tribRes) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.setCoreAttributesForDebug(divSense, bodyRef, qiDevRisk, tribRes);
            SyncCultivationDataPacket.send(player, cultivation);
            source.sendSuccess(() -> Component.translatable("command.seeking_immortals.debug.core_attrs",
                    cultivation.getDivSense(),
                    cultivation.getBodyRef(),
                    cultivation.getQiDevRisk(),
                    cultivation.getTribRes()), true);
        });
        return 1;
    }

    private static int debugStartTribulation(CommandSourceStack source, String targetRealmName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Realm targetRealm = parseRealm(targetRealmName);
        if (targetRealm == null) {
            source.sendFailure(Component.translatable("command.seeking_immortals.debug.tribulation.bad_realm", targetRealmName));
            return 0;
        }
        CultivationHelper.get(player).ifPresent(cultivation -> TribulationService.debugStart(player, cultivation, targetRealm));
        return 1;
    }

    private static int debugAddContribution(CommandSourceStack source, int amount) throws CommandSyntaxException {
        SectContributionService.addDebugContribution(source.getPlayerOrException(), amount);
        return 1;
    }

    private static int debugUnlockSkills(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            List<SkillType> unlocked = cultivation.unlockEligibleTechniqueSkills();
            SyncCultivationDataPacket.send(player, cultivation);
            SyncLearnedTechniquesPacket.send(player, cultivation);
            String names = unlocked.stream()
                    .map(SkillType::getDisplayName)
                    .collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.literal(unlocked.isEmpty()
                    ? "当前境界没有新的技能解锁，已刷新功法同步。"
                    : String.format("已解锁%d项技能：%s", unlocked.size(), names)), true);
        });
        return 1;
    }

    private static int breakthrough(CommandSourceStack source) throws CommandSyntaxException {
        BreakthroughService.attempt(source.getPlayerOrException());
        return 1;
    }

    private static Realm parseRealm(String value) {
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        for (Realm realm : Realm.values()) {
            if (realm.name().equals(normalized) || realm.getDesignId().equalsIgnoreCase(value)) {
                return realm;
            }
        }
        return null;
    }

    private static Component tribulationStatus(PlayerCultivation cultivation) {
        if (!cultivation.isTribulationActive()) {
            return Component.translatable("command.seeking_immortals.tribulation.none");
        }
        return Component.translatable("command.seeking_immortals.tribulation.active",
                cultivation.getTribulationTargetRealm().getDisplayName(),
                cultivation.getTribulationCurrentStrike(),
                cultivation.getTribulationTotalStrikes(),
                Math.max(0, (int)Math.ceil(cultivation.getTribulationNextStrikeTicks() / 20.0D)));
    }

    private static Component stockText(ShopService.Entry entry) {
        return stockText(entry.stock());
    }

    private static Component stockText(int stock) {
        return stock == ShopService.UNLIMITED_STOCK
                ? Component.translatable("command.seeking_immortals.market.stock.unlimited")
                : Component.literal(Integer.toString(Math.max(0, stock)));
    }

    /*
     * Command output is also a player-facing surface.  Catalog ids and enum names are
     * intentionally kept inside the command arguments, but never echoed back to a
     * normal player when a Chinese display name is available.
     */
    private static Component safeText(String value, String fallbackKey) {
        return PlayerDisplayText.safeLiteral(value, fallbackKey);
    }

    private static Component questDisplay(String id) {
        return ExtendedCatalogService.builtin().findQuest(id)
                .map(chain -> safeText(chain.display(), "text.seeking_immortals.unknown_quest"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_quest"));
    }

    private static Component npcDisplay(String id) {
        return NamedNpcRegistry.find(id)
                .map(npc -> safeText(npc.display(), "text.seeking_immortals.unknown_affiliation"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_affiliation"));
    }

    private static Component regionDisplay(String id) {
        return RegionRegistry.find(id)
                .map(region -> safeText(region.display(), "text.seeking_immortals.unknown_region"))
                .orElseGet(() -> WorldpackDataService.builtin().findRegion(id)
                        .map(region -> safeText(region.displayZh(), "text.seeking_immortals.unknown_region"))
                        .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_region")));
    }

    private static Component realmDisplay(String id) {
        return ArtifactDisplayTexts.realm(id);
    }

    private static Component itemDisplay(String id) {
        return PlayerDisplayText.itemName(id);
    }

    private static Component artifactDisplay(String id) {
        return ArtifactDataService.builtin().findArtifact(id)
                .map(artifact -> safeText(artifact.display(), "text.seeking_immortals.unknown_item"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_item"));
    }

    private static Component beastDisplay(String id) {
        return BeastBestiaryService.find(id)
                .map(entry -> safeText(entry.display(), "text.seeking_immortals.unknown_beast"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_beast"));
    }

    private static Component summonArchetypeDisplay(
            com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Archetype archetype) {
        if (archetype == null) {
            return Component.translatable("message.seeking_immortals.summon.archetype.generic");
        }
        return Component.translatable(switch (archetype) {
            case BEAST -> "message.seeking_immortals.summon.archetype.beast";
            case PUPPET -> "message.seeking_immortals.summon.archetype.puppet";
            case GHOST -> "message.seeking_immortals.summon.archetype.ghost";
            default -> "message.seeking_immortals.summon.archetype.generic";
        });
    }

    private static Component summonStanceDisplay(
            com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Stance stance) {
        if (stance == null) {
            return Component.translatable("message.seeking_immortals.summon.stance.follow");
        }
        return Component.translatable(switch (stance) {
            case GUARD -> "message.seeking_immortals.summon.stance.guard";
            case AGGRESSIVE -> "message.seeking_immortals.summon.stance.aggressive";
            case STAY -> "message.seeking_immortals.summon.stance.stay";
            default -> "message.seeking_immortals.summon.stance.follow";
        });
    }

    private static Component currencyDisplay(ShopService.Entry entry) {
        if (entry == null) {
            return Component.translatable("text.seeking_immortals.unknown_currency");
        }
        if (ShopService.CURRENCY_SECT_CONTRIBUTION.equals(entry.currency())) {
            return Component.translatable("screen.seeking_immortals.shop.currency.sect_contribution");
        }
        return itemDisplay(entry.currencyItemId());
    }

    private static Component factionDisplay(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        var sect = SectDefinitionService.find(normalized);
        if (sect.isPresent()) {
            return safeText(sect.get().displayZh(), "text.seeking_immortals.unknown_faction");
        }
        var catalog = ExtendedCatalogService.builtin().findSect(normalized);
        if (catalog.isPresent()) {
            return safeText(catalog.get().display(), "text.seeking_immortals.unknown_faction");
        }
        String key = "text.seeking_immortals.faction." + PlayerDisplayText.normalizeId(normalized);
        if (PlayerDisplayText.hasTranslation(key)) {
            return Component.translatable(key);
        }
        return Component.translatable("text.seeking_immortals.unknown_faction");
    }

    private static Component shopDisplay(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        // Shop ids are internal, so use a known faction/market label or a neutral fallback.
        if (ShopService.isMarketShop(normalized)) {
            return Component.literal("坊市");
        }
        String faction = normalized.replace("_contribution_hall", "")
                .replace("_merit_hall", "").replace("_shop", "");
        Component factionName = factionDisplay(faction);
        String rendered = factionName.getString();
        return PlayerDisplayText.isSafe(rendered)
                ? Component.literal(rendered + "坊市")
                : Component.literal("未知坊市");
    }

    private static Component dimensionDisplay(String id) {
        return com.xunxian.seekingimmortals.worldpack.DimensionRegistryService.find(id)
                .map(def -> safeText(def.display(), "text.seeking_immortals.unknown_dimension"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_dimension"));
    }

    private static Component branchDisplay(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "righteous" -> Component.literal("正道");
            case "demonic" -> Component.literal("魔道");
            case "neutral" -> Component.literal("中立");
            default -> Component.translatable("text.seeking_immortals.unknown_branch");
        };
    }

    private static Component statusDisplay(boolean value) {
        return Component.literal(value ? "是" : "否");
    }

    private static Component categoryDisplay(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Component.literal(switch (normalized) {
            case "herb" -> "灵草";
            case "ore", "mineral" -> "矿材";
            case "material" -> "材料";
            case "artifact" -> "法宝";
            case "pill" -> "丹药";
            case "talisman" -> "符箓";
            case "secret_realm_drop" -> "秘境产物";
            default -> "其他";
        });
    }

    private static Component rarityDisplay(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Component.literal(switch (normalized) {
            case "common" -> "常见";
            case "uncommon" -> "少见";
            case "rare" -> "稀有";
            case "epic" -> "珍奇";
            case "legendary" -> "传说";
            case "regional" -> "地域特产";
            default -> "未定品阶";
        });
    }

    private static Component alignmentDisplay(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Component.literal(switch (normalized) {
            case "righteous", "lawful", "good" -> "正道";
            case "demonic", "evil" -> "魔道";
            case "neutral" -> "中立";
            default -> "立场未明";
        });
    }

    private static Component discountDisplay(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Component.literal(switch (normalized) {
            case "honored-20%" -> "敬重八折";
            case "friendly-5%" -> "友善九五折";
            default -> "暂无优惠";
        });
    }

    private static Component spatialTypeDisplay(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "fixed_teleport_array" -> Component.translatable("text.seeking_immortals.spatial_type.fixed_array");
            case "ancient_rift" -> Component.translatable("text.seeking_immortals.spatial_type.ancient_rift");
            case "sect_gate" -> Component.translatable("text.seeking_immortals.spatial_type.sect_gate");
            case "ascension_gate" -> Component.translatable("text.seeking_immortals.spatial_type.ascension_gate");
            case "demon_rift_event" -> Component.translatable("text.seeking_immortals.spatial_type.demon_rift");
            case "pocket_gate" -> Component.translatable("text.seeking_immortals.spatial_type.pocket_gate");
            case "cycle_gate" -> Component.translatable("text.seeking_immortals.spatial_type.cycle_gate");
            case "hidden_rift" -> Component.translatable("text.seeking_immortals.spatial_type.hidden_rift");
            case "king_territory" -> Component.translatable("text.seeking_immortals.spatial_type.king_territory");
            default -> Component.translatable("text.seeking_immortals.unknown_spatial_type");
        };
    }

    private static Component ascensionStageDisplay(String id) {
        return com.xunxian.seekingimmortals.worldpack.AscensionService.snapshot().findStage(id)
                .map(stage -> safeText(stage.display(), "text.seeking_immortals.unknown_phase"))
                .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_phase"));
    }

    private static Component ascensionRequirementDisplay(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("realm_")) {
            return Component.literal("境界达到").append(realmDisplay(normalized.substring("realm_".length())));
        }
        if (normalized.startsWith("quest_flag_soft")) {
            return Component.literal("完成飞升引导任务");
        }
        return Component.literal(switch (normalized) {
            case "no_player" -> "缺少玩家数据";
            case "no_cultivation" -> "缺少修炼数据";
            case "realm_peak" -> "境界尚未圆满";
            case "tribulation_success" -> "尚未渡过天劫";
            default -> "未知条件";
        });
    }

    private static Component adminId(String id) {
        String value = id == null ? "" : id.trim();
        return value.isBlank() ? Component.empty() : Component.literal("内部标识「" + value + "」");
    }

}
