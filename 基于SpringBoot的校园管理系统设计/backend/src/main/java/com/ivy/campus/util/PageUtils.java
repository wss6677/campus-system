package com.ivy.campus.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数工具，默认第 1 页、每页 10 条，每页上限 100 条
 */
public final class PageUtils {

    private static final long DEFAULT_PAGE_NUM = 1L;

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 100L;

    private PageUtils() {
    }

    public static <T> Page<T> of(Integer pageNum, Integer pageSize) {
        long current = (pageNum == null || pageNum <= 0) ? DEFAULT_PAGE_NUM : pageNum;
        long size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : pageSize;
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        return new Page<>(current, size);
    }
}
