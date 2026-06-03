package com.ecommerce.rag.rag.rewrite;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Component
public class SoftSemanticLexicon {

    private static final Logger log = LoggerFactory.getLogger(SoftSemanticLexicon.class);

    private static final String LEXICON_PATH = "rag/soft_semantic_lexicon.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<String>> lexicon = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(LEXICON_PATH)) {
            if (is == null) {
                log.warn("SoftSemanticLexicon not found at {}, using empty lexicon", LEXICON_PATH);
                return;
            }
            lexicon = objectMapper.readValue(is, new TypeReference<Map<String, List<String>>>() {});
            log.info("SoftSemanticLexicon loaded with {} entries", lexicon.size());
        } catch (Exception e) {
            log.error("Failed to load SoftSemanticLexicon from {}", LEXICON_PATH, e);
        }
    }

    public List<String> lookup(String query) {
        return lookup(query, Integer.MAX_VALUE);
    }

    public List<String> lookup(String query, int maxKeywords) {
        if (query == null || query.isBlank() || lexicon.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : lexicon.entrySet()) {
            if (query.contains(entry.getKey())) {
                for (String kw : entry.getValue()) {
                    if (!result.contains(kw) && result.size() < maxKeywords) {
                        result.add(kw);
                    }
                }
            }
        }

        if (!result.isEmpty()) {
            log.debug("SoftSemanticLexicon matched query='{}': {}", query, result);
        }
        return result;
    }

    public int size() {
        return lexicon.size();
    }
}
