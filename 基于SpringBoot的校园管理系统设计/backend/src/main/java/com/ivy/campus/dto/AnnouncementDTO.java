package com.ivy.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ivy.campus.entity.AnnouncementAttachment;
import com.ivy.campus.entity.AnnouncementScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "公告保存入参")
public class AnnouncementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    @Schema(description = "标题")
    private String title;

    @Size(max = 200, message = "副标题长度不能超过200")
    @Schema(description = "副标题")
    private String subtitle;

    @Size(max = 500, message = "摘要长度不能超过500")
    @Schema(description = "摘要")
    private String summary;

    @NotBlank(message = "正文不能为空")
    @Schema(description = "正文")
    private String content;

    @Schema(description = "封面图")
    private String coverImage;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "发布部门ID")
    private Long deptId;

    @Schema(description = "校区")
    private String campus;

    @Schema(description = "可见范围 ALL/COLLEGE/CLASS/CUSTOM")
    private String visibility;

    @Schema(description = "紧急程度 NORMAL/IMPORTANT/URGENT")
    private String priority;

    @Schema(description = "是否置顶 1是0否")
    private Integer isTop;

    @Schema(description = "是否轮播 1是0否")
    private Integer isBanner;

    @Schema(description = "是否红头文件 1是0否")
    private Integer isRedHead;

    @Schema(description = "是否允许评论 1是0否")
    private Integer allowComment;

    @Schema(description = "是否需要回执 1是0否")
    private Integer needReceipt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "计划发布时间，为空表示立即发布")
    private LocalDateTime publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "失效时间")
    private LocalDateTime expireTime;

    @Schema(description = "主题词，逗号分隔")
    private String keywords;

    @Schema(description = "标签名称集合")
    private List<String> tagNames;

    @Schema(description = "可见范围明细集合")
    private List<AnnouncementScope> scopeList;

    @Schema(description = "附件集合")
    private List<AnnouncementAttachment> attachments;
}
