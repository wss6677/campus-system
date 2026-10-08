package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.service.NoticeService;
import com.ivy.campus.vo.NoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
@Validated
@Tag(name = "通知管理", description = "站内通知列表与已读")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/page")
    @Operation(summary = "我的通知分页")
    public R<PageResult<NoticeVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) Integer readFlag) {
        return R.ok(noticeService.myNotices(pageNum, pageSize, readFlag));
    }

    @GetMapping("/unread")
    @Operation(summary = "未读通知数")
    public R<Long> unread() {
        return R.ok(noticeService.unreadCount());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记通知已读")
    public R<Void> read(@PathVariable Long id) {
        noticeService.read(id);
        return R.ok("标记成功", null);
    }

    @PostMapping("/readAll")
    @Operation(summary = "全部已读")
    public R<Void> readAll() {
        noticeService.readAll();
        return R.ok("全部已读", null);
    }
}
