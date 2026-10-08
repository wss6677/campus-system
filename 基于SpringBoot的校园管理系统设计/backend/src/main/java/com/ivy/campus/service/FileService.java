package com.ivy.campus.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    // 上传文件，按日期分目录，返回可访问URL
    String upload(MultipartFile file);

    // 下载附件，累加下载次数
    void download(Long attachmentId, HttpServletResponse response);
}
