package com.xunxian.seekingimmortals.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M07 place/activate behavior for formation items (registration remains M03/ModBulkItems).
 */
public final class FormationItemService {
    private static final String USES_REMAINING_TAG = "SeekingImmortalsFormationUses";
    private static final Snapshot BUILTIN = loadBuiltin();

    private FormationItemService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record ItemBehavior(
            String id,
            String display,
            String formationId,
            Integer uses,
            String placeBlock,
            String action
    ) {}

    public record Snapshot(Map<String, ItemBehavior> items) {
        public Snapshot {
            items = items == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(items));
        }

        public int size() {
            return items.size();
        }

        public Optional<ItemBehavior> find(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            ItemBehavior direct = items.get(id);
            if (direct != null) {
                return Optional.of(direct);
            }
            String key = id.trim().toLowerCase(Locale.ROOT);
            return Optional.ofNullable(items.get(key));
        }
    }

    /**
     * Attempt to use a formation item stack. Returns empty if the item is not a formation item.
     */
    public static Optional<InteractionResultHolder<ItemStack>> tryUse(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        String itemId = stack.getItem().builtInRegistryHolder().key().location().getPath();
        Optional<ItemBehavior> behaviorOpt = BUILTIN.find(itemId);
        if (behaviorOpt.isEmpty()) {
            // also accept formation_id-named bulk aliases
            return Optional.empty();
        }
        ItemBehavior behavior = behaviorOpt.get();
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.of(InteractionResultHolder.consume(stack));
        }

        String action = behavior.action() == null ? "" : behavior.action().trim().toLowerCase(Locale.ROOT);
        String formationId = behavior.formationId() == null ? "" : behavior.formationId();
        if ("inspect_only".equals(action)
                || ("activate_free_field".equals(action) && behavior.uses() == null)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.formation_item.inspect",
                    behavior.display() == null ? behavior.id() : behavior.display()), true);
            return Optional.of(InteractionResultHolder.success(stack));
        }
        if (!meetsRealmGate(player, formationId)) {
            return Optional.of(InteractionResultHolder.fail(stack));
        }

        if ("place_block".equals(action) && behavior.placeBlock() != null && !behavior.placeBlock().isBlank()) {
            BlockPos target = player.blockPosition().relative(player.getDirection());
            if (!serverLevel.getBlockState(target).canBeReplaced()) {
                target = player.blockPosition();
            }
            Block block = resolvePlaceBlock(behavior.placeBlock());
            if (block != null && serverLevel.getBlockState(target).canBeReplaced()) {
                BlockState state = block.defaultBlockState();
                serverLevel.setBlock(target, state, 3);
                consumeUse(player, stack, behavior);
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.formation_item.placed",
                        behavior.display() == null ? behavior.id() : behavior.display()), true);
                return Optional.of(InteractionResultHolder.success(stack));
            }
        }

        if (!"activate_free_field".equals(action)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.formation_item.failed",
                    behavior.display() == null ? behavior.id() : behavior.display()), true);
            return Optional.of(InteractionResultHolder.fail(stack));
        }

        // Activate free field from formation_id mapping.
        FormationFieldService.FieldKind kind = CraftWorldMappedKind(formationId);
        boolean ok = FormationFieldService.activateFreeField(
                serverLevel,
                player.blockPosition(),
                kind,
                20 * 90,
                player,
                formationId.isBlank() ? null : formationId);
        if (ok) {
            consumeUse(player, stack, behavior);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.formation_item.activated",
                    behavior.display() == null ? behavior.id() : behavior.display(),
                    kind.name()), true);
            return Optional.of(InteractionResultHolder.success(stack));
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.formation_item.failed",
                behavior.display() == null ? behavior.id() : behavior.display()), true);
        return Optional.of(InteractionResultHolder.fail(stack));
    }

    private static FormationFieldService.FieldKind CraftWorldMappedKind(String formationId) {
        return com.xunxian.seekingimmortals.catalog.CraftWorldSoftService.mappedFieldKind(formationId)
                .orElse(FormationFieldService.FieldKind.CATALOG_GENERIC);
    }

    private static boolean meetsRealmGate(ServerPlayer player, String formationId) {
        if (formationId == null || formationId.isBlank()) {
            return true;
        }
        var params = FormationFieldCatalog.builtin().find(formationId).orElse(null);
        if (params == null || params.realmMin() == null || params.realmMin().isBlank()) {
            return true;
        }
        var required = com.xunxian.seekingimmortals.cultivation.Realm.fromDesignId(params.realmMin());
        var cultivation = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).orElse(null);
        if (required == null || (cultivation != null && cultivation.getRealm().ordinal() >= required.ordinal())) {
            return true;
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.formation_item.realm_too_low", required.getDisplayName()), true);
        return false;
    }

    private static void consumeUse(ServerPlayer player, ItemStack stack, ItemBehavior behavior) {
        if (player.getAbilities().instabuild) {
            return;
        }
        if (behavior.uses() == null) {
            return;
        }
        int maxUses = Math.max(1, behavior.uses());
        if (maxUses == 1) {
            stack.shrink(1);
            return;
        }
        int remaining = stack.getOrCreateTag().contains(USES_REMAINING_TAG)
                ? stack.getOrCreateTag().getInt(USES_REMAINING_TAG)
                : maxUses;
        remaining--;
        if (remaining <= 0) {
            stack.shrink(1);
            if (!stack.isEmpty() && stack.getTag() != null) {
                stack.getTag().remove(USES_REMAINING_TAG);
            }
        } else {
            stack.getOrCreateTag().putInt(USES_REMAINING_TAG, remaining);
        }
    }

    private static Block resolvePlaceBlock(String placeBlock) {
        if (placeBlock == null) {
            return null;
        }
        String id = placeBlock.trim().toLowerCase(Locale.ROOT);
        if (id.contains("spirit_gathering_array")) {
            return ModBlocks.SPIRIT_GATHERING_ARRAY.get();
        }
        if (id.contains("spirit_gathering_formation_core") || id.contains("spirit_gather")) {
            return ModBlocks.SPIRIT_GATHERING_FORMATION_CORE.get();
        }
        return ModBlocks.SPIRIT_GATHERING_ARRAY.get();
    }

    private static Snapshot loadBuiltin() {
        Map<String, ItemBehavior> map = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/formation_item_behaviors.json");
        if (root == null) {
            root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/formation_items_catalog.json");
        }
        if (root != null) {
            String arrayKey = root.has("items") ? "items" : "entries";
            for (JsonElement element : array(root, arrayKey)) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) {
                    continue;
                }
                Integer uses = null;
                if (o.has("uses") && !o.get("uses").isJsonNull()) {
                    try {
                        uses = o.get("uses").getAsInt();
                    } catch (RuntimeException ignored) {
                        uses = null;
                    }
                }
                String formationId = str(o, "formation_id");
                String place = str(o, "place_block");
                String action = str(o, "action");
                if (action.isBlank()) {
                    action = !place.isBlank() ? "place_block" : (!formationId.isBlank() ? "activate_free_field" : "inspect_only");
                }
                map.put(id, new ItemBehavior(
                        id,
                        str(o, "display").isBlank() ? id : str(o, "display"),
                        formationId,
                        uses,
                        place,
                        action));
            }
        }
        return new Snapshot(map);
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = FormationItemService.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (Exception e) {
            SeekingImmortalsMod.LOGGER.warn("Failed loading formation item behaviors {}", path, e);
            return null;
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
