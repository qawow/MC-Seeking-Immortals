package com.xunxian.seekingimmortals.event;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.command.SeekingImmortalsCommand;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.CultivationProvider;
import com.xunxian.seekingimmortals.cultivation.FlyingAuthority;
import com.xunxian.seekingimmortals.cultivation.MeditationFormula;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import com.xunxian.seekingimmortals.item.SpiritStoneItem;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.effect.spell.FlyingSwordBeginnerSpell;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SeekingImmortalsMod.MODID)
public final class ModEvents {
    private static final String EXCHANGE_DAY_KEY = "SeekingImmortalsExchangeDay";
    private static final String EXCHANGE_COUNT_KEY = "SeekingImmortalsExchangeCount";
    private static final int DAILY_EXCHANGE_LIMIT = 3;
    private static final String AGE_DAY_KEY = "SeekingImmortalsAgeDay";
    private static final String MEDITATION_X_KEY = "SeekingImmortalsMeditationX";
    private static final String MEDITATION_Y_KEY = "SeekingImmortalsMeditationY";
    private static final String MEDITATION_Z_KEY = "SeekingImmortalsMeditationZ";
    private static final String PATCHOULI_GUIDE_GIVEN_KEY = "SeekingImmortalsPatchouliGuideGiven";
    private static final ResourceLocation GUIDE_BOOK_ID = new ResourceLocation(SeekingImmortalsMod.MODID, "seeking_immortals_guide");
    private static final UUID SEVERE_INJURY_HEALTH_UUID = UUID.fromString("1a55257a-ea7e-4f42-95cf-3dc716c7f13a");
    private static final int MEDITATION_HUNGER_MINIMUM = 6;
    private static final double MEDITATION_MONSTER_CHECK_RADIUS = 8.0D;
    // 走火入魔风险：受伤修炼每秒 +2%
    private static final int INJURED_MEDITATION_RISK_PER_SECOND = 2;
    // 走火入魔风险衰减：平稳打坐每小时 -5%（每 720 秒 -1%）
    private static final int QI_DEV_RISK_DECAY_INTERVAL_SECONDS = 720;
    // 走火入魔风险衰减：灵脉打坐额外每小时 -10%（每 360 秒 -1%）
    private static final int LEYLINE_RISK_DECAY_INTERVAL_SECONDS = 360;
    private ModEvents() {}

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(CultivationProvider.ID, new CultivationProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        CultivationHelper.get(event.getOriginal()).ifPresent(oldData ->
                CultivationHelper.get(event.getEntity()).ifPresent(newData -> newData.loadNBTData(oldData.saveNBTData())));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        CultivationHelper.get(event.player).ifPresent(cultivation -> {
            cultivation.tickCultivationBoost();
            SpiritualAuraManager.AuraInfo auraInfo = SpiritualAuraManager.getAuraInfo(event.player.level(), event.player.blockPosition());
            boolean onCushion = isSittingOnMeditationCushion(event.player);
            ItemStack bonusStone = getBestHeldSpiritStone(event.player, cultivation);
            int stoneBonus = getMatchingPassiveBonus(bonusStone, cultivation);
            if (cultivation.isMeditating() && onCushion) {
                MeditationTechniqueBonus techniqueBonus = getBestMeditationTechniqueBonus(event.player, cultivation);
                MeditationFormula.Breakdown meditation = MeditationFormula.calculate(cultivation, auraInfo, true, techniqueBonus.multiplier(), bonusStone, stoneBonus);
                cultivation.addMeditationCultivation(meditation);
            }

            if (event.player.tickCount % 20 != 0) return;
            if (event.player instanceof ServerPlayer serverPlayer && unlockPhase4Skills(serverPlayer, cultivation)) {
                SyncLearnedTechniquesPacket.send(serverPlayer, cultivation);
            }
            handleMeditationMovement(event.player, cultivation);
            if (cultivation.isMeditating()) {
                if (shouldInterruptMeditation(event.player, cultivation)) {
                    // Interrupt helpers already clear meditation and notify the player.
                } else {
                    int baseGain = onCushion ? 2 : 1;
                    cultivation.addSpiritualPower(SpiritualAuraManager.adjustSpiritualPowerGain(baseGain, auraInfo));
                    if (onCushion && stoneBonus > 0) {
                        consumeStoneBonus(bonusStone, stoneBonus);
                    }

                    // 走火入魔风险：受伤状态下修炼每秒 +2%
                    if (event.player.getHealth() < event.player.getMaxHealth()) {
                        cultivation.addQiDeviationRisk(INJURED_MEDITATION_RISK_PER_SECOND);
                    }

                    // M3: 走火入魔风险衰减 —— 用累计 tick 计数器，不再依赖 tickCount 取模
                    cultivation.tickQiDeviationDecay(auraInfo.leyline());
                }
            } else {
                cultivation.addSpiritualPower(SpiritualAuraManager.adjustSpiritualPowerGain(1, auraInfo) + consumeStoneBonus(bonusStone, stoneBonus));
            }

            handleAgeAndLifespan(event.player, cultivation);

            absorbFromHeldStone(event.player, event.player.getMainHandItem(), cultivation::addSpiritualPower);
            absorbFromHeldStone(event.player, event.player.getOffhandItem(), cultivation::addSpiritualPower);

            if (ModList.get().isLoaded("curios")) {
                CuriosApi.getCuriosInventory(event.player).ifPresent(handler ->
                        handler.findFirstCurio(ModItems.SPIRIT_CHARM.get()).ifPresent(slotResult ->
                                cultivation.addSpiritualPower(Math.max(1, (int)Math.round(cultivation.getCultivationSpeedMultiplier())))));
            }
            handleFlyingArtifact(event.player, cultivation);
            handleQiFlying(event.player, cultivation);
            handleImmortalAfflictions(event.player, cultivation);
            if (event.player instanceof ServerPlayer serverPlayer) {
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player hurtPlayer) {
            // M13: 寿元耗尽致死不触发打坐中断与走火入魔
            if (hurtPlayer.getPersistentData().getBoolean("SeekingImmortalsLifespanDeath")) {
                hurtPlayer.getPersistentData().remove("SeekingImmortalsLifespanDeath");
                return;
            }
            CultivationHelper.get(hurtPlayer).ifPresent(cultivation -> {
                if (cultivation.isMeditating()) {
                    cultivation.addQiDeviationRisk(INJURED_MEDITATION_RISK_PER_SECOND);
                    stopMeditation(hurtPlayer, cultivation, "message.seeking_immortals.meditation.stop.attacked");
                    if (hurtPlayer instanceof ServerPlayer serverPlayer) {
                        BreakthroughService.tryTriggerQiDeviation(serverPlayer, cultivation, "message.seeking_immortals.qi_deviation.trigger.meditation_injury");
                    }
                }
            });
        }

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity != null && directEntity.getPersistentData().contains("SeekingImmortalsCustomDamage")) {
            event.setAmount((float) directEntity.getPersistentData().getDouble("SeekingImmortalsCustomDamage"));
            directEntity.getPersistentData().remove("SeekingImmortalsCustomDamage");  // 立即清理，防止内存泄漏
        }

        // H9: 飞剑/冰锥弹射物伤害已在生成时 calculateDamage，跳过 cultivation multiplier 与 PvP CombatCalculator 二次重算
        if (directEntity != null && directEntity.getPersistentData().getBoolean("SeekingImmortalsProjectileDamage")) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof Player player)) return;

