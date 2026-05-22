package com.ohcp.agroqa.service;

import com.ohcp.agroqa.domain.PestKnowledge;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class QuestionAnalyzer {
    private static final List<String> COMMON_TERMS = List.of(
            "水稻", "小麦", "玉米", "大豆", "棉花", "油菜", "甘薯", "白菜", "甘蓝", "番茄", "马铃薯",
            "黄瓜", "辣椒", "苹果", "梨", "桃", "柑橘", "病斑", "叶片", "叶背", "茎秆", "果实",
            "穗", "白穗", "卷叶", "发黄", "萎蔫", "腐烂", "霉层", "虫粪", "蛀食", "钻心",
            "蜜露", "煤污", "高湿", "多雨", "干旱", "防治", "用药", "药剂", "预防", "传播",
            "症状", "识别", "原因", "发生规律", "生物防治", "化学防治", "农业防治");

    public Set<String> extractTerms(String question, List<PestKnowledge> knowledgeItems) {
        String normalized = normalize(question);
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (PestKnowledge item : knowledgeItems) {
            addIfContains(normalized, item.name(), terms);
            item.aliasList().forEach(alias -> addIfContains(normalized, alias, terms));
            item.cropList().forEach(crop -> addIfContains(normalized, crop, terms));
        }
        COMMON_TERMS.forEach(term -> addIfContains(normalized, term, terms));

        for (String token : normalized.split("[\\s,，。？?！!；;、：:（）()]+")) {
            if (token.length() >= 2) {
                terms.add(token);
            }
        }
        addCharacterBigrams(normalized, terms);
        return terms;
    }

    public String detectIntent(String question) {
        String normalized = normalize(question);
        if (containsAny(normalized, "怎么治", "防治", "用药", "药剂", "打什么药", "怎么办", "治疗")) {
            return "防治咨询";
        }
        if (containsAny(normalized, "什么病", "什么虫", "识别", "判断", "叶片", "症状", "斑", "发黄", "萎蔫", "腐烂", "虫粪", "霉")) {
            return "症状识别";
        }
        if (containsAny(normalized, "为什么", "原因", "发生规律", "传播", "怎么传播", "条件")) {
            return "发生规律";
        }
        if (containsAny(normalized, "预防", "提前", "避免", "减少")) {
            return "预防建议";
        }
        return "综合问答";
    }

    public boolean asksForControl(String intent, String question) {
        String normalized = normalize(question);
        return "防治咨询".equals(intent)
                || containsAny(normalized, "防治", "用药", "药剂", "怎么办", "怎么治", "生物防治", "化学防治");
    }

    public boolean asksForSymptoms(String intent, String question) {
        String normalized = normalize(question);
        return "症状识别".equals(intent)
                || containsAny(normalized, "症状", "识别", "什么病", "什么虫", "表现", "病斑");
    }

    public boolean asksForOccurrence(String intent, String question) {
        String normalized = normalize(question);
        return "发生规律".equals(intent)
                || containsAny(normalized, "发生规律", "原因", "为什么", "传播", "条件");
    }

    public String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
    }

    private static void addIfContains(String normalizedQuestion, String term, Set<String> terms) {
        if (term != null && !term.isBlank() && normalizedQuestion.contains(term.toLowerCase(Locale.ROOT))) {
            terms.add(term);
        }
    }

    private static boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static void addCharacterBigrams(String normalized, Set<String> terms) {
        String chineseOnly = normalized.replaceAll("[^\\p{IsHan}]", "");
        for (int i = 0; i < chineseOnly.length() - 1; i++) {
            terms.add(chineseOnly.substring(i, i + 2));
        }
    }
}
