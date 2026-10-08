package com.ivy.campus.service;

import com.ivy.campus.vo.DeptUnreadVO;
import com.ivy.campus.vo.ReceiptVO;

import java.util.List;

public interface ReceiptService {

    void markRead(Long announcementId);

    ReceiptVO statByAnnouncement(Long announcementId);

    List<DeptUnreadVO> unreadRank(Long announcementId);

    Long urge(Long announcementId, Long deptId);

    // 批量生成回执目标行，返回目标人数
    int generateTargets(Long announcementId);
}
