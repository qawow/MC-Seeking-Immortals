package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.TalismanConsumePolicy;
import com.xunxian.seekingimmortals.skill.TechniqueGateService;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.SkillEffect;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxOrchestrator;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
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

                if (techniqueOpt.isPresent()) {
                    var technique = techniqueOpt.get();
                    // Wave466: realm/sect gate before cost or execute.
                    TechniqueGateService.GateResult gate = TechniqueGateService.canCast(player, cultivation, technique);
                    if (!gate.allowed()) {
                        if (gate.messageKey() != null && !gate.messageKey().isBlank()) {
                            player.displayClientMessage(Component.translatable(gate.messageKey(), gate.args()), true);
                        } else {
                            player.displayClientMessage(
                                    Component.translatable("message.seeking_immortals.technique_release.effect_failed"), true);
                        }
                        SyncCultivationDataPacket.send(player, cultivation);
                        SyncLearnedTechniquesPacket.send(player, cultivation);
                        return;
                    }
                    SkillType skillType = com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver
                            .resolveSkillType(technique);
                    SkillEffect effect = com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver
                            .resolve(technique);
                    CultivationSkill skill = null;
                    if (skillType != null) {
                        skill = cultivation.getSkill(skillType);
                        if (skill == null || !skill.isUnlocked()) {
                            // Auto-unlock mapped SkillType when the technique itself is already learned.
                            if (cultivation.hasLearnedTechnique(techniqueId)) {
                                cultivation.unlockSkillForQuest(skillType);
                                skill = cultivation.getSkill(skillType);
                            }
                        }
                    }
                    if (skill == null || !skill.isUnlocked()) {
                        // M02: abstract effect path for corpus techniques without SkillType.
                        skill = com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver.virtualSkill();
                    }
                    // H4: effect 未注册 → 拒绝释放，不扣费不冷却
                    if (effect == null) {
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
                    // Wave490: multi-cast special skill soft-reduces technique cooldown.
                    double multiScale = com.xunxian.seekingimmortals.skill.SpecialSkillService
                            .multiCastCooldownScale(player);
                    if (multiScale < 0.999D) {
                        cooldownTicks = Math.max(5, (int) Math.round(cooldownTicks * multiScale));
                    }
                    // H5: 只检查不扣；真正扣费放在过阶风险检查之后（避免走火入魔触发时白扣费）
                    if (cultivation.getSpiritualPower() < cost) {
                        player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                    // Apply over-tier risk only after every non-mutating cast gate has accepted the action.
                    Realm techniqueRealm = estimateTechniqueRealm(technique);
                    int realmDiff = techniqueRealm.ordinal() - cultivation.getRealm().ordinal();
                    if (realmDiff >= 2) {
                        cultivation.addQiDeviationRisk(5);
                        player.displayClientMessage(Component.translatable(
                                "message.seeking_immortals.technique_release.realm_too_high",
                                realmDiff, cultivation.getQiDeviationRisk()), true);
                        if (BreakthroughService.tryTriggerQiDeviation(player, cultivation,
                                "message.seeking_immortals.qi_deviation.trigger.over_tier_technique")) {
                            SyncCultivationDataPacket.send(player, cultivation);
                            return;
                        }
                    }
                    // H5: 原子性扣费——在 execute 之前扣除，避免 execute 内部消耗灵力后
                    // consume 失败导致已施法却不设冷却的免费重复施法
                    if (!cultivation.consumeSpiritualPower(cost)) {
                        player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                    // Soft talisman_consume policy for CAST_* / *talisman* techniques.
                    // Reserve first; refund if effect fails so failed casts never eat talismans.
                    TalismanConsumePolicy.Reservation talismanReservation =
                            TalismanConsumePolicy.tryReserve(player, technique.id(), skillType);
                    if (!talismanReservation.allowed()) {
                        cultivation.addSpiritualPower(cost); // 符箓预留失败，退还灵力
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                    SkillContext ctx = SkillContext.builder()
                            .level(player.serverLevel())
                            .position(player.position())
                            .lookDirection(player.getLookAngle())
                            .build();
                    Vec3 beforeCast = player.position();
                    TechniqueVfxPacket.CaptureScope vfxCapture = TechniqueVfxPacket.captureSynchronousIntents();
                    boolean executed;
                    try {
                        executed = effect.execute(player, cultivation, skill, ctx);
                    } finally {
                        vfxCapture.close();
                    }
                    if (!executed) {
                        talismanReservation.refund(player);
                        cultivation.addSpiritualPower(cost); // execute 失败，退还灵力
                        player.displayClientMessage(
                                Component.translatable("message.seeking_immortals.technique_release.effect_failed"), true);
                        SyncCultivationDataPacket.send(player, cultivation);
                        return;
                    }
                    talismanReservation.commit(player);
                    TechniqueVfxOrchestrator.emitSuccessfulCast(
                            player, technique, skillType, beforeCast, vfxCapture.packets(), false);
                    if (skillType != null) {
                        cultivation.addSkillProficiency(skillType, 10);
                    }
                    // Wave490/492: multi-cast practice + honest dual-cast extras.
                    if (cultivation.hasSkill(com.xunxian.seekingimmortals.skill.SkillType.MULTI_CASTING)) {
                        if (player.tickCount % 3 == 0) {
                            com.xunxian.seekingimmortals.skill.SpecialSkillService.practiceMultiCast(player);
                        }
                        int dualCount = tryDualCast(player, cultivation, packet.slot, gameTime, multiScale);
                        if (dualCount > 0) {
                            player.displayClientMessage(Component.translatable(
                                    "message.seeking_immortals.technique_release.dual_cast", dualCount), false);
                        }
                    }
                    effectExecuted = true;
                }

                // H4: 仅 effect 真正执行成功才设冷却 + 成功提示
                if (effectExecuted) {
                    cultivation.setTechniqueCooldown(techniqueId, gameTime + cooldownTicks);
                    SyncCultivationDataPacket.send(player, cultivation);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    SyncSkillDataPacket.send(player, cultivation);
                    var technique = techniqueOpt.get();
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.success",
                            packet.slot + 1,
                            techniqueDisplay(technique),
                            cost), false);
                }
            });
        });
        context.setPacketHandled(true);
    }

    private static Component techniqueDisplay(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return Component.translatable("text.seeking_immortals.unknown_technique");
        }
        return PlayerDisplayText.safeCatalogLiteral(technique.name(), "text.seeking_immortals.unknown_technique");
    }

    /**
     * Wave492: after primary cast succeeds, cast up to N other slotted techniques free of packet,
     * still gated by learn/cooldown/cost/effect availability. Shared multi-cast CD scale applies.
     */
    private static int tryDualCast(ServerPlayer player, com.xunxian.seekingimmortals.cultivation.PlayerCultivation cultivation,
                                   int primarySlot, long gameTime, double multiScale) {
        int extra = com.xunxian.seekingimmortals.skill.SpecialSkillService.dualCastExtraSlots(player);
        if (extra <= 0) {
            return 0;
        }
        int fired = 0;
        for (int slot = 0; slot < com.xunxian.seekingimmortals.cultivation.PlayerCultivation.TECHNIQUE_SLOT_COUNT && fired < extra; slot++) {
            if (slot == primarySlot) {
                continue;
            }
            String techniqueId = cultivation.getTechniqueSlot(slot);
            if (techniqueId == null || techniqueId.isBlank() || !cultivation.hasLearnedTechnique(techniqueId)) {
                continue;
            }
            if (cultivation.getTechniqueCooldownUntilTick(techniqueId) > gameTime) {
                continue;
            }
            var techniqueOpt = TechniqueDataManager.getTechnique(player.getServer(), techniqueId);
            if (techniqueOpt.isEmpty()) {
                continue;
            }
            var technique = techniqueOpt.get();
            TechniqueGateService.GateResult gate = TechniqueGateService.canCast(player, cultivation, technique);
            if (!gate.allowed()) {
                continue;
            }
            SkillType skillType = com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver
                    .resolveSkillType(technique);
            SkillEffect effect = com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver
                    .resolve(technique);
            CultivationSkill skill = null;
            if (skillType != null) {
                skill = cultivation.getSkill(skillType);
                if ((skill == null || !skill.isUnlocked()) && cultivation.hasLearnedTechnique(techniqueId)) {
                    cultivation.unlockSkillForQuest(skillType);
                    skill = cultivation.getSkill(skillType);
                }
            }
            if (skill == null || !skill.isUnlocked()) {
                skill = com.xunxian.seekingimmortals.skill.effect.AbstractTechniqueEffectResolver.virtualSkill();
            }
            if (effect == null || !effect.canExecute(player, cultivation)) {
                continue;
            }
            int cost = effect.getSpiritualPowerCost(skill.getLevel());
            // Dual-cast secondary arts cost half, min 1.
            cost = Math.max(1, cost / 2);
            // 原子性扣费：execute 之前扣除，避免 execute 后 consume 失败导致免费施法
            if (!cultivation.consumeSpiritualPower(cost)) {
                continue;
            }
            TalismanConsumePolicy.Reservation talismanReservation =
                    TalismanConsumePolicy.tryReserve(player, technique.id(), skillType);
            if (!talismanReservation.allowed()) {
                cultivation.addSpiritualPower(cost); // 符箓预留失败，退还灵力
                continue;
            }
            SkillContext ctx = SkillContext.builder()
                    .level(player.serverLevel())
                    .position(player.position())
                    .lookDirection(player.getLookAngle())
                    .build();
            Vec3 beforeCast = player.position();
            TechniqueVfxPacket.CaptureScope vfxCapture = TechniqueVfxPacket.captureSynchronousIntents();
            boolean executed;
            try {
                executed = effect.execute(player, cultivation, skill, ctx);
            } finally {
                vfxCapture.close();
            }
            if (!executed) {
                talismanReservation.refund(player);
                cultivation.addSpiritualPower(cost); // execute 失败，退还灵力
                continue;
            }
            talismanReservation.commit(player);
            TechniqueVfxOrchestrator.emitSuccessfulCast(
                    player, technique, skillType, beforeCast, vfxCapture.packets(), true);
            int cooldownTicks = effect.getCooldownTicks(skill.getLevel());
            if (multiScale < 0.999D) {
                cooldownTicks = Math.max(5, (int) Math.round(cooldownTicks * multiScale));
            }
            // Dual-cast secondary CD is slightly longer to prevent spam.
            cooldownTicks = Math.max(cooldownTicks, (int) Math.round(cooldownTicks * 1.15D));
            cultivation.setTechniqueCooldown(techniqueId, gameTime + cooldownTicks);
            if (skillType != null) {
                cultivation.addSkillProficiency(skillType, 6);
            }
            fired++;
        }
        return fired;
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
