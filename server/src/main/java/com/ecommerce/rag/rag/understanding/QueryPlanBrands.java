package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

public class QueryPlanBrands {

    private List<String> include = new ArrayList<>();
    private List<String> exclude = new ArrayList<>();

    public QueryPlanBrands() {
    }

    public List<String> getInclude() { return include; }
    public void setInclude(List<String> include) { this.include = include != null ? include : new ArrayList<>(); }

    public List<String> getExclude() { return exclude; }
    public void setExclude(List<String> exclude) { this.exclude = exclude != null ? exclude : new ArrayList<>(); }
}
