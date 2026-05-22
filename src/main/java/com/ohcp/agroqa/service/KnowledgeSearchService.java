package com.ohcp.agroqa.service;

import com.ohcp.agroqa.domain.PestKnowledge;
import com.ohcp.agroqa.domain.SearchHit;
import com.ohcp.agroqa.repository.PestKnowledgeRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class KnowledgeSearchService {
    private final PestKnowledgeRepository repository;
    private final QuestionAnalyzer analyzer;

    public KnowledgeSearchService(PestKnowledgeRepository repository, QuestionAnalyzer analyzer) {
        this.repository = repository;
        this.analyzer = analyzer;
    }

    public List<SearchHit> search(String question, int limit) {
        Set<String> terms = analyzer.extractTerms(question, repository.findAll());
        List<SearchHit> hits = new ArrayList<>();
        for (PestKnowledge item : repository.findAll()) {
            SearchHit hit = score(item, terms, analyzer.normalize(question));
            if (hit.score() > 0) {
                hits.add(hit);
            }
        }
        return hits.stream()
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(limit)
                .toList();
    }

    private SearchHit score(PestKnowledge item, Set<String> terms, String question) {
        double score = 0;
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        String searchable = item.searchableText().toLowerCase(Locale.ROOT);

        if (question.contains(item.name().toLowerCase(Locale.ROOT))) {
            score += 12;
            matched.add(item.name());
        }
        for (String alias : item.aliasList()) {
            if (question.contains(alias.toLowerCase(Locale.ROOT))) {
                score += 9;
                matched.add(alias);
            }
        }
        for (String crop : item.cropList()) {
            if (question.contains(crop.toLowerCase(Locale.ROOT))) {
                score += 5;
                matched.add(crop);
            }
        }
        for (String term : terms) {
            String normalizedTerm = term.toLowerCase(Locale.ROOT);
            if (normalizedTerm.length() < 2) {
                continue;
            }
            if (searchable.contains(normalizedTerm)) {
                score += normalizedTerm.length() >= 3 ? 1.5 : 0.6;
                matched.add(term);
            }
        }
        if (question.contains("防治") || question.contains("用药") || question.contains("怎么治")) {
            score += containsAny(item.chemicalControl(), terms) ? 2 : 0;
        }
        if (question.contains("症状") || question.contains("什么病") || question.contains("什么虫")) {
            score += containsAny(item.symptoms(), terms) ? 3 : 0;
        }
        return new SearchHit(item, Math.round(score * 10.0) / 10.0, List.copyOf(matched));
    }

    private static boolean containsAny(String text, Set<String> terms) {
        String normalized = text.toLowerCase(Locale.ROOT);
        return terms.stream().anyMatch(term -> term.length() >= 2 && normalized.contains(term.toLowerCase(Locale.ROOT)));
    }
}
