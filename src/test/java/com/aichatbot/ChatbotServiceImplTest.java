package com.aichatbot;

import com.aichatbot.exception.ChatbotException;
import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.service.ChatbotService;
import com.aichatbot.service.ChatbotServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Chatbot response tests - pure logic, no database needed. */
class ChatbotServiceImplTest {

    private static final String FALLBACK = "Sorry, I do not know that.";
    private final ChatbotService service = new ChatbotServiceImpl();

    private static KnowledgeBaseEntry entry(int id, String title, String keywords, String answer) {
        KnowledgeBaseEntry e = new KnowledgeBaseEntry();
        e.setId(id);
        e.setTitle(title);
        e.setKeywords(keywords);
        e.setAnswer(answer);
        return e;
    }

    private static List<KnowledgeBaseEntry> sampleKnowledge() {
        List<KnowledgeBaseEntry> list = new ArrayList<>();
        list.add(entry(1, "Greeting", "hello,hi,hey", "Hello! Welcome to AI Support."));
        list.add(entry(2, "Our Services", "services,service,provide", "We help with services."));
        list.add(entry(3, "Contact Support", "contact,contact support", "Submit your query here."));
        list.add(entry(4, "Refund Policy", "refund,money back", "Refunds within 7 days."));
        return list;
    }

    @Test
    void greetingIsMatched() {
        ChatbotService.Answer a = service.findAnswer("Hello!", sampleKnowledge(), FALLBACK);
        assertEquals(1, a.entryId());
        assertEquals("Hello! Welcome to AI Support.", a.text());
    }

    @Test
    void punctuationAndCaseAreIgnored() {
        assertEquals(2, service.findAnswer("WHAT SERVICES do you provide???", sampleKnowledge(), FALLBACK).entryId());
    }

    @Test
    void longerPhraseOutscoresSingleWord() {
        assertEquals(3, service.findAnswer("How can I contact support?", sampleKnowledge(), FALLBACK).entryId());
    }

    @Test
    void keywordMustMatchWholeWord() {
        // "hi" must not match inside "this" / "which"
        ChatbotService.Answer a = service.findAnswer("which thing is this", sampleKnowledge(), FALLBACK);
        assertNull(a.entryId());
        assertEquals(FALLBACK, a.text());
    }

    @Test
    void unknownQuestionUsesFallback() {
        ChatbotService.Answer a = service.findAnswer("Tell me about quantum physics", sampleKnowledge(), FALLBACK);
        assertNull(a.entryId());
        assertEquals(FALLBACK, a.text());
    }

    @Test
    void emptyKnowledgeBaseUsesFallback() {
        assertNull(service.findAnswer("hello", new ArrayList<>(), FALLBACK).entryId());
    }

    @Test
    void blankAndTooLongQuestionsAreRejectedBeforeAnyDatabaseAccess() {
        assertThrows(ChatbotException.class, () -> service.sendMessage(1, null, "   "));
        assertThrows(ChatbotException.class, () -> service.sendMessage(1, null, null));
        assertThrows(ChatbotException.class, () -> service.sendMessage(1, null, "x".repeat(501)));
    }
}
