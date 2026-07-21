package com.xunxian.seekingimmortals.network;

import com.xunxian.seekingimmortals.client.ClientCultivationData;
import com.xunxian.seekingimmortals.combat.CombatStats;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.MeditationFormula;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import com.xunxian.seekingimmortals.event.ModEvents;
import com.xunxian.seekingimmortals.item.SpiritStoneItem;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
        long cultivationMax,
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
        String goldCoreGrade,
        int goldCoreScore,
        boolean completeFiveElements,
        boolean tribulationActive,
        String tribulationTargetRealm,
        int tribulationCurrentStrike,
        int tribulationTotalStrikes,
        int tribulationNextStrikeTicks,
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
        double movementSpeedScale,
        double movementSpeedBonus,
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
        double meditationTotalPerSecond,
        // M01: 路线/种族/体质 id（登录同步，供下游门槛与 UI）
        String constitutionId,
        String cultivationPathId,
        String playableRaceId,
        String ghostPathStageId) {
    private static final int REALM_TEXT_LIMIT = 64;
    private static final int STAGE_TEXT_LIMIT = 64;
    private static final int ROOT_TEXT_LIMIT = 128;
    private static final int ROOT_ATTRIBUTES_TEXT_LIMIT = 256;
    private static final int PHYSIQUE_TEXT_LIMIT = 64;
    private static final int GOLD_CORE_TEXT_LIMIT = 64;
    private static final int TRIBULATION_TARGET_TEXT_LIMIT = 64;
    private static final int AURA_NATURE_TEXT_LIMIT = 64;
    private static final int CONSTITUTION_TEXT_LIMIT = 64;
    private static final int PATH_TEXT_LIMIT = 64;
    private static final int RACE_TEXT_LIMIT = 64;
    private static final int GHOST_STAGE_TEXT_LIMIT = 64;

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
        MeditationFormula.Breakdown meditation = MeditationFormula.calculate(
                cultivation, auraInfo, isSittingOnMeditationCushion(player),
                ModEvents.getBestMeditationTechniqueMultiplier(player, cultivation), bonusStone, stoneBonus);
        return new SyncCultivationDataPacket(
                cultivation.getRealm().getDisplayName(),
                cultivation.getStage().getDisplayName(),
                cultivation.getSpiritualPower(),
                cultivation.getMaxSpiritualPower(),
                cultivation.getCultivationExp(),
                cultivation.getCultivation(),
                cultivation.getCultivationMax(),
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
                cultivation.getGoldCoreGradeName(),
                cultivation.getGoldCoreScore(),
                cultivation.hasCompleteFiveElements(),
                cultivation.isTribulationActive(),
                cultivation.getTribulationTargetRealm().getDisplayName(),
                cultivation.getTribulationCurrentStrike(),
                cultivation.getTribulationTotalStrikes(),
                cultivation.getTribulationNextStrikeTicks(),
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
                cultivation.getMovementSpeedScale(),
                cultivation.getEffectiveMovementSpeedBonus(),
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
                meditation.totalPerSecond(),
                cultivation.getConstitutionId(),
                cultivation.getCultivationPathId(),
                cultivation.getPlayableRaceId(),
                cultivation.getGhostPathStageId());
    }

    public static void send(ServerPlayer player, PlayerCultivation cultivation) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(player, cultivation));
    }

    /**
     * M11：按 modified-UTF8 字节数截断同步文本，与 {@link FriendlyByteBuf#writeUtf(String, int)} 的字节上限口径一致，
     * 避免异常长值（尤其中文，每字符 ~3 字节）撑破 writeUtf 上限抛 EncoderException。正常服务端生成值远小于上限。
     */
    private static String cap(String s, int maxLen) {
        if (s == null) return "";
        int bytes = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // modified-UTF8（与 Netty writeUtf 一致）：U+0000=2、其余 BMP<=3、代理对=6。
            int cost = (c >= 0x0001 && c <= 0x007F) ? 1 : (c <= 0x07FF) ? 2 : 3;
            if (bytes + cost > maxLen) {
                return s.substring(0, i);
            }
            bytes += cost;
        }
        return s;
    }

    public static void encode(SyncCultivationDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(cap(packet.realm, REALM_TEXT_LIMIT), REALM_TEXT_LIMIT);
        buffer.writeUtf(cap(packet.stage, STAGE_TEXT_LIMIT), STAGE_TEXT_LIMIT);
        buffer.writeVarInt(packet.spiritualPower);
        buffer.writeVarInt(packet.maxSpiritualPower);
        buffer.writeVarInt(packet.cultivationExp);
        buffer.writeVarInt(packet.cultivation);
        buffer.writeLong(packet.cultivationMax);
        buffer.writeVarInt(packet.mana);
        buffer.writeVarInt(packet.manaMax);
        buffer.writeVarInt(packet.divSense);
        buffer.writeVarInt(packet.bodyRef);
        buffer.writeVarInt(packet.qiDevRisk);
        buffer.writeVarInt(packet.tribRes);
        buffer.writeVarInt(packet.lifespanYears);
        buffer.writeVarInt(packet.ageYears);
        buffer.writeVarInt(packet.remainingLifespanYears);
        buffer.writeUtf(cap(packet.spiritualRoot, ROOT_TEXT_LIMIT), ROOT_TEXT_LIMIT);
        buffer.writeUtf(cap(packet.spiritualRootAttributes, ROOT_ATTRIBUTES_TEXT_LIMIT), ROOT_ATTRIBUTES_TEXT_LIMIT);
        buffer.writeVarInt(packet.spiritualRootPurity);
        buffer.writeBoolean(packet.spiritualRootAwakened);
        buffer.writeBoolean(packet.spiritualRootTested);
        buffer.writeUtf(cap(packet.specialPhysique, PHYSIQUE_TEXT_LIMIT), PHYSIQUE_TEXT_LIMIT);
        buffer.writeUtf(cap(packet.goldCoreGrade, GOLD_CORE_TEXT_LIMIT), GOLD_CORE_TEXT_LIMIT);
        buffer.writeVarInt(packet.goldCoreScore);
        buffer.writeBoolean(packet.completeFiveElements);
        buffer.writeBoolean(packet.tribulationActive);
        buffer.writeUtf(cap(packet.tribulationTargetRealm, TRIBULATION_TARGET_TEXT_LIMIT), TRIBULATION_TARGET_TEXT_LIMIT);
        buffer.writeVarInt(packet.tribulationCurrentStrike);
        buffer.writeVarInt(packet.tribulationTotalStrikes);
        buffer.writeVarInt(packet.tribulationNextStrikeTicks);
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
        buffer.writeDouble(packet.movementSpeedScale);
        buffer.writeDouble(packet.movementSpeedBonus);
        buffer.writeVarInt(packet.auraConcentration);
        buffer.writeUtf(cap(packet.auraNature, AURA_NATURE_TEXT_LIMIT), AURA_NATURE_TEXT_LIMIT);
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
        buffer.writeUtf(cap(packet.constitutionId, CONSTITUTION_TEXT_LIMIT), CONSTITUTION_TEXT_LIMIT);
        buffer.writeUtf(cap(packet.cultivationPathId, PATH_TEXT_LIMIT), PATH_TEXT_LIMIT);
        buffer.writeUtf(cap(packet.playableRaceId, RACE_TEXT_LIMIT), RACE_TEXT_LIMIT);
        buffer.writeUtf(cap(packet.ghostPathStageId, GHOST_STAGE_TEXT_LIMIT), GHOST_STAGE_TEXT_LIMIT);
    }

    public static SyncCultivationDataPacket decode(FriendlyByteBuf buffer) {
        return new SyncCultivationDataPacket(
                buffer.readUtf(REALM_TEXT_LIMIT),
                buffer.readUtf(STAGE_TEXT_LIMIT),
                buffer.readVarInt(), // spiritualPower
                buffer.readVarInt(), // maxSpiritualPower
                buffer.readVarInt(), // cultivationExp
                buffer.readVarInt(), // cultivation
                buffer.readLong(),   // cultivationMax
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(ROOT_TEXT_LIMIT),
                buffer.readUtf(ROOT_ATTRIBUTES_TEXT_LIMIT),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(PHYSIQUE_TEXT_LIMIT),
                buffer.readUtf(GOLD_CORE_TEXT_LIMIT),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(TRIBULATION_TARGET_TEXT_LIMIT),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
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
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readUtf(AURA_NATURE_TEXT_LIMIT),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readUtf(CONSTITUTION_TEXT_LIMIT),
                buffer.readUtf(PATH_TEXT_LIMIT),
                buffer.readUtf(RACE_TEXT_LIMIT),
                buffer.readUtf(GHOST_STAGE_TEXT_LIMIT));
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
                        packet.cultivationMax,
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
                        packet.goldCoreGrade,
                        packet.goldCoreScore,
                        packet.completeFiveElements,
                        packet.tribulationActive,
                        packet.tribulationTargetRealm,
                        packet.tribulationCurrentStrike,
                        packet.tribulationTotalStrikes,
                        packet.tribulationNextStrikeTicks,
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
                        packet.movementSpeedScale,
                        packet.movementSpeedBonus,
                        packet.auraConcentration,
                        packet.auraNature,
                        packet.breakthroughChance,
                        packet.breakthroughPillBonus,
                        packet.breakthroughSpiritEyeBonus,
                        packet.breakthroughTechniqueQualityBonus,
                        packet.breakthroughObsessionBonus,
                        packet.failedBreakthroughs,
                        packet.meditationBasePerSecond,
                        packet.meditationRootMultiplier,
                        packet.meditationPhysiqueMultiplier,
                        packet.meditationBonus,
                        packet.meditationAuraMultiplier,
                        packet.meditationTechniqueMultiplier,
                        packet.meditationStoneBonus,
                        packet.meditationTotalPerSecond,
                        packet.constitutionId,
                        packet.cultivationPathId,
                        packet.playableRaceId,
                        packet.ghostPathStageId))));
        context.setPacketHandled(true);
    }

    private static boolean isSittingOnMeditationCushion(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof CushionSeatEntity seat) {
            return player.level().getBlockState(seat.getCushionPos()).is(ModBlocks.MEDITATION_CUSHION.get());
        }
        return false;
    }

    private static int getMatchingPassiveBonus(ItemStack stack, PlayerCultivation cultivation) {
        return SpiritStoneItem.getMatchingPassiveBonus(stack, cultivation.getSpiritualRootAttribute());
    }
}
