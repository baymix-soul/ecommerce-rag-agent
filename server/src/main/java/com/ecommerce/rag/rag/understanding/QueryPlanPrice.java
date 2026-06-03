package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;

public class QueryPlanPrice {

    private BigDecimal min;
    private BigDecimal max;
    private String currency;
    private Boolean strict;

    public QueryPlanPrice() {
    }

    public BigDecimal getMin() { return min; }
    public void setMin(BigDecimal min) { this.min = min; }

    public BigDecimal getMax() { return max; }
    public void setMax(BigDecimal max) { this.max = max; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getStrict() { return strict; }
    public void setStrict(Boolean strict) { this.strict = strict; }
}
