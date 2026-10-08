package com.ivy.campus.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "发布趋势")
public class TrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "总量")
    private Long count;

    @Schema(description = "发布量")
    private Long publishCount;

    @Schema(description = "阅读率")
    private Double readRate;
}
