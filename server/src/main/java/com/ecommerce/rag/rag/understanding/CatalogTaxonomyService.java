package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.services.ProductService;

import jakarta.annotation.PostConstruct;

@Service
public class CatalogTaxonomyService {

    private static final Logger log = LoggerFactory.getLogger(CatalogTaxonomyService.class);

    private final ProductService productService;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile CatalogTaxonomySnapshot snapshot;

    public CatalogTaxonomyService(ProductService productService) {
        this.productService = productService;
    }

    @PostConstruct
    public void init() {
        refresh();
        log.info("CatalogTaxonomyService initialized: {} categories, {} sub-categories, {} brands, price range [{}, {}]",
                snapshot.getCategories().size(), snapshot.getAllSubCategories().size(),
                snapshot.getBrands().size(), snapshot.getMinPrice(), snapshot.getMaxPrice());
    }

    public void refresh() {
        lock.writeLock().lock();
        try {
            this.snapshot = buildSnapshot();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public CatalogTaxonomySnapshot getSnapshot() {
        lock.readLock().lock();
        try {
            return snapshot;
        } finally {
            lock.readLock().unlock();
        }
    }

    private CatalogTaxonomySnapshot buildSnapshot() {
        List<Product> products = productService.listAll();

        CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
        snap.setFilterableFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
        snap.setTextFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));
        snap.setGeneratedAt(Instant.now());

        if (products.isEmpty()) {
            log.warn("Product catalog is empty, returning empty taxonomy snapshot");
            return snap;
        }

        TreeSet<String> catSet = new TreeSet<>();
        Map<String, TreeSet<String>> catToSub = new LinkedHashMap<>();
        TreeSet<String> allSubs = new TreeSet<>();
        TreeSet<String> brandSet = new TreeSet<>();
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        for (Product p : products) {
            if (p.getCategory() != null && !p.getCategory().isBlank()) {
                catSet.add(p.getCategory());
                catToSub.computeIfAbsent(p.getCategory(), k -> new TreeSet<>());
            }
            if (p.getSubCategory() != null && !p.getSubCategory().isBlank()) {
                allSubs.add(p.getSubCategory());
                if (p.getCategory() != null && !p.getCategory().isBlank()) {
                    catToSub.get(p.getCategory()).add(p.getSubCategory());
                }
            }
            if (p.getBrand() != null && !p.getBrand().isBlank()) {
                brandSet.add(p.getBrand());
            }
            if (p.getPrice() != null) {
                if (minPrice == null || p.getPrice().compareTo(minPrice) < 0) {
                    minPrice = p.getPrice();
                }
                if (maxPrice == null || p.getPrice().compareTo(maxPrice) > 0) {
                    maxPrice = p.getPrice();
                }
            }
        }

        snap.setCategories(new ArrayList<>(catSet));

        Map<String, List<String>> subByCat = new LinkedHashMap<>();
        for (Map.Entry<String, TreeSet<String>> e : catToSub.entrySet()) {
            subByCat.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        snap.setSubCategoriesByCategory(subByCat);
        snap.setAllSubCategories(new ArrayList<>(allSubs));
        snap.setBrands(new ArrayList<>(brandSet));
        snap.setMinPrice(minPrice);
        snap.setMaxPrice(maxPrice);

        return snap;
    }
}
