package com.accenture.sast.tracker.controller;

import com.accenture.sast.tracker.config.RandomConfig;
import com.accenture.sast.tracker.service.FileService;
import com.accenture.sast.tracker.service.FileService.FileServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.nio.file.Files.write;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@Import(RandomConfig.class)
@TestPropertySource(properties = "report.dir=temp")
class ReportControllerTest {

    @MockitoBean
    private FileService mockFileService;

    @Autowired
    private MockMvc mvc;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mockFileService);
    }

    @TempDir
    private Path tempDir;

    @Test
    void shouldDownloadReport() throws Exception {
        final var expected = "Test report content".getBytes(StandardCharsets.UTF_8);
        final var report = write(tempDir.resolve("test.txt"), expected);

        when(mockFileService.getFile("temp", "test.txt"))
                .thenReturn(report.toFile());

        mvc.perform(get("/api/reports/download").param("filename", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(content().bytes(expected));

        verify(mockFileService).getFile("temp", "test.txt");
    }

    @Test
    void shouldReturn404WhenReportIsMissing() throws Exception {
        when(mockFileService.getFile("temp", "missing.txt"))
                .thenReturn(tempDir.resolve("missing.txt").toFile());

        mvc.perform(get("/api/reports/download").param("filename", "missing.txt"))
                .andExpect(status().isNotFound());

        verify(mockFileService).getFile("temp", "missing.txt");
    }

    @Test
    void shouldReturn400WhenServiceRejectsFilename() throws Exception {
        when(mockFileService.getFile("temp", "rejected.txt"))
                .thenThrow(new FileServiceException("Filename rejected by service"));

        mvc.perform(get("/api/reports/download").param("filename", "rejected.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid report filename"));

        verify(mockFileService).getFile("temp", "rejected.txt");
    }
}