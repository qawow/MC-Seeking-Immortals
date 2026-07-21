package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class MultiSwordArraySpell extends SpellEffect {
    private static final String UNTIL_KEY = "SeekingImmortalsBigDipperUntil";
    private static final String NEXT_KEY = "SeekingImmortalsBigDipperNext";
    private static final String VOLLEY_KEY = "SeekingImmortalsBigDipperVolley";
    private static final String DAMAGE_KEY = "SeekingImmortalsBigDipperDamage";
    private static final String COUNT_KEY = "SeekingImmortalsBigDipperCount";
    private static final int DURATION_TICKS = 160;
    private static final int INTERVAL_TICKS = 10;

    private final int count;
    private final String message;

    public MultiSwordArraySpell(int cost, int cooldownTicks, double damage, int count, String message) {
        super(cost, cooldownTicks, damage);
        this.count = count;
        this.message = message;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        long now = player.getServer().overworld().getGameTime();
        CompoundTag data = player.getPersistentData();
        data.putLong(UNTIL_KEY, now + DURATION_TICKS);
        data.putLong(NEXT_KEY, now);
        data.putInt(VOLLEY_KEY, 0);
        data.putDouble(DAMAGE_KEY, calculateDamage(skill.getLevel(), skill.getProficiency()));
        data.putInt(COUNT_KEY, Math.max(1, count));
        TechniqueVfxPalette.profile("metal").castAt(player.serverLevel(), player);
        tickActive(player);
        player.displayClientMessage(Component.literal(message), true);
        return true;
    }

    public static void tickActive(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long until = data.getLong(UNTIL_KEY);
        if (until <= 0L) {
            return;
        }
        long now = player.getServer().overworld().getGameTime();
        if (now > until || player.isDeadOrDying()) {
            clear(player);
            return;
        }
        long next = data.getLong(NEXT_KEY);
        if (now < next) {
            return;
        }
        int volley = data.getInt(VOLLEY_KEY);
        int swordCount = Math.max(1, data.getInt(COUNT_KEY));
        double damage = Math.max(1.0D, data.getDouble(DAMAGE_KEY));
        spawnSword(player, damage, swordCount, volley);
        data.putInt(VOLLEY_KEY, volley + 1);
        data.putLong(NEXT_KEY, now + INTERVAL_TICKS);
    }

    public static void clear(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(UNTIL_KEY);
        data.remove(NEXT_KEY);
        data.remove(VOLLEY_KEY);
        data.remove(DAMAGE_KEY);
        data.remove(COUNT_KEY);
    }

    private static void spawnSword(ServerPlayer player, double damage, int count, int volley) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = look.cross(up);
        if (side.lengthSqr() < 0.001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        // Wave48/51: orbit ring visuals + temporary orbiting sword projectiles for 7 swords.
        double ringRadius = 1.4D;
        double baseAngle = (player.tickCount + volley * 17) * Math.PI / 90.0D;
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + (Math.PI * 2.0D * i / Math.max(1, count));
            double ox = player.getX() + Math.cos(angle) * ringRadius;
            double oz = player.getZ() + Math.sin(angle) * ringRadius;
            double oy = player.getY() + 1.2D + Math.sin(angle * 2.0D) * 0.15D;
            level.sendParticles(ParticleTypes.END_ROD, ox, oy, oz, 1, 0.01D, 0.02D, 0.01D, 0.0D);
            if (i == (volley % Math.max(1, count))) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, ox, oy, oz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                // Orbiting strike: spawn a short-lived sword projectile tangent to the ring.
                Vec3 tangent = new Vec3(-Math.sin(angle), 0.05D, Math.cos(angle)).normalize();
                level.addFreshEntity(new SwordProjectileEntity(level, player, tangent, Math.max(1.0D, damage * 0.55D), false));
            }
        }
        double centered = (volley % count) - (count - 1) / 2.0D;
        Vec3 vertical = up.scale(0.04D + Math.abs(centered) * 0.02D);
        Vec3 direction = look.add(side.scale(centered * 0.12D)).add(vertical).normalize();
        level.addFreshEntity(new SwordProjectileEntity(level, player, direction, damage, false));
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.1D, player.getZ(),
                6, 0.4D, 0.35D, 0.4D, 0.01D);
    }
}
