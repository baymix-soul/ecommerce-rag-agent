package com.ecommerce.rag.rag.rewrite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.config.AppProperties.RewriteProperties;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

@Service
public class QueryRewriteService {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriteService.class);

    private final AppProperties appProperties;
    private final SoftSemanticLexicon lexicon;
    private final LLMQueryRewriter llmRewriter;
    private final QueryRewriteValidator validator;

    private final Map<String, QueryRewriteResult> cache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, QueryRewriteResult> eldest) {
            return size() > 500;
        }
    };

    public QueryRewriteService(AppProperties appProperties,
                                SoftSemanticLexicon lexicon,
                                LLMQueryRewriter llmRewriter,
                                QueryRewriteValidator validator) {
        this.appProperties = appProperties;
        this.lexicon = lexicon;
        this.llmRewriter = llmRewriter;
        this.validator = validator;
    }

    public QueryRewriteResult rewrite(String query, QueryAnalysisResult analysis) {
        RewriteProperties rewrite = appProperties.getRewrite();

        if (rewrite == null || !rewrite.isEnabled()) {
            return QueryRewriteResult.none();
        }

        String provider = rewrite.getProvider();
        if (provider == null || provider.isBlank()) provider = "lexicon";

        String cacheKey = buildCacheKey(query, analysis);
        if (rewrite.isCacheEnabled()) {
            QueryRewriteResult cached = cache.get(cacheKey);
            if (cached != null) {
                log.debug("QueryRewriteService cache hit: {}", cacheKey);
                return cached;
            }
        }

        int maxVariants = rewrite.getMaxVariants();
        int maxKeywords = rewrite.getMaxSoftKeywords();

        QueryRewriteResult result = switch (provider.toLowerCase()) {
            case "lexicon" -> rewriteWithLexicon(query, maxKeywords);
            case "llm" -> rewriteWithLlm(query, maxVariants, maxKeywords);
            case "hybrid" -> rewriteHybrid(query, maxVariants, maxKeywords);
            default -> {
                log.warn("Unknown rewrite provider: {}, falling back to lexicon", provider);
                yield rewriteWithLexicon(query, maxKeywords);
            }
        };

        if (rewrite.isCacheEnabled()) {
            cache.put(cacheKey, result);
        }

        log.info("QueryRewriteService: source={}, confidence={}, keywords={}, variants={}, provider={}",
                result.getSource(), result.getConfidence(),
                result.getSoftKeywords().size(), result.getQueryVariants().size(), provider);

        return result;
    }

    private QueryRewriteResult rewriteWithLexicon(String query, int maxKeywords) {
        List<String> keywords = lexicon.lookup(query, maxKeywords);
        if (keywords.isEmpty()) {
            return QueryRewriteResult.none();
        }
        return QueryRewriteResult.fromLexicon(keywords, query);
    }

    private QueryRewriteResult rewriteWithLlm(String query, int maxVariants, int maxKeywords) {
        QueryRewriteResult raw = llmRewriter.rewrite(query);
        return validator.validate(raw, maxVariants, maxKeywords);
    }

    private QueryRewriteResult rewriteHybrid(String query, int maxVariants, int maxKeywords) {
        QueryRewriteResult lexiconResult = rewriteWithLexicon(query, maxKeywords);
        QueryRewriteResult llmResult = rewriteWithLlm(query, maxVariants, maxKeywords);
        return QueryRewriteResult.hybrid(lexiconResult, llmResult);
    }

    private String buildCacheKey(String query, QueryAnalysisResult analysis) {
        if (analysis == null) return query;
        String cat = analysis.getCategory() != null ? analysis.getCategory() : "";
        String sub = analysis.getSubCategory() != null ? analysis.getSubCategory() : "";
        return query + "|" + cat + "|" + sub;
    }
}
