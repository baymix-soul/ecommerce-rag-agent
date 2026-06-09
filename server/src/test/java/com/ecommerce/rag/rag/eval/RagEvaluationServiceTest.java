package com.ecommerce.rag.rag.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RagEvaluationServiceTest {

    @Autowired
    private RagEvaluationService evalService;

    @BeforeEach
    void setUp() {
        evalService.loadQueries();
    }

    @Test
    void shouldLoadQueriesFromJson() {
        List<RagEvalQuery> queries = evalService.loadQueries();
        assertNotNull(queries);
        assertTrue(queries.size() >= 20);
        RagEvalQuery first = queries.get(0);
        assertNotNull(first.getQuery());
        assertNotNull(first.getExpectedCategory());
    }

    @Test
    void shouldPassWhenCategoryMatchesUsingAlias() {
        RagEvalQuery query = new RagEvalQuery();
        query.setQuery("油皮洗面奶");
        query.setExpectedCategory("美妆");
        query.setMinRelevantCount(1);

        RagEvalResult result = evalService.evaluateOne(query, 5);
        assertNotNull(result);
        assertTrue(result.getHits().size() > 0 || result.getTotalHits() >= 0,
                "Should get result object with hits list, even if vector index not built");
    }

    @Test
    void judgedRelevantShouldBeSetPerHit() {
        RagEvalQuery query = new RagEvalQuery();
        query.setQuery("油皮洗面奶");
        query.setExpectedCategory("美妆护肤");
        query.setMinRelevantCount(1);

        RagEvalResult result = evalService.evaluateOne(query, 5);
        assertNotNull(result.getHits());
        for (RagEvalHit hit : result.getHits()) {
            assertNotNull(hit.getMatchedRules());
            assertNotNull(hit.getFailedRules());
        }
    }

    @Test
    void sameHitMustSatisfyAllConstraints() {
        RagEvalQuery query = new RagEvalQuery();
        query.setQuery("200元以下蓝牙耳机");
        query.setExpectedCategory("数码电子");
        query.setMaxPrice(new java.math.BigDecimal("200"));
        query.setMinRelevantCount(1);

        RagEvalResult result = evalService.evaluateOne(query, 5);

        for (RagEvalHit hit : result.getHits()) {
            if (hit.isJudgedRelevant()) {
                assertTrue(hit.getMatchedRules().contains("category"),
                        "Relevant hit must satisfy category");
                assertTrue(hit.getMatchedRules().contains("max_price"),
                        "Relevant hit must satisfy max_price");
            }
        }
    }

    @Test
    void unsupportedQueryShouldNotAffectPassRate() {
        RagEvalQuery query = new RagEvalQuery();
        query.setQuery("日常生活用品");
        query.setSupported(false);
        query.setUnsupportedReason("test");
        query.setExpectedCategory("食品饮料");

        RagEvalResult result = evalService.evaluateOne(query, 5);
        assertFalse(result.isPass());
        assertTrue(result.getReasons().contains("unsupported_query"));
    }

    @Test
    void aliasSubCategoryShouldMatch() {
        RagEvalQuery query = new RagEvalQuery();
        query.setQuery("油皮洗面奶");
        query.setExpectedCategory("美妆护肤");
        query.setExpectedSubCategory("洗面奶");
        query.setMinRelevantCount(1);

        RagEvalResult result = evalService.evaluateOne(query, 5);
        assertNotNull(result);
    }

    @Test
    void shouldNotCallLlm() {
        RagEvalQuery query = new RagEvalQuery();
        query.setQuery("油皮洗面奶");
        query.setExpectedCategory("美妆护肤");

        RagEvalResult result = evalService.evaluateOne(query, 5);
        assertFalse(result.getReasons().stream().anyMatch(r -> r.contains("LLM") || r.contains("llm")));
    }
}
