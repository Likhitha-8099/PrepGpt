package com.prepgpt.service;

import com.prepgpt.exception.BadRequestException;
import jakarta.annotation.PreDestroy;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ResumeParserService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeParserService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB limit
    private static final int MAX_EXTRACTED_CHARS = 100_000;
    private static final long PARSER_TIMEOUT_SECONDS = 10L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");

    private final Path uploadDir;
    private final Tika tika;

    // Single managed, bounded executor with fixed daemon threads to prevent thread/resource exhaustion
    private final ExecutorService parserExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "resume-parser-worker-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    public ResumeParserService(@Value("${app.upload.dir:uploads/resumes}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.tika = new Tika();
        this.tika.setMaxStringLength(200_000);

        try {
            Files.createDirectories(this.uploadDir);
            logger.info("Resume upload storage directory initialized at: {}", this.uploadDir);
        } catch (Exception e) {
            logger.error("Could not create resume upload storage directory: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ResumeParserService worker executor...");
        parserExecutor.shutdownNow();
    }

    public FileStorageResult storeAndExtractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded resume file cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Resume file size exceeds maximum limit of 5MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Uploaded file must have a valid name.");
        }

        // Clean filename and extract extension
        String cleanOriginalName = Paths.get(originalFilename).getFileName().toString();
        String extension = getFileExtension(cleanOriginalName).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported resume file format '. " + extension + "'. Supported formats are: PDF, DOCX, TXT.");
        }

        // Generate safe unique filename to prevent path traversal and collisions
        String safeFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + "." + extension;
        Path targetPath = this.uploadDir.resolve(safeFileName).normalize();

        // Safety check to guarantee targetPath remains within uploadDir
        if (!targetPath.startsWith(this.uploadDir)) {
            throw new BadRequestException("Security error: Path traversal attack detected in filename.");
        }

        String extractedText;
        try {
            // Save file safely to disk
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 1. Magic-Byte MIME detection via Apache Tika
            String detectedMime = tika.detect(targetPath.toFile()).toLowerCase();
            logger.debug("Detected MIME type for file {}: {}", cleanOriginalName, detectedMime);

            // Rejection of executable/binary binaries
            if (detectedMime.contains("executable") || detectedMime.contains("dosexec")
                    || detectedMime.contains("x-msdownload") || detectedMime.contains("x-sharedlib")
                    || detectedMime.contains("x-object")) {
                deleteFile(targetPath.toString());
                throw new BadRequestException("Security rejection: Executable or binary file types are not permitted.");
            }

            // Mismatched file extension validation
            if (extension.equals("pdf") && !detectedMime.equals("application/pdf")) {
                deleteFile(targetPath.toString());
                throw new BadRequestException("Invalid PDF document content. File structure does not match PDF specification.");
            }

            if (extension.equals("docx") && (!detectedMime.contains("wordprocessingml")
                    && !detectedMime.contains("ooxml") && !detectedMime.equals("application/zip"))) {
                deleteFile(targetPath.toString());
                throw new BadRequestException("Invalid DOCX document content. File structure does not match DOCX specification.");
            }

            if (extension.equals("txt") && !detectedMime.startsWith("text/")) {
                deleteFile(targetPath.toString());
                throw new BadRequestException("Invalid text file content. File structure is not readable plain text.");
            }

            // 2. Bounded execution timeout using fixed-size executor
            Future<String> parseFuture = parserExecutor.submit(() -> {
                String parsed = tika.parseToString(targetPath.toFile());
                return parsed != null ? parsed.trim() : "";
            });

            try {
                extractedText = parseFuture.get(PARSER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                parseFuture.cancel(true);
                deleteFile(targetPath.toString());
                logger.warn("Resume parsing timed out after {}s for file {}", PARSER_TIMEOUT_SECONDS, cleanOriginalName);
                throw new BadRequestException("Document parsing timed out. Please ensure the document is not corrupted or excessively complex.");
            } catch (ExecutionException ee) {
                parseFuture.cancel(true);
                deleteFile(targetPath.toString());
                logger.error("Parser execution failure for {}: {}", cleanOriginalName, ee.getMessage());
                throw new BadRequestException("Failed to extract text from resume file. Please ensure the file is not corrupted or password-protected.");
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                deleteFile(targetPath.toString());
                throw new BadRequestException("Parsing process was interrupted.");
            }

        } catch (BadRequestException bre) {
            deleteFile(targetPath.toString());
            throw bre;
        } catch (Exception e) {
            deleteFile(targetPath.toString());
            logger.error("Failed to parse and extract text from resume file {}: {}", cleanOriginalName, e.getMessage(), e);
            throw new BadRequestException("Failed to extract text from resume file. Please ensure the file is not corrupted or password-protected.");
        }

        if (extractedText == null || extractedText.isBlank()) {
            deleteFile(targetPath.toString());
            throw new BadRequestException("Extracted text from resume is empty. Please upload a readable text PDF, DOCX, or TXT file.");
        }

        if (extractedText.length() > MAX_EXTRACTED_CHARS) {
            deleteFile(targetPath.toString());
            throw new BadRequestException("Extracted resume text exceeds maximum allowed limit of 100,000 characters.");
        }

        return new FileStorageResult(cleanOriginalName, targetPath.toString(), extension.toUpperCase(), file.getSize(), extractedText);
    }

    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            if (path.startsWith(this.uploadDir)) {
                Files.deleteIfExists(path);
                logger.info("Deleted resume file from disk: {}", path);
            }
        } catch (Exception e) {
            logger.warn("Could not delete resume file at path {}: {}", filePath, e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex == -1 || lastIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastIndex + 1);
    }

    public static class FileStorageResult {
        private final String originalFileName;
        private final String storedFilePath;
        private final String fileType;
        private final long fileSize;
        private final String extractedText;

        public FileStorageResult(String originalFileName, String storedFilePath, String fileType, long fileSize, String extractedText) {
            this.originalFileName = originalFileName;
            this.storedFilePath = storedFilePath;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.extractedText = extractedText;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public String getStoredFilePath() {
            return storedFilePath;
        }

        public String getFileType() {
            return fileType;
        }

        public long getFileSize() {
            return fileSize;
        }

        public String getExtractedText() {
            return extractedText;
        }
    }
}
