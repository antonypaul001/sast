package com.accenture.sast.tracker.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final Path reportDir;

    public ReportController(@Value("${app.report-dir:./reports}") String reportDir) {
        this.reportDir = Path.of(reportDir).toAbsolutePath().normalize();
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) throws IOException {
        Path resolved = reportDir
                .resolve(filename)
                .normalize();

        if (!resolved.startsWith(reportDir)) {
            return ResponseEntity.badRequest().build();
        }

        if (!Files.isRegularFile(resolved)) {
            return ResponseEntity.notFound().build();
        }
        String safeName = resolved.getFileName().toString();

        byte[] content = Files.readAllBytes(resolved);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(content));
    }
}
