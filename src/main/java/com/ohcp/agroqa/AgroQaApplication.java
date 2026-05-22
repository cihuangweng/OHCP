package com.ohcp.agroqa;

import com.ohcp.agroqa.api.AgroQaServer;
import com.ohcp.agroqa.repository.PestKnowledgeRepository;
import com.ohcp.agroqa.service.ChatHistoryService;
import com.ohcp.agroqa.service.PestQaService;

import java.io.IOException;

public final class AgroQaApplication {
    private AgroQaApplication() {
    }

    public static void main(String[] args) throws IOException {
        int port = readPort(args);
        PestKnowledgeRepository repository = PestKnowledgeRepository.fromClasspath("/knowledge/pest_knowledge.tsv");
        PestQaService qaService = new PestQaService(repository);
        ChatHistoryService historyService = new ChatHistoryService();
        AgroQaServer server = new AgroQaServer(port, qaService, historyService, repository);
        server.start();
    }

    private static int readPort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }
        String envPort = System.getenv("PORT");
        return envPort == null || envPort.isBlank() ? 8080 : Integer.parseInt(envPort);
    }
}
