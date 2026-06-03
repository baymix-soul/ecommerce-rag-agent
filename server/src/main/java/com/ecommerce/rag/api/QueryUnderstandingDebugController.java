package com.ecommerce.rag.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.rag.understanding.CatalogTaxonomyService;
import com.ecommerce.rag.rag.understanding.CatalogTaxonomySnapshot;
import com.ecommerce.rag.rag.understanding.QueryPlan;
import com.ecommerce.rag.rag.understanding.QueryPlanValidationResult;
import com.ecommerce.rag.rag.understanding.QueryPlanValidator;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingResult;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingService;

@RestController
@RequestMapping("/api/rag/understanding")
public class QueryUnderstandingDebugController {

    private final CatalogTaxonomyService taxonomyService;
    private final QueryPlanValidator validator;
    private final QueryUnderstandingService understandingService;

    public QueryUnderstandingDebugController(CatalogTaxonomyService taxonomyService,
                                              QueryPlanValidator validator,
                                              QueryUnderstandingService understandingService) {
        this.taxonomyService = taxonomyService;
        this.validator = validator;
        this.understandingService = understandingService;
    }

    @GetMapping("/taxonomy")
    public ResponseEntity<Map<String, Object>> getTaxonomy() {
        CatalogTaxonomySnapshot snapshot = taxonomyService.getSnapshot();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", snapshot.getCategories());
        result.put("sub_categories_by_category", snapshot.getSubCategoriesByCategory());
        result.put("all_sub_categories", snapshot.getAllSubCategories());
        result.put("brands", snapshot.getBrands());
        result.put("min_price", snapshot.getMinPrice());
        result.put("max_price", snapshot.getMaxPrice());
        result.put("filterable_fields", snapshot.getFilterableFields());
        result.put("text_fields", snapshot.getTextFields());
        result.put("generated_at", snapshot.getGeneratedAt() != null ? snapshot.getGeneratedAt().toString() : null);
        result.put("empty", snapshot.isEmpty());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-plan")
    public ResponseEntity<QueryPlanValidationResult> validatePlan(@RequestBody QueryPlan plan) {
        CatalogTaxonomySnapshot taxonomy = taxonomyService.getSnapshot();
        QueryPlanValidationResult validationResult = validator.validate(plan, taxonomy);
        return ResponseEntity.ok(validationResult);
    }

    @PostMapping("/plan")
    public ResponseEntity<QueryUnderstandingResult> plan(@RequestBody Map<String, Object> request) {
        String query = (String) request.getOrDefault("query", "");
        String sessionId = (String) request.getOrDefault("session_id", "default");
        PageContext pageContext = null;
        QueryUnderstandingResult result = understandingService.understand(query, sessionId, pageContext);
        return ResponseEntity.ok(result);
    }
}
