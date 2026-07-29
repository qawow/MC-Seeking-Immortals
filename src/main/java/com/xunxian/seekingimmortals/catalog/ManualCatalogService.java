package com.xunxian.seekingimmortals.catalog;

import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaKnowledge;
import com.xunxian.seekingimmortals.alchemy.AlchemyDisplayTexts;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStage;
import com.xunxian.seekingimmortals.network.SyncLearnedMethodsPacket;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import com.xunxian.seekingimmortals.quest.DetailedQuestProofService;
import com.xunxian.seekingimmortals.sect.SectContributionService;
import com.xunxian.seekingimmortals.sect.SectDefinitionService;
import com.xunxian.seekingimmortals.skill.MethodLayerTechniqueService;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Applies manuals_catalog entries via existing catalog carriers.
 * Wave464: persistent studied set + forge-grade unlock ceiling from rich text_material fields.
 * Wave473: cultivation method learn authority (persistent learned methods for TechniqueGate).
 * Wave474: manual unlocks + sect outer promotion grant starter methods.
 * Wave475: technique-manual source text maps to related cultivation methods.
 * Wave481: method layer cultivation with spiritual/cultivation costs.
 * Reuses TextMaterialCatalogService.ManualEntry/MethodEntry and CultivationHelper only (no new systems).
 */
public final class ManualCatalogService {
    public static final String STUDIED_TAG = "seeking_immortals_studied_manuals";
    public static final String LEARNED_METHODS_TAG = "seeking_immortals_learned_methods";
    public static final String METHOD_LAYERS_TAG = "seeking_immortals_method_layers";
    public static final String SOFT_FORGE_GRADES_TAG = "seeking_immortals_soft_forge_grades";
    private static final List<String> PROGRESSION_TAGS = List.of(
            STUDIED_TAG,
            LEARNED_METHODS_TAG,
            METHOD_LAYERS_TAG,
            SOFT_FORGE_GRADES_TAG);

    private ManualCatalogService() {}

    enum MethodLearnFailure {
        NONE,
        REALM_TOO_LOW,
        REALM_TOO_HIGH,
        INVALID_REALM,
        PREREQUISITE_MISSING,
        PREREQUISITE_LAYER,
        ROOT_MISMATCH,
        CONSTITUTION_MISMATCH,
        RACE_MISMATCH,
        FACTION_MISMATCH,
        FACTION_RANK_TOO_LOW,
        CONVERT_REQUIRED
    }

    public enum BrahmaAssemblyResult {
        NOT_APPLICABLE,
        ASSEMBLED,
        BLOCKED
    }

    record MethodLearnGate(MethodLearnFailure failure, String requiredMethod,
                           int requiredLayer, int currentLayer) {
        static MethodLearnGate allowed() {
            return new MethodLearnGate(MethodLearnFailure.NONE, "", 0, 0);
        }

        static MethodLearnGate deny(MethodLearnFailure failure) {
            return new MethodLearnGate(failure == null ? MethodLearnFailure.INVALID_REALM : failure, "", 0, 0);
        }

        static MethodLearnGate deny(MethodLearnFailure failure, String requiredMethod) {
            return new MethodLearnGate(failure == null ? MethodLearnFailure.INVALID_REALM : failure,
                    requiredMethod == null ? "" : requiredMethod, 0, 0);
        }

        boolean isAllowed() {
            return failure == MethodLearnFailure.NONE;
        }
    }

    /** Preserve consumed-manual progression when Forge clones a player after death. */
    public static void copyProgressionData(CompoundTag originalData, CompoundTag clonedData) {
        if (originalData == null || clonedData == null) {
            return;
        }
        for (String key : PROGRESSION_TAGS) {
            if (originalData.contains(key) && originalData.get(key) != null) {
                clonedData.put(key, originalData.get(key).copy());
            }
        }
    }

