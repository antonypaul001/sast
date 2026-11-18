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

    private static final Path REPORT_DIR = Paths.get("C:/reports").toAbsolutePath().normalize();

    // ── VULNERABILITY V5: Path Traversal (CWE-22) ────────────────────────────
    // The filename parameter is used directly when building the File path.
    // An attacker can supply: ../../Windows/System32/drivers/etc/hosts
    // to read any file the process has access to, regardless of REPORT_DIR.
    // Fix: resolve the path, then verify it starts with REPORT_DIR:
    //   Path resolved = Paths.get(REPORT_DIR).resolve(filename).normalize();
    //   if (!resolved.startsWith(Paths.get(REPORT_DIR))) throw new ...;
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String filename) throws IOException {
        
        // Resolve the requested filename against REPORT_DIR
        Path filePath = REPORT_DIR.resolve(filename)
                .normalize()
                .toAbsolutePath();

        // Security check: file must remain inside REPORT_DIR        
        if (!filePath.startsWith(REPORT_DIR)) {
            return ResponseEntity.badRequest().build();
        }

        // Check it exists and is a regular file
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = Files.readAllBytes(filePath);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName() + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(content));
    }
}
