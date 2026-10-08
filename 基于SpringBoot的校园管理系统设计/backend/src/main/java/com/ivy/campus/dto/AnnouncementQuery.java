package com.ivy.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "公告分页查询条件")
public class AnnouncementQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字：标题/主题词/正文模糊匹配")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态 DRAFT/PENDING/PUBLISHED/SCHEDULED/REJECTED/WITHDRAWN/ARCHIVED")
    private String status;

    @Schema(description = "可见范围 ALL/COLLEGE/CLASS/CUSTOM")
    private String visibility;

    @Schema(description = "紧急程度 NORMAL/IMPORTANT/URGENT")
    private String priority;

    @Schema(description = "校区")
    private String campus;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "是否只看置顶 1是0否")
    private Integer onlyTop;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "起始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认10")
    private Integer pageSize = 10;

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    public boolean hasStatus() {
        return status != null && !status.isBlank();
    }

    public boolean hasVisibility() {
        return visibility != null && !visibility.isBlank();
    }

    public boolean hasPriority() {
        return priority != null && !priority.isBlank();
    }

    public boolean hasCampus() {
        return campus != null && !campus.isBlank();
    }

    public boolean hasTimeRange() {
        return startTime != null && endTime != null;
    }
}
