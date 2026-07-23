package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.item.ConsumableVfxOrchestrator;
import com.xunxian.seekingimmortals.item.ItemUsageGateService;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CatalogPillItem extends Item {
    public static final String FASTING_TICKS_KEY = "SeekingImmortalsFastingTicks";
    public static final String CLEAR_VOID_PET_CLARITY_KEY = "SeekingImmortalsClearVoidPetClarity";
    public static final String FORGET_DUST_TICKS_KEY = "SeekingImmortalsForgetDustTicks";
    public static final String APPEARANCE_FIXED_KEY = "SeekingImmortalsAppearanceFixed";
    public static final String MARROW_ADDICTION_KEY = "SeekingImmortalsMarrowAddiction";
    public static final String PRESSURE_RESIST_TICKS_KEY = "SeekingImmortalsPressureResistTicks";
    public static final String YIN_PROTECTION_TICKS_KEY = "SeekingImmortalsYinProtectionTicks";

    private final CatalogPillType type;
    private final PillQuality quality;

    public CatalogPillItem(Properties properties, CatalogPillType type) {
        this(properties, type, PillQuality.LOW);
    }

    public CatalogPillItem(Properties properties, CatalogPillType type, PillQuality quality) {
        super(properties);
        this.type = type;
        this.quality = quality;
    }

    public CatalogPillType type() {
        return type;
    }

    public PillQuality quality() {
        return quality;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean consumed = CultivationHelper.get(serverPlayer)
                    .map(cultivation -> consumeCatalogPill(serverPlayer, cultivation, stack))
                    .orElse(false);
            if (consumed) {
                if (!serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                ConsumableVfxOrchestrator.emitPill(serverPlayer, type.id(), quality);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    private boolean consumeCatalogPill(ServerPlayer player, PlayerCultivation cultivation, ItemStack stack) {
        if (isFutureSystemDisabledType()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_pill.future_system_disabled",
                    getDescription()), true);
            return false;
        }

        // 使用 ItemUsageGateService 进行境界门禁检查
        ItemUsageGateService.ItemRequirement requirement = ItemUsageGateService.ItemRequirement.realm(type.minRealm());
        ItemUsageGateService.GateResult gateCheck = ItemUsageGateService.canUse(player, requirement);
        if (!gateCheck.allowed()) {
            player.displayClientMessage(gateCheck.message(), true);
            return false;
        }

        BreakthroughService.HandBreakthroughAidResult aidResult =
                BreakthroughService.tryApplyHandConsumedBreakthroughAid(player, cultivation, stack, true);
        if (aidResult == BreakthroughService.HandBreakthroughAidResult.APPLIED) {
            return true;
        }
        if (aidResult == BreakthroughService.HandBreakthroughAidResult.BLOCKED) {
            return false;
        }

        double multiplier = effectiveMultiplier(cultivation);
        boolean success = switch (type) {
            case SPIRIT_GATHERING -> spiritGathering(player, cultivation, multiplier);
            case FIRE_ORIGIN -> fireOrigin(player, cultivation, multiplier);
            case ICE_FIRE -> iceFire(player, cultivation, multiplier);
            case MARROW_CLEANSING -> marrowCleansing(player, cultivation, multiplier);
            case BODY_TEMPERING -> bodyTempering(player, cultivation, multiplier);
            case ESSENCE_CONDENSING -> essenceCondensing(player, cultivation, multiplier);
            case SOUL_GATHERING -> soulGathering(player, cultivation, multiplier);
            case MARROW_REPAIR -> marrowRepair(player, cultivation, multiplier);
            case CLEAR_VOID -> clearVoid(player, multiplier);
            case FORGET_DUST -> forgetDust(player, multiplier);
            case APPEARANCE_FIXING -> appearanceFixing(player);
            case LONGEVITY -> longevity(player, cultivation, multiplier);
            case BLOOD_QI -> bloodQi(player, cultivation, multiplier);
            case RETURN_YANG_TRUE_WATER -> returnYangTrueWater(player, cultivation, multiplier);
            case MARROW_EXTRACTING -> marrowExtracting(player, cultivation, multiplier);
            case SOUL_BREAKING -> soulBreaking(player, multiplier);
            case POISON_DRAGON_PEARL -> poisonDragonPearl(player, cultivation, multiplier);
            case PRESSURE_RESIST -> pressureResist(player, multiplier);
            case SPIRIT_REALM_CONDENSE -> spiritRealmCondense(player, cultivation, multiplier);
            default -> false;
        };
        if (success) {
            SyncCultivationDataPacket.send(player, cultivation);
        }
        return success;
    }

    private boolean isFutureSystemDisabledType() {
        return type.futureSystemDisabled();
    }

    private boolean spiritGathering(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addSpiritualPower(scale(80, multiplier));
        cultivation.addCultivationExp(scale(30, multiplier));
        success(player);
        return true;
    }

    private boolean fireOrigin(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        if (cultivation.getSpiritualRootAttributes().contains(SpiritualRootAttribute.FIRE)) {
            cultivation.addCultivationExp(scale(120, multiplier));
            cultivation.addCultivationBoost(scaleTicks(24000, multiplier), 1.0D + 0.25D * multiplier);
        } else {
            cultivation.addCultivationExp(scale(35, multiplier));
            cultivation.addQiDeviationRisk(5);
        }
        success(player);
        return true;
    }

    private boolean iceFire(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        boolean fire = cultivation.getSpiritualRootAttributes().contains(SpiritualRootAttribute.FIRE);
        boolean ice = cultivation.getSpiritualRootAttributes().contains(SpiritualRootAttribute.ICE)
                || cultivation.getSpiritualRootAttributes().contains(SpiritualRootAttribute.WATER);
        if (fire && ice) {
            cultivation.addCultivationExp(scale(180, multiplier));
            cultivation.addCultivationBoost(scaleTicks(24000, multiplier), 1.0D + 0.35D * multiplier);
            cultivation.addQiDeviationRisk(4);
        } else if (fire || ice) {
            cultivation.addCultivationExp(scale(80, multiplier));
            cultivation.addQiDeviationRisk(12);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, scaleTicks(200, multiplier), 0));
        } else {
            cultivation.addCultivationExp(scale(40, multiplier));
            cultivation.addQiDeviationRisk(20);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, scaleTicks(400, multiplier), 0));
        }
        success(player);
        return true;
    }

    private boolean marrowCleansing(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.retestLingGen(player.getRandom(), true);
        cultivation.addBodyRefinement(scale(5, multiplier));
        if (player.getRandom().nextFloat() < 0.25F) {
            cultivation.addAgeYears(3);
        }
        success(player);
        return true;
    }

    private boolean bodyTempering(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addBodyRefinement(scale(4, multiplier));
        cultivation.addCultivationExp(scale(20, multiplier));
        success(player);
        return true;
    }

    private boolean essenceCondensing(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        if (cultivation.getRealm() != Realm.FOUNDATION_ESTABLISHMENT) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.catalog_pill.essence_condensing.realm"), true);
            return false;
        }
        if (!cultivation.isAtBreakthroughCap() || cultivation.getNextBreakthroughRealm() != Realm.CORE_FORMATION) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.catalog_pill.essence_condensing.not_at_cap"), true);
            return false;
        }
        if (cultivation.isBreakthroughAssisted()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.catalog_pill.breakthrough_exists"), true);
            return false;
        }
        cultivation.setBreakthroughPillBonus(quality.getBreakthroughBonus());
        success(player);
        return true;
    }

    private boolean soulGathering(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addDivineConsciousness(scale(12, multiplier));
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.BLINDNESS);
        success(player);
        return true;
    }

    private boolean marrowRepair(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        player.heal(scaleHealth(12.0F, multiplier));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, scaleTicks(240, multiplier), 1));
        cultivation.addAgeYears(2);
        cultivation.addBodyRefinement(-2);
        cultivation.addQiDeviationRisk(6);
        success(player);
        return true;
    }

    private boolean clearVoid(ServerPlayer player, double multiplier) {
        player.getPersistentData().putInt(CLEAR_VOID_PET_CLARITY_KEY, scaleTicks(6000, multiplier));
        player.removeEffect(MobEffects.CONFUSION);
        success(player);
        return true;
    }

    private boolean forgetDust(ServerPlayer player, double multiplier) {
        player.getPersistentData().putInt(FORGET_DUST_TICKS_KEY, scaleTicks(2400, multiplier));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, scaleTicks(2400, multiplier), 0));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, scaleTicks(200, multiplier), 0));
        success(player);
        return true;
    }

    private boolean appearanceFixing(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(APPEARANCE_FIXED_KEY)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.catalog_pill.appearance_fixed_exists"), true);
            return false;
        }
        data.putBoolean(APPEARANCE_FIXED_KEY, true);
        success(player);
        return true;
    }

    private boolean longevity(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addLifespanYears(scale(500, multiplier));
        cultivation.addQiDeviationRisk(5);
        success(player);
        return true;
    }

    private boolean bloodQi(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addLifespanYears(scale(40 + player.getRandom().nextInt(31), multiplier));
        cultivation.addQiDeviationRisk(8);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, scaleTicks(400, multiplier), 0));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, scaleTicks(160, multiplier), 0));
        success(player);
        return true;
    }

    private boolean returnYangTrueWater(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        if (cultivation.hasUsedReturnYangTrueWater()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.catalog_pill.return_yang_used"), true);
            return false;
        }
        cultivation.setUsedReturnYangTrueWater(true);
        cultivation.addLifespanYears(scale(300, multiplier));
        player.heal(player.getMaxHealth());
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, scaleTicks(600, multiplier), 2));
        success(player);
        return true;
    }

    private boolean marrowExtracting(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        CompoundTag data = player.getPersistentData();
        data.putInt(MARROW_ADDICTION_KEY, data.getInt(MARROW_ADDICTION_KEY) + 1);
        cultivation.addBodyRefinement(scale(10, multiplier));
        cultivation.addQiDeviationRisk(15);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, scaleTicks(1200, multiplier), 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, scaleTicks(1200, multiplier), 1));
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, scaleTicks(120, multiplier), 0));
        success(player);
        return true;
    }

    private boolean soulBreaking(ServerPlayer player, double multiplier) {
        player.addEffect(new MobEffectInstance(MobEffects.POISON, scaleTicks(1200, multiplier), 2));
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, scaleTicks(600, multiplier), 1));
        player.hurt(player.damageSources().magic(), scaleHealth(8.0F, multiplier));
        success(player);
        return true;
    }

    private boolean poisonDragonPearl(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addQiDeviationRisk(25);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, scaleTicks(1800, multiplier), 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, scaleTicks(1800, multiplier), 1));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, scaleTicks(400, multiplier), 1));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, scaleTicks(300, multiplier), 0));
        success(player);
        return true;
    }

    private boolean pressureResist(ServerPlayer player, double multiplier) {
        int duration = scaleTicks(24000, multiplier);
        CompoundTag data = player.getPersistentData();
        data.putInt(PRESSURE_RESIST_TICKS_KEY, Math.max(data.getInt(PRESSURE_RESIST_TICKS_KEY), duration));
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.CONFUSION);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, scaleTicks(1200, multiplier), 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, scaleTicks(600, multiplier), 0));
        success(player);
        return true;
    }

    private boolean spiritRealmCondense(ServerPlayer player, PlayerCultivation cultivation, double multiplier) {
        cultivation.addCultivationExp(scale(900, multiplier));
        cultivation.addDivineConsciousness(scale(30, multiplier));
        cultivation.addCultivationBoost(scaleTicks(36000, multiplier), 1.0D + 0.45D * multiplier);
        cultivation.addQiDeviationRisk(3);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, scaleTicks(300, multiplier), 1));
        success(player);
        return true;
    }

    private void success(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.seeking_immortals.catalog_pill.success", getDescription()), true);
    }

    public static boolean hasPressureResist(Player player) {
        return pressureResistTicks(player) > 0;
    }

    public static int pressureResistTicks(Player player) {
        return player == null ? 0 : player.getPersistentData().getInt(PRESSURE_RESIST_TICKS_KEY);
    }

    public static boolean hasYinProtection(Player player) {
        return yinProtectionTicks(player) > 0;
    }

    public static int yinProtectionTicks(Player player) {
        return player == null ? 0 : player.getPersistentData().getInt(YIN_PROTECTION_TICKS_KEY);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(PlayerDisplayText.translatedOr(
                        "tooltip.seeking_immortals.catalog_pill.quality." + quality.designId(),
                        "tooltip.seeking_immortals.catalog_pill.quality.unknown").copy()
                .withStyle(style -> style.withColor(quality.getColor())));
        tooltip.add(PlayerDisplayText.translatedOr(
                        "tooltip.seeking_immortals.catalog_pill." + type.id(),
                        "tooltip.seeking_immortals.catalog_pill.unknown").copy()
                .withStyle(ChatFormatting.GRAY));

        // 使用 ItemUsageGateService 显示境界要求
        ItemUsageGateService.ItemRequirement requirement = ItemUsageGateService.ItemRequirement.realm(type.minRealm());
        ItemUsageGateService.appendRequirementTooltip(stack, tooltip, requirement);
    }

    private double effectiveMultiplier(PlayerCultivation cultivation) {
        return quality.getEffectMultiplier() * cultivation.getPillAbsorptionMultiplier();
    }

    private static int scale(double base, double multiplier) {
        return Math.max(1, (int)Math.round(base * multiplier));
    }

    private static int scaleTicks(int baseTicks, double multiplier) {
        return Math.max(20, scale(baseTicks, multiplier));
    }

    private static float scaleHealth(float base, double multiplier) {
        return Math.max(1.0F, (float)(base * multiplier));
    }
}
