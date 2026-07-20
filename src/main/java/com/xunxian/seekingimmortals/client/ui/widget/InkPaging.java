package com.xunxian.seekingimmortals.client.ui.widget;

/**
 * 云笈墨卷 shared pagination math. Screens keep their public
 * {@code clampPage/pageStart/pageEnd/maxPage} statics (paging-test contract)
 * as one-line delegates to this class.
 */
public final class InkPaging {
    private InkPaging() {}

    public static int clampPage(int page, int maxPage) {
        return Math.max(0, Math.min(page, Math.max(0, maxPage)));
    }

    public static int maxPage(int entryCount, int pageSize) {
        int size = Math.max(1, pageSize);
        return entryCount <= 0 ? 0 : (entryCount - 1) / size;
    }

    public static int pageStart(int page, int pageSize) {
        return Math.max(0, page) * Math.max(1, pageSize);
    }

    public static int pageEnd(int page, int entryCount, int pageSize) {
        return Math.min(entryCount, pageStart(page, pageSize) + Math.max(1, pageSize));
    }
}
