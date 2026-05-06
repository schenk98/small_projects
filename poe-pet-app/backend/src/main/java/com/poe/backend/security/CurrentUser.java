package com.poe.backend.security;

public final class CurrentUser {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    /**
     * Per-request user id storage.
     *
     * The {@link com.poe.backend.security.AuthInterceptor} sets this at the start of an authenticated request
     * and clears it in {@code afterCompletion}. Controller code can call {@link #get()} to obtain the current
     * user id without threading it through every method signature.
     */
    private CurrentUser() {
    }

    public static void set(String userId) {
        USER_ID.set(userId);
    }

    /** Return current request's user id (or null if unauthenticated). */
    public static String get() {
        return USER_ID.get();
    }

    /** Remove user id from thread-local storage. */
    public static void clear() {
        USER_ID.remove();
    }
}
