package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public class ElementalProjectileSpell extends SpellEffect {
    private final CultivationFireballEntity.SpellElement element;
    private final String successKey;
    private final double speed;

    public ElementalProjectileSpell(int cost, int cooldownTicks, double damage,
                                    CultivationFireballEntity.SpellElement element,
                                    String successKey) {
        this(cost, cooldownTicks, damage, 1.15D, element, successKey);
    }

    public ElementalProjectileSpell(int cost, int cooldownTicks, double damage, double speed,
                                    CultivationFireballEntity.SpellElement element,
                                    String successKey) {
        super(cost, cooldownTicks, damage);
        this.element = element;
        this.successKey = successKey;
        this.speed = speed;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        double damage = calculateDamage(skill.getLevel(), skill.getProficiency());
        Vec3 look = player.getLookAngle();
        CultivationFireballEntity projectile = new CultivationFireballEntity(context.getLevel(), player, look, damage, speed, element);
        context.getLevel().addFreshEntity(projectile);
        player.displayClientMessage(Component.translatable(successKey, String.format(Locale.ROOT, "%.1f", damage)), true);
        return true;
    }
}
