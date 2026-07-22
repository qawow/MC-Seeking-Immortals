package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastBossService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.entity.CultivationBeastEntity;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Locale;
import java.util.Set;

/**
 * Typed secret-realm combat shells. Explicit bestiary/boss ids use the dedicated beast entity;
 * puppet, ghost, humanoid, war, and realm-only ids retain SummonedServitorEntity shells.
 */
public final class TrialCombatShellService {
    public static final int DEFAULT_LIFE_TICKS = 20 * 60 * 45;
    private static final String DEDICATED_BEAST_TAG = "seeking_immortals_hostile_trial_beast";
    private static final Set<String> BEAST_CATEGORIES = Set.of("yaoshou", "lingshou", "chong");
    private static final Set<String> EXPLICIT_BEAST_BOSSES = Set.of(
            "blood_jiao_guardian",
            "ice_fire_demon",
            "nether_river_guardian",
            "moon_beast",
            "ancient_beast_sovereign",
            "diyuan_guardian",
            "chaotic_sea_jiao_lord",
            "abyss_jiao",
            "tier_6_spirit_beast_generic");

    private TrialCombatShellService() {}

    public static SummonedServitorEntity.Archetype archetypeFor(String id) {
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        if (key.contains("ghost") || key.contains("yin") || key.contains("nether")
                || key.contains("soul") || key.contains("void") || key.contains("asura")
                || key.contains("palace")) {
            return SummonedServitorEntity.Archetype.GHOST;
        }
        if (key.contains("puppet") || key.contains("kunwu") || key.contains("diyuan")
                || key.contains("golem") || key.contains("iron") || key.contains("bamboo")
                || key.contains("stone")) {
            return SummonedServitorEntity.Archetype.PUPPET;
        }
        if (key.contains("beast") || key.contains("demon") || key.contains("blood")
                || key.contains("mulan") || key.contains("fox") || key.contains("wolf")
                || key.contains("bear") || key.contains("king") || key.contains("dragon")) {
            return SummonedServitorEntity.Archetype.BEAST;
        }
        return SummonedServitorEntity.Archetype.GENERIC;
    }

    public static Mob spawnHostile(
            ServerLevel level,
            BlockPos pos,
            float yRot,
            String shellId,
            double health,
            double damage,
            SummonedServitorEntity.Archetype archetype) {
        if (level == null || pos == null || level.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        String beastId = explicitBeastId(shellId);
        if (!beastId.isBlank()) {
            CultivationBeastEntity beast = spawnDedicatedBeast(
                    level, pos, yRot, beastId, health, damage);
            if (beast != null) {
                return beast;
            }
        }
        SummonedServitorEntity shell = ModEntities.SUMMONED_SERVITOR.get().create(level);
        if (shell == null) {
            return null;
        }
        shell.configureHostileTrial(
                shellId == null || shellId.isBlank() ? "trial_shell" : shellId,
                DEFAULT_LIFE_TICKS,
                health,
                damage,
                archetype == null ? SummonedServitorEntity.Archetype.GENERIC : archetype);
        if (!positionForSpawn(level, shell, pos, yRot, false)) {
            return null;
        }
        return level.addFreshEntity(shell) ? shell : null;
    }

    public static boolean isHostileShell(Mob mob) {
        return mob instanceof SummonedServitorEntity servitor && servitor.isHostileTrial()
                || mob instanceof CultivationBeastEntity
                && mob.getPersistentData().getBoolean(DEDICATED_BEAST_TAG);
    }

    static boolean isExplicitBeastId(String shellId) {
        return !explicitBeastId(shellId).isBlank();
    }

    static String explicitBeastId(String shellId) {
        String candidate = normalizeShellId(shellId);
        if (candidate.isBlank()) {
            return "";
        }
        if (EXPLICIT_BEAST_BOSSES.contains(candidate)) {
            return candidate;
        }
        return BeastBestiaryService.find(candidate)
                .filter(entry -> BEAST_CATEGORIES.contains(entry.category()))
                .filter(entry -> !BeastSpawnTableService.isBanned(entry.id()))
                .map(BeastBestiaryService.BeastEntry::id)
                .orElse("");
    }

    private static String normalizeShellId(String shellId) {
        String candidate = shellId == null ? "" : shellId.trim().toLowerCase(Locale.ROOT);
        for (String prefix : new String[]{"boss_", "guardian_", "patrol_", "add_", "ecology_"}) {
            if (candidate.startsWith(prefix)) {
                return candidate.substring(prefix.length());
            }
        }
        return candidate;
    }

    private static CultivationBeastEntity spawnDedicatedBeast(
            ServerLevel level,
            BlockPos pos,
            float yRot,
            String beastId,
            double health,
            double damage) {
        CultivationBeastEntity beast = ModEntities.CULTIVATION_BEAST.get().create(level);
        if (beast == null) {
            return null;
        }
        int tier = BeastBossService.find(beastId)
                .map(BeastBossService.BossDef::beastTier)
                .orElseGet(() -> BeastBestiaryService.find(beastId)
                        .map(BeastBestiaryService.BeastEntry::tier)
                        .orElse(1));
        BeastTierService.ScaledStats scaled = BeastTierService.scaleStats(tier);
        double safeHealth = Double.isFinite(health) ? Math.max(1.0D, health) : scaled.health();
        double safeDamage = Double.isFinite(damage) ? Math.max(0.0D, damage) : scaled.damage();
        beast.configureWild(beastId, tier);
        setBase(beast, Attributes.MAX_HEALTH, safeHealth);
        setBase(beast, Attributes.ATTACK_DAMAGE, safeDamage);
        beast.setHealth(beast.getMaxHealth());
        beast.getPersistentData().putBoolean(CultivationBeastEntity.TAG_ECOLOGY, false);
        beast.getPersistentData().putBoolean(DEDICATED_BEAST_TAG, true);
        beast.setPersistenceRequired();
        if (!positionForSpawn(level, beast, pos, yRot,
                beast.getBodyPlan() == CultivationBeastEntity.BodyPlan.AQUATIC)) {
            return null;
        }
        return level.addFreshEntity(beast) ? beast : null;
    }

    private static boolean positionForSpawn(ServerLevel level, Mob mob, BlockPos preferred,
                                            float yRot, boolean preferWater) {
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = 3; dy >= -4; dy--) {
                        BlockPos feet = preferred.offset(dx, dy, dz);
                        if (!level.isInWorldBounds(feet)) {
                            continue;
                        }
                        boolean water = preferWater
                                && level.getFluidState(feet).is(FluidTags.WATER)
                                && level.getFluidState(feet.above()).is(FluidTags.WATER);
                        boolean ground = level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                                && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP);
                        if (!water && !ground) {
                            continue;
                        }
                        mob.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, yRot, 0.0F);
                        if (level.noCollision(mob)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void setBase(CultivationBeastEntity beast,
                                net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                double value) {
        AttributeInstance instance = beast.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
