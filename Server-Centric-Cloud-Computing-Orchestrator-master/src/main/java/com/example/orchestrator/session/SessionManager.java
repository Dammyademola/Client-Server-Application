package com.example.orchestrator.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class SessionManager {
    private static final Map<String, Integer> userSessions = new HashMap<>();

    public static String createSession(int userId) {
        String sessionId = generateSessionId();
        userSessions.put(sessionId, userId);
        return sessionId;
    }

    public static Integer getUserIdFromSession(String sessionId) {
        return userSessions.get(sessionId);
    }

    public static void invalidateSession(String sessionId) {
        userSessions.remove(sessionId);
    }

    private static String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}
