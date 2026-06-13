package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

public class FireballSpell extends SpellEffect {
    public FireballSpell() {
        super(30, 100, 6.0);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        if (cultivation.getSpiritualPower() < getSpiritualPowerCost(skill.getLevel())) {
            return false;
        }

        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.getEyePosition().add(look);

        SmallFireball fireball = new SmallFireball(context.getLevel(), player, look.x, look.y, look.z);
        fireball.setPos(pos);
        context.getLevel().addFreshEntity(fireball);

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("释放火球术！造成" + String.format("%.1f", damage) + "伤害"),
            true
        );

        return true;
    }
}
