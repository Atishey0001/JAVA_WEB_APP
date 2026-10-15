package com.aichatbot.service;

import com.aichatbot.model.KnowledgeBaseEntry;

import java.util.List;
import java.util.Optional;

/** Polymorphism: interchangeable ways of picking an answer from the knowledge base. */
public interface ResponseStrategy {

    /** @param normalizedQuestion lower-case text without punctuation (see TextNormalizer) */
    Optional<KnowledgeBaseEntry> findMatch(String normalizedQuestion, List<KnowledgeBaseEntry> entries);

    String getName();
}
