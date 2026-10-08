package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "消息推送入参")
public class PushDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "公告摘要")
    private String summary;

    @Schema(description = "推送渠道 SITE/EMAIL/SMS/WECHAT/DINGTALK")
    private String channel;

    @Schema(description = "接收用户ID集合，为空表示按公告可见范围计算")
    private List<Long> userIds;

    @Schema(description = "接收部门ID集合")
    private List<Long> deptIds;

    @Schema(description = "目标人数")
    private Integer targetCount;

    @Schema(description = "是否同步推送 1同步0异步")
    private Integer sync;
}
