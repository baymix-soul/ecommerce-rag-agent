package com.ecommerce.rag.services.tts;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.exception.AppException;
import com.ecommerce.rag.models.dto.TtsSpeakRequest;
import com.ecommerce.rag.models.dto.TtsSpeakResponse;

@Service
public class TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    private final AppProperties appProperties;
    private final TtsProvider ttsProvider;
    private final TtsAudioStore ttsAudioStore;

    public TtsService(AppProperties appProperties, TtsProvider ttsProvider, TtsAudioStore ttsAudioStore) {
        this.appProperties = appProperties;
        this.ttsProvider = ttsProvider;
        this.ttsAudioStore = ttsAudioStore;
    }

    public TtsSpeakResponse speak(TtsSpeakRequest request) {
        if (!appProperties.getTts().isEnabled()) {
            throw new AppException("TTS_PROVIDER_UNAVAILABLE", "当前 TTS 服务不可用");
        }

        String text = validateAndNormalizeText(request.getText());
        String voice = resolveVoice(request.getVoice());
        String format = resolveFormat(request.getFormat());

        log.info("TTS speak request, textLength={}, voice={}, format={}", text.length(), voice, format);

        TtsAudioResult result = ttsProvider.synthesize(text, voice, format);
        StoredTtsAudio stored = ttsAudioStore.save(result.getAudioBytes(), result.getFileExtension());

        TtsSpeakResponse response = new TtsSpeakResponse();
        response.setAudioUrl(stored.getFileName());
        response.setContentType(stored.getContentType());
        return response;
    }

    public TtsSpeakResponse speakWithUrl(TtsSpeakRequest request, String baseUrl) {
        TtsSpeakResponse response = speak(request);
        String audioUrl = buildAudioUrl(baseUrl, response.getAudioUrl());
        response.setAudioUrl(audioUrl);
        return response;
    }

    String validateAndNormalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new AppException("INVALID_TTS_TEXT", "文本不能为空");
        }
        String trimmed = text.trim();
        int maxLength = appProperties.getTts().getMaxTextLength();
        if (trimmed.length() > maxLength) {
            log.warn("TTS text too long ({} chars), truncating to {}", trimmed.length(), maxLength);
            trimmed = trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    String resolveVoice(String voice) {
        List<String> allowedVoices = appProperties.getTts().getAllowedVoices();
        String defaultVoice = appProperties.getTts().getDefaultVoice();

        if (voice == null || voice.trim().isEmpty()) {
            return defaultVoice;
        }

        if (!allowedVoices.contains(voice)) {
            log.warn("Unsupported voice '{}', allowed={}", voice, allowedVoices);
            throw new AppException("INVALID_TTS_VOICE", "不支持的语音角色");
        }

        return voice;
    }

    String resolveFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return appProperties.getTts().getDefaultFormat();
        }
        if (!"mp3".equalsIgnoreCase(format)) {
            throw new AppException("INVALID_TTS_FORMAT", "当前仅支持 mp3");
        }
        return "mp3";
    }

    String buildAudioUrl(String baseUrl, String fileName) {
        String normalizedBase = baseUrl;
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        return normalizedBase + "/api/tts/audio/" + fileName;
    }

    public StoredTtsAudio storeAudio(byte[] audioBytes, String extension) {
        return ttsAudioStore.save(audioBytes, extension);
    }
}
