package com.aichatbot.service;

import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.util.TextNormalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lower-confidence second chance: counts how many meaningful words the question shares with an
 * entry's title and keywords. At least two shared words are required to avoid random matches.
 */
public class TokenOverlapStrategy implements ResponseStrategy {

    private static final int MIN_SHARED_WORDS = 2;

    @Override
    public Optional<KnowledgeBaseEntry> findMatch(String normalizedQuestion, List<KnowledgeBaseEntry> entries) {
        Set<String> questionWords = new HashSet<>(TextNormalizer.tokens(normalizedQuestion));
        KnowledgeBaseEntry best = null;
        int bestShared = 0;
        for (KnowledgeBaseEntry entry : entries) {
            String entryText = TextNormalizer.normalize(entry.getTitle() + " " + entry.getKeywords().replace(',', ' '));
            Set<String> entryWords = new HashSet<>(TextNormalizer.tokens(entryText));
            entryWords.retainAll(questionWords); // intersection
            if (entryWords.size() > bestShared) {
                best = entry;
                bestShared = entryWords.size();
            }
        }
        return bestShared >= MIN_SHARED_WORDS ? Optional.of(best) : Optional.empty();
    }

    @Override
    public String getName() {
        return "Word overlap";
    }
}
