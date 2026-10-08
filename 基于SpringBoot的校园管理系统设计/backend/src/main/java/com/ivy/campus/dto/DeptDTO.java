package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "组织架构入参")
public class DeptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID，新增时为空")
    private Long id;

    @Schema(description = "父级ID，顶级为0")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过50")
    @Schema(description = "部门名称")
    private String name;

    @Size(max = 50, message = "部门编码长度不能超过50")
    @Schema(description = "部门编码")
    private String code;

    @Schema(description = "类型 SCHOOL/COLLEGE/DEPT/CLASS")
    private String type;

    @Schema(description = "校区")
    private String campus;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "负责人")
    private String leaderName;

    @Schema(description = "状态 1正常0停用")
    private Integer status;
}
