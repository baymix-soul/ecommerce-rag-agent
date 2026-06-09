package com.ecommerce.rag.services.tts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.exception.AppException;

@Component
public class EdgeTtsCliProvider implements TtsProvider {

    private static final Logger log = LoggerFactory.getLogger(EdgeTtsCliProvider.class);

    private final AppProperties appProperties;

    public EdgeTtsCliProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public TtsAudioResult synthesize(String text, String voice, String format) {
        if (!available()) {
            throw new AppException("TTS_PROVIDER_UNAVAILABLE", "当前 TTS 服务不可用");
        }

        String tempFileName = "edge_tts_" + UUID.randomUUID().toString().replace("-", "") + ".mp3";
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File tempFile = new File(tempDir, tempFileName);

        int timeoutSeconds = appProperties.getTts().getCommandTimeoutSeconds();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "edge-tts",
                    "--voice", voice,
                    "--text", text,
                    "--write-media", tempFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            log.info("Starting edge-tts synthesis, textLength={}, voice={}, timeout={}s",
                    text.length(), voice, timeoutSeconds);

            Process process = pb.start();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (var is = process.getInputStream()) {
                is.transferTo(outputStream);
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("edge-tts command timed out after {} seconds", timeoutSeconds);
                throw new AppException("TTS_SYNTHESIS_FAILED", "语音合成超时，请稍后重试");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errOutput = outputStream.toString(java.nio.charset.StandardCharsets.UTF_8);
                log.warn("edge-tts failed with exitCode={}, output={}", exitCode,
                        errOutput.length() > 200 ? errOutput.substring(0, 200) + "..." : errOutput);
                throw new AppException("TTS_SYNTHESIS_FAILED", "语音合成失败，请稍后重试");
            }

            if (!tempFile.exists() || tempFile.length() == 0) {
                log.warn("edge-tts produced no output file");
                throw new AppException("TTS_SYNTHESIS_FAILED", "语音合成失败，请稍后重试");
            }

            byte[] audioBytes = Files.readAllBytes(tempFile.toPath());
            log.info("edge-tts synthesis succeeded, outputSize={} bytes", audioBytes.length);
            return new TtsAudioResult(audioBytes, "audio/mpeg", "mp3");

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("edge-tts synthesis unexpected error", e);
            throw new AppException("TTS_SYNTHESIS_FAILED", "语音合成失败，请稍后重试");
        } finally {
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public String providerName() {
        return "edge-tts-cli";
    }

    @Override
    public boolean available() {
        try {
            ProcessBuilder pb = new ProcessBuilder("edge-tts", "--help");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
