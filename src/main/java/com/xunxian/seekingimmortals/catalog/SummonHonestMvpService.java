package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.GhostContractService;
import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.beast.PuppetGrowthService;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.ServitorRegistrySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Summon runtime for text-material summon/puppet techniques.
 * Wave455: concurrent cap, explicit archetype spawn API.
 * Wave458: stance command, dismiss, puppet repair, focus target.
 * Wave472: BEAST path requires BeastContract; non-creative shard cost.
 */
public final class SummonHonestMvpService {
    public static final int MAX_ACTIVE_SERVITORS = 3;

    private SummonHonestMvpService() {}

    public static int puppetDefinitionCount() {
        int rich = com.xunxian.seekingimmortals.beast.PuppetDefinitionService.size();
        if (rich > 0) {
            return rich;
        }
        return LoreCatalogService.builtin().puppetDefinitions().size();
    }

    public static Optional<LoreCatalogService.Entry> findPuppet(String id) {
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        // M10 rich puppet definitions take precedence.
        var rich = com.xunxian.seekingimmortals.beast.PuppetDefinitionService.find(key);
        if (rich.isPresent()) {
            var def = rich.get();
            return Optional.of(new LoreCatalogService.Entry(def.id(), def.display(), def.tier()));
        }
        return Optional.ofNullable(LoreCatalogService.builtin().puppetDefinitions().get(key));
    }

