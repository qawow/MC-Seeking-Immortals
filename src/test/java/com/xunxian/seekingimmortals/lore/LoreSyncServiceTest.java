package com.xunxian.seekingimmortals.lore;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoreSyncServiceTest {
    @Test
    void bestiarySnapshotKeepsOnlyCanonicalCatalogIds() {
        BeastBestiaryService.BeastEntry entry = BeastBestiaryService.all().values().iterator().next();

        List<String> canonical = LoreSyncService.canonicalBestiaryIds(List.of(
                entry.id().toUpperCase(), entry.display(), "missing_beast_id", entry.id()));

        assertEquals(List.of(entry.id()), canonical);
    }
}
