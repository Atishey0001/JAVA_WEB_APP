package com.aichatbot;

import com.aichatbot.dao.ChatbotConfigDAO;
import com.aichatbot.dao.ConversationDAO;
import com.aichatbot.dao.FeedbackDAO;
import com.aichatbot.dao.KnowledgeBaseDAO;
import com.aichatbot.dao.MessageDAO;
import com.aichatbot.dao.UserDAO;
import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.ChatbotConfig;
import com.aichatbot.model.Conversation;
import com.aichatbot.model.Feedback;
import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.model.Message;
import com.aichatbot.model.User;
import com.aichatbot.service.ChatbotService;
import com.aichatbot.service.ChatbotServiceImpl;
import com.aichatbot.util.DBConnection;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real JDBC tests against MySQL. They are SKIPPED automatically when the database is not
 * reachable. Run database.sql first and set DB_URL / DB_USER / DB_PASSWORD if needed.
 * Every test creates its own uniquely named rows and removes them afterwards.
 */
class JdbcIntegrationTest {

    private final UserDAO userDAO = new UserDAO();
    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final KnowledgeBaseDAO knowledgeBaseDAO = new KnowledgeBaseDAO();

    @BeforeAll
    static void requireDatabase() {
        boolean available;
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            st.executeQuery("SELECT 1 FROM users LIMIT 1").close();
            available = true;
        } catch (Exception e) {
            available = false;
        }
        Assumptions.assumeTrue(available, "MySQL / ai_chatbot_db not available - skipping JDBC tests");
    }

    private User newUser() {
        User u = new User();
        u.setFullName("Test User");
        u.setEmail("junit-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash("hash");
        u.setRole(User.ROLE_USER);
        return userDAO.create(u);
    }

    @Test
    void userCrudAndDuplicateEmail() {
        User u = newUser();
        try {
            assertTrue(u.getId() > 0);
            assertEquals("Test User", userDAO.findByEmail(u.getEmail()).getFullName());
            assertTrue(userDAO.updateName(u.getId(), "Renamed"));
            assertEquals("Renamed", userDAO.findById(u.getId()).getFullName());

            User duplicate = new User();
            duplicate.setFullName("Dup");
            duplicate.setEmail(u.getEmail());
            duplicate.setPasswordHash("x");
            DatabaseException ex = assertThrows(DatabaseException.class, () -> userDAO.create(duplicate));
            assertTrue(ex.isDuplicateKey());
        } finally {
            assertTrue(userDAO.deleteUser(u.getId()));
        }
        assertNull(userDAO.findById(u.getId()));
    }

    @Test
    void conversationAndMessagesPersistInOneTransactionAndOwnershipIsEnforced() {
        User owner = newUser();
        User other = newUser();
        try {
            int convId = conversationDAO.create(owner.getId(), "Persistence test");
            messageDAO.saveExchange(convId, "Hello", "Hello! Welcome.", null);

            List<Message> messages = messageDAO.findByConversation(convId);
            assertEquals(2, messages.size());
            assertEquals("USER", messages.get(0).getSender());
            assertTrue(messages.get(0).isUnanswered());      // no matched entry => unanswered
            assertEquals("BOT", messages.get(1).getSender());

            assertNotNull(conversationDAO.findByIdAndUser(convId, owner.getId()));
            assertNull(conversationDAO.findByIdAndUser(convId, other.getId()));
            assertEquals(2, conversationDAO.findByUser(owner.getId()).get(0).getMessageCount());
        } finally {
            userDAO.deleteUser(owner.getId());   // ON DELETE CASCADE removes conversation + messages
            userDAO.deleteUser(other.getId());
        }
    }

    @Test
    void sendMessageThroughServiceStoresAnswerFromKnowledgeBase() throws Exception {
        User u = newUser();
        try {
            ChatbotService service = new ChatbotServiceImpl();
            int convId = service.sendMessage(u.getId(), null, "Hello");
            List<Message> messages = service.getMessages(convId, u.getId());
            assertEquals(2, messages.size());
            assertFalse(messages.get(1).getContent().isBlank());
        } finally {
            userDAO.deleteUser(u.getId());
        }
    }

    @Test
    void feedbackIsStoredAndAveraged() {
        User u = newUser();
        try {
            Feedback f = new Feedback();
            f.setUserId(u.getId());
            f.setRating(4);
            f.setComment("ok");
            feedbackDAO.create(f);
            assertTrue(f.getId() > 0);
            assertEquals(1, feedbackDAO.findByUser(u.getId()).size());
            assertTrue(feedbackDAO.averageRating() >= 1.0);
            assertTrue(feedbackDAO.ratingDistribution().get(4) >= 1);
        } finally {
            userDAO.deleteUser(u.getId());
        }
    }

    @Test
    void knowledgeBaseCrud() {
        KnowledgeBaseEntry e = new KnowledgeBaseEntry();
        e.setTitle("JUnit entry " + UUID.randomUUID());
        e.setKeywords("junitkeyword");
        e.setAnswer("answer");
        knowledgeBaseDAO.create(e);
        try {
            assertTrue(knowledgeBaseDAO.findAll("junitkeyword").stream().anyMatch(x -> x.getId() == e.getId()));
            e.setAnswer("changed");
            assertTrue(knowledgeBaseDAO.update(e));
            assertEquals("changed", knowledgeBaseDAO.findById(e.getId()).getAnswer());
        } finally {
            assertTrue(knowledgeBaseDAO.delete(e.getId()));
        }
        assertNull(knowledgeBaseDAO.findById(e.getId()));
    }

    @Test
    void configurationIsSavedAndReloaded() {
        ChatbotConfigDAO dao = new ChatbotConfigDAO();
        ChatbotConfig original = dao.get();
        try {
            ChatbotConfig changed = new ChatbotConfig();
            changed.setBotName("JUnit Bot");
            changed.setWelcomeMessage("Welcome from test");
            changed.setFallbackResponse("Fallback from test");
            dao.save(changed);
            assertEquals("JUnit Bot", dao.get().getBotName());
        } finally {
            dao.save(original);
        }
    }
}
