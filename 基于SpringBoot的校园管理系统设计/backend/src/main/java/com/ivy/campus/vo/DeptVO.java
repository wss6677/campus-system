package com.ivy.campus.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "组织架构节点")
public class DeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "部门名称")
    private String name;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子节点")
    private List<DeptVO> children = new ArrayList<>();
}
