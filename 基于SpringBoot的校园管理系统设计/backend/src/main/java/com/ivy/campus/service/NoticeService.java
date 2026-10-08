package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.vo.NoticeVO;

public interface NoticeService {

    PageResult<NoticeVO> myNotices(Integer pageNum, Integer pageSize, Integer readFlag);

    Long unreadCount();

    void read(Long id);

    void readAll();

    // 单条通知推送
    void send(Long userId, String title, String content, String type, Long bizId);

    // 批量通知推送
    void sendBatch(java.util.List<Long> userIds, String title, String content, String type, Long bizId);
}
