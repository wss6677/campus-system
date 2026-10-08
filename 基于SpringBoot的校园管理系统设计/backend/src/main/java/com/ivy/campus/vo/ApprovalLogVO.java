package com.ivy.campus.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "审批轨迹节点")
public class ApprovalLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "节点 SUBMIT/DEPT_AUDIT/UNIV_AUDIT/PUBLISH/REJECT/WITHDRAW/ARCHIVE/UPDATE")
    private String node;

    @Schema(description = "节点文本")
    private String nodeText;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "动作说明")
    private String action;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "变更前状态")
    private String fromStatus;

    @Schema(description = "变更后状态")
    private String toStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
