package com.ohcp.agroqa.domain;

import java.util.List;

public record QaResponse(
        String sessionId,
        String question,
        String answer,
        String intent,
        double confidence,
        List<KnowledgeSource> sources,
        List<String> followUpQuestions
) {
}
