package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告模板入参")
public class TemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID，新增时为空")
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "关联分类ID")
    private Long categoryId;

    @Size(max = 200, message = "模板标题长度不能超过200")
    @Schema(description = "模板标题")
    private String title;

    @NotBlank(message = "模板内容不能为空")
    @Schema(description = "模板内容")
    private String content;

    @Schema(description = "状态 1启用0停用")
    private Integer status;
}
