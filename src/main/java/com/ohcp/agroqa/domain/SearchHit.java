package com.ohcp.agroqa.domain;

import java.util.List;

public record SearchHit(
        PestKnowledge knowledge,
        double score,
        List<String> matchedTerms
) {
}