        CultivationHelper.get(player).ifPresent(cultivation -> {
            double multiplier = cultivation.getOutgoingDamageMultiplier();
            if (directEntity != null && directEntity.getPersistentData().contains("SeekingImmortalsDamageMultiplier")) {
                multiplier *= directEntity.getPersistentData().getDouble("SeekingImmortalsDamageMultiplier");
            }
            event.setAmount(event.getAmount() * (float)multiplier);
        });

        if (!(sourceEntity instanceof ServerPlayer attacker)) return;
        if (!(event.getEntity() instanceof ServerPlayer defender)) return;
        if (event.getEntity().level().isClientSide) return;

        com.xunxian.seekingimmortals.combat.DamageResult result =
                com.xunxian.seekingimmortals.combat.CombatCalculator.calculateDamage(
                        attacker, defender, event.getAmount(), attacker.getRandom());

        if (result.isMissed() || result.isDodged()) {
            event.setCanceled(true);
        } else {
            event.setAmount((float)result.getFinalDamage());
        }
        com.xunxian.seekingimmortals.combat.CombatCalculator.showDamageFeedback(attacker, defender, result);
    }

    // H11: 飞行生命周期清理 —— 死亡/重生/换维时强制清除所有飞行授权
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        player.getPersistentData().remove(FlyingSwordBeginnerSpell.ACTIVE_KEY);
        FlyingAuthority.clearAll(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FlyingAuthority.clearAll(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getPersistentData().remove(FlyingSwordBeginnerSpell.ACTIVE_KEY);
        FlyingAuthority.clearAll(player);
    }

    @SubscribeEvent
    public static void onVillagerExchange(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof Villager) || !event.getEntity().isShiftKeyDown()) return;

        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        long currentDay = serverLevel.getDayTime() / 24000L;
        CompoundTag data = player.getPersistentData();
        if (data.getLong(EXCHANGE_DAY_KEY) != currentDay) {
            data.putLong(EXCHANGE_DAY_KEY, currentDay);
            data.putInt(EXCHANGE_COUNT_KEY, 0);
        }

        int usedToday = data.getInt(EXCHANGE_COUNT_KEY);
        if (usedToday >= DAILY_EXCHANGE_LIMIT) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.exchange.limit", DAILY_EXCHANGE_LIMIT), true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (tryExchange(player, ModItems.METAL_SPIRIT_STONE_HIGH.get(), ModItems.METAL_SPIRIT_STONE_SUPERIOR.get())
                || tryExchange(player, ModItems.METAL_SPIRIT_STONE_MID.get(), ModItems.METAL_SPIRIT_STONE_HIGH.get())
                || tryExchange(player, ModItems.METAL_SPIRIT_STONE.get(), ModItems.METAL_SPIRIT_STONE_MID.get())
                || tryExchange(player, ModItems.WOOD_SPIRIT_STONE_HIGH.get(), ModItems.WOOD_SPIRIT_STONE_SUPERIOR.get())
                || tryExchange(player, ModItems.WOOD_SPIRIT_STONE_MID.get(), ModItems.WOOD_SPIRIT_STONE_HIGH.get())
                || tryExchange(player, ModItems.WOOD_SPIRIT_STONE.get(), ModItems.WOOD_SPIRIT_STONE_MID.get())
                || tryExchange(player, ModItems.WATER_SPIRIT_STONE_HIGH.get(), ModItems.WATER_SPIRIT_STONE_SUPERIOR.get())
                || tryExchange(player, ModItems.WATER_SPIRIT_STONE_MID.get(), ModItems.WATER_SPIRIT_STONE_HIGH.get())
                || tryExchange(player, ModItems.WATER_SPIRIT_STONE.get(), ModItems.WATER_SPIRIT_STONE_MID.get())
                || tryExchange(player, ModItems.FIRE_ELEMENT_SPIRIT_STONE_HIGH.get(), ModItems.FIRE_ELEMENT_SPIRIT_STONE_SUPERIOR.get())
                || tryExchange(player, ModItems.FIRE_ELEMENT_SPIRIT_STONE_MID.get(), ModItems.FIRE_ELEMENT_SPIRIT_STONE_HIGH.get())
                || tryExchange(player, ModItems.FIRE_ELEMENT_SPIRIT_STONE.get(), ModItems.FIRE_ELEMENT_SPIRIT_STONE_MID.get())
                || tryExchange(player, ModItems.EARTH_SPIRIT_STONE_HIGH.get(), ModItems.EARTH_SPIRIT_STONE_SUPERIOR.get())
                || tryExchange(player, ModItems.EARTH_SPIRIT_STONE_MID.get(), ModItems.EARTH_SPIRIT_STONE_HIGH.get())
                || tryExchange(player, ModItems.EARTH_SPIRIT_STONE.get(), ModItems.EARTH_SPIRIT_STONE_MID.get())) {
            data.putInt(EXCHANGE_COUNT_KEY, usedToday + 1);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.exchange.success", usedToday + 1, DAILY_EXCHANGE_LIMIT), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.exchange.no_stones"), true);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        CultivationHelper.get(event.getEntity()).ifPresent(cultivation -> {
            cultivation.ensureRootInitialized(event.getEntity().getRandom());
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                unlockPhase4Skills(serverPlayer, cultivation);
            }
            event.getEntity().displayClientMessage(
                    Component.translatable("message.seeking_immortals.login", cultivation.getRealm().getDisplayName(), cultivation.getStage().getDisplayName(), cultivation.getSpiritualPower(), cultivation.getMaxSpiritualPower()), false);
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                givePatchouliGuideBook(serverPlayer);
                SyncLearnedTechniquesPacket.send(serverPlayer, cultivation);
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
            }
        });
    }

    private static boolean unlockPhase4Skills(ServerPlayer player, PlayerCultivation cultivation) {
        java.util.List<SkillType> unlocked = cultivation.unlockEligiblePhase4Skills();
        if (unlocked.isEmpty()) return false;
        String names = unlocked.stream().map(SkillType::getDisplayName).collect(java.util.stream.Collectors.joining("、"));
        player.displayClientMessage(Component.translatable("message.seeking_immortals.skill.unlock", names), false);
        return true;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SeekingImmortalsCommand.register(event.getDispatcher());
    }

    private static void givePatchouliGuideBook(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(PATCHOULI_GUIDE_GIVEN_KEY)) return;

        ItemStack guideBook = PatchouliAPI.get().getBookStack(GUIDE_BOOK_ID);
        if (guideBook.isEmpty()) return;

        boolean added = player.getInventory().add(guideBook);
        if (!added) {
            player.drop(guideBook, false);
        }
        data.putBoolean(PATCHOULI_GUIDE_GIVEN_KEY, true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.guide_book.given"), false);
    }

    private static boolean isSittingOnMeditationCushion(Player player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof CushionSeatEntity seat) {
            return player.level().getBlockState(seat.getCushionPos()).is(ModBlocks.MEDITATION_CUSHION.get());
        }
        return false;
    }

    private static void handleMeditationMovement(Player player, com.xunxian.seekingimmortals.cultivation.PlayerCultivation cultivation) {
        CompoundTag data = player.getPersistentData();
        if (!cultivation.isMeditating()) {
            clearMeditationAnchor(data);
            return;
        }

        if (!data.contains(MEDITATION_X_KEY)) {
            data.putDouble(MEDITATION_X_KEY, player.getX());
            data.putDouble(MEDITATION_Y_KEY, player.getY());
            data.putDouble(MEDITATION_Z_KEY, player.getZ());
            return;
        }

        boolean onCushion = isSittingOnMeditationCushion(player);
        double dx = player.getX() - data.getDouble(MEDITATION_X_KEY);
        double dz = player.getZ() - data.getDouble(MEDITATION_Z_KEY);
        double dy = Math.abs(player.getY() - data.getDouble(MEDITATION_Y_KEY));
        boolean moved = dx * dx + dz * dz > 0.01D || dy > 0.50D;
        if (moved || (player.isPassenger() && !onCushion)) {
            stopMeditation(player, cultivation, "message.seeking_immortals.meditation.stop");
        }
    }

    private static void clearMeditationAnchor(CompoundTag data) {
        data.remove(MEDITATION_X_KEY);
        data.remove(MEDITATION_Y_KEY);
        data.remove(MEDITATION_Z_KEY);
    }

    private static boolean shouldInterruptMeditation(Player player, PlayerCultivation cultivation) {
        if (!cultivation.isMeditating()) return true;
        if (!isSittingOnMeditationCushion(player)) {
            stopMeditation(player, cultivation, "message.seeking_immortals.meditation.stop");
            return true;
        }
        if (!player.getAbilities().instabuild && player.getFoodData().getFoodLevel() <= MEDITATION_HUNGER_MINIMUM) {
            stopMeditation(player, cultivation, "message.seeking_immortals.meditation.stop.hungry");
            return true;
        }
        if (hasNearbyMonster(player)) {
            stopMeditation(player, cultivation, "message.seeking_immortals.meditation.stop.monster");
            return true;
        }
        return false;
    }

    private static boolean hasNearbyMonster(Player player) {
        return !player.level().getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(MEDITATION_MONSTER_CHECK_RADIUS),
                monster -> monster.isAlive() && !monster.isSpectator()).isEmpty();
    }

    private static void stopMeditation(Player player, PlayerCultivation cultivation, String reasonKey) {
        cultivation.setMeditating(false);
        clearMeditationAnchor(player.getPersistentData());
        if (player.isPassenger()) {
            if (player.getVehicle() instanceof CushionSeatEntity seat) {
                net.minecraft.core.BlockPos cushionPos = seat.getCushionPos();
                player.stopRiding();
                player.setPos(cushionPos.getX() + 0.5D, cushionPos.getY() + 6.0D / 16.0D, cushionPos.getZ() + 0.5D);
            } else {
                player.stopRiding();
            }
        }
        player.displayClientMessage(Component.translatable(reasonKey), true);
    }

    private static ItemStack getBestHeldSpiritStone(Player player, PlayerCultivation cultivation) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return getMatchingPassiveBonus(mainHand, cultivation) >= getMatchingPassiveBonus(offHand, cultivation) ? mainHand : offHand;
    }

    private static int getMatchingPassiveBonus(ItemStack stack, PlayerCultivation cultivation) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof SpiritStoneItem stone) || SpiritStoneItem.getStoredPower(stack) <= 0) return 0;
        SpiritualRootAttribute requiredAttribute = cultivation.getSpiritualRootAttribute();
        // M4: 变异/非五行灵根也能从五行灵石获得被动加成（半额）
        if (!isFiveElement(requiredAttribute)) return stone.getPassiveBonus();
        return stone.matchesAttribute(requiredAttribute) ? stone.getPassiveBonus() : 0;
    }

    private static boolean isFiveElement(SpiritualRootAttribute attribute) {
        return attribute == SpiritualRootAttribute.METAL
                || attribute == SpiritualRootAttribute.WOOD
                || attribute == SpiritualRootAttribute.WATER
                || attribute == SpiritualRootAttribute.FIRE
                || attribute == SpiritualRootAttribute.EARTH;
    }

    private static MeditationTechniqueBonus getBestMeditationTechniqueBonus(Player player, PlayerCultivation cultivation) {
        if (!(player.level() instanceof ServerLevel serverLevel) || cultivation.getLearnedTechniques().isEmpty()) {
            return new MeditationTechniqueBonus("未修主功法", 1.0D);
        }
        return cultivation.getLearnedTechniques().stream()
                .map(id -> TechniqueDataManager.getTechnique(serverLevel.getServer(), id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(technique -> new MeditationTechniqueBonus(technique.name().isBlank() ? technique.id() : technique.name(), getMeditationTechniqueMultiplier(cultivation, technique)))
                .max(Comparator.comparingDouble(MeditationTechniqueBonus::multiplier))
                .orElse(new MeditationTechniqueBonus("未修主功法", 1.0D));
    }

    private static double getMeditationTechniqueMultiplier(PlayerCultivation cultivation, TechniqueDataManager.TechniqueEntry technique) {
        double gradeMultiplier = getTechniqueGradeMultiplier(technique);
        double affinityMultiplier = TechniqueDataManager.getAffinityMultiplier(cultivation, technique);
        return Math.max(1.0D, gradeMultiplier * affinityMultiplier);
    }

    private static double getTechniqueGradeMultiplier(TechniqueDataManager.TechniqueEntry technique) {
        String id = technique.id().toLowerCase(Locale.ROOT);
        String source = technique.source().toLowerCase(Locale.ROOT);
        if (containsAny(source, "化神", "灵界", "古魔", "通天", "大衍", "元磁", "真魔") || containsAny(id, "spirit_transformation", "heaven", "void", "magnetic")) return 1.60D;
        if (containsAny(source, "元婴", "古宝", "高级", "真灵") || containsAny(id, "nascent", "soul")) return 1.45D;
        if (containsAny(source, "结丹", "金丹", "剑诀", "秘典") || containsAny(id, "core", "golden", "sword")) return 1.30D;
        if (containsAny(source, "筑基", "中阶", "阵法", "符宝") || containsAny(id, "foundation")) return 1.18D;
        if (containsAny(source, "长春功", "低阶", "炼气")) return 1.10D;
        return 1.05D;
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static void showMeditationStatus(Player player, PlayerCultivation cultivation, SpiritualAuraManager.AuraInfo auraInfo, MeditationTechniqueBonus techniqueBonus) {
        int currentExp = cultivation.getCultivationExp();
        int nextExp = ((currentExp / cultivation.getRealm().getStageExpSpan()) + 1) * cultivation.getRealm().getStageExpSpan();
        int efficiency = Math.max(1, (int)Math.round(Math.sqrt(auraInfo.concentration() / (double)SpiritualAuraManager.BASE_AURA) * techniqueBonus.multiplier() * cultivation.getCultivationSpeedMultiplier() * 100.0D));
        player.displayClientMessage(Component.translatable("message.seeking_immortals.meditation.status",
                cultivation.getRealm().getDisplayName(), cultivation.getStage().getDisplayName(), currentExp, nextExp,
                efficiency, techniqueBonus.name(), describeSpiritLand(auraInfo)), true);
    }

    private static String describeSpiritLand(SpiritualAuraManager.AuraInfo auraInfo) {
        if (auraInfo.nature() == SpiritualAuraManager.AuraNature.SECRET_REALM) return "秘境仙府";
        if (auraInfo.leylineMultiplier() >= 5.0D) return "四阶灵脉";
        if (auraInfo.leylineMultiplier() >= 4.0D) return "三阶灵脉";
        if (auraInfo.leylineMultiplier() >= 3.0D) return "二阶灵脉";
        if (auraInfo.formationBonus() > 0) return "聚灵阵灵地";
        if (auraInfo.biomeMultiplier() >= 2.0D) return "海岛灵地";
        if (auraInfo.biomeMultiplier() >= 1.5D) return "山川灵地";
        return auraInfo.nature().getDisplayName();
    }

    private static int consumeStoneBonus(ItemStack stack, int requestedBonus) {
        if (requestedBonus <= 0) return 0;
        return SpiritStoneItem.consumeStoredPower(stack, requestedBonus);
    }

    private static void handleFlyingArtifact(Player player, PlayerCultivation cultivation) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.isCreative() || player.isSpectator()) return;

        FlightProfile profile = FlightProfile.forRealm(cultivation.getRealm());
        if (profile == null) {
            revokeFlying(serverPlayer, "message.seeking_immortals.flight.stop.realm");
            return;
        }
        if (!hasFlyingArtifact(player)) {
            revokeFlying(serverPlayer, "message.seeking_immortals.flight.stop.no_artifact");
            return;
        }
        if (player.getY() > profile.maxHeight()) {
            revokeFlying(serverPlayer, "message.seeking_immortals.flight.stop.height");
            return;
        }

        // H11: 灵力不足时不授予飞行，避免 grant/revoke 抖动
        if (cultivation.getSpiritualPower() < profile.costPerSecond()) {
            revokeFlying(serverPlayer, "message.seeking_immortals.flight.stop.no_power");
            SyncCultivationDataPacket.send(serverPlayer, cultivation);
            return;
        }

        grantFlying(serverPlayer, profile);
        if (serverPlayer.getAbilities().flying && serverPlayer.tickCount % 20 == 0) {
            if (!cultivation.consumeSpiritualPower(profile.costPerSecond())) {
                revokeFlying(serverPlayer, "message.seeking_immortals.flight.stop.no_power");
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
                return;
            }
            SyncCultivationDataPacket.send(serverPlayer, cultivation);
        }
        if (serverPlayer.getAbilities().flying) {
            Vec3 movement = serverPlayer.getDeltaMovement();
            double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
            double maxHorizontal = profile.horizontalSpeed();
            if (horizontal > maxHorizontal) {
                double scale = maxHorizontal / horizontal;
                serverPlayer.setDeltaMovement(movement.x * scale, movement.y, movement.z * scale);
            }
            if (movement.y > profile.verticalSpeed()) {
                serverPlayer.setDeltaMovement(serverPlayer.getDeltaMovement().x, profile.verticalSpeed(), serverPlayer.getDeltaMovement().z);
            }
        }
    }

    private static void handleQiFlying(Player player, PlayerCultivation cultivation) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!player.getPersistentData().getBoolean(FlyingSwordBeginnerSpell.ACTIVE_KEY)) return;
        if (player.isCreative() || player.isSpectator()) {
            FlyingSwordBeginnerSpell.stop(serverPlayer, "御剑飞行已收束。");
            return;
        }
        if (cultivation.getRealm() != Realm.QI_REFINING || !cultivation.hasSkill(SkillType.FLYING_SWORD_BEGINNER)) {
            FlyingSwordBeginnerSpell.stop(serverPlayer, "境界或技能不足，御剑飞行中止。");
            return;
        }
        if (!serverPlayer.getAbilities().mayfly || Math.abs(serverPlayer.getAbilities().getFlyingSpeed() - FlyingSwordBeginnerSpell.SPEED) > 0.0001F) {
            FlyingAuthority.grant(serverPlayer, FlyingAuthority.SOURCE_QI_FLYING, FlyingSwordBeginnerSpell.SPEED);
        }
        if (serverPlayer.getAbilities().flying && serverPlayer.tickCount % 20 == 0) {
            if (!cultivation.consumeSpiritualPower(FlyingSwordBeginnerSpell.COST_PER_SECOND)) {
                FlyingSwordBeginnerSpell.stop(serverPlayer, "灵力不足，御剑飞行中止。");
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
                return;
            }
            SyncCultivationDataPacket.send(serverPlayer, cultivation);
        }
    }

    private static boolean hasFlyingArtifact(Player player) {
        if (!ModList.get().isLoaded("curios")) return false;
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(stack -> stack.is(ModItems.FLYING_SWORD.get()) || stack.is(ModItems.FLYING_ARTIFACT.get())).isPresent())
                .orElse(false);
    }

    private static void grantFlying(ServerPlayer player, FlightProfile profile) {
        boolean firstActivation = FlyingAuthority.activeSourceCount(player) == 0;
        FlyingAuthority.grant(player, FlyingAuthority.SOURCE_ARTIFACT, profile.flyingSpeed());
        if (firstActivation) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.flight.start"), true);
        }
    }

    private static void revokeFlying(ServerPlayer player, String reasonKey) {
        FlyingAuthority.revoke(player, FlyingAuthority.SOURCE_ARTIFACT, reasonKey, 0.0F);
    }

    private record FlightProfile(int costPerSecond, int maxHeight, float flyingSpeed, double horizontalSpeed, double verticalSpeed) {
        private static FlightProfile forRealm(Realm realm) {
            return switch (realm) {
                case MORTAL -> null;
                case QI_REFINING -> null;
                case FOUNDATION_ESTABLISHMENT -> new FlightProfile(3, 96, 0.045F, 0.45D, 0.35D);
                case CORE_FORMATION -> new FlightProfile(5, 128, 0.060F, 0.60D, 0.45D);
                case NASCENT_SOUL -> new FlightProfile(8, 160, 0.075F, 0.75D, 0.55D);
                case SOUL_TRANSFORMATION -> new FlightProfile(12, 192, 0.090F, 0.90D, 0.65D);
                case VOID_REFINEMENT -> new FlightProfile(16, 224, 0.105F, 1.05D, 0.75D);
                case UNITY -> new FlightProfile(22, 256, 0.120F, 1.20D, 0.85D);
                case MAHAYANA -> new FlightProfile(30, 320, 0.140F, 1.40D, 1.00D);
                case TRIBULATION -> new FlightProfile(40, 384, 0.160F, 1.60D, 1.15D);
                case TRUE_IMMORTAL -> new FlightProfile(60, 512, 0.200F, 2.00D, 1.35D);
            };
        }
    }

    private static void handleImmortalAfflictions(Player player, com.xunxian.seekingimmortals.cultivation.PlayerCultivation cultivation) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            AttributeModifier oldModifier = maxHealth.getModifier(SEVERE_INJURY_HEALTH_UUID);
            if (oldModifier != null) {
                maxHealth.removeModifier(SEVERE_INJURY_HEALTH_UUID);
            }
            if (cultivation.hasSevereInjury()) {
                maxHealth.addTransientModifier(new AttributeModifier(
                        SEVERE_INJURY_HEALTH_UUID,
                        "seeking_immortals_severe_injury_health",
                        -0.80D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }

        if (cultivation.tickHeartDemonTimer(player.getRandom())) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0, false, true));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.affliction.heart_demon.trigger", cultivation.getHeartDemonLevel()), false);
        }
    }

    private static void handleAgeAndLifespan(Player player, com.xunxian.seekingimmortals.cultivation.PlayerCultivation cultivation) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        long currentDay = serverLevel.getDayTime() / 24000L;
        CompoundTag data = player.getPersistentData();
        if (!data.contains(AGE_DAY_KEY)) {
            data.putLong(AGE_DAY_KEY, currentDay);
            return;
        }
        long lastDay = data.getLong(AGE_DAY_KEY);
        if (currentDay <= lastDay) return;
        long passedDays = currentDay - lastDay;
        data.putLong(AGE_DAY_KEY, currentDay);
        cultivation.addAgeYears((int) Math.min(passedDays, 1000L));
        if (cultivation.isLifespanExhausted() && !player.isCreative() && !player.isSpectator()) {
            // M13: 设置寿元死亡 flag，阻止 onLivingHurt 中打坐中断与走火入魔
            player.getPersistentData().putBoolean("SeekingImmortalsLifespanDeath", true);
            player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.lifespan.exhausted"), false);
        }
    }

    private static void absorbFromHeldStone(Player player, ItemStack stack, java.util.function.IntConsumer spiritualPowerConsumer) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof SpiritStoneItem) || !SpiritStoneItem.isAbsorbing(stack)) return;
        int drained = SpiritStoneItem.consumeStoredPower(stack, SpiritStoneItem.getAbsorbPerSecond(stack));
        if (drained <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spirit_stone.empty"), true);
            return;
        }

        spiritualPowerConsumer.accept(drained);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.spirit_stone.absorbing_tick", drained, SpiritStoneItem.getStoredPower(stack)), true);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 3, 0.25D, 0.35D, 0.25D, 0.01D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.25F, 1.4F);
        }
        if (SpiritStoneItem.getStoredPower(stack) <= 0 && !player.getAbilities().instabuild) {
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spirit_stone.depleted"), true);
        }
    }

    private record MeditationTechniqueBonus(String name, double multiplier) {}

    private static boolean tryExchange(Player player, Item input, Item output) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(input)) count += stack.getCount();
        }
        if (count < 100) return false;

        int remaining = 100;
        for (ItemStack stack : inventory.items) {
            if (!stack.is(input)) continue;
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (remaining <= 0) break;
        }

        if (!inventory.add(new ItemStack(output))) {
            player.drop(new ItemStack(output), false);
        }
        return true;
    }
}
