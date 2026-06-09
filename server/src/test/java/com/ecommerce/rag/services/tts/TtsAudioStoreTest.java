package com.ecommerce.rag.services.tts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ecommerce.rag.core.config.AppProperties;

class TtsAudioStoreTest {

    @TempDir
    Path tempDir;

    private TtsAudioStore store;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        AppProperties.TtsProperties ttsProps = new AppProperties.TtsProperties();
        ttsProps.setAudioDir(tempDir.toString());
        props.setTts(ttsProps);
        store = new TtsAudioStore(props);
    }

    @Test
    void save_shouldGenerateTtsFileName() {
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        StoredTtsAudio stored = store.save(data, "mp3");

        assertNotNull(stored);
        assertTrue(stored.getFileName().startsWith("tts_"));
        assertTrue(stored.getFileName().endsWith(".mp3"));
        assertEquals("audio/mpeg", stored.getContentType());
    }

    @Test
    void load_shouldReturnResource() throws IOException {
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        StoredTtsAudio stored = store.save(data, "mp3");

        var resource = store.load(stored.getFileName());
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void load_illegalFileName_shouldReturnNull() {
        assertNull(store.load("../evil.mp3"));
        assertNull(store.load("tts_../../etc/passwd.mp3"));
        assertNull(store.load("random.txt"));
        assertNull(store.load("tts_file.wav"));
    }

    @Test
    void load_fileNotExists_shouldReturnNull() {
        assertNull(store.load("tts_nonexistent123.mp3"));
    }

    @Test
    void exists_shouldWork() {
        byte[] data = new byte[]{0x01};
        StoredTtsAudio stored = store.save(data, "mp3");

        assertTrue(store.exists(stored.getFileName()));
        assertFalse(store.exists("tts_notfound.mp3"));
    }

    @Test
    void exists_illegalFileName_shouldReturnFalse() {
        assertFalse(store.exists("../evil.mp3"));
    }

    @Test
    void cleanup_shouldNotDeleteNewFiles() throws IOException, InterruptedException {
        byte[] data = new byte[]{0x01};
        StoredTtsAudio stored = store.save(data, "mp3");

        // Cleanup should not delete the just-created file
        store.cleanupOldFiles();

        assertTrue(Files.exists(tempDir.resolve(stored.getFileName())));
    }
}
