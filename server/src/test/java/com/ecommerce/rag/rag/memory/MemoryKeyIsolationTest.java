package com.ecommerce.rag.rag.memory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class MemoryKeyIsolationTest {

    private InMemoryConversationMemoryService memoryService;

    @BeforeEach
    void setUp() {
        memoryService = new InMemoryConversationMemoryService();
    }

    @Test
    void testDifferentUsersSameSessionIdDoNotShareContext() {
        String sessionId = "default";

        ConversationState stateA = memoryService.getOrCreate("userA:" + sessionId);
        stateA.setCategory("服饰运动");
        stateA.setSubCategory("跑步鞋");
        memoryService.save("userA:" + sessionId, stateA);

        ConversationState stateB = memoryService.getOrCreate("userB:" + sessionId);

        assertNull(stateB.getCategory());
        assertNull(stateB.getSubCategory());
    }

    @Test
    void testSameUserDifferentSessionIdsDoNotShareContext() {
        String userId = "userA";

        ConversationState state1 = memoryService.getOrCreate(userId + ":session1");
        state1.setCategory("服饰运动");
        state1.setSubCategory("跑步鞋");
        memoryService.save(userId + ":session1", state1);

        ConversationState state2 = memoryService.getOrCreate(userId + ":session2");

        assertNull(state2.getCategory());
        assertNull(state2.getSubCategory());
    }

    @Test
    void testAnonymousSessionWorks() {
        String sessionId = "anon-session-123";

        ConversationState state = memoryService.getOrCreate("anon:" + sessionId);
        state.setCategory("数码电器");
        state.setSubCategory("手机");
        memoryService.save("anon:" + sessionId, state);

        ConversationState retrieved = memoryService.getState("anon:" + sessionId);

        assertEquals("数码电器", retrieved.getCategory());
        assertEquals("手机", retrieved.getSubCategory());
    }

    @Test
    void testBuildMemoryKey() {
        assertEquals("userA:session1", memoryService.buildMemoryKey("session1", "userA"));
        assertEquals("anon:session1", memoryService.buildMemoryKey("session1", null));
        assertEquals("anon:default", memoryService.buildMemoryKey(null, null));
        assertEquals("anon:default", memoryService.buildMemoryKey("", ""));
    }

    @Test
    void testNoMatchDoesNotClearContext() {
        String key = "userA:default";

        ConversationState state = memoryService.getOrCreate(key);
        state.setCategory("服饰运动");
        state.setSubCategory("跑步鞋");
        state.setActiveSearchContext(new ActiveSearchContext(key));
        state.getActiveSearchContext().setCategory("服饰运动");
        state.getActiveSearchContext().setSubCategory("跑步鞋");
        memoryService.save(key, state);

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        memoryService.updateAfterNoMatch(key, analysis, false);

        ConversationState after = memoryService.getState(key);
        assertEquals("服饰运动", after.getCategory());
        assertEquals("跑步鞋", after.getSubCategory());
        assertNotNull(after.getActiveSearchContext());
        assertEquals("NO_MATCH", after.getLastNoMatchReason());
        assertFalse(after.isLastNoMatchRecovered());
    }

    @Test
    void testNoMatchRecoveredUpdatesState() {
        String key = "userA:default";

        ConversationState state = memoryService.getOrCreate(key);
        state.setActiveSearchContext(new ActiveSearchContext(key));
        memoryService.save(key, state);

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        memoryService.updateAfterNoMatch(key, analysis, true);

        ConversationState after = memoryService.getState(key);
        assertTrue(after.isLastNoMatchRecovered());
        assertEquals(ConversationContextMerger.ACTION_RELAX_CONSTRAINT,
                after.getActiveSearchContext().getLastContextAction());
    }
}
