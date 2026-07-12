package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

/**
 * Summon runtime for text-material summon/puppet techniques.
 * Spawns a real custom servitor entity (not vanilla attack projectiles).
 */
public final class SummonHonestMvpService {
    private SummonHonestMvpService() {}

    public static int puppetDefinitionCount() {
        return LoreCatalogService.builtin().puppetDefinitions().size();
    }

    public static Optional<LoreCatalogService.Entry> findPuppet(String id) {
        return Optional.ofNullable(LoreCatalogService.builtin().puppetDefinitions().get(id == null ? "" : id));
    }

    public static boolean summonProxy(ServerPlayer player, String puppetOrSummonId) {
        String id = puppetOrSummonId == null ? "" : puppetOrSummonId.trim().toLowerCase(Locale.ROOT);
        Optional<LoreCatalogService.Entry> puppet = findPuppet(id);
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            int amp = 0;
            int duration = 20 * 20;
            double health = 24.0D;
            double damage = 4.5D;
            if (puppet.isPresent()) {
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

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, Math.min(1, amp)));

            boolean spawned = spawnServitor(player, id, duration, health, damage);
            String display = puppet.map(LoreCatalogService.Entry::display).filter(s -> !s.isBlank()).orElse(id);
            if (spawned) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.entity_spawned", display), true);
            } else {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.honest_mvp", display), true);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.summon.entity_pending"), false);
            }
            ok[0] = true;
        });
        return ok[0];
    }

    /**
     * Public spawn helper for craft stations (puppet assembly) and spells.
     */
    public static boolean spawnConfigured(ServerPlayer player, String summonId, int lifeTicks, double health, double damage) {
        return spawnServitor(player, summonId, lifeTicks, health, damage);
    }

    private static boolean spawnServitor(ServerPlayer player, String summonId, int lifeTicks, double health, double damage) {
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
        SummonedServitorEntity.Archetype archetype = archetypeOf(summonId);
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
        return level.addFreshEntity(servitor);
    }

    public static SummonedServitorEntity.Archetype archetypeOf(String summonId) {
        String id = summonId == null ? "" : summonId.trim().toLowerCase(Locale.ROOT);
        if (id.contains("beast") || id.contains("wolf") || id.contains("tiger") || id.contains("bird")
                || id.contains("serpent") || id.contains("dragon") || id.contains("yuling")) {
            return SummonedServitorEntity.Archetype.BEAST;
        }
        if (id.contains("puppet") || id.contains("golem") || id.contains("wood") || id.contains("mech")
                || id.contains("kuilei") || id.contains("assemble")) {
            return SummonedServitorEntity.Archetype.PUPPET;
        }
        if (id.contains("ghost") || id.contains("soul") || id.contains("corpse") || id.contains("yin")
                || id.contains("spirit") || id.contains("wraith") || id.contains("gui")) {
            return SummonedServitorEntity.Archetype.GHOST;
        }
        return SummonedServitorEntity.Archetype.GENERIC;
    }
}
