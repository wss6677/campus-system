package com.ivy.campus.service.impl;

import com.ivy.campus.dto.PushDTO;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.entity.PushLog;
import com.ivy.campus.mapper.PushLogMapper;
import com.ivy.campus.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushServiceImpl implements PushService {

    private static final String CHANNEL_SITE = "SITE";
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_SMS = "SMS";

    private final PushLogMapper pushLogMapper;

    @Override
    @Async("pushExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void push(Announcement announcement) {
        if (announcement == null || announcement.getId() == null) {
            return;
        }
        int target = announcement.getTargetCount() == null ? 0 : announcement.getTargetCount();
        for (String channel : channels()) {
            try {
                writeLog(announcement, channel, target);
            } catch (Exception e) {
                log.warn("公告推送写日志失败，announcementId={}，channel={}，原因：{}",
                        announcement.getId(), channel, e.getMessage());
            }
        }
        log.info("公告推送完成，announcementId={}，渠道={}，目标人数={}", announcement.getId(), channels(), target);
    }

    @Override
    @Async("pushExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void push(PushDTO dto) {
        if (dto == null || dto.getAnnouncementId() == null) {
            return;
        }
        Announcement announcement = new Announcement();
        announcement.setId(dto.getAnnouncementId());
        announcement.setTitle(dto.getTitle());
        announcement.setSummary(dto.getSummary());
        announcement.setTargetCount(dto.getTargetCount() == null ? 0 : dto.getTargetCount());
        List<String> channels = new ArrayList<>();
        if (dto.getChannel() == null || dto.getChannel().isBlank()) {
            channels.addAll(channels());
        } else {
            channels.add(dto.getChannel().toUpperCase());
        }
        for (String channel : channels) {
            writeLog(announcement, channel, announcement.getTargetCount());
        }
    }

    @Override
    public List<String> channels() {
        return Arrays.asList(CHANNEL_SITE, CHANNEL_EMAIL, CHANNEL_SMS);
    }

    private void writeLog(Announcement announcement, String channel, int target) {
        PushLog pushLog = new PushLog();
        pushLog.setAnnouncementId(announcement.getId());
        pushLog.setChannel(channel);
        pushLog.setTargetCount(target);
        pushLog.setSuccessCount(target);
        pushLog.setFailCount(0);
        pushLog.setStatus(1);
        pushLog.setErrorMsg(null);
        pushLog.setCreateTime(LocalDateTime.now());
        pushLogMapper.insert(pushLog);
    }
}
