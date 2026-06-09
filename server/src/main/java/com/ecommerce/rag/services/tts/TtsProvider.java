package com.ecommerce.rag.services.tts;

public interface TtsProvider {

    TtsAudioResult synthesize(String text, String voice, String format);

    String providerName();

    boolean available();
}
