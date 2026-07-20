package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.registry.BulkItemClassifier;
import com.xunxian.seekingimmortals.registry.BulkItemKind;
import com.xunxian.seekingimmortals.structure.FormationItemService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public final class CatalogItemDescriptionService {
    private CatalogItemDescriptionService() {}

    public record Profile(String purposeKey, String interactionKey, String detailKey) {}

    public static boolean appendCatalogDescription(ItemStack stack, List<Component> tooltip, String description) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String id = stack.getItem().builtInRegistryHolder().key().location().getPath();
        ItemCatalogService.CarrierMeta meta = ItemCatalogService.findMeta(id).orElse(null);
        if (meta == null) {
            return false;
        }
        String explicit = description == null ? "" : description.trim();
        if (!isPlaceholder(explicit)) {
            tooltip.add(Component.literal(explicit).withStyle(ChatFormatting.DARK_GRAY));
        }
        Profile profile = profile(meta.id(), meta.category());
        tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_item.purpose",
                Component.translatable(profile.purposeKey())).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_item.interaction",
                Component.translatable(profile.interactionKey())).withStyle(ChatFormatting.GRAY));
        if (!profile.detailKey().isBlank()) {
            tooltip.add(Component.translatable(profile.detailKey()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }

    public static Profile profile(String id, String category) {
        String normalizedId = normalize(id);
        String normalizedCategory = normalize(category);
        String purpose = purposeKey(normalizedId, normalizedCategory);
        BulkItemKind kind = BulkItemClassifier.classify(normalizedId, normalizedCategory);
        String interaction = formationInteractionKey(normalizedId);
        if (interaction.isBlank()) {
            interaction = isFormationComponent(normalizedCategory, purpose)
                    ? "tooltip.seeking_immortals.catalog_item.interaction.material"
                    : switch (kind) {
                        case CONSUMABLE -> "tooltip.seeking_immortals.catalog_item.interaction.consume";
                        case PILL -> "tooltip.seeking_immortals.catalog_item.interaction.pill";
                        case FORMULA -> "tooltip.seeking_immortals.catalog_item.interaction.formula";
                        case MANUAL -> "tooltip.seeking_immortals.catalog_item.interaction.manual";
                        case TALISMAN -> "tooltip.seeking_immortals.catalog_item.interaction.talisman";
                        case EQUIPMENT -> "tooltip.seeking_immortals.catalog_item.interaction.equipment";
                        case ARTIFACT -> "tooltip.seeking_immortals.catalog_item.interaction.artifact";
                        case CARRIER -> carrierInteractionKey(normalizedCategory, purpose);
                    };
        }
        return new Profile(purpose, interaction, detailKey(normalizedId));
    }

    static boolean isPlaceholder(String description) {
        String normalized = normalize(description);
        return normalized.isBlank() || normalized.startsWith("目录载体") || normalized.startsWith("catalog carrier");
    }

    private static String purposeKey(String id, String category) {
        if ("sect_contribution_token".equals(id)) {
            return "tooltip.seeking_immortals.catalog_item.purpose.currency";
        }
        if (containsAny(id, "blueprint", "recipe", "manual", "scroll", "jade_slip", "book", "formula")) {
            return "tooltip.seeking_immortals.catalog_item.purpose.knowledge";
        }
        if (containsAny(id, "ticket", "permit", "token", "pass", "key", "map", "evidence", "decree", "receipt")) {
            return "tooltip.seeking_immortals.catalog_item.purpose.access";
        }
        if (containsAny(id, "formation", "array", "altar")) {
            return "tooltip.seeking_immortals.catalog_item.purpose.formation";
        }
        if (containsAny(id, "herb", "grass", "root", "flower", "mushroom", "fruit", "leaf", "seed")) {
            return "tooltip.seeking_immortals.catalog_item.purpose.alchemy";
        }
        if (containsAny(id, "beast", "fang", "claw", "horn", "hide", "scale", "blood", "bone", "feather")) {
            return "tooltip.seeking_immortals.catalog_item.purpose.beast";
        }
        return switch (category) {
            case "artifact" -> "tooltip.seeking_immortals.catalog_item.purpose.artifact";
            case "equipment" -> "tooltip.seeking_immortals.catalog_item.purpose.equipment";
            case "pill" -> "tooltip.seeking_immortals.catalog_item.purpose.pill";
            case "talisman" -> "tooltip.seeking_immortals.catalog_item.purpose.talisman";
            case "consumable" -> "tooltip.seeking_immortals.catalog_item.purpose.consumable";
            case "currency" -> "tooltip.seeking_immortals.catalog_item.purpose.currency";
            case "manual" -> "tooltip.seeking_immortals.catalog_item.purpose.knowledge";
            case "access_item" -> "tooltip.seeking_immortals.catalog_item.purpose.access";
            default -> "tooltip.seeking_immortals.catalog_item.purpose.crafting";
        };
    }

    private static String carrierInteractionKey(String category, String purpose) {
        if (isFormationComponent(category, purpose)) {
            return "tooltip.seeking_immortals.catalog_item.interaction.material";
        }
        if ("currency".equals(category)) {
            return "tooltip.seeking_immortals.catalog_item.interaction.currency";
        }
        if ("access_item".equals(category) || purpose.endsWith(".access")) {
            return "tooltip.seeking_immortals.catalog_item.interaction.access";
        }
        if ("manual".equals(category) || purpose.endsWith(".knowledge")) {
            return "tooltip.seeking_immortals.catalog_item.interaction.knowledge";
        }
        if ("talisman".equals(category)) {
            return "tooltip.seeking_immortals.catalog_item.interaction.talisman";
        }
        if ("artifact".equals(category) || "equipment".equals(category)) {
            return "tooltip.seeking_immortals.catalog_item.interaction.component";
        }
        return "tooltip.seeking_immortals.catalog_item.interaction.material";
    }

    private static boolean isFormationComponent(String category, String purpose) {
        return purpose.endsWith(".formation") && !"consumable".equals(category);
    }

    private static String formationInteractionKey(String id) {
        return FormationItemService.builtin().find(id)
                .map(behavior -> switch (normalize(behavior.action())) {
                    case "place_block" -> "tooltip.seeking_immortals.catalog_item.interaction.formation_place";
                    case "activate_free_field" -> "tooltip.seeking_immortals.catalog_item.interaction.formation_activate";
                    case "inspect_only" -> "tooltip.seeking_immortals.catalog_item.interaction.formation_inspect";
                    default -> "";
                })
                .orElse("");
    }

    private static String detailKey(String id) {
        return switch (id) {
            case "sect_contribution_token", "teleport_array_ticket", "array_disk_basic",
                    "array_disk_fragment", "crystal_array_disk", "formation_flag_jade",
                    "formation_flag_low", "formation_flag_mid", "formation_flag_post",
                    "immortal_array_disk", "jade_array_disk", "platinum_array_disk",
                    "space_array_disk", "spirit_gathering_array_disk" ->
                    "tooltip.seeking_immortals.catalog_item.detail." + id;
            default -> "";
        };
    }

    private static boolean containsAny(String value, String... parts) {
        for (String part : parts) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
