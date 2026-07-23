package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.skill.CultivationSkill;
import com.xunxian.seekingimmortals.quest.QuestProgress;
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
    private static final int INITIAL_MANA = 50;
    private static final int INITIAL_DIVINE_CONSCIOUSNESS = 3;
    private static final int MAX_QI_DEVIATION_RISK = 100;
    private static final int MAX_TRIBULATION_RESISTANCE = 90;
    private static final double WORLDPACK_SPIRIT_RAIN_CULTIVATION_MULTIPLIER = 1.10D;
    private static final double WORLDPACK_SPIRIT_VEIN_PULSE_CULTIVATION_MULTIPLIER = 1.20D;

    private int spiritualPower = INITIAL_MANA;
    private int divineConsciousness = INITIAL_DIVINE_CONSCIOUSNESS;
    private int bodyRefinement = 0;
    private int qiDeviationRisk = 0;
    private int tribulationResistance = 0;
    private boolean tribulationActive = false;
    private Realm tribulationTargetRealm = Realm.MORTAL;
    private int tribulationCurrentStrike = 0;
    private int tribulationTotalStrikes = 0;
    private int tribulationNextStrikeTicks = 0;
    private GoldCoreGrade goldCoreGrade = GoldCoreGrade.NONE;
    private int goldCoreScore = 0;
    private Realm realm = Realm.MORTAL;
    private RealmStage stage = RealmStage.MORTAL;
    private int cultivationExp = 0;
    private boolean breakthroughAssisted = false;
    private double breakthroughPillBonus = 0.0D;
    private boolean meditating = false;
    private SpiritualRoot spiritualRoot = SpiritualRoot.TRIPLE;
    private final EnumSet<SpiritualRootAttribute> spiritualRootAttributes = EnumSet.of(SpiritualRootAttribute.WOOD, SpiritualRootAttribute.WATER, SpiritualRootAttribute.FIRE);
    private SpecialPhysique specialPhysique = SpecialPhysique.NONE;
    /** 语料 constitution_catalog id；空表示沿用 specialPhysique 映射。 */
    private String constitutionId = "none";
    /** 修炼路线：orthodox / ghost_cultivator 等。 */
    private String cultivationPathId = PathRaceCatalog.DEFAULT_PATH_ID;
    /** 可玩种族：playable_races.json id。 */
    private String playableRaceId = PathRaceCatalog.DEFAULT_RACE_ID;
    /** 鬼修路线阶段 id；非鬼道为空。 */
    private String ghostPathStageId = "";
    private int lifespanYears = Realm.MORTAL.getLifespanYears();
    private int ageYears = 16;
    private boolean usedReturnYangTrueWater = false;
    private int failedBreakthroughs = 0;
    private boolean rootInitialized = false;
    private int spiritualRootPurity = 50;
    private boolean spiritualRootAwakened = true;
    private boolean spiritualRootTested = false;
    private boolean mysticVialGranted = false;
    private final QuestProgress sevenMysteriesQuest = new QuestProgress();
    private boolean severeInjury = false;
    private int heartDemonLevel = 0;
    private int heartDemonTriggerTicks = 0;
    private boolean deathSubstituteReady = false;
    private boolean shatteredCore = false;
    private int realmFallScars = 0;
    private final Set<String> learnedTechniques = new HashSet<>();
    private final List<String> techniqueSlots = new ArrayList<>();
    private final Map<String, Long> techniqueCooldownUntilTicks = new HashMap<>();
    private final Map<SkillType, CultivationSkill> skills = new HashMap<>();
    private double meditationCultivationProgress = 0.0D;
    private int cultivationBoostTicks = 0;
    private double cultivationBoostMultiplier = 1.0D;
    /**
     * Authoritative multiplier owned by the currently active authored daily event.
     * It is deliberately separate from pill/consumable cultivation boosts.
     */
    private double worldpackDailyCultivationMultiplier = 1.0D;
    private double movementSpeedScale = 1.0D;
    private String worldpackCurrentRegionId = "qinglan_mountains";
    private String worldpackActiveSecretRealmId = "";
    private boolean worldpackHasReturnLocation = false;
    private String worldpackReturnDimension = "";
    private double worldpackReturnX = 0.0D;
    private double worldpackReturnY = 64.0D;
    private double worldpackReturnZ = 0.0D;
    private float worldpackReturnYRot = 0.0F;
    private float worldpackReturnXRot = 0.0F;
    private final Map<String, Long> worldpackCooldownUntilTicks = new HashMap<>();
    private String worldpackActiveDailyEventId = "";
    private long worldpackActiveDailyEventUntilTick = 0L;
    // M3: 走火风险衰减累计 tick 计数器（非取模，保证稳定触发）
    private int qiDevDecayAccumulatorTicks = 0;
    private int leylineQiDevDecayAccumulatorTicks = 0;

    // tickQiDeviationDecay 每秒调用一次（ModEvents 中 tickCount % 20 门控），
    // 因此累加器以“秒”为单位，阈值直接用秒数，不再乘 20。
    private static final int QI_DEV_RISK_DECAY_TICKS = 720;     // 平稳打坐每 720 秒 -1
    private static final int LEYLINE_RISK_DECAY_TICKS = 360;    // 灵脉额外每 360 秒 -1

    public PlayerCultivation() {
        clearTechniqueSlots();
    }

    public int getSpiritualPower() { return spiritualPower; }
    public int getMana() { return getSpiritualPower(); }
    public int getQi() { return getSpiritualPower(); }
    public int getCultivation() { return getCultivationExp(); }
    public int getDivineConsciousness() {
        if (hasSkill(SkillType.DIVINE_SENSE_EXPANSION)) {
            return Math.max(divineConsciousness, Math.round(divineConsciousness * 1.5F));
        }
        return divineConsciousness;
    }
    public int getDivSense() { return getDivineConsciousness(); }
    public int getBodyRefinement() { return bodyRefinement; }
    public int getBodyRef() { return getBodyRefinement(); }
    public int getQiDeviationRisk() { return qiDeviationRisk; }
    public int getQiDevRisk() { return getQiDeviationRisk(); }
    public int getTribulationResistance() { return tribulationResistance; }
    public int getTribRes() { return getTribulationResistance(); }
    public boolean isTribulationActive() { return tribulationActive; }
    public boolean isInTribulation() { return tribulationActive; }
    public Realm getTribulationTargetRealm() { return tribulationTargetRealm; }
    public int getTribulationCurrentStrike() { return tribulationCurrentStrike; }
    public int getTribulationTotalStrikes() { return tribulationTotalStrikes; }
    public int getTribulationNextStrikeTicks() { return tribulationNextStrikeTicks; }
    public GoldCoreGrade getGoldCoreGrade() { return goldCoreGrade; }
    public String getGoldCoreGradeName() { return goldCoreGrade.getDisplayName(); }
    public int getGoldCoreScore() { return goldCoreScore; }
    public boolean hasCompleteFiveElements() {
        return spiritualRootAttributes.contains(SpiritualRootAttribute.METAL)
                && spiritualRootAttributes.contains(SpiritualRootAttribute.WOOD)
                && spiritualRootAttributes.contains(SpiritualRootAttribute.WATER)
                && spiritualRootAttributes.contains(SpiritualRootAttribute.FIRE)
                && spiritualRootAttributes.contains(SpiritualRootAttribute.EARTH);
    }

    public long getCultivationLong() { return cultivationExp; }
    public long getCultivationMax() { return getCurrentStageCapExp(); }
    public int getCultivationMaxInt() { return getCurrentStageCapExp(); }
    public long getManaMaxLong() { return getMaxSpiritualPower(); }
    public float getQiDevRiskFloat() { return qiDeviationRisk / 100.0F; }
    public float getQiDevRiskPercent() { return qiDeviationRisk; }
    public float getTribResFloat() { return tribulationResistance / 100.0F; }
    public float getTribResPercent() { return tribulationResistance; }
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
    public Realm getNextBreakthroughRealm() {
        if (isAtFinalStage()) return realm;
        RealmStage[] stages = getStagesForRealm(realm);
        for (int i = 0; i < stages.length; i++) {
            if (stages[i] != stage) continue;
            return i + 1 < stages.length ? realm : realm.next();
        }
        return realm;
    }
    public RealmStage getNextBreakthroughStage() {
        if (isAtFinalStage()) return stage;
        RealmStage[] stages = getStagesForRealm(realm);
        for (int i = 0; i < stages.length; i++) {
            if (stages[i] != stage) continue;
            if (i + 1 < stages.length) return stages[i + 1];
            Realm nextRealm = realm.next();
            return getStagesForRealm(nextRealm)[0];
        }
        return stage;
    }
    public boolean isMeditating() { return meditating; }
    public int getCultivationBoostTicks() { return cultivationBoostTicks; }
    public double getCultivationBoostMultiplier() { return cultivationBoostTicks > 0 ? cultivationBoostMultiplier : 1.0D; }
    public double getWorldpackDailyCultivationMultiplier() {
        return clampDailyCultivationMultiplier(worldpackDailyCultivationMultiplier);
    }
    public SpiritualRoot getSpiritualRoot() { return spiritualRoot; }
    public Set<SpiritualRootAttribute> getSpiritualRootAttributes() { return EnumSet.copyOf(spiritualRootAttributes); }
    public SpiritualRootAttribute getSpiritualRootAttribute() { return spiritualRootAttributes.iterator().next(); }
    public SpecialPhysique getSpecialPhysique() { return specialPhysique; }
    public String getConstitutionId() {
        if (constitutionId != null && !constitutionId.isBlank() && !"none".equalsIgnoreCase(constitutionId)) {
            return constitutionId;
        }
        return SpecialPhysique.toConstitutionId(specialPhysique);
    }
    public String getCultivationPathId() { return cultivationPathId == null || cultivationPathId.isBlank() ? PathRaceCatalog.DEFAULT_PATH_ID : cultivationPathId; }
    public String getPlayableRaceId() { return playableRaceId == null || playableRaceId.isBlank() ? PathRaceCatalog.DEFAULT_RACE_ID : playableRaceId; }
    public String getGhostPathStageId() { return ghostPathStageId == null ? "" : ghostPathStageId; }
    public boolean isGhostPath() { return PathRaceCatalog.builtin().isGhostPath(getCultivationPathId()); }
    public int getSpiritualRootPurity() { return spiritualRootPurity; }
    public boolean isSpiritualRootAwakened() { return spiritualRootAwakened; }
    public boolean isSpiritualRootTested() { return spiritualRootTested; }
    public boolean isMysticVialGranted() { return mysticVialGranted; }
    public void setMysticVialGranted(boolean granted) { this.mysticVialGranted = granted; }
    public QuestProgress getSevenMysteriesQuest() { return sevenMysteriesQuest; }
    public void resetSevenMysteriesQuest() { sevenMysteriesQuest.loadNBT(new CompoundTag()); }
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

    public boolean hasAlchemy() {
        return hasSkill(SkillType.ALCHEMY);
    }

    public Map<SkillType, CultivationSkill> getAllSkills() {
        return Map.copyOf(skills);
    }

    public boolean canLearnSkill(SkillType skillType) {
        if (!hasReachedSkillRequirement(skillType)) return false;
        if (skillType.isAutoTechniqueSkill()) return true;
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

    public boolean unlockSkillForQuest(SkillType skillType) {
        if (skillType == null) return false;
        CultivationSkill skill = skills.computeIfAbsent(skillType, CultivationSkill::new);
        if (skill.isUnlocked()) return false;
        skill.unlock();
        return true;
    }

    public List<SkillType> unlockEligiblePhase4Skills() {
        return unlockEligibleTechniqueSkills();
    }

    public List<SkillType> unlockEligibleTechniqueSkills() {
        List<SkillType> unlocked = new ArrayList<>();
        for (SkillType skillType : SkillType.values()) {
            if (!skillType.isAutoTechniqueSkill()) continue;
            if (unlockSkill(skillType)) {
                unlocked.add(skillType);
                learnTechnique(skillType.getTechniqueId());
            } else if (hasSkill(skillType) && !hasLearnedTechnique(skillType.getTechniqueId())) {
                learnTechnique(skillType.getTechniqueId());
            }
        }
        return unlocked;
    }

    private boolean hasReachedSkillRequirement(SkillType skillType) {
        Realm requiredRealm = skillType.getRequiredRealm();
        if (realm.ordinal() < requiredRealm.ordinal()) return false;
        RealmStage requiredStage = skillType.getRequiredStage();
        if (requiredStage == null || realm.ordinal() > requiredRealm.ordinal()) return true;
        if (realm != requiredRealm) return false;
        RealmStage[] stages = getStagesForRealm(requiredRealm);
        int currentIndex = -1;
        int requiredIndex = -1;
        for (int i = 0; i < stages.length; i++) {
            if (stages[i] == stage) currentIndex = i;
            if (stages[i] == requiredStage) requiredIndex = i;
        }
        return currentIndex >= 0 && requiredIndex >= 0 && currentIndex >= requiredIndex;
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
    public boolean hasUsedReturnYangTrueWater() { return usedReturnYangTrueWater; }
    public void setUsedReturnYangTrueWater(boolean used) { usedReturnYangTrueWater = used; }
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
    public void setMeditating(boolean meditating) {
        if (!meditating) {
            meditationCultivationProgress = 0.0D;
        }
        this.meditating = meditating;
    }
    public void setSpiritualRoot(SpiritualRoot spiritualRoot) { this.spiritualRoot = spiritualRoot; this.rootInitialized = true; }
    public void setSpiritualRootAttribute(SpiritualRootAttribute attribute) {
        this.spiritualRootAttributes.clear();
        this.spiritualRootAttributes.add(attribute);
        this.rootInitialized = true;
    }
    public void setSpecialPhysique(SpecialPhysique specialPhysique) {
        this.specialPhysique = specialPhysique == null ? SpecialPhysique.NONE : specialPhysique;
        this.constitutionId = SpecialPhysique.toConstitutionId(this.specialPhysique);
    }

    public void setConstitutionId(String constitutionId) {
        String cleaned = constitutionId == null || constitutionId.isBlank() ? "none" : constitutionId.trim().toLowerCase(Locale.ROOT);
        this.constitutionId = cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
        this.specialPhysique = SpecialPhysique.fromConstitutionId(this.constitutionId);
    }

    public void setCultivationPathId(String pathId) {
        this.cultivationPathId = PathRaceCatalog.sanitizePathId(pathId);
        if (!isGhostPath()) {
            this.ghostPathStageId = "";
        }
    }

    public void setPlayableRaceId(String raceId) {
        this.playableRaceId = PathRaceCatalog.sanitizeRaceId(raceId);
    }

    public void setGhostPathStageId(String stageId) {
        this.ghostPathStageId = PathRaceCatalog.sanitizeGhostStageId(stageId);
        if (this.ghostPathStageId != null && !this.ghostPathStageId.isBlank()) {
            this.cultivationPathId = PathRaceCatalog.GHOST_PATH_ID;
        }
    }
    public void setGoldCore(GoldCoreGrade grade, int score) {
        goldCoreGrade = grade == null ? GoldCoreGrade.NONE : grade;
        goldCoreScore = Math.max(0, score);
    }

    public void clearGoldCore() {
        setGoldCore(GoldCoreGrade.NONE, 0);
    }

    public boolean formGoldCoreIfAbsent(int score) {
        if (goldCoreGrade != GoldCoreGrade.NONE) return false;
        setGoldCore(GoldCoreGrade.fromScore(score), score);
        return true;
    }

    public boolean createLingGenIfAbsent(RandomSource random) {
        if (spiritualRootTested) return false;
        applyLingGenResult(LingGenCalculator.roll(random, 0.0D));
        return true;
    }

    public void applySevereInjury() {
        severeInjury = true;
    }

    public void clearSevereInjuryIfRecovered() {
        int max = getMaxSpiritualPower();
        if (severeInjury && max > 0 && spiritualPower >= max) {
            severeInjury = false;
        }
    }

    public double getSpiritualPowerRecoveryMultiplier() {
        double base = severeInjury ? 0.60D : 1.0D;
        return base * spiritualRoot.getQiRecoveryMultiplier();
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
            case YIN -> 1.15D;
            case YANG -> 1.15D;
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

    public boolean reduceHeartDemon(int layers) {
        if (layers <= 0 || heartDemonLevel <= 0) return false;
        heartDemonLevel = Math.max(0, heartDemonLevel - layers);
        if (heartDemonLevel == 0) {
            heartDemonTriggerTicks = 0;
        }
        return true;
    }

    public boolean hasDeathSubstituteReady() {
        return deathSubstituteReady;
    }

    public boolean grantDeathSubstitute() {
        if (deathSubstituteReady) return false;
        deathSubstituteReady = true;
        return true;
    }

    public boolean consumeDeathSubstitute() {
        if (!deathSubstituteReady) return false;
        deathSubstituteReady = false;
        return true;
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

    private void fallOneStageForTribulation() {
        Realm previousRealm = Realm.MORTAL;
        RealmStage previousStage = RealmStage.MORTAL;
        int previousStart = 0;
        int start = 0;
        for (Realm candidateRealm : Realm.values()) {
            for (RealmStage candidateStage : getStagesForRealm(candidateRealm)) {
                if (candidateRealm == realm && candidateStage == stage) {
                    realm = previousRealm;
                    stage = previousStage;
                    cultivationExp = previousStart;
                    lifespanYears = Math.max(lifespanYears, realm.getLifespanYears());
                    return;
                }
                previousRealm = candidateRealm;
                previousStage = candidateStage;
                previousStart = start;
                start += candidateRealm.getStageExpSpan();
            }
        }
    }

    public void applyLingGenResult(LingGenCalculator.Result result) {
        spiritualRoot = result.root();
        spiritualRootAttributes.clear();
        spiritualRootAttributes.addAll(result.attributes());
        spiritualRootPurity = (int) Math.round(spiritualRoot.getAttributeStrengthMultiplier() * 100.0D);
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
        // 与 spirit_roots_catalog 权重对齐
        LingGenCalculator.Result result = LingGenCalculator.roll(random, 0.0D);
        applyLingGenResult(result);
        SpecialPhysique rolled = SpecialPhysique.random(random);
        setSpecialPhysique(rolled);
        if (playableRaceId == null || playableRaceId.isBlank()) {
            playableRaceId = PathRaceCatalog.DEFAULT_RACE_ID;
        }
        if (cultivationPathId == null || cultivationPathId.isBlank()) {
            cultivationPathId = PathRaceCatalog.DEFAULT_PATH_ID;
        }
        rootInitialized = true;
    }

    private SpiritualRoot randomRoot(RandomSource random) {
        return LingGenCalculator.roll(random, 0.0D).root();
    }

    private List<SpiritualRootAttribute> randomAttributes(RandomSource random, SpiritualRoot root) {
        return new ArrayList<>(LingGenCalculator.roll(random, 0.0D).attributes());
    }

    public String getSpiritualRootAttributeNames() {
        return spiritualRootAttributes.stream()
                .map(SpiritualRootAttribute::getDisplayName)
                .collect(Collectors.joining("/"));
    }

    public double getSpiritualRootCultivationSpeedCoefficient() {
        return spiritualRoot.getCultivationSpeedCoefficient();
    }

    /** 丹药效果吸收倍率（低资质灵根更高） */
    public double getPillAbsorptionMultiplier() {
        return spiritualRoot.getPillAbsorptionMultiplier();
    }

    /** 青玉小瓶额外获取概率（预留接口） */
    public double getJadeVialDropChance() {
        return spiritualRoot.getJadeVialDropChance();
    }

    /** 灵根属性强度倍数（简化纯度后，基于灵根分类） */
    public double getAttributeStrengthMultiplier() {
        return spiritualRoot.getAttributeStrengthMultiplier();
    }

    public double getPhysiqueCultivationSpeedMultiplier() {
        return ConstitutionCatalogService.cultivationMultiplier(getConstitutionId());
    }

    public double getCultivationSpeedMultiplier() {
        // 运行时灵根分类倍率（天灵根 5.0 等）保留既有身份；
        // 语料 stack_cap x2.5 作用于目录品阶×体质叠乘，见 ConstitutionCatalogService.clampStackedCultivation。
        return Math.max(0.05D, getSpiritualRootCultivationSpeedCoefficient() * getPhysiqueCultivationSpeedMultiplier());
    }

    public double getGoldCoreAttributeMultiplier() {
        if (shatteredCore || realm.ordinal() < Realm.CORE_FORMATION.ordinal() || !goldCoreGrade.isFormed()) {
            return 1.0D;
        }
        return goldCoreGrade.getAttributeMultiplier();
    }

    public double getAdvancedBreakthroughBonus() {
        return getAdvancedBreakthroughBonus(getNextBreakthroughRealm());
    }

    public double getAdvancedBreakthroughBonus(Realm targetRealm) {
        if (targetRealm == null || targetRealm.ordinal() < Realm.CORE_FORMATION.ordinal()) return 0.0D;
        double bonus = 0.0D;
        if (targetRealm.ordinal() >= Realm.VOID_REFINEMENT.ordinal() && hasCompleteFiveElements()) {
            bonus += 0.08D;
        }
        if (spiritualRoot == SpiritualRoot.HEAVENLY || spiritualRoot == SpiritualRoot.MUTATED || spiritualRoot == SpiritualRoot.HIDDEN) {
            bonus += targetRealm.ordinal() >= Realm.SOUL_TRANSFORMATION.ordinal() ? 0.05D : 0.03D;
        }
        if (goldCoreGrade == GoldCoreGrade.HIGH) {
            bonus += 0.02D;
        } else if (goldCoreGrade == GoldCoreGrade.PERFECT) {
            bonus += 0.04D;
        }
        return bonus;
    }

    public double getMovementSpeedScale() {
        return movementSpeedScale;
    }

    public void setMovementSpeedScale(double scale) {
        movementSpeedScale = Math.max(0.0D, Math.min(1.0D, scale));
    }

    public String getWorldpackCurrentRegionId() {
        return worldpackCurrentRegionId == null || worldpackCurrentRegionId.isBlank()
                ? "qinglan_mountains"
                : worldpackCurrentRegionId;
    }

    public void setWorldpackCurrentRegionId(String regionId) {
        String normalized = cleanWorldpackId(regionId, "qinglan_mountains");
        if (!normalized.equals(getWorldpackCurrentRegionId())) {
            clearWorldpackDailyEvent();
        }
        worldpackCurrentRegionId = normalized;
    }

    public String getWorldpackActiveSecretRealmId() {
        return worldpackActiveSecretRealmId == null ? "" : worldpackActiveSecretRealmId;
    }

    public void setWorldpackActiveSecretRealmId(String realmId) {
        worldpackActiveSecretRealmId = cleanWorldpackId(realmId, "");
    }

    public boolean hasWorldpackReturnLocation() {
        return worldpackHasReturnLocation && worldpackReturnDimension != null && !worldpackReturnDimension.isBlank();
    }

    public String getWorldpackReturnDimension() {
        return worldpackReturnDimension == null ? "" : worldpackReturnDimension;
    }

    public double getWorldpackReturnX() {
        return worldpackReturnX;
    }

    public double getWorldpackReturnY() {
        return worldpackReturnY;
    }

    public double getWorldpackReturnZ() {
        return worldpackReturnZ;
    }

    public float getWorldpackReturnYRot() {
        return worldpackReturnYRot;
    }

    public float getWorldpackReturnXRot() {
        return worldpackReturnXRot;
    }

    public void setWorldpackReturnLocation(String dimension, double x, double y, double z, float yRot, float xRot) {
        worldpackHasReturnLocation = dimension != null && !dimension.isBlank();
        worldpackReturnDimension = dimension == null ? "" : dimension;
        worldpackReturnX = x;
        worldpackReturnY = y;
        worldpackReturnZ = z;
        worldpackReturnYRot = yRot;
        worldpackReturnXRot = xRot;
    }

    public void clearWorldpackReturnLocation() {
        worldpackHasReturnLocation = false;
        worldpackReturnDimension = "";
    }

    public long getWorldpackCooldownUntil(String realmId) {
        return worldpackCooldownUntilTicks.getOrDefault(cleanWorldpackId(realmId, ""), 0L);
    }

    public void setWorldpackCooldownUntil(String realmId, long untilTick) {
        String id = cleanWorldpackId(realmId, "");
        if (id.isBlank() || untilTick <= 0L) {
            worldpackCooldownUntilTicks.remove(id);
            return;
        }
        worldpackCooldownUntilTicks.put(id, untilTick);
    }

    public Map<String, Long> getWorldpackCooldowns() {
        return Map.copyOf(worldpackCooldownUntilTicks);
    }

    public String getWorldpackActiveDailyEventId() {
        return worldpackActiveDailyEventId == null ? "" : worldpackActiveDailyEventId;
    }

    public long getWorldpackActiveDailyEventUntilTick() {
        return worldpackActiveDailyEventUntilTick;
    }

    public void setWorldpackDailyEvent(String eventId, long untilTick) {
        String normalized = cleanWorldpackId(eventId, "");
        long normalizedUntil = Math.max(0L, untilTick);
        if (normalized.isBlank()) {
            clearWorldpackDailyEvent();
            return;
        }
        boolean changed = !normalized.equals(worldpackActiveDailyEventId)
                || normalizedUntil != worldpackActiveDailyEventUntilTick;
        worldpackActiveDailyEventId = normalized;
        worldpackActiveDailyEventUntilTick = normalizedUntil;
        // Preserve the legacy two-event behavior for old callers. When the
        // event identity/expiry is unchanged, retain the exact typed value
        // installed by DailyEventEffectExecutor.
        if (changed) {
            worldpackDailyCultivationMultiplier = getWorldpackDailyCultivationMultiplier(normalized);
        }
    }

    /** Clears the event identity, deadline, and only the event-owned multiplier. */
    public void clearWorldpackDailyEvent() {
        worldpackActiveDailyEventId = "";
        worldpackActiveDailyEventUntilTick = 0L;
        worldpackDailyCultivationMultiplier = 1.0D;
    }

    /** Sets the multiplier owned by the active authored daily event. */
    public void setWorldpackDailyCultivationMultiplier(double multiplier) {
        worldpackDailyCultivationMultiplier = clampDailyCultivationMultiplier(multiplier);
    }

    /** Clears only the daily-event multiplier; consumable boosts remain intact. */
    public void clearWorldpackDailyCultivationMultiplier() {
        worldpackDailyCultivationMultiplier = 1.0D;
    }

    public double getMeleeAttackPower() {
        double base = RealmStageConfig.getAttackBase(realm) * stage.getMaxSpiritualPowerMultiplier();
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
        return base * attributeMultiplier * getPhysiqueAttackMultiplier() * getGoldCoreAttributeMultiplier();
    }

    public double getMagicAttackPower() {
        return getMeleeAttackPower() * 1.2D;
    }

    public double getDefensePower() {
        double base = RealmStageConfig.getDefenseBase(realm) * stage.getMaxSpiritualPowerMultiplier();
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
        double bodyBonus = Math.sqrt(Math.max(0, bodyRefinement)) * (3.0D + RealmStageConfig.getRealmGrowthIndex(realm) * 2.0D);
        return (base * attributeMultiplier * getPhysiqueDefenseMultiplier() + bodyBonus) * getGoldCoreAttributeMultiplier();
    }

    public double getDodgeRate() {
        double base = RealmStageConfig.getDodgeChanceBase(realm);
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
        return Math.min(0.75D, base + attributeBonus + getPhysiqueDodgeBonus());
    }

    public double getCriticalRate() {
        double base = RealmStageConfig.getCritChanceBase(realm);
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
        return Math.min(0.80D, base + attributeBonus + getPhysiqueCritBonus());
    }

    public double getMagicResistance() {
        double base = RealmStageConfig.getMagicResistanceBase(realm);
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
        return Math.min(0.85D, base + attributeBonus + getPhysiqueMagicResistanceBonus());
    }

    public double getTribulationDamageReductionBonus() {
        double attributeBonus = spiritualRootAttributes.stream()
                .mapToDouble(attr -> switch (attr) {
                    case THUNDER, HIDDEN_THUNDER -> 0.08D;
                    case ICE, WATER, YIN -> 0.03D;
                    case EARTH, METAL, YANG -> 0.02D;
                    default -> 0.0D;
                })
                .max()
                .orElse(0.0D);
        double physiqueBonus = switch (specialPhysique) {
            case FIVE_THUNDER_BODY -> 0.12D;
            case HIDDEN_THUNDER_ROOT -> 0.10D;
            case HEAVENLY_YIN_BODY, MYSTIC_YIN_BODY -> 0.08D;
            case CHASTE_YIN_BODY, NINE_SPIRIT_SWORD_BODY, UNDYING_BODY -> 0.06D;
            case GOLD_FORGING_BODY, MOLTEN_GOLD_BODY, ICE_MARROW_BODY, VAJRA_BODY -> 0.04D;
            default -> 0.0D;
        };
        // constitution_catalog.bonus.thunder_tribulation 为伤害修正（负值=减伤）
        double catalogDelta = ConstitutionCatalogService.tribulationDamageDelta(getConstitutionId());
        double catalogBonus = catalogDelta < 0.0D ? -catalogDelta : 0.0D;
        return Math.min(0.20D, attributeBonus + physiqueBonus + catalogBonus + getMagicResistance() * 0.10D);
    }

    public double getAccuracyRate() {
        double base = RealmStageConfig.getAccuracyBase(realm);
        double attributeBonus = spiritualRootAttributes.stream()
                .mapToDouble(attr -> switch (attr) {
                    case THUNDER, HIDDEN_THUNDER -> 0.04D;
                    case METAL, WIND -> 0.03D;
                    case FIRE, DARK, HIDDEN_DARK -> 0.02D;
                    default -> 0.0D;
                })
                .max()
                .orElse(0.0D);
        double physiqueBonus = switch (specialPhysique) {
            case NINE_SPIRIT_SWORD_BODY -> 0.06D;
            case FIVE_THUNDER_BODY, GOLD_FORGING_BODY -> 0.04D;
            case HIDDEN_THUNDER_ROOT -> 0.03D;
            default -> 0.0D;
        };
        return Math.min(0.99D, base + attributeBonus + physiqueBonus);
    }

    private double getPhysiqueAttackMultiplier() {
        return switch (specialPhysique) {
            case FIVE_THUNDER_BODY -> 1.12D;
            case NINE_SPIRIT_SWORD_BODY -> 1.15D;
            case GOLD_FORGING_BODY, MOLTEN_GOLD_BODY -> 1.08D;
            case HIDDEN_THUNDER_ROOT, THREE_YANG_BODY -> 1.06D;
            case DRAGON_CHANT_BODY -> realm.ordinal() < Realm.CORE_FORMATION.ordinal() ? 0.95D : 1.08D;
            default -> 1.0D;
        };
    }

    private double getPhysiqueDefenseMultiplier() {
        return switch (specialPhysique) {
            case MOLTEN_GOLD_BODY -> 1.10D;
            case GOLD_FORGING_BODY, ICE_MARROW_BODY, HEAVENLY_YIN_BODY, MYSTIC_YIN_BODY -> 1.08D;
            case CHASTE_YIN_BODY, SEVEN_STAR_MOON_BODY, FIVE_THUNDER_BODY -> 1.05D;
            default -> 1.0D;
        };
    }

    private double getPhysiqueCritBonus() {
        return switch (specialPhysique) {
            case NINE_SPIRIT_SWORD_BODY -> 0.08D;
            case FIVE_THUNDER_BODY -> 0.06D;
            case GOLD_FORGING_BODY, MOLTEN_GOLD_BODY -> 0.04D;
            default -> 0.0D;
        };
    }

    private double getPhysiqueDodgeBonus() {
        return switch (specialPhysique) {
            case SEVEN_STAR_MOON_BODY, CHARMING_BODY -> 0.04D;
            case HIDDEN_THUNDER_ROOT, FIVE_THUNDER_BODY -> 0.03D;
            default -> 0.0D;
        };
    }

    private double getPhysiqueMagicResistanceBonus() {
        return switch (specialPhysique) {
            case HEAVENLY_YIN_BODY, MYSTIC_YIN_BODY -> 0.08D;
            case CHASTE_YIN_BODY, ICE_MARROW_BODY, FIVE_THUNDER_BODY -> 0.06D;
            case HIDDEN_THUNDER_ROOT -> 0.05D;
            default -> 0.0D;
        };
    }

    /**
     * 突破综合倍率（体质 × 属性，不含灵根加法加成）。
     * 灵根加成改为加法，在 getBreakthroughChanceBreakdown 中累加。
     */
    public double getBreakthroughMultiplier() {
        double attributeMultiplier = spiritualRootAttributes.stream()
                .mapToDouble(SpiritualRootAttribute::getBreakthroughCoefficient)
                .average()
                .orElse(1.0D);
        return attributeMultiplier * ConstitutionCatalogService.breakthroughMultiplier(getConstitutionId());
    }

    private static final int MORTAL_MIN_SPIRITUAL_POWER = 50;

    public int getMaxSpiritualPower() {
        int adjusted = saturatedPositiveInt((double) realm.getBaseMaxSpiritualPower()
                * stage.getMaxSpiritualPowerMultiplier()
                * getGoldCoreAttributeMultiplier());
        return realm == Realm.MORTAL ? Math.max(MORTAL_MIN_SPIRITUAL_POWER, adjusted) : adjusted;
    }

    public int getManaMax() { return getMaxSpiritualPower(); }
    public int getMaxQi() { return getMaxSpiritualPower(); }

    public int getMaxDivineConsciousness() {
        int base = RealmStageConfig.getDivSenseBase(realm);
        return saturatedPositiveInt((double) base
                * stage.getMaxSpiritualPowerMultiplier()
                * getGoldCoreAttributeMultiplier());
    }

    // ========== Phase 1: 衍生属性计算方法 ==========

    /**
     * 获取最大生命值（HP）
     * <p>公式: hpBase × 阶段倍率</p>
     */
    public int getMaxHealthPoints() {
        int base = RealmStageConfig.getHpBase(realm);
        double realmHealth = (double) base * stage.getMaxSpiritualPowerMultiplier();
        double bodyBonus = Math.sqrt(Math.max(0, bodyRefinement))
                * (4.0D + RealmStageConfig.getRealmGrowthIndex(realm) * 1.5D);
        return saturatedPositiveInt((realmHealth + bodyBonus) * getGoldCoreAttributeMultiplier());
    }

    private static int saturatedPositiveInt(double value) {
        if (Double.isNaN(value) || value <= 1.0D) {
            return 1;
        }
        if (!Double.isFinite(value) || value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.round(value));
    }

    /**
     * 获取灵力回复速度（点/秒）
     * <p>公式: 境界基准 × 灵根回复系数 × 重伤惩罚</p>
     */
    public float getManaRecoveryPerSecond() {
        float base = RealmStageConfig.getManaRecoveryBase(realm);
        double recoveryMultiplier = getSpiritualPowerRecoveryMultiplier();
        return base * (float) recoveryMultiplier;
    }

    /**
     * 获取修为增长速度（点/秒，打坐状态下）
     * <p>公式: 境界基准 × 灵根修炼速度系数 × 体质倍率</p>
     */
    public float getCultivationGainPerSecond() {
        float base = RealmStageConfig.getCultivationGainBase(realm);
        double speedMultiplier = getCultivationSpeedMultiplier();
        return base * (float) (speedMultiplier * getGoldCoreAttributeMultiplier());
    }

    /**
     * 获取飞行速度（方块/秒）
     * <p>公式: 境界基准 × 阶段加成</p>
     */
    public float getFlyingSpeed() {
        float base = RealmStageConfig.getFlyingSpeedBase(realm);
        // 阶段倍率简化：初期1.0，中期1.2，后期1.5，圆满1.8
        return base * stage.getMaxSpiritualPowerMultiplier();
    }

    public double getMovementSpeedBonus() {
        return Math.max(0.0D, getFlyingSpeed() * 0.002D);
    }

    public double getEffectiveMovementSpeedBonus() {
        return getMovementSpeedBonus() * movementSpeedScale;
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

    public void setManaMax(int ignoredDynamicMax) { setMana(spiritualPower); }

    public void setSpiritualPower(int amount) { setMana(amount); }

    public void addQi(int amount) { addSpiritualPower(amount); }

    public void restoreHalfManaFromPill() {
        addSpiritualPower((int)Math.ceil(getMaxSpiritualPower() * 0.5D));
    }

    public void addDivineConsciousness(int amount) {
        divineConsciousness = Math.max(0, Math.min(getMaxDivineConsciousness(), divineConsciousness + amount));
    }

    public void addDivSense(int amount) { addDivineConsciousness(amount); }

    public void setDivSense(int amount) {
        divineConsciousness = Math.max(0, Math.min(getMaxDivineConsciousness(), amount));
    }

    public void setDivineConsciousness(int amount) { setDivSense(amount); }

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

    /** M3: 平稳打坐每 tick 累加，达阈值 -1 走火风险；risk=0 时重置累加器。 */
    public void tickQiDeviationDecay(boolean leyline) {
        if (qiDeviationRisk <= 0) {
            qiDevDecayAccumulatorTicks = 0;
            leylineQiDevDecayAccumulatorTicks = 0;
            return;
        }
        if (++qiDevDecayAccumulatorTicks >= QI_DEV_RISK_DECAY_TICKS) {
            addQiDeviationRisk(-1);
            qiDevDecayAccumulatorTicks = 0;
        }
        if (leyline && ++leylineQiDevDecayAccumulatorTicks >= LEYLINE_RISK_DECAY_TICKS) {
            addQiDeviationRisk(-1);
            leylineQiDevDecayAccumulatorTicks = 0;
        }
    }

    private static final int MAX_CULTIVATION_BOOST_TICKS = 72000 * 2; // 2 * 鍑濊仛涓?boost ticks

    public void addCultivationBoost(int ticks, double multiplier) {
        cultivationBoostTicks = Math.min(cultivationBoostTicks + ticks, MAX_CULTIVATION_BOOST_TICKS);
        cultivationBoostMultiplier = Math.max(cultivationBoostMultiplier, multiplier);
    }

    public void tickCultivationBoost() {
        if (cultivationBoostTicks <= 0) {
            cultivationBoostMultiplier = 1.0D;
            return;
        }
        cultivationBoostTicks--;
        if (cultivationBoostTicks <= 0) {
            cultivationBoostMultiplier = 1.0D;
        }
    }

    public void setQiDeviationRisk(int amount) {
        qiDeviationRisk = clamp(amount, 0, MAX_QI_DEVIATION_RISK);
    }

    public void setQiDeviationRisk(float value) { setQiDeviationRisk(normalizeRiskPercent(value, MAX_QI_DEVIATION_RISK)); }
    public void setQiDevRisk(int amount) { setQiDeviationRisk(amount); }
    public void setQiDevRisk(float value) { setQiDeviationRisk(value); }

    public void addTribulationResistance(int amount) {
        tribulationResistance = clamp(tribulationResistance + amount, 0, MAX_TRIBULATION_RESISTANCE);
    }

    public void setTribulationResistance(int amount) {
        tribulationResistance = clamp(amount, 0, MAX_TRIBULATION_RESISTANCE);
    }

    public void setTribulationResistance(float value) { setTribulationResistance(normalizeRiskPercent(value, MAX_TRIBULATION_RESISTANCE)); }
    public void setTribRes(int amount) { setTribulationResistance(amount); }
    public void setTribRes(float value) { setTribulationResistance(value); }

    public void startTribulation(Realm targetRealm, int totalStrikes, int initialDelayTicks) {
        if (targetRealm == null || totalStrikes <= 0) {
            clearTribulation();
            return;
        }
        tribulationActive = true;
        tribulationTargetRealm = targetRealm;
        tribulationCurrentStrike = 0;
        tribulationTotalStrikes = Math.max(1, totalStrikes);
        tribulationNextStrikeTicks = Math.max(0, initialDelayTicks);
    }

    public void clearTribulation() {
        tribulationActive = false;
        tribulationTargetRealm = Realm.MORTAL;
        tribulationCurrentStrike = 0;
        tribulationTotalStrikes = 0;
        tribulationNextStrikeTicks = 0;
    }

    public boolean tickTribulationCountdown() {
        if (!tribulationActive) return false;
        if (tribulationNextStrikeTicks > 0) {
            tribulationNextStrikeTicks--;
        }
        return tribulationNextStrikeTicks <= 0;
    }

    public void recordTribulationStrike(int nextDelayTicks) {
        if (!tribulationActive) return;
        tribulationCurrentStrike = Math.min(tribulationTotalStrikes, tribulationCurrentStrike + 1);
        tribulationNextStrikeTicks = Math.max(0, nextDelayTicks);
    }

    public void scheduleTribulationRetry(int delayTicks) {
        if (tribulationActive) {
            tribulationNextStrikeTicks = Math.max(1, delayTicks);
        }
    }

    public boolean isTribulationComplete() {
        return tribulationActive && tribulationTotalStrikes > 0 && tribulationCurrentStrike >= tribulationTotalStrikes;
    }

    public int failTribulationPenalty() {
        int bodyLoss = bodyRefinement <= 0 ? 0 : Math.max(1, (int)Math.ceil(bodyRefinement * 0.15D));
        Realm failedTargetRealm = tribulationTargetRealm;
        clearTribulation();
        applySevereInjury();
        addQiDeviationRisk(20);
        bodyRefinement = Math.max(0, bodyRefinement - bodyLoss);
        fallOneStageForTribulation();
        if (failedTargetRealm == Realm.CORE_FORMATION) {
            clearGoldCore();
        }
        realmFallScars++;
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
        divineConsciousness = Math.min(divineConsciousness, getMaxDivineConsciousness());
        return bodyLoss;
    }

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
        int adjusted = Math.max(0, (int)Math.round(amount * getCultivationSpeedMultiplier() * getCultivationBoostMultiplier()));
        cultivationExp = Math.max(getCurrentStageStartExp(), Math.min(getCurrentStageCapExp(), cultivationExp + adjusted));
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
    }

    public int addMeditationCultivation(MeditationFormula.Breakdown breakdown) {
        meditationCultivationProgress += Math.max(0.0D,
                breakdown.perTick() * getCultivationBoostMultiplier() * getWorldpackDailyCultivationMultiplier());
        int whole = (int) Math.floor(meditationCultivationProgress);
        if (whole <= 0) return 0;
        meditationCultivationProgress -= whole;
        int before = cultivationExp;
        cultivationExp = Math.max(getCurrentStageStartExp(), Math.min(getCurrentStageCapExp(), cultivationExp + whole));
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
        return Math.max(0, cultivationExp - before);
    }

    public void setCultivation(long amount) {
        cultivationExp = (int)Math.max(getCurrentStageStartExp(), Math.min(getCurrentStageCapExp(), amount));
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
    }

    public void setCultivationExp(int amount) { setCultivation(amount); }
    public void setCultivationLong(long amount) { setCultivation(amount); }

    public void setAbsoluteCultivationForDebug(long amount) {
        cultivationExp = (int)Math.max(0L, Math.min(Integer.MAX_VALUE, amount));
        updateRealmFromCultivationExp();
        cultivationExp = Math.max(getCurrentStageStartExp(), Math.min(cultivationExp, getCurrentStageCapExp()));
        spiritualPower = Math.min(spiritualPower, getMaxSpiritualPower());
        divineConsciousness = Math.min(divineConsciousness, getMaxDivineConsciousness());
        unlockEligibleTechniqueSkills();
    }

    public void setCoreAttributesForDebug(int divSense, int bodyRef, int qiDevRisk, int tribRes) {
        divineConsciousness = Math.max(0, divSense);
        bodyRefinement = Math.max(0, bodyRef);
        setQiDeviationRisk(qiDevRisk);
        setTribulationResistance(tribRes);
    }

    /**
     * 直接增减修为经验值（不应用修炼速度倍率），用于走火入魔等惩罚。
     * 不会低于当前境界起始经验值。
     */
    public void addCultivationExpRaw(int amount) {
        cultivationExp = Math.max(getCurrentStageStartExp(), Math.min(getCurrentStageCapExp(), cultivationExp + amount));
    }

    /**
     * 公开方法：掉落一个境界阶段（用于走火入魔严重效果）。
     */
    public void fallOneStagePublic() {
        fallOneStage();
    }

    public void addAgeYears(int years) {
        ageYears = Math.max(0, ageYears + years);
    }

    public void addLifespanYears(int years) {
        lifespanYears = Math.max(realm.getLifespanYears(), lifespanYears + Math.max(0, years));
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
        double rootBonus = spiritualRoot.getBreakthroughBonus();
        double obsessionBonus = getBreakthroughObsessionBonus();
        double pillBonus = modifiers == null ? 0.0D : modifiers.pillBonus();
        double spiritEyeBonus = modifiers == null ? 0.0D : modifiers.spiritEyeBonus();
        double techniqueQualityBonus = modifiers == null ? 0.0D : modifiers.techniqueQualityBonus();
        double eventBonus = modifiers == null ? 0.0D : modifiers.eventBonus();
        Realm targetRealm = getNextBreakthroughRealm();
        double advancedBonus = getAdvancedBreakthroughBonus(targetRealm);
        double chanceCap = getBreakthroughChanceCap(targetRealm);
        double chance = Math.min(chanceCap, baseChance + rootBonus + pillBonus + spiritEyeBonus
                + techniqueQualityBonus + eventBonus + obsessionBonus + advancedBonus);
        return new BreakthroughChanceBreakdown(baseChance, pillBonus, spiritEyeBonus, techniqueQualityBonus,
                eventBonus, obsessionBonus, advancedBonus, chance);
    }

    public BreakthroughAttemptResult tryBreakthrough(RandomSource random) {
        return tryBreakthrough(random, BreakthroughChanceModifiers.NONE);
    }

    public BreakthroughAttemptResult tryBreakthrough(RandomSource random, BreakthroughChanceModifiers modifiers) {
        Realm oldRealm = realm;
        RealmStage oldStage = stage;
        BreakthroughChanceBreakdown breakdown = getBreakthroughChanceBreakdown(modifiers);
        if (isAtFinalStage()) return new BreakthroughAttemptResult(BreakthroughAttemptStatus.FINAL_STAGE, false, QiDeviationTier.NONE, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
        if (!isAtBreakthroughCap()) return new BreakthroughAttemptResult(BreakthroughAttemptStatus.NOT_AT_CAP, false, QiDeviationTier.NONE, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
        clearBreakthroughPillBonus();
        if (random.nextDouble() > breakdown.chance()) {
            failedBreakthroughs++;
            int stageStart = getCurrentStageStartExp();
            int remainingProgress = Math.max(0, (int)Math.floor(getCurrentStageExpSpan() * 0.80D));
            cultivationExp = stageStart + remainingProgress;
            addQiDeviationRisk(10);
            boolean qiDeviationTriggered = checkQiDeviation(random);
            QiDeviationTier tier = qiDeviationTriggered ? determineQiDeviationTier() : QiDeviationTier.NONE;
            return new BreakthroughAttemptResult(BreakthroughAttemptStatus.FAILURE, qiDeviationTriggered, tier, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
        }
        failedBreakthroughs = 0;
        spiritualPower = 0;
        advanceOneStage();
        cultivationExp = getCurrentStageStartExp();
        return new BreakthroughAttemptResult(BreakthroughAttemptStatus.SUCCESS, false, QiDeviationTier.NONE, oldRealm, oldStage, realm, stage, breakdown, qiDeviationRisk);
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

    /**
     * Non-resource breakthrough modifiers.  {@code eventBonus} is supplied by
     * server-authoritative temporary events and deliberately does not affect
     * {@link #isBreakthroughAssisted()} or breakthrough resource requirements.
     */
    public record BreakthroughChanceModifiers(double pillBonus, double spiritEyeBonus,
                                               double techniqueQualityBonus, double eventBonus) {
        public BreakthroughChanceModifiers(double pillBonus, double spiritEyeBonus,
                                           double techniqueQualityBonus) {
            this(pillBonus, spiritEyeBonus, techniqueQualityBonus, 0.0D);
        }

        public BreakthroughChanceModifiers {
            eventBonus = Double.isFinite(eventBonus) ? Math.max(0.0D, Math.min(0.20D, eventBonus)) : 0.0D;
        }

        public static final BreakthroughChanceModifiers NONE =
                new BreakthroughChanceModifiers(0.0D, 0.0D, 0.0D, 0.0D);
    }

    public record BreakthroughChanceBreakdown(
            double baseChance,
            double pillBonus,
            double spiritEyeBonus,
            double techniqueQualityBonus,
            double eventBonus,
            double obsessionBonus,
            double advancedBonus,
            double chance) {}

    public record BreakthroughAttemptResult(
            BreakthroughAttemptStatus status,
            boolean qiDeviationTriggered,
            QiDeviationTier qiDeviationTier,
            Realm oldRealm,
            RealmStage oldStage,
            Realm newRealm,
            RealmStage newStage,
            BreakthroughChanceBreakdown chanceBreakdown,
            int qiDeviationRisk) {
        public boolean success() { return status == BreakthroughAttemptStatus.SUCCESS; }
        public double chance() { return chanceBreakdown.chance(); }
    }

    // ========== 走火入魔分级 ==========
    public enum QiDeviationTier {
        NONE, MINOR, MODERATE, SEVERE, EXTREME
    }

    public QiDeviationTier determineQiDeviationTier() {
        if (qiDeviationRisk >= 100) return QiDeviationTier.EXTREME;
        if (qiDeviationRisk >= 90) return QiDeviationTier.SEVERE;
        if (qiDeviationRisk >= 80) return QiDeviationTier.MODERATE;
        if (qiDeviationRisk >= 70) return QiDeviationTier.MINOR;
        return QiDeviationTier.NONE;
    }

    public QiDeviationTier rollQiDeviation(RandomSource random) {
        return checkQiDeviation(random) ? determineQiDeviationTier() : QiDeviationTier.NONE;
    }

    private boolean checkQiDeviation(RandomSource random) {
        if (qiDeviationRisk >= MAX_QI_DEVIATION_RISK) return true;
        if (qiDeviationRisk < 70) return false;
        double chance = Math.min(0.50D, Math.max(0.20D, (qiDeviationRisk - 50) / 100.0D));
        return random.nextDouble() < chance;
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
        // 对齐 realm_breakthrough_v98 base_success（层内/跨境）
        return BreakthroughCatalog.baseSuccess(this);
    }

    private double getBreakthroughChanceCap(Realm targetRealm) {
        if (targetRealm == null) return GLOBAL_BREAKTHROUGH_CAP;
        return switch (targetRealm) {
            case SOUL_TRANSFORMATION -> 0.75D;
            case VOID_REFINEMENT -> 0.65D;
            case UNITY -> 0.55D;
            case MAHAYANA -> 0.45D;
            case TRIBULATION -> 0.35D;
            case TRUE_IMMORTAL -> 0.25D;
            default -> GLOBAL_BREAKTHROUGH_CAP;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String cleanWorldpackId(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }

    static double getWorldpackDailyCultivationMultiplier(String eventId) {
        return switch (cleanWorldpackId(eventId, "").toLowerCase(Locale.ROOT)) {
            case "spirit_rain" -> WORLDPACK_SPIRIT_RAIN_CULTIVATION_MULTIPLIER;
            case "spirit_vein_pulse" -> WORLDPACK_SPIRIT_VEIN_PULSE_CULTIVATION_MULTIPLIER;
            default -> 1.0D;
        };
    }

    private static double clampDailyCultivationMultiplier(double multiplier) {
        return Double.isFinite(multiplier) ? Math.max(1.0D, Math.min(5.0D, multiplier)) : 1.0D;
    }

    private static int normalizeRiskPercent(float value, int max) {
        float percent = value <= 1.0F ? value * 100.0F : value;
        return clamp(Math.round(percent), 0, max);
    }

    private static int loadPercentField(CompoundTag tag, String valueKey, String percentKey, int max) {
        if (tag.contains(percentKey)) {
            return clamp(tag.getInt(percentKey), 0, max);
        }
        if (!tag.contains(valueKey)) {
            return 0;
        }
        float value = tag.getFloat(valueKey);
        return normalizeRiskPercent(value, max);
    }

    private int getFailureLifespanPenalty() {
        return switch (realm) {
            case MORTAL -> 5;
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

    private void loadTribulationData(CompoundTag tag) {
        if (!tag.getBoolean("TribulationActive")) {
            clearTribulation();
            return;
        }
        try {
            tribulationTargetRealm = Realm.valueOf(tag.getString("TribulationTargetRealm"));
        } catch (Exception ignored) {
            clearTribulation();
            return;
        }
        tribulationTotalStrikes = Math.max(0, tag.getInt("TribulationTotalStrikes"));
        if (tribulationTotalStrikes <= 0 || tribulationTargetRealm.ordinal() < Realm.CORE_FORMATION.ordinal()) {
            clearTribulation();
            return;
        }
        tribulationActive = true;
        tribulationCurrentStrike = clamp(tag.getInt("TribulationCurrentStrike"), 0, tribulationTotalStrikes);
        tribulationNextStrikeTicks = Math.max(0, tag.getInt("TribulationNextStrikeTicks"));
    }

    public static RealmStage[] getStagesForRealmPublic(Realm targetRealm) {
        return stagesForRealm(targetRealm).clone();
    }

    private RealmStage[] getStagesForRealm(Realm targetRealm) {
        return stagesForRealm(targetRealm);
    }

    private static RealmStage[] stagesForRealm(Realm targetRealm) {
        if (targetRealm == Realm.MORTAL) {
            return new RealmStage[] { RealmStage.MORTAL };
        }
        if (targetRealm.isLayerBased()) {
            return new RealmStage[] {
                    RealmStage.LAYER_1, RealmStage.LAYER_2, RealmStage.LAYER_3, RealmStage.LAYER_4, RealmStage.LAYER_5,
                    RealmStage.LAYER_6, RealmStage.LAYER_7, RealmStage.LAYER_8, RealmStage.LAYER_9, RealmStage.LAYER_10,
                    RealmStage.LAYER_11, RealmStage.LAYER_12, RealmStage.LAYER_13
            };
        }
        if (targetRealm == Realm.FOUNDATION_ESTABLISHMENT) {
            return new RealmStage[] { RealmStage.EARLY, RealmStage.MIDDLE, RealmStage.LATE, RealmStage.PEAK };
        }
        if (targetRealm.getSubStages() == 1) {
            return new RealmStage[] { RealmStage.EARLY };
        }
        return new RealmStage[] { RealmStage.EARLY, RealmStage.MIDDLE, RealmStage.LATE };
    }

    public CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CultivationNbtVersion", 1);
        tag.putLong("cultivation", cultivationExp);
        tag.putLong("cultivationMax", getCultivationMax());
        tag.putInt("mana", spiritualPower);
        tag.putInt("manaMax", getMaxSpiritualPower());
        tag.putInt("divSense", divineConsciousness);
        tag.putInt("bodyRef", bodyRefinement);
        tag.putFloat("qiDevRisk", getQiDevRiskFloat());
        tag.putFloat("tribRes", getTribResFloat());
        tag.putInt("qiDevRiskPercent", qiDeviationRisk);
        tag.putInt("tribResPercent", tribulationResistance);
        tag.putBoolean("TribulationActive", tribulationActive);
        tag.putString("TribulationTargetRealm", tribulationTargetRealm.name());
        tag.putInt("TribulationCurrentStrike", tribulationCurrentStrike);
        tag.putInt("TribulationTotalStrikes", tribulationTotalStrikes);
        tag.putInt("TribulationNextStrikeTicks", tribulationNextStrikeTicks);
        tag.putString("GoldCoreGrade", goldCoreGrade.name());
        tag.putInt("GoldCoreScore", goldCoreScore);
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
        tag.putString("ConstitutionId", getConstitutionId());
        tag.putString("CultivationPathId", getCultivationPathId());
        tag.putString("PlayableRaceId", getPlayableRaceId());
        tag.putString("GhostPathStageId", getGhostPathStageId());
        tag.putInt("LifespanYears", lifespanYears);
        tag.putInt("AgeYears", ageYears);
        tag.putBoolean("UsedReturnYangTrueWater", usedReturnYangTrueWater);
        tag.putInt("FailedBreakthroughs", failedBreakthroughs);
        tag.putBoolean("RootInitialized", rootInitialized);
        tag.putInt("SpiritualRootPurity", spiritualRootPurity);
        tag.putBoolean("SpiritualRootAwakened", spiritualRootAwakened);
        tag.putBoolean("SpiritualRootTested", spiritualRootTested);
        tag.putBoolean("MysticVialGranted", mysticVialGranted);
        tag.put("SevenMysteriesQuest", sevenMysteriesQuest.saveNBT());
        tag.putBoolean("SevereInjury", severeInjury);
        tag.putInt("HeartDemonLevel", heartDemonLevel);
        tag.putInt("HeartDemonTriggerTicks", heartDemonTriggerTicks);
        tag.putBoolean("DeathSubstituteReady", deathSubstituteReady);
        tag.putBoolean("ShatteredCore", shatteredCore);
        tag.putInt("RealmFallScars", realmFallScars);
        tag.putInt("CultivationBoostTicks", cultivationBoostTicks);
        tag.putDouble("CultivationBoostMultiplier", cultivationBoostMultiplier);
        tag.putDouble("WorldpackDailyCultivationMultiplier", getWorldpackDailyCultivationMultiplier());
        tag.putDouble("MovementSpeedScale", movementSpeedScale);
        tag.putString("WorldpackCurrentRegion", getWorldpackCurrentRegionId());
        tag.putString("WorldpackActiveSecretRealm", getWorldpackActiveSecretRealmId());
        tag.putString("WorldpackActiveDailyEvent", getWorldpackActiveDailyEventId());
        tag.putLong("WorldpackDailyEventUntilTick", worldpackActiveDailyEventUntilTick);
        CompoundTag worldpackReturnTag = new CompoundTag();
        worldpackReturnTag.putBoolean("HasReturn", hasWorldpackReturnLocation());
        worldpackReturnTag.putString("Dimension", getWorldpackReturnDimension());
        worldpackReturnTag.putDouble("X", worldpackReturnX);
        worldpackReturnTag.putDouble("Y", worldpackReturnY);
        worldpackReturnTag.putDouble("Z", worldpackReturnZ);
        worldpackReturnTag.putFloat("YRot", worldpackReturnYRot);
        worldpackReturnTag.putFloat("XRot", worldpackReturnXRot);
        tag.put("WorldpackReturn", worldpackReturnTag);
        CompoundTag worldpackCooldownTag = new CompoundTag();
        worldpackCooldownUntilTicks.forEach(worldpackCooldownTag::putLong);
        tag.put("WorldpackCooldownUntilTicks", worldpackCooldownTag);
        tag.putInt("QiDevDecayTicks", qiDevDecayAccumulatorTicks);
        tag.putInt("LeylineQiDevDecayTicks", leylineQiDevDecayAccumulatorTicks);
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
        qiDeviationRisk = loadPercentField(tag, "qiDevRisk", "qiDevRiskPercent", MAX_QI_DEVIATION_RISK);
        tribulationResistance = loadPercentField(tag, "tribRes", "tribResPercent", MAX_TRIBULATION_RESISTANCE);
        loadTribulationData(tag);
        try {
            goldCoreGrade = tag.contains("GoldCoreGrade") ? GoldCoreGrade.valueOf(tag.getString("GoldCoreGrade")) : GoldCoreGrade.NONE;
        } catch (Exception ignored) {
            goldCoreGrade = GoldCoreGrade.NONE;
        }
        goldCoreScore = Math.max(0, tag.getInt("GoldCoreScore"));
        cultivationExp = tag.contains("cultivation") ? (int)Math.max(0L, Math.min(Integer.MAX_VALUE, tag.getLong("cultivation"))) : tag.getInt("CultivationExp");
        if (!loadRealmAndStage(tag)) {
            updateRealmFromCultivationExp();
        }
        cultivationExp = Math.max(getCurrentStageStartExp(), Math.min(cultivationExp, getCurrentStageCapExp()));
        breakthroughPillBonus = tag.contains("BreakthroughPillBonus") ? Math.max(0.0D, Math.min(0.20D, tag.getDouble("BreakthroughPillBonus"))) : (tag.getBoolean("BreakthroughAssisted") ? 0.05D : 0.0D);
        breakthroughAssisted = breakthroughPillBonus > 0.0D;
        meditating = tag.getBoolean("Meditating");
        spiritualRoot = SpiritualRoot.fromName(tag.getString("SpiritualRoot"));
        loadSpiritualRootAttributes(tag);
        try { specialPhysique = SpecialPhysique.valueOf(tag.getString("SpecialPhysique")); } catch (Exception ignored) { specialPhysique = SpecialPhysique.NONE; }
        if (tag.contains("ConstitutionId") && !tag.getString("ConstitutionId").isBlank()) {
            constitutionId = tag.getString("ConstitutionId").trim().toLowerCase(Locale.ROOT);
            specialPhysique = SpecialPhysique.fromConstitutionId(constitutionId);
        } else {
            constitutionId = SpecialPhysique.toConstitutionId(specialPhysique);
        }
        cultivationPathId = tag.contains("CultivationPathId")
                ? PathRaceCatalog.sanitizePathId(tag.getString("CultivationPathId"))
                : PathRaceCatalog.DEFAULT_PATH_ID;
        playableRaceId = tag.contains("PlayableRaceId")
                ? PathRaceCatalog.sanitizeRaceId(tag.getString("PlayableRaceId"))
                : PathRaceCatalog.DEFAULT_RACE_ID;
        ghostPathStageId = tag.contains("GhostPathStageId")
                ? PathRaceCatalog.sanitizeGhostStageId(tag.getString("GhostPathStageId"))
                : "";
        if (!ghostPathStageId.isBlank()) {
            cultivationPathId = PathRaceCatalog.GHOST_PATH_ID;
        }
        lifespanYears = tag.contains("LifespanYears") ? tag.getInt("LifespanYears") : realm.getLifespanYears();
        ageYears = tag.contains("AgeYears") ? tag.getInt("AgeYears") : 16;
        usedReturnYangTrueWater = tag.getBoolean("UsedReturnYangTrueWater");
        failedBreakthroughs = tag.getInt("FailedBreakthroughs");
        rootInitialized = tag.getBoolean("RootInitialized") || tag.contains("SpiritualRootAttributes") || tag.contains("SpiritualRootAttribute");
        spiritualRootPurity = tag.contains("SpiritualRootPurity") ? Math.max(1, Math.min(100, tag.getInt("SpiritualRootPurity"))) : 50;
        spiritualRootAwakened = !tag.contains("SpiritualRootAwakened") || tag.getBoolean("SpiritualRootAwakened");
        spiritualRootTested = tag.getBoolean("SpiritualRootTested");
        mysticVialGranted = tag.getBoolean("MysticVialGranted");
        sevenMysteriesQuest.loadNBT(tag.contains("SevenMysteriesQuest") ? tag.getCompound("SevenMysteriesQuest") : new CompoundTag());
        severeInjury = tag.getBoolean("SevereInjury");
        heartDemonLevel = Math.max(0, tag.getInt("HeartDemonLevel"));
        heartDemonTriggerTicks = Math.max(0, tag.getInt("HeartDemonTriggerTicks"));
        deathSubstituteReady = tag.getBoolean("DeathSubstituteReady");
        shatteredCore = tag.getBoolean("ShatteredCore");
        realmFallScars = Math.max(0, tag.getInt("RealmFallScars"));
        cultivationBoostTicks = Math.max(0, tag.getInt("CultivationBoostTicks"));
        cultivationBoostMultiplier = cultivationBoostTicks > 0 && tag.contains("CultivationBoostMultiplier")
                ? Math.max(1.0D, tag.getDouble("CultivationBoostMultiplier"))
                : 1.0D;
        movementSpeedScale = tag.contains("MovementSpeedScale")
                ? Math.max(0.0D, Math.min(1.0D, tag.getDouble("MovementSpeedScale")))
                : 1.0D;
        worldpackCurrentRegionId = cleanWorldpackId(tag.getString("WorldpackCurrentRegion"), "qinglan_mountains");
        worldpackActiveSecretRealmId = cleanWorldpackId(tag.getString("WorldpackActiveSecretRealm"), "");
        worldpackActiveDailyEventId = cleanWorldpackId(tag.getString("WorldpackActiveDailyEvent"), "");
        worldpackActiveDailyEventUntilTick = Math.max(0L, tag.getLong("WorldpackDailyEventUntilTick"));
        worldpackDailyCultivationMultiplier = tag.contains("WorldpackDailyCultivationMultiplier")
                ? clampDailyCultivationMultiplier(tag.getDouble("WorldpackDailyCultivationMultiplier"))
                : getWorldpackDailyCultivationMultiplier(worldpackActiveDailyEventId);
        worldpackHasReturnLocation = false;
        worldpackReturnDimension = "";
        if (tag.contains("WorldpackReturn")) {
            CompoundTag worldpackReturnTag = tag.getCompound("WorldpackReturn");
            worldpackHasReturnLocation = worldpackReturnTag.getBoolean("HasReturn");
            worldpackReturnDimension = worldpackReturnTag.getString("Dimension");
            worldpackReturnX = worldpackReturnTag.getDouble("X");
            worldpackReturnY = worldpackReturnTag.contains("Y") ? worldpackReturnTag.getDouble("Y") : 64.0D;
            worldpackReturnZ = worldpackReturnTag.getDouble("Z");
            worldpackReturnYRot = worldpackReturnTag.getFloat("YRot");
            worldpackReturnXRot = worldpackReturnTag.getFloat("XRot");
        }
        worldpackCooldownUntilTicks.clear();
        if (tag.contains("WorldpackCooldownUntilTicks")) {
            CompoundTag worldpackCooldownTag = tag.getCompound("WorldpackCooldownUntilTicks");
            for (String realmId : worldpackCooldownTag.getAllKeys()) {
                long untilTick = worldpackCooldownTag.getLong(realmId);
                if (!realmId.isBlank() && untilTick > 0L) {
                    worldpackCooldownUntilTicks.put(cleanWorldpackId(realmId, ""), untilTick);
                }
            }
        }
        qiDevDecayAccumulatorTicks = Math.max(0, tag.getInt("QiDevDecayTicks"));
        leylineQiDevDecayAccumulatorTicks = Math.max(0, tag.getInt("LeylineQiDevDecayTicks"));
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
        // M9: legacy cooldown values used per-dimension gameTime; clear them after global-time migration.
        if (tag.contains("TechniqueCooldownUntilTicks") && tag.getInt("CultivationNbtVersion") >= 1) {
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
