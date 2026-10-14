package com.aichatbot.dao;

import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.Feedback;
import com.aichatbot.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackDAO {

    private static final String SELECT_WITH_USER =
            "SELECT f.id, f.user_id, f.conversation_id, f.rating, f.comment, f.created_at, u.full_name "
          + "FROM feedback f JOIN users u ON u.id = f.user_id ";

    public Feedback create(Feedback fb) {
        String sql = "INSERT INTO feedback (user_id, conversation_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fb.getUserId());
            if (fb.getConversationId() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, fb.getConversationId());
            }
            ps.setInt(3, fb.getRating());
            ps.setString(4, fb.getComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    fb.setId(keys.getInt(1));
                }
            }
            return fb;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save feedback", e);
        }
    }

    /** @param rating null = all ratings */
    public List<Feedback> findAll(Integer rating) {
        String sql = SELECT_WITH_USER + (rating == null ? "" : "WHERE f.rating = ? ")
                   + "ORDER BY f.created_at DESC, f.id DESC LIMIT 500";
        return query(sql, rating);
    }

    public List<Feedback> findByUser(int userId) {
        return query(SELECT_WITH_USER + "WHERE f.user_id = ? ORDER BY f.created_at DESC, f.id DESC", userId);
    }

    public List<Feedback> findRecent(int limit) {
        return query(SELECT_WITH_USER + "ORDER BY f.created_at DESC, f.id DESC LIMIT " + Math.max(1, limit), null);
    }

    /** Low-rated interactions used by the "chatbot improvement" section. */
    public List<Feedback> findLowRated(int maxRating, int limit) {
        return query(SELECT_WITH_USER + "WHERE f.rating <= ? ORDER BY f.created_at DESC, f.id DESC LIMIT "
                + Math.max(1, limit), maxRating);
    }

    public int count() {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM feedback");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not count feedback", e);
        }
    }

    /** @return 0.0 when there is no feedback yet */
    public double averageRating() {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT AVG(rating) FROM feedback");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0; // AVG of no rows is NULL -> getDouble gives 0.0
        } catch (SQLException e) {
            throw new DatabaseException("Could not calculate average rating", e);
        }
    }

    /** Map rating (1..5) -> number of submissions; missing ratings are filled with 0. */
    public Map<Integer, Integer> ratingDistribution() {
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int r = 1; r <= 5; r++) {
            distribution.put(r, 0);
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT rating, COUNT(*) FROM feedback GROUP BY rating");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                distribution.put(rs.getInt(1), rs.getInt(2));
            }
            return distribution;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load rating distribution", e);
        }
    }

    private List<Feedback> query(String sql, Integer param) {
        List<Feedback> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Feedback f = new Feedback();
                    f.setId(rs.getInt("id"));
                    f.setUserId(rs.getInt("user_id"));
                    int convId = rs.getInt("conversation_id");
                    f.setConversationId(rs.wasNull() ? null : convId);
                    f.setRating(rs.getInt("rating"));
                    f.setComment(rs.getString("comment"));
                    f.setCreatedAt(rs.getTimestamp("created_at"));
                    f.setUserName(rs.getString("full_name"));
                    list.add(f);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Could not load feedback", e);
        }
    }
}
