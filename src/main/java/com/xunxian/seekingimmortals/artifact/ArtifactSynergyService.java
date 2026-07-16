package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import com.xunxian.seekingimmortals.item.CatalogCarrierItem;
import com.xunxian.seekingimmortals.item.FlyingArtifactItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * M15 多法宝协同：读取 item_synergy / artifact_combos，
 * 在战斗钩子中叠加 attack/defense/control 倍率（不绕过伤害管线）。
 */
public final class ArtifactSynergyService {
    private ArtifactSynergyService() {}

    public record SynergyBonus(
            double attackMultiplier,
            double defenseMultiplier,
            double controlBonus,
            double spiritClarity,
            List<String> activeRelations
    ) {
        public static SynergyBonus none() {
            return new SynergyBonus(1.0D, 1.0D, 0.0D, 0.0D, List.of());
        }

        public SynergyBonus {
            activeRelations = List.copyOf(activeRelations);
        }
    }

    public static SynergyBonus evaluate(Player player) {
        if (player == null) {
            return SynergyBonus.none();
        }
        Set<String> held = collectArtifactIds(player);
        if (held.isEmpty()) {
            return SynergyBonus.none();
        }
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        double attack = 1.0D;
        double defense = 1.0D;
        double control = 0.0D;
        double clarity = 0.0D;
        List<String> active = new ArrayList<>();

        for (ArtifactDataService.ArtifactCombo combo : snap.artifactCombos()) {
            if (containsAll(held, combo.artifacts())) {
                String bonus = combo.bonus() == null ? "" : combo.bonus().toLowerCase(Locale.ROOT);
                active.add("combo:" + bonus);
                switch (bonus) {
                    case "defense_stack" -> defense += 0.12D;
                    case "multi_projectile_synergy" -> attack += 0.15D;
                    case "puppet_duration" -> control += 0.10D;
                    case "illusion_resist_while_channeling" -> clarity += 0.20D;
                    default -> attack += 0.05D;
                }
            }
        }

        for (ArtifactDataService.SynergyRule rule : snap.synergies()) {
            if (!containsAll(held, rule.items())) {
                continue;
            }
            // 仅当条目双方都能在法宝 id 空间命中时计为法宝协同（跳过纯丹药/符箓组合）。
            if (!touchesArtifactCatalog(rule.items(), snap)) {
                continue;
            }
            String relation = rule.relation() == null ? "" : rule.relation().toLowerCase(Locale.ROOT);
            active.add("synergy:" + relation);
            switch (relation) {
                case "stack_defense" -> defense += 0.08D;
                case "stack_soul_clarity", "anti_illusion" -> clarity += 0.12D;
                case "stack" -> attack += 0.05D;
                case "controls", "counter" -> control += 0.08D;
                case "debuff_tradeoff" -> {
                    attack += 0.10D;
                    defense -= 0.04D;
                }
                default -> {
                    // cosmetic / quest / unlock 等不进战斗数值
                }
            }
        }

        defense = Math.max(0.5D, defense);
        attack = Math.max(0.5D, attack);
        return new SynergyBonus(attack, defense, control, clarity, active);
    }

    public static double outgoingDamageMultiplier(Player player) {
        return evaluate(player).attackMultiplier();
    }

    public static double incomingDamageMultiplier(Player player) {
        SynergyBonus bonus = evaluate(player);
        // defense 1.12 → 受伤约 0.89
        return 1.0D / Math.max(0.5D, bonus.defenseMultiplier());
    }

    public static Set<String> collectArtifactIds(Player player) {
        Set<String> ids = new LinkedHashSet<>();
        if (player == null) {
            return ids;
        }
        addIfArtifact(ids, player.getMainHandItem());
        addIfArtifact(ids, player.getOffhandItem());
        for (ItemStack stack : player.getInventory().items) {
            addIfArtifact(ids, stack);
        }
        if (ModList.get().isLoaded("curios")) {
            try {
                CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                        handler.getCurios().forEach((slot, stacks) -> {
                            for (int i = 0; i < stacks.getSlots(); i++) {
                                addIfArtifact(ids, stacks.getStacks().getStackInSlot(i));
                            }
                        }));
            } catch (Throwable ignored) {
                // Curios optional path
            }
        }
        return ids;
    }

    private static void addIfArtifact(Set<String> ids, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof ArtifactCatalogItem catalog) {
            ids.add(catalog.artifactId());
            return;
        }
        if (stack.getItem() instanceof FlyingArtifactItem flying) {
            ids.add(flying.artifactId());
            return;
        }
        if (stack.getItem() instanceof CatalogCarrierItem carrier) {
            String id = carrier.catalogId();
            if (id != null && ArtifactDataService.builtin().findArtifact(id).isPresent()) {
                ids.add(id);
            }
        }
    }

    private static boolean containsAll(Set<String> held, List<String> required) {
        if (required == null || required.isEmpty()) {
            return false;
        }
        for (String id : required) {
            if (id == null || !held.contains(id)) {
                return false;
            }
        }
        return true;
    }

    private static boolean touchesArtifactCatalog(List<String> items, ArtifactDataService.Snapshot snap) {
        int hits = 0;
        for (String id : items) {
            if (snap.findArtifact(id).isPresent()) {
                hits++;
            }
        }
        return hits >= 1;
    }

    /** 单元测试辅助：直接用 id 集合评估。 */
    public static SynergyBonus evaluateIds(Set<String> held) {
        if (held == null || held.isEmpty()) {
            return SynergyBonus.none();
        }
        ArtifactDataService.Snapshot snap = ArtifactDataService.builtin();
        double attack = 1.0D;
        double defense = 1.0D;
        double control = 0.0D;
        double clarity = 0.0D;
        List<String> active = new ArrayList<>();
        for (ArtifactDataService.ArtifactCombo combo : snap.artifactCombos()) {
            if (containsAll(held, combo.artifacts())) {
                String bonus = combo.bonus() == null ? "" : combo.bonus().toLowerCase(Locale.ROOT);
                active.add("combo:" + bonus);
                switch (bonus) {
                    case "defense_stack" -> defense += 0.12D;
                    case "multi_projectile_synergy" -> attack += 0.15D;
                    case "puppet_duration" -> control += 0.10D;
                    case "illusion_resist_while_channeling" -> clarity += 0.20D;
                    default -> attack += 0.05D;
                }
            }
        }
        for (ArtifactDataService.SynergyRule rule : snap.synergies()) {
            if (!containsAll(held, rule.items()) || !touchesArtifactCatalog(rule.items(), snap)) {
                continue;
            }
            String relation = rule.relation() == null ? "" : rule.relation().toLowerCase(Locale.ROOT);
            active.add("synergy:" + relation);
            switch (relation) {
                case "stack_defense" -> defense += 0.08D;
                case "stack_soul_clarity", "anti_illusion" -> clarity += 0.12D;
                case "stack" -> attack += 0.05D;
                case "controls", "counter" -> control += 0.08D;
                case "debuff_tradeoff" -> {
                    attack += 0.10D;
                    defense -= 0.04D;
                }
                default -> {
                }
            }
        }
        return new SynergyBonus(Math.max(0.5D, attack), Math.max(0.5D, defense), control, clarity, active);
    }
}
