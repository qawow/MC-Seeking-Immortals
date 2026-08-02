package com.xunxian.seekingimmortals.persistence;

import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.catalog.ChronicleTradeSoftService;
import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.MethodLayoutService;
import com.xunxian.seekingimmortals.catalog.NewGamePlusEconomyService;
import com.xunxian.seekingimmortals.craft.GardenLiquidService;
import com.xunxian.seekingimmortals.npc.NamedNpcRewardService;
import com.xunxian.seekingimmortals.npc.DialogueWorldActionService;
import com.xunxian.seekingimmortals.npc.NpcDialogueFlags;
import com.xunxian.seekingimmortals.npc.NpcFavorService;
import com.xunxian.seekingimmortals.quest.DetailedQuestRuntimeService;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TimelineChronicleService;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor;
import com.xunxian.seekingimmortals.worldpack.DailyEventRewardService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PlayerPersistentDataClonePolicy {
    public static final String EXTREME_PRESERVED_KEY = "SeekingImmortalsExtremePreserved";

    private static final Set<String> DURABLE_KEYS = Set.of(
            "SeekingImmortalsExchangeDay",
            "SeekingImmortalsExchangeCount",
            "SeekingImmortalsAgeDay",
            "SeekingImmortalsPatchouliGuideGiven",
            "SeekingImmortalsAppearanceFixed",
            "SeekingImmortalsMarrowAddiction",
            "seeking_immortals_tribulation_success",
            "seeking_immortals_last_tribulation_realm",
            "seeking_immortals_ling_gen_slab_cd",
            "seeking_immortals_herb_planter_cd",
            "seeking_immortals_natal_binding",
            "seeking_immortals_beast_contracts",
            "seeking_immortals_puppet_growth",
            "seeking_immortals_auction_interest",
            "seeking_immortals_auction_personal_raises",
            "seeking_immortals_auction_won",
            "seeking_immortals_unique_story_rewards",
            com.xunxian.seekingimmortals.quest.FtbRewardBridgeService.ROOT_TAG,
            "seeking_immortals_main_story",
            "seeking_immortals_soft_phases",
            "seeking_immortals_shop_quota",
            "seeking_immortals_reputation",
            "seeking_immortals_dim_travel_cd",
            "seeking_immortals_faction_conflict",
            "seeking_immortals_sect_mission_progress",
            "seeking_immortals_active_generated_mission",
            "seeking_immortals_ghost_hunt",
            "seeking_immortals_ghost_hunt_reason",
            "seeking_immortals_ghost_detected",
            "seeking_immortals_ascension_stage",
            "seeking_immortals_boss_kills",
            "seeking_immortals_secret_realm_trial_rewards",
            "seeking_immortals_secret_realm_encounters",
            "seeking_immortals_secret_realm_mid_encounters",
            "seeking_immortals_secret_realm_mid_clear",
            "seeking_immortals_secret_realm_core_clear",
            "seeking_immortals_live_smoke_signed",
            "seeking_immortals_live_smoke_signed_by",
            "seeking_immortals_live_smoke_signed_note",
            "seeking_immortals_live_smoke_signed_time",
            "seeking_immortals_mp_smoke_signed",
            "seeking_immortals_mp_smoke_signed_by",
            "seeking_immortals_mp_smoke_signed_note",
            "seeking_immortals_mp_smoke_signed_time");

    private static final List<String> DURABLE_PREFIXES = List.of(
            "seeking_immortals_boss_spawned_",
            "seeking_immortals_daily_spawned_");

    private PlayerPersistentDataClonePolicy() {}

    public static void copyDurableData(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return;
        }
        ManualCatalogService.copyProgressionData(source, target);
        com.xunxian.seekingimmortals.alchemy.AlchemyFormulaKnowledge.copyProgressionData(source, target);
        MethodLayoutService.copyLayoutData(source, target);
        GardenLiquidService.copyPersistentData(source, target);
        NewGamePlusEconomyService.copyPersistentData(source, target);
        TextQuestChainService.copyPersistentData(source, target);
        DetailedQuestRuntimeService.copyPersistentData(source, target);
        TimelineChronicleService.copyPersistentData(source, target);
        ChronicleTradeSoftService.copyPersistentData(source, target);
        BestiaryUnlockService.copyPersistentData(source, target);
        NamedNpcRewardService.copyPersistentData(source, target);
        NpcFavorService.copyPersistentData(source, target);
        NpcDialogueFlags.copyPersistentData(source, target);
        DialogueWorldActionService.copyPersistentData(source, target);
        DailyEventEffectExecutor.copyPersistentData(source, target);
        DailyEventRewardService.copyPersistentData(source, target);
        com.xunxian.seekingimmortals.quest.QuestHookRuntime.copyPersistentData(source, target);

        for (String key : DURABLE_KEYS) {
            copyKey(source, target, key);
        }
        for (String key : source.getAllKeys()) {
            if (DURABLE_PREFIXES.stream().anyMatch(key::startsWith)) {
                copyKey(source, target, key);
            }
        }
    }

    public static boolean moveExtremePreserved(CompoundTag source, CompoundTag target) {
        if (source == null || target == null) {
            return false;
        }
        Tag tag = source.get(EXTREME_PRESERVED_KEY);
        if (!isCompoundList(tag)) {
            return false;
        }
        target.put(EXTREME_PRESERVED_KEY, tag.copy());
        source.remove(EXTREME_PRESERVED_KEY);
        return true;
    }

    public static List<ItemStack> takeExtremePreserved(CompoundTag data) {
        List<ItemStack> stacks = new ArrayList<>();
        for (CompoundTag entry : takeExtremePreservedTags(data)) {
            ItemStack stack = ItemStack.of(entry);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return List.copyOf(stacks);
    }

    static List<CompoundTag> takeExtremePreservedTags(CompoundTag data) {
        if (data == null) {
            return List.of();
        }
        Tag tag = data.get(EXTREME_PRESERVED_KEY);
        if (!isCompoundList(tag)) {
            data.remove(EXTREME_PRESERVED_KEY);
            return List.of();
        }
        ListTag list = (ListTag) tag;
        List<CompoundTag> entries = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            entries.add(list.getCompound(i).copy());
        }
        data.remove(EXTREME_PRESERVED_KEY);
        return List.copyOf(entries);
    }

    public static Set<String> durableKeys() {
        return DURABLE_KEYS;
    }

    public static List<String> durablePrefixes() {
        return DURABLE_PREFIXES;
    }

    private static void copyKey(CompoundTag source, CompoundTag target, String key) {
        Tag tag = source.get(key);
        if (tag != null) {
            target.put(key, tag.copy());
        }
    }

    private static boolean isCompoundList(Tag tag) {
        return tag instanceof ListTag list
                && (list.isEmpty() || list.getElementType() == Tag.TAG_COMPOUND);
    }
}
