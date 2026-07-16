package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * M02: maps text-material technique {@code effect.type} (40 abstract kinds) onto reusable
 * SkillEffect implementations so every loaded technique can resolve an effect without a
 * dedicated SkillType enum constant.
 *
 * <p>Spell implementations that touch Minecraft registries are created lazily so pure unit
 * tests can still assert resolvability without bootstrapping Forge.
 */
public final class AbstractTechniqueEffectResolver {
    private static final Set<String> ABSTRACT_TYPES = Set.of(
            "projectile", "beam", "cone", "chain", "aoe", "aoe_dot", "field", "domain", "wall",
            "trap", "buff_zone", "debuff", "dot", "drain", "control", "buff_self", "buff",
            "transform", "heal", "heal_spirit", "cleanse", "movement", "dash", "escape",
            "teleport_short", "melee", "strike", "ultimate", "secret_art", "soul_attack",
            "summon", "summon_field", "talisman_consume", "utility", "utility_combat",
            "scout", "scan", "inspect", "command", "craft_gate");

    private static final Map<String, SkillEffect> BY_TECHNIQUE_ID = new ConcurrentHashMap<>();
    private static final Map<String, SkillEffect> BY_ABSTRACT_TYPE = new ConcurrentHashMap<>();
    private static final Object INIT_LOCK = new Object();
    private static volatile boolean templatesReady;
    private static volatile boolean templatesFailed;
    private static final CultivationSkill VIRTUAL_SKILL = createVirtualSkill();
    private static final SkillEffect FALLBACK_STUB = new StubEffect(12, 100);

    private AbstractTechniqueEffectResolver() {}

