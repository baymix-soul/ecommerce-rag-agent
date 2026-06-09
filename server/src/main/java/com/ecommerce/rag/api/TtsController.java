package com.ecommerce.rag.api;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.models.dto.TtsSpeakRequest;
import com.ecommerce.rag.models.dto.TtsSpeakResponse;
import com.ecommerce.rag.services.tts.TtsAudioStore;
import com.ecommerce.rag.services.tts.TtsService;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private static final Logger log = LoggerFactory.getLogger(TtsController.class);

    private final TtsService ttsService;
    private final TtsAudioStore ttsAudioStore;

    public TtsController(TtsService ttsService, TtsAudioStore ttsAudioStore) {
        this.ttsService = ttsService;
        this.ttsAudioStore = ttsAudioStore;
    }

    @PostMapping("/speak")
    public ResponseEntity<TtsSpeakResponse> speak(@RequestBody TtsSpeakRequest request,
                                                   HttpServletRequest httpRequest) {
        log.info("TTS speak request received");
        String baseUrl = resolveBaseUrl(httpRequest);
        TtsSpeakResponse response = ttsService.speakWithUrl(request, baseUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/audio/{fileName}")
    public ResponseEntity<Resource> getAudio(@PathVariable String fileName) {
        Resource resource = ttsAudioStore.load(fileName);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Cache-Control", "max-age=3600")
                .body(resource);
    }

    String resolveBaseUrl(HttpServletRequest request) {
        String publicBaseUrl = System.getenv("TTS_PUBLIC_BASE_URL");
        if (publicBaseUrl != null && !publicBaseUrl.isEmpty()) {
            return publicBaseUrl;
        }

        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isEmpty()) {
            scheme = request.getScheme();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isEmpty()) {
            host = request.getServerName();
        }

        int port = request.getServerPort();
        String forwardedPort = request.getHeader("X-Forwarded-Port");
        if (forwardedPort != null && !forwardedPort.isEmpty()) {
            try {
                port = Integer.parseInt(forwardedPort);
            } catch (NumberFormatException ignored) {
            }
        }

        boolean isDefaultPort = ("https".equalsIgnoreCase(scheme) && port == 443)
                || ("http".equalsIgnoreCase(scheme) && port == 80);

        if (isDefaultPort) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }
}
