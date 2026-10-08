package com.ivy.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "审核入参")
public class AuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "是否通过 true通过 false驳回")
    private Boolean pass;

    @Size(max = 500, message = "审核意见长度不能超过500")
    @Schema(description = "审核意见")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "指定发布时间，未来时间将进入定时发布")
    private LocalDateTime publishTime;
}
