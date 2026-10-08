package com.ivy.campus.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "统计总览")
public class StatsOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告总数")
    private Long totalAnnouncement;

    @Schema(description = "今日发布数")
    private Long todayPublish;

    @Schema(description = "待审核数")
    private Long pendingAudit;

    @Schema(description = "已发布数")
    private Long published;

    @Schema(description = "本月发布数")
    private Long publishedThisMonth;

    @Schema(description = "用户总数")
    private Long totalUser;

    @Schema(description = "今日活跃数")
    private Long todayActive;

    @Schema(description = "总阅读数")
    private Long totalRead;

    @Schema(description = "未读催办数")
    private Long unreadUrge;

    @Schema(description = "待审评论数")
    private Long commentPending;

    @Schema(description = "分类占比最高的分类名")
    private String topCategory;
}
