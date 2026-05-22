package com.ohcp.agroqa.repository;

import com.ohcp.agroqa.domain.PestKnowledge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PestKnowledgeRepository {
    private final List<PestKnowledge> items;

    public PestKnowledgeRepository(List<PestKnowledge> items) {
        this.items = List.copyOf(items);
    }

    public static PestKnowledgeRepository fromClasspath(String resourcePath) throws IOException {
        InputStream inputStream = PestKnowledgeRepository.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Knowledge resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<PestKnowledge> knowledge = new ArrayList<>();
            String line = reader.readLine();
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length != 13) {
                    throw new IOException("Invalid knowledge row at line " + lineNo + ": expected 13 columns but got " + columns.length);
                }
                knowledge.add(new PestKnowledge(
                        columns[0], columns[1], columns[2], columns[3], columns[4], columns[5],
                        columns[6], columns[7], columns[8], columns[9], columns[10], columns[11], columns[12]));
            }
            return new PestKnowledgeRepository(knowledge);
        }
    }

    public List<PestKnowledge> findAll() {
        return items;
    }

    public Optional<PestKnowledge> findById(String id) {
        return items.stream()
                .filter(item -> Objects.equals(item.id(), id))
                .findFirst();
    }
}
