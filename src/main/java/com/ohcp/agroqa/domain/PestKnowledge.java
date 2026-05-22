package com.ohcp.agroqa.domain;

import java.util.Arrays;
import java.util.List;

public record PestKnowledge(
        String id,
        String name,
        String aliases,
        String taxonomy,
        String category,
        String crops,
        String symptoms,
        String occurrence,
        String transmission,
        String agriculturalControl,
        String chemicalControl,
        String biologicalControl,
        String prevention
) {
    public List<String> cropList() {
        return split(crops);
    }

    public List<String> aliasList() {
        return split(aliases);
    }

    public String searchableText() {
        return String.join(" ",
                name,
                aliases,
                taxonomy,
                category,
                crops,
                symptoms,
                occurrence,
                transmission,
                agriculturalControl,
                chemicalControl,
                biologicalControl,
                prevention);
    }

    public String compactExcerpt() {
        return "危害作物：" + crops + "；症状：" + symptoms + "；防治：" + agriculturalControl;
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[、,，/;；]"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
