package com.ivy.campus.controller;

import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.SensitiveCheckDTO;
import com.ivy.campus.entity.SensitiveWord;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容安全：敏感词库维护（超级系统管理员）与在线内容检测（发布/审核人员）
 */
@RestController
@RequestMapping("/api/sensitive")
@RequiredArgsConstructor
@Validated
@Tag(name = "内容安全", description = "敏感词库维护与内容检测")
public class SensitiveWordController {

    private static final String LEVEL_BLOCK = "BLOCK";

    private static final String LEVEL_WARN = "WARN";

    private final SensitiveWordService sensitiveWordService;

    @GetMapping("/list")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "敏感词全量列表")
    public R<List<SensitiveWord>> list() {
        return R.ok(sensitiveWordService.listAll());
    }

    @GetMapping("/page")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "敏感词分页查询")
    public R<PageResult<SensitiveWord>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String level) {
        return R.ok(sensitiveWordService.page(pageNum, pageSize, keyword, level));
    }

    @GetMapping("/{id}")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "敏感词详情")
    public R<SensitiveWord> detail(@PathVariable Long id) {
        return R.ok(sensitiveWordService.getById(id));
    }

    @PostMapping
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "新增敏感词")
    public R<Long> save(@RequestBody SensitiveWord entity) {
        normalize(entity);
        return R.ok("新增成功", sensitiveWordService.save(entity));
    }

    @PutMapping("/{id}")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "修改敏感词")
    public R<Void> update(@PathVariable Long id, @RequestBody SensitiveWord entity) {
        normalize(entity);
        sensitiveWordService.update(id, entity);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "删除敏感词")
    public R<Void> remove(@PathVariable Long id) {
        sensitiveWordService.remove(id);
        return R.ok("删除成功", null);
    }

    @PostMapping("/check")
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR", "DEPT_PUBLISHER"})
    @Operation(summary = "内容安全在线检测：返回命中的敏感词与级别")
    public R<Map<String, Object>> check(@RequestBody SensitiveCheckDTO dto) {
        String content = dto == null || dto.getContent() == null ? "" : dto.getContent();
        List<Map<String, Object>> hits = new ArrayList<>();
        boolean blocked = false;
        for (SensitiveWord word : sensitiveWordService.listAll()) {
            if (word.getWord() == null || word.getWord().isBlank()) {
                continue;
            }
            if (!content.contains(word.getWord())) {
                continue;
            }
            String level = word.getLevel() == null ? LEVEL_WARN : word.getLevel().toUpperCase();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("word", word.getWord());
            item.put("level", level);
            item.put("replacement", word.getReplacement());
            hits.add(item);
            if (LEVEL_BLOCK.equals(level)) {
                blocked = true;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("blocked", blocked);
        result.put("hitCount", hits.size());
        result.put("hits", hits);
        return R.ok(result);
    }

    /** 校验并规整入参：词不能为空，级别只能是 BLOCK / WARN，默认启用 */
    private void normalize(SensitiveWord entity) {
        if (entity == null || entity.getWord() == null || entity.getWord().isBlank()) {
            throw new BizException("敏感词不能为空");
        }
        entity.setWord(entity.getWord().trim());
        String level = entity.getLevel() == null ? "" : entity.getLevel().trim().toUpperCase();
        if (!LEVEL_BLOCK.equals(level) && !LEVEL_WARN.equals(level)) {
            throw new BizException("敏感词级别只能是 BLOCK（阻断）或 WARN（警告）");
        }
        entity.setLevel(level);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
    }
}
