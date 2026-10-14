package com.aichatbot.dao;

import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.Message;
import com.aichatbot.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageDAO {

    public List<Message> findByConversation(int conversationId) {
        String sql = "SELECT id, conversation_id, sender, content, matched_entry_id, unanswered, created_at "
                   + "FROM messages WHERE conversation_id = ? ORDER BY id";
        List<Message> messages = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt("id"));
                    m.setConversationId(rs.getInt("conversation_id"));
                    m.setSender(rs.getString("sender"));
                    m.setContent(rs.getString("content"));
                    int matched = rs.getInt("matched_entry_id");
                    m.setMatchedEntryId(rs.wasNull() ? null : matched);
                    m.setUnanswered(rs.getBoolean("unanswered"));
                    m.setCreatedAt(rs.getTimestamp("created_at"));
                    messages.add(m);
                }
            }
            return messages;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load messages", e);
        }
    }

    /**
     * TRANSACTION: saves the user's question and the bot's answer together and refreshes the
     * conversation's updated_at. Either all three statements succeed (commit) or none do (rollback).
     *
     * @param matchedEntryId knowledge-base entry that produced the answer, or null for the fallback reply
     */
    public void saveExchange(int conversationId, String question, String answer, Integer matchedEntryId) {
        String insertSql = "INSERT INTO messages (conversation_id, sender, content, matched_entry_id, unanswered) "
                         + "VALUES (?, ?, ?, ?, ?)";
        String touchSql = "UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            try {
                con.setAutoCommit(false);
                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, conversationId);
                    ps.setString(2, Message.SENDER_USER);
                    ps.setString(3, question);
                    ps.setNull(4, Types.INTEGER);
                    ps.setBoolean(5, matchedEntryId == null); // no match => this question was unanswered
                    ps.executeUpdate();

                    ps.setString(2, Message.SENDER_BOT);
                    ps.setString(3, answer);
                    if (matchedEntryId == null) {
                        ps.setNull(4, Types.INTEGER);
                    } else {
                        ps.setInt(4, matchedEntryId);
                    }
                    ps.setBoolean(5, false);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(touchSql)) {
                    ps.setInt(1, conversationId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the conversation messages", e);
        }
    }

    public int count() {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM messages");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not count messages", e);
        }
    }

    /** Most frequent questions the bot could not answer (case-insensitive grouping). */
    public List<Map<String, Object>> findTopUnanswered(int limit) {
        String sql = "SELECT LOWER(TRIM(content)) AS question, COUNT(*) AS times, MAX(created_at) AS last_asked "
                   + "FROM messages WHERE sender = 'USER' AND unanswered = 1 "
                   + "GROUP BY question ORDER BY times DESC, last_asked DESC LIMIT ?";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("question", rs.getString("question"));
                    row.put("times", rs.getInt("times"));
                    row.put("lastAsked", rs.getTimestamp("last_asked"));
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load unanswered questions", e);
        }
    }
}
