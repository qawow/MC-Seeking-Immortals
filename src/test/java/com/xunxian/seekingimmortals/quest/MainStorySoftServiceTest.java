package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainStorySoftServiceTest {
    @Test
    void indexesMainStoryChapters() {
        assertTrue(MainStorySoftService.chapterCount() >= 5);
    }

    @Test
    void chapterChainRefsArePresent() {
        assertFalse(MainStorySoftService.chainsForChapter("chapter_1_sect").isEmpty());
        assertTrue(MainStorySoftService.chainsForChapter("chapter_1_sect").contains("huangfeng_cultivation_path"));
    }
}
