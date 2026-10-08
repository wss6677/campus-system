package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.entity.AnnouncementAttachment;
import com.ivy.campus.mapper.AnnouncementAttachmentMapper;
import com.ivy.campus.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final long MAX_SIZE = 50L * 1024 * 1024;

    @Value("${campus.upload.path:D:/campus-upload/}")
    private String uploadPath;

    @Value("${campus.upload.url-prefix:/upload/}")
    private String urlPrefix;

    private final AnnouncementAttachmentMapper attachmentMapper;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException("上传文件不能超过50MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "unnamed";
        }
        String suffix = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > -1) {
            suffix = originalName.substring(dotIndex);
        }
        String datePath = LocalDate.now().format(DAY_FORMAT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        File dir = new File(normalizeBase(), datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("创建上传目录失败");
        }
        File target = new File(dir, fileName);
        try {
            file.transferTo(target);
        } catch (Exception e) {
            log.error("文件保存失败：{}", e.getMessage(), e);
            throw new BizException("文件保存失败");
        }
        return buildUrl(datePath, fileName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void download(Long attachmentId, HttpServletResponse response) {
        AnnouncementAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BizException("附件不存在");
        }
        File target = resolveFile(attachment.getFilePath());
        if (target == null || !target.exists()) {
            throw new BizException("附件文件不存在");
        }
        String fileName = attachment.getFileName() == null ? target.getName() : attachment.getFileName();
        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setContentLengthLong(target.length());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encode(fileName) + "\"");
        try (OutputStream outputStream = response.getOutputStream()) {
            Files.copy(target.toPath(), outputStream);
            outputStream.flush();
        } catch (Exception e) {
            log.error("附件下载失败，attachmentId={}，原因：{}", attachmentId, e.getMessage(), e);
            throw new BizException("附件下载失败");
        }
        try {
            attachmentMapper.update(null, Wrappers.<AnnouncementAttachment>lambdaUpdate()
                    .eq(AnnouncementAttachment::getId, attachmentId)
                    .setSql("download_count = download_count + 1"));
        } catch (Exception e) {
            log.warn("附件下载次数自增失败，attachmentId={}，原因：{}", attachmentId, e.getMessage());
        }
    }

    private String buildUrl(String datePath, String fileName) {
        String prefix = urlPrefix == null || urlPrefix.isBlank() ? "/upload/" : urlPrefix;
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + datePath + "/" + fileName;
    }

    private String normalizeBase() {
        String base = uploadPath == null || uploadPath.isBlank() ? "D:/campus-upload/" : uploadPath;
        return base.replace("\\", "/").replaceAll("/+$", "");
    }

    private File resolveFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        String relative = filePath;
        String prefix = urlPrefix == null || urlPrefix.isBlank() ? "/upload/" : urlPrefix;
        if (relative.startsWith(prefix)) {
            relative = relative.substring(prefix.length());
        }
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return new File(normalizeBase(), relative);
    }

    private String encode(String text) {
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return "attachment";
        }
    }
}
