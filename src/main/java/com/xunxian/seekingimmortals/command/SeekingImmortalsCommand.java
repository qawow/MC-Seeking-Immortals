package com.xunxian.seekingimmortals.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SeekingImmortalsCommand {
    private SeekingImmortalsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seeking_immortals")
                .then(Commands.literal("lingli").executes(ctx -> showSpiritualPower(ctx.getSource())))
                .then(Commands.literal("qi").executes(ctx -> showSpiritualPower(ctx.getSource())))
                .then(Commands.literal("realm").executes(ctx -> showRealm(ctx.getSource())))
                .then(Commands.literal("root").executes(ctx -> showRoot(ctx.getSource())))
                .then(Commands.literal("affliction").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("severe_injury").executes(ctx -> applySevereInjury(ctx.getSource())))
                        .then(Commands.literal("heart_demon").executes(ctx -> applyHeartDemon(ctx.getSource())))
                        .then(Commands.literal("realm_fall").executes(ctx -> applyRealmFall(ctx.getSource())))
                        .then(Commands.literal("shattered_core").executes(ctx -> applyShatteredCore(ctx.getSource()))))
                .then(Commands.literal("breakthrough").executes(ctx -> breakthrough(ctx.getSource()))));
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
                cultivation.getTribRes()), false));
        return 1;
    }

    private static int showRealm(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> {
            double chance = BreakthroughService.preview(player, cultivation).chance();
            source.sendSuccess(() -> Component.translatable(
                    "command.seeking_immortals.realm", cultivation.getRealm().getDisplayName(), cultivation.getStage().getDisplayName(), cultivation.getAgeYears(), cultivation.getLifespanYears(), cultivation.getRemainingLifespanYears(), Math.round(chance * 10000.0D) / 100.0D), false);
        });
        return 1;
    }

    private static int showRoot(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationHelper.get(player).ifPresent(cultivation -> source.sendSuccess(() -> Component.translatable(
                "command.seeking_immortals.root",
                cultivation.getSpiritualRoot().getDisplayName(),
                cultivation.getSpiritualRootAttributeNames(),
                cultivation.getSpiritualRootPurity(),
                cultivation.isSpiritualRootAwakened() ? "是" : "否",
                cultivation.getSpiritualRoot().getStarLevel(),
                cultivation.getSpecialPhysique().getDisplayName(),
                Math.round(cultivation.getBreakthroughMultiplier() * 100.0D) / 100.0D,
                Math.round(cultivation.getCultivationSpeedMultiplier() * 100.0D) / 100.0D,
                cultivation.getSpecialPhysique().hasDefect() ? "是" : "否"), false));
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

    private static int breakthrough(CommandSourceStack source) throws CommandSyntaxException {
        BreakthroughService.attempt(source.getPlayerOrException());
        return 1;
    }
}
