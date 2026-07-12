package com.xunxian.seekingimmortals.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestProgressTest {
    @Test
    void savesAndLoadsSevenMysteriesQuestState() {
        QuestProgress progress = new QuestProgress();
        progress.setStage(SevenMysteriesQuest.STAGE_INFIGHTING);
        progress.addFlag(SevenMysteriesQuest.FLAG_ROOT_TESTED);
        progress.addFlag(SevenMysteriesQuest.FLAG_EVIDENCE);
        progress.setBranchChoice("report");
        progress.setSect("seven_mysteries", "outer_disciple");
        progress.setSectQuestStage(3);
        progress.addSectFlag("qinglan_foundation_dilemma");
        progress.addContribution(80);
        progress.addReputation(50);
        progress.setYueArrived(true);
        progress.setSectMission("qinglan_spirit_grass", 88L);
        progress.completeSectMission();
        progress.setSecretRoomMarker(new BlockPos(12, 64, -3));
        progress.setYuePortalMarker(new BlockPos(80, 70, 91));

        CompoundTag saved = progress.saveNBT();
        QuestProgress loaded = new QuestProgress();
        loaded.loadNBT(saved);

        assertEquals(SevenMysteriesQuest.STAGE_INFIGHTING, loaded.getStage());
        assertTrue(loaded.hasFlag(SevenMysteriesQuest.FLAG_ROOT_TESTED));
        assertTrue(loaded.hasFlag(SevenMysteriesQuest.FLAG_EVIDENCE));
        assertEquals("report", loaded.getBranchChoice());
        assertEquals("seven_mysteries", loaded.getSectId());
        assertEquals("outer_disciple", loaded.getSectRole());
        assertEquals(3, loaded.getSectQuestStage());
        assertTrue(loaded.hasSectFlag("qinglan_foundation_dilemma"));
        assertEquals(80, loaded.getContribution());
        assertEquals(50, loaded.getReputation());
        assertTrue(loaded.hasYueArrived());
        assertEquals("qinglan_spirit_grass", loaded.getSectMissionId());
        assertEquals(88L, loaded.getSectMissionDay());
        assertTrue(loaded.isSectMissionAccepted());
        assertTrue(loaded.isSectMissionCompleted());
        assertEquals(new BlockPos(12, 64, -3), loaded.getSecretRoomMarker());
        assertEquals(new BlockPos(80, 70, 91), loaded.getYuePortalMarker());
    }

    @Test
    void clampsStagesAndContributionToSupportedBounds() {
        QuestProgress progress = new QuestProgress();
        progress.setStage(-20);
        assertEquals(SevenMysteriesQuest.STAGE_NOT_STARTED, progress.getStage());
        assertFalse(progress.isStarted());

        progress.setStage(999);
        assertEquals(SevenMysteriesQuest.STAGE_COMPLETE, progress.getStage());
        assertTrue(progress.isComplete());

        progress.addContribution(30);
        progress.addContribution(-100);
        assertEquals(0, progress.getContribution());
    }

    @Test
    void spendsContributionOnlyWhenBalanceIsEnough() {
        QuestProgress progress = new QuestProgress();
        progress.addContribution(120);

        assertFalse(progress.spendContribution(0));
        assertEquals(120, progress.getContribution());

        assertFalse(progress.spendContribution(-10));
        assertEquals(120, progress.getContribution());

        assertFalse(progress.spendContribution(200));
        assertEquals(120, progress.getContribution());

        assertTrue(progress.spendContribution(80));
        assertEquals(40, progress.getContribution());
    }

    @Test
    void ignoresBlankFlagsDuringMutationAndLoad() {
        QuestProgress progress = new QuestProgress();
        assertFalse(progress.addFlag(""));
        assertFalse(progress.addFlag("   "));
        assertFalse(progress.hasFlag(""));

        CompoundTag empty = new CompoundTag();
        progress.loadNBT(empty);

        assertEquals(SevenMysteriesQuest.STAGE_NOT_STARTED, progress.getStage());
        assertTrue(progress.getFlags().isEmpty());
        assertEquals("", progress.getBranchChoice());
        assertNull(progress.getSecretRoomMarker());
        assertNull(progress.getYuePortalMarker());
    }

    @Test
    void missingMarkerTagsLoadAsNullForOldSaves() {
        CompoundTag oldSave = new CompoundTag();
        oldSave.putInt("SevenMysteriesStage", SevenMysteriesQuest.STAGE_SECRET);

        QuestProgress progress = new QuestProgress();
        progress.loadNBT(oldSave);

        assertNull(progress.getSecretRoomMarker());
        assertNull(progress.getYuePortalMarker());
        assertEquals(0, progress.getSectQuestStage());
        assertTrue(progress.getSectFlags().isEmpty());
    }

    @Test
    void migratesOldQinglanMemberWithoutSectQuestStageToOuterDisciple() {
        CompoundTag oldSave = new CompoundTag();
        oldSave.putString("SectId", "qinglan_sect");
        oldSave.putString("SectRole", "outer_disciple");

        QuestProgress progress = new QuestProgress();
        progress.loadNBT(oldSave);

        assertEquals("qinglan_sect", progress.getSectId());
        assertEquals(2, progress.getSectQuestStage());
    }

    @Test
    void migratesOldQinglanMemberWithZeroSectQuestStageToOuterDisciple() {
        CompoundTag oldSave = new CompoundTag();
        oldSave.putString("SectId", "qinglan_sect");
        oldSave.putInt("SectQuestStage", 0);

        QuestProgress progress = new QuestProgress();
        progress.loadNBT(oldSave);

        assertEquals(2, progress.getSectQuestStage());
    }

    @Test
    void migratesOldQinglanMemberWithIllegalSectQuestStageToOuterDisciple() {
        CompoundTag oldSave = new CompoundTag();
        oldSave.putString("SectId", "qinglan_sect");
        oldSave.putInt("SectQuestStage", 99);

        QuestProgress progress = new QuestProgress();
        progress.loadNBT(oldSave);

        assertEquals(2, progress.getSectQuestStage());
    }
}
