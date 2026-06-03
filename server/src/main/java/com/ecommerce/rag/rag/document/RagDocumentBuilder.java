package com.ecommerce.rag.rag.document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.entity.Product;

@Component
public class RagDocumentBuilder {

    private static final Logger log = LoggerFactory.getLogger(RagDocumentBuilder.class);

    private static final int MAX_TEXT_LENGTH = 800;

    private final RagChunkIdGenerator chunkIdGenerator = new RagChunkIdGenerator();

    public List<RagChunkDocument> buildChunks(Product product) {
        List<RagChunkDocument> chunks = new ArrayList<>();

        chunks.add(buildProfileChunk(product, 0));
        chunks.add(buildDescriptionChunk(product, 0));
        buildSpecsChunk(product, 0).ifPresent(chunks::add);
        chunks.add(buildSearchSummaryChunk(product, 0));
        buildReviewSummaryChunk(product, 0).ifPresent(chunks::add);
        buildFaqChunk(product, 0).ifPresent(chunks::add);
        buildMarketingCopyChunk(product, 0).ifPresent(chunks::add);

        return chunks;
    }

    public List<RagChunkDocument> buildChunks(List<Product> products) {
        List<RagChunkDocument> allChunks = new ArrayList<>();
        for (Product product : products) {
            allChunks.addAll(buildChunks(product));
        }
        log.info("Built {} chunks from {} products", allChunks.size(), products.size());
        return allChunks;
    }

    private RagChunkDocument buildProfileChunk(Product product, int index) {
        RagChunkDocument chunk = createBaseChunk(product, ChunkType.PRODUCT_PROFILE, index);
        chunk.setSourceField("profile");

        String brand = notBlank(product.getBrand()) ? product.getBrand() : "未知";
        String category = notBlank(product.getCategory()) ? product.getCategory() : "未知";
        String subCategory = notBlank(product.getSubCategory()) ? product.getSubCategory() : "未知";
        String priceStr = product.getPrice() != null ? product.getPrice().toPlainString() : "未知";
        String currency = notBlank(product.getCurrency()) ? product.getCurrency() : "CNY";
        String ratingStr = product.getAvgRating() != null ? product.getAvgRating().toString() : "未知";

        String text = String.format(
                "商品名称：%s\n品牌：%s\n类目：%s\n子类目：%s\n价格：%s %s\n评分：%s\n商品定位：%s %s %s",
                product.getName(), brand, category, subCategory,
                priceStr, currency, ratingStr,
                category, subCategory, brand);

        chunk.setText(truncateText(text));
        return chunk;
    }

    private RagChunkDocument buildDescriptionChunk(Product product, int index) {
        RagChunkDocument chunk = createBaseChunk(product, ChunkType.DESCRIPTION, index);
        chunk.setSourceField("description");

        String desc = product.getDescription();
        if (!notBlank(desc)) {
            desc = "暂无描述";
        }

        String text = String.format("商品名称：%s\n商品描述：%s", product.getName(), desc);
        chunk.setText(truncateText(text));
        return chunk;
    }

