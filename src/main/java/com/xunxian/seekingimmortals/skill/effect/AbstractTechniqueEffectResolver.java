package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.registry.ModMobEffects;
import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell;
import com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.WallTechniqueSpell;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;
import com.xunxian.seekingimmortals.skill.effect.spell.TalismanConsumeSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.HighImpactTechniqueSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.CraftGateTechniqueSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.CommandTechniqueSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.BuffZoneTechniqueSpell;
import com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.RegistryObject;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AbstractTechniqueEffectResolver {
    private static final Set<String> ABSTRACT_TYPES = Set.of(
            "projectile", "beam", "cone", "chain", "aoe", "aoe_control", "aoe_dot", "field", "domain", "wall",
            "trap", "buff_zone", "debuff", "dot", "drain", "control", "buff_self", "buff", "shield",
            "transform", "heal", "heal_spirit", "cleanse", "movement", "dash", "escape",
            "teleport_short", "melee", "strike", "ultimate", "secret_art", "soul_attack",
            "summon", "summon_field", "talisman_consume", "utility", "utility_combat",
            "scout", "scan", "inspect", "command", "craft_gate");
    private static final Set<String> GENERIC_RUNTIME_TYPES = Set.of(
            "projectile", "beam", "cone", "chain", "aoe", "aoe_control", "aoe_dot", "field", "domain",
            "trap", "debuff", "dot", "drain", "control", "buff_self", "buff", "shield", "transform",
            "heal", "heal_spirit", "cleanse", "movement", "dash", "escape", "teleport_short",
            "melee", "strike", "soul_attack", "summon", "summon_field", "utility",
            "utility_combat", "scout", "scan", "inspect");
    private static final Set<String> DEDICATED_ONLY_TYPES = Set.of(
            "ultimate", "secret_art", "talisman_consume", "command", "craft_gate", "wall", "buff_zone");
    private static final Map<String, SkillEffect> BY_TECHNIQUE_ID = new ConcurrentHashMap<>();
    private static final CultivationSkill VIRTUAL_SKILL = createVirtualSkill();

    private AbstractTechniqueEffectResolver() {}

    public record RuntimeSpec(String type, double damage, double range, double radius,
                              String target, String element, String effectKey, Set<String> tags) {}

    public static SkillEffect resolve(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return null;
        }
        String techniqueId = normalize(technique.id());
        SkillEffect cached = BY_TECHNIQUE_ID.get(techniqueId);
        if (cached != null) {
            return cached;
        }
        SkillType typed = resolveSkillType(technique);
        if (typed != null) {
            SkillEffect registered = safeGet(typed);
            if (registered != null) {
                // Simple registry shapes still lose to authored damage/range/effect_key.
                SkillEffect preferred = preferAuthoredRuntime(technique, registered);
                SkillEffect chosen = preferred != null ? preferred : registered;
                if (preferred != null && !techniqueId.isBlank()) {
                    BY_TECHNIQUE_ID.put(techniqueId, chosen);
                }
                return chosen;
            }
        }
        SkillEffect created = createForTechnique(technique);
        if (created != null && !techniqueId.isBlank()) {
            BY_TECHNIQUE_ID.put(techniqueId, created);
        }
        return created;
    }

    /**
     * When the SkillType registry only provides a generic-shaped spell, rebuild from corpus
     * RuntimeSpec so authored damage_base / range / effect_key win over hardcoded library stats.
     * Bespoke multi-form library spells (Dao/Illusion/Buddhist/...) keep their registry entry.
     */
    private static SkillEffect preferAuthoredRuntime(TechniqueDataManager.TechniqueEntry technique,
                                                     SkillEffect registered) {
        if (technique == null || registered == null || !hasAuthoredRuntimeFields(technique)) {
            return null;
        }
        if (!isGenericShapedRegistryEffect(registered)) {
            return null;
        }
        try {
            return createForTechnique(technique);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasAuthoredRuntimeFields(TechniqueDataManager.TechniqueEntry technique) {
        return technique.damageBase() > 0.0D
                || (technique.range() != null && !technique.range().isBlank())
                || (technique.effectKey() != null && !technique.effectKey().isBlank());
    }

    private static boolean isGenericShapedRegistryEffect(SkillEffect effect) {
        return effect instanceof ElementalProjectileSpell
                || effect instanceof ElementalAreaSpell
                || effect instanceof TargetedDebuffSpell
                || effect instanceof AreaDebuffSpell
                || effect instanceof SelfBuffSpell
                || effect instanceof RecoverySpell
                || effect instanceof HonestSummonSpell;
    }

    public static SkillType resolveSkillType(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return null;
        }
        SkillType typed = safeByTechniqueId(technique.id());
        return typed != null ? typed : safeByDisplayName(technique.name());
    }

    public static CultivationSkill virtualSkill() {
        return VIRTUAL_SKILL;
    }

    public static boolean isAbstractTypeRegistered(String effectType) {
        return ABSTRACT_TYPES.contains(normalize(effectType));
    }

    public static boolean isGenericRuntimeType(String effectType) {
        return GENERIC_RUNTIME_TYPES.contains(normalize(effectType));
    }

    public static boolean requiresDedicatedImplementation(String effectType) {
        return DEDICATED_ONLY_TYPES.contains(normalize(effectType));
    }

    public static int registeredAbstractTypeCount() {
        return ABSTRACT_TYPES.size();
    }

    public static RuntimeSpec runtimeSpec(TechniqueDataManager.TechniqueEntry technique) {
        if (technique == null) {
            return new RuntimeSpec("", 0.0D, 0.0D, 0.0D, "", "neutral", "", Set.of());
        }
        String type = normalize(technique.effectType());
        String target = normalize(technique.target());
        double damage = technique.damageBase() > 0.0D
                ? technique.damageBase()
                : defaultDamage(type);
        double range = resolveRange(technique.range(), type, target);
        double radius = resolveRadius(type, target);
        String element = resolveElement(technique);
        return new RuntimeSpec(type, damage, range, radius, target, element,
                normalize(technique.effectKey()), technique.tags());
    }

    private static SkillEffect createForTechnique(TechniqueDataManager.TechniqueEntry technique) {
        RuntimeSpec spec = runtimeSpec(technique);
        if (!GENERIC_RUNTIME_TYPES.contains(spec.type()) && !DEDICATED_ONLY_TYPES.contains(spec.type())) {
            return null;
        }
        int cost = Math.max(1, technique.cost());
        int cooldown = technique.cooldownTicks() > 0 ? technique.cooldownTicks() : 100;
        try {
            return createGenericEffect(technique, spec, cost, cooldown);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SkillEffect createGenericEffect(TechniqueDataManager.TechniqueEntry technique,
                                                   RuntimeSpec spec, int cost, int cooldown) {
        return switch (spec.type()) {
            case "projectile", "beam", "cone" -> new ElementalProjectileSpell(
                    cost, cooldown, spec.damage(), projectileSpeed(spec.type()), projectileElement(spec.element()),
                    "message.seeking_immortals.spell.generic_projectile.success");
            case "chain", "aoe", "aoe_control", "aoe_dot", "field", "domain" -> new ElementalAreaSpell(
                    cost, cooldown, spec.damage(), spec.range(), spec.radius(), areaElement(spec.element()),
                    "message.seeking_immortals.spell.generic_aoe.success");
            case "debuff", "dot", "drain", "soul_attack" -> {
                TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(spec.element());
                yield new TargetedDebuffSpell(
                        cost, cooldown, spec.damage(), spec.range(),
                        vfx.primaryDebuff(), 100, 2,
                        vfx.secondaryDebuff(), 80, 0,
                        vfx.core(), vfx.impactSound(),
                        "message.seeking_immortals.spell.generic_debuff.success",
                        "message.seeking_immortals.spell.target.fail");
            }
            case "trap", "control" -> {
                TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(spec.element());
                yield new AreaDebuffSpell(
                        cost, cooldown, spec.damage(), spec.range(), spec.radius(),
                        vfx.primaryDebuff(), 120, 3,
                        vfx.secondaryDebuff(), 100, 0,
                        vfx.edge(), vfx.castSound(),
                        "message.seeking_immortals.spell.generic_control.success",
                        "message.seeking_immortals.spell.area.fail");
            }
            case "buff_self", "buff", "shield", "transform", "utility", "utility_combat", "scout", "scan", "inspect" ->
                    createBuff(spec, cost, cooldown);
            case "heal", "heal_spirit", "cleanse" -> new RecoverySpell(
                    cost, cooldown, spec.damage(), Math.max(2.0D, spec.radius()),
                    recoveryForm(spec.type()), "message.seeking_immortals.spell.generic_heal.success");
            case "movement", "dash", "escape", "teleport_short" -> {
                TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(spec.element());
                MobEffect primaryBuff = vfx.buffPrimary();
                MobEffect secondaryBuff = vfx.buffSecondary();
                yield new SelfBuffSpell(
                        cost, cooldown,
                        primaryBuff != null ? primaryBuff : safeGetEffect("berserk"), movementDuration(spec), 1,
                        secondaryBuff != null ? secondaryBuff : safeGetEffect("shield"), movementDuration(spec), 0,
                        vfx.accent(), vfx.castSound(),
                        "message.seeking_immortals.spell.generic_movement.success");
            }
            case "melee", "strike" -> new SwordTechniqueSpell(
                    cost, cooldown, spec.damage(), spec.range(), Math.max(0.45D, spec.radius() / 4.0D),
                    SwordTechniqueSpell.SwordForm.FLYING_SWORD_STRIKE,
                    "message.seeking_immortals.spell.generic_melee.success");
            case "summon", "summon_field" -> new HonestSummonSpell(
                    cost, cooldown, technique.id(), summonStrength(spec.damage()),
                    Math.max(0, summonStrength(spec.damage()) - 1),
                    Math.max(120, cooldown), "message.seeking_immortals.spell.generic_summon.success");
            case "wall" -> new WallTechniqueSpell(
                    cost, cooldown, Math.max(12.0D, spec.damage()), Math.max(8.0D, spec.range()),
                    Math.max(2.5D, spec.radius()), spec.element(),
                    "message.seeking_immortals.spell.generic_wall.success");
            case "buff_zone" -> new BuffZoneTechniqueSpell(
                    cost, cooldown, spec.damage(), Math.max(6.0D, spec.range()), Math.max(3.5D, spec.radius()),
                    spec.element(), spec.tags(), spec.effectKey(),
                    "message.seeking_immortals.spell.generic_buff_zone.success");
            case "ultimate" -> new HighImpactTechniqueSpell(
                    cost, Math.max(cooldown, 160), Math.max(40.0D, spec.damage()),
                    Math.max(14.0D, spec.range()), Math.max(4.5D, spec.radius()),
                    spec.target(), spec.element(), spec.effectKey(), spec.tags(), "",
                    HighImpactTechniqueSpell.Form.ULTIMATE,
                    "message.seeking_immortals.spell.generic_ultimate.success");
            case "secret_art" -> new HighImpactTechniqueSpell(
                    cost, Math.max(cooldown, 140), Math.max(32.0D, spec.damage()),
                    Math.max(12.0D, spec.range()), Math.max(4.0D, spec.radius()),
                    spec.target(), spec.element(), spec.effectKey(), spec.tags(), "",
                    HighImpactTechniqueSpell.Form.SECRET_ART,
                    "message.seeking_immortals.spell.generic_secret_art.success");
            case "talisman_consume" -> new TalismanConsumeSpell(
                    cost, cooldown, Math.max(10.0D, spec.damage()), Math.max(10.0D, spec.range()),
                    Math.max(2.8D, spec.radius()), spec.element(), spec.effectKey(), spec.tags(),
                    "message.seeking_immortals.spell.generic_talisman_consume.success");
            case "command" -> new CommandTechniqueSpell(
                    cost, cooldown, Math.max(8.0D, spec.damage()), Math.max(10.0D, spec.range()),
                    Math.max(4.0D, spec.radius()), spec.element(), spec.tags(),
                    "message.seeking_immortals.spell.generic_command.success");
            case "craft_gate" -> new CraftGateTechniqueSpell(
                    cost, cooldown, spec.damage(), Math.max(8.0D, spec.range()), Math.max(3.5D, spec.radius()),
                    spec.tags(), "message.seeking_immortals.spell.generic_craft_gate.success");
            default -> null;
        };
    }

    private static SkillEffect createBuff(RuntimeSpec spec, int cost, int cooldown) {
        TechniqueVfxPalette.Profile vfx = TechniqueVfxPalette.profile(spec.element());
        MobEffect primary = switch (spec.type()) {
            case "scan", "scout", "inspect" -> safeGetEffect("conceal_qi");
            case "shield" -> safeGetEffect("shield");
            default -> vfx.buffPrimary();
        };
        MobEffect secondary = switch (spec.type()) {
            case "scan", "scout", "inspect" -> vfx.buffSecondary();
            case "shield" -> safeGetEffect("heal_hot");
            default -> vfx.buffSecondary();
        };
        return new SelfBuffSpell(
                cost, cooldown,
                primary != null ? primary : safeGetEffect("shield"), 160, "shield".equals(spec.type()) ? 1 : 0,
                secondary != null ? secondary : safeGetEffect("heal_hot"), 140, 0,
                vfx.core(), vfx.castSound(),
                "message.seeking_immortals.spell.generic_buff.success");
    }

    private static MobEffect safeGetEffect(String statusId) {
        RegistryObject<MobEffect> obj = ModMobEffects.get(statusId);
        return obj != null ? obj.get() : null;
    }

    private static CultivationFireballEntity.SpellElement projectileElement(String element) {
        return switch (TechniqueVfxPalette.familyOf(element)) {
            case WATER -> CultivationFireballEntity.SpellElement.WATER;
            case METAL -> CultivationFireballEntity.SpellElement.METAL;
            case ICE -> CultivationFireballEntity.SpellElement.ICE;
            case WIND -> CultivationFireballEntity.SpellElement.WIND;
            case WOOD -> CultivationFireballEntity.SpellElement.WOOD;
            case DARK, SOUL, BLOOD, ILLUSION -> CultivationFireballEntity.SpellElement.DARK;
            case LIGHT -> CultivationFireballEntity.SpellElement.LIGHT;
            case EARTH -> CultivationFireballEntity.SpellElement.EARTH;
            case THUNDER -> CultivationFireballEntity.SpellElement.THUNDER;
            case VOID -> CultivationFireballEntity.SpellElement.DARK;
            case FIRE, NEUTRAL -> CultivationFireballEntity.SpellElement.FIRE;
        };
    }

    private static ElementalAreaSpell.AreaElement areaElement(String element) {
        return switch (TechniqueVfxPalette.familyOf(element)) {
            case WATER -> ElementalAreaSpell.AreaElement.MIST_RAIN;
            case EARTH -> ElementalAreaSpell.AreaElement.SAND_STORM;
            case METAL -> ElementalAreaSpell.AreaElement.METAL_SHARD;
            case ICE -> ElementalAreaSpell.AreaElement.BLIZZARD;
            case WIND -> ElementalAreaSpell.AreaElement.CYCLONE;
            case WOOD -> ElementalAreaSpell.AreaElement.WOOD_BLOOM;
            case THUNDER -> ElementalAreaSpell.AreaElement.CHAIN_THUNDER;
            case LIGHT -> ElementalAreaSpell.AreaElement.LIGHT_BURST;
            case SOUL -> ElementalAreaSpell.AreaElement.SOUL_REND;
            case BLOOD -> ElementalAreaSpell.AreaElement.BLOOD_MIST;
            case VOID -> ElementalAreaSpell.AreaElement.VOID_RIFT;
            case ILLUSION -> ElementalAreaSpell.AreaElement.ILLUSION_HAZE;
            case DARK -> ElementalAreaSpell.AreaElement.SOUL_REND;
            case FIRE, NEUTRAL -> ElementalAreaSpell.AreaElement.LAVA;
        };
    }

    private static MobEffect primaryDebuff(String element) {
        return TechniqueVfxPalette.profile(element).primaryDebuff();
    }

    private static MobEffect buffPrimary(String element) {
        return TechniqueVfxPalette.profile(element).buffPrimary();
    }

    private static MobEffect buffSecondary(String element) {
        return TechniqueVfxPalette.profile(element).buffSecondary();
    }

    private static RecoverySpell.RecoveryForm recoveryForm(String type) {
        return switch (normalize(type)) {
            case "cleanse" -> RecoverySpell.RecoveryForm.DETOXIFY;
            case "heal_spirit" -> RecoverySpell.RecoveryForm.SPIRIT_RECOVERY;
            default -> RecoverySpell.RecoveryForm.HEAL_QI;
        };
    }

    private static String resolveElement(TechniqueDataManager.TechniqueEntry technique) {
        String direct = normalize(technique.effectElement());
        if (!direct.isBlank() && !"neutral".equals(direct)) {
            return direct;
        }
        String attribute = normalize(technique.attribute());
        if (!attribute.isBlank() && !"neutral".equals(attribute)) {
            return attribute;
        }
        for (String candidate : Set.of("fire", "water", "metal", "wood", "earth", "wind",
                "ice", "thunder", "lightning", "yin", "yang", "dark", "light", "ghost")) {
            if (technique.tags().contains(candidate)) {
                return candidate;
            }
        }
        return "neutral";
    }

    private static double resolveRange(String authoredRange, String type, String target) {
        String normalized = normalize(authoredRange);
        if (!normalized.isBlank()) {
            try {
                return Math.max(0.0D, Math.min(64.0D, Double.parseDouble(normalized)));
            } catch (NumberFormatException ignored) {
                return switch (normalized) {
                    case "long" -> 32.0D;
                    case "medium" -> 20.0D;
                    case "short" -> 10.0D;
                    case "dash" -> 9.0D;
                    case "touch", "melee" -> 4.0D;
                    default -> defaultRange(type, target);
                };
            }
        }
        return defaultRange(type, target);
    }

    private static double defaultRange(String type, String target) {
        if (Set.of("buff_self", "buff", "transform", "heal", "heal_spirit", "cleanse").contains(type)
                || "self".equals(target)) {
            return 0.0D;
        }
        if (Set.of("movement", "dash", "escape", "teleport_short", "melee", "strike").contains(type)) {
            return 9.0D;
        }
        if (Set.of("ultimate", "secret_art").contains(type) || "battlefield".equals(target)) {
            return 24.0D;
        }
        if (Set.of("wall", "buff_zone", "command", "craft_gate", "talisman_consume").contains(type)) {
            return 16.0D;
        }
        return "area".equals(target) ? 22.0D : 18.0D;
    }

    private static double resolveRadius(String type, String target) {
        if (Set.of("chain", "aoe", "aoe_dot", "field", "domain", "trap", "control").contains(type)) {
            return "area".equals(target) ? 5.0D : 3.5D;
        }
        if (Set.of("ultimate", "secret_art", "buff_zone").contains(type)) {
            return "battlefield".equals(target) || "area".equals(target) ? 6.0D : 4.5D;
        }
        if ("wall".equals(type)) {
            return 3.0D;
        }
        if (Set.of("talisman_consume", "command", "craft_gate").contains(type)) {
            return 3.5D;
        }
        if ("summon_field".equals(type)) {
            return 4.0D;
        }
        return "area".equals(target) ? 4.0D : 2.0D;
    }

    private static double defaultDamage(String type) {
        return switch (normalize(type)) {
            case "projectile", "beam", "cone" -> 12.0D;
            case "chain", "aoe", "aoe_dot", "field", "domain" -> 16.0D;
            case "debuff", "dot", "drain", "control", "trap", "soul_attack" -> 8.0D;
            case "melee", "strike" -> 18.0D;
            case "heal", "heal_spirit", "cleanse" -> 12.0D;
            case "wall" -> 20.0D;
            case "talisman_consume" -> 14.0D;
            case "command" -> 18.0D;
            case "ultimate" -> 80.0D;
            case "secret_art" -> 64.0D;
            default -> 0.0D;
        };
    }

    private static double projectileSpeed(String type) {
        return switch (normalize(type)) {
            case "beam" -> 1.65D;
            case "cone" -> 1.0D;
            default -> 1.2D;
        };
    }

    private static int movementDuration(RuntimeSpec spec) {
        return Math.max(80, (int) Math.round(100.0D + spec.range() * 4.0D));
    }

    private static int summonStrength(double damage) {
        return Math.max(0, Math.min(4, (int) Math.floor(Math.max(0.0D, damage) / 20.0D)));
    }

    private static SkillType safeByTechniqueId(String id) {
        try {
            SkillType registered = SkillEffectRegistry.byTechniqueId(id);
            if (registered != null) {
                return registered;
            }
        } catch (Throwable ignored) {
        }
        String normalized = normalize(id);
        for (SkillType type : SkillType.values()) {
            if (normalized.equals(normalize(type.getTechniqueId()))) {
                return type;
            }
        }
        return null;
    }

    private static SkillType safeByDisplayName(String name) {
        try {
            SkillType registered = SkillEffectRegistry.byDisplayName(name);
            if (registered != null) {
                return registered;
            }
        } catch (Throwable ignored) {
        }
        String normalized = normalize(name);
        for (SkillType type : SkillType.values()) {
            if (normalized.equals(normalize(type.getDisplayName()))) {
                return type;
            }
        }
        return null;
    }

    private static SkillEffect safeGet(SkillType type) {
        try {
            return SkillEffectRegistry.get(type);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static CultivationSkill createVirtualSkill() {
        CultivationSkill skill = new CultivationSkill(SkillType.FIREBALL);
        skill.unlock();
        return skill;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
