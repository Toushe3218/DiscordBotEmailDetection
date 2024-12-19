package com.phisebot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:sqlite:phisebot.db";

    // Initialize the database and create tables if they don't exist
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt1 = conn.prepareStatement("CREATE TABLE IF NOT EXISTS allowed_senders (email TEXT PRIMARY KEY)");
             PreparedStatement stmt2 = conn.prepareStatement("CREATE TABLE IF NOT EXISTS blocked_senders (email TEXT PRIMARY KEY)")) {

            stmt1.executeUpdate();
            stmt2.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add an allowed sender
    public static boolean addAllowedSender(String sender) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO allowed_senders (email) VALUES (?)")) {

            stmt.setString(1, sender);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Add a blocked sender
    public static boolean addBlockedSender(String sender) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO blocked_senders (email) VALUES (?)")) {

            stmt.setString(1, sender);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Remove an allowed sender
    public static boolean removeAllowedSender(String sender) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM allowed_senders WHERE email = ?")) {

            stmt.setString(1, sender);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Remove a blocked sender
    public static boolean removeBlockedSender(String sender) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM blocked_senders WHERE email = ?")) {

            stmt.setString(1, sender);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get all allowed senders
    public static List<String> getAllowedSenders() {
        List<String> allowedSenders = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT email FROM allowed_senders");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                allowedSenders.add(rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allowedSenders;
    }

    // Get all blocked senders
    public static List<String> getBlockedSenders() {
        List<String> blockedSenders = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT email FROM blocked_senders");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                blockedSenders.add(rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return blockedSenders;
    }

    // Check if sender is allowed
    public static boolean isSenderAllowed(String sender) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM allowed_senders WHERE email = ?")) {

            stmt.setString(1, sender);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if sender is blocked
    public static boolean isSenderBlocked(String sender) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM blocked_senders WHERE email = ?")) {

            stmt.setString(1, sender);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
