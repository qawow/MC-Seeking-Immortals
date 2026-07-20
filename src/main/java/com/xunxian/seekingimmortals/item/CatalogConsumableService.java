package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import com.xunxian.seekingimmortals.artifact.ArtifactStorageService;
import com.xunxian.seekingimmortals.catalog.FlightVehicleService;
import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyFormulaItem;
import com.xunxian.seekingimmortals.item.pill.CatalogPillItem;
import com.xunxian.seekingimmortals.item.pill.PillEffectCatalog;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.combat.status.PoisonAntidoteService;
import com.xunxian.seekingimmortals.sect.SectContributionTokenService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CatalogConsumableService {
    public static final String LIGHTNING_WARD_CHARGES_KEY = "SeekingImmortalsLightningWardCharges";
    public static final String TRIBULATION_DAMAGE_MARKER_KEY = "SeekingImmortalsTribulationDamage";
    public static final String FERTILIZER_CHARGES_KEY = "SeekingImmortalsFertilizerCharges";
    private static final float LIGHTNING_REMAINING_MULTIPLIER = 0.35F;

    public enum UseResult {
        SUCCESS,
        FAILED
    }

    private CatalogConsumableService() {}

    public static UseResult use(ServerPlayer player, ItemStack stack, net.minecraft.world.InteractionHand hand,
                                String catalogId, String effect, String realmMin) {
        if (player == null || stack == null || stack.isEmpty()) {
            return UseResult.FAILED;
        }
        if (!meetsRealm(player, realmMin)) {
            return UseResult.FAILED;
        }
        String id = normalize(catalogId);
        String action = normalize(effect);
        boolean success = switch (action) {
            case "minor_spirit_regen" -> minorSpiritRegen(player);
            case "courage_buff_short" -> courageBuff(player);
            case "satiation_plus_strength" -> satiationAndStrength(player);
            case "lightning_mitigate_once" -> armLightningWard(player, 1);
            case "formation_tribulation_buff" -> armLightningWard(player, 3);
            case "pet_loyalty_plus" -> BeastContractService.feedFromConsumable(player);
            case "contains_low_stone_10", "open_low_spirit_stone_pouch" ->
                    giveCatalogItem(player, "spirit_stone_low", 10)
                            || giveCatalogItem(player, "spirit_stone_shard", 10);
            case "spirit_stone_mid_bundle" -> giveCatalogItem(player, "spirit_stone_mid", 4);
            case "spirit_stone_high_bundle" -> giveCatalogItem(player, "spirit_stone_high", 2);
            case "forced_escape_once", "short_escape" -> escape(player);
            case "reveal_spirit_roots" -> revealRoots(player, stack);
            case "random_recipe_low_tier" -> giveRandomRecipe(player);
            case "portable_storage_9", "portable_storage_18", "portable_storage_27" ->
                    ArtifactStorageService.usePortableStorage(player, stack, hand);
            case "open_random_contraband" -> giveContraband(player);
            case "yin_corruption_mitigate", "demon_qi_resist" -> protectFromYin(player);
            case "herb_growth_speed" -> armFertilizer(player);
            case "dual_cultivation_mood" -> applyIncense(player);
            case "fireball_cast" -> castProjectile(player, CultivationFireballEntity.SpellElement.FIRE, 10);
            case "ice_shard_cast" -> castProjectile(player, CultivationFireballEntity.SpellElement.ICE_SPEAR, 8);
            case "golden_armor" -> castArmor(player);
            case "restore_spirit" -> restoreSpirit(player);
            case "restore_health" -> restoreHealth(player);
            case "clear_poison" -> clearPoison(player);
            case "smoke_screen" -> smokeScreen(player);
            case "sound_beacon" -> soundBeacon(player);
            case "random_talisman_low" -> giveRandomTalisman(player, 1);
            case "random_talisman_mid" -> giveRandomTalisman(player, 2);
            case "random_talisman_high" -> giveRandomTalisman(player, 3);
            case "detox_minor" -> clearMinorPoison(player);
            case "talisman_craft_material" -> explainTalismanInk(player);
            case "array_fuel" -> explainFormationFuel(player);
            case "corpse_control" -> SummonHonestMvpService.empowerNearestOwnedGhost(player);
            case "vehicle_craft" -> FlightVehicleService.craftWindFeatherRaftTicket(player);
            case "sect_contribution_redeem" -> SectContributionTokenService.redeem(player);
            case "travel_spirit_boat" -> travelRegion(player, "chaotic_sea",
                    "message.seeking_immortals.catalog_consumable.travel_spirit_boat");
            case "travel_nether_ferry" -> travelRegion(player, "nether_river",
                    "message.seeking_immortals.catalog_consumable.travel_nether_ferry");
            case "travel_chaotic_sea" -> travelRegion(player, "chaotic_sea",
                    "message.seeking_immortals.catalog_consumable.travel_chaotic_sea");
            case "travel_diyuan" -> enterRealmWithToken(player, "diyuan",
                    "message.seeking_immortals.catalog_consumable.travel_diyuan");
            case "deploy_spirit_gather_disk" -> deploySpiritGatherDisk(player);
            case "open_auction_invite" -> openAuctionInvite(player);
            case "show_sect_identity" -> showSectIdentity(player);
            case "island_trade_tax_paid", "star_palace_tax_paid" -> starPalaceTax(player);
            case "star_palace_patrol" -> starPalacePatrol(player);
            case "discover_void_palace" -> discoverLore(player, "A4_void_palace_built",
                    "message.seeking_immortals.catalog_consumable.discover_void_palace");
            case "discover_fallen_demon" -> discoverLore(player, "E_ancient_demon_seal_weak",
                    "message.seeking_immortals.catalog_consumable.discover_fallen_demon");
            case "discover_kunwu" -> discoverLore(player, "A1_kunwu_peak",
                    "message.seeking_immortals.catalog_consumable.discover_kunwu");
            case "inscribe_formula" -> inscribeFormula(player, id);
            case "redeem_spirit_pill_voucher" -> redeemSpiritPillVoucher(player);
            default -> knownIdAction(player, id);
        };
        if (success && shouldAnnounceGenericSuccess(action)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.success", stack.getHoverName()), true);
        }
        return success ? UseResult.SUCCESS : UseResult.FAILED;
    }

    public static void markTribulationDamage(ServerPlayer player) {
        if (player != null) {
            player.getPersistentData().putBoolean(TRIBULATION_DAMAGE_MARKER_KEY, true);
        }
    }

    public static void clearTribulationDamage(ServerPlayer player) {
        if (player != null) {
            player.getPersistentData().remove(TRIBULATION_DAMAGE_MARKER_KEY);
        }
    }

    public static float mitigateLightningDamage(ServerPlayer player, DamageSource source, float amount) {
        if (player == null || amount <= 0.0F) {
            return amount;
        }
        boolean vanillaLightning = source != null && source.is(DamageTypes.LIGHTNING_BOLT);
        boolean tribulation = player.getPersistentData().getBoolean(TRIBULATION_DAMAGE_MARKER_KEY);
        if (!isLightningStrike(vanillaLightning, tribulation)) {
            return amount;
        }
        int charges = lightningWardCharges(player);
        if (charges <= 0) {
            return amount;
        }
        player.getPersistentData().putInt(LIGHTNING_WARD_CHARGES_KEY, charges - 1);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.lightning_triggered", charges - 1), true);
        return amount * lightningDamageMultiplier(charges, true);
    }

    public static float lightningDamageMultiplier(int charges, boolean strike) {
        return strike && charges > 0 ? LIGHTNING_REMAINING_MULTIPLIER : 1.0F;
    }

    public static boolean isLightningStrike(boolean vanillaLightning, boolean tribulationMarker) {
        return vanillaLightning || tribulationMarker;
    }

    static boolean shouldConsumeOnSuccess(String effect, int storageSlots) {
        if (storageSlots > 0) {
            return false;
        }
        String action = normalize(effect);
        // Durable credentials/materials survive use; everything else is one-shot.
        return !"talisman_craft_material".equals(action)
                && !"array_fuel".equals(action)
                && !"show_sect_identity".equals(action)
                && !"open_auction_invite".equals(action);
    }

    public static int lightningWardCharges(Player player) {
        return player == null ? 0 : Math.max(0, player.getPersistentData().getInt(LIGHTNING_WARD_CHARGES_KEY));
    }

    public static boolean hasFertilizerCharge(Player player) {
        return player != null && player.getPersistentData().getInt(FERTILIZER_CHARGES_KEY) > 0;
    }

    public static boolean consumeFertilizerCharge(ServerPlayer player) {
        if (!hasFertilizerCharge(player)) {
            return false;
        }
        int remaining = Math.max(0, player.getPersistentData().getInt(FERTILIZER_CHARGES_KEY) - 1);
        player.getPersistentData().putInt(FERTILIZER_CHARGES_KEY, remaining);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.fertilizer_used", remaining), true);
        return true;
    }

    private static boolean knownIdAction(ServerPlayer player, String id) {
        if ("spirit_beast_feed".equals(id) || "beast_feed_spirit".equals(id)) {
            return BeastContractService.feedFromConsumable(player);
        }
        return false;
    }

    private static boolean meetsRealm(ServerPlayer player, String realmMin) {
        if (realmMin == null || realmMin.isBlank()) {
            return true;
        }
        Realm required = Realm.fromDesignId(realmMin);
        boolean allowed = required != null && CultivationHelper.get(player)
                .map(cultivation -> cultivation.getRealm().ordinal() >= required.ordinal())
                .orElse(false);
        if (!allowed) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.realm_too_low",
                    required == null ? realmMin : required.getDisplayName()), true);
        }
        return allowed;
    }

    private static boolean minorSpiritRegen(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            cultivation.addSpiritualPower(Math.max(1, (int) Math.round(
                    cultivation.getMaxSpiritualPower() * 0.12D)));
            player.getFoodData().eat(4, 0.4F);
            sync(player, cultivation);
            return true;
        }).orElse(false);
    }

    private static boolean courageBuff(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 15, 0));
        return true;
    }

    private static boolean satiationAndStrength(ServerPlayer player) {
        player.getFoodData().eat(10, 0.8F);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 45, 0));
        return true;
    }

    private static boolean armLightningWard(ServerPlayer player, int charges) {
        int current = lightningWardCharges(player);
        int next = Math.min(12, current + Math.max(1, charges));
        if (next == current) {
            return false;
        }
        player.getPersistentData().putInt(LIGHTNING_WARD_CHARGES_KEY, next);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.lightning_ready", next), true);
        return true;
    }

    private static boolean escape(ServerPlayer player) {
        Vec3 destination = findSafeDestination(player);
        if (destination == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.escape_failed"), true);
            return false;
        }
        if (!player.randomTeleport(destination.x, destination.y, destination.z, true)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.escape_failed"), true);
            return false;
        }
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
        return true;
    }

    private static Vec3 findSafeDestination(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = origin.getX() + player.getRandom().nextInt(17) - 8;
            int z = origin.getZ() + player.getRandom().nextInt(17) - 8;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (level.getBlockState(feet).isAir()
                    && level.getBlockState(feet.above()).isAir()
                    && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                return new Vec3(x + 0.5D, y, z + 0.5D);
            }
        }
        return null;
    }

    private static boolean revealRoots(ServerPlayer player, ItemStack stack) {
        if (CultivationHelper.get(player).isEmpty()) {
            return false;
        }
        LingGenTestStoneItem.testPlayer(player.serverLevel(), player, player, stack, false, true);
        return true;
    }

    private static boolean giveRandomRecipe(ServerPlayer player) {
        List<String> recipes = List.of(
                "recipe_bigu", "recipe_spirit_condense", "recipe_calm_spirit",
                "recipe_huoyuan", "recipe_forget_dust", "recipe_dingyan",
                "recipe_spirit_recovery", "recipe_body_tempering", "recipe_antidote");
        String id = recipes.get(player.getRandom().nextInt(recipes.size()));
        return giveCatalogItem(player, id, 1);
    }

    private static boolean giveContraband(ServerPlayer player) {
        List<String> pool = List.of("contraband_spirit_stone", "spirit_stone_shard",
                "beast_core", "yin_stone", "demon_core_fragment");
        boolean delivered = false;
        int rolls = 1 + player.getRandom().nextInt(2);
        for (int index = 0; index < rolls; index++) {
            String id = pool.get(player.getRandom().nextInt(pool.size()));
            delivered |= giveCatalogItem(player, id, 1 + player.getRandom().nextInt(2));
        }
        return delivered;
    }

    private static boolean protectFromYin(ServerPlayer player) {
        player.getPersistentData().putInt(CatalogPillItem.YIN_PROTECTION_TICKS_KEY, 12000);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.POISON);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        return true;
    }

    private static boolean armFertilizer(ServerPlayer player) {
        int current = Math.max(0, player.getPersistentData().getInt(FERTILIZER_CHARGES_KEY));
        if (current >= 8) {
            return false;
        }
        player.getPersistentData().putInt(FERTILIZER_CHARGES_KEY, current + 1);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.fertilizer_ready", current + 1), true);
        return true;
    }

    private static boolean applyIncense(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            cultivation.addCultivationBoost(20 * 60 * 10, 1.15D);
            sync(player, cultivation);
            return true;
        }).orElse(false);
    }

    private static boolean castProjectile(ServerPlayer player,
                                          CultivationFireballEntity.SpellElement element,
                                          int qiCost) {
        return CultivationHelper.get(player).map(cultivation -> {
            int spiritualPowerBeforeCast = cultivation.getSpiritualPower();
            if (!cultivation.consumeQi(qiCost)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.not_enough_qi"), true);
                return false;
            }
            double damage = element == CultivationFireballEntity.SpellElement.ICE_SPEAR ? 6.0D : 5.0D;
            CultivationFireballEntity projectile = new CultivationFireballEntity(
                    player.level(), player, player.getLookAngle(), damage, 1.1D, element);
            if (!player.level().addFreshEntity(projectile)) {
                cultivation.setSpiritualPower(spiritualPowerBeforeCast);
                sync(player, cultivation);
                return false;
            }
            sync(player, cultivation);
            return true;
        }).orElse(false);
    }

    private static boolean castArmor(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            int spiritualPowerBeforeCast = cultivation.getSpiritualPower();
            if (!cultivation.consumeQi(8)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.not_enough_qi"), true);
                return false;
            }
            if (!player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0))) {
                cultivation.setSpiritualPower(spiritualPowerBeforeCast);
                sync(player, cultivation);
                return false;
            }
            sync(player, cultivation);
            return true;
        }).orElse(false);
    }

    private static boolean restoreSpirit(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            int current = cultivation.getSpiritualPower();
            int maximum = cultivation.getMaxSpiritualPower();
            if (current >= maximum) {
                return false;
            }
            cultivation.addSpiritualPower(Math.max(1, maximum / 4));
            sync(player, cultivation);
            return true;
        }).orElse(false);
    }

    private static boolean restoreHealth(ServerPlayer player) {
        player.heal(8.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
        return true;
    }

    private static boolean clearPoison(ServerPlayer player) {
        boolean hadEffect = player.hasEffect(MobEffects.POISON)
                || player.hasEffect(MobEffects.WITHER)
                || player.hasEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.CONFUSION);
        return hadEffect;
    }

    private static boolean clearMinorPoison(ServerPlayer player) {
        boolean cleared = player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.CONFUSION);
        return PoisonAntidoteService.applyAntidote(player, "bailian_jiedu") || cleared;
    }

    private static boolean explainTalismanInk(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.talisman_ink_material"), true);
        return true;
    }

    private static boolean explainFormationFuel(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.formation_fuel_material"), true);
        return true;
    }

    private static boolean smokeScreen(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.6D, 0.7D, 0.6D, 0.04D);
        return true;
    }

    private static boolean soundBeacon(ServerPlayer player) {
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        for (ServerPlayer recipient : player.server.getPlayerList().getPlayers()) {
            if (recipient.level() == player.level() && recipient.distanceToSqr(player) <= 64.0D * 64.0D) {
                recipient.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.catalog_consumable.sound_beacon", player.getDisplayName()), false);
            }
        }
        return true;
    }

    private static boolean giveRandomTalisman(ServerPlayer player, int tier) {
        Item item = switch (player.getRandom().nextInt(3)) {
            case 0 -> ModItems.FIRE_TALISMAN.get();
            case 1 -> ModItems.ARMOR_TALISMAN.get();
            default -> ModItems.SPEED_TALISMAN.get();
        };
        return giveItem(player, new ItemStack(item, Math.max(1, tier)));
    }


    private static boolean travelRegion(ServerPlayer player, String regionId, String successKey) {
        boolean ok = com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService.travel(player, regionId);
        if (ok && successKey != null && !successKey.isBlank()) {
            player.displayClientMessage(Component.translatable(successKey), true);
        }
        return ok;
    }

    private static boolean enterRealmWithToken(ServerPlayer player, String realmId, String successKey) {
        // Access tokens are physical entry credentials; entry itself stays server-authoritative.
        boolean ok = com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService.enterSecretRealm(player, realmId);
        if (ok && successKey != null && !successKey.isBlank()) {
            player.displayClientMessage(Component.translatable(successKey), true);
        }
        return ok;
    }

    private static boolean deploySpiritGatherDisk(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        boolean ok = com.xunxian.seekingimmortals.structure.FormationFieldService.activateFreeField(
                level,
                player.blockPosition(),
                com.xunxian.seekingimmortals.structure.FormationFieldService.FieldKind.SPIRIT_GATHER,
                20 * 90,
                player,
                "spirit_gathering_array_disk");
        player.displayClientMessage(Component.translatable(ok
                ? "message.seeking_immortals.catalog_consumable.spirit_gather_disk"
                : "message.seeking_immortals.catalog_consumable.spirit_gather_disk_failed"), true);
        return ok;
    }

    private static boolean openAuctionInvite(ServerPlayer player) {
        // Soft invitation: merchant-guild introduction plus a hint toward the auction hall.
        com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, "merchant_guild", 2);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.auction_invite"), true);
        return true;
    }

    private static boolean showSectIdentity(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            var progress = cultivation.getSevenMysteriesQuest();
            String sectId = progress.getSectId();
            if (sectId == null || sectId.isBlank()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.catalog_consumable.sect_identity_none"), true);
                return false;
            }
            String role = progress.getSectRole();
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.sect_identity",
                    sectId, role == null || role.isBlank() ? "-" : role), true);
            return true;
        }).orElse(false);
    }

    private static boolean starPalaceTax(ServerPlayer player) {
        com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, "star_palace", 3);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.star_palace_tax"), true);
        return true;
    }

    private static boolean starPalacePatrol(ServerPlayer player) {
        com.xunxian.seekingimmortals.worldpack.ReputationService.add(player, "star_palace", 2);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 0));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.star_palace_patrol"), true);
        return true;
    }

    private static boolean discoverLore(ServerPlayer player, String eventId, String successKey) {
        boolean discovered = com.xunxian.seekingimmortals.catalog.ChronicleTradeSoftService
                .discoverChronicle(player, eventId);
        if (!discovered) {
            // Fail-closed: unknown/failed chronicle keeps the map fragment in hand.
            return false;
        }
        if (successKey != null && !successKey.isBlank()) {
            player.displayClientMessage(Component.translatable(successKey), true);
        }
        return true;
    }

    /**
     * Spirit pill vouchers redeem into a random low-tier restorative/cultivation pill.
     */
    private static boolean redeemSpiritPillVoucher(ServerPlayer player) {
        List<String> pool = List.of(
                "bigu_pill",
                "qi_recovery_pill",
                "cultivation_pill",
                "calming_pill_low",
                "qingxin_pill",
                "spirit_recovery_pill",
                "body_tempering_pill",
                "beast_taming_pill");
        String id = pool.get(player.getRandom().nextInt(pool.size()));
        boolean ok = giveCatalogItem(player, id, 1);
        if (ok) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.voucher_redeem",
                    Component.translatable("item.seeking_immortals." + id)), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.5F, 1.1F);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.voucher_empty"), true);
        }
        return ok;
    }

    /**
     * Blank jade slips and paper formula scrolls inscribe a random dedicated formula carrier
     * of matching media (jade/paper). Sect secrets are never rolled from blanks.
     */
    private static boolean inscribeFormula(ServerPlayer player, String blankId) {
        AlchemyFormulaSource preferred = "jade_slip_blank".equals(normalize(blankId))
                ? AlchemyFormulaSource.JADE
                : AlchemyFormulaSource.PAPER;
        List<Item> pool = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof AlchemyFormulaItem formula
                    && formula.source() == preferred
                    && formula.recipeId() != null
                    && !formula.recipeId().isBlank()) {
                pool.add(item);
            }
        }
        if (pool.isEmpty()) {
            // Fallback: any non-sect formula if preferred media is missing on this client/datapack.
            for (Item item : ForgeRegistries.ITEMS) {
                if (item instanceof AlchemyFormulaItem formula
                        && formula.source() != AlchemyFormulaSource.SECT_SECRET) {
                    pool.add(item);
                }
            }
        }
        if (pool.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.catalog_consumable.inscribe_empty"), true);
            return false;
        }
        Item chosen = pool.get(player.getRandom().nextInt(pool.size()));
        ItemStack product = new ItemStack(chosen, 1);
        InventoryDeliveryService.giveOrEnqueue(player, product, "inscribe_formula:" + normalize(blankId));
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.inscribe_success",
                product.getHoverName()), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.55F, 1.15F);
        return true;
    }

    private static boolean giveCatalogItem(ServerPlayer player, String id, int count) {
        Item item = ItemCatalogService.resolveCatalogItem(id);
        return item != null && giveItem(player, new ItemStack(item, Math.max(1, count)));
    }

    private static boolean giveItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        InventoryDeliveryService.giveOrEnqueue(player, stack, "catalog_consumable");
        return true;
    }

    private static boolean isDurableStorage(String effect) {
        return normalize(effect).startsWith("portable_storage_");
    }

    private static boolean isDeferredMaterial(String effect) {
        String action = normalize(effect);
        return "talisman_craft_material".equals(action) || "array_fuel".equals(action);
    }

    private static boolean shouldAnnounceGenericSuccess(String effect) {
        String action = normalize(effect);
        return !isDurableStorage(action)
                && !isDeferredMaterial(action)
                && !"sect_contribution_redeem".equals(action);
    }

    private static void sync(ServerPlayer player, PlayerCultivation cultivation) {
        SyncCultivationDataPacket.send(player, cultivation);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
