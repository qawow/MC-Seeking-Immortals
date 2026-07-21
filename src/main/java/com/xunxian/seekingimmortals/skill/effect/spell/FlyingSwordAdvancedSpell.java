package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.FlyingAuthority;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Advanced flying-sword flight. Wave48: three orbiting guard-sword particle visuals while active.
 */
public class FlyingSwordAdvancedSpell extends SpellEffect {
    public static final String ACTIVE_KEY = "SeekingImmortalsFoundationFlyingActive";
    public static final float SPEED = 0.072F;
    public static final int COST_PER_SECOND = 3;
    private static final int GUARD_SWORD_COUNT = 3;

    public FlyingSwordAdvancedSpell() {
        super(0, 20, 0.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(ACTIVE_KEY)) {
            stop(player, "进阶御剑飞行已收束。");
            return true;
        }
        if (cultivation.getRealm().ordinal() < Realm.FOUNDATION_ESTABLISHMENT.ordinal()) {
            player.displayClientMessage(Component.literal("尚未筑基，无法驾驭进阶御剑飞行。"), true);
            return false;
        }
        if (cultivation.getSpiritualPower() < COST_PER_SECOND) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
            return false;
        }
        data.putBoolean(ACTIVE_KEY, true);
        FlyingAuthority.grant(player, FlyingAuthority.SOURCE_FOUNDATION_FLYING, SPEED);
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("metal");
        vfx.castAt(player.serverLevel(), player);
        vfx.auraAt(player.serverLevel(), player, 1.15D, 32);
        player.displayClientMessage(Component.literal("进阶御剑飞行启动，每秒消耗3点灵力。三柄护体飞剑环绕。"), true);
        spawnGuardSwordVisuals(player);
        return true;
    }

    public static void stop(ServerPlayer player, String message) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(ACTIVE_KEY)) return;
        data.remove(ACTIVE_KEY);
        if (player.level() instanceof ServerLevel level) {
            TechniqueVfxPalette.profile("metal").impactAt(level, player.position().add(0.0D, 0.45D, 0.0D));
        }
        FlyingAuthority.revoke(player, FlyingAuthority.SOURCE_FOUNDATION_FLYING, null, 0.0F);
        player.displayClientMessage(Component.literal(message), true);
    }

    /**
     * Wave48: three orbiting END_ROD particle swords while advanced flight is active.
     * Called from ModEvents foundation flying tick.
     */
    public static void tickGuardSwordVisuals(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(ACTIVE_KEY)) {
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        spawnGuardSwordVisuals(player);
    }

    private static void spawnGuardSwordVisuals(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double baseAngle = (player.tickCount % 360) * Math.PI / 180.0D;
        double radius = 1.15D;
        double y = player.getY() + 1.0D;
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile("metal");
        for (int i = 0; i < GUARD_SWORD_COUNT; i++) {
            double angle = baseAngle + (Math.PI * 2.0D * i / GUARD_SWORD_COUNT);
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            vfx.trailAt(level, new Vec3(x, y, z),
                    new Vec3(-Math.sin(angle) * 0.45D, 0.08D, Math.cos(angle) * 0.45D));
        }
        // Trail near body when flying.
        if (player.getAbilities().flying) {
            Vec3 look = player.getLookAngle().scale(-0.4D);
            vfx.trailAt(level, player.position().add(look.x, 0.45D, look.z), player.getDeltaMovement());
        }
    }
}
