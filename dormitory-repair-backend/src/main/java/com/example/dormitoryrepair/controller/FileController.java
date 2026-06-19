package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".pdf"
    );

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件名不能为空");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "不支持的文件类型，仅允许: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String relativeDir = LocalDate.now().getYear() + "/" + String.format("%02d", LocalDate.now().getMonthValue());
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = basePath.resolve(relativeDir);
        Files.createDirectories(targetDir);

        Path targetFile = targetDir.resolve(fileName);
        file.transferTo(targetFile);

        Map<String, String> result = new HashMap<>();
        result.put("url", "/uploads/" + relativeDir.replace("\\", "/") + "/" + fileName);
        result.put("originalFilename", originalFilename);
        result.put("contentType", file.getContentType());
        return ApiResponse.success(result);
    }
}
