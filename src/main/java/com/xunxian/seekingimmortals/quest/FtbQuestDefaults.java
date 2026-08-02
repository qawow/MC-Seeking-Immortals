package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.compat.ModCompat;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class FtbQuestDefaults {
    private static final String RESOURCE_ROOT = "/seeking_immortals/ftbquests/quests/";
    private static final Path TARGET_ROOT = Path.of("ftbquests", "quests");
    private static final Path MANAGEMENT_ROOT = Path.of("seeking_immortals", "ftbquests");
    private static final Path STATE_FILE = MANAGEMENT_ROOT.resolve("default-pack-state.properties");

    private FtbQuestDefaults() {
    }

    public static void bootstrapDefaultPack() {
        try {
            InstallResult result = bootstrapDefaultPack(FMLPaths.CONFIGDIR.get(), ModCompat.FTB_QUESTS_LOADED);
            switch (result.status()) {
                case INSTALLED -> SeekingImmortalsMod.LOGGER.info(
                        "Seeded bundled FTB quest default revision {}", FtbDefaultPackManifest.REVISION);
                case UPGRADED -> SeekingImmortalsMod.LOGGER.info(
                        "Upgraded bundled FTB quest defaults to revision {}", FtbDefaultPackManifest.REVISION);
                case PRESERVED_CUSTOMIZED -> SeekingImmortalsMod.LOGGER.warn(
                        "Preserved customized FTB quest defaults; bundled revision {} is available under {} ({})",
                        FtbDefaultPackManifest.REVISION,
                        FMLPaths.CONFIGDIR.get().resolve(MANAGEMENT_ROOT).resolve("pending")
                                .resolve(FtbDefaultPackManifest.REVISION),
                        result.detail());
                case CURRENT, SKIPPED_FTB_ABSENT -> {
                }
            }
        } catch (IOException exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to install or migrate bundled FTB quest defaults", exception);
        }
    }

    static InstallResult bootstrapDefaultPack(Path configDir, boolean ftbQuestsPresent) throws IOException {
        if (!ftbQuestsPresent) {
            return new InstallResult(InstallStatus.SKIPPED_FTB_ABSENT, "FTB Quests is absent");
        }
        return installDefaultPack(configDir);
    }

    static InstallResult installDefaultPack(Path configDir) throws IOException {
        Path absoluteConfig = configDir.toAbsolutePath();
        if (Files.exists(absoluteConfig, LinkOption.NOFOLLOW_LINKS)
                && Files.isSymbolicLink(absoluteConfig)) {
            return new InstallResult(InstallStatus.PRESERVED_CUSTOMIZED,
                    "Config root is a symbolic link; no managed files were changed");
        }
        Path normalizedConfig = absoluteConfig.normalize();
        Path targetRoot = normalizedConfig.resolve(TARGET_ROOT).normalize();
        Path managementRoot = normalizedConfig.resolve(MANAGEMENT_ROOT).normalize();
        if (containsSymbolicLink(normalizedConfig, managementRoot)) {
            return new InstallResult(InstallStatus.PRESERVED_CUSTOMIZED,
                    "Default-pack management path contains a symbolic link; no managed files were changed");
        }
        List<Path> writableManagementPaths = List.of(
                normalizedConfig.resolve(STATE_FILE).normalize(),
                managementRoot.resolve("pending").resolve(FtbDefaultPackManifest.REVISION).normalize(),
                managementRoot.resolve("backups").normalize(),
                managementRoot.resolve("staging").normalize());
        for (Path path : writableManagementPaths) {
            if (containsSymbolicLink(normalizedConfig, path)) {
                return new InstallResult(InstallStatus.PRESERVED_CUSTOMIZED,
                        "Default-pack management path contains a symbolic link; no managed files were changed");
            }
        }
        Map<String, BundledFile> bundled = loadBundledFiles();
        StateRead stateRead = readState(normalizedConfig.resolve(STATE_FILE).normalize());
        Inspection inspection = inspect(normalizedConfig, targetRoot, bundled, stateRead);

        if (!inspection.safe()) {
            writePendingPack(managementRoot, bundled, inspection.detail());
            return new InstallResult(InstallStatus.PRESERVED_CUSTOMIZED, inspection.detail());
        }

        if (allDesired(inspection.files(), bundled)) {
            if (!stateRead.exists() || !stateRead.currentFor(bundled)) {
                writeState(managementRoot, normalizedConfig.resolve(STATE_FILE).normalize(), bundled);
            }
            return new InstallResult(InstallStatus.CURRENT, "Bundled pack is current");
        }

        boolean freshInstall = inspection.files().values().stream().noneMatch(ExistingFile::exists);
        if (!installKnownPack(targetRoot, managementRoot, normalizedConfig.resolve(STATE_FILE).normalize(),
                bundled, inspection.files())) {
            return new InstallResult(InstallStatus.PRESERVED_CUSTOMIZED,
                    "Managed pack changed during migration");
        }
        return new InstallResult(freshInstall ? InstallStatus.INSTALLED : InstallStatus.UPGRADED,
                freshInstall ? "Fresh managed pack" : "Known bundled pack upgraded");
    }

    static String canonicalSha256(byte[] bytes) throws IOException {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("FTB quest file is not valid UTF-8", exception);
        }
        if (!text.isEmpty() && text.charAt(0) == '\ufeff') {
            text = text.substring(1);
        }
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java", exception);
        }
    }

    private static Map<String, BundledFile> loadBundledFiles() throws IOException {
        Map<String, BundledFile> files = new LinkedHashMap<>();
        for (FtbDefaultPackManifest.ManagedFile managed : FtbDefaultPackManifest.FILES) {
            String relativePath = managed.relativePath().replace('\\', '/');
            String resource = RESOURCE_ROOT + relativePath;
            try (InputStream input = FtbQuestDefaults.class.getResourceAsStream(resource)) {
                if (input == null) {
                    throw new IOException("Missing bundled FTB quest default " + resource);
                }
                byte[] bytes = input.readAllBytes();
                files.put(relativePath, new BundledFile(managed, bytes, canonicalSha256(bytes)));
            }
        }
        return files;
    }

    private static StateRead readState(Path stateFile) {
        if (!Files.exists(stateFile, LinkOption.NOFOLLOW_LINKS)) {
            return StateRead.missing();
        }
        if (Files.isSymbolicLink(stateFile) || !Files.isRegularFile(stateFile, LinkOption.NOFOLLOW_LINKS)) {
            return StateRead.invalid("Default-pack state is not a regular file");
        }
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
                properties.load(reader);
                String revision = properties.getProperty("installer_revision", "").trim();
                Map<String, String> hashes = new LinkedHashMap<>();
                for (FtbDefaultPackManifest.ManagedFile managed : FtbDefaultPackManifest.FILES) {
                    String hash = properties.getProperty("sha256." + managed.relativePath(), "").trim();
                    if (hash.isEmpty()) {
                        // Entry added by a newer manifest: older installers never recorded it.
                        // Leave it unrecorded so the upgrade path installs it instead of
                        // permanently blocking every boot with an invalid state.
                        hashes.put(managed.relativePath(), "");
                        continue;
                    }
                    if (!hash.matches("[0-9a-f]{64}")) {
                        return StateRead.invalid("Default-pack state has a malformed hash for "
                                + managed.relativePath());
                    }
                    hashes.put(managed.relativePath(), hash);
                }
                if (revision.isBlank()) {
                    return StateRead.invalid("Default-pack state has no installer revision");
                }
                return StateRead.valid(new PackState(revision, hashes));
        } catch (IOException | IllegalArgumentException exception) {
            return StateRead.invalid("Default-pack state cannot be read: " + exception.getMessage());
        }
    }

    private static Inspection inspect(Path configRoot,
                                      Path targetRoot,
                                      Map<String, BundledFile> bundled,
                                      StateRead stateRead) throws IOException {
        if (stateRead.invalid()) {
            return Inspection.unsafe(stateRead.detail());
        }
        Map<String, ExistingFile> files = new LinkedHashMap<>();
        int present = 0;
        for (BundledFile bundledFile : bundled.values()) {
            String relativePath = bundledFile.managed().relativePath();
            Path target = resolveManaged(targetRoot, relativePath);
            if (containsSymbolicLink(configRoot, target)) {
                return Inspection.unsafe("Managed path contains a symbolic link: " + relativePath);
            }
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                files.put(relativePath, ExistingFile.missing(target));
                continue;
            }
            present++;
            if (Files.isSymbolicLink(target)) {
                return Inspection.unsafe("Managed file is a symbolic link: " + relativePath);
            }
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                return Inspection.unsafe("Managed path is not a regular file: " + relativePath);
            }
            byte[] bytes = Files.readAllBytes(target);
            String hash;
            try {
                hash = canonicalSha256(bytes);
            } catch (IOException exception) {
                return Inspection.unsafe("Managed file is not canonical UTF-8: " + relativePath);
            }
            files.put(relativePath, ExistingFile.present(target, bytes, hash));
        }

        if (stateRead.exists()) {
            for (Map.Entry<String, ExistingFile> entry : files.entrySet()) {
                ExistingFile existing = entry.getValue();
                String recorded = stateRead.state().hashes().get(entry.getKey());
                if (recorded == null || recorded.isBlank()) {
                    // Newly added manifest entry: an existing different file is treated as a
                    // user customization and blocks the upgrade; a missing file is simply the
                    // upgrade work item and stays safe.
                    if (existing.exists()) {
                        BundledFile desired = bundled.get(entry.getKey());
                        if (desired != null && !existing.digest().equals(desired.digest())
                                && !desired.managed().historicalHashes().contains(existing.digest())) {
                            return Inspection.unsafe("Legacy managed file is customized: " + entry.getKey());
                        }
                    }
                    continue;
                }
                if (!existing.exists()) {
                    return Inspection.unsafe("Managed file was deleted after installation: " + entry.getKey());
                }
                if (!existing.digest().equals(recorded)) {
                    return Inspection.unsafe("Managed file differs from installed state: " + entry.getKey());
                }
            }
            return Inspection.safe(files);
        }

        if (present == 0) {
            return Inspection.safe(files);
        }
        if (present != bundled.size()) {
            return Inspection.unsafe("Legacy managed pack is incomplete");
        }
        for (Map.Entry<String, ExistingFile> entry : files.entrySet()) {
            BundledFile desired = bundled.get(entry.getKey());
            String digest = entry.getValue().digest();
            if (!digest.equals(desired.digest())
                    && !desired.managed().historicalHashes().contains(digest)) {
                return Inspection.unsafe("Legacy managed file is customized: " + entry.getKey());
            }
        }
        return Inspection.safe(files);
    }

    private static void writePendingPack(Path managementRoot,
                                         Map<String, BundledFile> bundled,
                                         String detail) throws IOException {
        Path pendingRoot = managementRoot.resolve("pending")
                .resolve(FtbDefaultPackManifest.REVISION).normalize();
        if (containsSymbolicLink(managementRoot, pendingRoot)) {
            throw new IOException("Pending FTB quest path contains a symbolic link");
        }
        for (BundledFile file : bundled.values()) {
            Path target = resolveManaged(pendingRoot, file.managed().relativePath());
            if (containsSymbolicLink(managementRoot, target)) {
                throw new IOException("Pending FTB quest path contains a symbolic link: "
                        + file.managed().relativePath());
            }
            writeAtomically(target, file.bytes(), true);
        }
        Path reasonPath = pendingRoot.resolve("migration-reason.properties");
        if (containsSymbolicLink(managementRoot, reasonPath)) {
            throw new IOException("Pending FTB migration reason path contains a symbolic link");
        }
        String reason = "installer_revision=" + FtbDefaultPackManifest.REVISION + "\n"
                + "reason=" + detail.replace('\n', ' ').replace('\r', ' ') + "\n";
        writeAtomically(reasonPath, reason.getBytes(StandardCharsets.UTF_8), true);
    }

    private static boolean installKnownPack(Path targetRoot,
                                            Path managementRoot,
                                            Path stateFile,
                                            Map<String, BundledFile> bundled,
                                            Map<String, ExistingFile> existing) throws IOException {
        Files.createDirectories(managementRoot);
        Path stagingParent = managementRoot.resolve("staging");
        if (containsSymbolicLink(managementRoot, stagingParent)) {
            throw new IOException("FTB staging path contains a symbolic link");
        }
        Files.createDirectories(stagingParent);
        Path stagingRoot = Files.createTempDirectory(stagingParent, ".pack-");
        try {
            if (containsSymbolicLink(managementRoot, stagingRoot)) {
                throw new IOException("FTB staging path contains a symbolic link");
            }
            for (BundledFile file : bundled.values()) {
                Path staged = resolveManaged(stagingRoot, file.managed().relativePath());
                Files.createDirectories(staged.getParent());
                Files.write(staged, file.bytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                if (!canonicalSha256(Files.readAllBytes(staged)).equals(file.digest())) {
                    throw new IOException("Staged FTB quest default failed digest verification");
                }
            }

            if (!snapshotStillMatches(existing)) {
                writePendingPack(managementRoot, bundled, "Managed pack changed during migration");
                return false;
            }
            backupExisting(managementRoot, existing);

            List<ExistingFile> replaced = new ArrayList<>();
            try {
                for (BundledFile file : bundled.values()) {
                    ExistingFile old = existing.get(file.managed().relativePath());
                    if (old.exists() && old.digest().equals(file.digest())) {
                        continue;
                    }
                    Path target = resolveManaged(targetRoot, file.managed().relativePath());
                    if (containsSymbolicLink(targetRoot, target)) {
                        throw new IOException("Managed FTB quest path contains a symbolic link: "
                                + file.managed().relativePath());
                    }
                    Files.createDirectories(target.getParent());
                    moveAtomically(resolveManaged(stagingRoot, file.managed().relativePath()), target, true);
                    replaced.add(old);
                }
                writeState(managementRoot, stateFile, bundled);
            } catch (IOException exception) {
                try {
                    rollback(replaced);
                } catch (IOException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
            return true;
        } finally {
            deleteTree(stagingRoot);
        }
    }

    private static boolean snapshotStillMatches(Map<String, ExistingFile> existing) throws IOException {
        for (ExistingFile snapshot : existing.values()) {
            boolean present = Files.exists(snapshot.path(), LinkOption.NOFOLLOW_LINKS);
            if (!snapshot.exists()) {
                // Newly added manifest entries are expected to be installed by this migration.
                continue;
            }
            if (!present || Files.isSymbolicLink(snapshot.path())
                    || !Files.isRegularFile(snapshot.path(), LinkOption.NOFOLLOW_LINKS)) {
                return false;
            }
            if (!canonicalSha256(Files.readAllBytes(snapshot.path())).equals(snapshot.digest())) {
                return false;
            }
        }
        return true;
    }

    private static void backupExisting(Path managementRoot,
                                       Map<String, ExistingFile> existing) throws IOException {
        String fingerprint = packFingerprint(existing).substring(0, 16);
        Path backupRoot = managementRoot.resolve("backups")
                .resolve("before-" + FtbDefaultPackManifest.REVISION + "-" + fingerprint);
        if (containsSymbolicLink(managementRoot, backupRoot)) {
            throw new IOException("FTB backup path contains a symbolic link");
        }
        for (Map.Entry<String, ExistingFile> entry : existing.entrySet()) {
            ExistingFile file = entry.getValue();
            if (!file.exists()) {
                continue;
            }
            Path backup = resolveManaged(backupRoot, entry.getKey());
            if (containsSymbolicLink(managementRoot, backup)) {
                throw new IOException("FTB backup path contains a symbolic link: " + entry.getKey());
            }
            if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(backup) || !Arrays.equals(Files.readAllBytes(backup), file.bytes())) {
                    throw new IOException("Conflicting FTB quest backup " + backup);
                }
                continue;
            }
            writeAtomically(backup, file.bytes(), false);
        }
    }

    private static String packFingerprint(Map<String, ExistingFile> existing) throws IOException {
        StringBuilder value = new StringBuilder();
        for (Map.Entry<String, ExistingFile> entry : existing.entrySet()) {
            value.append(entry.getKey()).append('=')
                    .append(entry.getValue().exists() ? entry.getValue().digest() : "missing")
                    .append('\n');
        }
        return canonicalSha256(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void rollback(List<ExistingFile> replaced) throws IOException {
        IOException failure = null;
        for (int i = replaced.size() - 1; i >= 0; i--) {
            ExistingFile old = replaced.get(i);
            try {
                if (old.exists()) {
                    writeAtomically(old.path(), old.bytes(), true);
                } else {
                    Files.deleteIfExists(old.path());
                }
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static boolean allDesired(Map<String, ExistingFile> existing,
                                      Map<String, BundledFile> bundled) {
        for (Map.Entry<String, BundledFile> entry : bundled.entrySet()) {
            ExistingFile file = existing.get(entry.getKey());
            if (file == null || !file.exists() || !file.digest().equals(entry.getValue().digest())) {
                return false;
            }
        }
        return true;
    }

    private static void writeState(Path managementRoot, Path stateFile,
                                   Map<String, BundledFile> bundled) throws IOException {
        if (containsSymbolicLink(managementRoot, stateFile)) {
            throw new IOException("Default-pack state path contains a symbolic link");
        }
        StringBuilder state = new StringBuilder();
        state.append("installer_revision=").append(FtbDefaultPackManifest.REVISION).append('\n');
        for (BundledFile file : bundled.values()) {
            state.append("sha256.").append(file.managed().relativePath()).append('=')
                    .append(file.digest()).append('\n');
        }
        writeAtomically(stateFile, state.toString().getBytes(StandardCharsets.UTF_8), true);
    }

    private static void writeAtomically(Path target, byte[] bytes, boolean replace) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".si-ftb-", ".tmp");
        try {
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            moveAtomically(temporary, target, replace);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomically(Path source, Path target, boolean replace) throws IOException {
        if (!replace) {
            Files.move(source, target);
            return;
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path resolveManaged(Path root, String relativePath) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IOException("Managed FTB quest path escapes its root: " + relativePath);
        }
        return resolved;
    }

    private static boolean containsSymbolicLink(Path root, Path target) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IOException("FTB quest target escapes the config root: " + target);
        }
        Path current = normalizedRoot;
        for (Path part : normalizedRoot.relativize(normalizedTarget)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    enum InstallStatus {
        SKIPPED_FTB_ABSENT,
        INSTALLED,
        UPGRADED,
        CURRENT,
        PRESERVED_CUSTOMIZED
    }

    record InstallResult(InstallStatus status, String detail) {}

    private record BundledFile(FtbDefaultPackManifest.ManagedFile managed, byte[] bytes, String digest) {}

    private record ExistingFile(Path path, boolean exists, byte[] bytes, String digest) {
        static ExistingFile missing(Path path) {
            return new ExistingFile(path, false, new byte[0], "");
        }

        static ExistingFile present(Path path, byte[] bytes, String digest) {
            return new ExistingFile(path, true, bytes, digest);
        }
    }

    private record Inspection(boolean safe, Map<String, ExistingFile> files, String detail) {
        static Inspection safe(Map<String, ExistingFile> files) {
            return new Inspection(true, Map.copyOf(files), "");
        }

        static Inspection unsafe(String detail) {
            return new Inspection(false, Map.of(), detail);
        }
    }

    private record PackState(String revision, Map<String, String> hashes) {}

    private record StateRead(boolean exists, boolean invalid, PackState state, String detail) {
        static StateRead missing() {
            return new StateRead(false, false, null, "");
        }

        static StateRead valid(PackState state) {
            return new StateRead(true, false, state, "");
        }

        static StateRead invalid(String detail) {
            return new StateRead(true, true, null, detail);
        }

        boolean currentFor(Map<String, BundledFile> bundled) {
            if (!exists || invalid || state == null
                    || !FtbDefaultPackManifest.REVISION.equals(state.revision())) {
                return false;
            }
            for (Map.Entry<String, BundledFile> entry : bundled.entrySet()) {
                if (!entry.getValue().digest().equals(state.hashes().get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
