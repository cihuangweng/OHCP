package com.ohcp.agroqa.service;

import com.ohcp.agroqa.domain.ChatMessage;
import com.ohcp.agroqa.domain.QaResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatHistoryService {
    private final Map<String, List<ChatMessage>> messagesBySession = new ConcurrentHashMap<>();

    public void save(QaResponse response) {
        ChatMessage message = new ChatMessage(
                response.sessionId(),
                response.question(),
                response.answer(),
                response.intent(),
                response.confidence(),
                response.sources(),
                Instant.now());
        messagesBySession.computeIfAbsent(response.sessionId(), key -> new ArrayList<>()).add(message);
    }

    public List<ChatMessage> findBySession(String sessionId) {
        return messagesBySession.getOrDefault(sessionId, List.of()).stream()
                .sorted(Comparator.comparing(ChatMessage::createdAt))
                .toList();
    }

    public void clear(String sessionId) {
        messagesBySession.remove(sessionId);
    }
}
