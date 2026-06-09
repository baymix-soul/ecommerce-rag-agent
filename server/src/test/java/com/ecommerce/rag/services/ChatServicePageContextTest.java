package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.models.dto.PageType;

@SpringBootTest
class ChatServicePageContextTest {

    @Autowired
    private ChatService chatService;

    @Test
    void productDetailCurrentProductQAShouldNotError() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("这个适合敏感肌吗？");
        request.setSessionId("test-detail-qa");

        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_beauty_001");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void productDetailAlternativeShouldExcludeCurrent() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("有没有更便宜的");
        request.setSessionId("test-detail-alt");

        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_beauty_001");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void productListCheaperShouldUseSearchQuery() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("有没有更便宜的耳机？");
        request.setSessionId("test-list-cheaper");

        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.PRODUCT_LIST);
        pageContext.setSearchQuery("耳机");
        pageContext.setVisibleProductIds(java.util.List.of("p_digital_001", "p_digital_002", "p_digital_003"));
        pageContext.getSelectedFilters().put("category", "数码电子");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void oldChatRequestWithoutPageContextShouldStillWork() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setSessionId("test-old-compat");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void chatPageTypeShouldUseDefaultRetrieval() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐洗面奶");
        request.setSessionId("test-chat-page");

        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.CHAT);
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void unknownPageTypeShouldNotError() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("test-unknown-page");

        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.UNKNOWN);
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }
}
