package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.SwordProjectileEntity;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class IceConeSpell extends SpellEffect {
    public IceConeSpell() {
        super(10, 40, 5.0D);
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        Vec3 look = player.getLookAngle();
        SwordProjectileEntity projectile = new SwordProjectileEntity(context.getLevel(), player, look, calculateDamage(skill.getLevel(), skill.getProficiency()), true);
        context.getLevel().addFreshEntity(projectile);
        player.displayClientMessage(Component.literal("冰锥术凝成寒芒，命中将造成伤害并减速。"), true);
        return true;
    }
}
