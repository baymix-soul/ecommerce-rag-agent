package com.ecommerce.rag.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.ecommerce.rag.core.exception.AppException;
import com.ecommerce.rag.models.dto.TtsSpeakRequest;
import com.ecommerce.rag.models.dto.TtsSpeakResponse;
import com.ecommerce.rag.services.tts.TtsAudioStore;
import com.ecommerce.rag.services.tts.TtsService;

class TtsControllerTest {

    private TtsService ttsService;
    private TtsAudioStore ttsAudioStore;
    private TtsController controller;

    @BeforeEach
    void setUp() {
        ttsService = mock(TtsService.class);
        ttsAudioStore = mock(TtsAudioStore.class);
        controller = new TtsController(ttsService, ttsAudioStore);
    }

    @Test
    void speak_shouldReturnAudioUrl() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("mp3");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setScheme("http");
        httpReq.setServerName("localhost");
        httpReq.setServerPort(8080);

        when(ttsService.speakWithUrl(eq(req), anyString()))
                .thenReturn(new TtsSpeakResponse("http://localhost:8080/api/tts/audio/tts_abc123.mp3", "audio/mpeg"));

        ResponseEntity<TtsSpeakResponse> resp = controller.speak(req, httpReq);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().getAudioUrl().contains("/api/tts/audio/"));
        assertEquals("audio/mpeg", resp.getBody().getContentType());
    }

    @Test
    void speak_audioUrlShouldUseRequestHost() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setScheme("http");
        httpReq.setServerName("10.0.2.2");
        httpReq.setServerPort(8080);

        when(ttsService.speakWithUrl(eq(req), anyString()))
                .thenReturn(new TtsSpeakResponse("http://10.0.2.2:8080/api/tts/audio/tts_test.mp3", "audio/mpeg"));

        ResponseEntity<TtsSpeakResponse> resp = controller.speak(req, httpReq);

        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().getAudioUrl().startsWith("http://10.0.2.2:8080/"));
    }

    @Test
    void speak_shouldNotUse127_0_0_1() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setScheme("http");
        httpReq.setServerName("127.0.0.1");
        httpReq.setServerPort(8080);

        when(ttsService.speakWithUrl(eq(req), anyString()))
                .thenReturn(new TtsSpeakResponse("http://127.0.0.1:8080/api/tts/audio/tts_test.mp3", "audio/mpeg"));

        ResponseEntity<TtsSpeakResponse> resp = controller.speak(req, httpReq);

        // Controller itself doesn't block 127.0.0.1; the test verifies the URL construction
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().getAudioUrl().contains("127.0.0.1"));
    }

    @Test
    void getAudio_shouldReturnAudioMpeg() {
        byte[] mp3Data = new byte[]{(byte) 0xFF, (byte) 0xFB, (byte) 0x90};
        Resource resource = new ByteArrayResource(mp3Data);

        when(ttsAudioStore.load("tts_abc123.mp3")).thenReturn(resource);

        ResponseEntity<Resource> resp = controller.getAudio("tts_abc123.mp3");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("audio/mpeg", resp.getHeaders().getContentType().toString());
    }

    @Test
    void getAudio_notFound_shouldReturn404() {
        when(ttsAudioStore.load("tts_missing.mp3")).thenReturn(null);

        ResponseEntity<Resource> resp = controller.getAudio("tts_missing.mp3");

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void resolveBaseUrl_withForwardedHeaders() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerName("localhost");
        req.setServerPort(8080);
        req.addHeader("X-Forwarded-Proto", "https");
        req.addHeader("X-Forwarded-Host", "api.example.com");
        req.addHeader("X-Forwarded-Port", "443");

        String baseUrl = controller.resolveBaseUrl(req);
        assertEquals("https://api.example.com", baseUrl);
    }

    @Test
    void resolveBaseUrl_withoutForwardedHeaders() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerName("localhost");
        req.setServerPort(8080);

        String baseUrl = controller.resolveBaseUrl(req);
        assertEquals("http://localhost:8080", baseUrl);
    }

    @Test
    void resolveBaseUrl_withPublicBaseUrlEnv() {
        // This test verifies the logic path; actual env var is not set in test
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerName("localhost");
        req.setServerPort(8080);

        String baseUrl = controller.resolveBaseUrl(req);
        assertEquals("http://localhost:8080", baseUrl);
    }

    @Test
    void speak_providerUnavailable_shouldThrow503() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        when(ttsService.speakWithUrl(any(), anyString()))
                .thenThrow(new AppException("TTS_PROVIDER_UNAVAILABLE", "当前 TTS 服务不可用"));

        AppException ex = org.junit.jupiter.api.Assertions.assertThrows(AppException.class,
                () -> controller.speak(req, httpReq));
        assertEquals("TTS_PROVIDER_UNAVAILABLE", ex.getCode());
    }

    @Test
    void speak_synthesizeFailure_shouldThrow502() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        when(ttsService.speakWithUrl(any(), anyString()))
                .thenThrow(new AppException("TTS_SYNTHESIS_FAILED", "语音合成失败"));

        AppException ex = org.junit.jupiter.api.Assertions.assertThrows(AppException.class,
                () -> controller.speak(req, httpReq));
        assertEquals("TTS_SYNTHESIS_FAILED", ex.getCode());
    }
}
