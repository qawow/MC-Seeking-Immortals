package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
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

import java.util.Locale;
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

                long gameTime = player.serverLevel().getGameTime();
                long cooldownUntilTick = cultivation.getTechniqueCooldownUntilTick(techniqueId);
                if (cooldownUntilTick > gameTime) {
                    int remainingSeconds = (int)Math.ceil((cooldownUntilTick - gameTime) / 20.0D);
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.cooldown", remainingSeconds), true);
                    SyncLearnedTechniquesPacket.send(player, cultivation);
                    return;
                }

                int cost = estimateCost(player, techniqueId);
                if (!cultivation.consumeSpiritualPower(cost)) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                    SyncCultivationDataPacket.send(player, cultivation);
                    return;
                }

                var techniqueOpt = TechniqueDataManager.getTechnique(player.getServer(), techniqueId);
                int cooldownTicks = DEFAULT_COOLDOWN_TICKS;
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
                    }
                }

                if (techniqueOpt.isPresent()) {
                    var technique = techniqueOpt.get();
                    SkillType skillType = SkillEffectRegistry.byDisplayName(technique.name());
                    if (skillType != null) {
                        CultivationSkill skill = cultivation.getSkill(skillType);
                        SkillEffect effect = SkillEffectRegistry.get(skillType);
                        if (skill != null && skill.isUnlocked() && effect != null) {
                            cooldownTicks = effect.getCooldownTicks(skill.getLevel());
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
                            cultivation.addSkillProficiency(skillType, 10);
                            effectExecuted = true;
                        }
                    }
                }

                cultivation.setTechniqueCooldown(techniqueId, gameTime + cooldownTicks);
                SyncCultivationDataPacket.send(player, cultivation);
                SyncLearnedTechniquesPacket.send(player, cultivation);

                if (effectExecuted) {
                    var technique = techniqueOpt.get();
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.success",
                            packet.slot + 1,
                            technique.name().isBlank() ? technique.id() : technique.name(),
                            cost), false);
                } else {
                    techniqueOpt.ifPresentOrElse(technique ->
                                    player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.success",
                                            packet.slot + 1,
                                            technique.name().isBlank() ? technique.id() : technique.name(),
                                            cost), false),
                            () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.success",
                                    packet.slot + 1,
                                    techniqueId,
                                    cost), false));
                }
            });
        });
        context.setPacketHandled(true);
    }

    private static int estimateCost(ServerPlayer player, String techniqueId) {
        return TechniqueDataManager.getTechnique(player.getServer(), techniqueId)
                .map(technique -> {
                    String text = (technique.id() + " " + technique.source() + " " + technique.attribute()).toLowerCase(java.util.Locale.ROOT);
                    if (text.contains("formation") || text.contains("sword") || text.contains("阵") || text.contains("剑")) return 35;
                    if (text.contains("secret") || text.contains("divine") || text.contains("秘") || text.contains("神通")) return 30;
                    if (text.contains("talisman") || text.contains("符")) return 12;
                    if (text.contains("utility") || text.contains("通用")) return 8;
                    return 15;
                })
                .orElse(15);
    }

    /**
     * 根据功法的 source/id 文本推断功法对应的境界等级。
     * 用于走火入魔风险判定：使用超出当前境界 2 级以上的功法会增加风险。
     */
    private static Realm estimateTechniqueRealm(TechniqueDataManager.TechniqueEntry technique) {
        String id = technique.id().toLowerCase(Locale.ROOT);
        String source = technique.source().toLowerCase(Locale.ROOT);
        if (containsAny(source, "天阶", "化神", "灵界", "古魔", "通天", "大衍", "元磁", "真魔")
                || containsAny(id, "spirit_transformation", "heaven", "void", "magnetic")) return Realm.SOUL_TRANSFORMATION;
        if (containsAny(source, "元婴", "古宝", "高级", "真灵")
                || containsAny(id, "nascent", "soul")) return Realm.NASCENT_SOUL;
        if (containsAny(source, "结丹", "金丹", "剑诀", "秘典")
                || containsAny(id, "core", "golden", "sword")) return Realm.CORE_FORMATION;
        if (containsAny(source, "筑基", "中阶", "阵法", "符宝")
                || containsAny(id, "foundation")) return Realm.FOUNDATION_ESTABLISHMENT;
        if (containsAny(source, "长春功", "低阶", "炼气")) return Realm.QI_REFINING;
        return Realm.QI_REFINING;
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
