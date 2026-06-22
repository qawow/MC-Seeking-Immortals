package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import com.xunxian.seekingimmortals.skill.effect.SkillEffectRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ReleaseTechniquePacket(int slot) {
    private static final int SLOT_COUNT = 7;
    private static final int DEFAULT_COOLDOWN_TICKS = 5 * 20;

    public static void encode(ReleaseTechniquePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
    }

    public static ReleaseTechniquePacket decode(FriendlyByteBuf buffer) {
        return new ReleaseTechniquePacket(buffer.readVarInt());
    }

    public static void handle(ReleaseTechniquePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (packet.slot < 0 || packet.slot >= SLOT_COUNT) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.invalid_slot"), true);
                return;
            }

            CultivationHelper.get(player).ifPresent(cultivation -> {
                String techniqueId = cultivation.getTechniqueSlot(packet.slot);
                if (techniqueId.isBlank()) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.empty_slot", packet.slot + 1), true);
                    return;
                }

                if (!cultivation.hasLearnedTechnique(techniqueId)) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.not_learned"), true);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    return;
                }

                // M9: 使用全局 overworld gameTime 替代 per-dimension gameTime，跨维度冷却一致
                long gameTime = player.getServer().overworld().getGameTime();
                long cooldownUntilTick = cultivation.getTechniqueCooldownUntilTick(techniqueId);
                if (cooldownUntilTick > gameTime) {
                    int remainingSeconds = (int)Math.ceil((cooldownUntilTick - gameTime) / 20.0D);
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.cooldown", remainingSeconds), true);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    return;
                }

                var techniqueOpt = TechniqueDataManager.getTechnique(player.getServer(), techniqueId);
                int cooldownTicks = DEFAULT_COOLDOWN_TICKS;
                int cost = estimateCost(player, techniqueId);
                boolean effectExecuted = false;

                // 走火入魔风险：使用超出当前境界 2 级以上的功法
                if (techniqueOpt.isPresent()) {
                    Realm techniqueRealm = estimateTechniqueRealm(techniqueOpt.get());
                    int realmDiff = techniqueRealm.ordinal() - cultivation.getRealm().ordinal();
                    if (realmDiff >= 2) {
                        cultivation.addQiDeviationRisk(5);
                        player.displayClientMessage(Component.translatable(
                                "message.seeking_immortals.technique_release.realm_too_high",
                                realmDiff, cultivation.getQiDeviationRisk()), true);
                        if (BreakthroughService.tryTriggerQiDeviation(player, cultivation, "message.seeking_immortals.qi_deviation.trigger.over_tier_technique")) {
                            return;
                        }
                    }
                }

                if (techniqueOpt.isPresent()) {
                    var technique = techniqueOpt.get();
                    SkillType skillType = SkillEffectRegistry.byDisplayName(technique.name());
                    SkillEffect effect = skillType == null ? null : SkillEffectRegistry.get(skillType);
                    CultivationSkill skill = skillType == null ? null : cultivation.getSkill(skillType);
                    // H4: effect 未注册或未解锁 → 拒绝释放，不扣费不冷却
                    if (effect == null || skill == null || !skill.isUnlocked()) {
                        player.displayClientMessage(
                                Component.translatable("message.seeking_immortals.technique_release.effect_unavailable"), true);
                        SyncLearnedTechniquesPacket.send(player, cultivation);
                        return;
                    }
                    if (!effect.canExecute(player, cultivation)) {
                        player.displayClientMessage(
                                Component.translatable("message.seeking_immortals.technique_release.effect_failed"), true);
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                    cost = effect.getSpiritualPowerCost(skill.getLevel());
                    cooldownTicks = effect.getCooldownTicks(skill.getLevel());
                    // H5: 只检查不扣，execute 成功后才扣
                    if (cultivation.getSpiritualPower() < cost) {
                        player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                    SkillContext ctx = SkillContext.builder()
                            .level(player.serverLevel())
                            .position(player.position())
                            .lookDirection(player.getLookAngle())
                            .build();
                    if (!effect.execute(player, cultivation, skill, ctx)) {
                        player.displayClientMessage(
                                Component.translatable("message.seeking_immortals.technique_release.effect_failed"), true);
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                    // H5: execute 成功后扣费
                    if (!cultivation.consumeSpiritualPower(cost)) {
                        return;
                    }
                    cultivation.addSkillProficiency(skillType, 10);
                    effectExecuted = true;
                }

                // H4: 仅 effect 真正执行成功才设冷却 + 成功提示
                if (effectExecuted) {
                    cultivation.setTechniqueCooldown(techniqueId, gameTime + cooldownTicks);
                    SyncCultivationDataPacket.send(player, cultivation);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    var technique = techniqueOpt.get();
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.success",
                            packet.slot + 1,
                            technique.name().isBlank() ? technique.id() : technique.name(),
                            cost), false);
                }
            });
        });
        context.setPacketHandled(true);
    }

    private static int estimateCost(ServerPlayer player, String techniqueId) {
        return TechniqueDataManager.getTechnique(player.getServer(), techniqueId)
                .map(TechniqueDataManager.TechniqueEntry::cost)
                .orElse(15);
    }

    /**
     * 根据功法的 source/id 文本推断功法对应的境界等级。
     * 用于走火入魔风险判定：使用超出当前境界 2 级以上的功法会增加风险。
     */
    private static Realm estimateTechniqueRealm(TechniqueDataManager.TechniqueEntry technique) {
        return technique.requiredRealm();
    }
}
