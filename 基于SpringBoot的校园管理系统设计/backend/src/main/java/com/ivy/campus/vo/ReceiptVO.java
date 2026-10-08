package com.ivy.campus.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "回执统计")
public class ReceiptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "应读总人数")
    private Long total;

    @Schema(description = "已读数")
    private Long readCount;

    @Schema(description = "未读数")
    private Long unreadCount;

    @Schema(description = "阅读率")
    private Double rate;

    @Schema(description = "部门未读排行")
    private List<DeptUnreadVO> deptRanks = new ArrayList<>();
}
