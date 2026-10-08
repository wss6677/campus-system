package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "回执催办入参")
public class UrgeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "公告ID不能为空")
    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "部门ID，为空表示催办全部未读用户")
    private Long deptId;

    @Size(max = 200, message = "催办语长度不能超过200")
    @Schema(description = "催办语")
    private String remark;
}
