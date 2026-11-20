package com.accenture.sast.tracker.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final String REPORT_DIR = "C:/reports/";

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) throws IOException {
        Path reportPath = Paths.get(REPORT_DIR).toRealPath();
        Path filePath = reportPath.resolve(filename).normalize();

        if (!filePath.startsWith(reportPath)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Path realFilePath = filePath.toRealPath();
        if (!realFilePath.startsWith(reportPath)) {
            return ResponseEntity.badRequest().build();
        }

        byte[] content = Files.readAllBytes(realFilePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(content));
    }
}
