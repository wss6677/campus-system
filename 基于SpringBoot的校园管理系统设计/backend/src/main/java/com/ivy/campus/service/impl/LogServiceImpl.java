package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.LogQuery;
import com.ivy.campus.entity.OperationLog;
import com.ivy.campus.mapper.OperationLogMapper;
import com.ivy.campus.service.LogService;
import com.ivy.campus.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl implements LogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<OperationLog> page(LogQuery query) {
        LogQuery condition = query == null ? new LogQuery() : query;
        IPage<OperationLog> page = operationLogMapper.selectLogPage(
                PageUtils.of(condition.getPageNum(), condition.getPageSize()), condition);
        return PageResult.of(page);
    }

    @Override
    @Async("pushExecutor")
    public void asyncSave(OperationLog operationLog) {
        if (operationLog == null) {
            return;
        }
        try {
            if (operationLog.getCreateTime() == null) {
                operationLog.setCreateTime(LocalDateTime.now());
            }
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.warn("操作日志落库失败：{}", e.getMessage());
        }
    }
}
