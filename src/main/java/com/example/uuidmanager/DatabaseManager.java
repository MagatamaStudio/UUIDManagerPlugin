package com.example.uuidmanager;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.*;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String dbPath = plugin.getDataFolder() + "/player_uuids.db";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTable();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_uuids (\n" +
                     "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                     "    player_id TEXT NOT NULL,\n" +
                     "    old_uuid TEXT NOT NULL,\n" +
                     "    new_uuid TEXT,\n" +
                     "    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("玩家UUID表创建成功或已存在");
        } catch (SQLException e) {
            plugin.getLogger().severe("创建表失败: " + e.getMessage());
        }
    }

    public void insertPlayerUUID(String playerId, String uuid) {
        if (connection == null) {
            plugin.getLogger().severe("插入记录失败: 数据库连接不可用");
            return;
        }
        String sql = "INSERT INTO player_uuids (player_id, old_uuid) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("插入记录失败: " + e.getMessage());
        }
    }

    public String getPlayerIdByUUID(String uuid) {
        if (connection == null) {
            plugin.getLogger().severe("查询记录失败: 数据库连接不可用");
            return null;
        }
        String sql = "SELECT player_id FROM player_uuids WHERE old_uuid = ? OR new_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, uuid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("player_id");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("查询记录失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 按玩家名查询其最近一条记录实际生效的 UUID（有新 UUID 则用新 UUID，否则用旧 UUID）。
     */
    public String getLatestUuidByPlayerId(String playerId) {
        if (connection == null) {
            plugin.getLogger().severe("查询最新UUID失败: 数据库连接不可用");
            return null;
        }
        String sql = "SELECT COALESCE(new_uuid, old_uuid) AS uuid FROM player_uuids WHERE player_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("uuid");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("查询最新UUID失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 把指定旧 UUID 的未迁移记录标记为已迁移到新 UUID。
     */
    public boolean updateNewUuid(String playerId, String oldUuid, String newUuid) {
        if (connection == null) {
            plugin.getLogger().severe("更新UUID失败: 数据库连接不可用");
            return false;
        }
        String sql = "UPDATE player_uuids SET new_uuid = ?, last_updated = CURRENT_TIMESTAMP WHERE player_id = ? AND old_uuid = ? AND new_uuid IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newUuid);
            pstmt.setString(2, playerId);
            pstmt.setString(3, oldUuid);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新UUID失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 统计当前记录条数（供 /uuidmanager status 使用），查询失败返回 -1。
     */
    public int countRecords() {
        if (connection == null) {
            plugin.getLogger().severe("统计记录失败: 数据库连接不可用");
            return -1;
        }
        String sql = "SELECT COUNT(*) AS total FROM player_uuids";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("统计记录失败: " + e.getMessage());
        }
        return -1;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
}
