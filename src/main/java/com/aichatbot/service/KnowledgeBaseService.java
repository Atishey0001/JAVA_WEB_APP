package com.aichatbot.service;

import com.aichatbot.dao.KnowledgeBaseDAO;
import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validation + CRUD rules for knowledge-base entries managed by administrators. */
public class KnowledgeBaseService {

    private final KnowledgeBaseDAO dao;

    public KnowledgeBaseService() {
        this(new KnowledgeBaseDAO());
    }

    public KnowledgeBaseService(KnowledgeBaseDAO dao) {
        this.dao = dao;
    }

    public Map<String, String> validate(KnowledgeBaseEntry entry) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (Validator.isBlank(entry.getTitle())) {
            errors.put("title", "Title is required.");
        } else if (entry.getTitle().trim().length() > 150) {
            errors.put("title", "Title must be at most 150 characters.");
        }
        if (Validator.isBlank(entry.getKeywords())) {
            errors.put("keywords", "Enter at least one keyword (separate several with commas).");
        } else if (entry.getKeywords().trim().length() > 500) {
            errors.put("keywords", "Keywords must be at most 500 characters.");
        }
        if (Validator.isBlank(entry.getAnswer())) {
            errors.put("answer", "Answer is required.");
        } else if (entry.getAnswer().trim().length() > 2000) {
            errors.put("answer", "Answer must be at most 2000 characters.");
        }
        if (Validator.isBlank(entry.getCategory())) {
            errors.put("category", "Category is required.");
        } else if (entry.getCategory().trim().length() > 50) {
            errors.put("category", "Category must be at most 50 characters.");
        }
        return errors;
    }

    /** Creates the entry when its id is 0, otherwise updates it. Returns validation/duplicate errors. */
    public Map<String, String> save(KnowledgeBaseEntry entry) {
        Map<String, String> errors = validate(entry);
        if (!errors.isEmpty()) {
            return errors;
        }
        entry.setTitle(entry.getTitle().trim());
        entry.setKeywords(entry.getKeywords().trim());
        entry.setAnswer(entry.getAnswer().trim());
        entry.setCategory(entry.getCategory().trim());
        try {
            if (entry.getId() == 0) {
                dao.create(entry);
            } else if (!dao.update(entry)) {
                errors.put("general", "This entry no longer exists.");
            }
        } catch (DatabaseException e) {
            if (e.isDuplicateKey()) {
                errors.put("title", "An entry with this title already exists.");
            } else {
                throw e;
            }
        }
        return errors;
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    public KnowledgeBaseEntry find(int id) {
        return dao.findById(id);
    }

    public List<KnowledgeBaseEntry> list(String search) {
        return dao.findAll(search);
    }
}
