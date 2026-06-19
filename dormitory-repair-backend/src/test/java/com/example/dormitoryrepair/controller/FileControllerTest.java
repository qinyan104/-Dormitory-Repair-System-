package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadStoresFileAndReturnsPreviewUrl() throws Exception {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "demo".getBytes()
        );

        Map<String, String> data = controller.upload(file).getData();
        String url = data.get("url");
        String relativePath = url.replace("/uploads/", "");

        assertTrue(url.startsWith("/uploads/"));
        assertTrue(Files.exists(tempDir.resolve(relativePath)));
        assertEquals("test.png", data.get("originalFilename"));
    }

    @Test
    void uploadRejectsHtmlFile() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.html",
                "text/html",
                "<script>alert('xss')</script>".getBytes()
        );

        assertThrows(BusinessException.class, () -> controller.upload(file));
    }

    @Test
    void uploadRejectsExecutableFile() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "binary".getBytes()
        );

        assertThrows(BusinessException.class, () -> controller.upload(file));
    }
}
