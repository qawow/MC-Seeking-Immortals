package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbQuestBridgeServiceTest {
    private static final Path PACKAGED_ROOT = Path.of(
            "src/main/resources/seeking_immortals/ftbquests/quests");

    @Test
    void everyNativeChainHasAnExplicitChapterAndUnknownIdsFailClosed() {
        Set<String> nativeIds = ExtendedCatalogService.builtin().questChains().keySet();
        FtbQuestBridgeService.Snapshot snapshot = FtbQuestBridgeService.builtin();

        assertEquals(nativeIds, snapshot.chainToChapter().keySet(),
                "FTB mapping must be an explicit one-to-one native catalog projection");
        assertEquals(nativeIds.size(), snapshot.chainToChapter().size());
        assertTrue(FtbQuestBridgeService.allChainsMapped());
        assertTrue(FtbQuestBridgeService.chapterForChain("not_a_native_chain").isEmpty());
        assertTrue(FtbQuestBridgeService.chapterForChain(null).isEmpty());

        Set<String> chapterIds = snapshot.chapters().stream()
                .map(FtbQuestBridgeService.ChapterSeed::chapterId)
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(nativeIds.size(), snapshot.registeredChainIds().size());
        assertTrue(snapshot.chainToChapter().values().stream().allMatch(chapterIds::contains));
    }

    @Test
    void everyMappingPointsToAChapterThatReferencesTheNativeChain() throws IOException {
        FtbQuestBridgeService.Snapshot snapshot = FtbQuestBridgeService.builtin();
        Map<String, FtbQuestBridgeService.ChapterSeed> chapters = snapshot.chapters().stream()
                .collect(Collectors.toMap(FtbQuestBridgeService.ChapterSeed::chapterId, seed -> seed));

        for (Map.Entry<String, String> mapping : snapshot.chainToChapter().entrySet()) {
            FtbQuestBridgeService.ChapterSeed chapter = chapters.get(mapping.getValue());
            String snbt = Files.readString(PACKAGED_ROOT.resolve(chapter.relativePath()));
            Pattern reference = Pattern.compile("\\b" + Pattern.quote(mapping.getKey()) + "\\b");
            assertTrue(reference.matcher(snbt).find(), () -> mapping.getKey()
                    + " is mapped to a chapter that does not reference it: " + mapping.getValue());
        }
    }
}
