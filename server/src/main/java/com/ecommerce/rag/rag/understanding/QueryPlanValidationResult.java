package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

public class QueryPlanValidationResult {

    private QueryPlan originalPlan;
    private QueryPlan validatedPlan;
    private Boolean valid;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private List<String> fixedFields = new ArrayList<>();
    private Boolean categoryMatched;
    private Boolean subCategoryMatched;
    private Boolean priceValid;
    private Boolean brandValid;

    public QueryPlanValidationResult() {
    }

    public static QueryPlanValidationResult valid(QueryPlan originalPlan, QueryPlan validatedPlan) {
        QueryPlanValidationResult r = new QueryPlanValidationResult();
        r.originalPlan = originalPlan;
        r.validatedPlan = validatedPlan;
        r.valid = true;
        return r;
    }

    public static QueryPlanValidationResult invalid(QueryPlan originalPlan, QueryPlan validatedPlan,
                                                     List<String> warnings, List<String> errors) {
        QueryPlanValidationResult r = new QueryPlanValidationResult();
        r.originalPlan = originalPlan;
        r.validatedPlan = validatedPlan;
        r.valid = false;
        r.warnings = warnings != null ? warnings : new ArrayList<>();
        r.errors = errors != null ? errors : new ArrayList<>();
        return r;
    }

    public static QueryPlanValidationResult withWarnings(QueryPlan originalPlan, QueryPlan validatedPlan,
                                                          List<String> warnings, List<String> fixedFields) {
        QueryPlanValidationResult r = new QueryPlanValidationResult();
        r.originalPlan = originalPlan;
        r.validatedPlan = validatedPlan;
        r.valid = true;
        r.warnings = warnings != null ? warnings : new ArrayList<>();
        r.fixedFields = fixedFields != null ? fixedFields : new ArrayList<>();
        return r;
    }

    public QueryPlan getOriginalPlan() { return originalPlan; }
    public void setOriginalPlan(QueryPlan originalPlan) { this.originalPlan = originalPlan; }

    public QueryPlan getValidatedPlan() { return validatedPlan; }
    public void setValidatedPlan(QueryPlan validatedPlan) { this.validatedPlan = validatedPlan; }

    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors != null ? errors : new ArrayList<>(); }

    public List<String> getFixedFields() { return fixedFields; }
    public void setFixedFields(List<String> fixedFields) { this.fixedFields = fixedFields != null ? fixedFields : new ArrayList<>(); }

    public Boolean getCategoryMatched() { return categoryMatched; }
    public void setCategoryMatched(Boolean categoryMatched) { this.categoryMatched = categoryMatched; }

    public Boolean getSubCategoryMatched() { return subCategoryMatched; }
    public void setSubCategoryMatched(Boolean subCategoryMatched) { this.subCategoryMatched = subCategoryMatched; }

    public Boolean getPriceValid() { return priceValid; }
    public void setPriceValid(Boolean priceValid) { this.priceValid = priceValid; }

    public Boolean getBrandValid() { return brandValid; }
    public void setBrandValid(Boolean brandValid) { this.brandValid = brandValid; }
}
