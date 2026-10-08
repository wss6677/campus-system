package com.ivy.campus.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "部门未读统计")
public class DeptUnreadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "应读总数")
    private Long total;

    @Schema(description = "未读数")
    private Long unread;

    @Schema(description = "未读率")
    private Double rate;
}
