package com.aichatbot.dao;

import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.Conversation;
import com.aichatbot.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {

    /** Joined query reused by the list/detail methods: owner name + message count. */
    private static final String SELECT_WITH_OWNER =
            "SELECT c.id, c.user_id, c.title, c.created_at, c.updated_at, u.full_name, u.email, "
          + "(SELECT COUNT(*) FROM messages m WHERE m.conversation_id = c.id) AS msg_count "
          + "FROM conversations c JOIN users u ON u.id = c.user_id ";

    public int create(int userId, String title) {
        String sql = "INSERT INTO conversations (user_id, title) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DatabaseException("No id returned for new conversation", null);
        } catch (SQLException e) {
            throw new DatabaseException("Could not create conversation", e);
        }
    }

    /** Ownership is enforced in SQL: returns null if the conversation belongs to someone else. */
    public Conversation findByIdAndUser(int id, int userId) {
        return queryOne(SELECT_WITH_OWNER + "WHERE c.id = ? AND c.user_id = ?", id, userId);
    }

    /** For admins (no ownership restriction). */
    public Conversation findById(int id) {
        return queryOne(SELECT_WITH_OWNER + "WHERE c.id = ?", id, null);
    }

    public List<Conversation> findByUser(int userId) {
        String sql = SELECT_WITH_OWNER + "WHERE c.user_id = ? ORDER BY c.updated_at DESC, c.id DESC";
        List<Conversation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load conversations", e);
        }
    }

    /** Admin search over title, user name/email and message text. Empty keyword = everything. */
    public List<Conversation> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findRecent(200);
        }
        String like = "%" + keyword.trim() + "%";
        String sql = SELECT_WITH_OWNER
                + "WHERE c.title LIKE ? OR u.full_name LIKE ? OR u.email LIKE ? "
                + "OR EXISTS (SELECT 1 FROM messages m WHERE m.conversation_id = c.id AND m.content LIKE ?) "
                + "ORDER BY c.updated_at DESC, c.id DESC LIMIT 200";
        List<Conversation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Could not search conversations", e);
        }
    }

    public List<Conversation> findRecent(int limit) {
        String sql = SELECT_WITH_OWNER + "ORDER BY c.updated_at DESC, c.id DESC LIMIT ?";
        List<Conversation> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load recent conversations", e);
        }
    }

    public int count() {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM conversations");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not count conversations", e);
        }
    }

    // ---- helpers ----
    private Conversation queryOne(String sql, int first, Integer second) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, first);
            if (second != null) {
                ps.setInt(2, second);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load conversation", e);
        }
    }

    private Conversation map(ResultSet rs) throws SQLException {
        Conversation c = new Conversation();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setTitle(rs.getString("title"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        c.setUserName(rs.getString("full_name"));
        c.setUserEmail(rs.getString("email"));
        c.setMessageCount(rs.getInt("msg_count"));
        return c;
    }
}
