package com.ivy.campus.controller;

import com.ivy.campus.common.R;
import com.ivy.campus.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Validated
@Tag(name = "文件管理", description = "附件上传与下载")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传附件")
    public R<String> upload(@RequestPart("file") MultipartFile file) {
        return R.ok("上传成功", fileService.upload(file));
    }

    @GetMapping("/download/{attachmentId}")
    @Operation(summary = "下载附件")
    public void download(@PathVariable Long attachmentId, HttpServletResponse response) {
        fileService.download(attachmentId, response);
    }
}
