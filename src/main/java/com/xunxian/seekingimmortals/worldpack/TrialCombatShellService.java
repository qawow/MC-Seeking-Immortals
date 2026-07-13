package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.Locale;

/**
 * Wave480: typed secret-realm combat shells reusing SummonedServitorEntity archetypes
 * (BEAST/PUPPET/GHOST/GENERIC + existing GeckoLib textures) instead of bare vanilla monsters.
 */
public final class TrialCombatShellService {
    public static final int DEFAULT_LIFE_TICKS = 20 * 60 * 45;

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

    public static SummonedServitorEntity spawnHostile(
            ServerLevel level,
            BlockPos pos,
            float yRot,
            String shellId,
            double health,
            double damage,
            SummonedServitorEntity.Archetype archetype) {
        if (level == null || pos == null) {
            return null;
        }
        SummonedServitorEntity shell = ModEntities.SUMMONED_SERVITOR.get().create(level);
        if (shell == null) {
            return null;
        }
        shell.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yRot, 0.0F);
        shell.configureHostileTrial(
                shellId == null || shellId.isBlank() ? "trial_shell" : shellId,
                DEFAULT_LIFE_TICKS,
                health,
                damage,
                archetype == null ? SummonedServitorEntity.Archetype.GENERIC : archetype);
        level.addFreshEntity(shell);
        return shell;
    }

    public static boolean isHostileShell(Mob mob) {
        return mob instanceof SummonedServitorEntity servitor && servitor.isHostileTrial();
    }
}
