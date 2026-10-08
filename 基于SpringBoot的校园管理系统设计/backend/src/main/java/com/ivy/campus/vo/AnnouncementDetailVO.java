package com.ivy.campus.vo;

import com.ivy.campus.entity.AnnouncementAttachment;
import com.ivy.campus.entity.AnnouncementScope;
import com.ivy.campus.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公告详情")
public class AnnouncementDetailVO extends AnnouncementVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "主题词")
    private String keywords;

    @Schema(description = "审核意见")
    private String auditRemark;

    @Schema(description = "审核人姓名")
    private String auditUserName;

    @Schema(description = "附件集合")
    private List<AnnouncementAttachment> attachments;

    @Schema(description = "标签集合")
    private List<Tag> tags;

    @Schema(description = "可见范围明细")
    private List<AnnouncementScope> scopes;

    @Schema(description = "审批轨迹")
    private List<ApprovalLogVO> approvalLogs;

    @Schema(description = "回执完成率")
    private Double receiptRate;
}
