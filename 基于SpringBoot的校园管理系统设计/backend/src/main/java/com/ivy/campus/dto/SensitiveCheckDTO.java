package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 敏感词在线检测入参
 */
@Data
@Schema(description = "敏感词检测入参")
public class SensitiveCheckDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "待检测文本（公告标题或正文）")
    private String content;
}
