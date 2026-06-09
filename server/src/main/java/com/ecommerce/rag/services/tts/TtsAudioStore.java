package com.ecommerce.rag.services.tts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class TtsAudioStore {

    private static final Logger log = LoggerFactory.getLogger(TtsAudioStore.class);
    private static final Pattern SAFE_FILE_NAME_PATTERN = Pattern.compile("^tts_[a-zA-Z0-9_-]+\\.mp3$");
    private static final long MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L; // 24 hours

    private final Path audioDir;

    public TtsAudioStore(com.ecommerce.rag.core.config.AppProperties appProperties) {
        String dirPath = appProperties.getTts().getAudioDir();
        this.audioDir = Paths.get(dirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.audioDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create TTS audio directory: " + this.audioDir, e);
        }
    }

    public StoredTtsAudio save(byte[] audioBytes, String extension) {
        cleanupOldFiles();

        String fileName = "tts_" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path filePath = audioDir.resolve(fileName);

        try {
            Files.write(filePath, audioBytes);
            log.info("Saved TTS audio: {}, size={} bytes", fileName, audioBytes.length);
            return new StoredTtsAudio(fileName, "audio/mpeg");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save TTS audio file", e);
        }
    }

    public Resource load(String fileName) {
        if (!isSafeFileName(fileName)) {
            log.warn("Illegal TTS audio file name requested: {}", fileName);
            return null;
        }
        Path filePath = audioDir.resolve(fileName).normalize();
        if (!filePath.startsWith(audioDir)) {
            log.warn("Path traversal attempt detected: {}", fileName);
            return null;
        }
        if (!Files.exists(filePath)) {
            return null;
        }
        return new FileSystemResource(filePath.toFile());
    }

    public boolean exists(String fileName) {
        if (!isSafeFileName(fileName)) {
            return false;
        }
        Path filePath = audioDir.resolve(fileName).normalize();
        if (!filePath.startsWith(audioDir)) {
            return false;
        }
        return Files.exists(filePath);
    }

    public void cleanupOldFiles() {
        long now = System.currentTimeMillis();
        try (Stream<Path> paths = Files.list(audioDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            return (now - Files.getLastModifiedTime(p).toMillis()) > MAX_AGE_MILLIS;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            log.debug("Cleaned up old TTS file: {}", p.getFileName());
                        } catch (IOException e) {
                            log.warn("Failed to delete old TTS file: {}", p.getFileName());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to list TTS audio directory for cleanup", e);
        }
    }

    boolean isSafeFileName(String fileName) {
        return fileName != null && SAFE_FILE_NAME_PATTERN.matcher(fileName).matches();
    }
}
