package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.cultivation.BeastContractService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.worldpack.ServitorRegistrySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
                            "message.seeking_immortals.summon.beast_contract_required", beastId), true);
                    return;
                }
                // Prefer contract-scaled summon when possible.
                if (BeastContractService.summon(player, beastId)) {
                    ok[0] = true;
                    return;
                }
            }

            // Wave472: small shard cost for free catalog summons (not creative).
            int shardCost = 1 + Math.min(2, amp);
            if (!player.getAbilities().instabuild && !consumeShards(player, shardCost)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.summon.missing_shards", shardCost), true);
                return;
            }

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.min(duration, 100), Math.min(1, amp)));

            boolean spawned = spawnConfigured(player, id, duration, health, damage, archetype);
            String display = puppet.map(LoreCatalogService.Entry::display).filter(s -> !s.isBlank()).orElse(id);
            if (spawned) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.entity_spawned", display), true);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.summon.archetype", archetype.name().toLowerCase(Locale.ROOT)), false);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp));
                player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.honest_mvp", display), true);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.entity_pending"), false);
            }
            ok[0] = true;
        });
        return ok[0];
    }

    private static boolean consumeShards(ServerPlayer player, int count) {
        var shard = ModItems.SPIRIT_STONE_SHARD.get();
        int remaining = Math.max(1, count);
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(shard)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining <= 0;
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
        return spawnServitor(player, summonId, lifeTicks, health, damage, archetype, crafted);
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
        if (count > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.summon.stance_all",
                    stance.name().toLowerCase(Locale.ROOT), count), true);
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
            SummonedServitorEntity servitor = loadedServitor(player, state);
            if (servitor == null) {
                continue;
            }
            if (servitor.getArchetype() == SummonedServitorEntity.Archetype.BEAST) {
                String beastId = BeastContractService.beastIdFromSummonId(servitor.getSummonId());
                if (!beastId.isBlank()) {
                    float frac = servitor.getMaxLifeTicks() <= 0 ? 0.0F
                            : (float) servitor.getLifeTicksRemaining() / (float) servitor.getMaxLifeTicks();
                    BeastContractService.recordCombatCredit(player, beastId,
                            frac >= 0.5F ? BeastContractService.CreditKind.SURVIVE : BeastContractService.CreditKind.HIT);
                }
            }
            servitor.discard();
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
        for (SummonedServitorEntity puppet : puppets) {
            puppet.repair(8.0F);
            puppet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.puppet.repaired", puppets.size()), true);
        return puppets.size();
    }

    private static boolean consumeRepairMaterial(ServerPlayer player) {
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

    private static boolean spawnServitor(ServerPlayer player, String summonId, int lifeTicks, double health, double damage,
                                         SummonedServitorEntity.Archetype forcedArchetype, boolean crafted) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        enforceConcurrentCap(player);
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
        } else if (archetype == SummonedServitorEntity.Archetype.GHOST) {
            scaledHealth *= 0.90D;
            scaledDamage *= 1.20D;
        }
        servitor.configure(player, summonId, lifeTicks, scaledHealth, scaledDamage, archetype);
        servitor.setCrafted(crafted || archetype == SummonedServitorEntity.Archetype.PUPPET && summonId.startsWith("puppet_"));
        boolean added = level.addFreshEntity(servitor);
        if (added) {
            ServitorRegistrySavedData.get(level).register(
                    player.getUUID(), servitor.getUUID(), level.dimension().location().toString(),
                    servitor.getStance().name(), MAX_ACTIVE_SERVITORS);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.summon.cap_status",
                    countOwnedServitors(player), MAX_ACTIVE_SERVITORS), false);
        }
        return added;
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
            SummonedServitorEntity servitor = loadedServitor(player, state);
            if (servitor != null) {
                servitor.discard();
            }
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.summon.cap_despawn", dismissed.size(), MAX_ACTIVE_SERVITORS), true);
    }

    private static SummonedServitorEntity loadedServitor(ServerPlayer player,
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
        return entity instanceof SummonedServitorEntity servitor ? servitor : null;
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
