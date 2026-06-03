package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.List;

public class ConstraintCheckResult {

    private boolean passed;
    private final List<String> passedRules = new ArrayList<>();
    private final List<String> failedRules = new ArrayList<>();

    private ConstraintCheckResult() {}

    public static ConstraintCheckResult passed(String rule) {
        ConstraintCheckResult r = new ConstraintCheckResult();
        r.passed = true;
        r.passedRules.add(rule);
        return r;
    }

    public static ConstraintCheckResult failed(String rule) {
        ConstraintCheckResult r = new ConstraintCheckResult();
        r.passed = false;
        r.failedRules.add(rule);
        return r;
    }

    public static ConstraintCheckResult merge(ConstraintCheckResult a, ConstraintCheckResult b) {
        ConstraintCheckResult r = new ConstraintCheckResult();
        r.passed = a.passed && b.passed;
        r.passedRules.addAll(a.passedRules);
        r.passedRules.addAll(b.passedRules);
        r.failedRules.addAll(a.failedRules);
        r.failedRules.addAll(b.failedRules);
        return r;
    }

    public boolean isPassed() { return passed; }

    public List<String> getPassedRules() { return passedRules; }

    public List<String> getFailedRules() { return failedRules; }
}
