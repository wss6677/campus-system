package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "评论入参")
public class CommentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "公告ID不能为空")
    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "父评论ID，0或空表示顶层评论")
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500")
    @Schema(description = "评论内容")
    private String content;
}
