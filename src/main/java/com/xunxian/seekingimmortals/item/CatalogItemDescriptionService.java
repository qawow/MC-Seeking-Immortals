package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import com.xunxian.seekingimmortals.registry.BulkItemClassifier;
import com.xunxian.seekingimmortals.registry.BulkItemKind;
import com.xunxian.seekingimmortals.structure.FormationItemService;
import com.xunxian.seekingimmortals.structure.MultiblockStructureCatalog;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        String descriptionKey = "tooltip.seeking_immortals.catalog_item.description." + meta.id();
        if (PlayerDisplayText.hasTranslation(descriptionKey)) {
            tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.DARK_GRAY));
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
        // purpose ends with .formation and structure tokens share the material interaction line.
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

    /**
     * Structure-index ids that ship only as bulk carriers (no placeable controller block item).
     * They must never promise free right-click placement.
     */
    private static final Set<String> STRUCTURE_TOKEN_IDS = Set.of(
            "array_maintenance_obelisk",
            "blood_forbidden_exit_array",
            "capture_point_obelisk",
            "formation_flag_post",
            "furnace_safety_array",
            "illusion_array_hub",
            "immortal_alchemy_cauldron",
            "immortal_teleport_grand_array",
            "kill_array_hub",
            "kunwu_frost_forge",
            "puppet_core_forge",
            "qianzhu_control_console",
            "spirit_field_irrigation",
            "spirit_fire_brazier",
            "star_palace_teleport_gate",
            "time_acceleration_array",
            "war_banner_pole"
    );

    public static boolean isStructureTokenCarrier(String id) {
        String key = normalize(id);
        if (key.isBlank()) {
            return false;
        }
        // Meta tools are right-click executable even though they share station ids.
        if ("structure_repair_bench".equals(key) || "structure_blueprint_table".equals(key)) {
            return false;
        }
        if (STRUCTURE_TOKEN_IDS.contains(key)) {
            return true;
        }
        // Any bulk carrier whose id matches a multiblock index entry and is not a registered
        // formation behavior is a structure component/token, not a free placeable block.
        return MultiblockStructureCatalog.builtin().find(key).isPresent()
                && FormationItemService.builtin().find(key).isEmpty();
    }

    private static String detailKey(String id) {
        String key = normalize(id);
        return switch (key) {
            case "sect_contribution_token", "teleport_array_ticket", "array_disk_basic",
                    "array_disk_fragment", "crystal_array_disk", "formation_flag_jade",
                    "formation_flag_low", "formation_flag_mid", "formation_flag_post",
                    "immortal_array_disk", "jade_array_disk", "platinum_array_disk",
                    "space_array_disk", "spirit_gathering_array_disk", "array_blueprint_scroll",
                    "sect_identity_token", "sect_token",
                    "structure_repair_bench", "structure_blueprint_table" ->
                    "tooltip.seeking_immortals.catalog_item.detail." + key;
            default -> isStructureTokenCarrier(key)
                    ? "tooltip.seeking_immortals.catalog_item.detail.structure_token"
                    : "";
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
