package com.accenture.sast.tracker.service;

import com.accenture.sast.tracker.service.FileService.FileServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileServiceTest {

    private final FileService fileService = new FileService();

    @Test
    void getFileResolvesFilenameAgainstBaseDirectory() {
        final var file = fileService.getFile("files", "example.txt");
        assertEquals("example.txt", file.getName());
        assertEquals("files", file.getParentFile().getName());
    }

    @Test
    void getFileRejectsPathOutsideBaseDirectory() {
        assertThrows(FileServiceException.class,
                () -> fileService.getFile("files", "../../secrets.txt"));
    }
}