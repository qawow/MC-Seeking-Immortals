package com.xunxian.seekingimmortals.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainStorySoftServiceTest {
    @Test
    void indexesMainStoryChapters() {
        assertTrue(MainStorySoftService.chapterCount() >= 5);
    }
}
