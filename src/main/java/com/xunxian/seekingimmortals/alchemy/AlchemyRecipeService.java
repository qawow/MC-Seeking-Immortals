package com.xunxian.seekingimmortals.alchemy;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.item.material.BaseMaterialItem;
import com.xunxian.seekingimmortals.item.material.MaterialRarity;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class AlchemyRecipeService {
    private AlchemyRecipeService() {}

    public static boolean canCraft(ServerPlayer player, AlchemyRecipe recipe) {
        return hasIngredients(player.getInventory(), recipe)
                && CultivationHelper.get(player).map(cultivation -> cultivation.getSpiritualPower() >= recipe.manaCost()).orElse(false);
    }

    public static boolean consumeInputs(ServerPlayer player, AlchemyRecipe recipe) {
        if (!canCraft(player, recipe)) return false;
        if (!player.getAbilities().instabuild) {
            for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
                removeItems(player.getInventory(), ingredient);
            }
            CultivationHelper.get(player).ifPresent(cultivation -> cultivation.consumeSpiritualPower(recipe.manaCost()));
        }
        return true;
    }

    public static boolean consumeHalfInputs(ServerPlayer player, AlchemyRecipe recipe) {
        if (!hasHalfIngredients(player.getInventory(), recipe)) return false;
        int halfMana = Math.max(1, recipe.manaCost() / 2);
        if (!player.getAbilities().instabuild
                && CultivationHelper.get(player).map(cultivation -> cultivation.getSpiritualPower() < halfMana).orElse(true)) return false;
        if (!player.getAbilities().instabuild) {
            for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
                removeItems(player.getInventory(), halfRequirement(ingredient));
            }
            CultivationHelper.get(player).ifPresent(cultivation -> cultivation.consumeSpiritualPower(halfMana));
        }
        return true;
    }

    public static double successRate(ServerLevel level, ServerPlayer player, AlchemyRecipe recipe) {
        return successRate(level, player, recipe, 1, 1, AlchemyFormulaSource.PAPER);
    }

    public static double successRate(ServerLevel level, ServerPlayer player, AlchemyRecipe recipe,
                                     int furnaceTier, int fireTier, AlchemyFormulaSource formulaSource) {
        double alchemyBonus = getAlchemySkillBonus(player);
        double furnaceBonus = furnaceTier >= recipe.requiredFurnaceTier()
                ? Math.min(0.12D, (furnaceTier - recipe.requiredFurnaceTier()) * 0.04D + 0.04D)
                : -0.20D;
        int fireDelta = fireTier - recipe.idealFireTier();
        double fireBonus = fireDelta == 0 ? 0.06D : fireDelta < 0 ? -0.08D * Math.abs(fireDelta) : -0.04D * fireDelta;
        double materialBonus = getMaterialQualityBonus(recipe);
        double controlBonus = getFireControlBonus(player, recipe);
        double auraBonus = SpiritualAuraManager.getAuraInfo(level, player.blockPosition()).leyline() ? 0.05D : 0.0D;
        double controlledPenalty = recipe.controlled() && !formulaSource.isSectAuthorized()
                ? formulaSource.controlledSuccessModifier()
                : 0.0D;
        return Math.min(0.95D, Math.max(0.03D, recipe.successRate() + alchemyBonus + furnaceBonus + fireBonus + materialBonus + controlBonus + auraBonus + controlledPenalty));
    }

    public static double explosionChance(ServerPlayer player, AlchemyRecipe recipe) {
        return explosionChance(player, recipe, 1, 1, 1, AlchemyFormulaSource.PAPER);
    }

    public static double explosionChance(ServerPlayer player, AlchemyRecipe recipe,
                                         int furnaceTier, int lidTier, int fireTier, AlchemyFormulaSource formulaSource) {
        double chance = recipe.explosionChance() - getAlchemySkillBonus(player) * 0.5D;
        if (fireTier > recipe.idealFireTier()) chance += (fireTier - recipe.idealFireTier()) * 0.04D;
        if (!hasRealmControl(player, recipe)) chance += 0.08D;
        if (furnaceTier < recipe.requiredFurnaceTier()) chance += 0.06D;
        if (fireTier > lidTier) chance += 0.10D;
        if (recipe.controlled() && !formulaSource.isSectAuthorized()) {
            chance += formulaSource.controlledExplosionModifier();
        }
        return Math.min(0.90D, Math.max(0.0D, chance));
    }

    public static boolean hasRealmControl(ServerPlayer player, AlchemyRecipe recipe) {
        return CultivationHelper.get(player)
                .map(cultivation -> cultivation.getRealm().ordinal() >= recipe.minControlRealm().ordinal())
                .orElse(false);
    }

    public static String missingSummary(ServerPlayer player, AlchemyRecipe recipe) {
        StringBuilder builder = new StringBuilder();
        Inventory inventory = player.getInventory();
        for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
            int owned = countItems(inventory, ingredient);
            if (owned < ingredient.count()) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(ComponentName.item(ingredient.item().getDescriptionId()))
                        .append(" ")
                        .append(owned)
                        .append("/")
                        .append(ingredient.count());
            }
        }
        int mana = CultivationHelper.get(player).map(PlayerCultivation::getSpiritualPower).orElse(0);
        if (mana < recipe.manaCost()) {
            if (builder.length() > 0) builder.append(", ");
            builder.append("mana ").append(mana).append("/").append(recipe.manaCost());
        }
        return builder.length() == 0 ? "" : builder.toString();
    }

    private static boolean hasIngredients(Inventory inventory, AlchemyRecipe recipe) {
        for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
            if (countItems(inventory, ingredient) < ingredient.count()) return false;
        }
        return true;
    }

    private static boolean hasHalfIngredients(Inventory inventory, AlchemyRecipe recipe) {
        for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
            AlchemyRecipe.IngredientRequirement half = halfRequirement(ingredient);
            if (countItems(inventory, half) < half.count()) return false;
        }
        return true;
    }

    private static AlchemyRecipe.IngredientRequirement halfRequirement(AlchemyRecipe.IngredientRequirement ingredient) {
        return new AlchemyRecipe.IngredientRequirement(ingredient.item(), Math.max(1, (ingredient.count() + 1) / 2));
    }

    private static int countItems(Inventory inventory, AlchemyRecipe.IngredientRequirement ingredient) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(ingredient.item())) count += stack.getCount();
        }
        return count;
    }

    private static void removeItems(Inventory inventory, AlchemyRecipe.IngredientRequirement ingredient) {
        int remaining = ingredient.count();
        for (ItemStack stack : inventory.items) {
            if (!stack.is(ingredient.item())) continue;
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (remaining <= 0) return;
        }
    }

    private static double getFireControlBonus(ServerPlayer player, AlchemyRecipe recipe) {
        return CultivationHelper.get(player)
                .map(cultivation -> {
                    int delta = cultivation.getRealm().ordinal() - recipe.minControlRealm().ordinal();
                    if (delta < 0) return -0.18D;
                    return Math.min(0.10D, delta * 0.03D);
                })
                .orElse(-0.18D);
    }

    private static double getMaterialQualityBonus(AlchemyRecipe recipe) {
        double bonus = 0.0D;
        for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
            if (ingredient.item() instanceof BaseMaterialItem material) {
                MaterialRarity rarity = material.getRarity();
                bonus += switch (rarity) {
                    case COMMON -> 0.00D;
                    case UNCOMMON -> 0.01D;
                    case RARE -> 0.025D;
                    case EPIC -> 0.04D;
                    case LEGENDARY -> 0.06D;
                };
            }
        }
        return Math.min(0.12D, bonus);
    }

    /**
     * 炼丹技能等级带来的成功率加成（H13）。
     * <p>每级 +0.02，上限 +0.20；未学炼丹术视为 0。
     */
    public static double getAlchemySkillBonus(ServerPlayer player) {
        // Wave490: unify onto LifeSkillService bonus constants (no parallel table).
        return com.xunxian.seekingimmortals.skill.LifeSkillService.successBonus(player, SkillType.ALCHEMY);
    }

    private static final class ComponentName {
        static String item(String descriptionId) {
            return net.minecraft.network.chat.Component.translatable(descriptionId).getString();
        }
    }
}
