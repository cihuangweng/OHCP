package com.ohcp.agroqa.domain;

public record KnowledgeSource(
        String id,
        String name,
        String crop,
        String category,
        double score,
        String excerpt
) {
}
