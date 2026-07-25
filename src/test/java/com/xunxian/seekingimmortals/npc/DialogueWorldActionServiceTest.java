package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.structure.MultiblockStructureCatalog;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueWorldActionServiceTest {
    @Test
    void allAuthoredStructureMarkersTargetKnownMultiblocks() {
        for (String id : List.of(
                "inverse_star_smuggle_dock", "true_word_lecture_platform",
                "wuxing_world_seed_block", "qianzhu_control_console")) {
            assertTrue(MultiblockStructureCatalog.builtin().find(id).isPresent(), id);
        }
    }

    @Test
    void cloneCopyPreservesWorldActionLedgersDeeply() {
        CompoundTag source = new CompoundTag();
        for (String key : List.of(
                DialogueWorldActionService.MARKERS_TAG,
                DialogueWorldActionService.HINTS_TAG,
                DialogueWorldActionService.ANOMALIES_TAG,
                DialogueWorldActionService.SUSPICION_TAG,
                DialogueWorldActionService.COMBAT_TAG)) {
            CompoundTag value = new CompoundTag();
            value.putInt("entry", 7);
            source.put(key, value);
        }
        CompoundTag target = new CompoundTag();

        DialogueWorldActionService.copyPersistentData(source, target);

        for (String key : source.getAllKeys()) {
            assertNotSame(source.get(key), target.get(key), key);
            assertTrue(target.getCompound(key).contains("entry"), key);
        }
    }

    @Test
    void executorFailsClosedAndCombatShellsCarryPlayerBinding() throws Exception {
        String executor = Files.readString(Path.of("src/main/java/com/xunxian/seekingimmortals/npc/DialogueActionExecutor.java"));
        String service = Files.readString(Path.of("src/main/java/com/xunxian/seekingimmortals/npc/DialogueWorldActionService.java"));
        String entity = Files.readString(Path.of(
                "src/main/java/com/xunxian/seekingimmortals/entity/SummonedServitorEntity.java"));

        assertTrue(executor.contains("effect_unsupported"));
        assertFalse(executor.contains("effect_\" + type"));
        assertTrue(service.contains("putUUID(HOSTILE_PLAYER, player.getUUID())"));
        assertTrue(service.contains("MAX_BOUND_HOSTILES"));
        assertTrue(service.contains("COMBAT_COOLDOWN_TICKS"));
        assertTrue(entity.contains("enforceDialogueTarget()"));
        assertTrue(entity.contains("public boolean canAttack(LivingEntity target)"));
    }
}
