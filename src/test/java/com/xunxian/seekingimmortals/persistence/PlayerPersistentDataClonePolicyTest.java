package com.xunxian.seekingimmortals.persistence;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPersistentDataClonePolicyTest {
    private static final String DAILY_EVENT_SEMANTICS = "seeking_immortals_daily_event_semantics";
    private static final Set<String> EXPECTED_DURABLE_KEYS = Set.of(
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
            "seeking_immortals_ftb_reward_bridge",
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

    private static final List<String> EXPECTED_DURABLE_PREFIXES = List.of(
            "seeking_immortals_boss_spawned_",
            "seeking_immortals_daily_spawned_");

    @Test
    void exposesExactDurableKeyAndPrefixPolicy() {
        assertEquals(44, PlayerPersistentDataClonePolicy.durableKeys().size());
        assertEquals(EXPECTED_DURABLE_KEYS, PlayerPersistentDataClonePolicy.durableKeys());
        assertEquals(2, PlayerPersistentDataClonePolicy.durablePrefixes().size());
        assertEquals(EXPECTED_DURABLE_PREFIXES, PlayerPersistentDataClonePolicy.durablePrefixes());
    }

    @Test
    void copiesOnlyDurableDataDeeplyAndOverwritesHelperData() {
        CompoundTag source = new CompoundTag();
        for (String key : EXPECTED_DURABLE_KEYS) {
            source.put(key, intTag("value", 7));
        }

        List<String> dynamicKeys = List.of(
                EXPECTED_DURABLE_PREFIXES.get(0) + "trial_boss",
                EXPECTED_DURABLE_PREFIXES.get(1) + "spirit_market");
        source.put(dynamicKeys.get(0), intTag("value", 11));
        source.put(dynamicKeys.get(1), intTag("value", 13));

        CompoundTag helperSource = intTag("fresh", 17);
        source.put(ManualCatalogService.STUDIED_TAG, helperSource);
        CompoundTag eventSemantics = new CompoundTag();
        eventSemantics.putString("ActiveId", "mulan_border_patrol");
        eventSemantics.putLong("ActiveUntil", 24000L);
        eventSemantics.putString("AppliedId", "mulan_border_patrol");
        eventSemantics.putLong("AppliedUntil", 24000L);
        ListTag appliedFlags = new ListTag();
        appliedFlags.add(StringTag.valueOf("daily_event_mulan_border_patrol"));
        eventSemantics.put("AppliedFlags", appliedFlags);
        eventSemantics.putDouble("BreakthroughBonus", 0.05D);
        eventSemantics.putDouble("SmuggleChance", 0.2D);
        source.put(DAILY_EVENT_SEMANTICS, eventSemantics);
        source.putInt("seeking_immortals_unknown_clone_data", 19);
        source.putInt("seeking_immortals_dialogue_session", 23);
        source.putInt("seeking_immortals_text_dialogue_session", 29);
        source.putInt("seeking_immortals_boss_spawned", 31);
        source.put(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY,
                compoundList(intTag("slot", 1)));

        CompoundTag target = new CompoundTag();
        target.put(ManualCatalogService.STUDIED_TAG, intTag("stale", 37));

        PlayerPersistentDataClonePolicy.copyDurableData(source, target);

        Set<String> expectedTargetKeys = new HashSet<>(EXPECTED_DURABLE_KEYS);
        expectedTargetKeys.addAll(dynamicKeys);
        expectedTargetKeys.add(ManualCatalogService.STUDIED_TAG);
        expectedTargetKeys.add(DAILY_EVENT_SEMANTICS);
        assertEquals(expectedTargetKeys, target.getAllKeys());

        for (String key : EXPECTED_DURABLE_KEYS) {
            assertNotSame(source.get(key), target.get(key), key);
            assertEquals(7, target.getCompound(key).getInt("value"), key);
            source.getCompound(key).putInt("value", -1);
            assertEquals(7, target.getCompound(key).getInt("value"), key);
        }
        for (int index = 0; index < dynamicKeys.size(); index++) {
            String key = dynamicKeys.get(index);
            int expectedValue = index == 0 ? 11 : 13;
            assertNotSame(source.get(key), target.get(key), key);
            assertEquals(expectedValue, target.getCompound(key).getInt("value"), key);
            source.getCompound(key).putInt("value", -1);
            assertEquals(expectedValue, target.getCompound(key).getInt("value"), key);
        }

        assertNotSame(helperSource, target.get(ManualCatalogService.STUDIED_TAG));
        assertEquals(17, target.getCompound(ManualCatalogService.STUDIED_TAG).getInt("fresh"));
        assertFalse(target.getCompound(ManualCatalogService.STUDIED_TAG).contains("stale"));
        helperSource.putInt("fresh", -1);
        assertEquals(17, target.getCompound(ManualCatalogService.STUDIED_TAG).getInt("fresh"));

        CompoundTag copiedSemantics = target.getCompound(DAILY_EVENT_SEMANTICS);
        assertNotSame(eventSemantics, copiedSemantics);
        assertEquals("mulan_border_patrol", copiedSemantics.getString("ActiveId"));
        assertEquals(24000L, copiedSemantics.getLong("AppliedUntil"));
        assertEquals("daily_event_mulan_border_patrol",
                copiedSemantics.getList("AppliedFlags", Tag.TAG_STRING).getString(0));
        eventSemantics.putString("ActiveId", "changed_after_clone");
        eventSemantics.getList("AppliedFlags", Tag.TAG_STRING).clear();
        assertEquals("mulan_border_patrol", copiedSemantics.getString("ActiveId"));
        assertEquals(1, copiedSemantics.getList("AppliedFlags", Tag.TAG_STRING).size());

        assertFalse(target.contains("seeking_immortals_unknown_clone_data"));
        assertFalse(target.contains("seeking_immortals_dialogue_session"));
        assertFalse(target.contains("seeking_immortals_text_dialogue_session"));
        assertFalse(target.contains("seeking_immortals_boss_spawned"));
        assertFalse(target.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
        assertTrue(source.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
    }

    @Test
    void clearsStaleOrMalformedDailyEventOwnershipDuringClone() {
        CompoundTag target = new CompoundTag();
        target.put(DAILY_EVENT_SEMANTICS, intTag("stale", 1));

        PlayerPersistentDataClonePolicy.copyDurableData(new CompoundTag(), target);
        assertFalse(target.contains(DAILY_EVENT_SEMANTICS));

        CompoundTag malformedSource = new CompoundTag();
        malformedSource.putString(DAILY_EVENT_SEMANTICS, "not-a-compound");
        target.put(DAILY_EVENT_SEMANTICS, intTag("stale", 2));
        PlayerPersistentDataClonePolicy.copyDurableData(malformedSource, target);
        assertFalse(target.contains(DAILY_EVENT_SEMANTICS));
    }

    @Test
    void movesExtremeDataAcrossNonDeathClone() {
        CompoundTag source = new CompoundTag();
        ListTag preserved = compoundList(intTag("slot", 1));
        source.put(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY, preserved);
        CompoundTag target = new CompoundTag();

        assertTrue(PlayerPersistentDataClonePolicy.moveExtremePreserved(source, target));
        assertFalse(source.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
        assertNotSame(preserved, target.get(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
        assertEquals(1, target.getList(
                PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY, Tag.TAG_COMPOUND).size());
    }

    @Test
    void rejectsNonCompoundExtremeListsOnDeath() {
        assertMoveRejected(intTag("not", 1));

        ListTag stringList = new ListTag();
        stringList.add(StringTag.valueOf("not-an-item"));
        assertMoveRejected(stringList);
    }

    @Test
    void movesCompoundExtremeListDeeplyOnceOnDeath() {
        CompoundTag nested = intTag("value", 41);
        CompoundTag entry = new CompoundTag();
        entry.put("nested", nested);
        ListTag preserved = compoundList(entry);
        CompoundTag source = new CompoundTag();
        source.put(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY, preserved);
        CompoundTag target = new CompoundTag();

        assertTrue(PlayerPersistentDataClonePolicy.moveExtremePreserved(source, target));
        assertFalse(source.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));

        ListTag moved = assertInstanceOf(ListTag.class,
                target.get(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
        assertEquals(Tag.TAG_COMPOUND, moved.getElementType());
        assertNotSame(preserved, moved);
        assertNotSame(entry, moved.getCompound(0));
        assertNotSame(nested, moved.getCompound(0).getCompound("nested"));
        assertEquals(41, moved.getCompound(0).getCompound("nested").getInt("value"));

        nested.putInt("value", -1);
        assertEquals(41, moved.getCompound(0).getCompound("nested").getInt("value"));

        Tag afterFirstMove = moved.copy();
        assertFalse(PlayerPersistentDataClonePolicy.moveExtremePreserved(source, target));
        assertEquals(afterFirstMove, target.get(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
    }

    @Test
    void takeExtremePreservedClearsWrongTypes() {
        CompoundTag compoundData = new CompoundTag();
        compoundData.put(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY, intTag("not", 1));
        assertEquals(List.of(), PlayerPersistentDataClonePolicy.takeExtremePreserved(compoundData));
        assertFalse(compoundData.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));

        CompoundTag stringListData = new CompoundTag();
        ListTag stringList = new ListTag();
        stringList.add(StringTag.valueOf("not-an-item"));
        stringListData.put(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY, stringList);
        assertEquals(List.of(), PlayerPersistentDataClonePolicy.takeExtremePreserved(stringListData));
        assertFalse(stringListData.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
    }

    @Test
    void takeExtremePreservedTagsConsumesDeepCopiesOnce() {
        CompoundTag expected = new CompoundTag();
        expected.putString("id", "minecraft:diamond");
        expected.putByte("Count", (byte) 3);
        expected.put("tag", intTag("policy_marker", 1));
        ListTag preserved = new ListTag();
        preserved.add(expected);
        CompoundTag data = new CompoundTag();
        data.put(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY, preserved);

        List<CompoundTag> firstTake = PlayerPersistentDataClonePolicy.takeExtremePreservedTags(data);

        assertEquals(1, firstTake.size());
        CompoundTag consumed = firstTake.get(0);
        assertNotSame(expected, consumed);
        assertEquals("minecraft:diamond", consumed.getString("id"));
        assertEquals(3, consumed.getByte("Count"));
        assertEquals(1, consumed.getCompound("tag").getInt("policy_marker"));
        expected.getCompound("tag").putInt("policy_marker", -1);
        assertEquals(1, consumed.getCompound("tag").getInt("policy_marker"));
        assertFalse(data.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
        assertEquals(List.of(), PlayerPersistentDataClonePolicy.takeExtremePreservedTags(data));
    }

    private static CompoundTag intTag(String key, int value) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(key, value);
        return tag;
    }

    private static ListTag compoundList(CompoundTag... entries) {
        ListTag list = new ListTag();
        for (CompoundTag entry : entries) {
            list.add(entry);
        }
        return list;
    }

    private static void assertMoveRejected(Tag invalidTag) {
        CompoundTag source = new CompoundTag();
        source.put(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY, invalidTag);
        CompoundTag target = new CompoundTag();

        assertFalse(PlayerPersistentDataClonePolicy.moveExtremePreserved(source, target));
        assertSame(invalidTag, source.get(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
        assertFalse(target.contains(PlayerPersistentDataClonePolicy.EXTREME_PRESERVED_KEY));
    }
}
