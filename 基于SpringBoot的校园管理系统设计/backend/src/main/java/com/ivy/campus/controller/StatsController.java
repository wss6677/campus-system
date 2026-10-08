package com.ivy.campus.controller;

import com.ivy.campus.common.R;
import com.ivy.campus.service.StatsService;
import com.ivy.campus.vo.AnnouncementVO;
import com.ivy.campus.vo.CategoryStatVO;
import com.ivy.campus.vo.DeptUnreadVO;
import com.ivy.campus.vo.StatsOverviewVO;
import com.ivy.campus.vo.TrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Validated
@Tag(name = "统计分析", description = "总览指标、趋势、分类占比、部门未读、热门公告")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    @Operation(summary = "统计总览")
    public R<StatsOverviewVO> overview() {
        return R.ok(statsService.overview());
    }

    @GetMapping("/trend")
    @Operation(summary = "发布趋势")
    public R<List<TrendVO>> trend(@RequestParam(defaultValue = "15") Integer days) {
        return R.ok(statsService.trend(days));
    }

    @GetMapping("/category")
    @Operation(summary = "分类统计")
    public R<List<CategoryStatVO>> category(@RequestParam(defaultValue = "30") Integer days) {
        return R.ok(statsService.categoryStats(days));
    }

    @GetMapping("/deptUnread")
    @Operation(summary = "部门未读统计")
    public R<List<DeptUnreadVO>> deptUnread() {
        return R.ok(statsService.deptUnread());
    }

    @GetMapping("/topViewed")
    @Operation(summary = "热门公告")
    public R<List<AnnouncementVO>> topViewed(@RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statsService.topViewed(limit));
    }
}
