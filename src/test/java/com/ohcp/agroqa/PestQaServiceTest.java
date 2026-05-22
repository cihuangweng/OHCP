package com.ohcp.agroqa;

import com.ohcp.agroqa.domain.QaResponse;
import com.ohcp.agroqa.repository.PestKnowledgeRepository;
import com.ohcp.agroqa.service.PestQaService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PestQaServiceTest {
    private static PestQaService service;

    @BeforeAll
    static void setUp() throws IOException {
        PestKnowledgeRepository repository = PestKnowledgeRepository.fromClasspath("/knowledge/pest_knowledge.tsv");
        service = new PestQaService(repository);
    }

    @Test
    void shouldIdentifyTypicalSymptoms() {
        assertAnswerContains(service, "test-1", "水稻叶片有梭形灰白病斑，穗颈发黑，可能是什么病？", "稻瘟病");
        assertAnswerContains(service, "test-2", "番茄叶背有白霉，茎秆黑褐腐烂，怎么防治？", "番茄晚疫病");
        assertAnswerContains(service, "test-3", "玉米心叶有排孔并且茎秆被钻蛀，应该怎么办？", "玉米螟");
    }

    private static void assertAnswerContains(PestQaService service, String sessionId, String question, String expected) {
        QaResponse response = service.answer(sessionId, question, java.util.List.of());
        assertTrue(response.answer().contains(expected), "Expected answer to contain " + expected + " but got: " + response.answer());
        assertFalse(response.sources().isEmpty(), "Expected sources for question: " + question);
    }
}
