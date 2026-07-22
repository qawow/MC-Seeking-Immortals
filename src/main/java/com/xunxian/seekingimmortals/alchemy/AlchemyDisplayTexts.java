package com.xunxian.seekingimmortals.alchemy;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Resolves recipe ids and output ids to a player-facing formula name. */
public final class AlchemyDisplayTexts {
    private AlchemyDisplayTexts() {}

    public static Component recipe(String rawId) {
        String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            return Component.translatable("text.seeking_immortals.unknown_formula");
        }
        String directKey = "alchemy_recipe.seeking_immortals." + id;
        if (PlayerDisplayText.hasTranslation(directKey)) {
            return Component.translatable(directKey);
        }
        String itemKey = "item.seeking_immortals." + id;
        if (PlayerDisplayText.hasTranslation(itemKey)) {
            return Component.translatable(itemKey);
        }
        for (ExtendedCatalogService.IdDisplay entry : ExtendedCatalogService.builtin().alchemyRecipes().values()) {
            if (id.equalsIgnoreCase(entry.extra()) && PlayerDisplayText.isSafe(entry.display())) {
                return Component.literal(entry.display());
            }
        }
        return Component.translatable("text.seeking_immortals.unknown_formula");
    }

    public static Component source(AlchemyFormulaSource source) {
        if (source == null) {
            return Component.translatable("text.seeking_immortals.unknown_formula");
        }
        String key = "alchemy_formula_source.seeking_immortals." + source.id();
        return PlayerDisplayText.translatedOr(key, "text.seeking_immortals.unknown_formula");
    }
}