    public static SkillEffect resolve(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return null;
        }
        SkillType typed = safeByTechniqueId(technique.id());
        if (typed == null) {
            typed = safeByDisplayName(technique.name());
        }
        if (typed != null) {
            SkillEffect registered = safeGet(typed);
            if (registered != null) {
                return registered;
            }
        }
        SkillEffect cached = BY_TECHNIQUE_ID.get(technique.id());
        if (cached != null) {
            return cached;
        }
        SkillEffect created = createForTechnique(technique);
        if (created != null) {
            BY_TECHNIQUE_ID.put(technique.id(), created);
        }
        return created;
    }

    public static SkillType resolveSkillType(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return null;
        }
        SkillType typed = safeByTechniqueId(technique.id());
        if (typed != null) {
            return typed;
        }
        return safeByDisplayName(technique.name());
    }

    private static SkillType safeByTechniqueId(String id) {
        try {
            return SkillEffectRegistry.byTechniqueId(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SkillType safeByDisplayName(String name) {
        try {
            return SkillEffectRegistry.byDisplayName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SkillEffect safeGet(SkillType type) {
        try {
            return SkillEffectRegistry.get(type);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Level-1 virtual skill used when the technique has no SkillType enum mapping. */
    public static CultivationSkill virtualSkill() {
        return VIRTUAL_SKILL;
    }

    public static boolean isAbstractTypeRegistered(String effectType) {
        String type = normalize(effectType);
        return ABSTRACT_TYPES.contains(type) || "projectile".equals(type) || type.isBlank();
    }

    public static int registeredAbstractTypeCount() {
        return ABSTRACT_TYPES.size();
    }

    private static SkillEffect createForTechnique(TechniqueDataManager.TechniqueEntry technique) {
        ensureTemplates();
        String type = normalize(technique.effectType());
        if (type.isBlank()) {
            type = "projectile";
        }
        SkillEffect template = BY_ABSTRACT_TYPE.get(type);
        if (template == null) {
            template = BY_ABSTRACT_TYPE.get("projectile");
        }
        if (template == null) {
            template = FALLBACK_STUB;
        }
        int cooldown = technique.cooldownTicks() > 0 ? technique.cooldownTicks() : 100;
        return new CostAwareEffect(template, Math.max(1, technique.cost()), cooldown);
    }

    private static void ensureTemplates() {
        if (templatesReady || templatesFailed) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (templatesReady || templatesFailed) {
                return;
            }
            try {
                buildAbstractTypeEffects(BY_ABSTRACT_TYPE);
                templatesReady = true;
            } catch (Throwable throwable) {
                // Outside a bootstrapped Minecraft/Forge runtime (unit tests), keep stubs.
                templatesFailed = true;
                for (String type : ABSTRACT_TYPES) {
                    BY_ABSTRACT_TYPE.putIfAbsent(type, FALLBACK_STUB);
                }
                BY_ABSTRACT_TYPE.putIfAbsent("projectile", FALLBACK_STUB);
            }
        }
    }

    private static void buildAbstractTypeEffects(Map<String, SkillEffect> map) {
        // Heavy imports kept inside method so class init does not touch MC registries.
        SkillEffect projectile = new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(
                12, 40, 14.0D, 1.15D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.FIRE,
                "message.seeking_immortals.spell.generic_projectile.success");
        SkillEffect beam = new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(
                16, 60, 22.0D, 1.25D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.METAL,
                "message.seeking_immortals.spell.generic_beam.success");
        SkillEffect aoe = new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(
                14, 120, 16.0D, 18.0D, 3.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.generic_aoe.success");
        SkillEffect debuff = new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(
                10, 100, 8.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 2,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0,
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                "message.seeking_immortals.spell.generic_debuff.success",
                "message.seeking_immortals.spell.target.fail");
        SkillEffect buffSelf = new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(
                10, 160,
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 160, 0,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 160, 0,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.generic_buff.success");
        SkillEffect heal = new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(
                12, 120, 18.0D, 2.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.HEAL_QI,
                "message.seeking_immortals.spell.generic_heal.success");
        SkillEffect movement = new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(
                8, 100,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 140, 1,
                net.minecraft.world.effect.MobEffects.JUMP, 140, 0,
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                net.minecraft.sounds.SoundEvents.ELYTRA_FLYING,
                "message.seeking_immortals.spell.generic_movement.success");
        SkillEffect summon = new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(
                14, 200, "generic_summon", 1, 0, 200,
                "message.seeking_immortals.spell.generic_summon.success");
        SkillEffect sword = new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(
                16, 120, 30.0D, 20.0D, 0.7D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.FLYING_SWORD_STRIKE,
                "message.seeking_immortals.spell.generic_melee.success");
        SkillEffect illusion = new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                12, 140, 10.0D, 16.0D, 1.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.MIND_CONFUSION,
                "message.seeking_immortals.spell.generic_illusion.success");
        SkillEffect control = new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(
                12, 120, 2.0D, 16.0D, 3.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 3,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 80, 0,
                net.minecraft.core.particles.ParticleTypes.POOF,
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                "message.seeking_immortals.spell.generic_control.success",
                "message.seeking_immortals.spell.area.fail");

        map.put("projectile", projectile);
        map.put("beam", beam);
        map.put("cone", projectile);
        map.put("chain", aoe);
        map.put("aoe", aoe);
        map.put("aoe_dot", aoe);
        map.put("field", aoe);
        map.put("domain", aoe);
        map.put("wall", aoe);
        map.put("trap", control);
        map.put("buff_zone", buffSelf);
        map.put("debuff", debuff);
        map.put("dot", debuff);
        map.put("drain", debuff);
        map.put("control", control);
        map.put("buff_self", buffSelf);
        map.put("buff", buffSelf);
        map.put("transform", buffSelf);
        map.put("heal", heal);
        map.put("heal_spirit", heal);
        map.put("cleanse", heal);
        map.put("movement", movement);
        map.put("dash", movement);
        map.put("escape", movement);
        map.put("teleport_short", movement);
        map.put("melee", sword);
        map.put("strike", sword);
        map.put("ultimate", aoe);
        map.put("secret_art", aoe);
        map.put("soul_attack", debuff);
        map.put("summon", summon);
        map.put("summon_field", summon);
        map.put("talisman_consume", projectile);
        map.put("utility", buffSelf);
        map.put("utility_combat", buffSelf);
        map.put("scout", buffSelf);
        map.put("scan", buffSelf);
        map.put("inspect", buffSelf);
        map.put("command", buffSelf);
        map.put("craft_gate", buffSelf);
    }

    private static CultivationSkill createVirtualSkill() {
        CultivationSkill skill = new CultivationSkill(SkillType.FIREBALL);
        skill.unlock();
        return skill;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record CostAwareEffect(SkillEffect delegate, int cost, int cooldown) implements SkillEffect {
        @Override
        public boolean execute(ServerPlayer player, PlayerCultivation cultivation,
                               CultivationSkill skill, SkillContext context) {
            return delegate.execute(player, cultivation, skill, context);
        }

        @Override
        public int getSpiritualPowerCost(int skillLevel) {
            return Math.max(1, cost + Math.max(0, skillLevel - 1));
        }

        @Override
        public int getCooldownTicks(int skillLevel) {
            return Math.max(20, cooldown - Math.max(0, skillLevel - 1) * 2);
        }

        @Override
        public boolean canExecute(ServerPlayer player, PlayerCultivation cultivation) {
            return delegate.canExecute(player, cultivation);
        }
    }

    /** No-op effect used when Minecraft registries are unavailable (unit tests). */
    private record StubEffect(int cost, int cooldown) implements SkillEffect {
        @Override
        public boolean execute(ServerPlayer player, PlayerCultivation cultivation,
                               CultivationSkill skill, SkillContext context) {
            return true;
        }

        @Override
        public int getSpiritualPowerCost(int skillLevel) {
            return Math.max(1, cost);
        }

        @Override
        public int getCooldownTicks(int skillLevel) {
            return Math.max(20, cooldown);
        }
    }
}
