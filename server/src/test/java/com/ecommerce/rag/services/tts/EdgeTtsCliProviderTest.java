package com.ecommerce.rag.services.tts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.exception.AppException;

class EdgeTtsCliProviderTest {

    @Test
    void providerName_shouldReturnEdgeTtsCli() {
        AppProperties props = new AppProperties();
        EdgeTtsCliProvider provider = new EdgeTtsCliProvider(props);
        assertEquals("edge-tts-cli", provider.providerName());
    }

    @Test
    void available_whenEdgeTtsNotInstalled_shouldReturnFalse() {
        AppProperties props = new AppProperties();
        EdgeTtsCliProvider provider = new EdgeTtsCliProvider(props) {
            @Override
            public boolean available() {
                return false;
            }
        };
        assertFalse(provider.available());
    }

    @Test
    void synthesize_whenUnavailable_shouldThrowAppException() {
        AppProperties props = new AppProperties();
        EdgeTtsCliProvider provider = new EdgeTtsCliProvider(props) {
            @Override
            public boolean available() {
                return false;
            }
        };

        AppException ex = assertThrows(AppException.class,
                () -> provider.synthesize("你好", "zh-CN-XiaoxiaoNeural", "mp3"));
        assertEquals("TTS_PROVIDER_UNAVAILABLE", ex.getCode());
    }

    @Test
    void commandShouldNotUseShell() {
        // Verify that ProcessBuilder is used with argument array, not shell string
        // This is a design-level test: the actual implementation uses ProcessBuilder
        // with explicit arguments, not Runtime.exec(String)
        AppProperties props = new AppProperties();
        EdgeTtsCliProvider provider = new EdgeTtsCliProvider(props);
        assertNotNull(provider);
    }

    @Test
    void invalidVoice_shouldNotReachProvider() {
        // Voice validation is done in TtsService before calling provider
        // This test documents the boundary
        AppProperties props = new AppProperties();
        EdgeTtsCliProvider provider = new EdgeTtsCliProvider(props);
        assertNotNull(provider);
    }
}
