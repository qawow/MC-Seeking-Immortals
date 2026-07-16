package com.xunxian.seekingimmortals.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
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
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TribulationService;
import com.xunxian.seekingimmortals.entity.MarketTraderEntity;
import com.xunxian.seekingimmortals.entity.SpiritStoneBankerEntity;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.phase.SoftPhaseShellService;
import com.xunxian.seekingimmortals.quest.MainStorySoftService;
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
import com.xunxian.seekingimmortals.worldpack.SpatialNodeCatalogService;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import com.xunxian.seekingimmortals.worldpack.WorldpackDataService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
                        .then(Commands.literal("refine")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> artifactRefine(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("plan")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> artifactPlan(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("natal")
                                .executes(ctx -> natalStatus(ctx.getSource()))
                                .then(Commands.literal("bind").executes(ctx -> natalBind(ctx.getSource())))
                                .then(Commands.literal("grow").executes(ctx -> natalGrow(ctx.getSource())))))
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
                                .then(Commands.literal("start")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestStart(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("advance")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestAdvance(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("status")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestStatus(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("cost")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestCost(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("branch")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("choice", StringArgumentType.word())
                                                        .executes(ctx -> textQuestBranch(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "choice"))))))
                                .then(Commands.literal("talk")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestTalk(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                                                .then(Commands.argument("choice", StringArgumentType.word())
                                                        .executes(ctx -> textQuestTalkAct(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "choice"))))))
                                .then(Commands.literal("gui")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestGui(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("hooks")
                                        .executes(ctx -> textQuestHooks(ctx.getSource()))
                                        .then(Commands.literal("accept")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> textQuestHookAccept(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestHookPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("spawn_npc").requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> textQuestSpawnNpc(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("interact")
                                        .then(Commands.argument("npc", StringArgumentType.word())
                                                .executes(ctx -> textQuestInteract(ctx.getSource(), StringArgumentType.getString(ctx, "npc")))))
                                .then(Commands.literal("story")
                                        .executes(ctx -> mainStoryList(ctx.getSource()))
                                        .then(Commands.literal("list").executes(ctx -> mainStoryList(ctx.getSource())))
                                        .then(Commands.literal("start")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> mainStoryStart(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("complete")
                                                .then(Commands.argument("id", StringArgumentType.word())
                                                        .executes(ctx -> mainStoryComplete(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))))
                .then(Commands.literal("market")
                        .executes(ctx -> marketOpen(ctx.getSource()))
                        .then(Commands.literal("open")
                                .executes(ctx -> marketOpen(ctx.getSource()))
                                .then(Commands.argument("shopId", StringArgumentType.word())
                                        .executes(ctx -> marketOpen(ctx.getSource(), StringArgumentType.getString(ctx, "shopId")))))
                        .then(Commands.literal("list")
                                .executes(ctx -> marketList(ctx.getSource()))
                                .then(Commands.argument("shopId", StringArgumentType.word())
                                        .executes(ctx -> marketList(ctx.getSource(), StringArgumentType.getString(ctx, "shopId")))))
                        .then(Commands.literal("buy")
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
                        .then(Commands.literal("enter")
                                .then(Commands.argument("realm", StringArgumentType.word())
                                        .executes(ctx -> worldpackEnter(ctx.getSource(), StringArgumentType.getString(ctx, "realm")))))
                        .then(Commands.literal("return").executes(ctx -> worldpackReturn(ctx.getSource())))
                        .then(Commands.literal("set_anchor").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("anchor", StringArgumentType.word())
                                        .executes(ctx -> worldpackSetAnchor(ctx.getSource(), StringArgumentType.getString(ctx, "anchor")))))
                        .then(Commands.literal("regions").executes(ctx -> worldpackRegions(ctx.getSource())))
                        .then(Commands.literal("realms").executes(ctx -> worldpackRealms(ctx.getSource())))
                        .then(Commands.literal("events").executes(ctx -> worldpackEvents(ctx.getSource()))))
                .then(Commands.literal("catalog")
                        .executes(ctx -> catalogSummary(ctx.getSource()))
                        .then(Commands.literal("summary").executes(ctx -> catalogSummary(ctx.getSource())))
                        .then(Commands.literal("manual")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogStudyManual(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("flight")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogBoardFlight(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("methods")
                                .executes(ctx -> catalogMethods(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogMethods(ctx.getSource())))
                                .then(Commands.literal("learn")
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
                                .then(Commands.literal("open").executes(ctx -> catalogAuctionOpen(ctx.getSource())))
                                .then(Commands.literal("interest")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogAuctionInterest(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("bid")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogAuctionBid(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("settle")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogAuctionSettle(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogAuctionPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("spatial")
                                .executes(ctx -> catalogSpatialList(ctx.getSource()))
                                .then(Commands.literal("travel")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogSpatialTravel(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogSpatialPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
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
                                .then(Commands.literal("craft")
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
                                .then(Commands.literal("deploy")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogFormationsDeploy(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogFormationsPreview(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("talisman")
                                .executes(ctx -> catalogTalismanList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogTalismanList(ctx.getSource())))
                                .then(Commands.literal("craft")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogTalismanCraft(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("puppet")
                                .executes(ctx -> catalogPuppetList(ctx.getSource()))
                                .then(Commands.literal("list").executes(ctx -> catalogPuppetList(ctx.getSource())))
                                .then(Commands.literal("craft")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> catalogPuppetCraft(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                                .then(Commands.literal("repair").executes(ctx -> catalogPuppetRepair(ctx.getSource()))))
                        .then(Commands.literal("chronicle")
                                .executes(ctx -> catalogChronicleList(ctx.getSource()))
                                .then(Commands.literal("discover")
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
                                .then(Commands.literal("contract")
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
                .then(Commands.literal("boss")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> bossSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("phase")
                        .executes(ctx -> phaseStatus(ctx.getSource()))
                        .then(Commands.literal("mark")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> phaseMark(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("enter")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> phaseEnter(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("mission")
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
                        .then(Commands.literal("open").executes(ctx -> sectOpen(ctx.getSource())))
                        .then(Commands.literal("join").executes(ctx -> sectJoin(ctx.getSource())))
                        .then(Commands.literal("candidates").executes(ctx -> sectCandidates(ctx.getSource())))
                        .then(Commands.literal("apply")
                                .then(Commands.argument("sectId", StringArgumentType.word())
                                        .executes(ctx -> sectApply(ctx.getSource(), StringArgumentType.getString(ctx, "sectId")))))
                        .then(Commands.literal("advance").executes(ctx -> sectAdvance(ctx.getSource())))
                        .then(Commands.literal("shop").executes(ctx -> sectShop(ctx.getSource())))
                        .then(Commands.literal("buy")
                                .then(Commands.argument("entry", StringArgumentType.word())
                                        .executes(ctx -> sectBuy(ctx.getSource(), StringArgumentType.getString(ctx, "entry")))))
                        .then(Commands.literal("donate")
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
                                                StringArgumentType.getString(ctx, "note"))))))
                .then(Commands.literal("breakthrough").executes(ctx -> breakthrough(ctx.getSource()))));
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
            String line = progress.id() + " | stage=" + progress.stage() + "/" + progress.stepCount()
                    + (progress.complete() ? " | DONE" : "");
            source.sendSuccess(() -> Component.literal(line), false);
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

    private static int textQuestAdvance(CommandSourceStack source, String id) throws CommandSyntaxException {
        return TextQuestChainService.advance(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int textQuestStatus(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TextQuestChainService.ChainProgress progress = TextQuestChainService.progressOf(player, id);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.status",
                progress.id(), progress.stage(), progress.stepCount(),
                progress.complete() ? "DONE" : "ACTIVE"), false);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.branch_status",
                TextQuestChainService.getBranch(player, id), TextQuestChainService.getNpc(player, id)), false);
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
                stageCost.displayKey(), stageCost.count(), owned), false);
    }

    private static int textQuestBranch(CommandSourceStack source, String id, String choice) throws CommandSyntaxException {
        return TextQuestChainService.chooseBranch(source.getPlayerOrException(), id, choice) ? 1 : 0;
    }


    private static int natalStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String id = com.xunxian.seekingimmortals.artifact.NatalBindingService.boundId(player);
        int growth = com.xunxian.seekingimmortals.artifact.NatalBindingService.growth(player);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.natal.status",
                id.isBlank() ? "-" : id, growth), false);
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
            source.sendFailure(Component.translatable("message.seeking_immortals.artifact.refine.unknown_recipe", recipeId));
            return 0;
        }
        String materials = recipe.materials().stream()
                .map(m -> m.id() + "x" + m.count())
                .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.translatable("message.seeking_immortals.artifact.refine.plan",
                recipe.id(),
                recipe.realmMin() == null ? "?" : recipe.realmMin(),
                String.format(Locale.ROOT, "%.0f%%", Math.max(0.0D, Math.min(1.0D, recipe.baseSuccessRate())) * 100.0D),
                materials.isBlank() ? "-" : materials), false);
        source.sendSuccess(() -> Component.translatable("message.seeking_immortals.artifact.refine.plan_ready", recipe.id()), false);
        return 1;
    }

    private static int textQuestTalk(CommandSourceStack source, String id) throws CommandSyntaxException {
        return TextQuestDialogueService.talk(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int textQuestTalkAct(CommandSourceStack source, String id, String choice) throws CommandSyntaxException {
        return TextQuestDialogueService.act(source.getPlayerOrException(), id, choice) ? 1 : 0;
    }

    private static int textQuestGui(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        com.xunxian.seekingimmortals.network.OpenDialogueScreenPacket.send(player, id);
        // Also push current dialogue lines to chat for immediate feedback.
        TextQuestDialogueService.talk(player, id);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.gui_opened", id), false);
        return 1;
    }

    private static int textQuestHooks(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.hooks.header",
                QuestHookSoftService.hookCount()), false);
        for (String line : QuestHookSoftService.sampleHooks(20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int textQuestHookPreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return QuestHookSoftService.preview(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int textQuestHookAccept(CommandSourceStack source, String id) throws CommandSyntaxException {
        return QuestHookSoftService.accept(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int textQuestSpawnNpc(CommandSourceStack source, String chainId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String npcId = TextQuestChainService.npcFor(chainId);
        String display = TextQuestNpcHookService.displayNameForNpc(npcId);
        QuestService.spawnQuestVillager(player, display);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.text_quest.spawn_npc",
                chainId, display, npcId), true);
        return 1;
    }

    private static int textQuestInteract(CommandSourceStack source, String npc) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return TextQuestNpcHookService.chainForNpcId(player, npc)
                .map(chainId -> TextQuestNpcHookService.openDialogue(player, chainId, true) ? 1 : 0)
                .orElseGet(() -> {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.quest_hook.unknown", npc), false);
                    return 0;
                });
    }

    private static int mainStoryList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.main_story.header",
                MainStorySoftService.chapterCount(), MainStorySoftService.completedCount(player)), false);
        for (String line : MainStorySoftService.list(player)) {
            source.sendSuccess(() -> Component.literal(line), false);
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
        QuestService.spawnQuestVillager(source.getPlayerOrException(), npcName);
        return 1;
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
                "Artifact catalog: %d artifacts, %d refinement recipes, %d flight vehicles, %d talisman templates.",
                snapshot.artifacts().size(),
                snapshot.refinementRecipes().size(),
                snapshot.flightVehicles().size(),
                snapshot.talismanTreasureTemplates().size())), false);
        source.sendSuccess(() -> Component.literal(
                "Use /seeking_immortals artifact p0, info <id>, recipe <artifact_id>, or files."), false);
        return artifactPriority(source, "P0_launch");
    }

    private static int artifactPriority(CommandSourceStack source, String priorityTier) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        List<ArtifactDataService.ArtifactDefinition> artifacts = snapshot.priorityArtifacts(priorityTier);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Artifact priority %s: %d resolved entries from %d ids.",
                priorityTier,
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
            source.sendFailure(Component.literal("Unknown artifact id: " + artifactId));
            return 0;
        }
        source.sendSuccess(() -> artifactLine(snapshot, artifact), false);
        if (!artifact.tags().isEmpty()) {
            source.sendSuccess(() -> Component.literal("tags=" + String.join(",", artifact.tags())), false);
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
            source.sendFailure(Component.literal("Unknown artifact/refinement recipe id: " + artifactOrRecipeId));
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
                "Artifact data files: %d", ArtifactDataService.sourceFiles().size())), false);
        snapshot.sourceFileEntryCounts().forEach((file, count) ->
                source.sendSuccess(() -> Component.literal(file + " entries=" + count), false));
        return 1;
    }

    private static Component artifactLine(ArtifactDataService.Snapshot snapshot,
            ArtifactDataService.ArtifactDefinition artifact) {
        return Component.literal(String.format(Locale.ROOT,
                "- %s | %s | tier=%s (%s) | realm=%s | type=%s | gameTier=%d",
                artifact.id(),
                artifact.display(),
                artifact.tier(),
                snapshot.tierDisplay(artifact.tier()),
                artifact.realmMin(),
                artifact.type(),
                artifact.gameTier()));
    }

    private static Component artifactRecipeLine(ArtifactDataService.Snapshot snapshot,
            ArtifactDataService.RefinementRecipe recipe) {
        return Component.literal(String.format(Locale.ROOT,
                "Recipe %s -> %s | %s | tier=%s (%s) | realm=%s | forgeGrade=%d | success=%s",
                recipe.id(),
                recipe.artifactId(),
                recipe.display(),
                recipe.tier(),
                snapshot.tierDisplay(recipe.tier()),
                recipe.realmMin(),
                recipe.forgeGrade(),
                successPercent(recipe.baseSuccessRate())));
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
            source.sendFailure(Component.translatable("message.seeking_immortals.market.unknown_shop", shopId));
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
            source.sendFailure(Component.translatable("message.seeking_immortals.market.unknown_shop", shopId));
            return 0;
        }
        ShopService.CostModifier modifier = source.getEntity() instanceof ServerPlayer player
                ? WorldpackGameplayService.marketCostModifier(player)
                : ShopService.CostModifier.NONE;
        source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.market.header",
                normalizedShopId), false);
        for (ShopService.Entry entry : ShopService.entries(normalizedShopId)) {
            int cost = ShopService.adjustedCost(normalizedShopId, entry, modifier);
            source.sendSuccess(() -> Component.translatable(
                    "command.seeking_immortals.market.entry",
                    entry.id(),
                    ShopService.itemName(entry),
                    entry.count(),
                    cost,
                    entry.currencyItemId(),
                    stockText(entry)), false);
        }
        return 1;
    }

    private static int marketBuy(CommandSourceStack source, String shopId, String entry) throws CommandSyntaxException {
        String normalizedShopId = ShopService.canonicalMarketShopId(shopId);
        if (normalizedShopId.isBlank()) {
            source.sendFailure(Component.translatable("message.seeking_immortals.market.unknown_shop", shopId));
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
                        result.entry().currencyItemId(),
                        stockText(result.remainingStock())));
                return 1;
            }
            case UNKNOWN_ENTRY -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.market.unknown_entry", entry));
            case UNSUPPORTED_CURRENCY -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.market.unsupported_currency", entry));
            case BAD_ITEM -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.market.bad_shop_item", entry));
            case BAD_CURRENCY_ITEM -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.market.bad_currency_item", entry));
            case NOT_ENOUGH_CURRENCY -> player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.market.not_enough_currency",
                    result.paidCost(),
                    result.entry() == null ? "-" : result.entry().currencyItemId()));
            case OUT_OF_STOCK -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.market.out_of_stock", entry));
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
            source.sendSuccess(() -> Component.translatable(
                    "command.seeking_immortals.worldpack.region.entry",
                    region.id(),
                    region.displayZh(),
                    region.displayEn(),
                    region.minRealm(),
                    String.format(Locale.ROOT, "%.2f", region.auraMultiplier()),
                    region.travelAnchor()), false);
        }
        return 1;
    }

    private static int worldpackRealms(CommandSourceStack source) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.worldpack.realms.header", snapshot.secretRealms().size()), false);
        for (WorldpackDataService.SecretRealm realm : snapshot.secretRealms()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.seeking_immortals.worldpack.realm.entry",
                    realm.id(),
                    realm.displayZh(),
                    realm.displayEn(),
                    realm.regionId(),
                    realm.minRealm(),
                    realm.ticketItem(),
                    realm.cooldownTicks()), false);
        }
        return 1;
    }

    private static int worldpackEvents(CommandSourceStack source) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.worldpack.events.header", snapshot.dailyEvents().size()), false);
        for (WorldpackDataService.DailyEvent event : snapshot.dailyEvents()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.seeking_immortals.worldpack.event.entry",
                    event.id(),
                    event.displayZh(),
                    event.displayEn(),
                    event.regionId(),
                    event.weight(),
                    event.durationTicks()), false);
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
            String line = entry.id() + " | entries=" + entry.entries() + " | key=" + entry.primaryKey();
            source.sendSuccess(() -> Component.literal(line), false);
            if (++shown >= 30) {
                int remaining = manifest.totalFiles() - shown;
                source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.methods.truncated", remaining), false);
                break;
            }
        }
        return 1;
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
            source.sendSuccess(() -> Component.literal("venue " + venue.id() + " | " + venue.region() + " | " + venue.faction()), false);
        }
        int shown = 0;
        for (AuctionSoftService.Lot lot : snapshot.lots()) {
            source.sendSuccess(() -> Component.literal("lot " + lot.id() + " | " + lot.display() + " | " + lot.minEquiv() + "-" + lot.maxEquiv()), false);
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

    private static int catalogSpatialList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.spatial.header",
                SpatialNodeCatalogService.builtin().size()), false);
        for (String line : SpatialNodeCatalogService.builtin().sample(20)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogSpatialPreview(CommandSourceStack source, String id) throws CommandSyntaxException {
        return SpatialNodeCatalogService.preview(source.getPlayerOrException(), id) ? 1 : 0;
    }

    private static int catalogSpatialTravel(CommandSourceStack source, String id) throws CommandSyntaxException {
        boolean ok = SpatialNodeCatalogService.travel(source.getPlayerOrException(), id);
        if (ok) {
            source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.spatial.travel_ok", id), false);
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
            source.sendSuccess(() -> Component.literal(entry.getKey() + " = " + entry.getValue()), false);
        }
        return 1;
    }

    private static int catalogReputationGet(CommandSourceStack source, String faction) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int value = ReputationService.get(player, faction);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.value",
                faction, value), false);
        return 1;
    }

    private static int catalogReputationDiscount(CommandSourceStack source, String shopId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        double mult = ReputationService.shopDiscountMultiplier(player, shopId);
        String label = ReputationService.discountLabel(player, shopId);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.discount",
                shopId, label, mult), false);
        return 1;
    }

    private static int catalogReputationAdd(CommandSourceStack source, String faction, int delta) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int next = ReputationService.add(player, faction, delta);
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.reputation.added",
                faction, delta, next), true);
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
        for (String line : snapshot.sampleFiles(30)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int catalogBulkShow(CommandSourceStack source, String name) {
        var optional = BulkCatalogIndexService.builtin().find(name);
        if (optional.isEmpty()) {
            source.sendFailure(Component.translatable("command.seeking_immortals.catalog.bulk.unknown", name));
            return 0;
        }
        BulkCatalogIndexService.IndexFile file = optional.get();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.bulk.show",
                file.file(), file.size(), file.primaryKey()), false);
        int shown = 0;
        for (BulkCatalogIndexService.Entry entry : file.entries().values()) {
            source.sendSuccess(() -> Component.literal(entry.id() + " | " + entry.display()), false);
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
                source.sendSuccess(() -> Component.literal(servitor.getSummonId() + " | "
                        + servitor.getArchetype().name().toLowerCase() + " | "
                        + servitor.getStance().name().toLowerCase()), false));
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
        lines.forEach((id, info) -> source.sendSuccess(() -> Component.literal(id + " | " + info), false));
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
        String status = com.xunxian.seekingimmortals.phase.SoftPhaseShellService.status(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal(status), false);
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
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.war.status", status), false);
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
        source.sendSuccess(() -> Component.translatable(present
                        ? "command.seeking_immortals.catalog.has.yes"
                        : "command.seeking_immortals.catalog.has.no",
                id,
                present ? manifest.find(id).map(TextMaterialManifestService.FileEntry::entries).orElse(0) : 0), false);
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
            String line = method.id() + " | " + method.display() + " | " + method.realmMin() + " | " + method.school();
            source.sendSuccess(() -> Component.literal(line), false);
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
            String line = key;
            var optional = TextMaterialCatalogService.builtin().findMethod(key);
            if (optional.isPresent()) {
                line = optional.get().id() + " | " + optional.get().display();
            }
            String finalLine = line;
            source.sendSuccess(() -> Component.literal(finalLine), false);
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
            source.sendSuccess(() -> Component.literal(flavor.id() + " | " + flavor.openCondition()
                    + " | " + flavor.environment()), false);
        }
        return 1;
    }

    private static int catalogQuests(CommandSourceStack source) {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.quests.header",
                snapshot.questChains().size()), false);
        int shown = 0;
        for (ExtendedCatalogService.QuestChain chain : snapshot.questChains().values()) {
            String line = chain.id() + " | " + chain.display() + " | steps=" + chain.stepCount() + " | " + chain.region();
            source.sendSuccess(() -> Component.literal(line), false);
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
            source.sendSuccess(() -> Component.literal(sect.id() + " | " + sect.display() + " | " + sect.region()
                    + " | " + sect.alignment()), false);
        }
        return 1;
    }

    private static int catalogBands(CommandSourceStack source) {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.bands.header",
                snapshot.priceBands().size()), false);
        for (ExtendedCatalogService.PriceBand band : snapshot.priceBands().values()) {
            source.sendSuccess(() -> Component.literal(band.id() + " | min=" + band.min() + " max=" + band.max()
                    + " suggested=" + band.suggested()), false);
        }
        return 1;
    }

    private static int catalogChapters(CommandSourceStack source) {
        ExtendedCatalogService.Snapshot snapshot = ExtendedCatalogService.builtin();
        source.sendSuccess(() -> Component.translatable("command.seeking_immortals.catalog.chapters.header",
                snapshot.chapters().size()), false);
        for (ExtendedCatalogService.StoryChapter chapter : snapshot.chapters().values()) {
            source.sendSuccess(() -> Component.literal(chapter.id() + " | " + chapter.display() + " | " + chapter.region()), false);
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
                player.sendSystemMessage(Component.translatable(
                        "command.seeking_immortals.sect.candidate",
                        definition.id(),
                        definition.displayZh(),
                        definition.displayEn(),
                        definition.focusZh()));
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
                    "Set cultivation=%d -> %s %s, stage progress %d/%d, mana cap %d.",
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
                    "Filled mana to %d/%d.",
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
                    ? "No new skills unlocked for current realm; learned technique sync refreshed."
                    : String.format("Unlocked %d skill(s): %s", unlocked.size(), names)), true);
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
}
