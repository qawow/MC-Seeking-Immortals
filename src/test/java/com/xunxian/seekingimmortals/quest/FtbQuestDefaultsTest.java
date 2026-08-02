package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbQuestDefaultsTest {
    private static final String RESOURCE_ROOT = "/seeking_immortals/ftbquests/quests/";
    private static final String LEGACY_DATA = """
            {
              version: 13,
              default_reward_team: false,
              default_consume_items: false,
              default_autoclaim_rewards: "disabled",
              default_quest_shape: "circle",
              default_quest_disable_jei: false,
              emergency_items_cooldown: 300,
              drop_loot_crates: false,
              disable_gui: false,
              grid_scale: 0.5d,
              pause_game: false,
              lock_message: "",
              progression_mode: "linear",
              detection_delay: 20,
              drop_book_on_death: false,
              show_lock_icons: true,
              hide_excluded_quests: false
            }
            """;

    @TempDir
    Path tempDir;

    @Test
    void freshInstallSeedsAllManagedFilesAndRecordsIndependentRevision() throws IOException {
        Path configDir = tempDir.resolve("config");

        FtbQuestDefaults.InstallResult result =
                FtbQuestDefaults.bootstrapDefaultPack(configDir, true);

        assertEquals(FtbQuestDefaults.InstallStatus.INSTALLED, result.status());
        assertPackMatchesBundled(configDir.resolve("ftbquests/quests"));
        Path state = stateFile(configDir);
        assertTrue(Files.isRegularFile(state));
        String stateText = Files.readString(state);
        assertTrue(stateText.contains("installer_revision=" + FtbDefaultPackManifest.REVISION));
        assertFalse(stateText.contains("version=13"), "Installer revision must not reuse FTB's SNBT schema version");
    }

    @Test
    void knownHistoricalPackWithCrLfAndBomUpgradesAndIsBackedUp() throws IOException {
        Path configDir = tempDir.resolve("config");
        Path targetRoot = configDir.resolve("ftbquests/quests");
        seedBundledWithoutState(targetRoot, true);

        byte[] legacyCrLf = ("\ufeff" + LEGACY_DATA.replace("\n", "\r\n"))
                .getBytes(StandardCharsets.UTF_8);
        assertEquals("50d890f11368dd3085625ea2b091f827b4a69f2990e5eeb886f933a7dc45f045",
                FtbQuestDefaults.canonicalSha256(legacyCrLf));
        Files.write(targetRoot.resolve("data.snbt"), legacyCrLf);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.UPGRADED, result.status());
        assertPackMatchesBundled(targetRoot);
        Path backups = configDir.resolve("seeking_immortals/ftbquests/backups");
        try (var paths = Files.walk(backups)) {
            Path dataBackup = paths.filter(Files::isRegularFile)
                    .filter(path -> path.endsWith("data.snbt"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("50d890f11368dd3085625ea2b091f827b4a69f2990e5eeb886f933a7dc45f045",
                    FtbQuestDefaults.canonicalSha256(Files.readAllBytes(dataBackup)));
        }
    }

    @Test
    void customizedManagedFilePreservesWholePackAndWritesPendingCandidate() throws IOException {
        Path configDir = tempDir.resolve("config");
        Path targetRoot = configDir.resolve("ftbquests/quests");
        seedBundledWithoutState(targetRoot, false);
        Path customized = targetRoot.resolve("chapters/seeking_immortals_main.snbt");
        Files.writeString(customized, "\n// server customization\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
        Map<String, byte[]> before = managedBytes(targetRoot);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.PRESERVED_CUSTOMIZED, result.status());
        for (Map.Entry<String, byte[]> entry : before.entrySet()) {
            assertArrayEquals(entry.getValue(), Files.readAllBytes(targetRoot.resolve(entry.getKey())),
                    "Unsafe migration changed " + entry.getKey());
        }
        assertPackMatchesBundled(pendingRoot(configDir));
        assertTrue(Files.isRegularFile(pendingRoot(configDir).resolve("migration-reason.properties")));
    }

    @Test
    void currentManagedPackIsNoOpForQuestFiles() throws IOException {
        Path configDir = tempDir.resolve("config");
        FtbQuestDefaults.installDefaultPack(configDir);
        Path targetRoot = configDir.resolve("ftbquests/quests");
        Map<String, byte[]> before = managedBytes(targetRoot);
        Map<String, FileTime> modified = managedModifiedTimes(targetRoot);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.CURRENT, result.status());
        for (Map.Entry<String, byte[]> entry : before.entrySet()) {
            Path target = targetRoot.resolve(entry.getKey());
            assertArrayEquals(entry.getValue(), Files.readAllBytes(target));
            assertEquals(modified.get(entry.getKey()), Files.getLastModifiedTime(target));
        }
    }

    @Test
    void unrelatedUserChapterSurvivesFreshInstall() throws IOException {
        Path configDir = tempDir.resolve("config");
        Path customChapter = configDir.resolve("ftbquests/quests/chapters/user_custom.snbt");
        Files.createDirectories(customChapter.getParent());
        Files.writeString(customChapter, "{ filename: \"user_custom\" }");

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.INSTALLED, result.status());
        assertEquals("{ filename: \"user_custom\" }", Files.readString(customChapter));
        assertPackMatchesBundled(configDir.resolve("ftbquests/quests"));
    }

    @Test
    void deletedManagedFileAfterStateIsPreservedAsDeletion() throws IOException {
        Path configDir = tempDir.resolve("config");
        FtbQuestDefaults.installDefaultPack(configDir);
        Path deleted = configDir.resolve("ftbquests/quests/chapters/seeking_immortals_main.snbt");
        Files.delete(deleted);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.PRESERVED_CUSTOMIZED, result.status());
        assertFalse(Files.exists(deleted), "A file deleted after managed installation must not be re-seeded");
        assertPackMatchesBundled(pendingRoot(configDir));
    }

    @Test
    void symbolicLinkManagedFileIsNeverFollowedOrOverwritten() throws IOException {
        Path configDir = tempDir.resolve("config");
        FtbQuestDefaults.installDefaultPack(configDir);
        Path managed = configDir.resolve("ftbquests/quests/chapters/seeking_immortals_main.snbt");
        Path external = tempDir.resolve("external-custom.snbt");
        Files.writeString(external, "external custom content");
        Files.delete(managed);
        Files.createSymbolicLink(managed, external);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.PRESERVED_CUSTOMIZED, result.status());
        assertTrue(Files.isSymbolicLink(managed));
        assertEquals("external custom content", Files.readString(external));
        assertPackMatchesBundled(pendingRoot(configDir));
    }

    @Test
    void symbolicLinkManagementRootIsNeverFollowed() throws IOException {
        Path configDir = tempDir.resolve("config");
        Path external = tempDir.resolve("external-management");
        Files.createDirectories(configDir);
        Files.createDirectories(external);
        Files.createSymbolicLink(configDir.resolve("seeking_immortals"), external);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.PRESERVED_CUSTOMIZED, result.status());
        try (var paths = Files.list(external)) {
            assertTrue(paths.findAny().isEmpty(), "Installer followed a symlink into its management root");
        }
        assertFalse(Files.exists(configDir.resolve("ftbquests")));
    }

    @Test
    void symbolicLinkPendingRootIsNeverFollowed() throws IOException {
        Path configDir = tempDir.resolve("config");
        Path targetRoot = configDir.resolve("ftbquests/quests");
        seedBundledWithoutState(targetRoot, false);
        Path customized = targetRoot.resolve("chapters/seeking_immortals_main.snbt");
        Files.writeString(customized, "\n// server customization\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
        byte[] before = Files.readAllBytes(customized);
        Path external = tempDir.resolve("external-pending");
        Files.createDirectories(external);
        Path managementRoot = configDir.resolve("seeking_immortals/ftbquests");
        Files.createDirectories(managementRoot);
        Files.createSymbolicLink(managementRoot.resolve("pending"), external);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.PRESERVED_CUSTOMIZED, result.status());
        assertArrayEquals(before, Files.readAllBytes(customized));
        try (var paths = Files.list(external)) {
            assertTrue(paths.findAny().isEmpty(), "Installer followed a symlink into pending output");
        }
    }

    @Test
    void symbolicLinkBackupRootIsNeverFollowed() throws IOException {
        Path configDir = tempDir.resolve("config");
        Path targetRoot = configDir.resolve("ftbquests/quests");
        seedBundledWithoutState(targetRoot, false);
        Path legacy = targetRoot.resolve("data.snbt");
        Files.writeString(legacy, LEGACY_DATA, StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(legacy);
        Path external = tempDir.resolve("external-backups");
        Files.createDirectories(external);
        Path managementRoot = configDir.resolve("seeking_immortals/ftbquests");
        Files.createDirectories(managementRoot);
        Files.createSymbolicLink(managementRoot.resolve("backups"), external);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.PRESERVED_CUSTOMIZED, result.status());
        assertArrayEquals(before, Files.readAllBytes(legacy));
        try (var paths = Files.list(external)) {
            assertTrue(paths.findAny().isEmpty(), "Installer followed a symlink into backup output");
        }
    }

    @Test
    void symbolicLinkStagingRootIsNeverFollowed() throws IOException {
        Path configDir = tempDir.resolve("config");
        Path external = tempDir.resolve("external-staging");
        Files.createDirectories(external);
        Path managementRoot = configDir.resolve("seeking_immortals/ftbquests");
        Files.createDirectories(managementRoot);
        Files.createSymbolicLink(managementRoot.resolve("staging"), external);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.PRESERVED_CUSTOMIZED, result.status());
        assertFalse(Files.exists(configDir.resolve("ftbquests")));
        try (var paths = Files.list(external)) {
            assertTrue(paths.findAny().isEmpty(), "Installer followed a symlink into staging output");
        }
    }

    @Test
    void ftbAbsentDoesNotCreateConfigOrLoadBundledFiles() throws IOException {
        Path configDir = tempDir.resolve("config");

        FtbQuestDefaults.InstallResult result =
                FtbQuestDefaults.bootstrapDefaultPack(configDir, false);

        assertEquals(FtbQuestDefaults.InstallStatus.SKIPPED_FTB_ABSENT, result.status());
        assertFalse(Files.exists(configDir));
    }

    @Test
    void staleStateFromOlderRevisionUpgradesWhenManifestGrows() throws IOException {
        Path configDir = tempDir.resolve("config");
        FtbQuestDefaults.installDefaultPack(configDir);
        Path state = stateFile(configDir);
        List<String> lines = new java.util.ArrayList<>(Files.readAllLines(state));
        String lastEntry = lines.get(lines.size() - 1);
        lines.remove(lines.size() - 1);
        Files.writeString(state, String.join("\n", lines));
        String addedRelative = lastEntry.substring("sha256.".length(), lastEntry.indexOf('='));
        Path addedTarget = configDir.resolve("ftbquests/quests").resolve(addedRelative);
        Files.delete(addedTarget);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.UPGRADED, result.status());
        assertPackMatchesBundled(configDir.resolve("ftbquests/quests"));
        assertTrue(Files.readString(state).contains(lastEntry),
                "Rewritten state must record every manifest entry");
    }

    @Test
    void staleStateKeepsRecordedEntriesSafeAndUpgradeDoesNotRewriteOtherFiles() throws IOException {
        Path configDir = tempDir.resolve("config");
        FtbQuestDefaults.installDefaultPack(configDir);
        Path state = stateFile(configDir);
        List<String> lines = new java.util.ArrayList<>(Files.readAllLines(state));
        lines.remove(lines.size() - 1);
        Files.writeString(state, String.join("\n", lines));
        Path targetRoot = configDir.resolve("ftbquests/quests");
        Map<String, byte[]> before = managedBytes(targetRoot);

        FtbQuestDefaults.InstallResult result = FtbQuestDefaults.installDefaultPack(configDir);

        assertEquals(FtbQuestDefaults.InstallStatus.CURRENT, result.status());
        for (Map.Entry<String, byte[]> entry : before.entrySet()) {
            assertArrayEquals(entry.getValue(), Files.readAllBytes(targetRoot.resolve(entry.getKey())),
                    "Recorded files must not be rewritten by the stale-state upgrade");
        }
        for (FtbDefaultPackManifest.ManagedFile file : FtbDefaultPackManifest.FILES) {
            assertTrue(Files.readString(state).contains("sha256." + file.relativePath()),
                    "Rewritten state must record " + file.relativePath());
        }
    }

    private static void seedBundledWithoutState(Path targetRoot, boolean crLf) throws IOException {
        for (FtbDefaultPackManifest.ManagedFile file : FtbDefaultPackManifest.FILES) {
            Path target = targetRoot.resolve(file.relativePath());
            Files.createDirectories(target.getParent());
            byte[] bytes = resourceBytes(file.relativePath());
            if (crLf) {
                String normalized = new String(bytes, StandardCharsets.UTF_8)
                        .replace("\r\n", "\n")
                        .replace('\r', '\n')
                        .replace("\n", "\r\n");
                bytes = normalized.getBytes(StandardCharsets.UTF_8);
            }
            Files.write(target, bytes);
        }
    }

    private static void assertPackMatchesBundled(Path root) throws IOException {
        for (FtbDefaultPackManifest.ManagedFile file : FtbDefaultPackManifest.FILES) {
            Path target = root.resolve(file.relativePath());
            assertTrue(Files.isRegularFile(target), "Missing managed file " + file.relativePath());
            assertEquals(FtbQuestDefaults.canonicalSha256(resourceBytes(file.relativePath())),
                    FtbQuestDefaults.canonicalSha256(Files.readAllBytes(target)),
                    "Bundled content mismatch for " + file.relativePath());
        }
    }

    private static Map<String, byte[]> managedBytes(Path root) throws IOException {
        Map<String, byte[]> values = new LinkedHashMap<>();
        for (FtbDefaultPackManifest.ManagedFile file : FtbDefaultPackManifest.FILES) {
            values.put(file.relativePath(), Files.readAllBytes(root.resolve(file.relativePath())));
        }
        return values;
    }

    private static Map<String, FileTime> managedModifiedTimes(Path root) throws IOException {
        Map<String, FileTime> values = new LinkedHashMap<>();
        for (FtbDefaultPackManifest.ManagedFile file : FtbDefaultPackManifest.FILES) {
            values.put(file.relativePath(), Files.getLastModifiedTime(root.resolve(file.relativePath())));
        }
        return values;
    }

    private static byte[] resourceBytes(String relativePath) throws IOException {
        try (InputStream input = FtbQuestDefaultsTest.class.getResourceAsStream(RESOURCE_ROOT + relativePath)) {
            if (input == null) {
                throw new IOException("Missing test resource " + relativePath);
            }
            return input.readAllBytes();
        }
    }

    private static Path stateFile(Path configDir) {
        return configDir.resolve("seeking_immortals/ftbquests/default-pack-state.properties");
    }

    private static Path pendingRoot(Path configDir) {
        return configDir.resolve("seeking_immortals/ftbquests/pending")
                .resolve(FtbDefaultPackManifest.REVISION);
    }
}
