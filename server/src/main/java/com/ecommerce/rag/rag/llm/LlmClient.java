package com.ecommerce.rag.rag.llm;

import java.util.function.Consumer;

public interface LlmClient {

    void streamGenerate(String prompt, Consumer<String> onText, Runnable onComplete, Consumer<Throwable> onError);
}
