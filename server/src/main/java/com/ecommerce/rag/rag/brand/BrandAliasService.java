package com.ecommerce.rag.rag.brand;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.models.entity.Product;

@Service
public class BrandAliasService {

    private static final Logger log = LoggerFactory.getLogger(BrandAliasService.class);

    private static final Map<String, Set<String>> CANONICAL_TO_ALIASES = new LinkedHashMap<>();
    private static final Map<String, String> ALIAS_TO_CANONICAL = new LinkedHashMap<>();

    static {
        register("Apple", List.of(
                "苹果", "apple", "mac", "macbook", "ipad", "iphone", "airpods", "imac", "mac mini", "MacBook"));

        register("Nike", List.of("耐克", "nike", "NIKE"));
        register("Adidas", List.of("阿迪达斯", "阿迪", "adidas", "ADIDAS"));
        register("联想", List.of("lenovo", "Lenovo", "ThinkPad", "thinkpad"));
        register("小米", List.of("xiaomi", "Xiaomi", "mi", "Mi", "红米"));
        register("华为", List.of("huawei", "Huawei", "HUAWEI"));
        register("戴尔", List.of("dell", "Dell", "DELL"));
        register("惠普", List.of("hp", "HP", "Hp"));
        register("三星", List.of("samsung", "Samsung", "SAMSUNG"));
        register("索尼", List.of("sony", "Sony", "SONY"));
    }

    private static void register(String canonical, List<String> aliases) {
        Set<String> aliasSet = CANONICAL_TO_ALIASES.computeIfAbsent(canonical, k -> new LinkedHashSet<>());
        aliasSet.add(canonical);
        for (String alias : aliases) {
            aliasSet.add(alias);
            ALIAS_TO_CANONICAL.putIfAbsent(alias.toLowerCase(), canonical);
        }
        ALIAS_TO_CANONICAL.putIfAbsent(canonical.toLowerCase(), canonical);
    }

    public Set<String> expandBrandAliases(String brandOrAlias) {
        if (brandOrAlias == null || brandOrAlias.isBlank()) {
            return Set.of();
        }

        String key = brandOrAlias.toLowerCase();
        String canonical = ALIAS_TO_CANONICAL.get(key);
        if (canonical == null) {
            canonical = ALIAS_TO_CANONICAL.get(brandOrAlias);
        }
        if (canonical == null) {
            return Set.of(brandOrAlias);
        }
        Set<String> aliases = CANONICAL_TO_ALIASES.get(canonical);
        if (aliases == null) {
            return Set.of(brandOrAlias);
        }
        return aliases;
    }

    public String resolveCanonical(String brandOrAlias) {
        if (brandOrAlias == null || brandOrAlias.isBlank()) {
            return brandOrAlias;
        }
        String canonical = ALIAS_TO_CANONICAL.get(brandOrAlias.toLowerCase());
        return canonical != null ? canonical : brandOrAlias;
    }

    public boolean matchesBrandOrAlias(Product product, Collection<String> negativeBrands) {
        if (negativeBrands == null || negativeBrands.isEmpty()) {
            return false;
        }
        if (product == null) {
            return false;
        }

        for (String negBrand : negativeBrands) {
            if (negBrand == null || negBrand.isBlank()) {
                continue;
            }

            String negBrandLower = negBrand.toLowerCase();
            Set<String> negAliases = expandBrandAliases(negBrand);
            if (negAliases.size() <= 1 && negAliases.contains(negBrand)) {
                negAliases = Set.of(negBrand);
            }

            String productBrand = product.getBrand();
            if (productBrand != null && !productBrand.isBlank()) {
                String productBrandLower = productBrand.toLowerCase();
                for (String alias : negAliases) {
                    if (productBrandLower.equals(alias.toLowerCase())
                            || productBrandLower.contains(alias.toLowerCase())) {
                        return true;
                    }
                }
                String canonical = ALIAS_TO_CANONICAL.get(productBrandLower);
                if (canonical != null) {
                    if (negAliases.stream().anyMatch(a -> a.equalsIgnoreCase(canonical))) {
                        return true;
                    }
                }
            }

            if (matchesTextualFields(product, negBrandLower, negAliases)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesTextualFields(Product product, String negBrandLower, Set<String> negAliases) {
        StringBuilder sb = new StringBuilder();
        if (product.getName() != null) {
            sb.append(" ").append(product.getName().toLowerCase()).append(" ");
        }
        if (product.getDescription() != null) {
            sb.append(" ").append(product.getDescription().toLowerCase()).append(" ");
        }
        if (product.getSpecs() != null) {
            for (Map.Entry<String, String> e : product.getSpecs().entrySet()) {
                sb.append(" ").append(e.getKey().toLowerCase()).append(" ")
                        .append(e.getValue().toLowerCase()).append(" ");
            }
        }
        String fullText = sb.toString();

        for (String alias : negAliases) {
            String aliasLower = alias.toLowerCase();
            if (fullText.contains(" " + aliasLower + " ")
                    || fullText.contains(aliasLower + " ")
                    || fullText.contains(" " + aliasLower)) {
                return true;
            }
            if (fullText.contains(aliasLower) && aliasLower.length() >= 4) {
                return true;
            }
        }

        if (fullText.contains(negBrandLower)) {
            return true;
        }

        return false;
    }
}
