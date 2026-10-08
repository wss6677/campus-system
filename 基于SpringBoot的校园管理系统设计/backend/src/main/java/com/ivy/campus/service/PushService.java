package com.ivy.campus.service;

import com.ivy.campus.dto.PushDTO;
import com.ivy.campus.entity.Announcement;

import java.util.List;

public interface PushService {

    // 公告发布后的多渠道推送，异步写 ann_push_log
    void push(Announcement announcement);

    // 指定渠道推送
    void push(PushDTO dto);

    // 渠道推送明细
    List<String> channels();
}
