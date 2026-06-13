package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.skill.SkillType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

public class PlayerCultivation {
    public static final int TECHNIQUE_SLOT_COUNT = 7;
    private static final double GLOBAL_BREAKTHROUGH_CAP = 0.90D;
    private static final int INITIAL_MANA = 100;
    private static final int INITIAL_DIVINE_CONSCIOUSNESS = 5;
    private static final int MAX_QI_DEVIATION_RISK = 100;
    private static final int MAX_TRIBULATION_RESISTANCE = 90;

    private int spiritualPower = INITIAL_MANA;
    private int divineConsciousness = INITIAL_DIVINE_CONSCIOUSNESS;
    private int bodyRefinement = 0;
    private int qiDeviationRisk = 0;
    private int tribulationResistance = 0;
    private Realm realm = Realm.QI_REFINING;
    private RealmStage stage = RealmStage.LAYER_1;
    private int cultivationExp = 0;
    private boolean breakthroughAssisted = false;
    private double breakthroughPillBonus = 0.0D;
    private boolean meditating = false;
    private SpiritualRoot spiritualRoot = SpiritualRoot.TRIPLE;
    private final EnumSet<SpiritualRootAttribute> spiritualRootAttributes = EnumSet.of(SpiritualRootAttribute.WOOD, SpiritualRootAttribute.WATER, SpiritualRootAttribute.FIRE);
    private SpecialPhysique specialPhysique = SpecialPhysique.NONE;
    private int lifespanYears = Realm.QI_REFINING.getLifespanYears();
    private int ageYears = 16;
    private int failedBreakthroughs = 0;
    private boolean rootInitialized = false;
    private int spiritualRootPurity = 50;
    private boolean spiritualRootAwakened = true;
    private boolean spiritualRootTested = false;
    private boolean severeInjury = false;
    private int heartDemonLevel = 0;
    private int heartDemonTriggerTicks = 0;
    private boolean shatteredCore = false;
    private int realmFallScars = 0;
    private final Set<String> learnedTechniques = new HashSet<>();
    private final List<String> techniqueSlots = new ArrayList<>();
    private final Map<String, Long> techniqueCooldownUntilTicks = new HashMap<>();
    private final Map<SkillType, CultivationSkill> skills = new HashMap<>();

    public PlayerCultivation() {
        clearTechniqueSlots();
    }

