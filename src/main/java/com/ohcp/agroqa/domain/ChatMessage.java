package com.ohcp.agroqa.domain;

import java.time.Instant;
import java.util.List;

public record ChatMessage(
        String sessionId,
        String question,
        String answer,
        String intent,
        double confidence,
        List<KnowledgeSource> sources,
        Instant createdAt
) {
}
