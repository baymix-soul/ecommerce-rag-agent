package com.ecommerce.rag.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.core.auth.AuthContextSnapshot;
import com.ecommerce.rag.core.perf.PerformanceTraceService;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.services.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final PerformanceTraceService perfService;

    public ChatController(ChatService chatService, PerformanceTraceService perfService) {
        this.chatService = chatService;
        this.perfService = perfService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamChat(@RequestBody ChatRequest request) {
        AuthContextSnapshot authContext = AuthContextSnapshot.fromCurrentThread();
        log.info("Chat stream request: message={}, authenticated={}",
                request.getMessage(), authContext.isAuthenticated());
        SseEmitter emitter = chatService.chat(request, authContext);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
