package com.ecommerce.rag.core.auth;

public class AuthContextSnapshot {

    private final String userId;
    private final boolean authenticated;

    public AuthContextSnapshot(String userId) {
        this.userId = userId;
        this.authenticated = userId != null;
    }

    public static AuthContextSnapshot fromCurrentThread() {
        return new AuthContextSnapshot(CurrentUserContext.getUserId());
    }

    public static AuthContextSnapshot unauthenticated() {
        return new AuthContextSnapshot(null);
    }

    public String getUserId() {
        return userId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
