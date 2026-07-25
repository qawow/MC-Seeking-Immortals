package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ItemCatalogService;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailedQuestRuntimeServiceTest {
    @Test
    void loadsAllPlayableChainsAndStepsWithoutUnsupportedStructuredConditions() {
        DetailedQuestRuntimeService.Snapshot snapshot = DetailedQuestRuntimeService.builtin();

        assertEquals(23, snapshot.chains().size());
        assertEquals(95, snapshot.stepCount());
        assertTrue(snapshot.unsupportedPrerequisites().isEmpty(), snapshot.unsupportedPrerequisites().toString());
        assertTrue(snapshot.unsupportedNeeds().isEmpty(), snapshot.unsupportedNeeds().toString());
        for (DetailedQuestRuntimeService.Chain chain : snapshot.chains().values()) {
            assertFalse(chain.steps().isEmpty(), chain.id());
            for (int index = 0; index < chain.steps().size(); index++) {
                assertEquals(index + 1, chain.steps().get(index).number(), chain.id());
                assertFalse(chain.steps().get(index).action().isBlank(), chain.id() + ":" + (index + 1));
            }
        }
    }

    @Test
    void playableRewardCompatibilityAliasesResolveToRegisteredCarrierMetadata() {
        List<String> rewardIds = List.of(
                "blood_forbidden_token", "black_jiao_sinew", "jiao_pearl", "court_warrant_gray",
                "zhui_mo_ling", "lingzhu_fruit", "yin_zhi_horse_live", "peiying_dan",
                "puppet_core_embryo_broken");

        for (String rewardId : rewardIds) {
            String canonical = ItemCatalogService.resolveId(rewardId);
            assertTrue(ItemCatalogService.findMeta(canonical).isPresent(), rewardId + " -> " + canonical);
        }
    }

    @Test
    void placeParserExtractsOnlyStableIds() {
        assertEquals(List.of("contribution_stele", "inner_sect_task_board"),
                DetailedQuestRuntimeService.placeTokens("contribution_stele / inner_sect_task_board"));
        assertEquals(List.of("tianyuan_garrison"),
                DetailedQuestRuntimeService.placeTokens("tianyuan_garrison board"));
        assertEquals(List.of("zm_outer", "zm_inner"),
                DetailedQuestRuntimeService.placeTokens("zm_outer→zm_inner"));
    }

    @Test
    void cloneCopyPreservesAuthorityAndUsesDeepCopies() {
        CompoundTag source = new CompoundTag();
        CompoundTag state = new CompoundTag();
        state.putInt("Stage", 3);
        CompoundTag root = new CompoundTag();
        root.put("wuxing_intro", state);
        source.put(DetailedQuestRuntimeService.ROOT_TAG, root);
        source.put(DetailedQuestRuntimeService.REWARD_TAG, booleanTag("wuxing_intro:step:2"));
        source.put(DetailedQuestRuntimeService.EVIDENCE_TAG, booleanTag("wuxing_world_seed_block"));
        CompoundTag target = new CompoundTag();

        DetailedQuestRuntimeService.copyPersistentData(source, target);

        assertNotSame(source.get(DetailedQuestRuntimeService.ROOT_TAG),
                target.get(DetailedQuestRuntimeService.ROOT_TAG));
        assertEquals(3, target.getCompound(DetailedQuestRuntimeService.ROOT_TAG)
                .getCompound("wuxing_intro").getInt("Stage"));
        assertTrue(target.getCompound(DetailedQuestRuntimeService.REWARD_TAG)
                .getBoolean("wuxing_intro:step:2"));
        assertTrue(target.getCompound(DetailedQuestRuntimeService.EVIDENCE_TAG)
                .getBoolean("wuxing_world_seed_block"));
    }

    @Test
    void chanceRewardsAreDeterministicAndRespectBounds() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000123");
        boolean first = DetailedQuestRuntimeService.deterministicChance(player, "peiying:step:3", 0.25D);
        assertEquals(first,
                DetailedQuestRuntimeService.deterministicChance(player, "peiying:step:3", 0.25D));
        assertFalse(DetailedQuestRuntimeService.deterministicChance(player, "x", 0.0D));
        assertTrue(DetailedQuestRuntimeService.deterministicChance(player, "x", 1.0D));
    }

    private static CompoundTag booleanTag(String key) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(key, true);
        return tag;
    }
}
