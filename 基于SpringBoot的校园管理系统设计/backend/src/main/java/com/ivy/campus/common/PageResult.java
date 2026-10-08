package com.ivy.campus.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records;

    private Long total;

    private Long pageNum;

    private Long pageSize;

    private Long pages;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long pageNum, Long pageSize, Long pages) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        long size = pageSize <= 0 ? 10L : pageSize;
        long pages = (total + size - 1) / size;
        return new PageResult<>(records, total, pageNum, size, pages);
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(Collections.emptyList(), 0L, pageNum, pageSize);
    }
}
