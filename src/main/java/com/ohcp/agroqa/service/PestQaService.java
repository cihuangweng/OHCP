package com.ohcp.agroqa.service;

import com.ohcp.agroqa.domain.ChatMessage;
import com.ohcp.agroqa.domain.KnowledgeSource;
import com.ohcp.agroqa.domain.PestKnowledge;
import com.ohcp.agroqa.domain.QaResponse;
import com.ohcp.agroqa.domain.SearchHit;
import com.ohcp.agroqa.repository.PestKnowledgeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PestQaService {
    private final PestKnowledgeRepository repository;
    private final QuestionAnalyzer analyzer;
    private final KnowledgeSearchService searchService;

    public PestQaService(PestKnowledgeRepository repository) {
        this.repository = repository;
        this.analyzer = new QuestionAnalyzer();
        this.searchService = new KnowledgeSearchService(repository, analyzer);
    }

    public QaResponse answer(String sessionId, String question, List<ChatMessage> history) {
        String safeSessionId = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isEmpty()) {
            return new QaResponse(safeSessionId, normalizedQuestion, "请先输入作物症状或防治问题，我会基于本地病虫害知识库进行检索回答。", "空问题", 0, List.of(), List.of());
        }

        String contextualQuestion = withContext(normalizedQuestion, history);
        String intent = analyzer.detectIntent(contextualQuestion);
        List<SearchHit> hits = searchService.search(contextualQuestion, 4);
        if (hits.isEmpty() || hits.getFirst().score() < 2.4) {
            return fallback(safeSessionId, normalizedQuestion, intent);
        }

        SearchHit best = hits.getFirst();
        List<KnowledgeSource> sources = hits.stream()
                .map(hit -> new KnowledgeSource(
                        hit.knowledge().id(),
                        hit.knowledge().name(),
                        hit.knowledge().crops(),
                        hit.knowledge().category(),
                        hit.score(),
                        hit.knowledge().compactExcerpt()))
                .toList();

        double confidence = confidence(best.score());
        String answer = buildAnswer(normalizedQuestion, intent, hits);
        return new QaResponse(safeSessionId, normalizedQuestion, answer, intent, confidence, sources, followUps(best.knowledge(), intent));
    }

    private QaResponse fallback(String sessionId, String question, String intent) {
        String answer = """
                我没有在当前知识库中检索到足够可靠的依据，因此不能直接下结论。

                你可以补充这些信息：作物名称、受害部位、病斑颜色和形状、是否有霉层或虫粪、近期天气、发生面积。拿到这些线索后，我会重新匹配可能的病虫害并给出防治建议。""";
        return new QaResponse(sessionId, question, answer, intent, 0.18, List.of(), List.of(
                "受害作物是什么？",
                "叶片、茎秆、根部还是果实异常？",
                "病斑颜色、形状和天气条件是什么？"));
    }

    private String withContext(String question, List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return question;
        }
        String normalized = analyzer.normalize(question);
        boolean dependsOnPrevious = normalized.length() <= 12
                || normalized.contains("这个")
                || normalized.contains("它")
                || normalized.contains("该病")
                || normalized.contains("怎么治")
                || normalized.contains("用什么药");
        if (!dependsOnPrevious) {
            return question;
        }
        ChatMessage last = history.getLast();
        if (last.sources().isEmpty()) {
            return question;
        }
        KnowledgeSource previous = last.sources().getFirst();
        return question + " " + previous.name() + " " + previous.crop();
    }

    private String buildAnswer(String question, String intent, List<SearchHit> hits) {
        PestKnowledge best = hits.getFirst().knowledge();
        StringBuilder answer = new StringBuilder();
        if ("症状识别".equals(intent)) {
            answer.append("根据你描述的症状，优先怀疑是").append(best.name()).append("。");
            if (hits.size() > 1 && hits.get(1).score() >= hits.getFirst().score() * 0.72) {
                answer.append("同时可对比排查").append(hits.get(1).knowledge().name()).append("。");
            }
            answer.append("\n\n判断依据：").append(best.symptoms());
            answer.append("\n\n进一步确认：").append(confirmingQuestions(best));
        } else {
            answer.append("关于“").append(question).append("”，知识库中最相关的条目是").append(best.name()).append("。");
        }

        if (analyzer.asksForSymptoms(intent, question) && !"症状识别".equals(intent)) {
            answer.append("\n\n主要症状：").append(best.symptoms());
        }
        if (analyzer.asksForOccurrence(intent, question) || "症状识别".equals(intent)) {
            answer.append("\n\n发生规律与传播：").append(best.occurrence()).append(best.transmission());
        }
        if (analyzer.asksForControl(intent, question) || "症状识别".equals(intent) || "综合问答".equals(intent)) {
            answer.append("\n\n防治方案：");
            answer.append("\n1. 农业防治：").append(best.agriculturalControl());
            answer.append("\n2. 化学防治：").append(best.chemicalControl());
            answer.append("\n3. 生物防治：").append(best.biologicalControl());
            answer.append("\n4. 预防建议：").append(best.prevention());
        }
        if (hits.size() > 1) {
            List<String> alternatives = hits.stream()
                    .skip(1)
                    .filter(hit -> hit.score() >= 3)
                    .limit(2)
                    .map(hit -> hit.knowledge().name())
                    .toList();
            if (!alternatives.isEmpty()) {
                answer.append("\n\n相近条目：").append(String.join("、", alternatives)).append("，可结合来源卡片继续对比。");
            }
        }
        answer.append("\n\n用药提示：药剂名称仅作知识库参考，实际使用要以当地登记作物、防治对象、标签剂量和安全间隔期为准。");
        return answer.toString();
    }

    private List<String> followUps(PestKnowledge knowledge, String intent) {
        List<String> questions = new ArrayList<>();
        if (!"防治咨询".equals(intent)) {
            questions.add(knowledge.name() + "怎么防治？");
        }
        questions.add("如何区分" + knowledge.name() + "和相似病虫害？");
        questions.add(knowledge.cropList().isEmpty() ? "近期天气会影响发生吗？" : knowledge.cropList().getFirst() + "怎样提前预防？");
        return questions;
    }

    private String confirmingQuestions(PestKnowledge knowledge) {
        return "请观察是否符合这些线索：危害作物为" + knowledge.crops()
                + "；病虫害特征是否包括“" + shortText(knowledge.symptoms(), 48)
                + "”；近期条件是否接近“" + shortText(knowledge.occurrence(), 44) + "”。";
    }

    private static String shortText(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "……";
    }

    private static double confidence(double score) {
        double value = Math.min(0.96, 0.35 + score / 35.0);
        return Math.round(value * 100.0) / 100.0;
    }

    public List<PestKnowledge> knowledgeItems() {
        return repository.findAll();
    }
}
