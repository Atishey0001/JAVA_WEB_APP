package com.aichatbot.dao;

import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeBaseDAO {

    private static final String COLUMNS = "id, title, keywords, answer, category, is_active, created_at, updated_at";

    public KnowledgeBaseEntry create(KnowledgeBaseEntry e) {
        String sql = "INSERT INTO knowledge_base (title, keywords, answer, category, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getKeywords());
            ps.setString(3, e.getAnswer());
            ps.setString(4, e.getCategory());
            ps.setBoolean(5, e.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setId(keys.getInt(1));
                }
            }
            return e;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not create knowledge-base entry", ex);
        }
    }

    public boolean update(KnowledgeBaseEntry e) {
        String sql = "UPDATE knowledge_base SET title = ?, keywords = ?, answer = ?, category = ?, is_active = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getKeywords());
            ps.setString(3, e.getAnswer());
            ps.setString(4, e.getCategory());
            ps.setBoolean(5, e.isActive());
            ps.setInt(6, e.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not update knowledge-base entry", ex);
        }
    }

    public boolean delete(int id) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM knowledge_base WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not delete knowledge-base entry", ex);
        }
    }

    public KnowledgeBaseEntry findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM knowledge_base WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load knowledge-base entry", ex);
        }
    }

    /** Admin list with optional search over title, keywords, answer and category. */
    public List<KnowledgeBaseEntry> findAll(String search) {
        boolean hasSearch = search != null && !search.isBlank();
        String sql = "SELECT " + COLUMNS + " FROM knowledge_base "
                + (hasSearch ? "WHERE title LIKE ? OR keywords LIKE ? OR answer LIKE ? OR category LIKE ? " : "")
                + "ORDER BY category, title";
        List<KnowledgeBaseEntry> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (hasSearch) {
                String like = "%" + search.trim() + "%";
                for (int i = 1; i <= 4; i++) {
                    ps.setString(i, like);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not list knowledge-base entries", ex);
        }
    }

    /** Only active entries are used by the chatbot. */
    public List<KnowledgeBaseEntry> findActive() {
        String sql = "SELECT " + COLUMNS + " FROM knowledge_base WHERE is_active = 1 ORDER BY id";
        List<KnowledgeBaseEntry> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load active knowledge-base entries", ex);
        }
    }

    private KnowledgeBaseEntry map(ResultSet rs) throws SQLException {
        KnowledgeBaseEntry e = new KnowledgeBaseEntry();
        e.setId(rs.getInt("id"));
        e.setTitle(rs.getString("title"));
        e.setKeywords(rs.getString("keywords"));
        e.setAnswer(rs.getString("answer"));
        e.setCategory(rs.getString("category"));
        e.setActive(rs.getBoolean("is_active"));
        e.setCreatedAt(rs.getTimestamp("created_at"));
        e.setUpdatedAt(rs.getTimestamp("updated_at"));
        return e;
    }
}
