package com.accenture.sast.tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "API_SECRET=test-secret",
        "app.report-dir=build/test-reports"
})
@AutoConfigureMockMvc
public class ReportControllerTest {

    @Autowired
    MockMvc mvc;

    private final Path reportDir = Path.of("build/test-reports");

    @BeforeEach
    void prepareDirectory() throws Exception {
        Files.createDirectories(reportDir);
        try (var files = Files.list(reportDir)) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        }
    }

    @Test
    void download_existingFile_returns200AndContent() throws Exception {
        Path file = reportDir.resolve("report.txt");
        byte[] payload = "hello report".getBytes(StandardCharsets.UTF_8);
        Files.write(file, payload);

        mvc.perform(get("/api/reports/download").param("filename", "report.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.txt\""))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(content().bytes(payload));
    }

    @Test
    void download_missingFile_returns404() throws Exception {
        mvc.perform(get("/api/reports/download").param("filename", "missing.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void download_pathTraversalAttempt_returns400() throws Exception {
        mvc.perform(get("/api/reports/download").param("filename", "../../etc/passwd"))
                .andExpect(status().isBadRequest());
    }
}
