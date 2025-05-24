package org.example.framefusion_gp;

import java.sql.*;
import java.util.*;

public class Database {

    private static final String URL_WITHOUT_DB = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
    private static final String URL_WITH_DB = "jdbc:mysql://localhost:3306/framefusion?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connectDb() throws SQLException {
        // Connect to the 'framefusion' database specifically
        return DriverManager.getConnection(URL_WITH_DB, USER, PASSWORD);
    }

    private static Connection connectServer() throws SQLException {
        // Connect to MySQL server without selecting a database
        return DriverManager.getConnection(URL_WITHOUT_DB, USER, PASSWORD);
    }

    public static void createDatabaseIfNotExists() {
        String createDbSQL = "CREATE DATABASE IF NOT EXISTS framefusion CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection conn = connectServer(); Statement stmt = conn.createStatement()) {
            stmt.execute(createDbSQL);
            System.out.println("Database 'framefusion' created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTablesIfNotExist() {
        String createVideosTable = """
            CREATE TABLE IF NOT EXISTS videos (
                id INT(11) NOT NULL AUTO_INCREMENT,
                video_path TEXT DEFAULT NULL,
                PRIMARY KEY (id)
            );
        """;

        String createPhotosTable = """
            CREATE TABLE IF NOT EXISTS photos (
                id INT PRIMARY KEY AUTO_INCREMENT,
                image_path TEXT,
                annotation TEXT
            );
        """;

        try (Connection conn = connectDb(); Statement stmt = conn.createStatement()) {
            stmt.execute(createVideosTable);
            stmt.execute(createPhotosTable);
            System.out.println("Tables created or already exist.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void initializeDatabase() {
        createDatabaseIfNotExists();
        createTablesIfNotExist();
    }

    public static List<ImageData> getImages() {
        List<ImageData> list = new ArrayList<>();
        try (Connection conn = connectDb(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM photos"); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new ImageData(
                        rs.getInt("id"),
                        rs.getString("image_path"),
                        rs.getString("annotation")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void insertImage(String path, String annotation) {
        try (Connection conn = connectDb(); PreparedStatement stmt = conn.prepareStatement("INSERT INTO photos (image_path, annotation) VALUES (?, ?)")) {
            stmt.setString(1, path);
            stmt.setString(2, annotation);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateAnnotation(int id, String annotation) {
        try (Connection conn = connectDb(); PreparedStatement stmt = conn.prepareStatement("UPDATE photos SET annotation = ? WHERE id = ?")) {
            stmt.setString(1, annotation);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class VideoData {

        public final int id;
        public final String path;

        public VideoData(int id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    /**
     * Returns all saved videos (id + path)
     */
    public static List<VideoData> getVideos() {
        List<VideoData> out = new ArrayList<>();
        String sql = "SELECT id, video_path FROM videos ORDER BY id";
        try (Connection c = connectDb(); PreparedStatement p = c.prepareStatement(sql); ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                out.add(new VideoData(
                        rs.getInt("id"),
                        rs.getString("video_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Persist one exported video (path only)
     */
    public static void insertVideo(String path) {
        String sql = "INSERT INTO videos (video_path) VALUES (?)";
        try (Connection c = connectDb(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, path);
            p.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteVideo(String absolutePath) {
        String sql = "DELETE FROM videos WHERE video_path = ?";
        try (Connection conn = connectDb(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, absolutePath);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void deleteImage(String absolutePath) {
        String sql = "DELETE FROM photos WHERE image_path = ?";
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, absolutePath);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
