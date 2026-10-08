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
@Schema(description = "评论")
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "评论人ID")
    private Long userId;

    @Schema(description = "评论人姓名")
    private String userName;

    @Schema(description = "评论人头像")
    private String userAvatar;

    @Schema(description = "评论人部门")
    private String deptName;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "状态 PENDING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "子评论")
    private List<CommentVO> children = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "评论时间")
    private LocalDateTime createTime;
}
