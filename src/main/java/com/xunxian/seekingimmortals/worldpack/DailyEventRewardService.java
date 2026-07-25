package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.region.DailyEventScheduler;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Idempotent settlement for authored daily-event rewards. */
public final class DailyEventRewardService {
    private static final String ROOT = "seeking_immortals_daily_event_rewards";
    private static final String CLAIMS = "Claims";
    private static final int MAX_CLAIMS = 64;

    private static final String ENTITY_OWNER = "SeekingImmortalsDailyEventOwner";
    private static final String ENTITY_REGION = "SeekingImmortalsDailyEventRegion";
    private static final String ENTITY_EVENT = "SeekingImmortalsDailyEventId";
    private static final String ENTITY_UNTIL = "SeekingImmortalsDailyEventUntil";

    private DailyEventRewardService() {}

    public enum ClaimResult {
        CLAIMED,
        INACTIVE,
        NO_REWARD,
        COMBAT_REQUIRED,
        ALREADY_CLAIMED,
        UNRESOLVED
    }

    public static void copyPersistentData(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        Tag stored = source.get(ROOT);
        if (stored != null) {
            target.put(ROOT, stored.copy());
        }
    }

    public static void bindEncounter(Entity entity, ServerPlayer owner,
                                     String regionId, String eventId, long untilTick) {
        if (entity == null || owner == null || eventId == null || eventId.isBlank() || untilTick <= 0L) {
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        tag.putUUID(ENTITY_OWNER, owner.getUUID());
        tag.putString(ENTITY_REGION, normalize(regionId));
        tag.putString(ENTITY_EVENT, normalize(eventId));
        tag.putLong(ENTITY_UNTIL, untilTick);
    }

    public static ClaimResult claimEncounter(ServerPlayer killer, Entity entity) {
        if (killer == null || entity == null) {
            return ClaimResult.INACTIVE;
        }
        CompoundTag tag = entity.getPersistentData();
        if (!tag.hasUUID(ENTITY_OWNER) || !killer.getUUID().equals(tag.getUUID(ENTITY_OWNER))) {
            return ClaimResult.INACTIVE;
        }
        return settle(killer, tag.getString(ENTITY_REGION), tag.getString(ENTITY_EVENT),
                tag.getLong(ENTITY_UNTIL), true);
    }

    public static ClaimResult claimActive(ServerPlayer player) {
        if (player == null) {
            return ClaimResult.INACTIVE;
        }
        String regionId = RegionRegistry.resolveAndSync(player);
        WorldpackSavedData.EventRoll authoritative = DailyEventScheduler.ensurePlayerEvent(player, regionId);
        ActiveRoll roll = new ActiveRoll(
                authoritative.regionId(), authoritative.eventId(), authoritative.untilTick());
        if (roll.eventId().isBlank() || roll.untilTick() <= player.level().getGameTime()) {
            return ClaimResult.INACTIVE;
        }
        Optional<DailyEventEffectCatalog.Event> event = DailyEventEffectCatalog.builtin().find(roll.eventId());
        if (event.isEmpty()) {
            return ClaimResult.NO_REWARD;
        }
        if (DailyEventEncounterService.hasCombatPlan(roll.regionId(), event.get())) {
            return ClaimResult.COMBAT_REQUIRED;
        }
        return settle(player, roll.regionId(), roll.eventId(), roll.untilTick(), false);
    }

    public static void sendResult(ServerPlayer player, ClaimResult result) {
        if (player == null || result == null) {
            return;
        }
        String key = switch (result) {
            case CLAIMED -> "message.seeking_immortals.daily_event.reward_claimed";
            case INACTIVE -> "message.seeking_immortals.daily_event.reward_inactive";
            case NO_REWARD -> "message.seeking_immortals.daily_event.reward_none";
            case COMBAT_REQUIRED -> "message.seeking_immortals.daily_event.reward_combat_required";
            case ALREADY_CLAIMED -> "message.seeking_immortals.daily_event.reward_already_claimed";
            case UNRESOLVED -> "message.seeking_immortals.daily_event.reward_unresolved";
        };
        if (result == ClaimResult.CLAIMED) {
            String eventId = CultivationHelper.get(player)
                    .map(cultivation -> cultivation.getWorldpackActiveDailyEventId()).orElse("");
            Component display = DailyEventEffectCatalog.builtin().find(eventId)
                    .map(event -> PlayerDisplayText.safeLiteral(
                            event.display(), "text.seeking_immortals.unknown_event"))
                    .orElseGet(() -> Component.translatable("text.seeking_immortals.unknown_event"));
            player.sendSystemMessage(Component.translatable(key, display));
        } else {
            player.sendSystemMessage(Component.translatable(key));
        }
    }

    static String claimKey(String regionId, String eventId, long untilTick) {
        return normalize(regionId) + "|" + normalize(eventId) + "|" + Math.max(0L, untilTick);
    }

    static Optional<List<ItemStack>> resolveRewards(DailyEventEffectCatalog.Event event, RandomSource random) {
        if (event == null || random == null) {
            return Optional.empty();
        }
        List<ItemStack> rewards = new ArrayList<>();
        for (String token : event.rewards()) {
            Optional<List<ItemStack>> resolved = resolveToken(token, random);
            if (resolved.isEmpty()) {
                return Optional.empty();
            }
            rewards.addAll(resolved.get());
        }
        if (!event.rewardsTag().isBlank()) {
            if ("merit_points".equals(event.rewardsTag())) {
                ItemStack merit = stack("seeking_immortals:alliance_merit_token", 2);
                if (merit.isEmpty()) {
                    return Optional.empty();
                }
                rewards.add(merit);
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(List.copyOf(rewards));
    }

    private static ClaimResult settle(ServerPlayer player, String regionId, String eventId,
                                      long untilTick, boolean combatClaim) {
        long now = player.level().getGameTime();
        String eventKey = normalize(eventId);
        if (eventKey.isBlank() || untilTick <= now) {
            return ClaimResult.INACTIVE;
        }
        Optional<DailyEventEffectCatalog.Event> eventOpt = DailyEventEffectCatalog.builtin().find(eventKey);
        if (eventOpt.isEmpty()) {
            return ClaimResult.NO_REWARD;
        }
        DailyEventEffectCatalog.Event event = eventOpt.get();
        if (event.rewards().isEmpty() && event.rewardsTag().isBlank()) {
            return ClaimResult.NO_REWARD;
        }
        if (!DailyEventEffectExecutor.isRealmAllowed(player, event)) {
            return ClaimResult.INACTIVE;
        }
        if (!event.warPhase().isBlank()
                && !com.xunxian.seekingimmortals.sect.FactionConflictEventService.activePhase(player)
                .filter(event.warPhase()::equals).isPresent()) {
            return ClaimResult.INACTIVE;
        }
        if (!combatClaim && DailyEventEncounterService.hasCombatPlan(regionId, event)) {
            return ClaimResult.COMBAT_REQUIRED;
        }
        String claim = claimKey(regionId, eventKey, untilTick);
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        List<String> claims = readClaims(root, now);
        if (claims.contains(claim)) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        Optional<List<ItemStack>> resolvedRewards = resolveRewards(event, player.getRandom());
        if (resolvedRewards.isEmpty()) {
            return ClaimResult.UNRESOLVED;
        }
        List<ItemStack> rewards = resolvedRewards.get();
        if (rewards.isEmpty()) {
            // Chance rewards may honestly roll no item, but still consume the one claim.
            writeClaim(root, claims, claim);
            player.getPersistentData().put(ROOT, root);
            return ClaimResult.CLAIMED;
        }

        writeClaim(root, claims, claim);
        player.getPersistentData().put(ROOT, root);
        for (ItemStack reward : rewards) {
            InventoryDeliveryService.giveOrEnqueue(player, reward,
                    "daily_event:" + eventKey + ":" + untilTick);
        }
        return ClaimResult.CLAIMED;
    }

    private static Optional<List<ItemStack>> resolveToken(String rawToken, RandomSource random) {
        String token = normalize(rawToken);
        return switch (token) {
            case "random_low_artifact" -> artifactReward(random, 1, 3, 1.0D);
            case "random_mid_artifact_chance" -> artifactReward(random, 4, 6, 0.35D);
            case "herb_bundle" -> stacks(
                    stack("seeking_immortals:spirit_grass", 4),
                    stack("seeking_immortals:cloud_mushroom", 1));
            case "talisman_paper_bundle" -> stacks(stack("seeking_immortals:talisman_paper_bundle", 1));
            case "war_scout_token" -> stacks(stack("seeking_immortals:war_scout_token", 1));
            case "low_grade_spirit_stone" -> {
                List<String> stones = List.of("metal_spirit_stone", "wood_spirit_stone", "water_spirit_stone",
                        "fire_element_spirit_stone", "earth_spirit_stone");
                yield stacks(stack("seeking_immortals:" + stones.get(random.nextInt(stones.size())), 2));
            }
            case "array_core_fragment" -> stacks(stack("seeking_immortals:array_core_fragment", 2));
            case "fashi_art_fragment" -> stacks(stack("seeking_immortals:fashi_art_fragment", 1));
            case "war_merit_huangfeng" -> stacks(stack("seeking_immortals:war_contribution_token", 2));
            case "anti_fashi_talisman" -> stacks(stack("seeking_immortals:anti_fashi_talisman", 1));
            default -> Optional.empty();
        };
    }

    private static Optional<List<ItemStack>> artifactReward(RandomSource random, int minTier,
                                                             int maxTier, double chance) {
        if (random.nextDouble() > chance) {
            return Optional.of(List.of());
        }
        List<Item> candidates = ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> item instanceof ArtifactCatalogItem)
                .filter(item -> {
                    ArtifactCatalogItem carrier = (ArtifactCatalogItem) item;
                    return ArtifactDataService.builtin().findArtifact(carrier.artifactId())
                            .filter(def -> def.gameTier() >= minTier && def.gameTier() <= maxTier)
                            .filter(def -> def.binds().isBlank())
                            .filter(def -> !normalize(def.compliance()).contains("unique"))
                            .isPresent();
                })
                .sorted(Comparator.comparing(item -> {
                    ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
                    return key == null ? "" : key.toString();
                }))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(List.of(new ItemStack(candidates.get(random.nextInt(candidates.size())))));
    }

    private static Optional<List<ItemStack>> stacks(ItemStack... values) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack value : values) {
            if (value == null || value.isEmpty()) {
                return Optional.empty();
            }
            out.add(value);
        }
        return Optional.of(List.copyOf(out));
    }

