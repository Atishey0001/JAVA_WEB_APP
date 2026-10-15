package com.aichatbot.service;

import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.util.TextNormalizer;

import java.util.List;
import java.util.Optional;

/**
 * Exact keyword / phrase matching. Each entry stores comma separated keywords; a keyword matches when
 * it appears as whole word(s) in the question. A longer phrase scores more than a single word, and the
 * entry with the highest total score wins. This is plain string matching - NOT machine learning.
 */
public class KeywordMatchStrategy implements ResponseStrategy {

    @Override
    public Optional<KnowledgeBaseEntry> findMatch(String normalizedQuestion, List<KnowledgeBaseEntry> entries) {
        String padded = " " + normalizedQuestion + " "; // padding makes whole-word checks simple
        KnowledgeBaseEntry best = null;
        int bestScore = 0;
        for (KnowledgeBaseEntry entry : entries) {
            int score = 0;
            for (String rawKeyword : entry.getKeywords().split(",")) {
                String keyword = TextNormalizer.normalize(rawKeyword);
                if (!keyword.isEmpty() && padded.contains(" " + keyword + " ")) {
                    score += keyword.split(" ").length;
                }
            }
            if (score > bestScore) {
                best = entry;
                bestScore = score;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    public String getName() {
        return "Keyword match";
    }
}