    public static boolean summonProxy(ServerPlayer player, String puppetOrSummonId) {
        String id = puppetOrSummonId == null ? "" : puppetOrSummonId.trim().toLowerCase(Locale.ROOT);
        Optional<LoreCatalogService.Entry> puppet = findPuppet(id);
        Optional<com.xunxian.seekingimmortals.beast.PuppetDefinitionService.PuppetDef> richPuppet =
                com.xunxian.seekingimmortals.beast.PuppetDefinitionService.find(id);
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            int amp = 0;
            int duration = 20 * 20;
            double health = 24.0D;
            double damage = 4.5D;
            if (richPuppet.isPresent()) {
                var def = richPuppet.get();
                health = Math.max(8.0D, def.hpBase());
                damage = Math.max(2.0D, def.damage());
                amp = Math.min(2, def.tierIndex());
                duration = 20 * (20 + def.tierIndex() * 8);
            } else if (puppet.isPresent()) {
                String tier = puppet.get().extra() == null ? "" : puppet.get().extra().toLowerCase(Locale.ROOT);
                if (tier.contains("high") || tier.contains("3") || tier.contains("ancient")) {
                    amp = 2;
                    duration = 20 * 35;
                    health = 40.0D;
                    damage = 7.0D;
                } else if (tier.contains("mid") || tier.contains("2")) {
                    amp = 1;
                    duration = 20 * 28;
                    health = 32.0D;
                    damage = 5.5D;
                }
            } else if (id.contains("legion") || id.contains("ultimate") || id.contains("king") || id.contains("avatar")) {
                amp = 2;
                duration = 20 * 30;
                health = 36.0D;
                damage = 6.5D;
            }

            SummonedServitorEntity.Archetype archetype = richPuppet.isPresent()
                    ? SummonedServitorEntity.Archetype.PUPPET
                    : archetypeOf(id);

            // Wave472: BEAST catalog summons require a real beast contract.
            if (archetype == SummonedServitorEntity.Archetype.BEAST) {
                String beastId = BeastContractService.beastIdFromSummonId(id);
                if (beastId.isBlank()) {
                    beastId = id.startsWith("beast_") ? id.substring("beast_".length()) : id;
                }
                if (!BeastContractService.hasContract(player, beastId)) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.summon.beast_contract_required",
                            summonDisplay(beastId, Optional.empty(), SummonedServitorEntity.Archetype.BEAST)), true);
                    return;
                }
                // Prefer contract-scaled summon when possible.
                if (BeastContractService.summon(player, beastId)) {
                    ok[0] = true;
                    return;
                }
            }

            // Wave472: reserve the shard cost until the real entity has been created.
            int shardCost = 1 + Math.min(2, amp);
            List<ShardReservation> reservation = player.getAbilities().instabuild
                    ? List.of()
                    : reserveShards(player, shardCost).orElse(null);
            if (reservation == null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.summon.missing_shards", shardCost), true);
                return;
            }

            boolean spawned = spawnConfigured(player, id, duration, health, damage, archetype);
            Component display = summonDisplay(id, puppet, archetype);
            if (!spawned) {
                refundShards(player, reservation);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.summon.entity_failed", display), true);
                return;
            }
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.min(duration, 100), Math.min(1, amp)));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.entity_spawned", display), true);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.summon.archetype", archetypeDisplay(archetype)), false);
            ok[0] = true;
        });
        return ok[0];
    }

    private record ShardReservation(int slot, int count) {}

    private static Optional<List<ShardReservation>> reserveShards(ServerPlayer player, int count) {
        var shard = ModItems.SPIRIT_STONE_SHARD.get();
        int required = Math.max(1, count);
        int available = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(shard)) {
                available += stack.getCount();
            }
        }
        if (available < required) {
            return Optional.empty();
        }
        List<ShardReservation> reservations = new ArrayList<>();
        int remaining = required;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(shard)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            reservations.add(new ShardReservation(i, take));
            remaining -= take;
        }
        player.containerMenu.broadcastChanges();
        return Optional.of(List.copyOf(reservations));
    }

    private static void refundShards(ServerPlayer player, List<ShardReservation> reservations) {
        if (player == null || reservations == null || reservations.isEmpty()) {
            return;
        }
        var shard = ModItems.SPIRIT_STONE_SHARD.get();
        for (ShardReservation reservation : reservations) {
            ItemStack stack = player.getInventory().getItem(reservation.slot());
            if (stack.isEmpty()) {
                player.getInventory().setItem(reservation.slot(), new ItemStack(shard, reservation.count()));
            } else if (stack.is(shard) && stack.getCount() + reservation.count() <= stack.getMaxStackSize()) {
                stack.grow(reservation.count());
            } else {
                player.getInventory().placeItemBackInInventory(new ItemStack(shard, reservation.count()));
            }
        }
        player.containerMenu.broadcastChanges();
    }

    public static boolean spawnConfigured(ServerPlayer player, String summonId, int lifeTicks, double health, double damage) {
        return spawnConfigured(player, summonId, lifeTicks, health, damage, archetypeOf(summonId), false);
    }

    public static boolean spawnConfigured(ServerPlayer player, String summonId, int lifeTicks, double health, double damage,
                                          SummonedServitorEntity.Archetype archetype) {
        return spawnConfigured(player, summonId, lifeTicks, health, damage, archetype, false);
    }

    public static boolean spawnConfigured(ServerPlayer player, String summonId, int lifeTicks, double health, double damage,
                                          SummonedServitorEntity.Archetype archetype, boolean crafted) {
        Optional<String> realBeastId = resolveRealBeastId(player, summonId, archetype);
        if (realBeastId.isPresent()) {
            int tier = BeastBestiaryService.find(realBeastId.get())
                    .map(BeastBestiaryService.BeastEntry::tier).orElse(1);
            return spawnBeastConfigured(player, realBeastId.get(), tier, lifeTicks, health, damage);
        }
        return spawnServitor(player, summonId, lifeTicks, health, damage, archetype, crafted);
    }

    public static boolean spawnBeastConfigured(ServerPlayer player, String beastId, int tier, int lifeTicks,
                                               double health, double damage) {
        if (player == null) {
            return false;
        }
        String normalized = beastId == null ? "" : beastId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        String canonical = BeastBestiaryService.find(normalized)
                .map(BeastBestiaryService.BeastEntry::id).orElse(normalized);
        return spawnCompanionBeast(player, canonical, BeastTierService.clampTier(tier),
                lifeTicks, health, damage);
    }

    public static List<SummonedServitorEntity> listOwnedServitors(ServerPlayer player) {
        List<SummonedServitorEntity> list = new ArrayList<>();
        if (player == null || player.getServer() == null) {
            return list;
        }
        ServitorRegistrySavedData registry = ServitorRegistrySavedData.get(player.getServer());
        for (ServitorRegistrySavedData.State state : registry.activeFor(player.getUUID())) {
            SummonedServitorEntity servitor = loadedServitor(player, state);
            if (servitor != null && servitor.isAlive()) {
                list.add(servitor);
            }
        }
        return list;
    }

    public static List<CultivationBeastEntity> listOwnedCompanionBeasts(ServerPlayer player) {
        List<CultivationBeastEntity> list = new ArrayList<>();
        if (player == null || player.getServer() == null) {
            return list;
        }
        ServitorRegistrySavedData registry = ServitorRegistrySavedData.get(player.getServer());
        for (ServitorRegistrySavedData.State state : registry.activeFor(player.getUUID())) {
            CultivationBeastEntity beast = loadedCompanionBeast(player, state);
            if (beast != null && beast.isAlive()) {
                list.add(beast);
            }
        }
        return list;
    }

    public static int countOwnedServitors(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return 0;
        }
        return ServitorRegistrySavedData.get(player.getServer()).countActive(player.getUUID());
    }

    public static int setStanceAll(ServerPlayer player, SummonedServitorEntity.Stance stance) {
        if (player == null || player.getServer() == null) {
            return 0;
        }
        ServitorRegistrySavedData registry = ServitorRegistrySavedData.get(player.getServer());
        int count = registry.setStanceAll(player.getUUID(), stance.name());
        for (SummonedServitorEntity servitor : listOwnedServitors(player)) {
            servitor.setStance(stance);
        }
        for (CultivationBeastEntity beast : listOwnedCompanionBeasts(player)) {
            beast.setStance(stance);
        }
        if (count > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.summon.stance_all",
                    stanceDisplay(stance), count), true);
        }
        return count;
    }

    public static int dismissOwned(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return 0;
        }
        ServitorRegistrySavedData registry = ServitorRegistrySavedData.get(player.getServer());
        List<ServitorRegistrySavedData.State> dismissed = registry.dismissAll(player.getUUID());
        for (ServitorRegistrySavedData.State state : dismissed) {
            net.minecraft.world.entity.Entity entity = loadedOwnedEntity(player, state);
            if (entity == null) {
                continue;
            }
            if (entity instanceof CultivationBeastEntity beast) {
                beast.recordDismissCredit();
            } else if (entity instanceof SummonedServitorEntity servitor
                    && servitor.getArchetype() == SummonedServitorEntity.Archetype.BEAST) {
                String beastId = BeastContractService.beastIdFromSummonId(servitor.getSummonId());
                if (!beastId.isBlank()) {
                    float frac = servitor.getMaxLifeTicks() <= 0 ? 0.0F
                            : (float) servitor.getLifeTicksRemaining() / (float) servitor.getMaxLifeTicks();
                    BeastContractService.recordCombatCredit(player, beastId,
                            frac >= 0.5F ? BeastContractService.CreditKind.SURVIVE : BeastContractService.CreditKind.HIT);
                }
            }
            entity.discard();
        }
        int count = dismissed.size();
        if (count > 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.dismissed", count), true);
        }
        return count;
    }

    public static int focusTarget(ServerPlayer player, LivingEntity target) {
        if (target == null || !target.isAlive() || target == player) {
            return 0;
        }
        int count = 0;
        for (SummonedServitorEntity servitor : listOwnedServitors(player)) {
            servitor.setTarget(target);
            count++;
        }
        for (CultivationBeastEntity beast : listOwnedCompanionBeasts(player)) {
            if (beast.trySetCommandTarget(target)) {
                count++;
            }
        }
        return count;
    }

    public static int repairOwnedPuppets(ServerPlayer player) {
        List<SummonedServitorEntity> puppets = listOwnedServitors(player).stream()
                .filter(s -> s.getArchetype() == SummonedServitorEntity.Archetype.PUPPET)
                .toList();
        if (puppets.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.puppet.repair_none"), false);
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            if (!consumeRepairMaterial(player)) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.puppet.repair_missing"), false);
                return 0;
            }
        }
        boolean coreForgeReady = puppetCoreForgeReady(player);
        Set<String> progressed = new LinkedHashSet<>();
        int refinements = 0;
        boolean blocked = false;
        for (SummonedServitorEntity puppet : puppets) {
            puppet.repair(8.0F);
            puppet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            String puppetId = PuppetGrowthService.puppetIdFromSummonId(puppet.getSummonId());
            if (progressed.add(puppetId)) {
                PuppetGrowthService.GrowthResult growth = PuppetGrowthService.recordRepair(
                        player, puppetId, coreForgeReady);
                refinements += growth.update().evolutionsGained();
                blocked |= growth.update().evolutionBlocked();
            }
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.puppet.repaired", puppets.size()), true);
        if (refinements > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.puppet.refined", refinements), true);
        } else if (blocked) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.puppet.core_forge_required"), false);
        }
        return puppets.size();
    }

    public static boolean empowerNearestOwnedGhost(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        SummonedServitorEntity ghost = listOwnedServitors(player).stream()
                .filter(servitor -> servitor.getArchetype() == SummonedServitorEntity.Archetype.GHOST)
                .filter(servitor -> servitor.level() == player.level())
                .filter(servitor -> servitor.distanceToSqr(player) <= 16.0D * 16.0D)
                .min(Comparator.comparingDouble(servitor -> servitor.distanceToSqr(player)))
                .orElse(null);
        if (ghost == null) {
            return false;
        }
        ghost.heal(8.0F);
        ghost.extendLife(20 * 60 * 5);
        ghost.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60 * 5, 0));
        ghost.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60 * 5, 0));
        ghost.setStance(SummonedServitorEntity.Stance.GUARD);

        // Wave480: update contract stability and loyalty when empowering
        String ghostId = GhostContractService.extractGhostIdFromSummonId(ghost.getSummonId());
        if (GhostContractService.hasContract(player, ghostId)) {
            GhostContractService.maintainContract(player, ghostId);
        }

        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.catalog_consumable.coffin_nail_success",
                ghost.getDisplayName()), true);
        return true;
    }

    private static boolean consumeRepairMaterial(ServerPlayer player) {
        net.minecraft.world.item.Item repairKit = ItemCatalogService.resolveCatalogItem("puppet_repair_kit");
        if (repairKit != null) {
            for (int index = 0; index < player.getInventory().items.size(); index++) {
                ItemStack stack = player.getInventory().items.get(index);
                if (stack.is(repairKit)) {
                    stack.shrink(1);
                    player.containerMenu.broadcastChanges();
                    return true;
                }
            }
        }
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(ModItems.IRONWOOD.get()) || stack.is(ModItems.PUPPET_CORE_BLANK.get())
                    || stack.is(ModItems.SPIRIT_IRON.get())) {
                stack.shrink(1);
                player.containerMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    private static boolean spawnCompanionBeast(ServerPlayer player, String beastId, int tier, int lifeTicks,
                                               double health, double damage) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        CultivationBeastEntity beast = ModEntities.CULTIVATION_BEAST.get().create(level);
        if (beast == null) {
            return false;
        }
        beast.configureCompanion(player, beastId, tier, lifeTicks, health * 1.10D, damage * 1.15D);
        Vec3 spawn = preferredCompanionBeastSpawn(level, player, beast.getBodyPlan());
        beast.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
        if (!level.noCollision(beast)) {
            Vec3 fallback = forwardSpawn(player);
            beast.moveTo(fallback.x, fallback.y, fallback.z, player.getYRot(), 0.0F);
        }
        if (!level.noCollision(beast)) {
            return false;
        }
        boolean added = level.addFreshEntity(beast);
        if (added) {
            enforceConcurrentCap(player);
            ServitorRegistrySavedData.get(level).register(
                    player.getUUID(), beast.getUUID(), level.dimension().location().toString(),
                    beast.getStance().name(), MAX_ACTIVE_SERVITORS);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.summon.cap_status",
                    countOwnedServitors(player), MAX_ACTIVE_SERVITORS), false);
        }
        return added;
    }

    private static Vec3 preferredCompanionBeastSpawn(ServerLevel level, ServerPlayer player,
                                                       CultivationBeastEntity.BodyPlan bodyPlan) {
        if (bodyPlan == CultivationBeastEntity.BodyPlan.AQUATIC) {
            BlockPos origin = player.blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    for (int dy = -4; dy <= 3; dy++) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (!level.getFluidState(candidate).is(FluidTags.WATER)
                                || !level.getFluidState(candidate.above()).is(FluidTags.WATER)) {
                            continue;
                        }
                        double distance = candidate.distSqr(origin);
                        if (distance < bestDistance) {
                            best = candidate;
                            bestDistance = distance;
                        }
                    }
                }
            }
            if (best != null) {
                return Vec3.atBottomCenterOf(best).add(0.0D, 0.1D, 0.0D);
            }
        }
        return forwardSpawn(player);
    }

    private static Vec3 forwardSpawn(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        BlockPos spawnPos = player.blockPosition().offset((int) Math.round(look.x * 1.5D), 0,
                (int) Math.round(look.z * 1.5D));
        return new Vec3(spawnPos.getX() + 0.5D, player.getY(), spawnPos.getZ() + 0.5D);
    }

    private static boolean spawnServitor(ServerPlayer player, String summonId, int lifeTicks, double health, double damage,
                                         SummonedServitorEntity.Archetype forcedArchetype, boolean crafted) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        SummonedServitorEntity servitor = ModEntities.SUMMONED_SERVITOR.get().create(level);
        if (servitor == null) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        BlockPos spawnPos = player.blockPosition().offset((int) Math.round(look.x * 1.5D), 0, (int) Math.round(look.z * 1.5D));
        double x = spawnPos.getX() + 0.5D;
        double y = player.getY();
        double z = spawnPos.getZ() + 0.5D;
        servitor.moveTo(x, y, z, player.getYRot(), 0.0F);
        SummonedServitorEntity.Archetype archetype = forcedArchetype == null ? archetypeOf(summonId) : forcedArchetype;
        double scaledHealth = health;
        double scaledDamage = damage;
        if (archetype == SummonedServitorEntity.Archetype.BEAST) {
            scaledHealth *= 1.10D;
            scaledDamage *= 1.15D;
        } else if (archetype == SummonedServitorEntity.Archetype.PUPPET) {
            scaledHealth *= 1.25D;
            scaledDamage *= 0.90D;
            double growthMultiplier = PuppetGrowthService.statMultiplier(player, summonId);
            scaledHealth *= growthMultiplier;
            scaledDamage *= growthMultiplier;
        } else if (archetype == SummonedServitorEntity.Archetype.GHOST) {
            scaledHealth *= 0.90D;
            scaledDamage *= 1.20D;
        }
        servitor.configure(player, summonId, lifeTicks, scaledHealth, scaledDamage, archetype);
        if (archetype == SummonedServitorEntity.Archetype.PUPPET) {
            var growth = PuppetGrowthService.progress(player, summonId);
            servitor.setCustomName(Component.translatable(
                    "entity.seeking_immortals.summoned_servitor.growth_name",
                    summonDisplay(summonId, findPuppet(summonId), archetype),
                    growth.level(), growth.evolutionStage()));
        }
        servitor.setCrafted(crafted || archetype == SummonedServitorEntity.Archetype.PUPPET && summonId.startsWith("puppet_"));
        boolean added = level.addFreshEntity(servitor);
        if (added) {
            enforceConcurrentCap(player);
            ServitorRegistrySavedData.get(level).register(
                    player.getUUID(), servitor.getUUID(), level.dimension().location().toString(),
                    servitor.getStance().name(), MAX_ACTIVE_SERVITORS);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.summon.cap_status",
                    countOwnedServitors(player), MAX_ACTIVE_SERVITORS), false);
        }
        return added;
    }

    private static Component summonDisplay(String id, Optional<LoreCatalogService.Entry> entry,
                                            SummonedServitorEntity.Archetype archetype) {
        String normalized = PlayerDisplayText.normalizeId(id);
        String itemKey = "item.seeking_immortals." + normalized;
        if (!normalized.isBlank() && PlayerDisplayText.hasTranslation(itemKey)) {
            return Component.translatable(itemKey);
        }
        return entry.filter(value -> PlayerDisplayText.isSafe(value.display()))
                .<Component>map(value -> Component.literal(value.display().trim()))
                .orElseGet(() -> archetypeDisplay(archetype));
    }

    private static Component archetypeDisplay(SummonedServitorEntity.Archetype archetype) {
        String suffix = switch (archetype == null ? SummonedServitorEntity.Archetype.GENERIC : archetype) {
            case BEAST -> "beast";
            case PUPPET -> "puppet";
            case GHOST -> "ghost";
            case GENERIC -> "generic";
        };
        return Component.translatable("message.seeking_immortals.summon.archetype." + suffix);
    }

    private static Component stanceDisplay(SummonedServitorEntity.Stance stance) {
        String suffix = switch (stance == null ? SummonedServitorEntity.Stance.FOLLOW : stance) {
            case FOLLOW -> "follow";
            case GUARD -> "guard";
            case AGGRESSIVE -> "aggressive";
            case STAY -> "stay";
        };
        return Component.translatable("message.seeking_immortals.summon.stance." + suffix);
    }

    private static boolean puppetCoreForgeReady(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        String station = "puppet_core_forge";
        if (com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .bestNearbyEfficiency(player, station) > 0.0D) {
            return true;
        }
        com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .tryCommissionNearby(player, station);
        return com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .bestNearbyEfficiency(player, station) > 0.0D;
    }

    private static void enforceConcurrentCap(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return;
        }
        ServitorRegistrySavedData registry = ServitorRegistrySavedData.get(player.getServer());
        int overflow = registry.countActive(player.getUUID()) - (MAX_ACTIVE_SERVITORS - 1);
        if (overflow <= 0) {
            return;
        }
        List<ServitorRegistrySavedData.State> dismissed = registry.dismissOldest(player.getUUID(), overflow);
        for (ServitorRegistrySavedData.State state : dismissed) {
            net.minecraft.world.entity.Entity entity = loadedOwnedEntity(player, state);
            if (entity instanceof CultivationBeastEntity beast) {
                beast.recordDismissCredit();
            }
            if (entity != null) {
                entity.discard();
            }
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.summon.cap_despawn", dismissed.size(), MAX_ACTIVE_SERVITORS), true);
    }

    private static SummonedServitorEntity loadedServitor(ServerPlayer player,
                                                          ServitorRegistrySavedData.State state) {
        net.minecraft.world.entity.Entity entity = loadedOwnedEntity(player, state);
        return entity instanceof SummonedServitorEntity servitor ? servitor : null;
    }

    private static CultivationBeastEntity loadedCompanionBeast(ServerPlayer player,
                                                                ServitorRegistrySavedData.State state) {
        net.minecraft.world.entity.Entity entity = loadedOwnedEntity(player, state);
        return entity instanceof CultivationBeastEntity beast && beast.isCompanion()
                && beast.getOwnerUUID().filter(state.ownerId()::equals).isPresent() ? beast : null;
    }

    private static net.minecraft.world.entity.Entity loadedOwnedEntity(ServerPlayer player,
                                                                       ServitorRegistrySavedData.State state) {
        if (player == null || state == null || player.getServer() == null) {
            return null;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(state.dimensionId());
        if (dimensionId == null) {
            return null;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = player.getServer().getLevel(dimension);
        if (level == null) {
            return null;
        }
        net.minecraft.world.entity.Entity entity = level.getEntity(state.entityId());
        if (entity instanceof SummonedServitorEntity servitor
                && servitor.getOwnerUUID().filter(state.ownerId()::equals).isPresent()) {
            return servitor;
        }
        if (entity instanceof CultivationBeastEntity beast && beast.isCompanion()
                && beast.getOwnerUUID().filter(state.ownerId()::equals).isPresent()) {
            return beast;
        }
        return null;
    }

    static Optional<String> resolveRealBeastId(ServerPlayer player, String summonId,
                                               SummonedServitorEntity.Archetype archetype) {
        if (archetype != SummonedServitorEntity.Archetype.BEAST || summonId == null) {
            return Optional.empty();
        }
        String normalized = summonId.trim().toLowerCase(Locale.ROOT);
        String prefixed = BeastContractService.beastIdFromSummonId(normalized);
        String candidate = prefixed.isBlank() ? normalized : prefixed;
        Optional<BeastBestiaryService.BeastEntry> entry = BeastBestiaryService.find(candidate);
        if (entry.isPresent()) {
            return Optional.of(entry.get().id());
        }
        if (!prefixed.isBlank() && player != null && BeastContractService.hasContract(player, candidate)) {
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    public static SummonedServitorEntity.Archetype archetypeOf(String summonId) {
        String id = summonId == null ? "" : summonId.trim().toLowerCase(Locale.ROOT);
        // M10: catalog puppet ids (e.g. giant_ape_puppet) must win over beast keyword "ape".
        if (com.xunxian.seekingimmortals.beast.PuppetDefinitionService.find(id).isPresent()
                || id.startsWith("puppet_") || id.contains("puppet") || id.contains("golem")
                || id.contains("mech") || id.contains("kuilei") || id.contains("assemble")
                || id.contains("iron_puppet")) {
            return SummonedServitorEntity.Archetype.PUPPET;
        }
        if (id.startsWith("beast_") || id.startsWith("captured_") || id.startsWith("ecology_")
                || id.contains("beast") || id.contains("wolf")
                || id.contains("tiger") || id.contains("bird") || id.contains("serpent") || id.contains("dragon")
                || id.contains("yuling") || id.contains("fox") || id.contains("ape") || id.contains("turtle")) {
            return SummonedServitorEntity.Archetype.BEAST;
        }
        if (id.contains("ghost") || id.contains("soul") || id.contains("corpse") || id.contains("yin")
                || id.contains("wraith") || id.contains("gui") || id.contains("nascent")) {
            return SummonedServitorEntity.Archetype.GHOST;
        }
        return SummonedServitorEntity.Archetype.GENERIC;
    }
}
