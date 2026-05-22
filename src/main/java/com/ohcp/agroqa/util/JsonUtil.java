package com.ohcp.agroqa.util;

import com.ohcp.agroqa.domain.ChatMessage;
import com.ohcp.agroqa.domain.KnowledgeSource;
import com.ohcp.agroqa.domain.PestKnowledge;
import com.ohcp.agroqa.domain.QaResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static Map<String, String> parseFlatObject(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        int index = skipWhitespace(json, 0);
        if (index >= json.length() || json.charAt(index) != '{') {
            return values;
        }
        index++;
        while (index < json.length()) {
            index = skipWhitespace(json, index);
            if (index < json.length() && json.charAt(index) == '}') {
                break;
            }
            ParsedString key = parseString(json, index);
            if (key == null) {
                break;
            }
            index = skipWhitespace(json, key.nextIndex());
            if (index >= json.length() || json.charAt(index) != ':') {
                break;
            }
            index = skipWhitespace(json, index + 1);
            ParsedString value = parseString(json, index);
            if (value == null) {
                break;
            }
            values.put(key.value(), value.value());
            index = skipWhitespace(json, value.nextIndex());
            if (index < json.length() && json.charAt(index) == ',') {
                index++;
            }
        }
        return values;
    }

    public static String toJson(QaResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendField(json, "sessionId", response.sessionId()).append(",");
        appendField(json, "question", response.question()).append(",");
        appendField(json, "answer", response.answer()).append(",");
        appendField(json, "intent", response.intent()).append(",");
        appendNumberField(json, "confidence", response.confidence()).append(",");
        json.append("\"sources\":").append(sources(response.sources())).append(",");
        json.append("\"followUpQuestions\":").append(strings(response.followUpQuestions()));
        json.append("}");
        return json.toString();
    }

    public static String toJsonHistory(List<ChatMessage> messages) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            json.append("{");
            appendField(json, "sessionId", message.sessionId()).append(",");
            appendField(json, "question", message.question()).append(",");
            appendField(json, "answer", message.answer()).append(",");
            appendField(json, "intent", message.intent()).append(",");
            appendNumberField(json, "confidence", message.confidence()).append(",");
            appendField(json, "createdAt", message.createdAt().toString()).append(",");
            json.append("\"sources\":").append(sources(message.sources()));
            json.append("}");
            if (i < messages.size() - 1) {
                json.append(",");
            }
        }
        return json.append("]").toString();
    }

    public static String toJsonKnowledge(List<PestKnowledge> knowledge) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < knowledge.size(); i++) {
            PestKnowledge item = knowledge.get(i);
            json.append("{");
            appendField(json, "id", item.id()).append(",");
            appendField(json, "name", item.name()).append(",");
            appendField(json, "aliases", item.aliases()).append(",");
            appendField(json, "category", item.category()).append(",");
            appendField(json, "crops", item.crops()).append(",");
            appendField(json, "symptoms", item.symptoms()).append(",");
            appendField(json, "prevention", item.prevention());
            json.append("}");
            if (i < knowledge.size() - 1) {
                json.append(",");
            }
        }
        return json.append("]").toString();
    }

    public static String error(String message) {
        return "{\"error\":" + quote(message) + "}";
    }

    private static String sources(List<KnowledgeSource> sources) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < sources.size(); i++) {
            KnowledgeSource source = sources.get(i);
            json.append("{");
            appendField(json, "id", source.id()).append(",");
            appendField(json, "name", source.name()).append(",");
            appendField(json, "crop", source.crop()).append(",");
            appendField(json, "category", source.category()).append(",");
            appendNumberField(json, "score", source.score()).append(",");
            appendField(json, "excerpt", source.excerpt());
            json.append("}");
            if (i < sources.size() - 1) {
                json.append(",");
            }
        }
        return json.append("]").toString();
    }

    private static String strings(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            json.append(quote(values.get(i)));
            if (i < values.size() - 1) {
                json.append(",");
            }
        }
        return json.append("]").toString();
    }

    private static StringBuilder appendField(StringBuilder json, String name, String value) {
        return json.append(quote(name)).append(":").append(quote(value));
    }

    private static StringBuilder appendNumberField(StringBuilder json, String name, double value) {
        return json.append(quote(name)).append(":").append(value);
    }

    private static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.append("\"").toString();
    }

    private static ParsedString parseString(String json, int start) {
        if (start >= json.length() || json.charAt(start) != '"') {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                return new ParsedString(value.toString(), i + 1);
            }
            if (c == '\\' && i + 1 < json.length()) {
                char escaped = json.charAt(++i);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            value.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                    }
                    default -> value.append(escaped);
                }
            } else {
                value.append(c);
            }
        }
        return null;
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private record ParsedString(String value, int nextIndex) {
    }
}