    public static boolean study(ServerPlayer player, String manualId) {
        Optional<TextMaterialCatalogService.ManualEntry> optional =
                TextMaterialCatalogService.builtin().findManual(manualId);
        if (optional.isEmpty()) {
            // Bulk manuals without a manuals_catalog row still leave a durable studied mark
            // and try source→method grants from the carrier id itself.
            return studySoftManual(player, manualId);
        }
        TextMaterialCatalogService.ManualEntry manual = optional.get();
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            if (!manual.realmMin().isBlank() && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), manual.realmMin())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.realm_too_low",
                        displayName(manual),
                        com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(manual.realmMin())), false);
                return;
            }
            if (hasStudied(player, manual.id())) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.already_studied",
                        displayName(manual)), false);
                return;
            }
            if (!canGrantUnlockMethods(player, manual.unlocks(), cultivation.getRealm())) {
                return;
            }
            markStudied(player, manual.id());
            applyInsight(player, manual);
            // Wave474: unlock tokens that map to cultivation methods are granted as learned methods.
            int grantedMethods = grantUnlockMethods(player, manual.unlocks());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.studied",
                    displayName(manual), typeDisplay(manual.type())), true);
            if (!manual.note().isBlank() && !manual.note().equals(manual.display())
                    && PlayerDisplayText.isSafe(manual.note())) {
                player.displayClientMessage(Component.literal(manual.note()), false);
            }
            if (!manual.unlocks().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.unlocks",
                        unlocksDisplay(manual.unlocks())), false);
            }
            if (grantedMethods > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.manual.methods_granted", grantedMethods), false);
            }
            if (manual.unlocksForgeGrade() > 0) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.forge_grade",
                        manual.unlocksForgeGrade()), false);
            }
            if (!manual.recipeId().isBlank()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.manual.recipe",
                        recipeDisplay(manual.recipeId())), false);
            }
            ok[0] = true;
        });
        return ok[0];
    }

    /**
     * Soft study for bulk knowledge carriers that lack a manuals_catalog entry.
     * Marks studied + attempts source→method mapping; never invents forge grades.
     */
    private static boolean studySoftManual(ServerPlayer player, String manualId) {
        if (player == null || manualId == null || manualId.isBlank()) {
            return false;
        }
        String id = manualId.trim().toLowerCase(Locale.ROOT);
        if (hasStudied(player, id)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.manual.already_studied",
                    PlayerDisplayText.itemName(id)), false);
            return false;
        }
        markStudied(player, id);
        int granted = grantMethodsFromTechniqueSource(player, id);
        // Explicit soft grants for bulk manuals that never match method keywords.
        for (String methodId : softMethodGrants(id)) {
            TextMaterialCatalogService.MethodEntry method = TextMaterialCatalogService.builtin()
                    .findMethod(methodId).orElse(null);
            if (method != null && grantKnownMethodIfEligible(player, method)) {
                granted++;
            }
        }
        // Alchemy recipe carriers: recipe_<id> / alchemy_manual_* study a formula if known.
        String formulaId = softAlchemyRecipeId(id);
        if (!formulaId.isBlank()) {
            AlchemyFormulaKnowledge.study(player, formulaId);
        }
        // Soft refinement manuals raise forge grade via dedicated NBT map.
        int softGrade = softForgeGrade(id);
        if (softGrade > 0) {
            recordSoftForgeGrade(player, id, softGrade);
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.manual.studied",
                PlayerDisplayText.itemName(id),
                typeDisplay(guessSoftManualType(id))), true);
        if (granted > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.manual.methods_granted", granted), false);
        }
        if (softGrade > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.manual.forge_grade", softGrade), false);
        }
        return true;
    }

    private static String guessSoftManualType(String id) {
        String key = id == null ? "" : id.toLowerCase(Locale.ROOT);
        if (key.contains("alchemy") || key.startsWith("recipe_")) {
            return "alchemy";
        }
        if (key.contains("refine") || key.contains("artifact") || key.contains("forge")) {
            return "refinement";
        }
        if (key.contains("talisman") || key.contains("fu")) {
            return "talisman";
        }
        if (key.contains("formation") || key.contains("array")) {
            return "formation";
        }
        if (key.contains("puppet")) {
            return "puppet";
        }
        return "cultivation_path";
    }

    public static Component typeDisplay(String typeCode) {
        String code = typeCode == null ? "" : typeCode.trim().toLowerCase(Locale.ROOT);
        if (code.isBlank()) {
            return Component.translatable("manual.type.seeking_immortals.unknown");
        }
        String key = "manual.type.seeking_immortals." + code;
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        if (language != null && language.has(key)) {
            return Component.translatable(key);
        }
        String known = switch (code) {
            case "alchemy" -> "炼丹";
            case "refinement" -> "炼器";
            case "talisman" -> "符箓";
            case "formation" -> "阵法";
            case "puppet" -> "傀儡";
            case "cultivation_path" -> "修炼道路";
            case "quest" -> "任务";
            default -> "";
        };
        return known.isBlank() ? Component.translatable("manual.type.seeking_immortals.unknown")
                : Component.literal(known);
    }

    public static Component recipeDisplay(String recipeId) {
        String id = recipeId == null ? "" : recipeId.trim();
        if (id.isBlank()) {
            return Component.literal("");
        }
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        String itemKey = "item.seeking_immortals." + id;
        if (language != null && language.has(itemKey)) {
            return Component.translatable(itemKey);
        }
        String alchemyKey = "alchemy_recipe.seeking_immortals." + id;
        if (language != null && language.has(alchemyKey)) {
            return Component.translatable(alchemyKey);
        }
        Component resolved = AlchemyDisplayTexts.recipe(id);
        if (!resolved.getString().equals(Component.translatable("text.seeking_immortals.unknown_formula").getString())) {
            return resolved;
        }
        // Prefer catalog manual display when recipe id equals a manual carrier.
        Optional<TextMaterialCatalogService.ManualEntry> manual =
                TextMaterialCatalogService.builtin().findManual(id);
        if (manual.isPresent() && manual.get().display() != null && !manual.get().display().isBlank()) {
            return PlayerDisplayText.safeLiteral(manual.get().display(), "text.seeking_immortals.unknown_formula");
        }
        return Component.translatable("text.seeking_immortals.unknown_formula");
    }

    public static Component displayName(TextMaterialCatalogService.ManualEntry manual) {
        if (manual == null) {
            return Component.translatable("text.seeking_immortals.unknown_manual");
        }
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        String itemKey = "item.seeking_immortals." + manual.id();
        if (language != null && language.has(itemKey)) {
            return Component.translatable(itemKey);
        }
        if (PlayerDisplayText.isSafe(manual.display())) {
            return Component.literal(manual.display());
        }
        return Component.translatable("text.seeking_immortals.unknown_manual");
    }

    private static String unlocksDisplay(List<String> unlocks) {
        if (unlocks == null || unlocks.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String unlock : unlocks) {
            if (unlock == null || unlock.isBlank()) {
                continue;
            }
            Optional<TextMaterialCatalogService.MethodEntry> method =
                    TextMaterialCatalogService.builtin().findMethod(unlock);
            if (method.isPresent() && method.get().display() != null && !method.get().display().isBlank()
                    && PlayerDisplayText.isSafe(method.get().display())) {
                parts.add(method.get().display());
                continue;
            }
            net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
            String itemKey = "item.seeking_immortals." + unlock;
            if (language != null && language.has(itemKey)) {
                parts.add(language.getOrDefault(itemKey));
            } else {
                String unlockKey = "manual.unlock.seeking_immortals." + unlock;
                parts.add(language != null && language.has(unlockKey)
                        ? language.getOrDefault(unlockKey)
                        : language == null ? "未收录内容"
                        : language.getOrDefault("text.seeking_immortals.unknown_unlock"));
            }
        }
        return String.join("、", parts);
    }

    public static boolean hasStudied(ServerPlayer player, String manualId) {
        if (player == null || manualId == null || manualId.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(STUDIED_TAG).getBoolean(manualId.trim().toLowerCase(Locale.ROOT));
    }

    public static void markStudied(ServerPlayer player, String manualId) {
        if (player == null || manualId == null || manualId.isBlank()) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(STUDIED_TAG).copy();
        tag.putBoolean(manualId.trim().toLowerCase(Locale.ROOT), true);
        player.getPersistentData().put(STUDIED_TAG, tag);
    }

    /**
     * Max forge grade unlocked by studied refinement manuals.
     * Grade-1 is always available to callers (never returns below 1 for gameplay gating).
     */
    public static int maxUnlockedForgeGrade(ServerPlayer player) {
        if (player == null) {
            return 1;
        }
        int max = 1;
        CompoundTag studied = player.getPersistentData().getCompound(STUDIED_TAG);
        for (TextMaterialCatalogService.ManualEntry manual : TextMaterialCatalogService.builtin().manuals().values()) {
            if (manual == null || manual.unlocksForgeGrade() <= 0) {
                continue;
            }
            if (studied.getBoolean(manual.id().toLowerCase(Locale.ROOT))) {
                max = Math.max(max, manual.unlocksForgeGrade());
            }
        }
        CompoundTag softGrades = player.getPersistentData().getCompound(SOFT_FORGE_GRADES_TAG);
        for (String key : softGrades.getAllKeys()) {
            max = Math.max(max, softGrades.getInt(key));
        }
        return Math.max(1, max);
    }

    public static int manualCount() {
        return TextMaterialCatalogService.builtin().manuals().size();
    }

    public static int methodCount() {
        return TextMaterialCatalogService.builtin().methods().size();
    }

    /**
     * Wave473: learn a cultivation method from the catalog index.
     * Marks persistent learned flag used by TechniqueGateService requires_method checks.
     */
    public static boolean learnMethod(ServerPlayer player, String methodId) {
        Optional<TextMaterialCatalogService.MethodEntry> optional =
                TextMaterialCatalogService.builtin().findMethod(methodId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.unknown",
                    methodDisplay(methodId)), false);
            return false;
        }
        TextMaterialCatalogService.MethodEntry method = optional.get();
        if (hasLearnedMethod(player, method.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.already_learned",
                    methodDisplay(method)), false);
            return false;
        }
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            MethodLearnGate gate = evaluateLearnGate(method, cultivation,
                    prerequisiteId -> getMethodLayer(player, prerequisiteId),
                    cultivation.getSevenMysteriesQuest());
            if (!gate.isAllowed()) {
                displayLearnGateFailure(player, method, gate);
                return;
            }
            // M02: manual conflict matrix D/F pairs block learning.
            com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.GateResult conflict =
                    com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.canLearnMethod(player, method.id());
            if (!conflict.allowed()) {
                if (conflict.messageKey() != null && !conflict.messageKey().isBlank()) {
                    player.displayClientMessage(Component.translatable(conflict.messageKey(), conflict.args()), false);
                }
                return;
            }
            markLearnedMethod(player, method.id());
            setMethodLayer(player, method.id(), 1);
            int techniquesGranted = MethodLayerTechniqueService.grantForMethodLayer(player, method.id(), 1);
            DetailedQuestProofService.recordMethodLayerReached(player, method.id());
            // Light insight buff so learning is immediately felt.
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.learned",
                    methodDisplay(method), schoolDisplay(method.school())), true);
            if (techniquesGranted > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.method.techniques_granted", techniquesGranted), false);
            }
            ok[0] = true;
        });
        return ok[0];
    }

    /**
     * Consumes a real 梵圣真片 only after the authored 明王/托天 prerequisites are present.
     * The assembled method is a special progression transaction, not a generic method-tree
     * learning bypass; its high-tier techniques remain realm-gated by MethodLayerTechniqueService.
     */
    public static BrahmaAssemblyResult tryAssembleBrahmaSacred(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()
                || stack.getItem() != ModItems.TECHNIQUE_MANUAL_BRAHMA_SACRED_FRAGMENT.get()) {
            return BrahmaAssemblyResult.NOT_APPLICABLE;
        }
        if (hasLearnedMethod(player, "fansheng_zhenmogong")) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.brahma.already"), true);
            return BrahmaAssemblyResult.BLOCKED;
        }
        PlayerCultivation cultivation = CultivationHelper.get(player).orElse(null);
        if (cultivation == null || cultivation.getRealm().ordinal() < Realm.NASCENT_SOUL.ordinal()
                || (cultivation.getRealm() == Realm.NASCENT_SOUL
                && cultivation.getStage() != RealmStage.LATE
                && cultivation.getStage() != RealmStage.PEAK)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.brahma.realm"), true);
            return BrahmaAssemblyResult.BLOCKED;
        }
        if (!hasLearnedMethod(player, "mingwang_jue") || !hasLearnedMethod(player, "tuotian_mogong")) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.brahma.prerequisite"), true);
            return BrahmaAssemblyResult.BLOCKED;
        }
        TextMaterialCatalogService.MethodEntry method = TextMaterialCatalogService.builtin()
                .findMethod("fansheng_zhenmogong").orElse(null);
        if (method == null) {
            return BrahmaAssemblyResult.BLOCKED;
        }
        com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.GateResult conflict =
                com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.canLearnMethod(
                        player, method.id());
        if (!conflict.allowed()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.brahma.conflict"), true);
            return BrahmaAssemblyResult.BLOCKED;
        }
        markLearnedMethod(player, method.id());
        setMethodLayer(player, method.id(), 1);
        MethodLayerTechniqueService.grantForMethodLayer(player, method.id(), 1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        SyncLearnedMethodsPacket.send(player);
        DetailedQuestProofService.recordMethodLayerReached(player, method.id());
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.method.brahma.assembled"), false);
        return BrahmaAssemblyResult.ASSEMBLED;
    }

    /**
     * Wave481: cultivate an already-learned method to raise its catalog-defined layer.
     * Costs spiritual power + cultivation exp scaled by current layer.
     */
    public static boolean cultivateMethod(ServerPlayer player, String methodId) {
        Optional<TextMaterialCatalogService.MethodEntry> optional =
                TextMaterialCatalogService.builtin().findMethod(methodId);
        if (optional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.unknown",
                    methodDisplay(methodId)), false);
            return false;
        }
        TextMaterialCatalogService.MethodEntry method = optional.get();
        if (!hasLearnedMethod(player, method.id())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.not_learned",
                    methodDisplay(method)), false);
            return false;
        }
        int layer = getMethodLayer(player, method.id());
        int maxLayer = maxMethodLayer(method.id());
        if (layer >= maxLayer) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.layer_max",
                    methodDisplay(method), maxLayer), false);
            return false;
        }
        boolean[] ok = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            int nextLayer = layer + 1;
            String requiredRealm = MethodLayerTechniqueService.requiredRealmForLayer(method.id(), nextLayer);
            if (!requiredRealm.isBlank()
                    && !WorldpackGameplayService.meetsMinRealm(cultivation.getRealm(), requiredRealm)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.method.layer_realm_too_low",
                        methodDisplay(method), nextLayer,
                        com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(requiredRealm)), false);
                return;
            }
            int spCost = cultivateSpiritualCost(method, layer);
            int expCost = cultivateCultivationCost(method, layer);
            if (cultivation.getSpiritualPower() < spCost) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.method.cultivate_need_sp",
                        spCost, cultivation.getSpiritualPower()), false);
                return;
            }
            long haveExp = cultivation.getCultivationLong() - cultivation.getCurrentStageStartExp();
            if (haveExp < expCost) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.method.cultivate_need_exp",
                        expCost, haveExp), false);
                return;
            }
            if (!player.getAbilities().instabuild) {
                if (!cultivation.consumeSpiritualPower(spCost)) {
                    return;
                }
                long next = Math.max(cultivation.getCurrentStageStartExp(),
                        cultivation.getCultivationLong() - expCost);
                cultivation.setCultivation(next);
            }
            setMethodLayer(player, method.id(), nextLayer);
            int techniquesGranted = MethodLayerTechniqueService.grantForMethodLayer(player, method.id(), nextLayer);
            DetailedQuestProofService.recordMethodLayerReached(player, method.id());
            SyncLearnedMethodsPacket.send(player);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 0));
            player.displayClientMessage(Component.translatable("message.seeking_immortals.method.cultivated",
                    methodDisplay(method), nextLayer, maxLayer), true);
            if (techniquesGranted > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.method.techniques_granted", techniquesGranted), false);
            }
            ok[0] = true;
        });
        return ok[0];
    }

    public static int cultivateSpiritualCost(int currentLayer) {
        return cultivateSpiritualCost((TextMaterialCatalogService.MethodEntry) null, currentLayer);
    }

    public static int cultivateCultivationCost(int currentLayer) {
        return cultivateCultivationCost((TextMaterialCatalogService.MethodEntry) null, currentLayer);
    }

    public static int cultivateSpiritualCost(String methodId, int currentLayer) {
        return cultivateSpiritualCost(findMethod(methodId), currentLayer);
    }

    public static int cultivateCultivationCost(String methodId, int currentLayer) {
        return cultivateCultivationCost(findMethod(methodId), currentLayer);
    }

    public static int cultivateSpiritualCost(TextMaterialCatalogService.MethodEntry method, int currentLayer) {
        int layer = Math.max(1, currentLayer);
        CostProfile profile = costProfile(method);
        return Math.max(1, profile.baseSp() + layer * profile.spPerLayer());
    }

    public static int cultivateCultivationCost(TextMaterialCatalogService.MethodEntry method, int currentLayer) {
        int layer = Math.max(1, currentLayer);
        CostProfile profile = costProfile(method);
        return Math.max(1, profile.baseExp() + layer * profile.expPerLayer());
    }

    private static TextMaterialCatalogService.MethodEntry findMethod(String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return null;
        }
        return TextMaterialCatalogService.builtin().findMethod(methodId).orElse(null);
    }

    /**
     * Individualized cultivation cost profile.
     * Life/craft methods are cheaper; combat/demon methods cost more; long ladders amortize.
     */
    static CostProfile costProfile(TextMaterialCatalogService.MethodEntry method) {
        if (method == null) {
            return CostProfile.DEFAULT;
        }
        String school = method.school() == null ? "" : method.school().toLowerCase(Locale.ROOT);
        String attr = method.attribute() == null ? "" : method.attribute().toLowerCase(Locale.ROOT);
        String id = method.id() == null ? "" : method.id().toLowerCase(Locale.ROOT);
        String blob = school + " " + attr + " " + id;

        int baseSp = 20;
        int spPerLayer = 12;
        int baseExp = 40;
        int expPerLayer = 30;

        if (blob.contains("craft") || blob.contains("alchemy") || blob.contains("appraise")
                || blob.contains("formation") || blob.contains("life") || blob.contains("auxiliary")
                || blob.contains("scripture") || blob.contains("manual_life")) {
            baseSp = 12;
            spPerLayer = 6;
            baseExp = 24;
            expPerLayer = 14;
        } else if (blob.contains("sword") || blob.contains("combat") || blob.contains("battle")
                || blob.contains("demon") || blob.contains("ghost") || blob.contains("blood")
                || blob.contains("kill") || blob.contains("dao")) {
            baseSp = 28;
            spPerLayer = 16;
            baseExp = 55;
            expPerLayer = 38;
        } else if (blob.contains("body") || blob.contains("temper") || blob.contains("physique")) {
            baseSp = 18;
            spPerLayer = 10;
            baseExp = 48;
            expPerLayer = 34;
        }

        // Long ladders (e.g. Changchun 13) amortize per-layer cost slightly.
        int maxLayers = maxMethodLayer(method.id());
        if (maxLayers >= 12) {
            spPerLayer = Math.max(4, spPerLayer - 2);
            expPerLayer = Math.max(10, expPerLayer - 4);
        } else if (maxLayers <= 1) {
            // Zero-stage life methods: fixed single-layer learn; cultivate should not apply, but keep tiny costs.
            baseSp = Math.min(baseSp, 10);
            spPerLayer = Math.min(spPerLayer, 4);
            baseExp = Math.min(baseExp, 20);
            expPerLayer = Math.min(expPerLayer, 8);
        }

        // Wood/support methods lean cheaper; fire/thunder lean costlier.
        if (blob.contains("wood") || blob.contains("heal") || blob.contains("spirit")) {
            baseSp = Math.max(8, baseSp - 2);
            baseExp = Math.max(16, baseExp - 4);
        } else if (blob.contains("fire") || blob.contains("thunder") || blob.contains("lightning")) {
            baseSp += 2;
            spPerLayer += 1;
        }

        return new CostProfile(baseSp, spPerLayer, baseExp, expPerLayer);
    }

    record CostProfile(int baseSp, int spPerLayer, int baseExp, int expPerLayer) {
        static final CostProfile DEFAULT = new CostProfile(20, 12, 40, 30);
    }

    public static int maxMethodLayer(String methodId) {
        return MethodLayerTechniqueService.maxLayers(methodId);
    }

    public static int getMethodLayer(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return 0;
        }
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        if (!hasLearnedMethod(player, key)) {
            return 0;
        }
        CompoundTag layers = player.getPersistentData().getCompound(METHOD_LAYERS_TAG);
        int layer = layers.getInt(key);
        return layer <= 0 ? 1 : Math.min(maxMethodLayer(key), layer);
    }

    public static void setMethodLayer(ServerPlayer player, String methodId, int layer) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return;
        }
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        CompoundTag layers = player.getPersistentData().getCompound(METHOD_LAYERS_TAG).copy();
        layers.putInt(key, Math.max(1, Math.min(maxMethodLayer(key), layer)));
        player.getPersistentData().put(METHOD_LAYERS_TAG, layers);
        // Ensure learned flag stays true when layer is set.
        if (!hasLearnedMethod(player, key)) {
            CompoundTag learned = player.getPersistentData().getCompound(LEARNED_METHODS_TAG).copy();
            learned.putBoolean(key, true);
            player.getPersistentData().put(LEARNED_METHODS_TAG, learned);
        }
    }

    /** Wave481: ordered id -> layer map for network sync. */
    public static Map<String, Integer> learnedMethodLayers(ServerPlayer player) {
        Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (String id : learnedMethodIds(player)) {
            map.put(id, getMethodLayer(player, id));
        }
        return map;
    }

    public static boolean hasLearnedMethod(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(LEARNED_METHODS_TAG)
                .getBoolean(methodId.trim().toLowerCase(Locale.ROOT));
    }

    public static void markLearnedMethod(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(LEARNED_METHODS_TAG).copy();
        String key = methodId.trim().toLowerCase(Locale.ROOT);
        boolean already = tag.getBoolean(key);
        tag.putBoolean(key, true);
        player.getPersistentData().put(LEARNED_METHODS_TAG, tag);
        if (!already) {
            // Wave481: first learn starts at layer 1.
            CompoundTag layers = player.getPersistentData().getCompound(METHOD_LAYERS_TAG).copy();
            if (layers.getInt(key) <= 0) {
                layers.putInt(key, 1);
                player.getPersistentData().put(METHOD_LAYERS_TAG, layers);
            }
            SyncLearnedMethodsPacket.send(player);
        }
    }

    public static int learnedMethodCount(ServerPlayer player) {
        return learnedMethodIds(player).size();
    }

    /** Wave477: sorted learned method ids for network sync / UI. */
    public static List<String> learnedMethodIds(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag tag = player.getPersistentData().getCompound(LEARNED_METHODS_TAG);
        List<String> ids = new ArrayList<>();
        for (String key : tag.getAllKeys()) {
            if (tag.getBoolean(key) && key != null && !key.isBlank()) {
                ids.add(key.trim().toLowerCase(Locale.ROOT));
            }
        }
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }

    public static void syncLearnedMethods(ServerPlayer player) {
        if (player != null) {
            sanitizeMethodLayers(player);
        }
        SyncLearnedMethodsPacket.send(player);
    }

    /**
     * One-shot clamp for old fixed-9-layer NBT: drop unknown methods, clamp layers to catalog max,
     * and drop zero/negative layer entries for unlearned keys.
     */
    public static int sanitizeMethodLayers(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        CompoundTag learned = player.getPersistentData().getCompound(LEARNED_METHODS_TAG).copy();
        CompoundTag layers = player.getPersistentData().getCompound(METHOD_LAYERS_TAG).copy();
        int changed = 0;
        // Remove layer entries for methods that are no longer learned / unknown.
        for (String key : List.copyOf(layers.getAllKeys())) {
            String id = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
            if (id.isBlank() || !learned.getBoolean(id) || findMethod(id) == null) {
                layers.remove(key);
                changed++;
                continue;
            }
            int max = maxMethodLayer(id);
            int raw = layers.getInt(key);
            int clamped = Math.max(1, Math.min(max, raw <= 0 ? 1 : raw));
            if (clamped != raw) {
                layers.putInt(key, clamped);
                changed++;
            } else if (!id.equals(key)) {
                layers.remove(key);
                layers.putInt(id, clamped);
                changed++;
            }
        }
        // Ensure every learned method has a layer entry.
        for (String key : learned.getAllKeys()) {
            if (!learned.getBoolean(key)) {
                continue;
            }
            String id = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
            if (id.isBlank() || findMethod(id) == null) {
                continue;
            }
            if (!layers.contains(id)) {
                layers.putInt(id, 1);
                changed++;
            }
        }
        if (changed > 0) {
            player.getPersistentData().put(METHOD_LAYERS_TAG, layers);
        }
        return changed;
    }

    /**
     * Wave474: grant starter method when a player becomes outer disciple of a sect.
     * Returns method id granted, or empty if none/already known.
     */
    public static Optional<String> grantSectStarterMethod(ServerPlayer player, String sectId) {
        return grantSectSpecialtyMethods(player, sectId, SectContributionService.STAGE_OUTER_DISCIPLE)
                .stream().findFirst();
    }

    /**
     * Grant all authored specialty methods unlocked at the player's current sect stage.
     * Calling this repeatedly is safe and backfills existing members after data upgrades.
     */
    public static List<String> grantSectSpecialtyMethods(ServerPlayer player, String sectId, int stage) {
        if (player == null || sectId == null || sectId.isBlank()) {
            return List.of();
        }
        com.xunxian.seekingimmortals.sect.SectMasterDataService.Specialty specialty =
                com.xunxian.seekingimmortals.sect.SectMasterDataService.specialty(sectId).orElse(null);
        if (specialty == null || specialty.methodGrants().isEmpty()) {
            return List.of();
        }
        List<String> granted = new ArrayList<>();
        for (com.xunxian.seekingimmortals.sect.SectMasterDataService.MethodGrant grant
                : specialty.methodGrants()) {
            if (grant.stage() > stage || hasLearnedMethod(player, grant.methodId())) {
                continue;
            }
            TextMaterialCatalogService.MethodEntry method = TextMaterialCatalogService.builtin()
                    .findMethod(grant.methodId()).orElse(null);
            if (method == null || !grantKnownMethodIfEligible(player, method)) {
                continue;
            }
            String display = methodDisplay(method);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.sect_granted", display, specialty.display()), true);
            granted.add(method.id());
        }
        return List.copyOf(granted);
    }

    private static int grantUnlockMethods(ServerPlayer player, java.util.List<String> unlocks) {
        if (player == null || unlocks == null || unlocks.isEmpty()) {
            return 0;
        }
        int granted = 0;
        for (String raw : unlocks) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String token = raw.trim().toLowerCase(Locale.ROOT);
            TextMaterialCatalogService.MethodEntry method = TextMaterialCatalogService.builtin()
                    .findMethod(token).orElse(null);
            if (method != null && grantKnownMethodIfEligible(player, method)) {
                granted++;
            }
        }
        return granted;
    }

    private static boolean canGrantUnlockMethods(ServerPlayer player, List<String> unlocks, Realm currentRealm) {
        if (unlocks == null || unlocks.isEmpty()) {
            return true;
        }
        for (String raw : unlocks) {
            TextMaterialCatalogService.MethodEntry method = TextMaterialCatalogService.builtin()
                    .findMethod(raw).orElse(null);
            if (method == null || hasLearnedMethod(player, method.id())) {
                continue;
            }
            PlayerCultivation cultivation = CultivationHelper.get(player).orElse(null);
            MethodLearnGate gate = evaluateLearnGate(method, cultivation != null ? cultivation : null,
                    prerequisiteId -> getMethodLayer(player, prerequisiteId),
                    cultivation != null ? cultivation.getSevenMysteriesQuest() : null);
            // Pure realm fallback for unit-safe unlock previews when capability is absent.
            if (cultivation == null) {
                gate = evaluateLearnGate(method, currentRealm,
                        prerequisiteId -> getMethodLayer(player, prerequisiteId));
            }
            if (!gate.isAllowed()) {
                displayLearnGateFailure(player, method, gate);
                return false;
            }
            com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.GateResult conflict =
                    com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.canLearnMethod(player, method.id());
            if (!conflict.allowed()) {
                if (conflict.messageKey() != null && !conflict.messageKey().isBlank()) {
                    player.displayClientMessage(Component.translatable(conflict.messageKey(), conflict.args()), false);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Wave475: when a player learns techniques from a technique-manual source string,
     * also mark matching cultivation methods as learned for TechniqueGate.
     */
    public static int grantMethodsFromTechniqueSource(ServerPlayer player, String source) {
        if (player == null || source == null || source.isBlank()) {
            return 0;
        }
        String blob = source.trim().toLowerCase(Locale.ROOT);
        int granted = 0;
        // Direct known method ids appearing in source text.
        for (TextMaterialCatalogService.MethodEntry method
                : TextMaterialCatalogService.builtin().methods().values()) {
            String mid = method.id() == null ? "" : method.id().toLowerCase(Locale.ROOT);
            if (mid.isBlank()) {
                continue;
            }
            if (blob.contains(mid) || blob.contains(mid.replace('_', ' '))) {
                if (grantKnownMethodIfEligible(player, method)) {
                    granted++;
                }
            }
        }
        // Keyword map for common Chinese/English source labels.
        String mapped = methodFromSourceKeywords(blob);
        TextMaterialCatalogService.MethodEntry mappedMethod = TextMaterialCatalogService.builtin()
                .findMethod(mapped).orElse(null);
        if (mappedMethod != null && grantKnownMethodIfEligible(player, mappedMethod)) {
            granted++;
        }
        return granted;
    }

    /**
     * Pure realm/prerequisite gate used by unit tests and capability-free previews.
     * Player-facing learning uses the cultivation-aware overload so root/faction gates apply.
     */
    static MethodLearnGate evaluateLearnGate(TextMaterialCatalogService.MethodEntry method, Realm currentRealm,
                                             Function<String, Integer> layerLookup) {
        return evaluateLearnGate(method, currentRealm, layerLookup, null, null);
    }

    static MethodLearnGate evaluateLearnGate(TextMaterialCatalogService.MethodEntry method,
                                             PlayerCultivation cultivation,
                                             Function<String, Integer> layerLookup,
                                             QuestProgress progress) {
        Realm realm = cultivation == null ? null : cultivation.getRealm();
        return evaluateLearnGate(method, realm, layerLookup, cultivation, progress);
    }

    private static MethodLearnGate evaluateLearnGate(TextMaterialCatalogService.MethodEntry method,
                                                     Realm currentRealm,
                                                     Function<String, Integer> layerLookup,
                                                     PlayerCultivation cultivation,
                                                     QuestProgress progress) {
        if (method == null || currentRealm == null) {
            return MethodLearnGate.deny(MethodLearnFailure.INVALID_REALM);
        }
        if (!method.realmMin().isBlank()
                && !WorldpackGameplayService.meetsMinRealm(currentRealm, method.realmMin())) {
            return MethodLearnGate.deny(MethodLearnFailure.REALM_TOO_LOW);
        }
        if (!method.realmMaxLearn().isBlank()) {
            Realm maxRealm = Realm.fromDesignId(method.realmMaxLearn());
            if (maxRealm == null) {
                return MethodLearnGate.deny(MethodLearnFailure.INVALID_REALM);
            }
            if (currentRealm.ordinal() > maxRealm.ordinal()) {
                return MethodLearnGate.deny(MethodLearnFailure.REALM_TOO_HIGH);
            }
        }
        Function<String, Integer> lookup = layerLookup == null ? ignored -> 0 : layerLookup;
        for (String prerequisite : method.prerequisiteMethods()) {
            int currentLayer = Math.max(0, lookup.apply(prerequisite));
            int requiredLayer = Math.max(1,
                    method.prerequisiteMethodLayers().getOrDefault(prerequisite, 1));
            if (currentLayer <= 0) {
                return new MethodLearnGate(MethodLearnFailure.PREREQUISITE_MISSING,
                        prerequisite, requiredLayer, currentLayer);
            }
            if (currentLayer < requiredLayer) {
                return new MethodLearnGate(MethodLearnFailure.PREREQUISITE_LAYER,
                        prerequisite, requiredLayer, currentLayer);
            }
        }

        // Extended authored gates. Empty requirements remain permissive.
        if (cultivation != null) {
            if (!method.requiredSpiritRoots().isEmpty()
                    && !ProgressionGateApi.meetsAnyRoot(cultivation, method.requiredSpiritRoots())) {
                return MethodLearnGate.deny(MethodLearnFailure.ROOT_MISMATCH);
            }
            if (!method.requiredConstitution().isBlank()
                    && !ProgressionGateApi.meetsConstitution(cultivation, method.requiredConstitution())) {
                return MethodLearnGate.deny(MethodLearnFailure.CONSTITUTION_MISMATCH,
                        method.requiredConstitution());
            }
            if (!method.requiredRace().isBlank()
                    && !ProgressionGateApi.meetsRace(cultivation, method.requiredRace())) {
                return MethodLearnGate.deny(MethodLearnFailure.RACE_MISMATCH, method.requiredRace());
            }
            if (!method.mustConvertAfter().isBlank()) {
                Realm convertAfter = Realm.fromDesignId(method.mustConvertAfter());
                if (convertAfter != null && currentRealm.ordinal() > convertAfter.ordinal()) {
                    return MethodLearnGate.deny(MethodLearnFailure.CONVERT_REQUIRED,
                            method.mustConvertAfter());
                }
            }
        }
        if (!method.requiredFaction().isBlank()) {
            String requiredFaction = SectDefinitionService.canonicalizeSectId(method.requiredFaction());
            String currentFaction = progress == null ? ""
                    : SectDefinitionService.canonicalizeSectId(progress.getSectId());
            // Empty membership keeps the manual/legacy path open. A different live sect hard-blocks.
            // Rank is only enforced when the player already belongs to the authored faction.
            if (!requiredFaction.isBlank() && !currentFaction.isBlank()
                    && !requiredFaction.equals(currentFaction)) {
                return MethodLearnGate.deny(MethodLearnFailure.FACTION_MISMATCH, requiredFaction);
            }
            if (!requiredFaction.isBlank()
                    && requiredFaction.equals(currentFaction)
                    && !method.factionRelationMin().isBlank()
                    && !meetsFactionRelation(progress, method.factionRelationMin())) {
                return MethodLearnGate.deny(MethodLearnFailure.FACTION_RANK_TOO_LOW,
                        method.factionRelationMin());
            }
        }
        return MethodLearnGate.allowed();
    }

    private static boolean meetsFactionRelation(QuestProgress progress, String relationMin) {
        if (progress == null || relationMin == null || relationMin.isBlank()) {
            return true;
        }
        int stage = Math.max(0, progress.getSectQuestStage());
        String key = relationMin.trim().toLowerCase(Locale.ROOT);
        int required = switch (key) {
            case "candidate", "applicant", "outer_candidate" -> 1;
            case "outer", "outer_disciple" -> SectContributionService.STAGE_OUTER_DISCIPLE;
            case "inner", "inner_disciple" -> SectContributionService.STAGE_INNER_DISCIPLE;
            case "core", "core_disciple", "true_disciple" -> Math.max(SectContributionService.STAGE_INNER_DISCIPLE + 1, 5);
            case "elder", "deacon" -> Math.max(SectContributionService.STAGE_INNER_DISCIPLE + 2, 6);
            default -> 0;
        };
        return stage >= required;
    }

    private static boolean grantKnownMethodIfEligible(ServerPlayer player,
                                                       TextMaterialCatalogService.MethodEntry method) {
        if (player == null || method == null || hasLearnedMethod(player, method.id())) {
            return false;
        }
        boolean[] granted = {false};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            MethodLearnGate gate = evaluateLearnGate(method, cultivation,
                    prerequisiteId -> getMethodLayer(player, prerequisiteId),
                    cultivation.getSevenMysteriesQuest());
            if (!gate.isAllowed()) {
                return;
            }
            com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.GateResult conflict =
                    com.xunxian.seekingimmortals.skill.ManualConflictMatrixService.canLearnMethod(player, method.id());
            if (!conflict.allowed()) {
                return;
            }
            markLearnedMethod(player, method.id());
            setMethodLayer(player, method.id(), 1);
            MethodLayerTechniqueService.grantForMethodLayer(player, method.id(), 1);
            DetailedQuestProofService.recordMethodLayerReached(player, method.id());
            granted[0] = true;
        });
        return granted[0];
    }

    private static void displayLearnGateFailure(ServerPlayer player,
                                                TextMaterialCatalogService.MethodEntry method,
                                                MethodLearnGate gate) {
        String methodName = methodDisplay(method);
        switch (gate.failure()) {
            case REALM_TOO_LOW -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.realm_too_low", methodName,
                    com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(method.realmMin())), false);
            case REALM_TOO_HIGH -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.realm_too_high", methodName,
                    com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(method.realmMaxLearn())), false);
            case PREREQUISITE_MISSING -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.prerequisite_missing", methodName,
                    methodDisplay(gate.requiredMethod())), false);
            case PREREQUISITE_LAYER -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.prerequisite_layer", methodName,
                    methodDisplay(gate.requiredMethod()), gate.requiredLayer(), gate.currentLayer()), false);
            case ROOT_MISMATCH -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.root_mismatch", methodName,
                    rootsDisplay(method.requiredSpiritRoots())), false);
            case CONSTITUTION_MISMATCH -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.constitution_mismatch", methodName,
                    requirementDisplay(gate.requiredMethod(), "相应体质")), false);
            case RACE_MISMATCH -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.race_mismatch", methodName,
                    requirementDisplay(gate.requiredMethod(), "相应种族")), false);
            case FACTION_MISMATCH -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.faction_mismatch", methodName,
                    requirementDisplay(gate.requiredMethod(), "指定宗门")), false);
            case FACTION_RANK_TOO_LOW -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.faction_rank_too_low", methodName,
                    requirementDisplay(gate.requiredMethod(), "更高宗门身份")), false);
            case CONVERT_REQUIRED -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.convert_required", methodName,
                    com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(gate.requiredMethod())), false);
            case INVALID_REALM -> player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.method.invalid_realm_gate", methodName), false);
            case NONE -> {
            }
        }
    }

    private static String methodDisplay(String methodId) {
        return TextMaterialCatalogService.builtin().findMethod(methodId)
                .map(ManualCatalogService::methodDisplay)
                .orElseGet(() -> {
                    String id = methodId == null ? "" : methodId.trim().toLowerCase(Locale.ROOT);
                    String key = "item.seeking_immortals." + id;
                    return PlayerDisplayText.hasTranslation(key)
                            ? Component.translatable(key).getString()
                            : "未知功法";
                });
    }

    private static String methodDisplay(TextMaterialCatalogService.MethodEntry method) {
        if (method == null) {
            return "未知功法";
        }
        String display = method.display();
        if (PlayerDisplayText.isSafe(display)) {
            return display.trim();
        }
        String id = method.id() == null ? "" : method.id().trim().toLowerCase(Locale.ROOT);
        String key = "item.seeking_immortals." + id;
        return PlayerDisplayText.hasTranslation(key)
                ? Component.translatable(key).getString()
                : "未知功法";
    }

    private static String schoolDisplay(String school) {
        if (PlayerDisplayText.isSafe(school)) {
            return school.trim();
        }
        String code = school == null ? "" : school.trim().toLowerCase(Locale.ROOT);
        return switch (code) {
            case "sword" -> "剑道";
            case "elemental", "elemental_fire", "elemental_ice", "elemental_water" -> "五行术道";
            case "body" -> "炼体";
            case "talisman" -> "符箓";
            case "formation" -> "阵法";
            case "puppet" -> "傀儡";
            case "illusion" -> "幻术";
            case "movement" -> "身法";
            case "divine_sense" -> "神识";
            case "recovery" -> "恢复";
            case "demonic", "ghost", "xuan_yin", "blood" -> "魔道";
            case "craft_alchemy" -> "炼丹";
            case "craft_artifact" -> "炼器";
            case "craft_appraise" -> "鉴宝";
            case "mixed", "generic", "misc", "" -> "综合";
            default -> "综合";
        };
    }

    private static String rootsDisplay(List<String> roots) {
        if (roots == null || roots.isEmpty()) {
            return "相应灵根";
        }
        List<String> labels = new ArrayList<>();
        for (String root : roots) {
            String code = root == null ? "" : root.trim().toLowerCase(Locale.ROOT);
            labels.add(switch (code) {
                case "metal" -> "金灵根";
                case "wood" -> "木灵根";
                case "water" -> "水灵根";
                case "fire" -> "火灵根";
                case "earth" -> "土灵根";
                case "ice" -> "冰灵根";
                case "thunder" -> "雷灵根";
                case "wind" -> "风灵根";
                case "yin", "dark" -> "阴灵根";
                case "yang", "light" -> "阳灵根";
                default -> PlayerDisplayText.isSafe(root) ? root.trim() : "特殊灵根";
            });
        }
        return String.join("、", labels);
    }

    private static String requirementDisplay(String raw, String fallback) {
        return PlayerDisplayText.isSafe(raw) ? raw.trim() : fallback;
    }

    private static List<String> softMethodGrants(String manualId) {
        String id = manualId == null ? "" : manualId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "alchemy_manual_low", "huangfeng_alchemy_scripture" -> List.of("huangfeng_alchemy_scripture");
            case "refinement_manual_high", "refinement_manual_low", "refinement_manual_mid",
                    "refinement_manual_ancient", "silver_giant_sword_blueprint" -> List.of("artifact_refining_basic");
            case "manual_ancient_puppet_method", "ancient_puppet_method" -> List.of("qianzhu_puppet_art");
            case "beast_taming_manual" -> List.of("beast_taming_basic");
            case "demonic_manual_low" -> List.of("demonic_blood_art_generic");
            case "fashi_art_fragment", "fashi_array_manual", "formation_scroll_mid" -> List.of("sect_specialty_formation");
            case "illusion_scroll" -> List.of("yanyue_illusion_art");
            case "shape_shift_scroll" -> List.of("beast_transformation_art");
            case "talisman_recipe", "talisman_recipe_mid", "talisman_recipe_high_bundle" -> List.of("tianfu_scripture");
            case "artifact_identify_scroll" -> List.of("treasure_appraisal_art");
            case "void_palace_intel_scroll" -> List.of("kunwu_seal_art");
            case "ghost_cultivation_manual" -> List.of("ghost_nether_art");
            default -> List.of();
        };
    }

    private static String softAlchemyRecipeId(String manualId) {
        String id = manualId == null ? "" : manualId.trim().toLowerCase(Locale.ROOT);
        if (id.startsWith("recipe_")) {
            // recipe_foundation / recipe_bigu → foundation / bigu when present in alchemy catalog.
            return id.substring("recipe_".length());
        }
        if (id.startsWith("recipe")) {
            return id;
        }
        return switch (id) {
            case "alchemy_manual_low" -> "bigu";
            default -> "";
        };
    }

    private static int softForgeGrade(String manualId) {
        String id = manualId == null ? "" : manualId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "refinement_manual_low" -> 1;
            case "refinement_manual_mid" -> 3;
            case "refinement_manual_high" -> 4;
            case "refinement_manual_ancient" -> 5;
            case "silver_giant_sword_blueprint" -> 2;
            default -> 0;
        };
    }

    private static void recordSoftForgeGrade(ServerPlayer player, String manualId, int grade) {
        if (player == null || manualId == null || manualId.isBlank() || grade <= 0) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(SOFT_FORGE_GRADES_TAG).copy();
        String key = manualId.trim().toLowerCase(Locale.ROOT);
        tag.putInt(key, Math.max(tag.getInt(key), grade));
        player.getPersistentData().put(SOFT_FORGE_GRADES_TAG, tag);
    }

    private static String methodFromSourceKeywords(String blob) {
        if (blob.contains("长春") || blob.contains("changchun") || blob.contains("黄枫")) {
            return "changchun_gong";
        }
        if (blob.contains("梵圣") || blob.contains("fansheng")) {
            return "fansheng_zhenmogong";
        }
        if (blob.contains("托天") || blob.contains("tuotian")
                || blob.contains("乱星海魔修") || blob.contains("top_demonic")) {
            return "tuotian_mogong";
        }
        if (blob.contains("明王") || blob.contains("mingwang") || blob.contains("大晋")) {
            return "mingwang_jue";
        }
        if (blob.contains("真言门") || blob.contains("true_word") || blob.contains("幻世")) {
            return "great_five_elements_world_art";
        }
        if (blob.contains("青元") || blob.contains("qingyuan") || blob.contains("剑诀") || blob.contains("剑宗") || blob.contains("巨剑") || blob.contains("凌霄")) {
            return "qingyuan_sword_art";
        }
        if (blob.contains("玄阴") || blob.contains("xuan_yin") || blob.contains("xuanyin")) {
            return "xuan_yin_art";
        }
        if (blob.contains("大衍") || blob.contains("dayan") || blob.contains("傀儡")) {
            return "dayan_art";
        }
        if (blob.contains("烈焰") || blob.contains("lieyan") || blob.contains("魔焰")) {
            return "lieyan_gong";
        }
        if (blob.contains("鬼灵") || blob.contains("guiling") || blob.contains("摄魂")) {
            return "guiling_soul_art";
        }
        if (blob.contains("天魔") || blob.contains("tianmo")) {
            return "tianmo_body_art";
        }
        if (blob.contains("血巫") || blob.contains("xuewu")) {
            return "xuewu_blood_art";
        }
        if (blob.contains("万狐") || blob.contains("wanhu") || blob.contains("千幻")) {
            return "wanhu_phantom_art";
        }
        if (blob.contains("星宫") || blob.contains("star_palace")) {
            return "star_palace_art";
        }
        if (blob.contains("逆星") || blob.contains("inverse_star")) {
            return "inverse_star_art";
        }
        if (blob.contains("掩月") || blob.contains("yanyue") || blob.contains("幻术")) {
            return "yanyue_illusion_art";
        }
        if (blob.contains("清虚") || blob.contains("qingxu") || blob.contains("符箓")) {
            return "qingxu_pure_tao_art";
        }
        if (blob.contains("千竹") || blob.contains("qianzhu")) {
            return "qianzhu_puppet_art";
        }
        if (blob.contains("御灵") || blob.contains("yuling") || blob.contains("灵兽")) {
            return "yuling_beast_puppet_art";
        }
        if (blob.contains("合欢") || blob.contains("hehuan")) {
            return "hehuan_mind_art";
        }
        if (blob.contains("七玄") || blob.contains("qixuan")) {
            return "qixuan_mortal_art";
        }
        if (blob.contains("木兰") || blob.contains("mulan")) {
            return "mulan_wind_spirit_art";
        }
        if (blob.contains("天岚") || blob.contains("tianlan")) {
            return "tianlan_holy_beast_art";
        }
        if (blob.contains("昆吾") || blob.contains("kunwu") || blob.contains("阵法") || blob.contains("seal")) {
            return "kunwu_ice_guard_art";
        }
        if (blob.contains("佛") || blob.contains("buddhist") || blob.contains("金刚")) {
            return "dajin_clan_ancestor_art";
        }
        if (blob.contains("青萝") || blob.contains("qingluo") || blob.contains("毒")) {
            return "qingluo_poison_art";
        }
        if (blob.contains("乱星") || blob.contains("chaotic")) {
            return "chaotic_sea_nav_art";
        }
        return "";
    }

    private static void applyInsight(ServerPlayer player, TextMaterialCatalogService.ManualEntry manual) {
        String type = manual.type() == null ? "" : manual.type().toLowerCase(Locale.ROOT);
        int duration = 20 * 60 * 5;
        if (type.contains("refinement") || type.contains("forge")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
        } else if (type.contains("puppet")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
        } else if (type.contains("ghost") || type.contains("cultivation")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, Math.min(duration, 20 * 90), 0));
        }
    }
}
