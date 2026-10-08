package com.ivy.campus.service;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.LogQuery;
import com.ivy.campus.entity.OperationLog;

public interface LogService {

    PageResult<OperationLog> page(LogQuery query);

    // 异步落库操作日志
    void asyncSave(OperationLog log);
}
