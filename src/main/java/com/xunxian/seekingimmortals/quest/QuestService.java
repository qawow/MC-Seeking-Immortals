package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.item.LingGenTestStoneItem;
import com.xunxian.seekingimmortals.item.MysticVialItem;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.network.SyncSkillDataPacket;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public final class QuestService {
    public static final String BRANCH_REPORT = "report";
    public static final String BRANCH_SILENT = "silent";
    public static final String BRANCH_BLACKMAIL = "blackmail";

    private QuestService() {}

    public static void show(ServerPlayer player) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            player.sendSystemMessage(Component.translatable(
                    "command.seeking_immortals.quest.status",
                    SevenMysteriesQuest.stageName(progress.getStage()),
                    SevenMysteriesQuest.objective(progress),
                    progress.getSectRole().isBlank() ? "-" : progress.getSectRole(),
                    progress.getContribution(),
                    progress.getReputation(),
                    progress.getBranchChoice().isBlank() ? "-" : progress.getBranchChoice()));
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.no_data")));
    }

    public static void start(ServerPlayer player) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (!progress.isStarted()) {
                progress.setStage(SevenMysteriesQuest.STAGE_ROOT_TEST);
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.started").withStyle(ChatFormatting.GOLD));
            }
            checkProgress(player, cultivation);
        }, () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.no_data")));
    }

    public static void reset(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.resetSevenMysteriesQuest();
            sync(player, cultivation);
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.reset"));
        });
    }

    public static void check(ServerPlayer player) {
        CultivationHelper.get(player).ifPresentOrElse(cultivation -> checkProgress(player, cultivation),
                () -> player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.no_data")));
    }

    public static void forceAdvance(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (!progress.isStarted()) {
                progress.setStage(SevenMysteriesQuest.STAGE_ROOT_TEST);
            } else if (!progress.isComplete()) {
                progress.setStage(progress.getStage() + 1);
            }
            sync(player, cultivation);
            show(player);
        });
    }

    public static boolean handleNamedVillagerInteraction(ServerPlayer player, Villager villager) {
        String name = villager.getCustomName() == null ? "" : villager.getCustomName().getString();
        if (SevenMysteriesQuest.NPC_MO_LAO.equals(name)) {
            start(player);
            LingGenTestStoneItem.testPlayer(player.serverLevel(), player, player, ItemStack.EMPTY, false, false);
            return true;
        }
        if (SevenMysteriesQuest.NPC_STEWARD.equals(name)) {
            check(player);
            return true;
        }
        // Wave55: text-quest named NPC authority entry (after seven-mysteries).
        return TextQuestNpcHookService.handleNamedVillagerInteraction(player, villager);
    }

    public static boolean handleBlockInteraction(ServerPlayer player, ServerLevel level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.CHISELED_BOOKSHELF)) {
            return discoverSecretRoom(player, level, pos);
        }
        if (state.is(Blocks.CRYING_OBSIDIAN)) {
            return useYuePortal(player, level, pos);
        }
        return false;
    }

    public static void onRootTested(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (!progress.isStarted()) return;
            progress.addFlag(SevenMysteriesQuest.FLAG_ROOT_TESTED);
            checkProgress(player, cultivation);
        });
    }

    public static void onMysticVialUsed(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (progress.getStage() == SevenMysteriesQuest.STAGE_SECRET) {
                progress.addFlag(SevenMysteriesQuest.FLAG_VIAL_USED);
                checkProgress(player, cultivation);
            }
        });
    }

    public static boolean chooseBranch(ServerPlayer player, String choice) {
        String normalized = choice == null ? "" : choice.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals(BRANCH_REPORT) && !normalized.equals(BRANCH_SILENT) && !normalized.equals(BRANCH_BLACKMAIL)) {
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.bad_choice"));
            return false;
        }
        CultivationHelper.get(player).ifPresent(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (progress.getStage() != SevenMysteriesQuest.STAGE_INFIGHTING || !progress.hasFlag(SevenMysteriesQuest.FLAG_EVIDENCE)) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.choice_locked"));
                return;
            }
            if (!progress.getBranchChoice().isBlank()) {
                player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.choice_done",
                        branchDisplay(progress.getBranchChoice())));
                return;
            }
            progress.setBranchChoice(normalized);
            switch (normalized) {
                case BRANCH_REPORT -> {
                    progress.addReputation(50);
                    progress.addContribution(80);
                }
                case BRANCH_BLACKMAIL -> {
                    progress.addReputation(-20);
                    progress.addContribution(120);
                    giveItem(player, ModItems.IMMORTAL_JADE.get(), 1);
                }
                default -> progress.addContribution(40);
            }
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.choice_success",
                    branchDisplay(normalized)));
            checkProgress(player, cultivation);
        });
        return true;
    }

    public static void spawnQuestVillager(ServerPlayer player, String name) {
        ServerLevel level = player.serverLevel();
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) return;
        villager.moveTo(player.getX() + 1.5D, player.getY(), player.getZ() + 1.5D, player.getYRot(), 0.0F);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        level.addFreshEntity(villager);
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.spawned_npc", name));
    }

    private static Component branchDisplay(String branch) {
        return switch (branch == null ? "" : branch.trim().toLowerCase(java.util.Locale.ROOT)) {
            case BRANCH_REPORT -> Component.literal("上报宗门");
            case BRANCH_SILENT -> Component.literal("保持沉默");
            case BRANCH_BLACKMAIL -> Component.literal("以证据要挟");
            default -> Component.translatable("text.seeking_immortals.unknown_branch");
        };
    }

    public static void placeSecretRoomMarker(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(2, 0, 0);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
            }
        }
        level.setBlock(origin, Blocks.CHISELED_BOOKSHELF.defaultBlockState(), 3);
        level.setBlock(origin.above(), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.getSevenMysteriesQuest().setSecretRoomMarker(origin);
            sync(player, cultivation);
        });
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.secret_marker", origin.getX(), origin.getY(), origin.getZ()));
    }

    public static void placeYuePortalMarker(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(2, 0, 0);
        for (int y = 0; y < 3; y++) {
            level.setBlock(origin.offset(0, y, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(0, 1, 1), Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.getSevenMysteriesQuest().setYuePortalMarker(origin);
            sync(player, cultivation);
        });
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.yue_marker", origin.getX(), origin.getY(), origin.getZ()));
    }

    public static void giveEvidence(ServerPlayer player) {
        giveItem(player, ModItems.SEVEN_MYSTERIES_EVIDENCE.get(), 1);
        CultivationHelper.get(player).ifPresent(cultivation -> {
            cultivation.getSevenMysteriesQuest().addFlag(SevenMysteriesQuest.FLAG_EVIDENCE);
            sync(player, cultivation);
        });
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.evidence_given"));
    }

    public static void triggerAttack(ServerPlayer player) {
        CultivationHelper.get(player).ifPresent(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            progress.addFlag(SevenMysteriesQuest.FLAG_ATTACK_TRIGGERED);
            progress.addFlag(SevenMysteriesQuest.FLAG_ESCAPE_READY);
            checkProgress(player, cultivation);
        });
    }

    private static void checkProgress(ServerPlayer player, PlayerCultivation cultivation) {
        QuestProgress progress = cultivation.getSevenMysteriesQuest();
        boolean changed;
        do {
            changed = false;
            switch (progress.getStage()) {
                case SevenMysteriesQuest.STAGE_ROOT_TEST -> changed = completeRootStage(player, cultivation, progress);
                case SevenMysteriesQuest.STAGE_ENTRY -> changed = completeEntryStage(player, cultivation, progress);
                case SevenMysteriesQuest.STAGE_SECRET -> changed = completeSecretStage(player, cultivation, progress);
                case SevenMysteriesQuest.STAGE_INFIGHTING -> changed = completeInfightingStage(player, progress);
                case SevenMysteriesQuest.STAGE_LEAVE -> changed = completeLeaveStage(player, progress);
                default -> {
                }
            }
        } while (changed);
        sync(player, cultivation);
        show(player);
    }

    private static boolean completeRootStage(ServerPlayer player, PlayerCultivation cultivation, QuestProgress progress) {
        if (!cultivation.isSpiritualRootTested() && !progress.hasFlag(SevenMysteriesQuest.FLAG_ROOT_TESTED)) return false;
        progress.addFlag(SevenMysteriesQuest.FLAG_ROOT_TESTED);
        progress.addFlag(SevenMysteriesQuest.FLAG_ADMITTED);
        progress.setSect("seven_mysteries", "外门弟子");
        progress.setStage(SevenMysteriesQuest.STAGE_ENTRY);
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.stage_complete", SevenMysteriesQuest.stageName(SevenMysteriesQuest.STAGE_ROOT_TEST)));
        return true;
    }

    private static boolean completeEntryStage(ServerPlayer player, PlayerCultivation cultivation, QuestProgress progress) {
        boolean changed = false;
        if (!progress.hasFlag(SevenMysteriesQuest.FLAG_HUANGLONG_MANUAL)) {
            giveItem(player, ModItems.TECHNIQUE_MANUAL_HUANGLONG_METHOD.get(), 1);
            progress.addFlag(SevenMysteriesQuest.FLAG_HUANGLONG_MANUAL);
            changed = true;
        }
        if (!progress.hasFlag(SevenMysteriesQuest.FLAG_LABOR_DONE) && consumeItems(player, ModItems.SPIRIT_GRASS.get(), 10)) {
            progress.addFlag(SevenMysteriesQuest.FLAG_LABOR_DONE);
            progress.addContribution(30);
            changed = true;
        }
        if (!progress.hasFlag(SevenMysteriesQuest.FLAG_ALCHEMY_LEARNED)) {
            cultivation.unlockSkillForQuest(SkillType.ALCHEMY);
            cultivation.addSkillExperience(SkillType.ALCHEMY, 30);
            progress.addFlag(SevenMysteriesQuest.FLAG_ALCHEMY_LEARNED);
            changed = true;
        }
        if (progress.hasFlag(SevenMysteriesQuest.FLAG_HUANGLONG_MANUAL)
                && progress.hasFlag(SevenMysteriesQuest.FLAG_LABOR_DONE)
                && progress.hasFlag(SevenMysteriesQuest.FLAG_ALCHEMY_LEARNED)) {
            progress.setStage(SevenMysteriesQuest.STAGE_SECRET);
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.stage_complete", SevenMysteriesQuest.stageName(SevenMysteriesQuest.STAGE_ENTRY)));
            return true;
        }
        return changed;
    }

    private static boolean completeSecretStage(ServerPlayer player, PlayerCultivation cultivation, QuestProgress progress) {
        if (!progress.hasFlag(SevenMysteriesQuest.FLAG_SECRET_ROOM)) return false;
        boolean changed = false;
        if (!progress.hasFlag(SevenMysteriesQuest.FLAG_VIAL_GRANTED)) {
            grantMysticVial(player, cultivation);
            progress.addFlag(SevenMysteriesQuest.FLAG_VIAL_GRANTED);
            changed = true;
        }
        if (progress.hasFlag(SevenMysteriesQuest.FLAG_VIAL_USED)) {
            progress.setStage(SevenMysteriesQuest.STAGE_INFIGHTING);
            giveEvidence(player);
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.stage_complete", SevenMysteriesQuest.stageName(SevenMysteriesQuest.STAGE_SECRET)));
            return true;
        }
        return changed;
    }

    private static boolean completeInfightingStage(ServerPlayer player, QuestProgress progress) {
        if (!progress.hasFlag(SevenMysteriesQuest.FLAG_EVIDENCE) && hasItem(player, ModItems.SEVEN_MYSTERIES_EVIDENCE.get(), 1)) {
            consumeItems(player, ModItems.SEVEN_MYSTERIES_EVIDENCE.get(), 1);
            progress.addFlag(SevenMysteriesQuest.FLAG_EVIDENCE);
            return true;
        }
        if (!progress.hasFlag(SevenMysteriesQuest.FLAG_EVIDENCE) || progress.getBranchChoice().isBlank()) return false;
        progress.setStage(SevenMysteriesQuest.STAGE_LEAVE);
        progress.addFlag(SevenMysteriesQuest.FLAG_ATTACK_TRIGGERED);
        progress.addFlag(SevenMysteriesQuest.FLAG_ESCAPE_READY);
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.stage_complete", SevenMysteriesQuest.stageName(SevenMysteriesQuest.STAGE_INFIGHTING)));
        return true;
    }

    private static boolean completeLeaveStage(ServerPlayer player, QuestProgress progress) {
        if (progress.hasFlag(SevenMysteriesQuest.FLAG_ATTACK_TRIGGERED)
                && progress.hasFlag(SevenMysteriesQuest.FLAG_ESCAPE_READY)
                && progress.hasFlag(SevenMysteriesQuest.FLAG_YUE_PORTAL)
                && progress.hasYueArrived()) {
            if (!progress.hasFlag(SevenMysteriesQuest.FLAG_FINAL_REWARD)) {
                grantFinalReward(player);
                progress.addFlag(SevenMysteriesQuest.FLAG_FINAL_REWARD);
            }
            progress.setStage(SevenMysteriesQuest.STAGE_COMPLETE);
            player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.stage_complete", SevenMysteriesQuest.stageName(SevenMysteriesQuest.STAGE_LEAVE)));
            return true;
        }
        return false;
    }

    private static boolean discoverSecretRoom(ServerPlayer player, ServerLevel level, BlockPos pos) {
        boolean[] handled = { false };
        CultivationHelper.get(player).ifPresent(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (progress.getStage() == SevenMysteriesQuest.STAGE_SECRET
                    && pos.equals(progress.getSecretRoomMarker())
                    && isSecretRoomMarkerStructure(level, pos)) {
                progress.addFlag(SevenMysteriesQuest.FLAG_SECRET_ROOM);
                checkProgress(player, cultivation);
                handled[0] = true;
            }
        });
        return handled[0];
    }

    private static boolean useYuePortal(ServerPlayer player, ServerLevel level, BlockPos portalPos) {
        boolean[] handled = { false };
        CultivationHelper.get(player).ifPresent(cultivation -> {
            QuestProgress progress = cultivation.getSevenMysteriesQuest();
            if (progress.getStage() != SevenMysteriesQuest.STAGE_LEAVE
                    || !portalPos.equals(progress.getYuePortalMarker())
                    || !isYuePortalMarkerStructure(level, portalPos)) {
                return;
            }
            progress.addFlag(SevenMysteriesQuest.FLAG_YUE_PORTAL);
            progress.setYueArrived(true);
            teleportToYue(player, portalPos);
            checkProgress(player, cultivation);
            handled[0] = true;
        });
        return handled[0];
    }

    private static boolean isSecretRoomMarkerStructure(ServerLevel level, BlockPos origin) {
        if (!level.getBlockState(origin).is(Blocks.CHISELED_BOOKSHELF)) return false;
        BlockState lantern = level.getBlockState(origin.above());
        if (!lantern.is(Blocks.SOUL_LANTERN) && !lantern.is(Blocks.LANTERN)) return false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!level.getBlockState(origin.offset(x, -1, z)).is(Blocks.POLISHED_DEEPSLATE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isYuePortalMarkerStructure(ServerLevel level, BlockPos origin) {
        for (int y = 0; y < 3; y++) {
            if (!level.getBlockState(origin.offset(0, y, 0)).is(Blocks.CRYING_OBSIDIAN)) {
                return false;
            }
        }
        return level.getBlockState(origin.offset(0, 1, 1)).is(Blocks.AMETHYST_BLOCK);
    }

    private static void teleportToYue(ServerPlayer player, BlockPos portalPos) {
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server == null ? null : server.getLevel(Level.OVERWORLD);
        ServerLevel targetLevel = overworld == null ? player.serverLevel() : overworld;
        BlockPos target = new BlockPos(128, targetLevel.getMinBuildHeight() + 80, 128);
        while (target.getY() > targetLevel.getMinBuildHeight() + 2 && !targetLevel.getBlockState(target.below()).isSolid()) {
            target = target.below();
        }
        if (targetLevel == player.serverLevel()) {
            player.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        } else {
            player.teleportTo(targetLevel, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D,
                    EnumSet.noneOf(RelativeMovement.class), player.getYRot(), player.getXRot());
        }
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.quest.teleported_yue", target.getX(), target.getY(), target.getZ()));
    }

    private static void grantMysticVial(ServerPlayer player, PlayerCultivation cultivation) {
        if (hasItem(player, ModItems.MYSTIC_VIAL.get(), 1)) {
            cultivation.setMysticVialGranted(true);
            return;
        }
        ItemStack vial = new ItemStack(ModItems.MYSTIC_VIAL.get());
        MysticVialItem.setOwner(vial, player);
        MysticVialItem.refillIfNeeded(vial, System.currentTimeMillis());
        giveStack(player, vial);
        cultivation.setMysticVialGranted(true);
        player.sendSystemMessage(Component.translatable("message.seeking_immortals.mystic_vial.granted"));
    }

    private static void grantFinalReward(ServerPlayer player) {
        giveItem(player, ModItems.METAL_SPIRIT_STONE.get(), 60);
        giveItem(player, ModItems.WOOD_SPIRIT_STONE.get(), 60);
        giveItem(player, ModItems.WATER_SPIRIT_STONE.get(), 60);
        giveItem(player, ModItems.FIRE_ELEMENT_SPIRIT_STONE.get(), 60);
        giveItem(player, ModItems.EARTH_SPIRIT_STONE.get(), 60);
    }

    private static boolean hasItem(ServerPlayer player, Item item, int count) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total >= count;
    }

    private static boolean consumeItems(ServerPlayer player, Item item, int count) {
        if (player.getAbilities().instabuild) return true;
        if (!hasItem(player, item, count)) return false;
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) continue;
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (remaining <= 0) return true;
        }
        return true;
    }

    private static void giveItem(ServerPlayer player, Item item, int count) {
        int remaining = count;
        int maxStackSize = new ItemStack(item).getMaxStackSize();
        while (remaining > 0) {
            int batch = Math.min(maxStackSize, remaining);
            giveStack(player, new ItemStack(item, batch));
            remaining -= batch;
        }
    }

    private static void giveStack(ServerPlayer player, ItemStack stack) {
        com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                player, stack, "quest_reward");
    }

    private static void sync(ServerPlayer player, PlayerCultivation cultivation) {
        SyncCultivationDataPacket.send(player, cultivation);
        SyncLearnedTechniquesPacket.send(player, cultivation);
        SyncSkillDataPacket.send(player, cultivation);
    }
}
