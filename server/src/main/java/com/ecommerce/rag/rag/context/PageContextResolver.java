package com.ecommerce.rag.rag.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.models.dto.PageType;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.services.ProductService;

@Component
public class PageContextResolver {

    private static final Logger log = LoggerFactory.getLogger(PageContextResolver.class);

    private final ProductService productService;

    public PageContextResolver(ProductService productService) {
        this.productService = productService;
    }

    public PageContextResolution resolve(PageContext pageContext) {
        PageContextResolution result = new PageContextResolution();

        if (pageContext == null) {
            result.setPageType(PageType.UNKNOWN);
            result.getWarnings().add("page_context is null");
            return result;
        }

        PageType pageType = pageContext.getPageType();
        result.setPageType(pageType);

        result.setPageSearchQuery(pageContext.getSearchQuery());
        result.setSelectedFilters(new LinkedHashMap<>(pageContext.getSelectedFilters()));

        switch (pageType) {
            case PRODUCT_DETAIL -> resolveProductDetail(result, pageContext);
            case PRODUCT_LIST -> resolveProductList(result, pageContext);
            case CHAT -> resolveChat(result, pageContext);
            case UNKNOWN -> result.getWarnings().add("page_type is UNKNOWN");
        }

        resolveRecentlyViewed(result, pageContext);

        log.debug("PageContext resolved: pageType={}, hasCurrentProduct={}, visibleProducts={}, "
                        + "recentlyViewed={}, warnings={}",
                result.getPageType(), result.isHasValidCurrentProduct(),
                result.getVisibleProducts().size(), result.getRecentlyViewedProducts().size(),
                result.getWarnings());

        return result;
    }

    private void resolveProductDetail(PageContextResolution result, PageContext pageContext) {
        String currentProductId = pageContext.getCurrentProductId();

        if (currentProductId == null || currentProductId.isBlank()) {
            result.getWarnings().add("PRODUCT_DETAIL page but current_product_id is null");
            return;
        }

        Optional<Product> product = productService.findById(currentProductId);
        if (product.isPresent()) {
            result.setCurrentProduct(product.get());
        } else {
            result.getWarnings().add("current_product_id not found in product library: " + currentProductId);
        }
    }

    private void resolveProductList(PageContextResolution result, PageContext pageContext) {
        List<String> visibleIds = pageContext.getVisibleProductIds();
        if (visibleIds == null || visibleIds.isEmpty()) {
            result.getWarnings().add("PRODUCT_LIST page but visible_product_ids is empty");
            return;
        }

        List<Product> products = new ArrayList<>();
        for (String id : visibleIds) {
            Optional<Product> product = productService.findById(id);
            if (product.isPresent()) {
                products.add(product.get());
            } else {
                result.getWarnings().add("visible_product_id not found: " + id);
            }
        }
        result.setVisibleProducts(products);

        String searchQuery = pageContext.getSearchQuery();
        if (searchQuery == null || searchQuery.isBlank()) {
            result.getWarnings().add("PRODUCT_LIST page but search_query is empty");
        }
    }

    private void resolveChat(PageContextResolution result, PageContext pageContext) {
        result.getWarnings().add("CHAT page type, using default retrieval");
    }

    private void resolveRecentlyViewed(PageContextResolution result, PageContext pageContext) {
        List<String> recentlyViewedIds = pageContext.getRecentlyViewedProductIds();
        if (recentlyViewedIds == null || recentlyViewedIds.isEmpty()) {
            return;
        }

        List<Product> products = new ArrayList<>();
        for (String id : recentlyViewedIds) {
            Optional<Product> product = productService.findById(id);
            if (product.isPresent()) {
                products.add(product.get());
            }
        }
        result.setRecentlyViewedProducts(products);
    }
}
