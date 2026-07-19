package com.xunxian.seekingimmortals.event;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipeManager;
import com.xunxian.seekingimmortals.command.SeekingImmortalsCommand;
import com.xunxian.seekingimmortals.compat.ModCompat;
import com.xunxian.seekingimmortals.compat.patchouli.PatchouliGuideBridge;
import com.xunxian.seekingimmortals.combat.status.StatusRegistry;
import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.CultivationProvider;
import com.xunxian.seekingimmortals.cultivation.FlyingAuthority;
import com.xunxian.seekingimmortals.cultivation.MeditationFormula;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.SpecialPhysique;
import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.cultivation.TribulationService;
import com.xunxian.seekingimmortals.entity.CushionSeatEntity;
import com.xunxian.seekingimmortals.entity.MarketTraderEntity;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import com.xunxian.seekingimmortals.entity.SpiritStoneBankerEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.catalog.SpiritStoneLadderService;
import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import com.xunxian.seekingimmortals.item.CatalogConsumableService;
import com.xunxian.seekingimmortals.item.SpiritStoneItem;
import com.xunxian.seekingimmortals.item.pill.CatalogPillItem;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.network.ModNetwork;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.network.SyncLearnedTechniquesPacket;
import com.xunxian.seekingimmortals.network.SyncSkillDataPacket;
import com.xunxian.seekingimmortals.persistence.PlayerPersistentDataClonePolicy;
import com.xunxian.seekingimmortals.quest.QuestService;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TextQuestNpcHookService;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.sect.EscortMissionService;
import com.xunxian.seekingimmortals.sect.SectMissionGenerator;
import com.xunxian.seekingimmortals.shop.ShopService;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.effect.spell.AuraBodyShieldSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.FlyingSwordAdvancedSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.FlyingSwordBeginnerSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.MultiSwordArraySpell;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import com.xunxian.seekingimmortals.worldpack.DemonRiftHazard;
import com.xunxian.seekingimmortals.worldpack.SecretRealmRewardService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import com.xunxian.seekingimmortals.worldpack.YinUnderworldHazard;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

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
    private static final UUID CULTIVATION_HEALTH_UUID = UUID.fromString("1504ac42-2794-4af5-a58b-5f5fdc9e3c6a");
    private static final UUID CULTIVATION_ATTACK_UUID = UUID.fromString("53f8a9e8-27e8-41d2-bd58-0c9a4dc81c0c");
    private static final UUID CULTIVATION_ARMOR_UUID = UUID.fromString("cc8fdd0b-7549-4f56-9b60-2dd0e4dc6f45");
    private static final UUID CULTIVATION_ARMOR_TOUGHNESS_UUID = UUID.fromString("74f37687-6849-4b38-a0b1-a40a2fd0bd9c");
    private static final UUID CULTIVATION_KNOCKBACK_RESISTANCE_UUID = UUID.fromString("ca17a3b2-0ae3-46ee-8257-efcf3193bc5b");
    private static final UUID CULTIVATION_MOVEMENT_SPEED_UUID = UUID.fromString("275d4c23-2678-4f45-8445-2525d5896053");
    private static final UUID SEVERE_INJURY_HEALTH_UUID = UUID.fromString("1a55257a-ea7e-4f42-95cf-3dc716c7f13a");
    private static final int MEDITATION_HUNGER_MINIMUM = 6;
    private static final double MEDITATION_MONSTER_CHECK_RADIUS = 8.0D;
    private static final String DIYUAN_SECRET_REALM_ID = "diyuan";
    private static final int DIYUAN_PRESSURE_INTERVAL_TICKS = 200;
    private static final int DIYUAN_PRESSURE_MESSAGE_INTERVAL_TICKS = 1200;
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
        try {
            CompoundTag originalData = event.getOriginal().getPersistentData();
            CompoundTag clonedData = event.getEntity().getPersistentData();
            PlayerPersistentDataClonePolicy.moveExtremePreserved(originalData, clonedData);
            CultivationHelper.get(event.getOriginal()).ifPresent(oldData ->
                    CultivationHelper.get(event.getEntity()).ifPresent(newData ->
                            newData.loadNBTData(oldData.saveNBTData())));
            PlayerPersistentDataClonePolicy.copyDurableData(originalData, clonedData);
            if (event.getOriginal() instanceof ServerPlayer originalPlayer
                    && SectMissionGenerator.hasActiveEscortMission(originalPlayer)) {
                EscortMissionService.clearEscort(originalPlayer, true);
            }
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(net.minecraftforge.event.level.LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
            FormationFieldService.loadFromSavedData(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }
        if (event.level instanceof ServerLevel serverLevel) {
            FormationFieldService.serverTick(serverLevel);
            // M06: daily event day-roll is overworld-driven and server-authoritative.
            if (serverLevel.dimension() == ServerLevel.OVERWORLD) {
                com.xunxian.seekingimmortals.region.DailyEventScheduler.serverTick(serverLevel.getServer());
            }
        }
    }

    /** M07: invalidate station formed cache when nearby blocks change (large structure dirty flags). */
    @SubscribeEvent
    public static void onBlockPlace(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            com.xunxian.seekingimmortals.structure.MultiblockStationService.markDirty(serverLevel, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (SecretRealmRewardService.isBoundReward(serverLevel.getBlockEntity(event.getPos()))) {
                event.getPlayer().displayClientMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.reward_break_denied"), true);
                event.setCanceled(true);
                return;
            }
            com.xunxian.seekingimmortals.structure.MultiblockStationService.markDirty(serverLevel, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            event.getAffectedBlocks().removeIf(pos ->
                    SecretRealmRewardService.isBoundReward(serverLevel.getBlockEntity(pos)));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        handleCatalogPillTimers(event.player);
        // M04: re-assert palm bottle uniqueness occasionally
        if (event.player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 100 == 0) {
            com.xunxian.seekingimmortals.craft.GardenLiquidService.enforceUniqueBottle(serverPlayer);
        }
        CultivationHelper.get(event.player).ifPresent(cultivation -> {
            cultivation.tickCultivationBoost();
            String preferredRegion = cultivation.getWorldpackCurrentRegionId();
            SpiritualAuraManager.AuraInfo auraInfo = SpiritualAuraManager.getAuraInfo(
                    event.player.level(), event.player.blockPosition(), preferredRegion);
            boolean onCushion = isSittingOnMeditationCushion(event.player);
            ItemStack bonusStone = getBestHeldSpiritStone(event.player, cultivation);
            int stoneBonus = getMatchingPassiveBonus(bonusStone, cultivation);
            if (cultivation.isMeditating() && onCushion) {
                MeditationTechniqueBonus techniqueBonus = getBestMeditationTechniqueBonus(event.player, cultivation);
                MeditationFormula.Breakdown meditation = MeditationFormula.calculate(cultivation, auraInfo, true, techniqueBonus.multiplier(), bonusStone, stoneBonus);
                cultivation.addMeditationCultivation(meditation);
            }
            if (event.player instanceof ServerPlayer serverPlayer) {
                TribulationService.tick(serverPlayer, cultivation);
                handlePhysiqueDefects(serverPlayer, cultivation);
                MultiSwordArraySpell.tickActive(serverPlayer);
                // Wave472: secret-realm hazard pulse every 5s while active.
                if (serverPlayer.tickCount % 100 == 0) {
                    com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService.tickHazard(serverPlayer);
                }
                // M09: timeout kick / session recovery every second.
                if (serverPlayer.tickCount % 20 == 0) {
                    com.xunxian.seekingimmortals.worldpack.SecretRealmSessionService.tickSessions(serverPlayer);
                }
                // Wave485: battlefield AI pulse while sect war is active.
                if (serverPlayer.tickCount % 40 == 0) {
                    com.xunxian.seekingimmortals.sect.SectWarService.tickBattlefieldAi(serverPlayer);
                }
                // Wave491: escort servitor leash/follow health.
                if (serverPlayer.tickCount % 20 == 0) {
                    SectMissionGenerator.retryPendingEscort(serverPlayer);
                    if (EscortMissionService.tick(serverPlayer)) {
                        SectMissionGenerator.restartEscortAfterRespawn(serverPlayer);
                    }
                }
                // Wave492: divine sense expansion passive tick.
                if (serverPlayer.tickCount % 40 == 0) {
                    com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseExpansionPassive.tick(serverPlayer);
                }
            }

            if (event.player.tickCount % 20 != 0) return;
            if (event.player instanceof ServerPlayer serverPlayer && unlockConfiguredTechniqueSkills(serverPlayer, cultivation)) {
                SyncLearnedTechniquesPacket.send(serverPlayer, cultivation);
            }
            handleMeditationMovement(event.player, cultivation);
            if (cultivation.isMeditating()) {
                if (shouldInterruptMeditation(event.player, cultivation)) {
                    // Interrupt helpers already clear meditation and notify the player.
                } else {
                    int baseGain = onCushion ? 2 : 1;
                    int auraGain = SpiritualAuraManager.adjustSpiritualPowerGain(baseGain, auraInfo);
                    if (event.player instanceof ServerPlayer serverPlayer) {
                        auraGain = WorldpackGameplayService.applyAuraBonus(serverPlayer, cultivation, auraGain);
                    }
                    cultivation.addSpiritualPower(auraGain);
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
                int auraGain = SpiritualAuraManager.adjustSpiritualPowerGain(1, auraInfo);
                if (event.player instanceof ServerPlayer serverPlayer) {
                    auraGain = WorldpackGameplayService.applyAuraBonus(serverPlayer, cultivation, auraGain);
                }
                cultivation.addSpiritualPower(auraGain + consumeStoneBonus(bonusStone, stoneBonus));
            }

            handleAgeAndLifespan(event.player, cultivation);

            absorbFromHeldStone(event.player, event.player.getMainHandItem(), cultivation::addSpiritualPower);
            absorbFromHeldStone(event.player, event.player.getOffhandItem(), cultivation::addSpiritualPower);

            // Wave481: SpiritCharm recovery lives on ICurioItem.curioTick (no double tick here).
            boolean flightSuppressed = event.player instanceof ServerPlayer serverPlayer
                    && handleWorldpackNoFly(serverPlayer, cultivation);
            if (!flightSuppressed) {
                handleFlyingArtifact(event.player, cultivation);
                handleQiFlying(event.player, cultivation);
                handleFoundationFlying(event.player, cultivation);
            }
            if (event.player instanceof ServerPlayer serverPlayer) {
                handleDiyuanPressure(serverPlayer, cultivation);
                handleYinUnderworldHazard(serverPlayer, cultivation);
                handleDemonRiftHazard(serverPlayer, cultivation);
            }
            refreshCultivationAttributeState(event.player, cultivation);
            handleImmortalAfflictions(event.player, cultivation);
            if (event.player instanceof ServerPlayer serverPlayer) {
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob encounterMob) {
            CompoundTag binding = null;
            if (com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService.isTrialMob(encounterMob)) {
                binding = encounterMob.getPersistentData().getCompound(
                        com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService.TRIAL_TAG);
            } else if (com.xunxian.seekingimmortals.worldpack.BossEncounterService.isBossMob(encounterMob)) {
                binding = encounterMob.getPersistentData().getCompound(
                        com.xunxian.seekingimmortals.worldpack.BossEncounterService.BOSS_TAG);
            }
            Entity authoritySource = event.getSource().getEntity();
            ServerPlayer authorityPlayer = authoritySource instanceof ServerPlayer player
                    ? player
                    : authoritySource instanceof SummonedServitorEntity servitor
                    ? servitor.getOwnerUUID()
                            .map(uuid -> event.getEntity().getServer() == null ? null
                                    : event.getEntity().getServer().getPlayerList().getPlayer(uuid))
                            .orElse(null)
                    : null;
            boolean playerControlled = authoritySource instanceof ServerPlayer
                    || authoritySource instanceof SummonedServitorEntity;
            if (com.xunxian.seekingimmortals.worldpack.SecretRealmSessionService
                    .hasEncounterBinding(binding) && playerControlled
                    && (authorityPlayer == null
                    || !com.xunxian.seekingimmortals.worldpack.SecretRealmSessionService
                    .matchesEncounter(authorityPlayer, binding))) {
                event.setAmount(0.0F);
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof Player hurtPlayer) {
            // M13: 寿元耗尽致死不触发打坐中断与走火入魔
            if (hurtPlayer.getPersistentData().getBoolean("SeekingImmortalsLifespanDeath")) {
                return;
            }
            if (hurtPlayer instanceof ServerPlayer serverPlayer
                    && hurtPlayer.getPersistentData().getBoolean(AuraBodyShieldSpell.ACTIVE_KEY)) {
                hurtPlayer.getPersistentData().remove(AuraBodyShieldSpell.ACTIVE_KEY);
                event.setAmount(0.0F);
                event.setCanceled(true);
                serverPlayer.serverLevel().sendParticles(ParticleTypes.END_ROD,
                        serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                        32, 0.6D, 0.8D, 0.6D, 0.03D);
                serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.1F);
                serverPlayer.displayClientMessage(Component.literal("Aura body shield absorbed this damage."), true);
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
            directEntity.getPersistentData().remove("SeekingImmortalsCustomDamage");
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof LivingEntity livingAttacker) {
            event.setAmount(event.getAmount() * (float) StatusRegistry.outgoingDamageMultiplier(livingAttacker));
        }

        if (sourceEntity instanceof Player player) {
            CultivationHelper.get(player).ifPresent(cultivation -> {
                double multiplier = cultivation.getOutgoingDamageMultiplier();
                if (directEntity != null && directEntity.getPersistentData().contains("SeekingImmortalsDamageMultiplier")) {
                    multiplier *= directEntity.getPersistentData().getDouble("SeekingImmortalsDamageMultiplier");
                }
                multiplier *= com.xunxian.seekingimmortals.artifact.ArtifactSynergyService
                        .outgoingDamageMultiplier(player);
                event.setAmount(event.getAmount() * (float)multiplier);
            });
        }

        // M15: 防御协同降低承伤（仍走 LivingHurt 管线）。
        if (event.getEntity() instanceof Player hurtTarget) {
            float incoming = (float) com.xunxian.seekingimmortals.artifact.ArtifactSynergyService
                    .incomingDamageMultiplier(hurtTarget);
            if (incoming != 1.0F) {
                event.setAmount(event.getAmount() * incoming);
            }
        }

        if (!(sourceEntity instanceof ServerPlayer attacker)) return;
        if (!(event.getEntity() instanceof ServerPlayer defender)) return;
        if (event.getEntity().level().isClientSide) return;

        com.xunxian.seekingimmortals.combat.DamageResult result =
                com.xunxian.seekingimmortals.combat.CombatCalculator.calculateDamage(
                        attacker, defender, event.getAmount(), attacker.getRandom(), false);

        if (result.isMissed() || result.isDodged()) {
            event.setCanceled(true);
        } else {
            event.setAmount((float)result.getFinalDamage());
        }
        com.xunxian.seekingimmortals.combat.CombatCalculator.showDamageFeedback(attacker, defender, result);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            float mitigated = CatalogConsumableService.mitigateLightningDamage(
                    serverPlayer, event.getSource(), event.getAmount());
            if (mitigated != event.getAmount()) {
                event.setAmount(mitigated);
            }
        }
    }

    // H11: Flying lifecycle cleanup.
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        try {
            handleCommittedLivingDrops(event);
        } catch (RuntimeException exception) {
            SeekingImmortalsMod.LOGGER.error("Failed to apply committed death side effects", exception);
        } finally {
            if (event.getEntity() instanceof ServerPlayer player) {
                BreakthroughService.markExtremeDeathCommitted(player);
            }
        }
    }

    private static void handleCommittedLivingDrops(LivingDropsEvent event) {
        // Wave49: sect-war scoring when a player dies to another player.
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().getEntity() instanceof ServerPlayer killer) {
            com.xunxian.seekingimmortals.sect.SectWarService.onKill(killer, victim);
        }
        // Wave471: secret-realm kill gates (patrol/guardian/boss).
        // Wave485: sect-war battlefield shell kills score for the killer's side.
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob
                && event.getSource().getEntity() instanceof ServerPlayer killer) {
            if (com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService.isTrialMob(mob)) {
                boolean accepted = com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService
                        .onTrialMobKilled(killer, mob);
                if (accepted) {
                    // M11: advance quest hooks only for the bound session owner.
                    String realmId = com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService.trialRealm(mob);
                    String kind = com.xunxian.seekingimmortals.worldpack.SecretRealmTrialService.trialKind(mob);
                    String layer = "guardian".equals(kind) ? "core" : "mid";
                    com.xunxian.seekingimmortals.quest.QuestHookRuntime.onSecretRealmClear(killer, realmId, layer);
                }
            }
            if (com.xunxian.seekingimmortals.worldpack.BossEncounterService.isBossMob(mob)) {
                com.xunxian.seekingimmortals.worldpack.BossEncounterService.onBossKilled(killer, mob);
            }
            if (com.xunxian.seekingimmortals.sect.SectWarService.isWarShell(mob)) {
                com.xunxian.seekingimmortals.sect.SectWarService.onWarShellKilled(killer, mob);
            }
            // M10: ecology beast loot + bestiary unlock on kill.
            com.xunxian.seekingimmortals.beast.BeastLootService.handleEcologyKill(killer, mob);
            // Wave489/491: sect daily kill mission progress with typed target filter.
            if (mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
                String typeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                        .getKey(mob.getType()) == null ? "monster"
                        : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getPath();
                com.xunxian.seekingimmortals.sect.SectMissionGenerator.onHostileKill(killer, typeId);
            }
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getPersistentData().remove(FlyingSwordBeginnerSpell.ACTIVE_KEY);
        player.getPersistentData().remove(FlyingSwordAdvancedSpell.ACTIVE_KEY);
        player.getPersistentData().remove(AuraBodyShieldSpell.ACTIVE_KEY);
        MultiSwordArraySpell.clear(player);
        FlyingAuthority.clearAll(player);
        // M09: death inside secret realm ejects to return anchor.
        com.xunxian.seekingimmortals.worldpack.SecretRealmSessionService.handlePlayerDeath(player);
        CultivationHelper.get(player).ifPresent(cultivation -> TribulationService.handleDeath(player, cultivation));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getPersistentData().remove(FlyingSwordBeginnerSpell.ACTIVE_KEY);
        player.getPersistentData().remove(FlyingSwordAdvancedSpell.ACTIVE_KEY);
        player.getPersistentData().remove(AuraBodyShieldSpell.ACTIVE_KEY);
        MultiSwordArraySpell.clear(player);
        FlyingAuthority.clearAll(player);
        BreakthroughService.restorePreservedOnRespawn(player);
        SectMissionGenerator.restartEscortAfterRespawn(player);
        CultivationHelper.get(player).ifPresent(cultivation -> {
            refreshCultivationAttributeState(player, cultivation);
            syncClientMirrors(player, cultivation);
        });
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.getPersistentData().remove(FlyingSwordBeginnerSpell.ACTIVE_KEY);
        player.getPersistentData().remove(FlyingSwordAdvancedSpell.ACTIVE_KEY);
        player.getPersistentData().remove(AuraBodyShieldSpell.ACTIVE_KEY);
        MultiSwordArraySpell.clear(player);
        FlyingAuthority.clearAll(player);
        SectMissionGenerator.restartEscortAfterRespawn(player);
        // M13: re-apply realm/dimension flight policy after clearing transient sources.
        com.xunxian.seekingimmortals.worldpack.FlyingAuthorityPolicy.onDimensionChanged(
                player, event.getTo().location().toString());
        CultivationHelper.get(player).ifPresent(cultivation -> {
            TribulationService.handleDimensionChange(player, cultivation);
            refreshCultivationAttributeState(player, cultivation);
            syncClientMirrors(player, cultivation);
        });
    }

    @SubscribeEvent
    public static void onVillagerExchange(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof Villager villager)) return;

        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer && villager instanceof MarketTraderEntity trader) {
            // M12: named trader opens dialogue/shop via MarketTraderEntity; shelves still M05.
            trader.openFor(serverPlayer);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && villager instanceof SectStewardEntity steward) {
            // Wave489: steward interaction also marks escort-proxy daily progress.
            com.xunxian.seekingimmortals.sect.SectMissionGenerator.onStewardEscortMark(serverPlayer, steward);
            // M12: try named-NPC dialogue first; fall back to M08 sect hall business.
            if (!steward.openDialogue(serverPlayer)) {
                SectContributionService.handleStewardInteraction(serverPlayer, steward);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        // M05: spirit stone ladder upgrades only on original banker NPC (not vanilla villagers).
        if (player instanceof ServerPlayer serverPlayer && villager instanceof SpiritStoneBankerEntity) {
            handleSpiritStoneBankerExchange(serverPlayer, event);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && QuestService.handleNamedVillagerInteraction(serverPlayer, villager)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        // Wave55 defensive path: text-quest NPC hook if QuestService did not claim the villager.
        if (player instanceof ServerPlayer serverPlayer
                && TextQuestNpcHookService.handleNamedVillagerInteraction(serverPlayer, villager)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        // Compatibility fallback until banker NPCs have a survival spawn path.
        if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            handleSpiritStoneBankerExchange(serverPlayer, event);
        }
    }

    private static void handleSpiritStoneBankerExchange(ServerPlayer player, PlayerInteractEvent.EntityInteract event) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
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

        // Right-click banker to upgrade ladder stones; no shift required on original NPC.
        if (SpiritStoneLadderService.tryUpgrade(player)) {
            data.putInt(EXCHANGE_COUNT_KEY, usedToday + 1);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.exchange.success", usedToday + 1, DAILY_EXCHANGE_LIMIT), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.exchange.no_stones"), true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onQuestBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        SecretRealmRewardService.ClaimResult reward = SecretRealmRewardService.claim(
                player, serverLevel.getBlockEntity(event.getPos()));
        if (reward != SecretRealmRewardService.ClaimResult.NOT_BOUND) {
            if (reward == SecretRealmRewardService.ClaimResult.DENIED) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.reward_owner_denied"), true);
                event.setCancellationResult(InteractionResult.FAIL);
            } else if (reward == SecretRealmRewardService.ClaimResult.SEALED) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.reward_sealed"), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else if (reward == SecretRealmRewardService.ClaimResult.CLAIMED) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.worldpack.reward_claimed"), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else {
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            event.setCanceled(true);
            return;
        }
        if (QuestService.handleBlockInteraction(player, serverLevel, event.getPos(), event.getHand())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        CultivationHelper.get(event.getEntity()).ifPresent(cultivation -> {
            cultivation.ensureRootInitialized(event.getEntity().getRandom());
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                unlockConfiguredTechniqueSkills(serverPlayer, cultivation);
            }
            event.getEntity().displayClientMessage(
                    Component.translatable("message.seeking_immortals.login", cultivation.getRealm().getDisplayName(), cultivation.getStage().getDisplayName(), cultivation.getSpiritualPower(), cultivation.getMaxSpiritualPower()), false);
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                refreshCultivationAttributeState(serverPlayer, cultivation);
                givePatchouliGuideBook(serverPlayer);
                // Wave467: claim offline auction outbid refunds.
                com.xunxian.seekingimmortals.catalog.AuctionSoftService.claimPendingRefunds(serverPlayer);
                // Persistent delivery outbox (rewards that could not fit inventory).
                int delivered = com.xunxian.seekingimmortals.item.InventoryDeliveryService.claimQueued(serverPlayer);
                if (delivered > 0) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.delivery.claimed", delivered), false);
                }
                // M04: 掌天瓶唯一性服务端强制
                com.xunxian.seekingimmortals.craft.GardenLiquidService.enforceUniqueBottle(serverPlayer);
                syncClientMirrors(serverPlayer, cultivation);
            }
        });
    }

    private static void syncClientMirrors(ServerPlayer player, PlayerCultivation cultivation) {
        // Resolve location-owned state before emitting the complete client mirror snapshot.
        com.xunxian.seekingimmortals.region.RegionRegistry.resolveAndSync(player);
        SyncCultivationDataPacket.send(player, cultivation);
        SyncLearnedTechniquesPacket.send(player, cultivation);
        SyncSkillDataPacket.send(player, cultivation);
        com.xunxian.seekingimmortals.catalog.ManualCatalogService.syncLearnedMethods(player);
        com.xunxian.seekingimmortals.catalog.MethodLayoutService.sync(player);
        SectContributionService.syncSect(player, cultivation, false);
        WorldpackGameplayService.syncSnapshot(player);
        TextQuestChainService.syncTracker(player);
        com.xunxian.seekingimmortals.lore.LoreSyncService.syncOnly(player);
    }

    private static boolean unlockConfiguredTechniqueSkills(ServerPlayer player, PlayerCultivation cultivation) {
        java.util.List<SkillType> unlocked = cultivation.unlockEligibleTechniqueSkills();
        if (unlocked.isEmpty()) return false;
        String names = unlocked.stream().map(SkillType::getDisplayName).collect(java.util.stream.Collectors.joining(", "));
        player.displayClientMessage(Component.translatable("message.seeking_immortals.skill.unlock", names), false);
        return true;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SeekingImmortalsCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AlchemyRecipeManager());
    }

    private static void givePatchouliGuideBook(ServerPlayer player) {
        // Patchouli is optional: never touch PatchouliAPI when the mod is absent.
        if (!ModCompat.PATCHOULI_LOADED) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(PATCHOULI_GUIDE_GIVEN_KEY)) {
            return;
        }

        ItemStack guideBook = PatchouliGuideBridge.getBookStackSafe(GUIDE_BOOK_ID);
        if (guideBook.isEmpty()) {
            return;
        }

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
        return SpiritStoneItem.getMatchingPassiveBonus(stack, cultivation.getSpiritualRootAttribute());
    }

    private static MeditationTechniqueBonus getBestMeditationTechniqueBonus(Player player, PlayerCultivation cultivation) {
        if (!(player.level() instanceof ServerLevel serverLevel) || cultivation.getLearnedTechniques().isEmpty()) {
            return new MeditationTechniqueBonus("No main cultivation technique", 1.0D);
        }
        return cultivation.getLearnedTechniques().stream()
                .map(id -> TechniqueDataManager.getTechnique(serverLevel.getServer(), id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(technique -> new MeditationTechniqueBonus(technique.name().isBlank() ? technique.id() : technique.name(), getMeditationTechniqueMultiplier(cultivation, technique)))
                .max(Comparator.comparingDouble(MeditationTechniqueBonus::multiplier))
                .orElse(new MeditationTechniqueBonus("No main cultivation technique", 1.0D));
    }

    private static double getMeditationTechniqueMultiplier(PlayerCultivation cultivation, TechniqueDataManager.TechniqueEntry technique) {
        double gradeMultiplier = getTechniqueGradeMultiplier(technique);
        double affinityMultiplier = TechniqueDataManager.getAffinityMultiplier(cultivation, technique);
        return Math.max(1.0D, gradeMultiplier * affinityMultiplier);
    }

    static double getTechniqueGradeMultiplier(TechniqueDataManager.TechniqueEntry technique) {
        String id = technique.id().toLowerCase(Locale.ROOT);
        String source = technique.source().toLowerCase(Locale.ROOT);
        if (containsAny(source, "化神", "灵界", "古魔", "通天", "大衍", "元磁", "真魔", "仙界", "真仙", "上古魔功", "通天灵宝") || containsAny(id, "spirit_transformation", "heaven", "void", "magnetic")) return 1.60D;
        if (containsAny(source, "元婴", "古宝", "高级", "真灵") || containsAny(id, "nascent", "soul")) return 1.45D;
        if (containsAny(source, "结丹", "金丹", "剑诀", "秘典") || containsAny(id, "core", "golden", "sword")) return 1.30D;
        if (containsAny(source, "筑基", "中阶", "阵法", "符宝") || containsAny(id, "foundation")) return 1.18D;
        if (containsAny(source, "evergreen", "low", "qi")) return 1.10D;
        return 1.05D;
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static int consumeStoneBonus(ItemStack stack, int requestedBonus) {
        if (requestedBonus <= 0) return 0;
        return SpiritStoneItem.consumeStoredPower(stack, requestedBonus);
    }

    private static boolean handleWorldpackNoFly(ServerPlayer player, PlayerCultivation cultivation) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        boolean dimensionDenied = !com.xunxian.seekingimmortals.worldpack.FlyingAuthorityPolicy
                .permitsManagedFlightDimension(player.level().dimension().location().toString());
        boolean realmSuppressed = WorldpackGameplayService.isFlightSuppressed(cultivation);
        if (!dimensionDenied && !realmSuppressed) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        boolean hadManagedFlight = FlyingAuthority.activeSourceCount(player) > 0
                || data.getBoolean(FlyingSwordBeginnerSpell.ACTIVE_KEY)
                || data.getBoolean(FlyingSwordAdvancedSpell.ACTIVE_KEY);
        data.remove(FlyingSwordBeginnerSpell.ACTIVE_KEY);
        data.remove(FlyingSwordAdvancedSpell.ACTIVE_KEY);
        FlyingAuthority.clearAll(player);
        if (hadManagedFlight) {
            String reason = dimensionDenied
                    ? "message.seeking_immortals.flight.stop.dimension"
                    : "message.seeking_immortals.flight.stop.no_fly_secret_realm";
            player.displayClientMessage(Component.translatable(reason), true);
        }
        return true;
    }

    private static void handleDiyuanPressure(ServerPlayer player, PlayerCultivation cultivation) {
        if (player.isCreative() || player.isSpectator()
                || !DIYUAN_SECRET_REALM_ID.equals(cultivation.getWorldpackActiveSecretRealmId())) {
            return;
        }
        if (CatalogPillItem.hasPressureResist(player)) {
            if (player.tickCount % DIYUAN_PRESSURE_INTERVAL_TICKS == 0) {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.CONFUSION);
            }
            return;
        }
        if (player.tickCount % DIYUAN_PRESSURE_INTERVAL_TICKS != 0) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DIYUAN_PRESSURE_INTERVAL_TICKS + 40, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, true));
        cultivation.addDivineConsciousness(-1);
        if (player.getHealth() > 6.0F) {
            player.hurt(player.damageSources().magic(), 1.0F);
        }
        if (player.tickCount % DIYUAN_PRESSURE_MESSAGE_INTERVAL_TICKS == 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.worldpack.diyuan_pressure"), true);
        }
    }

    private static void handleYinUnderworldHazard(ServerPlayer player, PlayerCultivation cultivation) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        YinUnderworldHazard.Profile profile = YinUnderworldHazard.profile(
                cultivation.getWorldpackCurrentRegionId(),
                cultivation.getWorldpackActiveSecretRealmId(),
                player.level().dimension().location().toString(),
                cultivation.getWorldpackActiveDailyEventId());
        boolean hasYinProtection = CatalogPillItem.hasYinProtection(player);
        if (!profile.active()) {
            return;
        }
        if (hasYinProtection) {
            if (player.tickCount % 40 == 0) {
                player.removeEffect(MobEffects.DARKNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.CONFUSION);
            }
            profile = profile.mitigatedByYinProtection();
        }
        if (!profile.shouldApply(player.tickCount)) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, profile.effectDurationTicks(), 0, false, true));
        if (profile.slownessAmplifier() >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    profile.effectDurationTicks(), profile.slownessAmplifier(), false, true));
        }
        if (profile.weaknessAmplifier() >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    profile.effectDurationTicks(), profile.weaknessAmplifier(), false, true));
        }
        if (profile.nauseaTicks() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, profile.nauseaTicks(), 0, false, true));
        }
        if (profile.divineConsciousnessDrain() > 0) {
            cultivation.addDivineConsciousness(-profile.divineConsciousnessDrain());
        }
        float damage = profile.safeDamage(player.getHealth());
        if (damage > 0.0F) {
            player.hurt(player.damageSources().magic(), damage);
        }
        if (profile.shouldMessage(player.tickCount)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.worldpack.yin_underworld_hazard"), true);
        }
    }

    private static void handleDemonRiftHazard(ServerPlayer player, PlayerCultivation cultivation) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        DemonRiftHazard.Profile profile = DemonRiftHazard.profile(
                cultivation.getWorldpackCurrentRegionId(),
                cultivation.getWorldpackActiveSecretRealmId(),
                player.level().dimension().location().toString(),
                cultivation.getWorldpackActiveDailyEventId());
        if (!profile.active() || !profile.shouldApply(player.tickCount)) {
            return;
        }

        if (profile.darknessTicks() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, profile.darknessTicks(), 0, false, true));
        }
        if (profile.slownessAmplifier() >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    profile.effectDurationTicks(), profile.slownessAmplifier(), false, true));
        }
        if (profile.weaknessAmplifier() >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    profile.effectDurationTicks(), profile.weaknessAmplifier(), false, true));
        }
        if (profile.confusionTicks() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, profile.confusionTicks(), 0, false, true));
        }
        if (profile.divineConsciousnessDrain() > 0) {
            cultivation.addDivineConsciousness(-profile.divineConsciousnessDrain());
        }
        if (profile.qiDeviationRisk() > 0) {
            cultivation.addQiDeviationRisk(profile.qiDeviationRisk());
        }
        float damage = profile.safeDamage(player.getHealth());
        if (damage > 0.0F) {
            player.hurt(player.damageSources().magic(), damage);
        }
        if (profile.shouldMessage(player.tickCount)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.worldpack.demon_rift_hazard"), true);
        }
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
            FlyingSwordBeginnerSpell.stop(serverPlayer, "Qi flying stopped.");
            return;
        }
        if (cultivation.getRealm() != Realm.QI_REFINING || !cultivation.hasSkill(SkillType.FLYING_SWORD_BEGINNER)) {
            FlyingSwordBeginnerSpell.stop(serverPlayer, "Realm or skill is insufficient; qi flying stopped.");
            return;
        }
        if (!serverPlayer.getAbilities().mayfly || Math.abs(serverPlayer.getAbilities().getFlyingSpeed() - FlyingSwordBeginnerSpell.SPEED) > 0.0001F) {
            FlyingAuthority.grant(serverPlayer, FlyingAuthority.SOURCE_QI_FLYING, FlyingSwordBeginnerSpell.SPEED);
        }
        if (serverPlayer.getAbilities().flying && serverPlayer.tickCount % 20 == 0) {
            if (!cultivation.consumeSpiritualPower(FlyingSwordBeginnerSpell.COST_PER_SECOND)) {
                FlyingSwordBeginnerSpell.stop(serverPlayer, "Insufficient mana; qi flying stopped.");
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
                return;
            }
            // Wave490: special skill practice while qi-flying.
            if (serverPlayer.tickCount % 100 == 0) {
                com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(
                        serverPlayer, SkillType.FLYING_SWORD_BEGINNER, 6, 2);
            }
            SyncCultivationDataPacket.send(serverPlayer, cultivation);
        }
        if (serverPlayer.getAbilities().flying) {
            serverPlayer.serverLevel().sendParticles(ParticleTypes.END_ROD,
                    serverPlayer.getX(), serverPlayer.getY() + 0.2D, serverPlayer.getZ(),
                    8, 0.25D, 0.08D, 0.25D, 0.01D);
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.18F, 1.7F);
        }
    }

    private static void handleFoundationFlying(Player player, PlayerCultivation cultivation) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!player.getPersistentData().getBoolean(FlyingSwordAdvancedSpell.ACTIVE_KEY)) return;
        if (player.isCreative() || player.isSpectator()) {
            FlyingSwordAdvancedSpell.stop(serverPlayer, "Advanced flying stopped.");
            return;
        }
        if (cultivation.getRealm().ordinal() < Realm.FOUNDATION_ESTABLISHMENT.ordinal()
                || !cultivation.hasSkill(SkillType.FLYING_SWORD_ADVANCED)) {
            FlyingSwordAdvancedSpell.stop(serverPlayer, "Realm or skill is insufficient; advanced flying stopped.");
            return;
        }
        if (!serverPlayer.getAbilities().mayfly || Math.abs(serverPlayer.getAbilities().getFlyingSpeed() - FlyingSwordAdvancedSpell.SPEED) > 0.0001F) {
            FlyingAuthority.grant(serverPlayer, FlyingAuthority.SOURCE_FOUNDATION_FLYING, FlyingSwordAdvancedSpell.SPEED);
        }
        if (serverPlayer.getAbilities().flying && serverPlayer.tickCount % 20 == 0) {
            if (!cultivation.consumeSpiritualPower(FlyingSwordAdvancedSpell.COST_PER_SECOND)) {
                FlyingSwordAdvancedSpell.stop(serverPlayer, "Insufficient mana; advanced flying stopped.");
                SyncCultivationDataPacket.send(serverPlayer, cultivation);
                return;
            }
            // Wave490: special skill practice while foundation flying.
            if (serverPlayer.tickCount % 100 == 0) {
                com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(
                        serverPlayer, SkillType.FLYING_SWORD_ADVANCED, 8, 3);
            }
            SyncCultivationDataPacket.send(serverPlayer, cultivation);
        }
        // Wave48: orbiting guard-sword particle visuals while advanced flight is active.
        FlyingSwordAdvancedSpell.tickGuardSwordVisuals(serverPlayer);
        if (serverPlayer.getAbilities().flying) {
            serverPlayer.serverLevel().sendParticles(ParticleTypes.END_ROD,
                    serverPlayer.getX(), serverPlayer.getY() + 0.2D, serverPlayer.getZ(),
                    12, 0.35D, 0.10D, 0.35D, 0.015D);
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.22F, 1.9F);
        }
    }

    private static boolean hasFlyingArtifact(Player player) {
        if (!ModList.get().isLoaded("curios")) return false;
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(ModEvents::isManagedFlyingArtifact).isPresent())
                .orElse(false);
    }

    private static boolean isManagedFlyingArtifact(ItemStack stack) {
        if (stack.is(ModItems.FLYING_SWORD.get()) || stack.is(ModItems.FLYING_ARTIFACT.get())) {
            return true;
        }
        if (stack.getItem() instanceof ArtifactCatalogItem artifactItem) {
            // M15: 语料飞行能力 + 硬编码兼容旧 id。
            if (com.xunxian.seekingimmortals.artifact.ArtifactDataService.builtin()
                    .isFlyingCapable(artifactItem.artifactId())) {
                return true;
            }
            return switch (artifactItem.artifactId()) {
                case "flying_sword_low", "silver_giant_sword", "wind_escape_sail", "cloud_boots" -> true;
                default -> false;
            };
        }
        if (stack.getItem() instanceof com.xunxian.seekingimmortals.item.FlyingArtifactItem flying) {
            return com.xunxian.seekingimmortals.artifact.ArtifactDataService.builtin()
                    .isFlyingCapable(flying.artifactId());
        }
        return false;
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

    private static void refreshCultivationAttributeState(Player player, PlayerCultivation cultivation) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        float previousMaxHealth = player.getMaxHealth();
        if (maxHealth != null) {
            AttributeModifier oldModifier = maxHealth.getModifier(SEVERE_INJURY_HEALTH_UUID);
            if (oldModifier != null) {
                maxHealth.removeModifier(SEVERE_INJURY_HEALTH_UUID);
            }
        }

        applyAdditiveAttribute(player, Attributes.MAX_HEALTH, CULTIVATION_HEALTH_UUID,
                "seeking_immortals_cultivation_health",
                Math.max(20.0D, cultivation.getMaxHealthPoints()) - 20.0D);
        applyAdditiveAttribute(player, Attributes.ATTACK_DAMAGE, CULTIVATION_ATTACK_UUID,
                "seeking_immortals_cultivation_attack",
                Math.max(0.0D, cultivation.getMeleeAttackPower()));
        applyAdditiveAttribute(player, Attributes.ARMOR, CULTIVATION_ARMOR_UUID,
                "seeking_immortals_cultivation_armor",
                Math.max(0.0D, cultivation.getDefensePower() / 35.0D));
        applyAdditiveAttribute(player, Attributes.ARMOR_TOUGHNESS, CULTIVATION_ARMOR_TOUGHNESS_UUID,
                "seeking_immortals_cultivation_armor_toughness",
                Math.max(0.0D, cultivation.getDefensePower() / 140.0D));
        applyAdditiveAttribute(player, Attributes.KNOCKBACK_RESISTANCE, CULTIVATION_KNOCKBACK_RESISTANCE_UUID,
                "seeking_immortals_cultivation_knockback_resistance",
                Math.max(0.0D, cultivation.getDefensePower() / 1000.0D));
        applyAdditiveAttribute(player, Attributes.MOVEMENT_SPEED, CULTIVATION_MOVEMENT_SPEED_UUID,
                "seeking_immortals_cultivation_movement_speed",
                cultivation.getEffectiveMovementSpeedBonus());

        if (maxHealth != null && cultivation.hasSevereInjury()) {
            maxHealth.addTransientModifier(new AttributeModifier(
                    SEVERE_INJURY_HEALTH_UUID,
                    "seeking_immortals_severe_injury_health",
                    -0.80D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        float currentMaxHealth = player.getMaxHealth();
        if (player.getHealth() > currentMaxHealth) {
            player.setHealth(currentMaxHealth);
        } else if (currentMaxHealth > previousMaxHealth && player.getHealth() >= previousMaxHealth - 0.5F) {
            player.setHealth(currentMaxHealth);
        }
    }

    private static void applyAdditiveAttribute(Player player, Attribute attribute, UUID uuid, String name, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier oldModifier = instance.getModifier(uuid);
        if (oldModifier != null && Math.abs(oldModifier.getAmount() - amount) < 0.0001D) {
            return;
        }
        if (oldModifier != null) {
            instance.removeModifier(uuid);
        }
        if (Math.abs(amount) > 0.0001D) {
            instance.addTransientModifier(new AttributeModifier(
                    uuid,
                    name,
                    amount,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static void handleImmortalAfflictions(Player player, com.xunxian.seekingimmortals.cultivation.PlayerCultivation cultivation) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && cultivation.hasSevereInjury()) {
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

        if (cultivation.tickHeartDemonTimer(player.getRandom())) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0, false, true));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.affliction.heart_demon.trigger", cultivation.getHeartDemonLevel()), false);
        }
    }

    private static void handlePhysiqueDefects(ServerPlayer player, PlayerCultivation cultivation) {
        if (player.tickCount % 200 != 0 || !cultivation.getSpecialPhysique().hasDefect()) return;
        if (cultivation.getSpecialPhysique() == SpecialPhysique.DRAGON_CHANT_BODY) {
            int risk = cultivation.getRealm().ordinal() < Realm.CORE_FORMATION.ordinal() ? 2 : 1;
            cultivation.addQiDeviationRisk(risk);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.physique.dragon_chant_defect",
                    risk, cultivation.getQiDevRisk()), true);
            return;
        }
        if (cultivation.getSpecialPhysique() == SpecialPhysique.ICE_MARROW_BODY
                && !cultivation.getSpiritualRootAttributes().contains(SpiritualRootAttribute.ICE)
                && !cultivation.getSpiritualRootAttributes().contains(SpiritualRootAttribute.WATER)) {
            cultivation.addQiDeviationRisk(1);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false));
            if (player.getHealth() > 2.0F) {
                player.hurt(player.damageSources().freeze(), 1.0F);
            }
            player.displayClientMessage(Component.translatable("message.seeking_immortals.physique.ice_marrow_defect",
                    cultivation.getQiDevRisk()), true);
        }
    }

    private static void handleCatalogPillTimers(Player player) {
        CompoundTag data = player.getPersistentData();
        int fastingTicks = data.getInt(CatalogPillItem.FASTING_TICKS_KEY);
        if (fastingTicks > 0) {
            data.putInt(CatalogPillItem.FASTING_TICKS_KEY, fastingTicks - 1);
            if (player.getFoodData().getFoodLevel() < 18) {
                player.getFoodData().setFoodLevel(18);
            }
            if (player.getFoodData().getSaturationLevel() < 2.0F) {
                player.getFoodData().setSaturation(2.0F);
            }
        }
        int forgetTicks = data.getInt(CatalogPillItem.FORGET_DUST_TICKS_KEY);
        if (forgetTicks > 0) {
            data.putInt(CatalogPillItem.FORGET_DUST_TICKS_KEY, forgetTicks - 1);
        }
        int pressureResistTicks = data.getInt(CatalogPillItem.PRESSURE_RESIST_TICKS_KEY);
        if (pressureResistTicks > 1) {
            data.putInt(CatalogPillItem.PRESSURE_RESIST_TICKS_KEY, pressureResistTicks - 1);
        } else if (pressureResistTicks == 1) {
            data.remove(CatalogPillItem.PRESSURE_RESIST_TICKS_KEY);
        }
        int yinProtectionTicks = data.getInt(CatalogPillItem.YIN_PROTECTION_TICKS_KEY);
        if (yinProtectionTicks > 1) {
            data.putInt(CatalogPillItem.YIN_PROTECTION_TICKS_KEY, yinProtectionTicks - 1);
        } else if (yinProtectionTicks == 1) {
            data.remove(CatalogPillItem.YIN_PROTECTION_TICKS_KEY);
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
            data.putBoolean("SeekingImmortalsLifespanDeath", true);
            try {
                player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            } finally {
                data.remove("SeekingImmortalsLifespanDeath");
            }
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

    @Deprecated
    private static boolean tryExchange(Player player, Item input, Item output) {
        return SpiritStoneLadderService.exchange(player, input, output, SpiritStoneLadderService.ratioPerTier());
    }
}
