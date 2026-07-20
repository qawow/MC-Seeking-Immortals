package com.xunxian.seekingimmortals.skill.effect;

import com.xunxian.seekingimmortals.skill.SkillType;
import java.util.HashMap;
import java.util.Map;

public class SkillEffectRegistry {
    private static final Map<SkillType, SkillEffect> EFFECTS = new HashMap<>();
    private static final Map<String, SkillType> EFFECTS_BY_TECHNIQUE_ID = new HashMap<>();

    public static void register(SkillType type, SkillEffect effect) {
        EFFECTS.put(type, effect);
        if (type.getTechniqueId() != null && !type.getTechniqueId().isBlank()) {
            EFFECTS_BY_TECHNIQUE_ID.put(type.getTechniqueId(), type);
        }
    }

    public static void registerTechniqueAlias(String techniqueId, SkillType type) {
        if (techniqueId != null && !techniqueId.isBlank() && type != null) {
            EFFECTS_BY_TECHNIQUE_ID.put(techniqueId, type);
        }
    }

    public static SkillEffect get(SkillType type) {
        return EFFECTS.get(type);
    }

    public static SkillType byDisplayName(String displayName) {
        for (SkillType type : EFFECTS.keySet()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    public static SkillType byTechniqueId(String techniqueId) {
        return techniqueId == null ? null : EFFECTS_BY_TECHNIQUE_ID.get(techniqueId);
    }

    public static boolean hasEffect(SkillType type) {
        return EFFECTS.containsKey(type);
    }

    static {
        // 法术
        register(SkillType.QI_GUIDING, new com.xunxian.seekingimmortals.skill.effect.spell.QiGuidingPassive());
        register(SkillType.FIREBALL, new com.xunxian.seekingimmortals.skill.effect.spell.FireballSpell());
        register(SkillType.FIRE_BULLET, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 40, 12.0D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.FIRE,
                "message.seeking_immortals.spell.fire_bullet.success"));
        register(SkillType.WATER_ARROW, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 40, 10.0D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WATER,
                "message.seeking_immortals.spell.water_arrow.success"));
        register(SkillType.METAL_NEEDLE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 40, 11.0D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.METAL,
                "message.seeking_immortals.spell.metal_needle.success"));
        register(SkillType.DARK_FLAME, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(9, 60, 19.0D, 1.10D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.DARK,
                "message.seeking_immortals.spell.dark_flame.success"));
        register(SkillType.LIGHT_ORB, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 55, 17.0D, 1.08D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.LIGHT,
                "message.seeking_immortals.spell.light_orb.success"));
        register(SkillType.WIND_BLADE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 40, 12.0D, 1.35D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WIND,
                "message.seeking_immortals.spell.wind_blade.success"));
        register(SkillType.VINE_ARROW, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 40, 9.0D, 1.05D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WOOD,
                "message.seeking_immortals.spell.vine_arrow.success"));
        register(SkillType.THUNDER_PALM, new com.xunxian.seekingimmortals.skill.effect.spell.ThunderPalmSpell());
        register(SkillType.FLAME_RING, new com.xunxian.seekingimmortals.skill.effect.spell.FlameRingSpell());
        register(SkillType.FROST_ARMOR, new com.xunxian.seekingimmortals.skill.effect.spell.FrostArmorSpell());
        register(SkillType.GOLD_BEAM, new com.xunxian.seekingimmortals.skill.effect.spell.GoldBeamSpell());
        register(SkillType.LAVA_BURST, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(14, 130, 13.5D, 19.0D, 3.7D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.lava_burst.success"));
        register(SkillType.MIST_RAIN, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(8, 90, 7.5D, 18.0D, 3.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.MIST_RAIN,
                "message.seeking_immortals.spell.mist_rain.success"));
        register(SkillType.SAND_STORM, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(12, 120, 10.5D, 18.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.SAND_STORM,
                "message.seeking_immortals.spell.sand_storm.success"));
        register(SkillType.BLIZZARD, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(13, 140, 12.0D, 19.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.BLIZZARD,
                "message.seeking_immortals.spell.blizzard.success"));
        register(SkillType.CYCLONE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(10, 110, 9.5D, 19.0D, 3.6D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CYCLONE,
                "message.seeking_immortals.spell.cyclone.success"));
        register(SkillType.CHAIN_LIGHTNING, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(17, 170, 15.5D, 20.0D, 4.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CHAIN_THUNDER,
                "message.seeking_immortals.spell.chain_lightning.success"));
        register(SkillType.FIVE_THUNDER, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(25, 220, 50.0D, 22.0D, 4.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.FIVE_THUNDER,
                "message.seeking_immortals.spell.five_thunder.success"));
        register(SkillType.PURE_YANG_SWORD, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(21, 160, 42.0D, 24.0D, 0.62D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.PURE_YANG_SWORD,
                "message.seeking_immortals.spell.pure_yang_sword.success"));
        register(SkillType.TAOIST_SEAL, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(14, 130, 28.0D, 18.0D, 0.9D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.TAOIST_SEAL,
                "message.seeking_immortals.spell.taoist_seal.success"));
        register(SkillType.CLOUD_WALK, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(12, 150, 0.0D, 0.0D, 2.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.CLOUD_WALK,
                "message.seeking_immortals.spell.cloud_walk.success"));
        register(SkillType.IMMORTAL_ROPE, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(16, 150, 33.0D, 20.0D, 0.82D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.IMMORTAL_ROPE,
                "message.seeking_immortals.spell.immortal_rope.success"));
        register(SkillType.BAGUA_SEAL, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(22, 190, 44.0D, 20.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.BAGUA_SEAL,
                "message.seeking_immortals.spell.bagua_seal.success"));
        register(SkillType.DAO_NATURE_BREATH, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(10, 140, 25.0D, 0.0D, 2.6D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.DAO_NATURE_BREATH,
                "message.seeking_immortals.spell.dao_nature_breath.success"));
        register(SkillType.BUDDHA_LIGHT, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(19, 170, 38.0D, 20.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.BUDDHA_LIGHT,
                "message.seeking_immortals.spell.buddha_light.success"));
        register(SkillType.SARIRA_SHIELD, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(12, 190, 0.0D, 0.0D, 3.6D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.SARIRA_SHIELD,
                "message.seeking_immortals.spell.sarira_shield.success"));
        register(SkillType.DEMON_SUBDUE_PALM, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(22, 150, 45.0D, 8.5D, 1.45D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.DEMON_SUBDUE_PALM,
                "message.seeking_immortals.spell.demon_subdue_palm.success"));
        register(SkillType.ZEN_PULSE, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(15, 150, 30.0D, 0.0D, 4.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.ZEN_PULSE,
                "message.seeking_immortals.spell.zen_pulse.success"));
        register(SkillType.VAJRA_PALM, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(18, 145, 37.0D, 21.0D, 0.82D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.VAJRA_PALM,
                "message.seeking_immortals.spell.vajra_palm.success"));
        register(SkillType.DAJIN_BUDDHIST_VAJRA, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(18, 170, 45.0D, 10.0D, 1.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.DAJIN_BUDDHIST_VAJRA,
                "message.seeking_immortals.spell.dajin_buddhist_vajra.success"));
        register(SkillType.RIGHTEOUS_QI, new com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell(12, 160, 0.0D, 0.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell.ConfucianForm.RIGHTEOUS_QI,
                "message.seeking_immortals.spell.righteous_qi.success"));
        register(SkillType.WORD_SUPPRESS, new com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell(16, 150, 32.0D, 20.0D, 0.95D,
                com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell.ConfucianForm.WORD_SUPPRESS,
                "message.seeking_immortals.spell.word_suppress.success"));
        register(SkillType.SCROLL_STRIKE, new com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell(18, 145, 36.0D, 22.0D, 1.05D,
                com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell.ConfucianForm.SCROLL_STRIKE,
                "message.seeking_immortals.spell.scroll_strike.success"));
        register(SkillType.INK_SEA, new com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell(20, 210, 40.0D, 20.0D, 4.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell.ConfucianForm.INK_SEA,
                "message.seeking_immortals.spell.ink_sea.success"));
        register(SkillType.CONFUCIAN_RIGHTEOUS_QI, new com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell(18, 170, 37.0D, 24.0D, 0.82D,
                com.xunxian.seekingimmortals.skill.effect.spell.ConfucianSpell.ConfucianForm.CONFUCIAN_RIGHTEOUS_QI,
                "message.seeking_immortals.spell.confucian_righteous_qi.success"));
        register(SkillType.SMALL_SWORD_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(20, 160, 40.0D, 22.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SMALL_SWORD_ARRAY,
                "message.seeking_immortals.spell.small_sword_array.success"));
        register(SkillType.ILLUSION_FORMATION, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(12, 150, 25.0D, 20.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.ILLUSION_FORMATION,
                "message.seeking_immortals.spell.illusion_formation.success"));
        register(SkillType.SPIRIT_GATHER_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(12, 220, 0.0D, 0.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SPIRIT_GATHER_ARRAY,
                "message.seeking_immortals.spell.spirit_gather_array.success"));
        register(SkillType.THUNDER_TRAP_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(27, 220, 55.0D, 22.0D, 4.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.THUNDER_TRAP_ARRAY,
                "message.seeking_immortals.spell.thunder_trap_array.success"));
        register(SkillType.SEAL_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(15, 170, 30.0D, 20.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SEAL_ARRAY,
                "message.seeking_immortals.spell.seal_array.success"));
        register(SkillType.KILL_SWORD_FORMATION, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(30, 240, 60.0D, 22.0D, 4.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.KILL_SWORD_FORMATION,
                "message.seeking_immortals.spell.kill_sword_formation.success"));
        register(SkillType.DEFENSE_FORMATION, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(12, 220, 0.0D, 0.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.DEFENSE_FORMATION,
                "message.seeking_immortals.spell.defense_formation.success"));
        register(SkillType.SEA_LOCK_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(24, 220, 36.0D, 22.0D, 4.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SEA_LOCK_ARRAY,
                "message.seeking_immortals.spell.sea_lock_array.success"));
        register(SkillType.STAR_PALACE_PATROL_BEACON, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(18, 220, 0.0D, 0.0D, 7.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.STAR_PALACE_PATROL_BEACON,
                "message.seeking_immortals.spell.star_palace_patrol_beacon.success"));
        register(SkillType.FORMATION_TRAP_BASIC, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(18, 180, 28.0D, 20.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.FORMATION_TRAP_BASIC,
                "message.seeking_immortals.spell.formation_trap_basic.success"));
        register(SkillType.STAR_PALACE_SEAL, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(26, 220, 60.0D, 24.0D, 4.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.STAR_PALACE_SEAL,
                "message.seeking_immortals.spell.star_palace_seal.success"));
        register(SkillType.KUNWU_SEAL_STRIKE, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(26, 210, 70.0D, 24.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.KUNWU_SEAL_STRIKE,
                "message.seeking_immortals.spell.kunwu_seal_strike.success"));
        register(SkillType.STAR_PALACE_TIDAL_LOCK, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(26, 240, 48.0D, 24.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.STAR_PALACE_TIDAL_LOCK,
                "message.seeking_immortals.spell.star_palace_tidal_lock.success"));
        register(SkillType.ICE_CONE, new com.xunxian.seekingimmortals.skill.effect.spell.IceConeSpell());
        register(SkillType.ICE_FREEZING, new com.xunxian.seekingimmortals.skill.effect.spell.IceConeSpell(
                "message.seeking_immortals.spell.ice_freezing.success"));
        register(SkillType.VINE_BIND, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(8, 80, 2.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 3,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0,
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                net.minecraft.sounds.SoundEvents.GRASS_BREAK,
                "message.seeking_immortals.spell.entangling.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.VOICE_TRANSMISSION, new com.xunxian.seekingimmortals.skill.effect.spell.VoiceTransmissionSpell(6, 80, 48.0D));
        register(SkillType.EARTH_SPIKE, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(12, 60, 7.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 50, 1,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.POOF,
                net.minecraft.sounds.SoundEvents.STONE_BREAK,
                "message.seeking_immortals.spell.earth_spike.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.OBJECT_CONTROL, new com.xunxian.seekingimmortals.skill.effect.spell.ObjectControlSpell(8, 60, 16.0D));
        register(SkillType.QUICKSAND, new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(12, 120, 1.0D, 18.0D, 3.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 4,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0,
                net.minecraft.core.particles.ParticleTypes.POOF,
                net.minecraft.sounds.SoundEvents.SAND_BREAK,
                "message.seeking_immortals.spell.quicksand.success",
                "message.seeking_immortals.spell.area.fail"));
        register(SkillType.THUNDER_STRIKE, new com.xunxian.seekingimmortals.skill.effect.spell.ThunderStrikeSpell());
        register(SkillType.EARTH_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.EarthEscapeStepSpell());
        register(SkillType.FLYING_SWORD_BEGINNER, new com.xunxian.seekingimmortals.skill.effect.spell.FlyingSwordBeginnerSpell());
        register(SkillType.SINGLE_SWORD_THRUST, new com.xunxian.seekingimmortals.skill.effect.spell.SwordProjectileSpell(20, 20, 8.0D, 1));
        register(SkillType.THREE_TALENT_SWORD_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.SwordProjectileSpell(40, 60, 7.0D, 3));
        register(SkillType.ELEMENTAL_BURST_FIRE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.20D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.FIRE,
                "message.seeking_immortals.spell.elemental_burst_fire.success"));
        register(SkillType.ELEMENTAL_BURST_WATER, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.18D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WATER,
                "message.seeking_immortals.spell.elemental_burst_water.success"));
        register(SkillType.ELEMENTAL_BURST_EARTH, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.02D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.EARTH,
                "message.seeking_immortals.spell.elemental_burst_earth.success"));
        register(SkillType.ELEMENTAL_BURST_WIND, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.38D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WIND,
                "message.seeking_immortals.spell.elemental_burst_wind.success"));
        register(SkillType.ELEMENTAL_BURST_METAL, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.28D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.METAL,
                "message.seeking_immortals.spell.elemental_burst_metal.success"));
        register(SkillType.ELEMENTAL_BURST_WOOD, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.10D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WOOD,
                "message.seeking_immortals.spell.elemental_burst_wood.success"));
        register(SkillType.ELEMENTAL_BURST_ICE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.12D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.ICE,
                "message.seeking_immortals.spell.elemental_burst_ice.success"));
        register(SkillType.ELEMENTAL_BURST_THUNDER, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.45D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.THUNDER,
                "message.seeking_immortals.spell.elemental_burst_thunder.success"));
        register(SkillType.ICE_SPEAR, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.30D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.ICE_SPEAR,
                "message.seeking_immortals.spell.ice_spear.success"));
        register(SkillType.FLAME_BURST, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 37.0D, 1.18D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.FLAME_BURST,
                "message.seeking_immortals.spell.flame_burst.success"));
        register(SkillType.HUANGFENG_FIRE_SERPENT, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 150, 42.0D, 1.32D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.FIRE_SERPENT,
                "message.seeking_immortals.spell.huangfeng_fire_serpent.success"));
        register(SkillType.LUOYUN_SPIRIT_FLAME, new com.xunxian.seekingimmortals.skill.effect.spell.SpiritFlameBeamSpell());
        register(SkillType.ICE_JADE_SHIELD, new com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell(22, 180, 0.0D, 18.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell.UtilityElement.ICE_JADE_SHIELD,
                "message.seeking_immortals.spell.ice_jade_shield.success"));
        register(SkillType.WOOD_SPIRIT_VINE, new com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell(20, 160, 8.0D, 20.0D, 1.3D,
                com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell.UtilityElement.WOOD_SPIRIT_VINE,
                "message.seeking_immortals.spell.wood_spirit_vine.success"));
        register(SkillType.WATER_MIRROR_REFLECT, new com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell(20, 180, 0.0D, 18.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell.UtilityElement.WATER_MIRROR_REFLECT,
                "message.seeking_immortals.spell.water_mirror_reflect.success"));
        register(SkillType.HEAL_QI, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(15, 120, 22.0D, 2.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.HEAL_QI,
                "message.seeking_immortals.spell.heal_qi.success"));
        register(SkillType.DETOXIFY, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(12, 110, 0.0D, 2.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.DETOXIFY,
                "message.seeking_immortals.spell.detoxify.success"));
        register(SkillType.SPIRIT_RECOVERY, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(12, 140, 8.0D, 2.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.SPIRIT_RECOVERY,
                "message.seeking_immortals.spell.spirit_recovery.success"));
        register(SkillType.BODY_REPAIR, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(20, 160, 9.0D, 2.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.BODY_REPAIR,
                "message.seeking_immortals.spell.body_repair.success"));
        register(SkillType.GROUP_HEAL, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(25, 240, 8.0D, 5.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.GROUP_HEAL,
                "message.seeking_immortals.spell.group_heal.success"));
        register(SkillType.REVIVE_WEAK, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(12, 220, 6.0D, 2.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.REVIVE_WEAK,
                "message.seeking_immortals.spell.revive_weak.success"));
        register(SkillType.SPIRIT_SHIELD, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(10, 180, 0.0D, 3.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.SPIRIT_SHIELD,
                "message.seeking_immortals.spell.spirit_shield.success"));
        register(SkillType.TRIBULATION_THUNDER_WARD, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(34, 360, 0.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.TRIBULATION_THUNDER_WARD,
                "message.seeking_immortals.spell.tribulation_thunder_ward.success"));
        register(SkillType.MIRROR_PHANTOM, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(8, 100, 10.0D, 18.0D, 0.85D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.MIRROR_PHANTOM,
                "message.seeking_immortals.spell.mirror_phantom.success"));
        register(SkillType.HUNDRED_ILLUSION, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(17, 180, 35.0D, 20.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.HUNDRED_ILLUSION,
                "message.seeking_immortals.spell.hundred_illusion.success"));
        register(SkillType.MIND_CONFUSION, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(9, 120, 18.0D, 18.0D, 0.95D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.MIND_CONFUSION,
                "message.seeking_immortals.spell.mind_confusion.success"));
        register(SkillType.VOID_STEP, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(11, 120, 22.0D, 8.5D, 1.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VOID_STEP,
                "message.seeking_immortals.spell.void_step.success"));
        register(SkillType.DREAM_SNARE, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(13, 150, 26.0D, 20.0D, 1.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.DREAM_SNARE,
                "message.seeking_immortals.spell.dream_snare.success"));
        register(SkillType.CLONE_IMAGE, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(12, 180, 0.0D, 0.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.CLONE_IMAGE,
                "message.seeking_immortals.spell.clone_image.success"));
        register(SkillType.VEIL_OF_MOON, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(20, 200, 0.0D, 0.0D, 3.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VEIL_OF_MOON,
                "message.seeking_immortals.spell.veil_of_moon.success"));
        register(SkillType.INVISIBILITY_BASIC, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 220, 0.0D, 0.0D, 2.6D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.INVISIBILITY_BASIC,
                "message.seeking_immortals.spell.invisibility_basic.success"));
        register(SkillType.ILLUSION_MIST, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 180, 0.0D, 0.0D, 4.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.ILLUSION_MIST,
                "message.seeking_immortals.spell.illusion_mist.success"));
        register(SkillType.INVERSE_STAR_VEIL, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 220, 0.0D, 0.0D, 4.6D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.INVERSE_STAR_VEIL,
                "message.seeking_immortals.spell.inverse_star_veil.success"));
        register(SkillType.YANYUE_MOON_ILLUSION, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 220, 0.0D, 0.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.YANYUE_MOON_ILLUSION,
                "message.seeking_immortals.spell.yanyue_moon_illusion.success"));
        register(SkillType.YANYUE_PHANTOM_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(38, 300, 72.0D, 24.0D, 6.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.YANYUE_PHANTOM_ARRAY,
                "message.seeking_immortals.spell.yanyue_phantom_array.success"));
        register(SkillType.WANHU_NINE_ILLUSION, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(26, 260, 62.0D, 22.0D, 5.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.WANHU_NINE_ILLUSION,
                "message.seeking_immortals.spell.wanhu_nine_illusion.success"));
        register(SkillType.SOUL_DEVOURING_CLOUD, new com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell(8, 180, 6.0D, 18.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell.XuanYinForm.SOUL_DEVOURING_CLOUD,
                "message.seeking_immortals.spell.soul_devouring_cloud.success"));
        register(SkillType.YIN_SOUL_CHAIN, new com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell(14, 140, 28.0D, 20.0D, 0.95D,
                com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell.XuanYinForm.YIN_SOUL_CHAIN,
                "message.seeking_immortals.spell.yin_soul_chain.success"));
        register(SkillType.UNDERWORLD_FLAME, new com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell(19, 150, 38.0D, 22.0D, 0.72D,
                com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell.XuanYinForm.UNDERWORLD_FLAME,
                "message.seeking_immortals.spell.underworld_flame.success"));
        register(SkillType.CORPSE_ARMOR, new com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell(12, 220, 0.0D, 0.0D, 2.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell.XuanYinForm.CORPSE_ARMOR,
                "message.seeking_immortals.spell.corpse_armor.success"));
        register(SkillType.QINGYUAN_SWORD_RAY, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(22, 150, 45.0D, 24.0D, 0.62D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.QINGYUAN_SWORD_RAY,
                "message.seeking_immortals.spell.qingyuan_sword_ray.success"));
        register(SkillType.FLYING_SWORD_STRIKE, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(19, 120, 38.0D, 22.0D, 0.9D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.FLYING_SWORD_STRIKE,
                "message.seeking_immortals.spell.flying_sword_strike.success"));
        register(SkillType.GREEN_BAMBOO_SWORD_QI, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(20, 140, 40.0D, 23.0D, 0.68D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.GREEN_BAMBOO_SWORD_QI,
                "message.seeking_immortals.spell.green_bamboo_sword_qi.success"));
        register(SkillType.SWORD_SHIELD, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(12, 180, 0.0D, 0.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.SWORD_SHIELD,
                "message.seeking_immortals.spell.sword_shield.success"));
        register(SkillType.SWORD_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(15, 150, 30.0D, 14.0D, 1.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.SWORD_ESCAPE,
                "message.seeking_immortals.spell.sword_escape.success"));
        register(SkillType.THOUSAND_SWORD_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(40, 240, 80.0D, 22.0D, 4.6D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.THOUSAND_SWORD_ARRAY,
                "message.seeking_immortals.spell.thousand_sword_array.success"));
        register(SkillType.BLOOD_SWORD_SLASH, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(25, 150, 50.0D, 7.5D, 1.35D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.BLOOD_SWORD_SLASH,
                "message.seeking_immortals.spell.blood_sword_slash.success"));
        register(SkillType.SWORD_MERGE, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(12, 220, 0.0D, 0.0D, 2.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.SWORD_MERGE,
                "message.seeking_immortals.spell.sword_merge.success"));
        register(SkillType.INVISIBLE_SWORD, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(26, 160, 52.0D, 26.0D, 0.44D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.INVISIBLE_SWORD,
                "message.seeking_immortals.spell.invisible_sword.success"));
        register(SkillType.SWORD_DOMAIN, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(35, 260, 70.0D, 0.0D, 5.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.SWORD_DOMAIN,
                "message.seeking_immortals.spell.sword_domain.success"));
        register(SkillType.DUAL_SWORD_DANCE, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(24, 130, 48.0D, 8.5D, 1.15D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.DUAL_SWORD_DANCE,
                "message.seeking_immortals.spell.dual_sword_dance.success"));
        register(SkillType.PRIMORDIAL_MAGNET_SPHERE, new com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell(26, 220, 80.0D, 24.0D, 4.3D,
                com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell.CoreElement.PRIMORDIAL_MAGNET,
                "message.seeking_immortals.spell.primordial_magnet_sphere.success"));
        register(SkillType.FLAME_SERPENT_STORM, new com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell(30, 220, 75.0D, 21.0D, 4.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell.CoreElement.FLAME_SERPENT_STORM,
                "message.seeking_immortals.spell.flame_serpent_storm.success"));
        register(SkillType.EARTH_MOUNTAIN_PRESS, new com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell(28, 210, 65.0D, 20.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell.CoreElement.EARTH_MOUNTAIN_PRESS,
                "message.seeking_immortals.spell.earth_mountain_press.success"));
        register(SkillType.XUANTIAN_ICE_PRISON, new com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell(30, 240, 42.0D, 20.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.CoreElementalAreaSpell.CoreElement.XUANTIAN_ICE_PRISON,
                "message.seeking_immortals.spell.xuantian_ice_prison.success"));
        register(SkillType.SENSE_SCAN, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(12, 120, 0.0D, 22.0D, 0.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_SCAN,
                "message.seeking_immortals.spell.sense_scan.success"));
        register(SkillType.SENSE_PRESSURE, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(12, 130, 25.0D, 20.0D, 0.95D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_PRESSURE,
                "message.seeking_immortals.spell.sense_pressure.success"));
        register(SkillType.SENSE_NEEDLE, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(15, 140, 30.0D, 22.0D, 0.45D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_NEEDLE,
                "message.seeking_immortals.spell.sense_needle.success"));
        register(SkillType.SENSE_DOMAIN, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(22, 190, 45.0D, 0.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_DOMAIN,
                "message.seeking_immortals.spell.sense_domain.success"));
        register(SkillType.MIND_READ, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(12, 160, 0.0D, 18.0D, 0.9D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.MIND_READ,
                "message.seeking_immortals.spell.mind_read.success"));
        register(SkillType.SENSE_LOCK, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(20, 180, 40.0D, 24.0D, 1.15D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_LOCK,
                "message.seeking_immortals.spell.sense_lock.success"));
        register(SkillType.DIVINE_SENSE_SCAN, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(18, 180, 0.0D, 32.0D, 0.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.DIVINE_SENSE_SCAN,
                "message.seeking_immortals.spell.divine_sense_scan.success"));
        register(SkillType.DIVINE_SENSE_LOCK, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(26, 220, 55.0D, 30.0D, 1.45D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.DIVINE_SENSE_LOCK,
                "message.seeking_immortals.spell.divine_sense_lock.success"));
        register(SkillType.SOUL_ATTACK_WAVE, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(26, 200, 60.0D, 24.0D, 1.85D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SOUL_ATTACK_WAVE,
                "message.seeking_immortals.spell.soul_attack_wave.success"));
        register(SkillType.SOUL_CRY_SHOCK, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(26, 240, 60.0D, 0.0D, 6.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SOUL_CRY_SHOCK,
                "message.seeking_immortals.spell.soul_cry_shock.success"));
        register(SkillType.BLOOD_SHADOW_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(35, 200, 58.0D, 16.0D, 1.25D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.BLOOD_SHADOW_ESCAPE,
                "message.seeking_immortals.spell.blood_shadow_escape.success"));
        register(SkillType.SKY_SUPPORTING_DEMONIC_SKILL, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(32, 260, 0.0D, 0.0D, 4.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.SKY_SUPPORTING_DEMONIC_SKILL,
                "message.seeking_immortals.spell.sky_supporting_demonic_skill.success"));
        register(SkillType.MYSTIC_SOUL_GHOST_FIRE, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(30, 180, 82.0D, 24.0D, 0.85D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.MYSTIC_SOUL_GHOST_FIRE,
                "message.seeking_immortals.spell.mystic_soul_ghost_fire.success"));
        register(SkillType.MYSTIC_SOUL_BONE_CONDENSING_ART, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(24, 240, 0.0D, 0.0D, 3.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.MYSTIC_SOUL_BONE_CONDENSING_ART,
                "message.seeking_immortals.spell.mystic_soul_bone_condensing_art.success"));
        register(SkillType.BLOOD_LUO_BARRIER, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(34, 240, 30.0D, 0.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.BLOOD_LUO_BARRIER,
                "message.seeking_immortals.spell.blood_luo_barrier.success"));
        register(SkillType.YIN_DEMON_SLASH, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(32, 190, 88.0D, 18.0D, 1.1D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.YIN_DEMON_SLASH,
                "message.seeking_immortals.spell.yin_demon_slash.success"));
        register(SkillType.FIVE_ELEMENT_FUSION_BURST, new com.xunxian.seekingimmortals.skill.effect.spell.SecretElementalSpell(48, 320, 80.0D, 24.0D, 4.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.SecretElementalSpell.SecretElement.FIVE_ELEMENT_FUSION,
                "message.seeking_immortals.spell.five_element_fusion_burst.success"));
        register(SkillType.LIFE_FIRE, new com.xunxian.seekingimmortals.skill.effect.spell.SecretElementalSpell(42, 320, 85.0D, 24.0D, 0.95D,
                com.xunxian.seekingimmortals.skill.effect.spell.SecretElementalSpell.SecretElement.LIFE_FIRE,
                "message.seeking_immortals.spell.life_fire.success"));
        register(SkillType.LIEYAN_TRUE_FIRE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.SecretElementalSpell(54, 360, 90.0D, 26.0D, 4.6D,
                com.xunxian.seekingimmortals.skill.effect.spell.SecretElementalSpell.SecretElement.TRUE_FIRE_HEAVEN,
                "message.seeking_immortals.spell.lieyan_true_fire_secret.success"));
        register(SkillType.DIVINE_SENSE_EXPANSION, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseExpansionPassive());
        register(SkillType.FLYING_SWORD_ADVANCED, new com.xunxian.seekingimmortals.skill.effect.spell.FlyingSwordAdvancedSpell());
        register(SkillType.AURA_BODY_SHIELD, new com.xunxian.seekingimmortals.skill.effect.spell.AuraBodyShieldSpell());
        register(SkillType.WATER_SHIELD, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(30, 220,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 180, 0,
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 180, 0,
                net.minecraft.core.particles.ParticleTypes.BUBBLE,
                net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                "message.seeking_immortals.spell.water_shield.success"));
        register(SkillType.EARTH_PRISON, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(35, 180, 3.0D, 20.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 4,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 120, 1,
                net.minecraft.core.particles.ParticleTypes.POOF,
                net.minecraft.sounds.SoundEvents.STONE_PLACE,
                "message.seeking_immortals.spell.earth_prison.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.WIND_BINDING, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(28, 160, 1.0D, 22.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 3,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0,
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                net.minecraft.sounds.SoundEvents.TRIDENT_THROW,
                "message.seeking_immortals.spell.wind_binding.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.WIND_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.WindWallSpell(32, 200, 160, 4.5D));
        register(SkillType.FIVE_ELEMENTS_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.FiveElementsEscapeSpell());
        register(SkillType.BIG_DIPPER_SWORD_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.MultiSwordArraySpell(80, 160, 9.0D, 7, "北斗剑阵七星齐出。"));
        register(SkillType.FORMATION_SENSE, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSenseSpell());
        register(SkillType.DETECTION, new com.xunxian.seekingimmortals.skill.effect.spell.DetectionSpell());
        register(SkillType.INVISIBILITY, new com.xunxian.seekingimmortals.skill.effect.spell.InvisibilitySpell());
        register(SkillType.LIGHTNESS_SKILL, new com.xunxian.seekingimmortals.skill.effect.spell.LightBodySpell());
        register(SkillType.EARTH_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.EarthWallSpell());
        register(SkillType.ICE_SHARD, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 40, 13.0D, 1.20D,
                        com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.ICE,
                        "message.seeking_immortals.spell.ice_shard.success"));
        register(SkillType.WOOD_BIND, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(8, 80, 2.0D, 18.0D,
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 90, 3,
                        net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 90, 0,
                        net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        net.minecraft.sounds.SoundEvents.GRASS_BREAK,
                        "message.seeking_immortals.spell.wood_bind.success",
                        "message.seeking_immortals.spell.target.fail"));
        register(SkillType.STEAM_CLOUD, new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(8, 100, 1.0D, 16.0D, 3.2D,
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2,
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0,
                        net.minecraft.core.particles.ParticleTypes.CLOUD,
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                        "message.seeking_immortals.spell.steam_cloud.success",
                        "message.seeking_immortals.spell.area.fail"));
        register(SkillType.EVIL_WARD_THUNDER, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(26, 160, 49.0D, 1.40D,
                        com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.THUNDER,
                        "message.seeking_immortals.spell.evil_ward_thunder.success"));
        register(SkillType.METAL_SWORD_FINGER, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(12, 80, 40.0D, 22.0D, 0.55D,
                        com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.QINGYUAN_SWORD_RAY,
                        "message.seeking_immortals.spell.metal_sword_finger.success"));
        register(SkillType.GREEN_BAMBOO_SWORD_RAY, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(26, 150, 49.0D, 24.0D, 0.60D,
                        com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.GREEN_BAMBOO_SWORD_QI,
                        "message.seeking_immortals.spell.green_bamboo_sword_ray.success"));
        register(SkillType.STAR_SWORD_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(35, 220, 70.0D, 22.0D, 4.5D,
                        com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.THOUSAND_SWORD_ARRAY,
                        "message.seeking_immortals.spell.star_sword_array.success"));
        register(SkillType.SWORD_FORMATION_BASIC, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(26, 180, 49.0D, 22.0D, 3.8D,
                        com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SMALL_SWORD_ARRAY,
                        "message.seeking_immortals.spell.sword_formation_basic.success"));
        register(SkillType.QINGLUO_POISON_NEEDLE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 100, 35.0D, 1.25D,
                        com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.DARK,
                        "message.seeking_immortals.spell.qingluo_poison_needle.success"));
        register(SkillType.DEMON_FLAME, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(21, 160, 42.0D, 18.0D, 3.6D,
                        com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                        "message.seeking_immortals.spell.demon_flame.success"));
        register(SkillType.BLOOD_SACRIFICE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 200,
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 160, 1,
                        net.minecraft.world.effect.MobEffects.ABSORPTION, 160, 0,
                        net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                        net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                        "message.seeking_immortals.spell.blood_sacrifice.success",
                        "berserk", 160, 0));
        register(SkillType.BLOOD_CURSE_MARK, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(28, 160, 12.0D, 20.0D,
                        net.minecraft.world.effect.MobEffects.WEAKNESS, 140, 1,
                        net.minecraft.world.effect.MobEffects.WITHER, 80, 0,
                        net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                        net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                        "message.seeking_immortals.spell.blood_curse_mark.success",
                        "message.seeking_immortals.spell.target.fail"));
        register(SkillType.DEMON_ARMOR, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 200,
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 180, 0,
                        net.minecraft.world.effect.MobEffects.ABSORPTION, 180, 1,
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                        "message.seeking_immortals.spell.demon_armor.success"));
        register(SkillType.FOX_PHANTOM_SHIFT, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 160, 0.0D, 0.0D, 3.5D,
                        com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VOID_STEP,
                        "message.seeking_immortals.spell.fox_phantom_shift.success"));
        register(SkillType.NETHER_GHOST_WALK, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 120,
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 140, 1,
                        net.minecraft.world.effect.MobEffects.JUMP, 140, 0,
                        net.minecraft.core.particles.ParticleTypes.SOUL,
                        net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                        "message.seeking_immortals.spell.nether_ghost_walk.success"));
        register(SkillType.YIN_SOUL_DEVOUR, new com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell(14, 140, 28.0D, 18.0D, 0.95D,
                        com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell.XuanYinForm.YIN_SOUL_CHAIN,
                        "message.seeking_immortals.spell.yin_soul_devour.success"));
        register(SkillType.SPIRIT_ART_WIND_BLADE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(8, 40, 14.0D, 1.35D,
                        com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WIND,
                        "message.seeking_immortals.spell.spirit_art_wind_blade.success"));
        register(SkillType.SPIRIT_ART_THUNDER, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(11, 60, 22.0D, 1.40D,
                        com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.THUNDER,
                        "message.seeking_immortals.spell.spirit_art_thunder.success"));
        register(SkillType.SPIRIT_ART_HEAL, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(15, 120, 22.0D, 2.0D,
                        com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.HEAL_QI,
                        "message.seeking_immortals.spell.spirit_art_heal.success"));
        register(SkillType.SPIRIT_ART_SAND_STORM, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(18, 140, 22.0D, 18.0D, 3.5D,
                        com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.SAND_STORM,
                        "message.seeking_immortals.spell.spirit_art_sand_storm.success"));
        register(SkillType.SPIRIT_ART_EARTH_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.EarthWallSpell());
        register(SkillType.SPIRIT_ART_HOLY_LIGHT, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(17, 140, 18.0D, 2.2D,
                        com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.BODY_REPAIR,
                        "message.seeking_immortals.spell.spirit_art_holy_light.success"));
        register(SkillType.SPIRIT_ART_WIND_RIDE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(10, 140,
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 1,
                        net.minecraft.world.effect.MobEffects.SLOW_FALLING, 160, 0,
                        net.minecraft.core.particles.ParticleTypes.CLOUD,
                        net.minecraft.sounds.SoundEvents.ELYTRA_FLYING,
                        "message.seeking_immortals.spell.spirit_art_wind_ride.success"));
        register(SkillType.VAJRA_BODY, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 200,
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 1,
                        net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 0,
                        net.minecraft.core.particles.ParticleTypes.CRIT,
                        net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                        "message.seeking_immortals.spell.vajra_body.success"));
        register(SkillType.IRON_SKIN, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 180,
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 180, 0,
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 100, 0,
                        net.minecraft.core.particles.ParticleTypes.CRIT,
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,
                        "message.seeking_immortals.spell.iron_skin.success"));
        register(SkillType.DRAGON_STRENGTH, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 180,
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 180, 1,
                        net.minecraft.world.effect.MobEffects.DIG_SPEED, 180, 0,
                        net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
                        "message.seeking_immortals.spell.dragon_strength.success"));
        register(SkillType.BODY_FLASH, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(10, 80, 0.0D, 0.0D, 2.0D,
                        com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.CLOUD_WALK,
                        "message.seeking_immortals.spell.body_flash.success"));
        register(SkillType.PALM_WIND, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(12, 60, 25.0D, 8.0D,
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1,
                        null, 0, 0,
                        net.minecraft.core.particles.ParticleTypes.CLOUD,
                        net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                        "message.seeking_immortals.spell.palm_wind.success",
                        "message.seeking_immortals.spell.target.fail"));
        register(SkillType.BONE_CRUSH, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(16, 80, 32.0D, 6.0D,
                        net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0,
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1,
                        net.minecraft.core.particles.ParticleTypes.CRIT,
                        net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                        "message.seeking_immortals.spell.bone_crush.success",
                        "message.seeking_immortals.spell.target.fail"));
        register(SkillType.TIANYUAN_JOINT_ARRAY, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(40, 260, 90.0D, 24.0D, 5.0D,
                        com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.KILL_SWORD_FORMATION,
                        "message.seeking_immortals.spell.tianyuan_joint_array.success"));
        register(SkillType.SOUL_DEVOUR, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(17, 140, 35.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 1,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                "message.seeking_immortals.spell.soul_devour.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.DEMON_FORM, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 220,
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 1,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 0,
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
                "message.seeking_immortals.spell.demon_form.success"));
        register(SkillType.DEMON_CONTRACT, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(19, 160, 38.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 140, 1,
                net.minecraft.world.effect.MobEffects.GLOWING, 100, 0,
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                "message.seeking_immortals.spell.demon_contract.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.HEHUAN_CHARM, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 140, 18.0D, 18.0D, 0.95D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.MIND_CONFUSION,
                "message.seeking_immortals.spell.hehuan_charm.success"));
        register(SkillType.BLOOD_CURSE_STRIKE, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(18, 120, 37.0D, 18.0D, 0.7D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.BLOOD_SWORD_SLASH,
                "message.seeking_immortals.spell.blood_curse_strike.success"));
        register(SkillType.POISON_MIST, new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(18, 150, 8.0D, 16.0D, 3.5D,
                net.minecraft.world.effect.MobEffects.POISON, 120, 0,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1,
                net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW,
                "message.seeking_immortals.spell.poison_mist.success",
                "message.seeking_immortals.spell.area.fail"));
        register(SkillType.DEMON_CLAW, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(18, 100, 37.0D, 7.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,
                "message.seeking_immortals.spell.demon_claw.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.BLOOD_DEMON_SLASH, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(28, 140, 85.0D, 8.0D, 1.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.BLOOD_SWORD_SLASH,
                "message.seeking_immortals.spell.blood_demon_slash.success"));
        register(SkillType.SOUL_BANNER_WAVE, new com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell(22, 180, 40.0D, 18.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.XuanYinSpell.XuanYinForm.SOUL_DEVOURING_CLOUD,
                "message.seeking_immortals.spell.soul_banner_wave.success"));
        register(SkillType.YIN_LUO_GHOST_CLOAK, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 200, 0.0D, 0.0D, 3.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.INVISIBILITY_BASIC,
                "message.seeking_immortals.spell.yin_luo_ghost_cloak.success"));
        register(SkillType.GHOST_WALK, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(18, 120,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 140, 1,
                net.minecraft.world.effect.MobEffects.INVISIBILITY, 80, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                "message.seeking_immortals.spell.ghost_walk.success"));
        register(SkillType.SHORT_TELEPORT, new com.xunxian.seekingimmortals.skill.effect.spell.EarthEscapeStepSpell());
        register(SkillType.WIND_RIDE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 120,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 1,
                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 160, 0,
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                net.minecraft.sounds.SoundEvents.ELYTRA_FLYING,
                "message.seeking_immortals.spell.wind_ride.success"));
        register(SkillType.SHADOW_FLASH, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(12, 100, 0.0D, 0.0D, 2.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VOID_STEP,
                "message.seeking_immortals.spell.shadow_flash.success"));
        register(SkillType.WATER_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(18, 140,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 1,
                net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE, 160, 0,
                net.minecraft.core.particles.ParticleTypes.BUBBLE,
                net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                "message.seeking_immortals.spell.water_escape.success"));
        register(SkillType.FIRE_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(18, 140,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 1,
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 160, 0,
                net.minecraft.core.particles.ParticleTypes.FLAME,
                net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,
                "message.seeking_immortals.spell.fire_escape.success"));
        register(SkillType.QI_BURST_PALM, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(14, 70, 28.0D, 7.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 0,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                "message.seeking_immortals.spell.qi_burst_palm.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.BODY_HARDNESS, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(26, 240,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 220, 1,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 220, 1,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                "message.seeking_immortals.spell.body_hardness.success"));
        register(SkillType.TIANMO_BLOOD_ARMOR, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(18, 200,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 180, 1,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 180, 0,
                net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                "message.seeking_immortals.spell.tianmo_blood_armor.success"));
        register(SkillType.SMUGGLE_RIFT_STEP, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(16, 120, 0.0D, 0.0D, 2.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VOID_STEP,
                "message.seeking_immortals.spell.smuggle_rift_step.success"));
        register(SkillType.INVERSE_STAR_SHADOW_STEP, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 140, 0.0D, 0.0D, 3.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.INVERSE_STAR_VEIL,
                "message.seeking_immortals.spell.inverse_star_shadow_step.success"));
        register(SkillType.XUEWU_BLOOD_CURSE_MARK, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(18, 150, 10.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 140, 1,
                net.minecraft.world.effect.MobEffects.GLOWING, 120, 0,
                net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                "message.seeking_immortals.spell.xuewu_blood_curse_mark.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.FIRE_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(10, 40, 16.0D, 1.15D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.FIRE,
                "message.seeking_immortals.spell.fire_talisman.success"));
        register(SkillType.ICE_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(9, 40, 14.0D, 1.15D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.ICE,
                "message.seeking_immortals.spell.ice_talisman.success"));
        register(SkillType.TELEPORT_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.EarthEscapeStepSpell());
        register(SkillType.SPIRIT_SHIELD_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 160,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 160, 0,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 160, 0,
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.spirit_shield_talisman.success"));
        register(SkillType.EXPLOSION_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(17, 140, 28.0D, 16.0D, 3.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.explosion_talisman.success"));
        register(SkillType.WIND_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 120,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 1,
                net.minecraft.world.effect.MobEffects.JUMP, 160, 0,
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                net.minecraft.sounds.SoundEvents.ELYTRA_FLYING,
                "message.seeking_immortals.spell.wind_talisman.success"));
        register(SkillType.SEAL_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(12, 100, 4.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 3,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 100, 1,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                "message.seeking_immortals.spell.seal_talisman.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.GHOST_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(14, 120, 22.0D, 16.0D, 3.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CHAIN_THUNDER,
                "message.seeking_immortals.spell.ghost_talisman.success"));
        register(SkillType.SPIRIT_CHAIN_TALISMAN_CAST, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(16, 140, 8.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 3,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                net.minecraft.sounds.SoundEvents.CHAIN_PLACE,
                "message.seeking_immortals.spell.spirit_chain_talisman_cast.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.GOLDEN_ARMOR_TALISMAN_CAST, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(14, 180,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 180, 1,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 180, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GOLD,
                "message.seeking_immortals.spell.golden_armor_talisman_cast.success"));
        register(SkillType.THUNDER_TALISMAN_STORM, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(32, 200, 55.0D, 20.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CHAIN_THUNDER,
                "message.seeking_immortals.spell.thunder_talisman_storm.success"));
        register(SkillType.PALM_THUNDER, new com.xunxian.seekingimmortals.skill.effect.spell.ThunderPalmSpell());
        register(SkillType.BLOOD_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(10, 100,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 120, 1,
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 80, 0,
                net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                "message.seeking_immortals.spell.blood_escape.success"));
        register(SkillType.EARTH_BURROW, new com.xunxian.seekingimmortals.skill.effect.spell.EarthEscapeStepSpell());
        register(SkillType.GREEN_SHIELD, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(10, 160,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 160, 0,
                net.minecraft.world.effect.MobEffects.REGENERATION, 80, 0,
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.green_shield.success"));
        register(SkillType.SOUL_CHOP, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(10, 80, 20.0D, 1.20D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.DARK,
                "message.seeking_immortals.spell.soul_chop.success"));
        register(SkillType.DEMON_ROAR, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(10, 120, 18.0D, 14.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CYCLONE,
                "message.seeking_immortals.spell.demon_roar.success"));
        register(SkillType.SPIRIT_NEEDLE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(10, 50, 15.0D, 1.30D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.METAL,
                "message.seeking_immortals.spell.spirit_needle.success"));
        register(SkillType.ICE_PRISON, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(10, 140, 6.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 4,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 100, 1,
                net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                "message.seeking_immortals.spell.ice_prison.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.FIRE_RAIN, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(10, 140, 20.0D, 16.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.fire_rain.success"));
        register(SkillType.SAND_BURY, new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(10, 120, 2.0D, 14.0D, 3.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 3,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0,
                net.minecraft.core.particles.ParticleTypes.POOF,
                net.minecraft.sounds.SoundEvents.SAND_BREAK,
                "message.seeking_immortals.spell.sand_bury.success",
                "message.seeking_immortals.spell.area.fail"));
        register(SkillType.GHOST_BIND, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(10, 100, 5.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 3,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                "message.seeking_immortals.spell.ghost_bind.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.CORPSE_EXPLODE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(10, 160, 24.0D, 14.0D, 3.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.corpse_explode.success"));
        register(SkillType.SWORD_RAIN, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(10, 160, 30.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.THOUSAND_SWORD_ARRAY,
                "message.seeking_immortals.spell.sword_rain.success"));
        register(SkillType.MIRROR_REFLECT, new com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell(10, 180, 0.0D, 16.0D, 4.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.FoundationElementalUtilitySpell.UtilityElement.WATER_MIRROR_REFLECT,
                "message.seeking_immortals.spell.mirror_reflect.success"));
        register(SkillType.WATER_DRAGON, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(10, 90, 22.0D, 1.18D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WATER,
                "message.seeking_immortals.spell.water_dragon.success"));
        register(SkillType.GOLD_NEEDLE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(10, 40, 14.0D, 1.28D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.METAL,
                "message.seeking_immortals.spell.gold_needle.success"));
        register(SkillType.YIN_FIRE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(10, 140, 22.0D, 15.0D, 3.3D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.yin_fire.success"));
        register(SkillType.YANG_BURST, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(10, 140, 24.0D, 15.0D, 3.4D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.yang_burst.success"));
        register(SkillType.FIVE_ELEMENTS_PALM, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(10, 70, 20.0D, 7.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 0,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                "message.seeking_immortals.spell.five_elements_palm.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.SOUL_SEARCH_SPELL, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(12, 160, 0.0D, 18.0D, 0.9D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.MIND_READ,
                "message.seeking_immortals.spell.soul_search_spell.success"));
        register(SkillType.SPIRIT_SEAL, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(12, 140, 6.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                "message.seeking_immortals.spell.spirit_seal.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.DEMON_SUBDUE_SEAL, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(12, 140, 28.0D, 16.0D, 1.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.DEMON_SUBDUE_PALM,
                "message.seeking_immortals.spell.demon_subdue_seal.success"));
        register(SkillType.NASCENT_SOUL_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(12, 160, 0.0D, 0.0D, 3.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VOID_STEP,
                "message.seeking_immortals.spell.nascent_soul_escape.success"));
        register(SkillType.DOMAIN_COMPRESS, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(45, 240, 50.0D, 20.0D, 4.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CYCLONE,
                "message.seeking_immortals.spell.domain_compress.success"));
        register(SkillType.VOID_REFINE_TOUCH, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(37, 180, 30.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 140, 1,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1,
                net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                "message.seeking_immortals.spell.void_refine_touch.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.SPIRIT_SENSE_MERGE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 220,
                net.minecraft.world.effect.MobEffects.NIGHT_VISION, 200, 0,
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 160, 0,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.spirit_sense_merge.success"));
        register(SkillType.SPACE_TEAR_SLASH, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(47, 160, 70.0D, 22.0D, 0.55D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.QINGYUAN_SWORD_RAY,
                "message.seeking_immortals.spell.space_tear_slash.success"));
        register(SkillType.TRIBULATION_REDIRECT, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(12, 260, 15.0D, 20.0D,
                net.minecraft.world.effect.MobEffects.GLOWING, 160, 0,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 0,
                net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                "message.seeking_immortals.spell.tribulation_redirect.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.BLOODLINE_AWAKEN, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 240,
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 1,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 200, 0,
                net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                net.minecraft.sounds.SoundEvents.TOTEM_USE,
                "message.seeking_immortals.spell.bloodline_awaken.success"));
        register(SkillType.MULAN_WIND_BLADE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(18, 100, 30.0D, 1.35D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.WIND,
                "message.seeking_immortals.spell.mulan_wind_blade.success"));
        register(SkillType.MULAN_EARTH_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.EarthWallSpell());
        register(SkillType.SPIRIT_ART_HOLY_FEATHER_GUARD, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(35, 200,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 1,
                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 200, 0,
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.spirit_art_holy_feather_guard.success"));
        register(SkillType.PUPPET_SELF_DESTRUCT, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(30, 200, 40.0D, 14.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.puppet_self_destruct.success"));
        register(SkillType.SPIRIT_THREAD_CONTROL, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(10, 140,
                net.minecraft.world.effect.MobEffects.DIG_SPEED, 160, 0,
                net.minecraft.world.effect.MobEffects.NIGHT_VISION, 160, 0,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                "message.seeking_immortals.spell.spirit_thread_control.success"));
        register(SkillType.ARRAY_PUPPET, new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(19, 180, 5.0D, 14.0D, 3.2D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 2,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                "message.seeking_immortals.spell.array_puppet.success",
                "message.seeking_immortals.spell.area.fail"));
        register(SkillType.PUPPET_ARRAY_TRAP, new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(20, 180, 6.0D, 15.0D, 3.3D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 140, 3,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 120, 1,
                net.minecraft.core.particles.ParticleTypes.POOF,
                net.minecraft.sounds.SoundEvents.STONE_PLACE,
                "message.seeking_immortals.spell.puppet_array_trap.success",
                "message.seeking_immortals.spell.area.fail"));
        register(SkillType.PUPPET_SUMMON_BASIC, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(26, 220, "puppet_summon_basic", 1, 0, 240, "message.seeking_immortals.spell.puppet_summon_basic.success"));
        register(SkillType.CORPSE_EXPLOSION, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(20, 160, 35.0D, 14.0D, 3.3D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.corpse_explosion.success"));
        register(SkillType.SOUL_BANNER, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(11, 140, 18.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 1,
                net.minecraft.world.effect.MobEffects.GLOWING, 100, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                "message.seeking_immortals.spell.soul_banner.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.SOUL_ANCHOR_RITE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(25, 220,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 0,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.soul_anchor_rite.success"));
        register(SkillType.YIN_SOUL_BURST, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(55, 260, 60.0D, 18.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.BLIZZARD,
                "message.seeking_immortals.spell.yin_soul_burst.success"));
        register(SkillType.POLUO_SOUL_PULL, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(18, 150, 20.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 120, 2,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                "message.seeking_immortals.spell.poluo_soul_pull.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.HEAVEN_DEMON_HAND, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(35, 120, 55.0D, 8.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 1,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,
                "message.seeking_immortals.spell.heaven_demon_hand.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.DEMON_QI_BURST, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(32, 180, 48.0D, 16.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.demon_qi_burst.success"));
        register(SkillType.SOUL_CONTRACT, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(20, 160, 25.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 140, 1,
                net.minecraft.world.effect.MobEffects.GLOWING, 120, 0,
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                "message.seeking_immortals.spell.soul_contract.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.DEMON_SOUL_DEVOUR, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(27, 160, 35.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 140, 1,
                net.minecraft.world.effect.MobEffects.WITHER, 80, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                "message.seeking_immortals.spell.demon_soul_devour.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.BARBARIAN_ROAR, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(50, 200, 55.0D, 16.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CYCLONE,
                "message.seeking_immortals.spell.barbarian_roar.success"));
        register(SkillType.HEHUAN_SOUL_CHARM, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(18, 140, 18.0D, 18.0D, 0.95D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.MIND_CONFUSION,
                "message.seeking_immortals.spell.hehuan_soul_charm.success"));
        register(SkillType.SPIRIT_ART_LIGHTNING_PALM, new com.xunxian.seekingimmortals.skill.effect.spell.ThunderPalmSpell());
        register(SkillType.SPIRIT_FENGYUAN_WIND_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.WindWallSpell(18, 160, 160, 4.0D));
        register(SkillType.SPIRIT_ART_ARRAY_ANCHOR, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(20, 180,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 180, 0,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 180, 0,
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.spirit_art_array_anchor.success"));
        register(SkillType.YIN_BODY_CONDENSE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(26, 200,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 1,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                "message.seeking_immortals.spell.yin_body_condense.success"));
        register(SkillType.SPIRIT_WALL, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(12, 160, 0.0D, 3.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.SPIRIT_SHIELD,
                "message.seeking_immortals.spell.spirit_wall.success"));
        register(SkillType.BLOOD_SWORD, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(12, 80, 28.0D, 18.0D, 0.7D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.BLOOD_SWORD_SLASH,
                "message.seeking_immortals.spell.blood_sword.success"));
        register(SkillType.PHANTOM_CLONE, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(12, 180, 0.0D, 0.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.CLONE_IMAGE,
                "message.seeking_immortals.spell.phantom_clone.success"));
        register(SkillType.SKY_HOOK, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(12, 90, 22.0D, 1.20D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.DARK,
                "message.seeking_immortals.spell.sky_hook.success"));
        register(SkillType.GROUND_SPIKE, new com.xunxian.seekingimmortals.skill.effect.spell.AreaDebuffSpell(12, 120, 8.0D, 14.0D, 3.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.POOF,
                net.minecraft.sounds.SoundEvents.STONE_BREAK,
                "message.seeking_immortals.spell.ground_spike.success",
                "message.seeking_immortals.spell.area.fail"));
        register(SkillType.LIGHTNING_CHAIN, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(14, 140, 26.0D, 16.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CHAIN_THUNDER,
                "message.seeking_immortals.spell.lightning_chain.success"));
        register(SkillType.FROST_BREATH, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(12, 100, 18.0D, 12.0D, 3.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.BLIZZARD,
                "message.seeking_immortals.spell.frost_breath.success"));
        register(SkillType.MOLTEN_SPLASH, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(12, 130, 24.0D, 14.0D, 3.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.LAVA,
                "message.seeking_immortals.spell.molten_splash.success"));
        register(SkillType.SPIRIT_ABSORB, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(14, 140, 20.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 1,
                net.minecraft.world.effect.MobEffects.HUNGER, 80, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                "message.seeking_immortals.spell.spirit_absorb.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.BONE_ARMOR, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 180,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 180, 0,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 180, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC,
                "message.seeking_immortals.spell.bone_armor.success"));
        register(SkillType.STAR_FALL, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(16, 160, 32.0D, 16.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.SAND_STORM,
                "message.seeking_immortals.spell.star_fall.success"));
        register(SkillType.VOID_SLASH, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(14, 90, 30.0D, 8.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                "message.seeking_immortals.spell.void_slash.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.HEAVEN_PUNISH, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell(18, 180, 36.0D, 16.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.ElementalAreaSpell.AreaElement.CHAIN_THUNDER,
                "message.seeking_immortals.spell.heaven_punish.success"));
        register(SkillType.BEAST_TAME_CALL, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(16, 140, 12.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0,
                net.minecraft.world.effect.MobEffects.GLOWING, 100, 0,
                net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
                "message.seeking_immortals.spell.beast_tame_call.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.SPIRIT_BEAST_CONTRACT, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(18, 180,
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 160, 0,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 0,
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.spirit_beast_contract.success"));
        register(SkillType.VOID_RIFT_SLASH, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(40, 160, 75.0D, 22.0D, 0.55D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.QINGYUAN_SWORD_RAY,
                "message.seeking_immortals.spell.void_rift_slash.success"));
        register(SkillType.GREAT_VEHICLE_PALM, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(60, 200, 80.0D, 10.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1,
                null, 0, 0,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                "message.seeking_immortals.spell.great_vehicle_palm.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.DEFENSIVE_PEARL_LIGHT, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(28, 200,
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 1,
                net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 1,
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                "message.seeking_immortals.spell.defensive_pearl_light.success"));
        register(SkillType.MAGNET_CONTROL, new com.xunxian.seekingimmortals.skill.effect.spell.ObjectControlSpell(26, 160, 18.0D));
        register(SkillType.NASCENT_SOUL_AVATAR, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(12, 220, "nascent_soul_avatar", 1, 1, 260, "message.seeking_immortals.spell.nascent_soul_avatar.success"));
        register(SkillType.DEITY_TRANSFORMATION_DOMAIN, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(48, 260, 70.0D, 22.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.KILL_SWORD_FORMATION,
                "message.seeking_immortals.spell.deity_transformation_domain.success"));
        register(SkillType.LINGZU_PEARL_BARRAGE, new com.xunxian.seekingimmortals.skill.effect.spell.ElementalProjectileSpell(40, 140, 50.0D, 1.25D,
                com.xunxian.seekingimmortals.entity.CultivationFireballEntity.SpellElement.LIGHT,
                "message.seeking_immortals.spell.lingzu_pearl_barrage.success"));
        register(SkillType.STAR_PALACE_SEAL_BURST, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(20, 160, 40.0D, 18.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.STAR_PALACE_SEAL,
                "message.seeking_immortals.spell.star_palace_seal_burst.success"));
        register(SkillType.RIVER_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(12, 120,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 140, 1,
                net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE, 140, 0,
                net.minecraft.core.particles.ParticleTypes.BUBBLE,
                net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                "message.seeking_immortals.spell.river_escape.success"));
        register(SkillType.WIND_ESCAPE_MOVE, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(18, 140,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 160, 1,
                net.minecraft.world.effect.MobEffects.SLOW_FALLING, 160, 0,
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                net.minecraft.sounds.SoundEvents.ELYTRA_FLYING,
                "message.seeking_immortals.spell.wind_escape.success"));
        register(SkillType.DAYAN_EYE, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(14, 160, 0.0D, 28.0D, 0.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_SCAN,
                "message.seeking_immortals.spell.dayan_eye.success"));
        register(SkillType.TIANFU_GOLDEN_CHAIN, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(18, 150, 10.0D, 18.0D,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 140, 3,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 120, 1,
                net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.CHAIN_PLACE,
                "message.seeking_immortals.spell.tianfu_golden_chain.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.BLOOD_CORRUPTION, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(16, 140, 12.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.WITHER, 100, 0,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 0,
                net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                "message.seeking_immortals.spell.blood_corruption.success",
                "message.seeking_immortals.spell.target.fail"));
        register(SkillType.YIN_CORROSION, new com.xunxian.seekingimmortals.skill.effect.spell.TargetedDebuffSpell(16, 140, 12.0D, 16.0D,
                net.minecraft.world.effect.MobEffects.POISON, 120, 0,
                net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 0,
                net.minecraft.core.particles.ParticleTypes.SOUL,
                net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                "message.seeking_immortals.spell.yin_corrosion.success",
                "message.seeking_immortals.spell.target.fail"));

        // Text-material id aliases for already-wired techniques
register(SkillType.SPIRIT_ART_BEAST_CALL, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(12, 220, "spirit_art_beast_call", 0, 0, 220, "message.seeking_immortals.spell.spirit_art_beast_call.success"));
        register(SkillType.SPIRIT_ART_HOLY_BEAST_CALL, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(25, 220, "spirit_art_holy_beast_call", 1, 0, 240, "message.seeking_immortals.spell.spirit_art_holy_beast_call.success"));
        register(SkillType.GHOST_KING_AVATAR, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(40, 220, "ghost_king_avatar", 2, 1, 260, "message.seeking_immortals.spell.ghost_king_avatar.success"));
        register(SkillType.GUILING_CORPSE_SUMMON, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(18, 220, "guiling_corpse_summon", 1, 0, 240, "message.seeking_immortals.spell.guiling_corpse_summon.success"));
        register(SkillType.BEAST_SUMMON, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(10, 220, "beast_summon", 0, 0, 220, "message.seeking_immortals.spell.beast_summon.success"));
        register(SkillType.GOLD_DEVOUR_SWARM, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(26, 220, "gold_devour_swarm", 1, 0, 240, "message.seeking_immortals.spell.gold_devour_swarm.success"));
        register(SkillType.TREASURE_APPRAISAL_GLIMPSE, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(8, 140, 0.0D, 24.0D, 0.0D,
                        com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_SCAN,
                        "message.seeking_immortals.spell.treasure_appraisal_glimpse.success"));
        register(SkillType.SUMMON_WOOD_PUPPET, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(12, 220, "basic_wood_puppet", 0, 0, 220, "message.seeking_immortals.spell.summon_wood_puppet.success"));
        register(SkillType.PUPPET_SWARM, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(12, 220, "puppet_swarm", 1, 0, 240, "message.seeking_immortals.spell.puppet_swarm.success"));
        register(SkillType.IRON_PUPPET, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(12, 220, "iron_puppet", 1, 1, 240, "message.seeking_immortals.spell.iron_puppet.success"));
        register(SkillType.PUPPET_CONTROL_BASIC, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(8, 140, "puppet_control_basic", 0, 0, 180, "message.seeking_immortals.spell.puppet_control_basic.success"));
        register(SkillType.PUPPET_SWARM_COMMAND, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(30, 220, "puppet_swarm_command", 1, 1, 260, "message.seeking_immortals.spell.puppet_swarm_command.success"));
        register(SkillType.BEAST_SOUL_PUPPET_BIND, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(22, 140, "beast_soul_puppet_bind", 1, 0, 200, "message.seeking_immortals.spell.beast_soul_puppet_bind.success"));
        register(SkillType.SECOND_NASCENT_SOUL, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(12, 220, "second_nascent_soul", 1, 1, 280, "message.seeking_immortals.spell.second_nascent_soul.success"));
        // Wave36: rebind remaining secret cyclone stubs onto form libraries.
        register(SkillType.SWORD_FORMATION_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(
                57, 260, 55.0D, 20.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.SWORD_DOMAIN,
                "message.seeking_immortals.spell.sword_formation_secret.success"));
        register(SkillType.NETHER_CORE_FORM, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(
                57, 260, 50.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.MYSTIC_SOUL_BONE_CONDENSING_ART,
                "message.seeking_immortals.spell.nether_core_form.success"));
        register(SkillType.NINE_PALACE_SEAL_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(
                74, 260, 48.0D, 18.0D, 4.2D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SEAL_ARRAY,
                "message.seeking_immortals.spell.nine_palace_seal_secret.success"));
        register(SkillType.DAYAN_PUPPET_LEGION, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(74, 260, "dayan_puppet_legion", 2, 1, 300, "message.seeking_immortals.spell.dayan_puppet_legion.success"));
        register(SkillType.STAR_PALACE_HEAVEN_SEAL, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(
                92, 260, 55.0D, 20.0D, 4.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.BAGUA_SEAL,
                "message.seeking_immortals.spell.star_palace_heaven_seal.success"));
        register(SkillType.INVERSE_STAR_REBELLION, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                57, 260, 40.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.INVERSE_STAR_VEIL,
                "message.seeking_immortals.spell.inverse_star_rebellion.success"));
        register(SkillType.VOID_REFINING_DOMAIN, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(
                110, 260, 60.0D, 22.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.KILL_SWORD_FORMATION,
                "message.seeking_immortals.spell.void_refining_domain.success"));
        register(SkillType.GREAT_VEHICLE_DHARMA_BODY, new com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell(
                127, 260, 55.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.BuddhistSpell.BuddhistForm.DAJIN_BUDDHIST_VAJRA,
                "message.seeking_immortals.spell.great_vehicle_dharma_body.success"));
        register(SkillType.SPIRIT_SEVERING_FLASH, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(
                145, 260, 70.0D, 24.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.INVISIBLE_SWORD,
                "message.seeking_immortals.spell.spirit_severing_flash.success"));
        register(SkillType.FALLEN_DEMON_TRANSFORM, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(
                74, 260, 55.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.SKY_SUPPORTING_DEMONIC_SKILL,
                "message.seeking_immortals.spell.fallen_demon_transform.success"));
        register(SkillType.BLOOD_DEMON_AVATAR, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(40, 260, "blood_demon_avatar", 2, 1, 280, "message.seeking_immortals.spell.blood_demon_avatar.success"));
        register(SkillType.HEHUAN_UNION_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                57, 260, 35.0D, 16.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.MIND_CONFUSION,
                "message.seeking_immortals.spell.hehuan_union_secret.success"));
        register(SkillType.TIANMO_BERSERK, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(
                57, 260,
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 120, 1,
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 120, 1,
                net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                "message.seeking_immortals.spell.tianmo_berserk.success",
                "berserk", 120, 0));
        register(SkillType.ILLUSION_WORLD, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                74, 260, 42.0D, 18.0D, 4.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.HUNDRED_ILLUSION,
                "message.seeking_immortals.spell.illusion_world.success"));
        register(SkillType.BEAST_SOUL_FUSION, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(
                57, 260, "beast_summon", 2, 1, 280,
                "message.seeking_immortals.spell.beast_soul_fusion.success"));
        register(SkillType.SPATIAL_TEAR_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                74, 260, 20.0D, 24.0D, 2.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VOID_STEP,
                "message.seeking_immortals.spell.spatial_tear_escape.success"));
        register(SkillType.WAN_SWORD_RETURN, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(
                74, 260, 60.0D, 22.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.THOUSAND_SWORD_ARRAY,
                "message.seeking_immortals.spell.wan_sword_return.success"));
        register(SkillType.VOID_PALACE_HEAVEN_EARTH, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(
                92, 260, 58.0D, 22.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.DEFENSE_FORMATION,
                "message.seeking_immortals.spell.void_palace_heaven_earth.success"));
        register(SkillType.TRUE_IMMORTAL_SWORD_ART, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(
                145, 260, 80.0D, 26.0D, 4.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.QINGYUAN_SWORD_RAY,
                "message.seeking_immortals.spell.true_immortal_sword_art.success"));
        register(SkillType.TIANMO_DEMON_BODY_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(
                18, 260, 45.0D, 16.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.BLOOD_LUO_BARRIER,
                "message.seeking_immortals.spell.tianmo_demon_body_secret.success"));
        register(SkillType.HEHUAN_SOUL_DEVOUR_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(
                18, 260, 45.0D, 16.0D, 3.5D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.MYSTIC_SOUL_GHOST_FIRE,
                "message.seeking_immortals.spell.hehuan_soul_devour_secret.success"));
        register(SkillType.INVERSE_STAR_VOID_ESCAPE, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                18, 260, 20.0D, 22.0D, 2.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.VOID_STEP,
                "message.seeking_immortals.spell.inverse_star_void_escape.success"));
        register(SkillType.PILL_SOUL_CONDENSE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell(
                18, 260, 30.0D, 0.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.RecoverySpell.RecoveryForm.SPIRIT_RECOVERY,
                "message.seeking_immortals.spell.pill_soul_condense_secret.success"));
        register(SkillType.HUADAO_SLASH_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(
                18, 260, 45.0D, 18.0D, 3.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.BLOOD_SWORD_SLASH,
                "message.seeking_immortals.spell.huadao_slash_secret.success"));
        register(SkillType.MULAN_HOLY_BIRD_CALL, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(
                18, 260, "spirit_art_holy_beast_call", 1, 0, 240,
                "message.seeking_immortals.spell.mulan_holy_bird_call.success"));
        register(SkillType.BEAST_SOUL_FUSION_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(
                18, 260, "beast_soul_puppet_bind", 1, 1, 240,
                "message.seeking_immortals.spell.beast_soul_fusion_secret.success"));
        register(SkillType.QINGLUO_TEN_POISON_SEAL, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(
                18, 260, 40.0D, 16.0D, 3.8D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SEAL_ARRAY,
                "message.seeking_immortals.spell.qingluo_ten_poison_seal.success"));
        // Wave35: rebind cyclone-stub secrets onto form-based spell libraries (no vanilla projectile core).
        register(SkillType.WANHU_THOUSAND_PHANTOM_DOMAIN, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                18, 260, 45.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.WANHU_NINE_ILLUSION,
                "message.seeking_immortals.spell.wanhu_thousand_phantom_domain.success"));
        register(SkillType.TIANYUAN_BOUNDARY_BREAK, new com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell(
                18, 260, 45.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.FormationSpell.FormationForm.SEAL_ARRAY,
                "message.seeking_immortals.spell.tianyuan_boundary_break.success"));
        register(SkillType.YINLUO_SOUL_HARVEST, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(
                18, 260, 45.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.YIN_DEMON_SLASH,
                "message.seeking_immortals.spell.yinluo_soul_harvest.success"));
        register(SkillType.INVERSE_STAR_COVERT_ULTIMATE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell(
                18, 260, 45.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.IllusionSpell.IllusionForm.INVERSE_STAR_VEIL,
                "message.seeking_immortals.spell.inverse_star_covert_ultimate_secret.success"));
        register(SkillType.DEMONIC_GUILING_ULTIMATE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(
                18, 260, "guiling_corpse_summon", 2, 1, 280,
                "message.seeking_immortals.spell.demonic_guiling_ultimate_secret.success"));
        register(SkillType.YIN_CLUSTER_GHOST_ULTIMATE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell(
                18, 260, 45.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DemonicGhostSpell.DemonicGhostForm.MYSTIC_SOUL_GHOST_FIRE,
                "message.seeking_immortals.spell.yin_cluster_ghost_ultimate_secret.success"));
        register(SkillType.PUPPET_QIANZHU_ULTIMATE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(18, 260, "puppet_qianzhu_ultimate_secret", 2, 1, 280, "message.seeking_immortals.spell.puppet_qianzhu_ultimate_secret.success"));
        register(SkillType.PUPPET_YULING_ULTIMATE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(18, 260, "puppet_yuling_ultimate_secret", 2, 1, 280, "message.seeking_immortals.spell.puppet_yuling_ultimate_secret.success"));
        register(SkillType.BLADE_GIANT_SWORD_ULTIMATE_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell(
                18, 260, 45.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.SwordTechniqueSpell.SwordForm.THOUSAND_SWORD_ARRAY,
                "message.seeking_immortals.spell.blade_giant_sword_ultimate_secret.success"));
        register(SkillType.ARTIFACT_SPIRIT_AWAKEN_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell(
                35, 260, 45.0D, 18.0D, 4.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DaoSpell.DaoForm.BAGUA_SEAL,
                "message.seeking_immortals.spell.artifact_spirit_awaken_secret.success"));
        register(SkillType.AUCTION_BID_INSIGHT_SECRET, new com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell(
                20, 260, 12.0D, 24.0D, 5.0D,
                com.xunxian.seekingimmortals.skill.effect.spell.DivineSenseSpell.DivineSenseForm.SENSE_SCAN,
                "message.seeking_immortals.spell.auction_bid_insight_secret.success"));
        // CAST_* talismans: mode-aware TalismanConsumeSpell (effectKey drives projectile/aoe/buff/control/movement).
        register(SkillType.CAST_FIRE_BURST_TALISMAN, castTalisman(22.0D, "fire", "aoe_burst_fire",
                "message.seeking_immortals.spell.cast_fire_burst_talisman.success"));
        register(SkillType.CAST_ICE_SEAL_TALISMAN, castTalisman(18.0D, "ice", "seal_control_ice",
                "message.seeking_immortals.spell.cast_ice_seal_talisman.success"));
        register(SkillType.CAST_ESCAPE_HEAVEN_TALISMAN, castTalisman(8.0D, "wind", "escape_teleport",
                "message.seeking_immortals.spell.cast_escape_heaven_talisman.success"));
        register(SkillType.CAST_SPIRIT_FIX_TALISMAN, castTalisman(10.0D, "spirit", "buff_spirit_fix",
                "message.seeking_immortals.spell.cast_spirit_fix_talisman.success"));
        register(SkillType.CAST_THUNDER_TALISMAN, castTalisman(24.0D, "thunder", "aoe_thunder_strike",
                "message.seeking_immortals.spell.cast_thunder_talisman.success"));
        register(SkillType.CAST_SOUL_LOCK_TALISMAN, castTalisman(16.0D, "dark", "lock_seal_control",
                "message.seeking_immortals.spell.cast_soul_lock_talisman.success"));
        register(SkillType.CAST_TELEPORT_ARRAY_TALISMAN, castTalisman(8.0D, "space", "teleport_array_escape",
                "message.seeking_immortals.spell.cast_teleport_array_talisman.success"));
        register(SkillType.CAST_BEAST_CONTRACT_TALISMAN, castTalisman(12.0D, "beast", "contract_buff",
                "message.seeking_immortals.spell.cast_beast_contract_talisman.success"));
        register(SkillType.CAST_ANTI_DEMON_TALISMAN, castTalisman(22.0D, "fire", "aoe_burst_anti_demon",
                "message.seeking_immortals.spell.cast_anti_demon_talisman.success"));
        register(SkillType.CAST_YIN_PROTECT_TALISMAN, castTalisman(10.0D, "yin", "protect_armor_buff",
                "message.seeking_immortals.spell.cast_yin_protect_talisman.success"));
        // Keep SelfBuffSpell so CAST_GHOST_HIDE continues to produce canonical conceal_qi status.
        register(SkillType.CAST_GHOST_HIDE_TALISMAN, new com.xunxian.seekingimmortals.skill.effect.spell.SelfBuffSpell(
                        8, 80,
                        net.minecraft.world.effect.MobEffects.INVISIBILITY, 600, 0,
                        null, 0, 0,
                        net.minecraft.core.particles.ParticleTypes.SOUL,
                        net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                        "message.seeking_immortals.spell.cast_ghost_hide_talisman.success",
                        "conceal_qi", 600, 0));
        register(SkillType.CAST_SPACE_ANCHOR_TALISMAN, castTalisman(14.0D, "space", "anchor_seal_control",
                "message.seeking_immortals.spell.cast_space_anchor_talisman.success"));
        register(SkillType.CAST_LIFE_SAVE_TALISMAN, castTalisman(12.0D, "life", "life_resurrect_buff",
                "message.seeking_immortals.spell.cast_life_save_talisman.success"));
        register(SkillType.CAST_EARTH_WALL_TALISMAN, castTalisman(12.0D, "earth", "wall_protect_buff",
                "message.seeking_immortals.spell.cast_earth_wall_talisman.success"));
        register(SkillType.CAST_MIRAGE_HEART_TALISMAN, castTalisman(10.0D, "illusion", "illusion_mask_buff",
                "message.seeking_immortals.spell.cast_mirage_heart_talisman.success"));
        register(SkillType.CAST_INVISIBILITY_TALISMAN, castTalisman(8.0D, "wind", "invis_hide_escape",
                "message.seeking_immortals.spell.cast_invisibility_talisman.success"));
        register(SkillType.CAST_SPIRIT_GATHER_TALISMAN, castTalisman(10.0D, "spirit", "gather_spirit_buff",
                "message.seeking_immortals.spell.cast_spirit_gather_talisman.success"));
        register(SkillType.CAST_ILLUSION_TALISMAN, castTalisman(10.0D, "illusion", "illusion_mask_buff",
                "message.seeking_immortals.spell.cast_illusion_talisman.success"));
        register(SkillType.CAST_STAR_PALACE_PATROL_TALISMAN, castTalisman(18.0D, "star", "aoe_patrol_burst",
                "message.seeking_immortals.spell.cast_star_palace_patrol_talisman.success"));
        register(SkillType.CAST_GOLDEN_ARMOR_TALISMAN, castTalisman(12.0D, "metal", "armor_protect_buff",
                "message.seeking_immortals.spell.cast_golden_armor_talisman.success"));
        register(SkillType.CAST_INVERSE_STAR_CIPHER_TALISMAN, castTalisman(14.0D, "star", "seal_control_cipher",
                "message.seeking_immortals.spell.cast_inverse_star_cipher_talisman.success"));
        register(SkillType.CAST_BU_TIAN_TALISMAN, castTalisman(14.0D, "life", "life_buff_heal",
                "message.seeking_immortals.spell.cast_bu_tian_talisman.success"));
        register(SkillType.CAST_WOOD_BIND_TALISMAN, castTalisman(16.0D, "wood", "bind_seal_control",
                "message.seeking_immortals.spell.cast_wood_bind_talisman.success"));
        register(SkillType.CAST_METAL_BLADE_TALISMAN, castTalisman(20.0D, "metal", "projectile_blade_metal",
                "message.seeking_immortals.spell.cast_metal_blade_talisman.success"));
        register(SkillType.CAST_VOID_PALACE_KEY_TALISMAN, castTalisman(10.0D, "void", "teleport_escape_void",
                "message.seeking_immortals.spell.cast_void_palace_key_talisman.success"));
        register(SkillType.CAST_TALISMAN_WOODEN_OX, castTalisman(12.0D, "wood", "buff_contract_ox",
                "message.seeking_immortals.spell.cast_talisman_wooden_ox.success"));
        register(SkillType.GHOST_KING_SUMMON, new com.xunxian.seekingimmortals.skill.effect.spell.HonestSummonSpell(12, 220, "ghost_king_summon", 1, 0, 240, "message.seeking_immortals.spell.ghost_king_summon.success"));

                registerTechniqueAlias("fireball", SkillType.FIREBALL);
        registerTechniqueAlias("earth_spike", SkillType.EARTH_SPIKE);
        registerTechniqueAlias("five_elements_escape", SkillType.FIVE_ELEMENTS_ESCAPE);
        registerTechniqueAlias("ice_cone", SkillType.ICE_CONE);
        registerTechniqueAlias("thunder_strike", SkillType.THUNDER_STRIKE);
        registerTechniqueAlias("earth_escape", SkillType.EARTH_ESCAPE);
        registerTechniqueAlias("aura_detection", SkillType.DETECTION);
        registerTechniqueAlias("entangling", SkillType.VINE_BIND);
        registerTechniqueAlias("voice_transmission", SkillType.VOICE_TRANSMISSION);
        registerTechniqueAlias("object_control", SkillType.OBJECT_CONTROL);
        registerTechniqueAlias("quicksand", SkillType.QUICKSAND);
        registerTechniqueAlias("water_shield", SkillType.WATER_SHIELD);
        registerTechniqueAlias("earth_prison", SkillType.EARTH_PRISON);
        registerTechniqueAlias("wind_binding", SkillType.WIND_BINDING);
        registerTechniqueAlias("wind_wall", SkillType.WIND_WALL);
        registerTechniqueAlias("big_dipper_sword_array", SkillType.BIG_DIPPER_SWORD_ARRAY);
        registerTechniqueAlias("light_body", SkillType.LIGHTNESS_SKILL);
        registerTechniqueAlias("lightness_skill", SkillType.LIGHTNESS_SKILL);
        registerTechniqueAlias("soul_search", SkillType.SOUL_SEARCH_SPELL);
        registerTechniqueAlias("demon_subdue", SkillType.DEMON_SUBDUE_SEAL);

        // Batch 1: Core technique mappings (200 entries)
        registerTechniqueAlias("array_puppet", SkillType.ARRAY_PUPPET);
        registerTechniqueAlias("artifact_spirit_awaken_secret", SkillType.ARTIFACT_SPIRIT_AWAKEN_SECRET);
        registerTechniqueAlias("auction_bid_insight_secret", SkillType.AUCTION_BID_INSIGHT_SECRET);
        registerTechniqueAlias("bagua_seal", SkillType.BAGUA_SEAL);
        registerTechniqueAlias("baipulse_full_armor", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("baipulse_sword_bone", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("baipulse_weaponize", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("barbarian_roar", SkillType.BARBARIAN_ROAR);
        registerTechniqueAlias("barbarian_roar_art", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("beast_soul_fusion", SkillType.BEAST_SOUL_FUSION);
        registerTechniqueAlias("beast_soul_fusion_secret", SkillType.BEAST_SOUL_FUSION_SECRET);
        registerTechniqueAlias("beast_soul_puppet_bind", SkillType.BEAST_SOUL_PUPPET_BIND);
        registerTechniqueAlias("beast_summon", SkillType.BEAST_SUMMON);
        registerTechniqueAlias("beast_tame_bond", SkillType.FIREBALL);
        registerTechniqueAlias("beast_tame_call", SkillType.BEAST_TAME_CALL);
        registerTechniqueAlias("blade_giant_sword_ultimate_secret", SkillType.BLADE_GIANT_SWORD_ULTIMATE_SECRET);
        registerTechniqueAlias("blizzard", SkillType.BLIZZARD);
        registerTechniqueAlias("blood_corruption", SkillType.BLOOD_CORRUPTION);
        registerTechniqueAlias("blood_curse_mark", SkillType.BLOOD_CURSE_MARK);
        registerTechniqueAlias("blood_curse_strike", SkillType.BLOOD_CURSE_STRIKE);
        registerTechniqueAlias("blood_demon_avatar", SkillType.BLOOD_DEMON_AVATAR);
        registerTechniqueAlias("blood_demon_slash", SkillType.BLOOD_DEMON_SLASH);
        registerTechniqueAlias("blood_escape", SkillType.BLOOD_ESCAPE);
        registerTechniqueAlias("blood_refine_light", SkillType.DEMON_FORM);
        registerTechniqueAlias("blood_sacrifice", SkillType.BLOOD_SACRIFICE);
        registerTechniqueAlias("blood_shadow_escape", SkillType.BLOOD_SHADOW_ESCAPE);
        registerTechniqueAlias("blood_sword", SkillType.BLOOD_SWORD);
        registerTechniqueAlias("blood_sword_slash", SkillType.BLOOD_SWORD_SLASH);
        registerTechniqueAlias("bloodline_awaken", SkillType.BLOODLINE_AWAKEN);
        registerTechniqueAlias("body_flash", SkillType.BODY_FLASH);
        registerTechniqueAlias("body_hardness", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("body_repair", SkillType.BODY_REPAIR);
        registerTechniqueAlias("bone_armor", SkillType.BONE_ARMOR);
        registerTechniqueAlias("bone_crush", SkillType.BONE_CRUSH);
        registerTechniqueAlias("border_guard_stance", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("buddha_light", SkillType.BUDDHA_LIGHT);
        registerTechniqueAlias("buddhist_vajra_body_art_auto_3", SkillType.VAJRA_PALM);
        registerTechniqueAlias("cast_anti_demon_talisman", SkillType.CAST_ANTI_DEMON_TALISMAN);
        registerTechniqueAlias("cast_beast_contract_talisman", SkillType.CAST_BEAST_CONTRACT_TALISMAN);
        registerTechniqueAlias("cast_bu_tian_talisman", SkillType.CAST_BU_TIAN_TALISMAN);
        registerTechniqueAlias("cast_earth_wall_talisman", SkillType.CAST_EARTH_WALL_TALISMAN);
        registerTechniqueAlias("cast_escape_heaven_talisman", SkillType.CAST_ESCAPE_HEAVEN_TALISMAN);
        registerTechniqueAlias("cast_fire_burst_talisman", SkillType.CAST_FIRE_BURST_TALISMAN);
        registerTechniqueAlias("cast_ghost_hide_talisman", SkillType.CAST_GHOST_HIDE_TALISMAN);
        registerTechniqueAlias("cast_golden_armor_talisman", SkillType.CAST_GOLDEN_ARMOR_TALISMAN);
        registerTechniqueAlias("cast_ice_seal_talisman", SkillType.CAST_ICE_SEAL_TALISMAN);
        registerTechniqueAlias("cast_illusion_talisman", SkillType.CAST_ILLUSION_TALISMAN);
        registerTechniqueAlias("cast_inverse_star_cipher_talisman", SkillType.CAST_INVERSE_STAR_CIPHER_TALISMAN);
        registerTechniqueAlias("cast_invisibility_talisman", SkillType.CAST_INVISIBILITY_TALISMAN);
        registerTechniqueAlias("cast_life_save_talisman", SkillType.CAST_LIFE_SAVE_TALISMAN);
        registerTechniqueAlias("cast_metal_blade_talisman", SkillType.CAST_METAL_BLADE_TALISMAN);
        registerTechniqueAlias("cast_mirage_heart_talisman", SkillType.CAST_MIRAGE_HEART_TALISMAN);
        registerTechniqueAlias("cast_soul_lock_talisman", SkillType.CAST_SOUL_LOCK_TALISMAN);
        registerTechniqueAlias("cast_space_anchor_talisman", SkillType.CAST_SPACE_ANCHOR_TALISMAN);
        registerTechniqueAlias("cast_spirit_fix_talisman", SkillType.CAST_SPIRIT_FIX_TALISMAN);
        registerTechniqueAlias("cast_spirit_gather_talisman", SkillType.CAST_SPIRIT_GATHER_TALISMAN);
        registerTechniqueAlias("cast_star_palace_patrol_talisman", SkillType.CAST_STAR_PALACE_PATROL_TALISMAN);
        registerTechniqueAlias("cast_talisman_wooden_ox", SkillType.CAST_TALISMAN_WOODEN_OX);
        registerTechniqueAlias("cast_teleport_array_talisman", SkillType.CAST_TELEPORT_ARRAY_TALISMAN);
        registerTechniqueAlias("cast_thunder_talisman", SkillType.CAST_THUNDER_TALISMAN);
        registerTechniqueAlias("cast_void_palace_key_talisman", SkillType.CAST_VOID_PALACE_KEY_TALISMAN);
        registerTechniqueAlias("cast_wood_bind_talisman", SkillType.CAST_WOOD_BIND_TALISMAN);
        registerTechniqueAlias("cast_yin_protect_talisman", SkillType.CAST_YIN_PROTECT_TALISMAN);
        registerTechniqueAlias("chain_lightning", SkillType.CHAIN_LIGHTNING);
        registerTechniqueAlias("clone_image", SkillType.CLONE_IMAGE);
        registerTechniqueAlias("cloud_walk", SkillType.CLOUD_WALK);
        registerTechniqueAlias("confucian_righteous_qi", SkillType.CONFUCIAN_RIGHTEOUS_QI);
        registerTechniqueAlias("corpse_armor", SkillType.CORPSE_ARMOR);
        registerTechniqueAlias("corpse_explode", SkillType.CORPSE_EXPLODE);
        registerTechniqueAlias("corpse_explosion", SkillType.CORPSE_EXPLOSION);
        registerTechniqueAlias("cyclone", SkillType.CYCLONE);
        registerTechniqueAlias("dajin_buddhist_vajra", SkillType.DAJIN_BUDDHIST_VAJRA);
        registerTechniqueAlias("dajin_clan_heritage_shield", SkillType.AURA_BODY_SHIELD);
        registerTechniqueAlias("dao_ancestor_glimpse_domain", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("dao_ancestor_glimpse_xian_3", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("dao_ancestor_silence", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("dao_nature_breath", SkillType.DAO_NATURE_BREATH);
        registerTechniqueAlias("dark_flame", SkillType.DARK_FLAME);
        registerTechniqueAlias("dayan_eye", SkillType.DAYAN_EYE);
        registerTechniqueAlias("dayan_puppet_legion", SkillType.DAYAN_PUPPET_LEGION);
        registerTechniqueAlias("dayan_sense_boost", SkillType.SENSE_SCAN);
        registerTechniqueAlias("defensive_pearl_light", SkillType.DEFENSIVE_PEARL_LIGHT);
        registerTechniqueAlias("deity_transformation_domain", SkillType.DEITY_TRANSFORMATION_DOMAIN);
        registerTechniqueAlias("demon_armor", SkillType.DEMON_ARMOR);
        registerTechniqueAlias("demon_claw", SkillType.DEMON_CLAW);
        registerTechniqueAlias("demon_contract", SkillType.DEMON_CONTRACT);
        registerTechniqueAlias("demon_flame", SkillType.DEMON_FLAME);
        registerTechniqueAlias("demon_form", SkillType.DEMON_FORM);
        registerTechniqueAlias("demon_qi_burst", SkillType.DEMON_QI_BURST);
        registerTechniqueAlias("demon_roar", SkillType.DEMON_ROAR);
        registerTechniqueAlias("demon_soul_devour", SkillType.DEMON_SOUL_DEVOUR);
        registerTechniqueAlias("demon_subdue_palm", SkillType.DEMON_SUBDUE_PALM);
        registerTechniqueAlias("demonic_guiling_ultimate_secret", SkillType.DEMONIC_GUILING_ULTIMATE_SECRET);
        registerTechniqueAlias("detoxify", SkillType.DETOXIFY);
        registerTechniqueAlias("divine_sense_expansion", SkillType.DIVINE_SENSE_EXPANSION);
        registerTechniqueAlias("divine_sense_lock", SkillType.DIVINE_SENSE_LOCK);
        registerTechniqueAlias("divine_sense_scan", SkillType.DIVINE_SENSE_SCAN);
        registerTechniqueAlias("domain_compress", SkillType.DOMAIN_COMPRESS);
        registerTechniqueAlias("dragon_strength", SkillType.DRAGON_STRENGTH);
        registerTechniqueAlias("dream_snare", SkillType.DREAM_SNARE);
        registerTechniqueAlias("dual_sword_dance", SkillType.DUAL_SWORD_DANCE);
        registerTechniqueAlias("earth_burrow", SkillType.EARTH_BURROW);
        registerTechniqueAlias("earth_mountain_press", SkillType.EARTH_MOUNTAIN_PRESS);
        registerTechniqueAlias("earth_prison", SkillType.EARTH_PRISON);
        registerTechniqueAlias("earth_wall", SkillType.EARTH_WALL);
        registerTechniqueAlias("elemental_burst_earth", SkillType.ELEMENTAL_BURST_EARTH);
        registerTechniqueAlias("elemental_burst_fire", SkillType.ELEMENTAL_BURST_FIRE);
        registerTechniqueAlias("elemental_burst_ice", SkillType.ELEMENTAL_BURST_ICE);
        registerTechniqueAlias("elemental_burst_metal", SkillType.ELEMENTAL_BURST_METAL);
        registerTechniqueAlias("elemental_burst_thunder", SkillType.ELEMENTAL_BURST_THUNDER);
        registerTechniqueAlias("elemental_burst_water", SkillType.ELEMENTAL_BURST_WATER);
        registerTechniqueAlias("elemental_burst_wind", SkillType.ELEMENTAL_BURST_WIND);
        registerTechniqueAlias("elemental_burst_wood", SkillType.ELEMENTAL_BURST_WOOD);
        registerTechniqueAlias("evil_ward_thunder", SkillType.EVIL_WARD_THUNDER);
        registerTechniqueAlias("explosion_talisman", SkillType.EXPLOSION_TALISMAN);
        registerTechniqueAlias("fallen_demon_transform", SkillType.FALLEN_DEMON_TRANSFORM);
        registerTechniqueAlias("fansheng_nirvana_flash", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("fire_bullet", SkillType.FIRE_BULLET);
        registerTechniqueAlias("fire_escape", SkillType.FIRE_ESCAPE);
        registerTechniqueAlias("fire_rain", SkillType.FIRE_RAIN);
        registerTechniqueAlias("fire_talisman", SkillType.FIRE_TALISMAN);
        registerTechniqueAlias("five_element_fusion_burst", SkillType.FIVE_ELEMENT_FUSION_BURST);
        registerTechniqueAlias("five_elements_palm", SkillType.FIVE_ELEMENTS_PALM);
        registerTechniqueAlias("five_thunder", SkillType.FIVE_THUNDER);
        registerTechniqueAlias("flame_burst", SkillType.FLAME_BURST);
        registerTechniqueAlias("flame_ring", SkillType.FLAME_RING);
        registerTechniqueAlias("flame_serpent_storm", SkillType.FLAME_SERPENT_STORM);
        registerTechniqueAlias("flying_sword_advanced", SkillType.FLYING_SWORD_ADVANCED);
        registerTechniqueAlias("flying_sword_beginner", SkillType.FLYING_SWORD_BEGINNER);
        registerTechniqueAlias("flying_sword_strike", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("formation_sense", SkillType.FORMATION_SENSE);
        registerTechniqueAlias("formation_trap_basic", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("fox_phantom_shift", SkillType.FOX_PHANTOM_SHIFT);
        registerTechniqueAlias("frost_armor", SkillType.FROST_ARMOR);
        registerTechniqueAlias("frost_breath", SkillType.FROST_BREATH);
        registerTechniqueAlias("ghost_bind", SkillType.GHOST_BIND);
        registerTechniqueAlias("ghost_king_avatar", SkillType.GHOST_KING_AVATAR);
        registerTechniqueAlias("ghost_king_summon", SkillType.GHOST_KING_SUMMON);
        registerTechniqueAlias("ghost_talisman", SkillType.GHOST_TALISMAN);
        registerTechniqueAlias("ghost_walk", SkillType.GHOST_WALK);
        registerTechniqueAlias("giant_sword_cleave", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("giant_sword_guard", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("giant_sword_mountain", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("giant_sword_smash", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("giant_sword_throw", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("gold_beam", SkillType.GOLD_BEAM);
        registerTechniqueAlias("gold_devour_swarm", SkillType.GOLD_DEVOUR_SWARM);
        registerTechniqueAlias("gold_needle", SkillType.GOLD_NEEDLE);
        registerTechniqueAlias("golden_armor_talisman_cast", SkillType.GOLDEN_ARMOR_TALISMAN_CAST);
        registerTechniqueAlias("golden_armor_talisman_forge_cast", SkillType.AURA_BODY_SHIELD);
        registerTechniqueAlias("gray_soul_drain", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("great_vehicle_dao_light", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("great_vehicle_dharma_body", SkillType.GREAT_VEHICLE_DHARMA_BODY);
        registerTechniqueAlias("great_vehicle_palm", SkillType.GREAT_VEHICLE_PALM);
        registerTechniqueAlias("green_bamboo_sword_qi", SkillType.GREEN_BAMBOO_SWORD_QI);
        registerTechniqueAlias("green_bamboo_sword_ray", SkillType.GREEN_BAMBOO_SWORD_RAY);
        registerTechniqueAlias("green_shield", SkillType.GREEN_SHIELD);
        registerTechniqueAlias("ground_spike", SkillType.GROUND_SPIKE);
        registerTechniqueAlias("group_heal", SkillType.GROUP_HEAL);
        registerTechniqueAlias("guiling_corpse_burst_chain", SkillType.YIN_SOUL_CHAIN);
        registerTechniqueAlias("guiling_corpse_drive", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("guiling_corpse_summon", SkillType.GUILING_CORPSE_SUMMON);
        registerTechniqueAlias("guiling_soul_banner_wave", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("guiling_soul_hook", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("guiling_yin_fire", SkillType.UNDERWORLD_FLAME);
        registerTechniqueAlias("heal_qi", SkillType.HEAL_QI);
        registerTechniqueAlias("heaven_demon_hand", SkillType.HEAVEN_DEMON_HAND);
        registerTechniqueAlias("heaven_punish", SkillType.HEAVEN_PUNISH);
        registerTechniqueAlias("hehuan_charm", SkillType.HEHUAN_CHARM);
        registerTechniqueAlias("hehuan_soul_charm", SkillType.HEHUAN_SOUL_CHARM);
        registerTechniqueAlias("hehuan_soul_devour_secret", SkillType.HEHUAN_SOUL_DEVOUR_SECRET);
        registerTechniqueAlias("hehuan_union_secret", SkillType.HEHUAN_UNION_SECRET);
        registerTechniqueAlias("huadao_draw", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("huadao_intent_slash", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("huadao_slash", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("huadao_slash_finisher", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("huadao_slash_secret", SkillType.HUADAO_SLASH_SECRET);
        registerTechniqueAlias("huadao_wind_blade", SkillType.WIND_BLADE);
        registerTechniqueAlias("huangfeng_fire_serpent", SkillType.HUANGFENG_FIRE_SERPENT);
        registerTechniqueAlias("hundred_illusion", SkillType.HUNDRED_ILLUSION);
        registerTechniqueAlias("ice_cone", SkillType.ICE_CONE);
        registerTechniqueAlias("ice_freezing", SkillType.ICE_FREEZING);
        registerTechniqueAlias("ice_jade_shield", SkillType.ICE_JADE_SHIELD);
        registerTechniqueAlias("ice_prison", SkillType.ICE_PRISON);
        registerTechniqueAlias("ice_shard", SkillType.ICE_SHARD);
        registerTechniqueAlias("ice_spear", SkillType.ICE_SPEAR);
        registerTechniqueAlias("ice_talisman", SkillType.ICE_TALISMAN);
        registerTechniqueAlias("illusion_formation", SkillType.ILLUSION_FORMATION);
        registerTechniqueAlias("illusion_mist", SkillType.ILLUSION_MIST);
        registerTechniqueAlias("illusion_world", SkillType.ILLUSION_WORLD);
        registerTechniqueAlias("immortal_body_fist", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("immortal_body_harden", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("immortal_golden_avatar", SkillType.SPIRIT_ART_BEAST_CALL);
        registerTechniqueAlias("immortal_rope", SkillType.IMMORTAL_ROPE);
        registerTechniqueAlias("immortal_sword_domain", SkillType.SWORD_DOMAIN);
        registerTechniqueAlias("immortal_sword_law_cut", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("immortal_sword_ray", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("ink_sea", SkillType.INK_SEA);
        registerTechniqueAlias("inverse_shadow_step", SkillType.SHADOW_FLASH);
        registerTechniqueAlias("inverse_star_black_market_burst", SkillType.FIREBALL);
        registerTechniqueAlias("inverse_star_chaos_brand", SkillType.DEMON_FORM);
        registerTechniqueAlias("inverse_star_covert_ultimate_secret", SkillType.INVERSE_STAR_COVERT_ULTIMATE_SECRET);
        registerTechniqueAlias("inverse_star_double_feint", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("inverse_star_rebellion", SkillType.INVERSE_STAR_REBELLION);
        registerTechniqueAlias("inverse_star_shadow_art_auto_3", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("inverse_star_shadow_step", SkillType.INVERSE_STAR_SHADOW_STEP);
        registerTechniqueAlias("inverse_star_smuggle_route", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("inverse_star_veil", SkillType.INVERSE_STAR_VEIL);
        registerTechniqueAlias("inverse_star_veil_trace", SkillType.FIREBALL);
        registerTechniqueAlias("inverse_star_void_escape", SkillType.INVERSE_STAR_VOID_ESCAPE);
        registerTechniqueAlias("invisibility_basic", SkillType.INVISIBILITY_BASIC);
        registerTechniqueAlias("invisible_sword", SkillType.INVISIBLE_SWORD);
        registerTechniqueAlias("iron_puppet", SkillType.IRON_PUPPET);
        registerTechniqueAlias("iron_skin", SkillType.IRON_SKIN);

        // Batch 2: Extended technique mappings (200 entries)
        registerTechniqueAlias("jifeng_burst", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("jifeng_jiubian_auto_2", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("jifeng_jiubian_auto_3", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("jingshen_spike", SkillType.SENSE_SCAN);
        registerTechniqueAlias("jingzhe_partial_change", SkillType.FIREBALL);
        registerTechniqueAlias("jingzhe_second_blood", SkillType.FIREBALL);
        registerTechniqueAlias("jingzhe_wing_storm", SkillType.FIREBALL);
        registerTechniqueAlias("jinyi_blade_feather", SkillType.FIREBALL);
        registerTechniqueAlias("kill_sword_formation", SkillType.KILL_SWORD_FORMATION);
        registerTechniqueAlias("kunwu_absolute_zero_guard", SkillType.FIREBALL);
        registerTechniqueAlias("kunwu_frost_armor", SkillType.FROST_ARMOR);
        registerTechniqueAlias("kunwu_frost_spike", SkillType.FIREBALL);
        registerTechniqueAlias("kunwu_seal_reinforce", SkillType.SEAL_ARRAY);
        registerTechniqueAlias("kunwu_seal_strike", SkillType.KUNWU_SEAL_STRIKE);
        registerTechniqueAlias("kunwu_tower_bind", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("lava_burst", SkillType.LAVA_BURST);
        registerTechniqueAlias("lian_shen_focus", SkillType.SENSE_SCAN);
        registerTechniqueAlias("lian_shen_layer2", SkillType.SENSE_SCAN);
        registerTechniqueAlias("lian_shen_spike", SkillType.SENSE_SCAN);
        registerTechniqueAlias("lianxi_shu", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("lianxi_shu_method_auto_2", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("lianxi_shu_method_auto_3", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("lieyan_true_fire_secret", SkillType.LIEYAN_TRUE_FIRE_SECRET);
        registerTechniqueAlias("life_fire", SkillType.LIFE_FIRE);
        registerTechniqueAlias("light_orb", SkillType.LIGHT_ORB);
        registerTechniqueAlias("lightning_chain", SkillType.LIGHTNING_CHAIN);
        registerTechniqueAlias("lingshou_call_pet", SkillType.SPIRIT_ART_BEAST_CALL);
        registerTechniqueAlias("lingshou_frenzy_command", SkillType.FIREBALL);
        registerTechniqueAlias("lingshou_pack_hunt", SkillType.FIREBALL);
        registerTechniqueAlias("lingshou_shared_sense", SkillType.SENSE_SCAN);
        registerTechniqueAlias("lingwu_iron_skin", SkillType.IRON_SKIN);
        registerTechniqueAlias("lingyun_cloud_flash", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("lingyun_storm_array", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("lingyun_swift_thrust", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("lingyun_void_step", SkillType.SHADOW_FLASH);
        registerTechniqueAlias("lingyun_wind_slash", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("lingzu_pearl_barrage", SkillType.LINGZU_PEARL_BARRAGE);
        registerTechniqueAlias("luoyan_bu", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("luoyun_cloud_blade", SkillType.FIREBALL);
        registerTechniqueAlias("luoyun_cloud_mist_escape", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("luoyun_cloud_sword_assist", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("luoyun_elder_seal", SkillType.SEAL_ARRAY);
        registerTechniqueAlias("luoyun_grand_array_node", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("luoyun_message_talisman_net", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("luoyun_pill_emergency", SkillType.HEAL_QI);
        registerTechniqueAlias("luoyun_refine_flame_steady", SkillType.FIREBALL);
        registerTechniqueAlias("luoyun_sect_guard_array", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("luoyun_spirit_flame", SkillType.LUOYUN_SPIRIT_FLAME);
        registerTechniqueAlias("luoyun_spirit_flame_combat", SkillType.FIREBALL);
        registerTechniqueAlias("magnet_control", SkillType.MAGNET_CONTROL);
        registerTechniqueAlias("medicine_king_detox", SkillType.HEAL_QI);
        registerTechniqueAlias("medicine_king_pill_boost", SkillType.HEAL_QI);
        registerTechniqueAlias("metal_needle", SkillType.METAL_NEEDLE);
        registerTechniqueAlias("metal_sword_finger", SkillType.METAL_SWORD_FINGER);
        registerTechniqueAlias("miaoyin_charm_song", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("miaoyin_finale_kill", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("miaoyin_note_pierce", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("miaoyin_resonance_shield", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("miaoyin_sense_echo", SkillType.SENSE_SCAN);
        registerTechniqueAlias("miaoyin_zither_domain", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("miaoyin_zither_secret_auto_3", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("mid_grade_talisman_burst", SkillType.FIREBALL);
        registerTechniqueAlias("mind_confusion", SkillType.MIND_CONFUSION);
        registerTechniqueAlias("mind_read", SkillType.MIND_READ);
        registerTechniqueAlias("mingwang_body_harden", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("mingwang_layer2", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("mingwang_layer3", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("mingwang_vajra_strike", SkillType.VAJRA_PALM);
        registerTechniqueAlias("mirror_phantom", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("mirror_reflect", SkillType.MIRROR_REFLECT);
        registerTechniqueAlias("mist_rain", SkillType.MIST_RAIN);
        registerTechniqueAlias("molten_splash", SkillType.MOLTEN_SPLASH);
        registerTechniqueAlias("mulan_earth_wall", SkillType.MULAN_EARTH_WALL);
        registerTechniqueAlias("mulan_holy_bird_call", SkillType.MULAN_HOLY_BIRD_CALL);
        registerTechniqueAlias("mulan_wind_blade", SkillType.MULAN_WIND_BLADE);
        registerTechniqueAlias("muyuan_wood_spike", SkillType.FIREBALL);
        registerTechniqueAlias("nascent_soul_avatar", SkillType.NASCENT_SOUL_AVATAR);
        registerTechniqueAlias("nascent_soul_escape", SkillType.NASCENT_SOUL_ESCAPE);
        registerTechniqueAlias("nav_tide_step", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("nether_core_form", SkillType.NETHER_CORE_FORM);
        registerTechniqueAlias("nether_ghost_walk", SkillType.NETHER_GHOST_WALK);
        registerTechniqueAlias("nine_palace_seal_secret", SkillType.NINE_PALACE_SEAL_SECRET);
        registerTechniqueAlias("nongyan_flame_burst", SkillType.FIREBALL);
        registerTechniqueAlias("palm_thunder", SkillType.PALM_THUNDER);
        registerTechniqueAlias("palm_wind", SkillType.PALM_WIND);
        registerTechniqueAlias("phantom_clone", SkillType.PHANTOM_CLONE);
        registerTechniqueAlias("pill_soul_condense_secret", SkillType.PILL_SOUL_CONDENSE_SECRET);
        registerTechniqueAlias("pixie_thunder_sword", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("poison_mist", SkillType.POISON_MIST);
        registerTechniqueAlias("poluo_soul_pull", SkillType.POLUO_SOUL_PULL);
        registerTechniqueAlias("primordial_magnet_sphere", SkillType.PRIMORDIAL_MAGNET_SPHERE);
        registerTechniqueAlias("puppet_array_trap", SkillType.PUPPET_ARRAY_TRAP);
        registerTechniqueAlias("puppet_control_basic", SkillType.PUPPET_CONTROL_BASIC);
        registerTechniqueAlias("puppet_qianzhu_ultimate_secret", SkillType.PUPPET_QIANZHU_ULTIMATE_SECRET);
        registerTechniqueAlias("puppet_self_destruct", SkillType.PUPPET_SELF_DESTRUCT);
        registerTechniqueAlias("puppet_summon_basic", SkillType.PUPPET_SUMMON_BASIC);
        registerTechniqueAlias("puppet_swarm", SkillType.PUPPET_SWARM);
        registerTechniqueAlias("puppet_swarm_command", SkillType.PUPPET_SWARM_COMMAND);
        registerTechniqueAlias("puppet_yuling_ultimate_secret", SkillType.PUPPET_YULING_ULTIMATE_SECRET);
        registerTechniqueAlias("pure_yang_sword", SkillType.PURE_YANG_SWORD);
        registerTechniqueAlias("qi_burst_palm", SkillType.QI_BURST_PALM);
        registerTechniqueAlias("qianzhu_puppet_art_auto_3", SkillType.PUPPET_SUMMON_BASIC);
        registerTechniqueAlias("qianzhu_puppet_assemble", SkillType.PUPPET_SUMMON_BASIC);
        registerTechniqueAlias("qingluo_heart_toxin", SkillType.DEMON_FORM);
        registerTechniqueAlias("qingluo_insect_dart", SkillType.DEMON_FORM);
        registerTechniqueAlias("qingluo_paralytic_spore", SkillType.DEMON_FORM);
        registerTechniqueAlias("qingluo_poison_needle", SkillType.QINGLUO_POISON_NEEDLE);
        registerTechniqueAlias("qingluo_ten_poison_seal", SkillType.QINGLUO_TEN_POISON_SEAL);
        registerTechniqueAlias("qingluo_toxin_mist", SkillType.DEMON_FORM);
        registerTechniqueAlias("qingluo_worm_puppet", SkillType.DEMON_FORM);
        registerTechniqueAlias("qingxu_clear_mind", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("qingxu_cloud_sword_array", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingxu_seal_noise", SkillType.BAGUA_SEAL);
        registerTechniqueAlias("qingxu_sword_clear", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("qingxu_wind_step", SkillType.WIND_RIDE);
        registerTechniqueAlias("qingyan_array_stone", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("qingyan_border_bastion", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("qingyan_earth_spike_wall", SkillType.FIREBALL);
        registerTechniqueAlias("qingyan_quake", SkillType.FIREBALL);
        registerTechniqueAlias("qingyan_stone_skin", SkillType.FIREBALL);
        registerTechniqueAlias("qingyuan_bamboo_cloud_drive", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingyuan_layer5_intent", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingyuan_layer9_split_mastery", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingyuan_sword_light_split", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingyuan_sword_pill_intent", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingyuan_sword_ray", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingyuan_sword_silk", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingyuan_to_immortal_bridge", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("qingzhu_water_orb", SkillType.FIREBALL);
        registerTechniqueAlias("reincarnation_judgment", SkillType.SENSE_SCAN);
        registerTechniqueAlias("reincarnation_mark", SkillType.SENSE_SCAN);
        registerTechniqueAlias("reincarnation_soul_pull", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("reincarnation_trade_seal", SkillType.FIREBALL);
        registerTechniqueAlias("revive_weak", SkillType.REVIVE_WEAK);
        registerTechniqueAlias("righteous_qi", SkillType.RIGHTEOUS_QI);
        registerTechniqueAlias("river_escape", SkillType.RIVER_ESCAPE);
        registerTechniqueAlias("sand_bury", SkillType.SAND_BURY);
        registerTechniqueAlias("sand_storm", SkillType.SAND_STORM);
        registerTechniqueAlias("sanzhuan_rebuild", SkillType.HEAL_QI);
        registerTechniqueAlias("sarira_shield", SkillType.SARIRA_SHIELD);
        registerTechniqueAlias("scroll_strike", SkillType.SCROLL_STRIKE);
        registerTechniqueAlias("sea_lock_array", SkillType.SEA_LOCK_ARRAY);
        registerTechniqueAlias("seal_array", SkillType.SEAL_ARRAY);
        registerTechniqueAlias("seal_talisman", SkillType.SEAL_TALISMAN);
        registerTechniqueAlias("second_nascent_soul", SkillType.SECOND_NASCENT_SOUL);
        registerTechniqueAlias("sect_body_intro", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("sect_demonic_intro", SkillType.DEMON_FORM);
        registerTechniqueAlias("sect_formation_intro", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("sect_ghost_intro", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("sect_puppet_intro", SkillType.PUPPET_SUMMON_BASIC);

        // Batch 3: Final technique mappings (200 entries)
        registerTechniqueAlias("sect_sword_intro_slash", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("sect_talisman_intro", SkillType.FIREBALL);
        registerTechniqueAlias("sense_domain", SkillType.SENSE_DOMAIN);
        registerTechniqueAlias("sense_lock", SkillType.SENSE_LOCK);
        registerTechniqueAlias("sense_needle", SkillType.SENSE_NEEDLE);
        registerTechniqueAlias("sense_pressure", SkillType.SENSE_PRESSURE);
        registerTechniqueAlias("sense_scan", SkillType.SENSE_SCAN);
        registerTechniqueAlias("shadow_flash", SkillType.SHADOW_FLASH);
        registerTechniqueAlias("shishen_spike", SkillType.SENSE_SCAN);
        registerTechniqueAlias("short_teleport", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("sky_hook", SkillType.SKY_HOOK);
        registerTechniqueAlias("small_sword_array", SkillType.SMALL_SWORD_ARRAY);
        registerTechniqueAlias("smuggle_rift_step", SkillType.SMUGGLE_RIFT_STEP);
        registerTechniqueAlias("soul_anchor_rite", SkillType.SOUL_ANCHOR_RITE);
        registerTechniqueAlias("soul_attack_wave", SkillType.SOUL_ATTACK_WAVE);
        registerTechniqueAlias("soul_banner", SkillType.SOUL_BANNER);
        registerTechniqueAlias("soul_banner_wave", SkillType.SOUL_BANNER_WAVE);
        registerTechniqueAlias("soul_chop", SkillType.SOUL_CHOP);
        registerTechniqueAlias("soul_contract", SkillType.SOUL_CONTRACT);
        registerTechniqueAlias("soul_cry_shock", SkillType.SOUL_CRY_SHOCK);
        registerTechniqueAlias("soul_devour", SkillType.SOUL_DEVOUR);
        registerTechniqueAlias("soul_devouring_cloud", SkillType.SOUL_DEVOURING_CLOUD);
        registerTechniqueAlias("space_tear_slash", SkillType.SPACE_TEAR_SLASH);
        registerTechniqueAlias("spatial_tear_escape", SkillType.SPATIAL_TEAR_ESCAPE);
        registerTechniqueAlias("spirit_absorb", SkillType.SPIRIT_ABSORB);
        registerTechniqueAlias("spirit_art_array_anchor", SkillType.SPIRIT_ART_ARRAY_ANCHOR);
        registerTechniqueAlias("spirit_art_beast_call", SkillType.SPIRIT_ART_BEAST_CALL);
        registerTechniqueAlias("spirit_art_earth_wall", SkillType.SPIRIT_ART_EARTH_WALL);
        registerTechniqueAlias("spirit_art_heal", SkillType.SPIRIT_ART_HEAL);
        registerTechniqueAlias("spirit_art_holy_beast_call", SkillType.SPIRIT_ART_HOLY_BEAST_CALL);
        registerTechniqueAlias("spirit_art_holy_feather_guard", SkillType.SPIRIT_ART_HOLY_FEATHER_GUARD);
        registerTechniqueAlias("spirit_art_holy_light", SkillType.SPIRIT_ART_HOLY_LIGHT);
        registerTechniqueAlias("spirit_art_lightning_palm", SkillType.SPIRIT_ART_LIGHTNING_PALM);
        registerTechniqueAlias("spirit_art_sand_storm", SkillType.SPIRIT_ART_SAND_STORM);
        registerTechniqueAlias("spirit_art_thunder", SkillType.SPIRIT_ART_THUNDER);
        registerTechniqueAlias("spirit_art_wind_blade", SkillType.SPIRIT_ART_WIND_BLADE);
        registerTechniqueAlias("spirit_art_wind_ride", SkillType.SPIRIT_ART_WIND_RIDE);
        registerTechniqueAlias("spirit_beast_contract", SkillType.SPIRIT_BEAST_CONTRACT);
        registerTechniqueAlias("spirit_chain_talisman_cast", SkillType.SPIRIT_CHAIN_TALISMAN_CAST);
        registerTechniqueAlias("spirit_fengyuan_wind_wall", SkillType.SPIRIT_FENGYUAN_WIND_WALL);
        registerTechniqueAlias("spirit_gather_array", SkillType.SPIRIT_GATHER_ARRAY);
        registerTechniqueAlias("spirit_needle", SkillType.SPIRIT_NEEDLE);
        registerTechniqueAlias("spirit_recovery", SkillType.SPIRIT_RECOVERY);
        registerTechniqueAlias("spirit_seal", SkillType.SPIRIT_SEAL);
        registerTechniqueAlias("spirit_sense_merge", SkillType.SPIRIT_SENSE_MERGE);
        registerTechniqueAlias("spirit_sever_generic_probe", SkillType.SENSE_SCAN);
        registerTechniqueAlias("spirit_severing_flash", SkillType.SPIRIT_SEVERING_FLASH);
        registerTechniqueAlias("spirit_shield", SkillType.SPIRIT_SHIELD);
        registerTechniqueAlias("spirit_shield_talisman", SkillType.SPIRIT_SHIELD_TALISMAN);
        registerTechniqueAlias("spirit_thread_control", SkillType.SPIRIT_THREAD_CONTROL);
        registerTechniqueAlias("spirit_wall", SkillType.SPIRIT_WALL);
        registerTechniqueAlias("spring_wood_heal_minor", SkillType.HEAL_QI);
        registerTechniqueAlias("star_fall", SkillType.STAR_FALL);
        registerTechniqueAlias("star_palace_fleet_signal", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("star_palace_heaven_seal", SkillType.STAR_PALACE_HEAVEN_SEAL);
        registerTechniqueAlias("star_palace_law_bind", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("star_palace_patrol_beacon", SkillType.STAR_PALACE_PATROL_BEACON);
        registerTechniqueAlias("star_palace_register_seal", SkillType.SEAL_ARRAY);
        registerTechniqueAlias("star_palace_seal", SkillType.STAR_PALACE_SEAL);
        registerTechniqueAlias("star_palace_seal_burst", SkillType.STAR_PALACE_SEAL_BURST);
        registerTechniqueAlias("star_palace_star_ray", SkillType.QINGYUAN_SWORD_RAY);
        registerTechniqueAlias("star_palace_tidal_lock", SkillType.STAR_PALACE_TIDAL_LOCK);
        registerTechniqueAlias("star_palace_tide_cut", SkillType.FIREBALL);
        registerTechniqueAlias("star_palace_whirlpool", SkillType.FIREBALL);
        registerTechniqueAlias("star_sea_seal_array", SkillType.SEAL_ARRAY);
        registerTechniqueAlias("star_sea_wave", SkillType.FIREBALL);
        registerTechniqueAlias("star_sword_array", SkillType.STAR_SWORD_ARRAY);
        registerTechniqueAlias("steam_cloud", SkillType.STEAM_CLOUD);
        registerTechniqueAlias("summon_wood_puppet", SkillType.SUMMON_WOOD_PUPPET);
        registerTechniqueAlias("suyu_cycle_drain", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("sword_domain", SkillType.SWORD_DOMAIN);
        registerTechniqueAlias("sword_escape", SkillType.SWORD_ESCAPE);
        registerTechniqueAlias("sword_formation_basic", SkillType.SWORD_FORMATION_BASIC);
        registerTechniqueAlias("sword_formation_secret", SkillType.SWORD_FORMATION_SECRET);
        registerTechniqueAlias("sword_merge", SkillType.SWORD_MERGE);
        registerTechniqueAlias("sword_rain", SkillType.SWORD_RAIN);
        registerTechniqueAlias("sword_shield", SkillType.SWORD_SHIELD);
        registerTechniqueAlias("taiyi_xuan_light", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("taoist_seal", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("teleport_talisman", SkillType.TELEPORT_TALISMAN);
        registerTechniqueAlias("thousand_sword_array", SkillType.THOUSAND_SWORD_ARRAY);
        registerTechniqueAlias("thunder_palm", SkillType.THUNDER_PALM);
        registerTechniqueAlias("thunder_talisman_storm", SkillType.THUNDER_TALISMAN_STORM);
        registerTechniqueAlias("thunder_trap_array", SkillType.THUNDER_TRAP_ARRAY);
        registerTechniqueAlias("tianfu_golden_chain", SkillType.TIANFU_GOLDEN_CHAIN);
        registerTechniqueAlias("tianfu_grade3_master_cast", SkillType.FIREBALL);
        registerTechniqueAlias("tianfu_ink_resonance", SkillType.FIREBALL);
        registerTechniqueAlias("tianfu_paper_shield_wall", SkillType.FIREBALL);
        registerTechniqueAlias("tianlan_beast_soul_link", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("tianlan_demon_subdue", SkillType.VAJRA_PALM);
        registerTechniqueAlias("tianlan_holy_descent", SkillType.FIREBALL);
        registerTechniqueAlias("tianlan_iron_palm", SkillType.IRON_SKIN);
        registerTechniqueAlias("tianlan_vajra_body", SkillType.VAJRA_PALM);
        registerTechniqueAlias("tianlan_war_front_oath", SkillType.VAJRA_PALM);
        registerTechniqueAlias("tianlan_zen_shock", SkillType.VAJRA_PALM);
        registerTechniqueAlias("tianmo_berserk", SkillType.TIANMO_BERSERK);
        registerTechniqueAlias("tianmo_blood_altar_strike", SkillType.DEMON_FORM);
        registerTechniqueAlias("tianmo_blood_armor", SkillType.TIANMO_BLOOD_ARMOR);
        registerTechniqueAlias("tianmo_blood_frenzy", SkillType.DEMON_FORM);
        registerTechniqueAlias("tianmo_body_rebuild", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("tianmo_demon_body_secret", SkillType.TIANMO_DEMON_BODY_SECRET);
        registerTechniqueAlias("tianmo_demon_fist", SkillType.DEMON_FORM);
        registerTechniqueAlias("tianmo_heaven_hand", SkillType.DEMON_FORM);
        registerTechniqueAlias("tianmo_iron_demon_skin", SkillType.IRON_SKIN);
        registerTechniqueAlias("tianque_bastion_guard", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("tianque_forge_focus", SkillType.FIREBALL);
        registerTechniqueAlias("tianque_fortress_domain", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("tianque_shield_bash", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("tianque_wall_array", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("tiansha_prison_domain", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("tiansha_prison_force", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("tiansha_xuanqiao_open", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("tianyuan_boundary_break", SkillType.TIANYUAN_BOUNDARY_BREAK);
        registerTechniqueAlias("tianyuan_guard_stance", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("tianyuan_joint_array", SkillType.TIANYUAN_JOINT_ARRAY);
        registerTechniqueAlias("tianyuan_wall_array", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("time_haste_self", SkillType.FIREBALL);
        registerTechniqueAlias("time_lock_spike", SkillType.FIREBALL);
        registerTechniqueAlias("time_reversion_blink", SkillType.FIREBALL);
        registerTechniqueAlias("time_reversion_fragment_xian_3", SkillType.FIREBALL);
        registerTechniqueAlias("time_sense_echo", SkillType.SENSE_SCAN);
        registerTechniqueAlias("time_skip_step", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("time_slow_field", SkillType.FIREBALL);
        registerTechniqueAlias("time_stasis_prison", SkillType.FIREBALL);
        registerTechniqueAlias("treasure_appraisal_glimpse", SkillType.TREASURE_APPRAISAL_GLIMPSE);
        registerTechniqueAlias("tribulation_redirect", SkillType.TRIBULATION_REDIRECT);
        registerTechniqueAlias("tribulation_thunder_ward", SkillType.TRIBULATION_THUNDER_WARD);
        registerTechniqueAlias("true_immortal_sword_art", SkillType.TRUE_IMMORTAL_SWORD_ART);
        registerTechniqueAlias("tuotian_devil_arm", SkillType.DEMON_FORM);
        registerTechniqueAlias("tuotian_mogong_auto_2", SkillType.DEMON_FORM);
        registerTechniqueAlias("tuotian_mogong_auto_3", SkillType.DEMON_FORM);
        registerTechniqueAlias("underworld_flame", SkillType.UNDERWORLD_FLAME);
        registerTechniqueAlias("vajra_body", SkillType.VAJRA_BODY);
        registerTechniqueAlias("vajra_body_flash", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("vajra_body_layer2", SkillType.VAJRA_PALM);
        registerTechniqueAlias("vajra_palm", SkillType.VAJRA_PALM);
        registerTechniqueAlias("veil_of_moon", SkillType.VEIL_OF_MOON);
        registerTechniqueAlias("vine_arrow", SkillType.VINE_ARROW);
        registerTechniqueAlias("void_immortal_blink", SkillType.SHORT_TELEPORT);
        registerTechniqueAlias("void_immortal_fold", SkillType.FIREBALL);
        registerTechniqueAlias("void_immortal_rift", SkillType.FIREBALL);
        registerTechniqueAlias("void_palace_heaven_earth", SkillType.VOID_PALACE_HEAVEN_EARTH);
        registerTechniqueAlias("void_refine_space_sense", SkillType.SENSE_SCAN);
        registerTechniqueAlias("void_refine_touch", SkillType.VOID_REFINE_TOUCH);
        registerTechniqueAlias("void_refining_domain", SkillType.VOID_REFINING_DOMAIN);
        registerTechniqueAlias("void_refining_scripture_auto_3", SkillType.FIREBALL);
        registerTechniqueAlias("void_rift_slash", SkillType.VOID_RIFT_SLASH);
        registerTechniqueAlias("void_rift_step", SkillType.SHADOW_FLASH);
        registerTechniqueAlias("void_slash", SkillType.VOID_SLASH);
        registerTechniqueAlias("void_step", SkillType.VOID_STEP);
        registerTechniqueAlias("wan_sword_return", SkillType.WAN_SWORD_RETURN);
        registerTechniqueAlias("wanhu_afterimage", SkillType.CLONE_IMAGE);
        registerTechniqueAlias("wanhu_beast_shift", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("wanhu_charm_tail", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("wanhu_fox_fire", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("wanhu_nine_illusion", SkillType.WANHU_NINE_ILLUSION);
        registerTechniqueAlias("wanhu_nine_tail_mirage", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("wanhu_thousand_phantom_domain", SkillType.WANHU_THOUSAND_PHANTOM_DOMAIN);
        registerTechniqueAlias("water_arrow", SkillType.WATER_ARROW);
        registerTechniqueAlias("water_dragon", SkillType.WATER_DRAGON);
        registerTechniqueAlias("water_escape", SkillType.WATER_ESCAPE);
        registerTechniqueAlias("water_mirror_reflect", SkillType.WATER_MIRROR_REFLECT);
        registerTechniqueAlias("wind_blade", SkillType.WIND_BLADE);
        registerTechniqueAlias("wind_escape", SkillType.WIND_RIDE);
        registerTechniqueAlias("wind_ride", SkillType.WIND_RIDE);
        registerTechniqueAlias("wind_talisman", SkillType.WIND_TALISMAN);
        registerTechniqueAlias("wood_bind", SkillType.WOOD_BIND);
        registerTechniqueAlias("wood_spirit_shield_basic", SkillType.FIREBALL);
        registerTechniqueAlias("wood_spirit_vine", SkillType.WOOD_SPIRIT_VINE);
        registerTechniqueAlias("word_suppress", SkillType.WORD_SUPPRESS);
        registerTechniqueAlias("wudang_sword_form", SkillType.FLYING_SWORD_STRIKE);
        registerTechniqueAlias("wuxing_cycle_array", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("wuxing_earth_suppress", SkillType.FIREBALL);
        registerTechniqueAlias("wuxing_fire_realm", SkillType.FIREBALL);
        registerTechniqueAlias("wuxing_full_world", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("wuxing_metal_edge", SkillType.FIREBALL);
        registerTechniqueAlias("wuxing_water_mirror", SkillType.FIREBALL);
        registerTechniqueAlias("wuxing_wood_bind_world", SkillType.FIREBALL);
        registerTechniqueAlias("wuxing_world_seed", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("wuzang_mana_well", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("wuzang_organ_shield", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("wuzang_overflow", SkillType.BODY_HARDNESS);
        registerTechniqueAlias("xiaji_clear_channel", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("xiaji_ice_flame_aid", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("xiaji_spirit_surge", SkillType.FIREBALL);
        registerTechniqueAlias("xuantian_ice_prison", SkillType.XUANTIAN_ICE_PRISON);
        registerTechniqueAlias("xuanyin_cold_aura", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("xuanyin_soul_thread", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("xue_lian_shenguang_auto_2", SkillType.DEMON_FORM);
        registerTechniqueAlias("xue_lian_shenguang_auto_3", SkillType.DEMON_FORM);
        registerTechniqueAlias("xuewu_blood_curse_mark", SkillType.XUEWU_BLOOD_CURSE_MARK);
        registerTechniqueAlias("xuewu_blood_lance", SkillType.DEMON_FORM);
        registerTechniqueAlias("xuewu_blood_puppet", SkillType.DEMON_FORM);
        registerTechniqueAlias("xuewu_blood_sigil", SkillType.DEMON_FORM);
        registerTechniqueAlias("xuewu_grand_curse", SkillType.DEMON_FORM);
        registerTechniqueAlias("xuewu_hex_decay", SkillType.DEMON_FORM);
        registerTechniqueAlias("yang_burst", SkillType.YANG_BURST);
        registerTechniqueAlias("yanyue_heart_mirror", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("yanyue_mirage", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("yanyue_moon_illusion", SkillType.YANYUE_MOON_ILLUSION);

        // Batch 4: Remaining technique mappings (29 entries)
        registerTechniqueAlias("yanyue_moon_veil", SkillType.VEIL_OF_MOON);
        registerTechniqueAlias("yanyue_phantom_array", SkillType.YANYUE_PHANTOM_ARRAY);
        registerTechniqueAlias("yao_transform_partial", SkillType.DEMON_FORM);
        registerTechniqueAlias("ye_clan_bloodline_flash", SkillType.BLOOD_ESCAPE);
        registerTechniqueAlias("ye_clan_dark_mandate", SkillType.DEMON_FORM);
        registerTechniqueAlias("yin_body_condense", SkillType.YIN_BODY_CONDENSE);
        registerTechniqueAlias("yin_cluster_ghost_ultimate_secret", SkillType.YIN_CLUSTER_GHOST_ULTIMATE_SECRET);
        registerTechniqueAlias("yin_corrosion", SkillType.YIN_CORROSION);
        registerTechniqueAlias("yin_fire", SkillType.YIN_FIRE);
        registerTechniqueAlias("yin_luo_ghost_cloak", SkillType.YIN_LUO_GHOST_CLOAK);
        registerTechniqueAlias("yin_soul_burst", SkillType.YIN_SOUL_BURST);
        registerTechniqueAlias("yin_soul_chain", SkillType.YIN_SOUL_CHAIN);
        registerTechniqueAlias("yin_soul_devour", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("yin_yang_lunhui_jue_auto_2", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("yin_yang_lunhui_jue_auto_3", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("yinluo_banner_call", SkillType.SPIRIT_ART_BEAST_CALL);
        registerTechniqueAlias("yinluo_corpse_tide", SkillType.CORPSE_ARMOR);
        registerTechniqueAlias("yinluo_nine_soul", SkillType.YIN_SOUL_DEVOUR);
        registerTechniqueAlias("yinluo_soul_harvest", SkillType.YINLUO_SOUL_HARVEST);
        registerTechniqueAlias("yinyang_cycle_balance", SkillType.MIRROR_PHANTOM);
        registerTechniqueAlias("yuanci_shenguang_art_auto_3", SkillType.FIREBALL);
        registerTechniqueAlias("yuling_beast_puppet_art_auto_3", SkillType.BEAST_SUMMON);
        registerTechniqueAlias("zen_pulse", SkillType.ZEN_PULSE);
        registerTechniqueAlias("zhenyan_command_bind", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("zhenyan_elder_canon_xian_3", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("zhenyan_lesson_seal", SkillType.BAGUA_SEAL);
        registerTechniqueAlias("zhenyan_sect_array", SkillType.FORMATION_TRAP_BASIC);
        registerTechniqueAlias("zhenyan_voice_shield", SkillType.TAOIST_SEAL);
        registerTechniqueAlias("zhenyan_word_bolt", SkillType.TAOIST_SEAL);
    }
    private static SkillEffect castTalisman(double damage, String element, String effectKey, String successKey) {
        return new com.xunxian.seekingimmortals.skill.effect.spell.TalismanConsumeSpell(
                8, 80, damage, 16.0D, 3.0D, element, effectKey,
                java.util.Set.of("talisman", "talisman_consume"), successKey);
    }

}