    private Optional<RagChunkDocument> buildSpecsChunk(Product product, int index) {
        Map<String, String> specs = product.getSpecs();
        if (specs == null || specs.isEmpty()) {
            return Optional.empty();
        }

        RagChunkDocument chunk = createBaseChunk(product, ChunkType.SPECS, index);
        chunk.setSourceField("specs");

        StringBuilder specsText = new StringBuilder();
        specsText.append("商品名称：").append(product.getName()).append("\n规格参数：\n");

        boolean hasContent = false;
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!notBlank(key) || !notBlank(value)) {
                continue;
            }
            specsText.append(key).append("：").append(value).append("\n");
            hasContent = true;
        }

        if (!hasContent) {
            return Optional.empty();
        }

        chunk.setText(truncateText(specsText.toString().trim()));
        return Optional.of(chunk);
    }

    private RagChunkDocument buildSearchSummaryChunk(Product product, int index) {
        RagChunkDocument chunk = createBaseChunk(product, ChunkType.SEARCH_SUMMARY, index);
        chunk.setSourceField("search_summary");

        StringBuilder summary = new StringBuilder();
        summary.append(product.getName());

        if (notBlank(product.getBrand())) {
            summary.append(" ").append(product.getBrand());
        }
        if (notBlank(product.getCategory())) {
            summary.append(" ").append(product.getCategory());
        }
        if (notBlank(product.getSubCategory())) {
            summary.append(" ").append(product.getSubCategory());
        }

        if (notBlank(product.getDescription())) {
            String shortDesc = product.getDescription();
            if (shortDesc.length() > 150) {
                shortDesc = shortDesc.substring(0, 150);
            }
            summary.append(" ").append(shortDesc);
        }

        if (product.getSpecs() != null && !product.getSpecs().isEmpty()) {
            for (String value : product.getSpecs().values()) {
                if (notBlank(value)) {
                    summary.append(" ").append(value);
                }
            }
        }

        chunk.setText(truncateText(summary.toString().trim()));
        return chunk;
    }

    private Optional<RagChunkDocument> buildReviewSummaryChunk(Product product, int index) {
        String review = product.getReviewSummary();
        if (!notBlank(review)) {
            return Optional.empty();
        }

        RagChunkDocument chunk = createBaseChunk(product, ChunkType.REVIEW_SUMMARY, index);
        chunk.setSourceField("review_summary");

        String brand = notBlank(product.getBrand()) ? product.getBrand() : "未知";
        String category = notBlank(product.getCategory()) ? product.getCategory() : "未知";
        String subCategory = notBlank(product.getSubCategory()) ? product.getSubCategory() : "未知";
        String ratingStr = product.getAvgRating() != null ? product.getAvgRating().toString() : "未知";

        String text = String.format(
                "商品用户评价摘要：\n商品名称：%s\n品牌：%s\n类目：%s/%s\n平均评分：%s\n评价摘要：%s",
                product.getName(), brand, category, subCategory, ratingStr, review);

        chunk.setText(truncateText(text));
        return Optional.of(chunk);
    }

    private Optional<RagChunkDocument> buildFaqChunk(Product product, int index) {
        String faq = product.getFaqSummary();
        if (!notBlank(faq)) {
            return Optional.empty();
        }

        RagChunkDocument chunk = createBaseChunk(product, ChunkType.FAQ, index);
        chunk.setSourceField("faq_summary");

        String brand = notBlank(product.getBrand()) ? product.getBrand() : "未知";
        String category = notBlank(product.getCategory()) ? product.getCategory() : "未知";
        String subCategory = notBlank(product.getSubCategory()) ? product.getSubCategory() : "未知";

        String text = String.format(
                "商品常见问答：\n商品名称：%s\n品牌：%s\n类目：%s/%s\n问答信息：%s",
                product.getName(), brand, category, subCategory, faq);

        chunk.setText(truncateText(text));
        return Optional.of(chunk);
    }

    private Optional<RagChunkDocument> buildMarketingCopyChunk(Product product, int index) {
        String mc = product.getMarketingCopy();
        if (!notBlank(mc)) {
            return Optional.empty();
        }

        RagChunkDocument chunk = createBaseChunk(product, ChunkType.MARKETING_COPY, index);
        chunk.setSourceField("marketing_copy");

        String brand = notBlank(product.getBrand()) ? product.getBrand() : "未知";
        String category = notBlank(product.getCategory()) ? product.getCategory() : "未知";
        String subCategory = notBlank(product.getSubCategory()) ? product.getSubCategory() : "未知";

        String text = String.format(
                "商品卖点文案：\n商品名称：%s\n品牌：%s\n类目：%s/%s\n核心卖点：%s",
                product.getName(), brand, category, subCategory, mc);

        chunk.setText(truncateText(text));
        return Optional.of(chunk);
    }

    private RagChunkDocument createBaseChunk(Product product, ChunkType chunkType, int index) {
        RagChunkDocument chunk = new RagChunkDocument();
        String chunkId = chunkIdGenerator.buildChunkId(product.getProductId(), chunkType, index);
        chunk.setChunkId(chunkId);
        chunk.setVectorPointId(chunkIdGenerator.buildVectorPointId(chunkId));
        chunk.setChunkType(chunkType.name());
        chunk.setParentId(product.getProductId());
        chunk.setProductId(product.getProductId());
        chunk.setName(product.getName());
        chunk.setBrand(product.getBrand());
        chunk.setCategory(product.getCategory());
        chunk.setSubCategory(product.getSubCategory());
        chunk.setPrice(product.getPrice());
        chunk.setCurrency(product.getCurrency());
        chunk.setAvgRating(product.getAvgRating());
        chunk.setImageUrl(product.getImageUrl());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("product_id", emptyIfNull(product.getProductId()));
        metadata.put("category", emptyIfNull(product.getCategory()));
        metadata.put("sub_category", emptyIfNull(product.getSubCategory()));
        metadata.put("brand", emptyIfNull(product.getBrand()));
        if (product.getPrice() != null) {
            metadata.put("price", product.getPrice().toPlainString());
        }
        if (product.getCurrency() != null) {
            metadata.put("currency", product.getCurrency());
        }
        if (product.getAvgRating() != null) {
            metadata.put("avg_rating", product.getAvgRating().toString());
        }
        chunk.setMetadata(metadata);

        return chunk;
    }

    private String truncateText(String text) {
        if (text != null && text.length() > MAX_TEXT_LENGTH) {
            return text.substring(0, MAX_TEXT_LENGTH);
        }
        return text;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String emptyIfNull(String value) {
        return value != null ? value : "";
    }
}
