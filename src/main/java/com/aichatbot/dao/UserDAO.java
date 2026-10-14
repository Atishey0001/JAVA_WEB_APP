package com.aichatbot.dao;

import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.User;
import com.aichatbot.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** JDBC access to the users table. Every query uses PreparedStatement + try-with-resources. */
public class UserDAO {

    private static final String COLUMNS = "id, full_name, email, password_hash, role, created_at";

    /** INSERT: returns the same user with its generated id. */
    public User create(User user) {
        String sql = "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Could not create user", e);
        }
    }

    /** SELECT one row; returns null when not found. */
    public User findByEmail(String email) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not look up user by email", e);
        }
    }

    public User findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not look up user", e);
        }
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    public List<User> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY created_at DESC, id DESC";
        List<User> users = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list users", e);
        }
    }

    public int count() {
        return scalarCount("SELECT COUNT(*) FROM users");
    }

    public int countAdmins() {
        return scalarCount("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'");
    }

    /** UPDATE */
    public boolean updateName(int id, String fullName) {
        return update("UPDATE users SET full_name = ? WHERE id = ?", fullName, id);
    }

    public boolean updatePassword(int id, String passwordHash) {
        return update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    /** DELETE: only ordinary users can be deleted, admins are protected at SQL level. */
    public boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ? AND role = 'USER'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete user", e);
        }
    }

    private boolean update(String sql, String value, int id) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not update user", e);
        }
    }

    private int scalarCount(String sql) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not count users", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
