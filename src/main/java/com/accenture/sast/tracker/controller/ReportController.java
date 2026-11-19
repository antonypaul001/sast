package com.accenture.sast.tracker.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final Path reportsDir;

    public ReportController(@Value("${app.reports-dir}") Path reportsDir) {
        this.reportsDir = reportsDir;
    }

    // ── VULNERABILITY V5: Path Traversal (CWE-22) ────────────────────────────
    // The filename parameter is used directly when building the File path.
    // An attacker can supply: ../../Windows/System32/drivers/etc/hosts
    // to read any file the process has access to, regardless of REPORT_DIR.
    // Fix: resolve the path, then verify it starts with REPORT_DIR:
    // Path resolved = Paths.get(REPORT_DIR).resolve(filename).normalize();
    // if (!resolved.startsWith(Paths.get(REPORT_DIR))) throw new ...;
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) throws IOException {
        Path resolved = reportsDir.resolve(filename).normalize();

        if (!resolved.startsWith(reportsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        File file = resolved.toFile();
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = Files.readAllBytes(file.toPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(content));
    }
}
