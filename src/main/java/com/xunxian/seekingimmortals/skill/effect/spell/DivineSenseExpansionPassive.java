package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.LifeSkillService;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Wave492: real divine-sense expansion passive.
 * Applies night-vision/glow pulse and stores a persistent sense-range bonus on player NBT.
 */
public class DivineSenseExpansionPassive extends SpellEffect {
    public static final String TAG_ACTIVE = "seeking_immortals_divine_sense_expand";
    public static final String TAG_UNTIL = "seeking_immortals_divine_sense_expand_until";

    public DivineSenseExpansionPassive() {
        super(0, 0, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        int level = skill == null ? LifeSkillService.level(player, SkillType.DIVINE_SENSE_EXPANSION) : Math.max(1, skill.getLevel());
        int duration = 20 * (45 + level * 15);
        long until = player.getServer().overworld().getGameTime() + duration;
        player.getPersistentData().putBoolean(TAG_ACTIVE, true);
        player.getPersistentData().putLong(TAG_UNTIL, until);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.min(duration, 20 * 8), 0, false, true, true));
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("soul");
        vfx.castAt(player.serverLevel(), player);
        vfx.auraAt(player.serverLevel(), player, 1.35D, 24);
        LifeSkillService.grantPractice(player, SkillType.DIVINE_SENSE_EXPANSION, 10, 4);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.spell.divine_sense_expansion.active",
                String.format(java.util.Locale.ROOT, "%.0f", senseRangeMultiplier(player) * 100.0D - 100.0D),
                duration / 20), true);
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!player.getPersistentData().getBoolean(TAG_ACTIVE)) {
            // Skill owned still grants baseline expansion via PlayerCultivation#getDivineConsciousness.
            return player.getCapability(com.xunxian.seekingimmortals.cultivation.CultivationProvider.CULTIVATION)
                    .map(c -> c.hasSkill(SkillType.DIVINE_SENSE_EXPANSION))
                    .orElse(false);
        }
        long until = player.getPersistentData().getLong(TAG_UNTIL);
        long now = player.getServer() == null ? 0L : player.getServer().overworld().getGameTime();
        if (until > 0L && now > until) {
            player.getPersistentData().putBoolean(TAG_ACTIVE, false);
            return player.getCapability(com.xunxian.seekingimmortals.cultivation.CultivationProvider.CULTIVATION)
                    .map(c -> c.hasSkill(SkillType.DIVINE_SENSE_EXPANSION))
                    .orElse(false);
        }
        return true;
    }

    /** 1.5 base with skill, +0.05 per skill level while pulse active (cap 2.0). */
    public static double senseRangeMultiplier(ServerPlayer player) {
        if (player == null) {
            return 1.0D;
        }
        int level = LifeSkillService.level(player, SkillType.DIVINE_SENSE_EXPANSION);
        if (level <= 0) {
            return 1.0D;
        }
        double multi = 1.5D;
        if (player.getPersistentData().getBoolean(TAG_ACTIVE)) {
            multi += Math.min(0.5D, level * 0.05D);
        }
        return Math.min(2.0D, multi);
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.tickCount % 40 != 0) {
            return;
        }
        if (!isActive(player)) {
            return;
        }
        // Soft passive recovery of divine consciousness while expanded.
        player.getCapability(com.xunxian.seekingimmortals.cultivation.CultivationProvider.CULTIVATION).ifPresent(c -> {
            if (c.hasSkill(SkillType.DIVINE_SENSE_EXPANSION)) {
                c.addDivineConsciousness(1);
            }
        });
    }
}
