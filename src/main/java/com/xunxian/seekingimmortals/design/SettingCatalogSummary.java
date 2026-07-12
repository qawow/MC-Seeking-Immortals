package com.xunxian.seekingimmortals.design;

import java.util.List;
import java.util.Optional;

public record SettingCatalogSummary(
        int declaredTechniqueCount,
        int declaredTechniqueFileCount,
        List<String> declaredTechniqueFiles,
        List<CatalogFileStatus> files
) {
    public SettingCatalogSummary {
        declaredTechniqueFiles = List.copyOf(declaredTechniqueFiles);
        files = List.copyOf(files);
    }

    public long presentFiles() {
        return files.stream().filter(CatalogFileStatus::present).count();
    }

    public long validFiles() {
        return files.stream().filter(CatalogFileStatus::valid).count();
    }

    public long invalidFiles() {
        return files.stream().filter(status -> status.present() && !status.valid()).count();
    }

    public int validEntryCount() {
        return files.stream().filter(CatalogFileStatus::valid).mapToInt(CatalogFileStatus::entryCount).sum();
    }

    public Optional<CatalogFileStatus> find(String relativePath) {
        return files.stream().filter(status -> status.relativePath().equals(relativePath)).findFirst();
    }

    public record CatalogFileStatus(String relativePath, boolean present, boolean valid, int entryCount, String error) {
        public static CatalogFileStatus valid(String relativePath, int entryCount) {
            return new CatalogFileStatus(relativePath, true, true, entryCount, "");
        }

        public static CatalogFileStatus invalid(String relativePath, String error) {
            return new CatalogFileStatus(relativePath, true, false, 0, error);
        }

        public static CatalogFileStatus missing(String relativePath) {
            return new CatalogFileStatus(relativePath, false, false, 0, "missing");
        }
    }
}
