package com.ecommerce.rag.rag.retriever;

public class ConstraintFailure {

    public static final String CATEGORY_MISMATCH = "CATEGORY_MISMATCH";
    public static final String SUB_CATEGORY_MISMATCH = "SUB_CATEGORY_MISMATCH";
    public static final String PRICE_LT_MIN = "PRICE_LT_MIN";
    public static final String PRICE_GT_MAX = "PRICE_GT_MAX";
    public static final String NEGATIVE_BRAND = "NEGATIVE_BRAND";
    public static final String NEGATIVE_KEYWORD = "NEGATIVE_KEYWORD";
    public static final String EXCLUDED_PRODUCT = "EXCLUDED_PRODUCT";
    public static final String SOFT_PREFERENCE_MISSING = "SOFT_PREFERENCE_MISSING";

    private String type;
    private String field;
    private String expected;
    private String actual;
    private String message;

    public ConstraintFailure() {}

    public ConstraintFailure(String type, String field, String expected, String actual, String message) {
        this.type = type;
        this.field = field;
        this.expected = expected;
        this.actual = actual;
        this.message = message;
    }

    public static ConstraintFailure of(String type, String field, String expected, String actual) {
        String msg = type + ": expected=" + expected + ", actual=" + actual;
        return new ConstraintFailure(type, field, expected, actual, msg);
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
