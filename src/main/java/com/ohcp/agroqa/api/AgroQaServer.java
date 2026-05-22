package com.ohcp.agroqa.api;

import com.ohcp.agroqa.domain.ChatMessage;
import com.ohcp.agroqa.domain.PestKnowledge;
import com.ohcp.agroqa.domain.QaResponse;
import com.ohcp.agroqa.repository.PestKnowledgeRepository;
import com.ohcp.agroqa.service.ChatHistoryService;
import com.ohcp.agroqa.service.PestQaService;
import com.ohcp.agroqa.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class AgroQaServer {
    private final int port;
    private final PestQaService qaService;
    private final ChatHistoryService historyService;
    private final PestKnowledgeRepository repository;
    private HttpServer server;

    public AgroQaServer(int port, PestQaService qaService, ChatHistoryService historyService, PestKnowledgeRepository repository) {
        this.port = port;
        this.qaService = qaService;
        this.historyService = historyService;
        this.repository = repository;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/chat", new ChatHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/knowledge", new KnowledgeHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("农业病虫害智能问答助手已启动：http://localhost:" + port);
    }

    private final class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, JsonUtil.error("只支持 POST /api/chat"));
                return;
            }
            Map<String, String> body = JsonUtil.parseFlatObject(readBody(exchange));
            String sessionId = body.getOrDefault("sessionId", "");
            String question = body.getOrDefault("question", "");
            List<ChatMessage> history = historyService.findBySession(sessionId);
            QaResponse response = qaService.answer(sessionId, question, history);
            historyService.save(response);
            writeJson(exchange, 200, JsonUtil.toJson(response));
        }
    }

    private final class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String sessionId = query.getOrDefault("sessionId", "");
            if (sessionId.isBlank()) {
                writeJson(exchange, 400, JsonUtil.error("缺少 sessionId"));
                return;
            }
            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                historyService.clear(sessionId);
                writeJson(exchange, 200, "{\"ok\":true}");
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, JsonUtil.error("只支持 GET 或 DELETE /api/history"));
                return;
            }
            writeJson(exchange, 200, JsonUtil.toJsonHistory(historyService.findBySession(sessionId)));
        }
    }

    private final class KnowledgeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, JsonUtil.error("只支持 GET /api/knowledge"));
                return;
            }
            List<PestKnowledge> knowledge = repository.findAll();
            writeJson(exchange, 200, JsonUtil.toJsonKnowledge(knowledge));
        }
    }

    private static final class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeText(exchange, 405, "Method Not Allowed", "text/plain; charset=utf-8");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            if (path.contains("..")) {
                writeText(exchange, 403, "Forbidden", "text/plain; charset=utf-8");
                return;
            }
            String resourcePath = "/static" + path;
            InputStream inputStream = AgroQaServer.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                writeText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
                return;
            }
            byte[] bytes;
            try (inputStream) {
                bytes = inputStream.readAllBytes();
            }
            String contentType = contentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> query = new HashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            query.put(key, value);
        }
        return query;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        writeText(exchange, statusCode, json, "application/json; charset=utf-8");
    }

    private static void writeText(HttpExchange exchange, int statusCode, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (path.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
