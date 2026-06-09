package com.ecommerce.rag.core.auth;

public class CurrentUserContext {

    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();

    public static void setUserId(String userId) {
        currentUserId.set(userId);
    }

    public static String getUserId() {
        return currentUserId.get();
    }

    public static void clear() {
        currentUserId.remove();
    }

    public static boolean isAuthenticated() {
        return currentUserId.get() != null;
    }
}
