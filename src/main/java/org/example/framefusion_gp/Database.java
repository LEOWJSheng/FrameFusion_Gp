package org.example.framefusion_gp;

import java.sql.*;
import java.util.*;

public class Database {
    private static Connection connectDb() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/framefusion", "root", "");
    }

    public static List<ImageData> getImages() {
        List<ImageData> list = new ArrayList<>();
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM photos");
             ResultSet rs = stmt.executeQuery()) {

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
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO photos (image_path, annotation) VALUES (?, ?)")) {
            stmt.setString(1, path);
            stmt.setString(2, annotation);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateAnnotation(int id, String annotation) {
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement("UPDATE photos SET annotation = ? WHERE id = ?")) {
            stmt.setString(1, annotation);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class VideoData {
        public final int    id;
        public final String path;
        public VideoData(int id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    /** Returns all saved videos (id + path) */
    public static List<VideoData> getVideos() {
        List<VideoData> out = new ArrayList<>();
        String sql = "SELECT id, video_path FROM videos ORDER BY id";
        try (Connection c = connectDb();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
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

    /** Persist one exported video (path only) */
    public static void insertVideo(String path) {
        String sql = "INSERT INTO videos (video_path) VALUES (?)";
        try (Connection c = connectDb();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, path);
            p.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}