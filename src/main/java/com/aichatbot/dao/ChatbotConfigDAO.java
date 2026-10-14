package com.aichatbot.dao;

import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.ChatbotConfig;
import com.aichatbot.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** The configuration lives in a single row (id = 1). */
public class ChatbotConfigDAO {

    /** Returns the stored configuration, or sensible defaults if the row is missing. */
    public ChatbotConfig get() {
        String sql = "SELECT id, bot_name, welcome_message, fallback_response, created_at, updated_at "
                   + "FROM chatbot_config WHERE id = 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ChatbotConfig cfg = new ChatbotConfig();
            if (rs.next()) {
                cfg.setId(rs.getInt("id"));
                cfg.setBotName(rs.getString("bot_name"));
                cfg.setWelcomeMessage(rs.getString("welcome_message"));
                cfg.setFallbackResponse(rs.getString("fallback_response"));
                cfg.setCreatedAt(rs.getTimestamp("created_at"));
                cfg.setUpdatedAt(rs.getTimestamp("updated_at"));
            } else {
                cfg.setId(1);
                cfg.setBotName("AI Support");
                cfg.setWelcomeMessage("Hello! How can I help you today?");
                cfg.setFallbackResponse("Sorry, I could not find an answer to that.");
            }
            return cfg;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load chatbot configuration", e);
        }
    }

    /** UPDATE the single row; INSERT it if it does not exist yet. */
    public void save(ChatbotConfig cfg) {
        String update = "UPDATE chatbot_config SET bot_name = ?, welcome_message = ?, fallback_response = ? WHERE id = 1";
        String insert = "INSERT INTO chatbot_config (id, bot_name, welcome_message, fallback_response) VALUES (1, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            int rows;
            try (PreparedStatement ps = con.prepareStatement(update)) {
                ps.setString(1, cfg.getBotName());
                ps.setString(2, cfg.getWelcomeMessage());
                ps.setString(3, cfg.getFallbackResponse());
                rows = ps.executeUpdate();
            }
            if (rows == 0) {
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    ps.setString(1, cfg.getBotName());
                    ps.setString(2, cfg.getWelcomeMessage());
                    ps.setString(3, cfg.getFallbackResponse());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save chatbot configuration", e);
        }
    }
}
