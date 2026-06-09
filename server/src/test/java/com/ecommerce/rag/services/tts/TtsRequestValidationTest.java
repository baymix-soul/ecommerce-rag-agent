package com.ecommerce.rag.services.tts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.exception.AppException;
import com.ecommerce.rag.models.dto.TtsSpeakRequest;

class TtsRequestValidationTest {

    private AppProperties appProperties;
    private TtsService ttsService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        AppProperties.TtsProperties ttsProps = new AppProperties.TtsProperties();
        ttsProps.setEnabled(true);
        ttsProps.setMaxTextLength(500);
        ttsProps.setDefaultVoice("zh-CN-XiaoxiaoNeural");
        ttsProps.setAllowedVoices(List.of(
                "zh-CN-XiaoxiaoNeural",
                "zh-CN-YunxiNeural",
                "zh-CN-XiaoyiNeural",
                "zh-CN-YunjianNeural"
        ));
        ttsProps.setDefaultFormat("mp3");
        appProperties.setTts(ttsProps);

        TtsProvider fakeProvider = new FakeTtsProvider();
        TtsAudioStore fakeStore = mock(TtsAudioStore.class);
        when(fakeStore.save(new byte[]{0x01}, "mp3")).thenReturn(new StoredTtsAudio("tts_test.mp3", "audio/mpeg"));

        ttsService = new TtsService(appProperties, fakeProvider, fakeStore);
    }

    @Test
    void textNormal_shouldPass() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好，我是你的智能导购助手。");
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("mp3");

        var resp = ttsService.speak(req);
        assertEquals("audio/mpeg", resp.getContentType());
    }

    @Test
    void textEmpty_shouldThrow400() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("");
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("mp3");

        AppException ex = assertThrows(AppException.class, () -> ttsService.speak(req));
        assertEquals("INVALID_TTS_TEXT", ex.getCode());
    }

    @Test
    void textNull_shouldThrow400() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText(null);
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("mp3");

        AppException ex = assertThrows(AppException.class, () -> ttsService.speak(req));
        assertEquals("INVALID_TTS_TEXT", ex.getCode());
    }

    @Test
    void textTooLong_shouldTruncate() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("a".repeat(600));
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("mp3");

        var resp = ttsService.speak(req);
        assertEquals("audio/mpeg", resp.getContentType());
    }

    @Test
    void voiceEmpty_shouldUseDefault() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");
        req.setVoice("");
        req.setFormat("mp3");

        var resp = ttsService.speak(req);
        assertEquals("audio/mpeg", resp.getContentType());
    }

    @Test
    void voiceNull_shouldUseDefault() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");
        req.setVoice(null);
        req.setFormat("mp3");

        var resp = ttsService.speak(req);
        assertEquals("audio/mpeg", resp.getContentType());
    }

    @Test
    void voiceNotAllowed_shouldThrow400() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");
        req.setVoice("invalid-voice");
        req.setFormat("mp3");

        AppException ex = assertThrows(AppException.class, () -> ttsService.speak(req));
        assertEquals("INVALID_TTS_VOICE", ex.getCode());
    }

    @Test
    void formatMp3_shouldPass() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("mp3");

        var resp = ttsService.speak(req);
        assertEquals("audio/mpeg", resp.getContentType());
    }

    @Test
    void formatWav_shouldThrow400() {
        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("wav");

        AppException ex = assertThrows(AppException.class, () -> ttsService.speak(req));
        assertEquals("INVALID_TTS_FORMAT", ex.getCode());
    }

    @Test
    void ttsDisabled_shouldThrow503() {
        appProperties.getTts().setEnabled(false);

        TtsSpeakRequest req = new TtsSpeakRequest();
        req.setText("你好");
        req.setVoice("zh-CN-XiaoxiaoNeural");
        req.setFormat("mp3");

        AppException ex = assertThrows(AppException.class, () -> ttsService.speak(req));
        assertEquals("TTS_PROVIDER_UNAVAILABLE", ex.getCode());
    }

    private static class FakeTtsProvider implements TtsProvider {
        @Override
        public TtsAudioResult synthesize(String text, String voice, String format) {
            return new TtsAudioResult(new byte[]{0x01}, "audio/mpeg", "mp3");
        }

        @Override
        public String providerName() {
            return "fake";
        }

        @Override
        public boolean available() {
            return true;
        }
    }
}
