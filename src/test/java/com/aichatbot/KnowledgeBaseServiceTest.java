package com.aichatbot;

import com.aichatbot.dao.KnowledgeBaseDAO;
import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.service.KnowledgeBaseService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Knowledge-base add / edit / delete / validation using an in-memory fake DAO. */
class KnowledgeBaseServiceTest {

    static class FakeKnowledgeBaseDAO extends KnowledgeBaseDAO {
        final List<KnowledgeBaseEntry> rows = new ArrayList<>();
        int nextId = 1;

        @Override
        public KnowledgeBaseEntry create(KnowledgeBaseEntry e) {
            e.setId(nextId++);
            rows.add(e);
            return e;
        }

        @Override
        public boolean update(KnowledgeBaseEntry e) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).getId() == e.getId()) {
                    rows.set(i, e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean delete(int id) {
            return rows.removeIf(r -> r.getId() == id);
        }

        @Override
        public KnowledgeBaseEntry findById(int id) {
            return rows.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
        }
    }

    private final FakeKnowledgeBaseDAO dao = new FakeKnowledgeBaseDAO();
    private final KnowledgeBaseService service = new KnowledgeBaseService(dao);

    private static KnowledgeBaseEntry valid() {
        KnowledgeBaseEntry e = new KnowledgeBaseEntry();
        e.setTitle("  Shipping  ");
        e.setKeywords("shipping, delivery");
        e.setAnswer("We ship in 3 days.");
        e.setCategory("Orders");
        return e;
    }

    @Test
    void validEntryIsCreatedAndTrimmed() {
        assertTrue(service.save(valid()).isEmpty());
        assertEquals(1, dao.rows.size());
        assertEquals("Shipping", dao.rows.get(0).getTitle());
    }

    @Test
    void missingFieldsAreReported() {
        Map<String, String> errors = service.save(new KnowledgeBaseEntry() {{ setCategory(""); }});
        assertTrue(errors.containsKey("title"));
        assertTrue(errors.containsKey("keywords"));
        assertTrue(errors.containsKey("answer"));
        assertTrue(errors.containsKey("category"));
        assertTrue(dao.rows.isEmpty());
    }

    @Test
    void editChangesAnExistingEntry() {
        KnowledgeBaseEntry e = valid();
        service.save(e);
        KnowledgeBaseEntry edited = valid();
        edited.setId(e.getId());
        edited.setAnswer("We ship in 5 days.");
        assertTrue(service.save(edited).isEmpty());
        assertEquals("We ship in 5 days.", service.find(e.getId()).getAnswer());
    }

    @Test
    void editingAMissingEntryReportsAnError() {
        KnowledgeBaseEntry ghost = valid();
        ghost.setId(99);
        assertTrue(service.save(ghost).containsKey("general"));
    }

    @Test
    void deleteRemovesTheEntry() {
        KnowledgeBaseEntry e = valid();
        service.save(e);
        assertTrue(service.delete(e.getId()));
        assertNull(service.find(e.getId()));
        assertFalse(service.delete(e.getId()));
    }
}
