package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.catalog.ChronicleTradeSoftService;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.quest.TimelineChronicleService;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Clone must preserve authority PersistentData while dropping temporary dialogue sessions. */
class PersistentAuthorityCopyTest {
    @Test
    void copiesAuthorityLedgersAndSkipsTemporaryDialogueSessions() {
        CompoundTag original = new CompoundTag();

        CompoundTag questRoot = new CompoundTag();
        questRoot.putInt("chain_a", 3);
        original.put("seeking_immortals_text_quest_chains", questRoot);
        CompoundTag rewards = new CompoundTag();
        rewards.putBoolean("chain_a", true);
        original.put("seeking_immortals_text_quest_rewards", rewards);
        original.put("seeking_immortals_text_quest_branches", tagString("chain_a", "neutral"));
        original.put("seeking_immortals_text_quest_npc", tagString("chain_a", "npc_a"));
        original.put("seeking_immortals_text_quest_mid_rewards", tagBool("chain_a:1", true));
        original.put("seeking_immortals_quest_authority_rewards", tagBool("chain_a", true));

        original.put("seeking_immortals_hanli_timeline", tagBool("event_1", true));
        original.put("seeking_immortals_bestiary", tagBool("beast_a", true));
        original.put("seeking_immortals_npc_rewards_claimed", tagBool("tree:node", true));
        original.put("seeking_immortals_npc_favor", tagInt("npc_a", 7));
        original.put("seeking_immortals_npc_dialogue_flags", tagBool("flag_a", true));

        // Temporary dialogue sessions must never be cloned.
        original.put("seeking_immortals_dialogue_session", tagString("context", "nonce-1"));
        original.put("seeking_immortals_text_dialogue_session", tagString("context", "nonce-2"));

        CompoundTag clone = new CompoundTag();
        TextQuestChainService.copyPersistentData(original, clone);
        TimelineChronicleService.copyPersistentData(original, clone);
        ChronicleTradeSoftService.copyPersistentData(original, clone);
        BestiaryUnlockService.copyPersistentData(original, clone);
        NamedNpcRewardService.copyPersistentData(original, clone);
        NpcFavorService.copyPersistentData(original, clone);
        NpcDialogueFlags.copyPersistentData(original, clone);

        assertEquals(3, clone.getCompound("seeking_immortals_text_quest_chains").getInt("chain_a"));
        assertTrue(clone.getCompound("seeking_immortals_text_quest_rewards").getBoolean("chain_a"));
        assertEquals("neutral", clone.getCompound("seeking_immortals_text_quest_branches").getString("chain_a"));
        assertEquals("npc_a", clone.getCompound("seeking_immortals_text_quest_npc").getString("chain_a"));
        assertTrue(clone.getCompound("seeking_immortals_text_quest_mid_rewards").getBoolean("chain_a:1"));
        assertTrue(clone.getCompound("seeking_immortals_quest_authority_rewards").getBoolean("chain_a"));
        assertTrue(clone.getCompound("seeking_immortals_hanli_timeline").getBoolean("event_1"));
        assertTrue(clone.getCompound("seeking_immortals_bestiary").getBoolean("beast_a"));
        assertTrue(clone.getCompound("seeking_immortals_npc_rewards_claimed").getBoolean("tree:node"));
        assertEquals(7, clone.getCompound("seeking_immortals_npc_favor").getInt("npc_a"));
        assertTrue(clone.getCompound("seeking_immortals_npc_dialogue_flags").getBoolean("flag_a"));

        assertFalse(clone.contains("seeking_immortals_dialogue_session"));
        assertFalse(clone.contains("seeking_immortals_text_dialogue_session"));
    }

    private static CompoundTag tagString(String key, String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }

    private static CompoundTag tagBool(String key, boolean value) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(key, value);
        return tag;
    }

    private static CompoundTag tagInt(String key, int value) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(key, value);
        return tag;
    }
}
