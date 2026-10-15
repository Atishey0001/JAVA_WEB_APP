package com.aichatbot;

import com.aichatbot.dao.ConversationDAO;
import com.aichatbot.dao.FeedbackDAO;
import com.aichatbot.model.Conversation;
import com.aichatbot.model.Feedback;
import com.aichatbot.service.FeedbackService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackServiceTest {

    static class FakeFeedbackDAO extends FeedbackDAO {
        final List<Feedback> rows = new ArrayList<>();

        @Override
        public Feedback create(Feedback fb) {
            rows.add(fb);
            return fb;
        }
    }

    /** User 1 owns conversation 10 only. */
    static class FakeConversationDAO extends ConversationDAO {
        @Override
        public Conversation findByIdAndUser(int id, int userId) {
            return id == 10 && userId == 1 ? new Conversation() : null;
        }
    }

    private final FakeFeedbackDAO feedbackDAO = new FakeFeedbackDAO();
    private final FeedbackService service = new FeedbackService(feedbackDAO, new FakeConversationDAO());

    @Test
    void validFeedbackIsStored() {
        assertTrue(service.submit(1, "4", "  Helpful  ", "10").isEmpty());
        Feedback saved = feedbackDAO.rows.get(0);
        assertEquals(4, saved.getRating());
        assertEquals("Helpful", saved.getComment());
        assertEquals(10, saved.getConversationId());
    }

    @Test
    void conversationAndCommentAreOptional() {
        assertTrue(service.submit(1, "5", "", "").isEmpty());
        assertNull(feedbackDAO.rows.get(0).getConversationId());
        assertNull(feedbackDAO.rows.get(0).getComment());
    }

    @Test
    void ratingMustBeBetweenOneAndFive() {
        for (String bad : new String[]{"0", "6", "abc", "", null}) {
            assertTrue(service.submit(1, bad, "x", "").containsKey("rating"), "should reject: " + bad);
        }
        assertTrue(feedbackDAO.rows.isEmpty());
    }

    @Test
    void cannotAttachFeedbackToSomeoneElsesConversation() {
        Map<String, String> errors = service.submit(2, "3", "hi", "10");
        assertTrue(errors.containsKey("conversationId"));
        assertTrue(feedbackDAO.rows.isEmpty());
    }

    @Test
    void tooLongCommentIsRejected() {
        assertTrue(service.submit(1, "3", "x".repeat(1001), "").containsKey("comment"));
    }
}
