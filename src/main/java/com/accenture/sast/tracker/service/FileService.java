package com.accenture.sast.tracker.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

@Service
public class FileService {

    public File getFile(final String baseDir, final String filename) {
        final Path reportPath = Path.of(baseDir).toAbsolutePath().normalize();
        final Path filePath = reportPath.resolve(filename).normalize();
        if (!filePath.startsWith(reportPath)) {
            throw new FileServiceException("Invalid filename: " + filename);
        }
        return filePath.toFile();
    }

    public static class FileServiceException extends RuntimeException {
        public FileServiceException(final String message) {
            super(message);
        }
    }

}
