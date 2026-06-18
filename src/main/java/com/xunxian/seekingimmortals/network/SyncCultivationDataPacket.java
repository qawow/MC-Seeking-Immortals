package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.client.ClientCultivationData;
import com.xunxian.seekingimmortals.combat.CombatStats;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.MeditationFormula;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.item.SpiritStoneItem;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record SyncCultivationDataPacket(
        String realm,
        String stage,
        int spiritualPower,
        int maxSpiritualPower,
        int cultivationExp,
        int cultivation,
        int mana,
        int manaMax,
        int divSense,
        int bodyRef,
        int qiDevRisk,
        int tribRes,
        int lifespanYears,
        int ageYears,
        int remainingLifespanYears,
        String spiritualRoot,
        String spiritualRootAttributes,
        int spiritualRootPurity,
        boolean spiritualRootAwakened,
        boolean spiritualRootTested,
        String specialPhysique,
        int learnedTechniqueCount,
        boolean meditating,
        boolean severeInjury,
        int heartDemonLevel,
        boolean shatteredCore,
        int realmFallScars,
        double cultivationSpeedMultiplier,
        double rootCultivationSpeedCoefficient,
        double physiqueCultivationSpeedMultiplier,
        double baseAttack,
        double baseDefense,
        double critChance,
        double critDamage,
        double dodgeChance,
        double accuracy,
        int auraConcentration,
        String auraNature,
        double breakthroughChance,
        double breakthroughPillBonus,
        double breakthroughSpiritEyeBonus,
        double breakthroughTechniqueQualityBonus,
        double breakthroughObsessionBonus,
        int failedBreakthroughs,
        double meditationBasePerSecond,
        double meditationRootMultiplier,
        double meditationPhysiqueMultiplier,
        double meditationBonus,
        double meditationAuraMultiplier,
        double meditationTechniqueMultiplier,
        double meditationStoneBonus,
        double meditationTotalPerSecond) {
    public static SyncCultivationDataPacket from(ServerPlayer player, PlayerCultivation cultivation) {
        SpiritualAuraManager.AuraInfo auraInfo = SpiritualAuraManager.getAuraInfo(player.level(), player.blockPosition());
        CombatStats combatStats = new CombatStats(cultivation);
        PlayerCultivation.BreakthroughChanceBreakdown breakthrough = BreakthroughService.preview(player, cultivation);
        int stoneBonus = getMatchingPassiveBonus(player.getMainHandItem(), cultivation) >= getMatchingPassiveBonus(player.getOffhandItem(), cultivation)
                ? getMatchingPassiveBonus(player.getMainHandItem(), cultivation)
                : getMatchingPassiveBonus(player.getOffhandItem(), cultivation);
        ItemStack bonusStone = getMatchingPassiveBonus(player.getMainHandItem(), cultivation) >= getMatchingPassiveBonus(player.getOffhandItem(), cultivation)
                ? player.getMainHandItem()
                : player.getOffhandItem();
        MeditationFormula.Breakdown meditation = MeditationFormula.calculate(cultivation, auraInfo, isSittingOnMeditationCushion(player), cultivation.getPhysiqueCultivationSpeedMultiplier(), bonusStone, stoneBonus);
        return new SyncCultivationDataPacket(
                cultivation.getRealm().getDisplayName(),
                cultivation.getStage().getDisplayName(),
                cultivation.getSpiritualPower(),
                cultivation.getMaxSpiritualPower(),
                cultivation.getCultivationExp(),
                cultivation.getCultivation(),
                cultivation.getMana(),
                cultivation.getManaMax(),
                cultivation.getDivSense(),
                cultivation.getBodyRefinement(),
                cultivation.getQiDeviationRisk(),
                cultivation.getTribulationResistance(),
                cultivation.getLifespanYears(),
                cultivation.getAgeYears(),
                cultivation.getRemainingLifespanYears(),
                cultivation.getSpiritualRoot().getDisplayName(),
                cultivation.getSpiritualRootAttributeNames(),
                cultivation.getSpiritualRootPurity(),
                cultivation.isSpiritualRootAwakened(),
                cultivation.isSpiritualRootTested(),
                cultivation.getSpecialPhysique().getDisplayName(),
                cultivation.getLearnedTechniques().size(),
                cultivation.isMeditating(),
                cultivation.hasSevereInjury(),
                cultivation.getHeartDemonLevel(),
                cultivation.hasShatteredCore(),
                cultivation.getRealmFallScars(),
                cultivation.getCultivationSpeedMultiplier(),
                cultivation.getSpiritualRootCultivationSpeedCoefficient(),
                cultivation.getPhysiqueCultivationSpeedMultiplier(),
                combatStats.getBaseAttack(),
                combatStats.getBaseDefense(),
                combatStats.getCritChance(),
                combatStats.getCritDamage(),
                combatStats.getDodgeChance(),
                combatStats.getAccuracy(),
                auraInfo.concentration(),
                auraInfo.nature().getDisplayName(),
                breakthrough.chance(),
                breakthrough.pillBonus(),
                breakthrough.spiritEyeBonus(),
                breakthrough.techniqueQualityBonus(),
                breakthrough.obsessionBonus(),
                cultivation.getFailedBreakthroughs(),
                meditation.basePerSecond(),
                meditation.rootMultiplier(),
                meditation.physiqueMultiplier(),
                meditation.meditationBonus(),
                meditation.auraMultiplier(),
                meditation.techniqueMultiplier(),
                meditation.heldStoneBonus(),
                meditation.totalPerSecond());
    }

    public static void send(ServerPlayer player, PlayerCultivation cultivation) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(player, cultivation));
    }

    public static void encode(SyncCultivationDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.realm);
        buffer.writeUtf(packet.stage);
        buffer.writeVarInt(packet.spiritualPower);
        buffer.writeVarInt(packet.maxSpiritualPower);
        buffer.writeVarInt(packet.cultivationExp);
        buffer.writeVarInt(packet.cultivation);
        buffer.writeVarInt(packet.mana);
        buffer.writeVarInt(packet.manaMax);
        buffer.writeVarInt(packet.divSense);
        buffer.writeVarInt(packet.bodyRef);
        buffer.writeVarInt(packet.qiDevRisk);
        buffer.writeVarInt(packet.tribRes);
        buffer.writeVarInt(packet.lifespanYears);
        buffer.writeVarInt(packet.ageYears);
        buffer.writeVarInt(packet.remainingLifespanYears);
        buffer.writeUtf(packet.spiritualRoot);
        buffer.writeUtf(packet.spiritualRootAttributes);
        buffer.writeVarInt(packet.spiritualRootPurity);
        buffer.writeBoolean(packet.spiritualRootAwakened);
        buffer.writeBoolean(packet.spiritualRootTested);
        buffer.writeUtf(packet.specialPhysique);
        buffer.writeVarInt(packet.learnedTechniqueCount);
        buffer.writeBoolean(packet.meditating);
        buffer.writeBoolean(packet.severeInjury);
        buffer.writeVarInt(packet.heartDemonLevel);
        buffer.writeBoolean(packet.shatteredCore);
        buffer.writeVarInt(packet.realmFallScars);
        buffer.writeDouble(packet.cultivationSpeedMultiplier);
        buffer.writeDouble(packet.rootCultivationSpeedCoefficient);
        buffer.writeDouble(packet.physiqueCultivationSpeedMultiplier);
        buffer.writeDouble(packet.baseAttack);
        buffer.writeDouble(packet.baseDefense);
        buffer.writeDouble(packet.critChance);
        buffer.writeDouble(packet.critDamage);
        buffer.writeDouble(packet.dodgeChance);
        buffer.writeDouble(packet.accuracy);
        buffer.writeVarInt(packet.auraConcentration);
        buffer.writeUtf(packet.auraNature);
        buffer.writeDouble(packet.breakthroughChance);
        buffer.writeDouble(packet.breakthroughPillBonus);
        buffer.writeDouble(packet.breakthroughSpiritEyeBonus);
        buffer.writeDouble(packet.breakthroughTechniqueQualityBonus);
        buffer.writeDouble(packet.breakthroughObsessionBonus);
        buffer.writeVarInt(packet.failedBreakthroughs);
        buffer.writeDouble(packet.meditationBasePerSecond);
        buffer.writeDouble(packet.meditationRootMultiplier);
        buffer.writeDouble(packet.meditationPhysiqueMultiplier);
        buffer.writeDouble(packet.meditationBonus);
        buffer.writeDouble(packet.meditationAuraMultiplier);
        buffer.writeDouble(packet.meditationTechniqueMultiplier);
        buffer.writeDouble(packet.meditationStoneBonus);
        buffer.writeDouble(packet.meditationTotalPerSecond);
    }

    public static SyncCultivationDataPacket decode(FriendlyByteBuf buffer) {
        return new SyncCultivationDataPacket(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble());
    }

    public static void handle(SyncCultivationDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientCultivationData.setSnapshot(new ClientCultivationData.Snapshot(
                        packet.realm,
                        packet.stage,
                        packet.spiritualPower,
                        packet.maxSpiritualPower,
                        packet.cultivationExp,
                        packet.cultivation,
                        packet.mana,
                        packet.manaMax,
                        packet.divSense,
                        packet.bodyRef,
                        packet.qiDevRisk,
                        packet.tribRes,
                        packet.lifespanYears,
                        packet.ageYears,
                        packet.remainingLifespanYears,
                        packet.spiritualRoot,
                        packet.spiritualRootAttributes,
                        packet.spiritualRootPurity,
                        packet.spiritualRootAwakened,
                        packet.spiritualRootTested,
                        packet.specialPhysique,
                        packet.learnedTechniqueCount,
                        packet.meditating,
                        packet.severeInjury,
                        packet.heartDemonLevel,
                        packet.shatteredCore,
                        packet.realmFallScars,
                        packet.cultivationSpeedMultiplier,
                        packet.rootCultivationSpeedCoefficient,
                        packet.physiqueCultivationSpeedMultiplier,
                        packet.baseAttack,
                        packet.baseDefense,
                        packet.critChance,
                        packet.critDamage,
                        packet.dodgeChance,
                        packet.accuracy,
                        packet.auraConcentration,
                        packet.auraNature,
                        packet.breakthroughChance,
                        packet.breakthroughPillBonus,
                        packet.breakthroughSpiritEyeBonus,
                        packet.breakthroughTechniqueQualityBonus,
                        packet.breakthroughObsessionBonus,
                        packet.failedBreakthroughs))));
        context.setPacketHandled(true);
    }
}
