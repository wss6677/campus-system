package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.vo.AnnouncementVO;
import com.ivy.campus.vo.ApprovalLogVO;

import java.util.List;

public interface ApprovalService {

    List<ApprovalLogVO> timeline(Long announcementId);

    PageResult<AnnouncementVO> todoList(Integer pageNum, Integer pageSize);
}