    private static ItemStack stack(String itemId, int count) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        Item item = location == null ? Items.AIR : ForgeRegistries.ITEMS.getValue(location);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, count));
    }

    private static List<String> readClaims(CompoundTag root, long now) {
        LinkedHashSet<String> claims = new LinkedHashSet<>();
        ListTag list = root.getList(CLAIMS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String claim = list.getString(i);
            int separator = claim.lastIndexOf('|');
            if (separator <= 0 || separator >= claim.length() - 1) {
                continue;
            }
            try {
                long until = Long.parseLong(claim.substring(separator + 1));
                if (until > now) {
                    claims.add(claim);
                }
            } catch (NumberFormatException ignored) {
                // Discard malformed legacy entries.
            }
        }
        List<String> ordered = new ArrayList<>(claims);
        int from = Math.max(0, ordered.size() - MAX_CLAIMS);
        return new ArrayList<>(ordered.subList(from, ordered.size()));
    }

    private static void writeClaim(CompoundTag root, List<String> claims, String claim) {
        claims.remove(claim);
        claims.add(claim);
        ListTag list = new ListTag();
        int from = Math.max(0, claims.size() - MAX_CLAIMS);
        for (int i = from; i < claims.size(); i++) {
            list.add(StringTag.valueOf(claims.get(i)));
        }
        root.put(CLAIMS, list);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ActiveRoll(String regionId, String eventId, long untilTick) {
        private ActiveRoll {
            regionId = normalize(regionId);
            eventId = normalize(eventId);
        }
    }
}
