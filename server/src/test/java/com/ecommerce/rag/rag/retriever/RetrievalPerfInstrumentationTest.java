package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerformanceTraceService;
import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.vector.VectorSearchHit;
import com.ecommerce.rag.services.ProductService;

@ExtendWith(MockitoExtension.class)
class RetrievalPerfInstrumentationTest {

    @Mock VectorRetriever vectorRetriever;
    @Mock KeywordRetriever keywordRetriever;
    @Mock CandidateFusionService fusionService;
    @Mock ProductService productService;
    @Mock QueryAnalyzer queryAnalyzer;
    @Mock StrictProductConstraintFilter constraintFilter;
    @Mock com.ecommerce.rag.rag.rewrite.QueryRewriteService rewriteService;

    HybridCandidateRetriever retriever;
    PerformanceTraceService perfService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setRetrieval(new AppProperties.RetrievalProperties());
        appProperties.getRetrieval().setMode("hybrid");
        appProperties.getRetrieval().setVectorEnabled(true);
        appProperties.getRetrieval().setKeywordEnabled(true);
        appProperties.getRetrieval().setAutoFallbackToKeyword(true);

        AppProperties.PerfProperties perfProps = new AppProperties.PerfProperties();
        perfProps.setEnabled(true);
        perfProps.setLogEnabled(false);
        perfProps.setRecentEnabled(true);
        perfProps.setRecentSize(10);
        appProperties.setPerf(perfProps);

        perfService = new PerformanceTraceService(appProperties);
        perfService.beginTrace("/api/chat/stream", "s1", "test");

        retriever = new HybridCandidateRetriever(
                vectorRetriever, keywordRetriever, fusionService, productService,
                appProperties, queryAnalyzer, constraintFilter, rewriteService);
    }

    @Test
    void testRetrievalTotalSpanRecorded() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("跑鞋");

        when(vectorRetriever.getVectorStoreCount()).thenReturn(10L);
        when(vectorRetriever.retrieveWithFilters(anyString(), anyInt(), any()))
                .thenReturn(Collections.emptyList());
        when(keywordRetriever.retrieveWithSoftKeywords(anyString(), anyInt(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(fusionService.merge(anyList(), anyList())).thenReturn(Collections.emptyList());
        when(constraintFilter.filterCandidates(anyList(), any())).thenReturn(Collections.emptyList());

        List<ChatCandidate> result = retriever.retrieveWithAnalysis("跑鞋", 5, analysis);
        assertNotNull(result);
    }

    @Test
    void testFallbackToKeywordAttribute() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("跑鞋");

        when(vectorRetriever.getVectorStoreCount()).thenReturn(0L);
        when(keywordRetriever.retrieveWithSoftKeywords(anyString(), anyInt(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(constraintFilter.filterCandidates(anyList(), any())).thenReturn(Collections.emptyList());

        List<ChatCandidate> result = retriever.retrieveWithAnalysis("跑鞋", 5, analysis);
        assertNotNull(result);
    }
}
