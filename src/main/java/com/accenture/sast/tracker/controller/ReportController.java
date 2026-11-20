package com.accenture.sast.tracker.controller;

import com.accenture.sast.tracker.service.FileService;
import com.accenture.sast.tracker.service.FileService.FileServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    @Value("${report.dir:C:/reports/}")
    private final String reportDir;

    private final FileService fileService;

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) throws IOException {

        final File file = fileService.getFile(reportDir, filename);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        final byte[] content = Files.readAllBytes(file.toPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(content));
    }

    @ExceptionHandler(FileServiceException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFilename(FileServiceException rse) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid report filename"));
    }
}
