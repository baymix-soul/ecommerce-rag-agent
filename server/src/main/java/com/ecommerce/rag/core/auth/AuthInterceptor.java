package com.ecommerce.rag.core.auth;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ecommerce.rag.services.auth.AuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final Set<String> STRONG_AUTH_PREFIXES = Set.of("/api/cart");

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            boolean isStrongAuthPath = STRONG_AUTH_PREFIXES.stream().anyMatch(path::startsWith);

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String userId = authService.validateToken(token);
                if (userId != null) {
                    CurrentUserContext.setUserId(userId);
                    log.debug("Authenticated userId={} for path={}", userId, path);
                } else if (isStrongAuthPath) {
                    sendUnauthorized(response, "登录已过期，请重新登录");
                    return;
                }
            } else if (isStrongAuthPath) {
                sendUnauthorized(response, "请先登录");
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"INVALID_TOKEN\",\"message\":\"" + message + "\"}");
    }
}
