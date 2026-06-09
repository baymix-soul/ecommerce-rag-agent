package com.ecommerce.rag.rag.understanding;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class QueryPlanJsonParser {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanJsonParser.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryPlanJsonParser() {
    }

    public Optional<QueryPlan> parse(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return Optional.empty();
        }

        String json = extractJson(rawOutput);
        if (json == null) {
            log.debug("QueryPlanJsonParser: no JSON found in output");
            return Optional.empty();
        }

        try {
            QueryPlan plan = objectMapper.readValue(json, QueryPlan.class);
            return Optional.of(plan);
        } catch (Exception e) {
            log.debug("QueryPlanJsonParser: failed to parse JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }

    String extractJson(String text) {
        if (text == null || text.isBlank()) return null;

        String trimmed = text.trim();

        int fenceStart = trimmed.indexOf("```json");
        if (fenceStart >= 0) {
            int contentStart = trimmed.indexOf('\n', fenceStart);
            if (contentStart >= 0) {
                int fenceEnd = trimmed.indexOf("```", contentStart);
                if (fenceEnd >= 0) {
                    return trimmed.substring(contentStart, fenceEnd).trim();
                }
                return trimmed.substring(contentStart).trim();
            }
        }

        int fenceStart2 = trimmed.indexOf("```");
        if (fenceStart2 >= 0) {
            int contentStart = trimmed.indexOf('\n', fenceStart2);
            if (contentStart >= 0) {
                int fenceEnd = trimmed.indexOf("```", contentStart);
                if (fenceEnd >= 0) {
                    return trimmed.substring(contentStart, fenceEnd).trim();
                }
            }
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return null;
    }
}
