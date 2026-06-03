package com.ecommerce.rag.rag.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryVectorStoreServiceTest {

    private InMemoryVectorStoreService store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVectorStoreService();
        store.clear();
    }

    @Test
    void shouldReturnZeroWhenEmpty() {
        assertEquals(0, store.count());
    }

    @Test
    void shouldCountAfterUpsert() {
        store.upsert(List.of(createChunk("vp_1", "p_001", List.of(1.0, 0.0))));

        assertEquals(1, store.count());
    }

    @Test
    void sameVectorPointIdShouldOverwrite() {
        EmbeddedRagChunk chunk1 = createChunk("vp_1", "p_001", List.of(1.0, 0.0));
        EmbeddedRagChunk chunk2 = createChunk("vp_1", "p_002", List.of(0.0, 1.0));

        store.upsert(List.of(chunk1));
        store.upsert(List.of(chunk2));

        assertEquals(1, store.count());
    }

    @Test
    void searchShouldReturnSortedByScore() {
        store.upsert(List.of(
                createChunk("vp_1", "p_001", List.of(1.0, 0.0)),
                createChunk("vp_2", "p_002", List.of(0.0, 1.0))
        ));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(10);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(2, hits.size());
        assertTrue(hits.get(0).getScore() >= hits.get(1).getScore());
        assertEquals("p_001", hits.get(0).getProductId());
    }

    @Test
    void searchShouldRespectLimit() {
        store.upsert(List.of(
                createChunk("vp_1", "p_001", List.of(1.0, 0.0)),
                createChunk("vp_2", "p_002", List.of(0.9, 0.1)),
                createChunk("vp_3", "p_003", List.of(0.0, 1.0))
        ));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(2);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(2, hits.size());
    }

    @Test
    void filterByCategoryShouldWork() {
        EmbeddedRagChunk chunkA = createChunk("vp_1", "p_001", List.of(1.0, 0.0));
        chunkA.getPayload().put("category", "美妆护肤");
        EmbeddedRagChunk chunkB = createChunk("vp_2", "p_002", List.of(0.9, 0.1));
        chunkB.getPayload().put("category", "数码电子");

        store.upsert(List.of(chunkA, chunkB));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(10);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("category", "美妆护肤");
        request.setFilters(filters);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(1, hits.size());
        assertEquals("p_001", hits.get(0).getProductId());
    }

    @Test
    void filterByProductIdShouldWork() {
        EmbeddedRagChunk chunkA = createChunk("vp_1", "p_aaa", List.of(1.0, 0.0));
        EmbeddedRagChunk chunkB = createChunk("vp_2", "p_bbb", List.of(0.9, 0.1));

        store.upsert(List.of(chunkA, chunkB));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(10);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("product_id", "p_bbb");
        request.setFilters(filters);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(1, hits.size());
        assertEquals("p_bbb", hits.get(0).getProductId());
    }

    @Test
    void filterByChunkTypeShouldWork() {
        EmbeddedRagChunk chunkA = createChunk("vp_1", "p_001", List.of(1.0, 0.0));
        chunkA.getPayload().put("chunk_type", "DESCRIPTION");
        chunkA.setChunkType("DESCRIPTION");
        EmbeddedRagChunk chunkB = createChunk("vp_2", "p_001", List.of(0.9, 0.1));
        chunkB.getPayload().put("chunk_type", "PRODUCT_PROFILE");
        chunkB.setChunkType("PRODUCT_PROFILE");

        store.upsert(List.of(chunkA, chunkB));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(10);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("chunk_type", "PRODUCT_PROFILE");
        request.setFilters(filters);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(1, hits.size());
        assertEquals("PRODUCT_PROFILE", hits.get(0).getChunkType());
    }

    @Test
    void filterByPriceShouldWork() {
        EmbeddedRagChunk chunkA = createChunk("vp_1", "p_001", List.of(1.0, 0.0));
        chunkA.getPayload().put("price", 89.0);
        EmbeddedRagChunk chunkB = createChunk("vp_2", "p_002", List.of(0.9, 0.1));
        chunkB.getPayload().put("price", 200.0);

        store.upsert(List.of(chunkA, chunkB));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(10);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("max_price", 100.0);
        request.setFilters(filters);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(1, hits.size());
        assertEquals("p_001", hits.get(0).getProductId());
    }

    @Test
    void clearShouldResetCount() {
        store.upsert(List.of(createChunk("vp_1", "p_001", List.of(1.0, 0.0))));
        assertEquals(1, store.count());

        store.clear();
        assertEquals(0, store.count());
    }

    @Test
    void searchWithNoVectorsShouldReturnEmpty() {
        EmbeddedRagChunk chunk = createChunk("vp_1", "p_001", null);
        store.upsert(List.of(chunk));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(10);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(0, hits.size());
    }

    @Test
    void eachHitShouldHaveRequiredFields() {
        store.upsert(List.of(createChunk("vp_1", "p_001", List.of(1.0, 0.0))));

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(new ArrayList<>(List.of(1.0, 0.0)));
        request.setLimit(10);

        List<VectorSearchHit> hits = store.search(request);

        assertEquals(1, hits.size());
        VectorSearchHit hit = hits.get(0);
        assertNotNull(hit.getChunkId());
        assertNotNull(hit.getVectorPointId());
        assertNotNull(hit.getProductId());
        assertNotNull(hit.getScore());
    }

    private EmbeddedRagChunk createChunk(String vectorPointId, String productId, List<Double> vector) {
        EmbeddedRagChunk chunk = new EmbeddedRagChunk();
        chunk.setChunkId("chk_" + productId);
        chunk.setVectorPointId(vectorPointId);
        chunk.setProductId(productId);
        chunk.setChunkType("DESCRIPTION");
        chunk.setText("测试文本");
        chunk.setVector(vector);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("product_id", productId);
        payload.put("chunk_type", "DESCRIPTION");
        payload.put("category", "美妆护肤");
        chunk.setPayload(payload);
        return chunk;
    }
}
