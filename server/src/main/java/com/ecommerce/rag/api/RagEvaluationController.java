package com.ecommerce.rag.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.rag.eval.RagEvalQuery;
import com.ecommerce.rag.rag.eval.RagEvalResult;
import com.ecommerce.rag.rag.eval.RagEvalSummary;
import com.ecommerce.rag.rag.eval.RagEvaluationService;

@RestController
@RequestMapping("/api/rag/eval")
public class RagEvaluationController {

    private final RagEvaluationService evalService;

    public RagEvaluationController(RagEvaluationService evalService) {
        this.evalService = evalService;
    }

    @GetMapping("/queries")
    public ResponseEntity<List<RagEvalQuery>> listQueries() {
        return ResponseEntity.ok(evalService.loadQueries());
    }

    @PostMapping("/run")
    public ResponseEntity<RagEvalSummary> runAll(@RequestBody EvalRunBody body) {
        int topK = body != null && body.topK > 0 ? Math.min(body.topK, 50) : 5;
        RagEvalSummary summary = evalService.evaluateAll(topK);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/run-one")
    public ResponseEntity<RagEvalResult> runOne(@RequestBody RagEvalQuery evalQuery,
                                                 @RequestParam(value = "top_k", defaultValue = "5") int topK) {
        if (evalQuery == null || evalQuery.getQuery() == null || evalQuery.getQuery().isBlank()) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("code", "INVALID_REQUEST");
            error.put("message", "query must not be blank");
            return ResponseEntity.badRequest().build();
        }
        topK = Math.min(Math.max(topK, 1), 50);
        RagEvalResult result = evalService.evaluateOne(evalQuery, topK);
        return ResponseEntity.ok(result);
    }

    public static class EvalRunBody {
        public int topK = 5;
    }
}
