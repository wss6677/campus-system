package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.CommentDTO;
import com.ivy.campus.vo.CommentVO;

public interface CommentService {

    PageResult<CommentVO> pageByAnnouncement(Long announcementId, Integer pageNum, Integer pageSize);

    Long add(CommentDTO dto);

    void audit(Long id, String status);

    void remove(Long id);
}