    public int getSpiritualPower() { return spiritualPower; }
    public int getMana() { return getSpiritualPower(); }
    public int getQi() { return getSpiritualPower(); }
    public int getCultivation() { return getCultivationExp(); }
    public int getDivineConsciousness() { return divineConsciousness; }
    public int getDivSense() { return getDivineConsciousness(); }
    public int getBodyRefinement() { return bodyRefinement; }
    public int getBodyRef() { return getBodyRefinement(); }
    public int getQiDeviationRisk() { return qiDeviationRisk; }
    public int getQiDevRisk() { return getQiDeviationRisk(); }
    public int getTribulationResistance() { return tribulationResistance; }
    public int getTribRes() { return getTribulationResistance(); }
    public Realm getRealm() { return realm; }
    public RealmStage getStage() { return stage; }
    public int getCultivationExp() { return cultivationExp; }
    public int getCurrentStageStartExp() {
        int start = 0;
        for (Realm candidateRealm : Realm.values()) {
            for (RealmStage candidateStage : getStagesForRealm(candidateRealm)) {
                if (candidateRealm == realm && candidateStage == stage) {
                    return start;
                }
                start += candidateRealm.getStageExpSpan();
            }
        }
        return start;
    }
    public int getCurrentStageExpSpan() { return realm.getStageExpSpan(); }
    public int getCurrentStageCapExp() { return getCurrentStageStartExp() + getCurrentStageExpSpan(); }
    public int getCurrentStageProgressExp() { return clamp(cultivationExp - getCurrentStageStartExp(), 0, getCurrentStageExpSpan()); }
    public boolean isAtBreakthroughCap() { return cultivationExp >= getCurrentStageCapExp(); }
    public boolean isAtFinalStage() { return realm == Realm.TRUE_IMMORTAL && stage == RealmStage.LATE; }
    public boolean isMeditating() { return meditating; }
    public SpiritualRoot getSpiritualRoot() { return spiritualRoot; }
    public Set<SpiritualRootAttribute> getSpiritualRootAttributes() { return EnumSet.copyOf(spiritualRootAttributes); }
    public SpiritualRootAttribute getSpiritualRootAttribute() { return spiritualRootAttributes.iterator().next(); }
    public SpecialPhysique getSpecialPhysique() { return specialPhysique; }
    public int getSpiritualRootPurity() { return spiritualRootPurity; }
    public boolean isSpiritualRootAwakened() { return spiritualRootAwakened; }
    public boolean isSpiritualRootTested() { return spiritualRootTested; }
    public boolean hasSevereInjury() { return severeInjury; }
    public boolean hasShatteredCore() { return shatteredCore; }
    public int getHeartDemonLevel() { return heartDemonLevel; }
    public int getHeartDemonTriggerTicks() { return heartDemonTriggerTicks; }
    public int getRealmFallScars() { return realmFallScars; }
    public Set<String> getLearnedTechniques() { return Set.copyOf(learnedTechniques); }
    public List<String> getTechniqueSlots() {
        ensureTechniqueSlotsInitialized();
        return List.copyOf(techniqueSlots);
    }
    public boolean hasLearnedTechnique(String techniqueId) { return learnedTechniques.contains(techniqueId); }
    public boolean learnTechnique(String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) return false;
        boolean added = learnedTechniques.add(techniqueId);
        ensureTechniqueSlotsInitialized();
        if (added && !techniqueSlots.contains(techniqueId)) {
            for (int i = 0; i < TECHNIQUE_SLOT_COUNT; i++) {
                if (techniqueSlots.get(i).isBlank()) {
                    techniqueSlots.set(i, techniqueId);
                    break;
                }
            }
        }
        return added;
    }
    public boolean setTechniqueSlot(int slot, String techniqueId) {
        ensureTechniqueSlotsInitialized();
        if (slot < 0 || slot >= TECHNIQUE_SLOT_COUNT) return false;
        if (techniqueId == null || techniqueId.isBlank()) {
            techniqueSlots.set(slot, "");
            return true;
        }
        if (!hasLearnedTechnique(techniqueId)) return false;
        techniqueSlots.set(slot, techniqueId);
        return true;
    }
    public String getTechniqueSlot(int slot) {
        ensureTechniqueSlotsInitialized();
        return slot >= 0 && slot < TECHNIQUE_SLOT_COUNT ? techniqueSlots.get(slot) : "";
    }
    public long getTechniqueCooldownUntilTick(String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) return 0L;
        return techniqueCooldownUntilTicks.getOrDefault(techniqueId, 0L);
    }
    public void setTechniqueCooldown(String techniqueId, long untilTick) {
        if (techniqueId == null || techniqueId.isBlank()) return;
        if (untilTick <= 0L) {
            techniqueCooldownUntilTicks.remove(techniqueId);
        } else {
            techniqueCooldownUntilTicks.put(techniqueId, untilTick);
        }
    }
    public Map<String, Long> getTechniqueCooldownUntilTicks() {
        return Map.copyOf(techniqueCooldownUntilTicks);
    }

    public boolean hasSkill(SkillType skillType) {
        return skills.containsKey(skillType) && skills.get(skillType).isUnlocked();
    }

    public CultivationSkill getSkill(SkillType skillType) {
        return skills.get(skillType);
    }

    public Map<SkillType, CultivationSkill> getAllSkills() {
        return Map.copyOf(skills);
    }

    public boolean canLearnSkill(SkillType skillType) {
        if (realm.ordinal() < skillType.getRequiredRealm().ordinal()) return false;
        if (!skillType.hasAffinityRequirement()) return true;
        for (SpiritualRootAttribute required : skillType.getAffinityAttributes()) {
            if (spiritualRootAttributes.contains(required)) return true;
        }
        return false;
    }

    public boolean unlockSkill(SkillType skillType) {
        if (!canLearnSkill(skillType)) return false;
        CultivationSkill skill = skills.computeIfAbsent(skillType, CultivationSkill::new);
        if (skill.isUnlocked()) return false;
        skill.unlock();
        return true;
    }

    public boolean addSkillExperience(SkillType skillType, int amount) {
        CultivationSkill skill = skills.get(skillType);
        if (skill == null || !skill.isUnlocked()) return false;
        skill.addExperience(amount);
        return true;
    }

    public boolean addSkillProficiency(SkillType skillType, int amount) {
        CultivationSkill skill = skills.get(skillType);
        if (skill == null || !skill.isUnlocked()) return false;
        skill.addProficiency(amount);
        return true;
    }

    public boolean hasHeartDemon() { return heartDemonLevel > 0; }
    public boolean hasAffliction(ImmortalAffliction affliction) {
        return switch (affliction) {
            case SEVERE_INJURY -> severeInjury;
            case HEART_DEMON -> heartDemonLevel > 0;
            case REALM_FALL -> realmFallScars > 0;
            case SHATTERED_CORE -> shatteredCore;
        };
    }
    public int getLifespanYears() { return lifespanYears; }
    public int getAgeYears() { return ageYears; }
    public int getRemainingLifespanYears() { return Math.max(0, lifespanYears - ageYears); }
    public int getFailedBreakthroughs() { return failedBreakthroughs; }
    public boolean isBreakthroughAssisted() { return breakthroughAssisted || breakthroughPillBonus > 0.0D; }
    public void setBreakthroughAssisted(boolean assisted) {
        this.breakthroughAssisted = assisted;
        if (assisted && breakthroughPillBonus <= 0.0D) {
            setBreakthroughPillBonus(0.05D);
        }
    }
    public double getBreakthroughPillBonus() { return breakthroughPillBonus; }
    public void setBreakthroughPillBonus(double bonus) {
        breakthroughPillBonus = Math.max(0.0D, Math.min(0.20D, bonus));
        breakthroughAssisted = breakthroughPillBonus > 0.0D;
    }
    public void clearBreakthroughPillBonus() {
        breakthroughPillBonus = 0.0D;
        breakthroughAssisted = false;
    }
    public double getBreakthroughObsessionBonus() { return Math.min(0.30D, failedBreakthroughs * 0.05D); }
    public void setMeditating(boolean meditating) { this.meditating = meditating; }
    public void setSpiritualRoot(SpiritualRoot spiritualRoot) { this.spiritualRoot = spiritualRoot; this.rootInitialized = true; }
    public void setSpiritualRootAttribute(SpiritualRootAttribute attribute) {
        this.spiritualRootAttributes.clear();
        this.spiritualRootAttributes.add(attribute);
        this.rootInitialized = true;
    }
    public void setSpecialPhysique(SpecialPhysique specialPhysique) { this.specialPhysique = specialPhysique; }

    public boolean createLingGenIfAbsent(RandomSource random) {
        if (spiritualRootTested) return false;
        applyLingGenResult(LingGenCalculator.roll(random, 0.0D));
        return true;
    }

    public void applySevereInjury() {
        severeInjury = true;
    }

    public void clearSevereInjuryIfRecovered() {
        if (severeInjury && spiritualPower >= getMaxSpiritualPower()) {
            severeInjury = false;
        }
    }

    public double getSpiritualPowerRecoveryMultiplier() {
        return severeInjury ? 0.60D : 1.0D;
    }

    public void applyShatteredCore() {
        shatteredCore = true;
    }

    public double getOutgoingDamageMultiplier() {
        return (shatteredCore ? 0.70D : 1.0D) * getSpiritualRootDamageMultiplier();
    }

    /**
     * 基础战斗亲和：把灵根属性落实到现有近战、投射物和符箓伤害上。
     * 该倍率只在灵根已觉醒时生效，纯度越高收益越接近满值；未觉醒不提供伤害加成。
     */
    public double getSpiritualRootDamageMultiplier() {
        if (!spiritualRootAwakened || spiritualRootAttributes.isEmpty()) return 1.0D;
        double affinity = spiritualRootAttributes.stream()
                .mapToDouble(this::getAttributeDamageAffinity)
                .average()
                .orElse(1.0D);
        double purityFactor = 0.50D + Math.max(1, Math.min(100, spiritualRootPurity)) / 200.0D;
        return Math.max(0.70D, 1.0D + (affinity - 1.0D) * purityFactor);
    }

    /**
     * 指定属性术法亲和倍率。
     * 主属性命中给完整加成，副属性命中给半额加成；未觉醒或未检测灵根时不提供专精加成。
     *
     * <p>保留该旧入口用于兼容物品与后续代码，实际公式统一委托给 TechniqueAffinityCalculator。</p>
     */
    public double getTechniqueAffinityMultiplier(SpiritualRootAttribute primary, SpiritualRootAttribute... secondary) {
        return TechniqueAffinityCalculator.calculate(this, primary, secondary).multiplier();
    }

    public int getTechniqueEffectAmplifierBonus(SpiritualRootAttribute primary, SpiritualRootAttribute... secondary) {
        return TechniqueAffinityCalculator.calculate(this, primary, secondary).multiplier() >= 1.25D ? 1 : 0;
    }

    public int getTechniqueDurationBonusTicks(int baseDurationTicks, SpiritualRootAttribute primary, SpiritualRootAttribute... secondary) {
        double multiplier = TechniqueAffinityCalculator.calculate(this, primary, secondary).multiplier();
        return Math.max(0, (int)Math.round(baseDurationTicks * (multiplier - 1.0D)));
    }

    private double getAttributeDamageAffinity(SpiritualRootAttribute attribute) {
        return switch (attribute) {
            case METAL -> 1.12D;
            case WOOD -> 1.04D;
            case WATER -> 1.06D;
            case FIRE -> 1.15D;
            case EARTH -> 1.08D;
            case WIND -> 1.13D;
            case THUNDER -> 1.20D;
            case ICE -> 1.14D;
            case DARK -> 1.16D;
            case HIDDEN_THUNDER -> 1.24D;
            case HIDDEN_DARK -> 1.22D;
            case IMMORTAL -> 1.28D;
            case NONE -> 1.0D;
        };
    }

    public void applyHeartDemon(RandomSource random) {
        if (heartDemonLevel <= 0) {
            heartDemonLevel = 1;
        }
        scheduleHeartDemonTrigger(random);
    }

    public void increaseHeartDemonLayer(RandomSource random) {
        heartDemonLevel = Math.max(1, heartDemonLevel + 1);
        scheduleHeartDemonTrigger(random);
    }

    public void clearHeartDemon() {
        heartDemonLevel = 0;
        heartDemonTriggerTicks = 0;
    }

    public void scheduleHeartDemonTrigger(RandomSource random) {
        if (heartDemonLevel <= 0) return;
        int minSeconds = Math.max(60, 600 - (heartDemonLevel - 1) * 60);
        int maxSeconds = Math.max(minSeconds + 60, 1800 - (heartDemonLevel - 1) * 120);
        heartDemonTriggerTicks = (minSeconds + random.nextInt(maxSeconds - minSeconds + 1)) * 20;
    }

    public boolean tickHeartDemonTimer(RandomSource random) {
        if (heartDemonLevel <= 0) return false;
        if (heartDemonTriggerTicks <= 0) {
            scheduleHeartDemonTrigger(random);
            return true;
        }
        heartDemonTriggerTicks--;
        if (heartDemonTriggerTicks <= 0) {
            scheduleHeartDemonTrigger(random);
            return true;
        }
        return false;
    }

    public void applyRealmFall(RandomSource random) {
        int layers = random.nextDouble() < 0.20D ? 2 : 1;
        for (int i = 0; i < layers; i++) {
            fallOneStage();
        }
        realmFallScars++;
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
    }

    private void fallOneStage() {
        cultivationExp = Math.max(0, cultivationExp - realm.getStageExpSpan());
        updateRealmFromCultivationExp();
    }

    public void applyLingGenResult(LingGenCalculator.Result result) {
        spiritualRoot = result.root();
        spiritualRootAttributes.clear();
        spiritualRootAttributes.addAll(result.attributes());
        spiritualRootPurity = Math.max(1, Math.min(100, result.purity()));
        spiritualRootAwakened = result.awakened();
        spiritualRootTested = true;
        rootInitialized = true;
    }

    public void retestLingGen(RandomSource random, boolean purified) {
        LingGenCalculator.Result result = purified
                ? LingGenCalculator.rollAfterPurifying(random, spiritualRootPurity)
                : LingGenCalculator.roll(random, 0.0D);
        applyLingGenResult(result);
    }

    public void ensureRootInitialized(RandomSource random) {
        if (rootInitialized) return;
        spiritualRoot = randomRoot(random);
        spiritualRootAttributes.clear();
        spiritualRootAttributes.addAll(randomAttributes(random, spiritualRoot));
        specialPhysique = SpecialPhysique.random(random);
        rootInitialized = true;
    }

    private SpiritualRoot randomRoot(RandomSource random) {
        int roll = random.nextInt(10000);
        if (roll < 8) return SpiritualRoot.HEAVENLY;
        // 隐灵根千年难遇且需特殊血脉/机缘觉醒，暂不参与首次登录随机。
        if (roll < 88) return SpiritualRoot.MUTATED;
        if (roll < 1350) return SpiritualRoot.DUAL;
        if (roll < 4300) return SpiritualRoot.TRIPLE;
        if (roll < 8900) return SpiritualRoot.PSEUDO;
        return SpiritualRoot.FIVE_ELEMENTS;
    }

    private List<SpiritualRootAttribute> randomAttributes(RandomSource random, SpiritualRoot root) {
        List<SpiritualRootAttribute> result = new ArrayList<>();
        EnumSet<SpiritualRootAttribute> excluded = EnumSet.noneOf(SpiritualRootAttribute.class);
        if (root == SpiritualRoot.HIDDEN) {
            result.add(SpiritualRootAttribute.randomHidden(random));
            return result;
        }
        if (root == SpiritualRoot.MUTATED) {
            result.add(SpiritualRootAttribute.randomMutated(random));
            return result;
        }
        int count = root.getAttributeCount();
        while (result.size() < count) {
            SpiritualRootAttribute attribute = SpiritualRootAttribute.randomFiveElement(random, excluded);
            excluded.add(attribute);
            result.add(attribute);
        }
        return result;
    }

    public String getSpiritualRootAttributeNames() {
        return spiritualRootAttributes.stream()
                .map(SpiritualRootAttribute::getDisplayName)
                .collect(Collectors.joining("/"));
    }

    public double getSpiritualRootCultivationSpeedCoefficient() {
        return spiritualRoot.getCultivationSpeedCoefficient();
    }

    public double getPhysiqueCultivationSpeedMultiplier() {
        return specialPhysique.getCultivationMultiplier();
    }

    public double getCultivationSpeedMultiplier() {
        return Math.max(0.05D, getSpiritualRootCultivationSpeedCoefficient() * getPhysiqueCultivationSpeedMultiplier());
    }

    public double getMeleeAttackPower() {
        double base = switch (realm) {
            case QI_REFINING -> 2.0D + stage.ordinal() * 0.5D;
            case FOUNDATION_ESTABLISHMENT -> 15.0D + stage.ordinal() * 3.0D;
            case CORE_FORMATION -> 40.0D + stage.ordinal() * 8.0D;
            case NASCENT_SOUL -> 100.0D + stage.ordinal() * 20.0D;
            case SOUL_TRANSFORMATION -> 250.0D + stage.ordinal() * 50.0D;
            case VOID_REFINEMENT -> 600.0D + stage.ordinal() * 120.0D;
            case UNITY -> 1500.0D + stage.ordinal() * 300.0D;
            case MAHAYANA -> 4000.0D + stage.ordinal() * 800.0D;
            case TRIBULATION -> 10000.0D + stage.ordinal() * 2000.0D;
            case TRUE_IMMORTAL -> 25000.0D + stage.ordinal() * 5000.0D;
        };
        double attributeMultiplier = spiritualRootAttributes.stream()
                .mapToDouble(attr -> switch (attr) {
                    case FIRE -> 1.15D;
                    case THUNDER -> 1.20D;
                    case METAL -> 1.12D;
                    case DARK -> 1.10D;
                    case HIDDEN_THUNDER -> 1.18D;
                    default -> 1.0D;
                })
                .average()
                .orElse(1.0D);
        return base * attributeMultiplier * specialPhysique.getCultivationMultiplier();
    }

    public double getMagicAttackPower() {
        return getMeleeAttackPower() * 1.2D;
    }

    public double getDefensePower() {
        double base = switch (realm) {
            case QI_REFINING -> 1.0D + stage.ordinal() * 0.3D;
            case FOUNDATION_ESTABLISHMENT -> 10.0D + stage.ordinal() * 2.0D;
            case CORE_FORMATION -> 30.0D + stage.ordinal() * 6.0D;
            case NASCENT_SOUL -> 80.0D + stage.ordinal() * 15.0D;
            case SOUL_TRANSFORMATION -> 200.0D + stage.ordinal() * 40.0D;
            case VOID_REFINEMENT -> 500.0D + stage.ordinal() * 100.0D;
            case UNITY -> 1200.0D + stage.ordinal() * 250.0D;
            case MAHAYANA -> 3000.0D + stage.ordinal() * 600.0D;
            case TRIBULATION -> 8000.0D + stage.ordinal() * 1600.0D;
            case TRUE_IMMORTAL -> 20000.0D + stage.ordinal() * 4000.0D;
        };
        double attributeMultiplier = spiritualRootAttributes.stream()
                .mapToDouble(attr -> switch (attr) {
                    case EARTH -> 1.20D;
                    case WATER -> 1.12D;
                    case ICE -> 1.15D;
                    case METAL -> 1.08D;
                    default -> 1.0D;
                })
                .average()
                .orElse(1.0D);
        return base * attributeMultiplier * specialPhysique.getBreakthroughMultiplier();
    }

    public double getDodgeRate() {
        double base = switch (realm) {
            case QI_REFINING -> 0.05D;
            case FOUNDATION_ESTABLISHMENT -> 0.08D;
            case CORE_FORMATION -> 0.12D;
            case NASCENT_SOUL -> 0.15D;
            case SOUL_TRANSFORMATION -> 0.18D;
            case VOID_REFINEMENT -> 0.22D;
            case UNITY -> 0.26D;
            case MAHAYANA -> 0.30D;
            case TRIBULATION -> 0.35D;
            case TRUE_IMMORTAL -> 0.40D;
        };
        double attributeBonus = spiritualRootAttributes.stream()
                .mapToDouble(attr -> switch (attr) {
                    case WIND -> 0.10D;
                    case THUNDER -> 0.06D;
                    case HIDDEN_THUNDER -> 0.08D;
                    case WATER -> 0.04D;
                    default -> 0.0D;
                })
                .max()
                .orElse(0.0D);
        return Math.min(0.75D, base + attributeBonus);
    }

    public double getCriticalRate() {
        double base = switch (realm) {
            case QI_REFINING -> 0.05D;
            case FOUNDATION_ESTABLISHMENT -> 0.08D;
            case CORE_FORMATION -> 0.10D;
            case NASCENT_SOUL -> 0.12D;
            case SOUL_TRANSFORMATION -> 0.15D;
            case VOID_REFINEMENT -> 0.18D;
            case UNITY -> 0.22D;
            case MAHAYANA -> 0.26D;
            case TRIBULATION -> 0.30D;
            case TRUE_IMMORTAL -> 0.35D;
        };
        double attributeBonus = spiritualRootAttributes.stream()
                .mapToDouble(attr -> switch (attr) {
                    case THUNDER -> 0.12D;
                    case HIDDEN_THUNDER -> 0.15D;
                    case FIRE -> 0.08D;
                    case METAL -> 0.06D;
                    case DARK -> 0.10D;
                    case HIDDEN_DARK -> 0.12D;
                    default -> 0.0D;
                })
                .max()
                .orElse(0.0D);
        return Math.min(0.80D, base + attributeBonus);
    }

    public double getMagicResistance() {
        double base = switch (realm) {
            case QI_REFINING -> 0.0D;
            case FOUNDATION_ESTABLISHMENT -> 0.05D;
            case CORE_FORMATION -> 0.12D;
            case NASCENT_SOUL -> 0.20D;
            case SOUL_TRANSFORMATION -> 0.28D;
            case VOID_REFINEMENT -> 0.35D;
            case UNITY -> 0.42D;
            case MAHAYANA -> 0.50D;
            case TRIBULATION -> 0.60D;
            case TRUE_IMMORTAL -> 0.70D;
        };
        double attributeBonus = spiritualRootAttributes.stream()
                .mapToDouble(attr -> switch (attr) {
                    case EARTH -> 0.08D;
                    case WATER -> 0.06D;
                    case METAL -> 0.05D;
                    case ICE -> 0.07D;
                    default -> 0.0D;
                })
                .max()
                .orElse(0.0D);
        return Math.min(0.85D, base + attributeBonus);
    }

    public double getBreakthroughMultiplier() {
        double attributeMultiplier = spiritualRootAttributes.stream()
                .mapToDouble(SpiritualRootAttribute::getBreakthroughCoefficient)
                .average()
                .orElse(1.0D);
        return spiritualRoot.getBreakthroughCoefficient() * attributeMultiplier * specialPhysique.getBreakthroughMultiplier();
    }

    public int getMaxSpiritualPower() {
        return Math.round(realm.getBaseMaxSpiritualPower() * stage.getMaxSpiritualPowerMultiplier());
    }

    public int getManaMax() { return getMaxSpiritualPower(); }
    public int getMaxQi() { return getMaxSpiritualPower(); }

    public int getMaxDivineConsciousness() {
        int base = switch (realm) {
            case QI_REFINING -> 50;
            case FOUNDATION_ESTABLISHMENT -> 150;
            case CORE_FORMATION -> 400;
            case NASCENT_SOUL -> 1000;
            case SOUL_TRANSFORMATION -> 2500;
            case VOID_REFINEMENT -> 6000;
            case UNITY -> 15000;
            case MAHAYANA -> 40000;
            case TRIBULATION -> 100000;
            case TRUE_IMMORTAL -> 250000;
        };
        return Math.round(base * stage.getMaxSpiritualPowerMultiplier());
    }

    public void addSpiritualPower(int amount) {
        int adjusted = amount > 0 ? Math.max(0, (int)Math.floor(amount * getSpiritualPowerRecoveryMultiplier())) : amount;
        if (amount > 0 && adjusted <= 0) adjusted = 1;
        spiritualPower = Math.max(0, Math.min(getMaxSpiritualPower(), spiritualPower + adjusted));
        clearSevereInjuryIfRecovered();
    }

    public void setMana(int amount) {
        spiritualPower = Math.max(0, Math.min(getMaxSpiritualPower(), amount));
        clearSevereInjuryIfRecovered();
    }

    public void setSpiritualPower(int amount) { setMana(amount); }

    public void addQi(int amount) { addSpiritualPower(amount); }

    public void addDivineConsciousness(int amount) {
        divineConsciousness = Math.max(0, Math.min(getMaxDivineConsciousness(), divineConsciousness + amount));
    }

    public void addDivSense(int amount) { addDivineConsciousness(amount); }

    public void setDivSense(int amount) {
        divineConsciousness = Math.max(0, Math.min(getMaxDivineConsciousness(), amount));
    }

    public void addBodyRefinement(int amount) {
        bodyRefinement = Math.max(0, bodyRefinement + amount);
    }

    public void addBodyRef(int amount) { addBodyRefinement(amount); }

    public void setBodyRefinement(int amount) {
        bodyRefinement = Math.max(0, amount);
    }

    public void setBodyRef(int amount) { setBodyRefinement(amount); }

    public void addQiDeviationRisk(int amount) {
        qiDeviationRisk = clamp(qiDeviationRisk + amount, 0, MAX_QI_DEVIATION_RISK);
    }

    public void setQiDeviationRisk(int amount) {
        qiDeviationRisk = clamp(amount, 0, MAX_QI_DEVIATION_RISK);
    }

    public void setQiDevRisk(int amount) { setQiDeviationRisk(amount); }

    public void addTribulationResistance(int amount) {
        tribulationResistance = clamp(tribulationResistance + amount, 0, MAX_TRIBULATION_RESISTANCE);
    }

    public void setTribulationResistance(int amount) {
        tribulationResistance = clamp(amount, 0, MAX_TRIBULATION_RESISTANCE);
    }

    public void setTribRes(int amount) { setTribulationResistance(amount); }

    public boolean consumeSpiritualPower(int amount) {
        if (spiritualPower < amount) return false;
        spiritualPower -= amount;
        return true;
    }

    public boolean consumeQi(int amount) { return consumeSpiritualPower(amount); }

    public boolean consumeDivineConsciousness(int amount) {
        if (divineConsciousness < amount) return false;
        divineConsciousness -= amount;
        return true;
    }

    public void addCultivationExp(int amount) {
        int adjusted = Math.max(0, (int)Math.round(amount * getCultivationSpeedMultiplier()));
        cultivationExp = Math.max(getCurrentStageStartExp(), Math.min(getCurrentStageCapExp(), cultivationExp + adjusted));
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
    }

    public void addAgeYears(int years) {
        ageYears = Math.max(0, ageYears + years);
    }

    public boolean isLifespanExhausted() {
        return ageYears >= lifespanYears;
    }

    public double getBreakthroughChance() {
        return getBreakthroughChance(BreakthroughChanceModifiers.NONE);
    }

    public double getBreakthroughChance(BreakthroughChanceModifiers modifiers) {
        return getBreakthroughChanceBreakdown(modifiers).chance();
    }

    public BreakthroughChanceBreakdown getBreakthroughChanceBreakdown(BreakthroughChanceModifiers modifiers) {
        double baseChance = Math.min(GLOBAL_BREAKTHROUGH_CAP, getBaseBreakthroughChance() * getBreakthroughMultiplier());
        double obsessionBonus = getBreakthroughObsessionBonus();
        double pillBonus = modifiers == null ? 0.0D : modifiers.pillBonus();
        double spiritEyeBonus = modifiers == null ? 0.0D : modifiers.spiritEyeBonus();
        double techniqueQualityBonus = modifiers == null ? 0.0D : modifiers.techniqueQualityBonus();
        double chance = Math.min(GLOBAL_BREAKTHROUGH_CAP, baseChance + pillBonus + spiritEyeBonus + techniqueQualityBonus + obsessionBonus);
        return new BreakthroughChanceBreakdown(baseChance, pillBonus, spiritEyeBonus, techniqueQualityBonus, obsessionBonus, chance);
    }

    public BreakthroughAttemptResult tryBreakthrough(RandomSource random) {
        return tryBreakthrough(random, BreakthroughChanceModifiers.NONE);
    }

    public BreakthroughAttemptResult tryBreakthrough(RandomSource random, BreakthroughChanceModifiers modifiers) {
        Realm oldRealm = realm;
        RealmStage oldStage = stage;
        BreakthroughChanceBreakdown breakdown = getBreakthroughChanceBreakdown(modifiers);
        if (isAtFinalStage()) return new BreakthroughAttemptResult(BreakthroughAttemptStatus.FINAL_STAGE, false, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
        if (!isAtBreakthroughCap()) return new BreakthroughAttemptResult(BreakthroughAttemptStatus.NOT_AT_CAP, false, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
        clearBreakthroughPillBonus();
        if (random.nextDouble() > breakdown.chance()) {
            failedBreakthroughs++;
            int stageStart = getCurrentStageStartExp();
            int remainingProgress = Math.max(0, (int)Math.floor(getCurrentStageExpSpan() * 0.80D));
            cultivationExp = stageStart + remainingProgress;
            addQiDeviationRisk(10);
            boolean qiDeviationTriggered = checkQiDeviation(random);
            return new BreakthroughAttemptResult(BreakthroughAttemptStatus.FAILURE, qiDeviationTriggered, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
        }
        failedBreakthroughs = 0;
        spiritualPower = 0;
        advanceOneStage();
        cultivationExp = getCurrentStageStartExp();
        return new BreakthroughAttemptResult(BreakthroughAttemptStatus.SUCCESS, false, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
    }

    public boolean tryBreakthrough() {
        return tryBreakthrough(RandomSource.create()).success();
    }

    public enum BreakthroughAttemptStatus {
        SUCCESS,
        FAILURE,
        NOT_AT_CAP,
        FINAL_STAGE
    }

    public record BreakthroughChanceModifiers(double pillBonus, double spiritEyeBonus, double techniqueQualityBonus) {
        public static final BreakthroughChanceModifiers NONE = new BreakthroughChanceModifiers(0.0D, 0.0D, 0.0D);
    }

    public record BreakthroughChanceBreakdown(
            double baseChance,
            double pillBonus,
            double spiritEyeBonus,
            double techniqueQualityBonus,
            double obsessionBonus,
            double chance) {}

    public record BreakthroughAttemptResult(
            BreakthroughAttemptStatus status,
            boolean qiDeviationTriggered,
            Realm oldRealm,
            RealmStage oldStage,
            Realm newRealm,
            RealmStage newStage,
            BreakthroughChanceBreakdown chanceBreakdown,
            int qiDeviationRisk) {
        public boolean success() { return status == BreakthroughAttemptStatus.SUCCESS; }
        public double chance() { return chanceBreakdown.chance(); }
    }

    private boolean checkQiDeviation(RandomSource random) {
        if (qiDeviationRisk < 70) return false;
        double chance = Math.min(0.50D, Math.max(0.20D, (qiDeviationRisk - 50) / 100.0D));
        if (random.nextDouble() >= chance) return false;
        if (heartDemonLevel <= 0) {
            applyHeartDemon(random);
        } else {
            increaseHeartDemonLayer(random);
        }
        return true;
    }

    private void advanceOneStage() {
        RealmStage[] stages = getStagesForRealm(realm);
        for (int i = 0; i < stages.length; i++) {
            if (stages[i] != stage) continue;
            if (i + 1 < stages.length) {
                stage = stages[i + 1];
            } else {
                Realm nextRealm = realm.next();
                if (nextRealm != realm) {
                    realm = nextRealm;
                    stage = getStagesForRealm(realm)[0];
                }
            }
            lifespanYears = Math.max(lifespanYears, realm.getLifespanYears());
            divineConsciousness = Math.min(divineConsciousness, getMaxDivineConsciousness());
            spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
            return;
        }
    }

    private double getBaseBreakthroughChance() {
        return switch (realm) {
            case QI_REFINING -> 0.05D;
            case FOUNDATION_ESTABLISHMENT -> 0.03D;
            case CORE_FORMATION -> 0.08D;
            case NASCENT_SOUL -> 0.02D;
            case SOUL_TRANSFORMATION -> 0.005D;
            case VOID_REFINEMENT -> 0.003D;
            case UNITY -> 0.002D;
            case MAHAYANA -> 0.001D;
            case TRIBULATION -> 0.0005D;
            case TRUE_IMMORTAL -> 0.0001D;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int getFailureLifespanPenalty() {
        return switch (realm) {
            case QI_REFINING -> 10;
            case FOUNDATION_ESTABLISHMENT -> 15;
            case CORE_FORMATION -> 25;
            case NASCENT_SOUL -> 50;
            case SOUL_TRANSFORMATION -> 100;
            case VOID_REFINEMENT -> 200;
            case UNITY -> 400;
            case MAHAYANA -> 800;
            case TRIBULATION -> 1600;
            case TRUE_IMMORTAL -> 3000;
        };
    }

    private void updateRealmFromCultivationExp() {
        int remaining = cultivationExp;
        for (Realm candidateRealm : Realm.values()) {
            RealmStage[] stages = getStagesForRealm(candidateRealm);
            for (RealmStage candidateStage : stages) {
                if (remaining < candidateRealm.getStageExpSpan()) {
                    realm = candidateRealm;
                    stage = candidateStage;
                    lifespanYears = Math.max(lifespanYears, candidateRealm.getLifespanYears());
                    divineConsciousness = Math.min(divineConsciousness, getMaxDivineConsciousness());
                    return;
                }
                remaining -= candidateRealm.getStageExpSpan();
            }
        }
        realm = Realm.TRUE_IMMORTAL;
        stage = RealmStage.LATE;
        lifespanYears = Math.max(lifespanYears, realm.getLifespanYears());
        divineConsciousness = Math.min(divineConsciousness, getMaxDivineConsciousness());
    }

    private boolean loadRealmAndStage(CompoundTag tag) {
        try {
            Realm loadedRealm = Realm.valueOf(tag.getString("Realm"));
            RealmStage loadedStage = RealmStage.valueOf(tag.getString("Stage"));
            boolean validStage = false;
            for (RealmStage candidateStage : getStagesForRealm(loadedRealm)) {
                if (candidateStage == loadedStage) {
                    validStage = true;
                    break;
                }
            }
            if (!validStage) return false;
            realm = loadedRealm;
            stage = loadedStage;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private RealmStage[] getStagesForRealm(Realm targetRealm) {
        if (targetRealm.isLayerBased()) {
            return new RealmStage[] {
                    RealmStage.LAYER_1, RealmStage.LAYER_2, RealmStage.LAYER_3, RealmStage.LAYER_4, RealmStage.LAYER_5,
                    RealmStage.LAYER_6, RealmStage.LAYER_7, RealmStage.LAYER_8, RealmStage.LAYER_9, RealmStage.LAYER_10,
                    RealmStage.LAYER_11, RealmStage.LAYER_12, RealmStage.LAYER_13
            };
        }
        return new RealmStage[] { RealmStage.EARLY, RealmStage.MIDDLE, RealmStage.LATE };
    }

    public CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("cultivation", cultivationExp);
        tag.putInt("mana", spiritualPower);
        tag.putInt("manaMax", getMaxSpiritualPower());
        tag.putInt("divSense", divineConsciousness);
        tag.putInt("bodyRef", bodyRefinement);
        tag.putInt("qiDevRisk", qiDeviationRisk);
        tag.putInt("tribRes", tribulationResistance);
        tag.putInt("SpiritualPower", spiritualPower);
        tag.putInt("Qi", spiritualPower);
        tag.putInt("DivineConsciousness", divineConsciousness);
        tag.putString("Realm", realm.name());
        tag.putString("Stage", stage.name());
        tag.putInt("CultivationExp", cultivationExp);
        tag.putBoolean("BreakthroughAssisted", isBreakthroughAssisted());
        tag.putDouble("BreakthroughPillBonus", breakthroughPillBonus);
        tag.putBoolean("Meditating", meditating);
        tag.putString("SpiritualRoot", spiritualRoot.name());
        tag.putString("SpiritualRootAttributes", spiritualRootAttributes.stream().map(Enum::name).collect(Collectors.joining(",")));
        tag.putString("SpiritualRootAttribute", getSpiritualRootAttribute().name());
        tag.putString("SpecialPhysique", specialPhysique.name());
        tag.putInt("LifespanYears", lifespanYears);
        tag.putInt("AgeYears", ageYears);
        tag.putInt("FailedBreakthroughs", failedBreakthroughs);
        tag.putBoolean("RootInitialized", rootInitialized);
        tag.putInt("SpiritualRootPurity", spiritualRootPurity);
        tag.putBoolean("SpiritualRootAwakened", spiritualRootAwakened);
        tag.putBoolean("SpiritualRootTested", spiritualRootTested);
        tag.putBoolean("SevereInjury", severeInjury);
        tag.putInt("HeartDemonLevel", heartDemonLevel);
        tag.putInt("HeartDemonTriggerTicks", heartDemonTriggerTicks);
        tag.putBoolean("ShatteredCore", shatteredCore);
        tag.putInt("RealmFallScars", realmFallScars);
        ListTag learnedTechniqueList = new ListTag();
        learnedTechniques.stream().sorted().forEach(techniqueId -> learnedTechniqueList.add(StringTag.valueOf(techniqueId)));
        tag.put("LearnedTechniques", learnedTechniqueList);
        ensureTechniqueSlotsInitialized();
        ListTag techniqueSlotList = new ListTag();
        for (String techniqueId : techniqueSlots) {
            techniqueSlotList.add(StringTag.valueOf(techniqueId == null ? "" : techniqueId));
        }
        tag.put("TechniqueSlots", techniqueSlotList);
        CompoundTag cooldownTag = new CompoundTag();
        techniqueCooldownUntilTicks.forEach(cooldownTag::putLong);
        tag.put("TechniqueCooldownUntilTicks", cooldownTag);
        ListTag skillList = new ListTag();
        skills.values().stream()
                .filter(CultivationSkill::isUnlocked)
                .forEach(skill -> skillList.add(skill.saveNBT()));
        tag.put("Skills", skillList);
        return tag;
    }

    public void loadNBTData(CompoundTag tag) {
        spiritualPower = tag.contains("mana") ? tag.getInt("mana") : (tag.contains("SpiritualPower") ? tag.getInt("SpiritualPower") : (tag.contains("Qi") ? tag.getInt("Qi") : INITIAL_MANA));
        divineConsciousness = tag.contains("divSense") ? tag.getInt("divSense") : (tag.contains("DivineConsciousness") ? tag.getInt("DivineConsciousness") : INITIAL_DIVINE_CONSCIOUSNESS);
        bodyRefinement = Math.max(0, tag.getInt("bodyRef"));
        qiDeviationRisk = clamp(tag.getInt("qiDevRisk"), 0, MAX_QI_DEVIATION_RISK);
        tribulationResistance = clamp(tag.getInt("tribRes"), 0, MAX_TRIBULATION_RESISTANCE);
        cultivationExp = tag.contains("cultivation") ? tag.getInt("cultivation") : tag.getInt("CultivationExp");
        if (!loadRealmAndStage(tag)) {
            updateRealmFromCultivationExp();
        }
        cultivationExp = Math.max(getCurrentStageStartExp(), Math.min(cultivationExp, getCurrentStageCapExp()));
        breakthroughPillBonus = tag.contains("BreakthroughPillBonus") ? Math.max(0.0D, Math.min(0.20D, tag.getDouble("BreakthroughPillBonus"))) : (tag.getBoolean("BreakthroughAssisted") ? 0.05D : 0.0D);
        breakthroughAssisted = breakthroughPillBonus > 0.0D;
        meditating = tag.getBoolean("Meditating");
        try { spiritualRoot = SpiritualRoot.valueOf(tag.getString("SpiritualRoot")); } catch (Exception ignored) { spiritualRoot = SpiritualRoot.TRIPLE; }
        loadSpiritualRootAttributes(tag);
        try { specialPhysique = SpecialPhysique.valueOf(tag.getString("SpecialPhysique")); } catch (Exception ignored) { specialPhysique = SpecialPhysique.NONE; }
        lifespanYears = tag.contains("LifespanYears") ? tag.getInt("LifespanYears") : Realm.QI_REFINING.getLifespanYears();
        ageYears = tag.contains("AgeYears") ? tag.getInt("AgeYears") : 16;
        failedBreakthroughs = tag.getInt("FailedBreakthroughs");
        rootInitialized = tag.getBoolean("RootInitialized") || tag.contains("SpiritualRootAttributes") || tag.contains("SpiritualRootAttribute");
        spiritualRootPurity = tag.contains("SpiritualRootPurity") ? Math.max(1, Math.min(100, tag.getInt("SpiritualRootPurity"))) : 50;
        spiritualRootAwakened = !tag.contains("SpiritualRootAwakened") || tag.getBoolean("SpiritualRootAwakened");
        spiritualRootTested = tag.getBoolean("SpiritualRootTested");
        severeInjury = tag.getBoolean("SevereInjury");
        heartDemonLevel = Math.max(0, tag.getInt("HeartDemonLevel"));
        heartDemonTriggerTicks = Math.max(0, tag.getInt("HeartDemonTriggerTicks"));
        shatteredCore = tag.getBoolean("ShatteredCore");
        realmFallScars = Math.max(0, tag.getInt("RealmFallScars"));
        learnedTechniques.clear();
        if (tag.contains("LearnedTechniques")) {
            ListTag learnedTechniqueList = tag.getList("LearnedTechniques", 8);
            for (int i = 0; i < learnedTechniqueList.size(); i++) {
                String techniqueId = learnedTechniqueList.getString(i);
                if (!techniqueId.isBlank()) {
                    learnedTechniques.add(techniqueId);
                }
            }
        }
        clearTechniqueSlots();
        if (tag.contains("TechniqueSlots")) {
            ListTag techniqueSlotList = tag.getList("TechniqueSlots", 8);
            for (int i = 0; i < Math.min(TECHNIQUE_SLOT_COUNT, techniqueSlotList.size()); i++) {
                String techniqueId = techniqueSlotList.getString(i);
                techniqueSlots.set(i, learnedTechniques.contains(techniqueId) ? techniqueId : "");
            }
        } else {
            fillDefaultTechniqueSlots();
        }
        techniqueCooldownUntilTicks.clear();
        if (tag.contains("TechniqueCooldownUntilTicks")) {
            CompoundTag cooldownTag = tag.getCompound("TechniqueCooldownUntilTicks");
            for (String techniqueId : cooldownTag.getAllKeys()) {
                long untilTick = cooldownTag.getLong(techniqueId);
                if (!techniqueId.isBlank() && untilTick > 0L) {
                    techniqueCooldownUntilTicks.put(techniqueId, untilTick);
                }
            }
        }
        skills.clear();
        if (tag.contains("Skills")) {
            ListTag skillList = tag.getList("Skills", 10);
            for (int i = 0; i < skillList.size(); i++) {
                CompoundTag skillTag = skillList.getCompound(i);
                CultivationSkill skill = CultivationSkill.loadNBT(skillTag);
                if (skill != null) {
                    skills.put(skill.getSkillType(), skill);
                }
            }
        }
        divineConsciousness = Math.min(divineConsciousness, getMaxDivineConsciousness());
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
    }

    private void clearTechniqueSlots() {
        techniqueSlots.clear();
        for (int i = 0; i < TECHNIQUE_SLOT_COUNT; i++) {
            techniqueSlots.add("");
        }
    }

    private void ensureTechniqueSlotsInitialized() {
        if (techniqueSlots.size() != TECHNIQUE_SLOT_COUNT) {
            clearTechniqueSlots();
            fillDefaultTechniqueSlots();
            return;
        }
        for (int i = 0; i < TECHNIQUE_SLOT_COUNT; i++) {
            String techniqueId = techniqueSlots.get(i);
            if (techniqueId == null || !techniqueId.isBlank() && !learnedTechniques.contains(techniqueId)) {
                techniqueSlots.set(i, "");
            }
        }
    }

    private void fillDefaultTechniqueSlots() {
        List<String> sorted = learnedTechniques.stream().sorted().toList();
        for (int i = 0; i < Math.min(TECHNIQUE_SLOT_COUNT, sorted.size()); i++) {
            techniqueSlots.set(i, sorted.get(i));
        }
    }

    private void loadSpiritualRootAttributes(CompoundTag tag) {
        spiritualRootAttributes.clear();
        String stored = tag.contains("SpiritualRootAttributes") ? tag.getString("SpiritualRootAttributes") : tag.getString("SpiritualRootAttribute");
        if (!stored.isBlank()) {
            for (String token : stored.split(",")) {
                try {
                    spiritualRootAttributes.add(SpiritualRootAttribute.valueOf(token.trim().toUpperCase(Locale.ROOT)));
                } catch (Exception ignored) {
                    // Ignore invalid legacy/custom tokens and fall back below when empty.
                }
            }
        }
        if (spiritualRootAttributes.isEmpty()) {
            spiritualRootAttributes.add(SpiritualRootAttribute.WOOD);
        }
    }
}
