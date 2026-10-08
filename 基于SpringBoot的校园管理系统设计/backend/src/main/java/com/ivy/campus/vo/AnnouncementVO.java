package com.ivy.campus.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "公告列表项")
public class AnnouncementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类颜色")
    private String categoryColor;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者姓名")
    private String authorName;

    @Schema(description = "发布部门ID")
    private Long deptId;

    @Schema(description = "发布部门名称")
    private String deptName;

    @Schema(description = "校区")
    private String campus;

    @Schema(description = "状态 DRAFT/PENDING/PUBLISHED/SCHEDULED/REJECTED/WITHDRAWN/ARCHIVED")
    private String status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "可见范围 ALL/COLLEGE/CLASS/CUSTOM")
    private String visibility;

    @Schema(description = "紧急程度 NORMAL/IMPORTANT/URGENT")
    private String priority;

    @Schema(description = "是否置顶 1是0否")
    private Integer isTop;

    @Schema(description = "是否轮播 1是0否")
    private Integer isBanner;

    @Schema(description = "是否红头 1是0否")
    private Integer isRedHead;

    @Schema(description = "是否允许评论 1是0否")
    private Integer allowComment;

    @Schema(description = "是否需要回执 1是0否")
    private Integer needReceipt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "失效时间")
    private LocalDateTime expireTime;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "已读回执数")
    private Integer receiptCount;

    @Schema(description = "目标人数")
    private Integer targetCount;

    @Schema(description = "评论数")
    private Long commentCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
