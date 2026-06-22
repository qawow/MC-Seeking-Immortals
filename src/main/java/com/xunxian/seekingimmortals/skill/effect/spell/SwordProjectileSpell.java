package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SwordProjectileSpell extends SpellEffect {
    private final int count;

    public SwordProjectileSpell(int cost, int cooldownTicks, double damage, int count) {
        super(cost, cooldownTicks, damage);
        this.count = count;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        Vec3 look = player.getLookAngle();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = look.cross(up);
        if (side.lengthSqr() < 0.001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        for (int i = 0; i < count; i++) {
            double offset = count == 1 ? 0.0D : (i - 1) * 0.18D;
            Vec3 direction = look.add(side.scale(offset)).normalize();
            SwordProjectileEntity projectile = new SwordProjectileEntity(context.getLevel(), player, direction, calculateDamage(skill.getLevel(), skill.getProficiency()), false);
            context.getLevel().addFreshEntity(projectile);
        }
        player.displayClientMessage(Component.literal(count == 1 ? "单剑刺击破空而出。" : "三才剑阵三剑齐发。"), true);
        return true;
    }
}
