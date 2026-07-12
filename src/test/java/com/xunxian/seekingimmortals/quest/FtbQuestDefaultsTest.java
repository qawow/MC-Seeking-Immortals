package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbQuestDefaultsTest {
    @TempDir
    Path tempDir;

    @Test
    void copyIfMissingSeedsBundledDataFile() throws IOException {
        Path configRoot = tempDir.resolve("ftbquests").resolve("quests");

        assertDoesNotThrow(() -> FtbQuestDefaults.copyIfMissing(configRoot, "data.snbt"));

        Path target = configRoot.resolve("data.snbt");
        assertTrue(Files.exists(target), "Bundled FTB data.snbt should be copied when missing");
        assertTrue(Files.size(target) > 0, "Seeded FTB data.snbt should not be empty");
    }

    @Test
    void copyIfMissingDoesNotOverwriteExistingFile() throws IOException {
        Path configRoot = tempDir.resolve("ftbquests").resolve("quests");
        Path target = configRoot.resolve("data.snbt");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "existing default");

        FtbQuestDefaults.copyIfMissing(configRoot, "data.snbt");

        assertEquals("existing default", Files.readString(target),
                "Existing user or server FTB quest defaults must not be overwritten");
    }
}
