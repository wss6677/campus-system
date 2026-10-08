package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.LogQuery;
import com.ivy.campus.entity.OperationLog;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@Validated
@RequireRole({"SUPER_ADMIN"})
@Tag(name = "操作日志", description = "系统操作日志查询")
public class LogController {

    private final LogService logService;

    @GetMapping("/page")
    @Operation(summary = "操作日志分页")
    public R<PageResult<OperationLog>> page(@Valid LogQuery query) {
        return R.ok(logService.page(query));
    }
}
