package com.ivy.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "操作日志分页查询条件")
public class LogQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "操作人用户名")
    private String username;

    @Schema(description = "模块名")
    private String module;

    @Schema(description = "操作名")
    private String operation;

    @Schema(description = "状态 1成功0失败")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "起始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，默认1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认10")
    private Integer pageSize = 10;

    public boolean hasUsername() {
        return username != null && !username.isBlank();
    }

    public boolean hasModule() {
        return module != null && !module.isBlank();
    }

    public boolean hasOperation() {
        return operation != null && !operation.isBlank();
    }

    public boolean hasTimeRange() {
        return startTime != null && endTime != null;
    }
}
