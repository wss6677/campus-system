package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告分类入参")
public class CategoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID，新增时为空")
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50")
    @Schema(description = "分类名称")
    private String name;

    @Size(max = 50, message = "分类编码长度不能超过50")
    @Schema(description = "分类编码")
    private String code;

    @Size(max = 20, message = "颜色值长度不能超过20")
    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "是否需要审核 1需要0免审")
    private Integer needAudit;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "状态 1启用0停用")
    private Integer status;

    @Size(max = 255, message = "描述长度不能超过255")
    @Schema(description = "描述")
    private String description;
}
