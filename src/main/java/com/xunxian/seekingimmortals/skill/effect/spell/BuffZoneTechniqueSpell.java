package com.xunxian.seekingimmortals.skill.effect.spell;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.effect.SkillContext;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Dedicated buff_zone family: temporary free formation field + ally/self buffs.
 */
public class BuffZoneTechniqueSpell extends SpellEffect {
    private final double range;
    private final double radius;
    private final String element;
    private final Set<String> tags;
    private final String effectKey;
    private final String successKey;

    public BuffZoneTechniqueSpell(int cost, int cooldownTicks, double damage, double range, double radius,
                                  String element, Set<String> tags, String effectKey, String successKey) {
        super(cost, cooldownTicks, Math.max(0.0D, damage));
        this.range = Math.max(0.0D, range);
        this.radius = Math.max(2.5D, radius);
        this.element = element == null ? "neutral" : element;
        this.tags = tags == null ? Set.of() : tags;
        this.effectKey = effectKey == null ? "" : effectKey;
        this.successKey = successKey == null || successKey.isBlank()
                ? "message.seeking_immortals.spell.generic_buff_zone.success"
                : successKey;
    }

    @Override
    public boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context) {
        ServerLevel level = player.serverLevel();
        Vec3 center = findCenter(player);
        BlockPos core = BlockPos.containing(center);
        FormationFieldService.FieldKind kind = resolveKind();
        int duration = 20 * (12 + Math.max(0, skill.getLevel()));
        FormationFieldService.activateFreeField(level, core, kind, duration, player, effectKey);

        AABB area = new AABB(center, center).inflate(radius, 2.5D, radius);
        List<ServerPlayer> allies = level.getEntitiesOfClass(ServerPlayer.class, area,
                target -> target.isAlive() && !target.isSpectator()
                        && (target == player || !player.canHarmPlayer(target)));
        if (allies.isEmpty()) {
            allies = List.of(player);
        }

        int amp = Math.max(0, skill.getLevel() / 5);
        int buffTicks = 100 + skill.getLevel() * 8;
        int restored = Math.max(2, (int) Math.round(4.0D + skill.getLevel() * 1.5D + baseDamage * 0.05D));
        for (ServerPlayer ally : allies.stream().limit(8).toList()) {
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, buffTicks, amp, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, buffTicks, Math.min(2, amp), false, true));
            if (isSpiritGather()) {
                ally.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, true));
            }
            if (isDefense()) {
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, buffTicks, Math.min(2, 1 + amp), false, true));
            }
        }
        cultivation.addSpiritualPower(restored);

        spawnVisual(level, center);
        level.playSound(null, core, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.25F);
        player.displayClientMessage(Component.translatable(successKey, allies.size(), restored), true);
        return true;
    }

    private Vec3 findCenter(ServerPlayer player) {
        if (range <= 0.5D) {
            return player.position();
        }
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 0.001D) {
            return player.position();
        }
        return player.getEyePosition().add(look.normalize().scale(Math.min(range, 8.0D)));
    }

    private FormationFieldService.FieldKind resolveKind() {
        String blob = (element + " " + effectKey + " " + String.join(" ", tags)).toLowerCase(Locale.ROOT);
        if (blob.contains("defense") || blob.contains("shield") || blob.contains("guard")) {
            return FormationFieldService.FieldKind.DEFENSE;
        }
        if (blob.contains("seal") || blob.contains("demon")) {
            return FormationFieldService.FieldKind.SEAL_DEMON;
        }
        if (blob.contains("illusion") || blob.contains("maze")) {
            return FormationFieldService.FieldKind.ILLUSION_MAZE;
        }
        if (blob.contains("kill") || blob.contains("sword")) {
            return FormationFieldService.FieldKind.KILL_SWORD;
        }
        return FormationFieldService.FieldKind.SPIRIT_GATHER;
    }

    private boolean isSpiritGather() {
        return resolveKind() == FormationFieldService.FieldKind.SPIRIT_GATHER;
    }

    private boolean isDefense() {
        return resolveKind() == FormationFieldService.FieldKind.DEFENSE;
    }

    private void spawnVisual(ServerLevel level, Vec3 center) {
        DustParticleOptions core = new DustParticleOptions(new Vector3f(0.46F, 1.00F, 0.70F), 0.55F);
        level.sendParticles(core, center.x, center.y + 0.2D, center.z, 56, radius * 0.45D, 0.35D, radius * 0.45D, 0.01D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.8D, center.z, 18, 0.4D, 0.5D, 0.4D, 0.02D);
    }
}
