package com.example.uuidmanager;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PlayerJoinListener implements Listener {
    private final UUIDManagerPlugin plugin;
    private final DatabaseManager dbManager;

    public PlayerJoinListener(UUIDManagerPlugin plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDbManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        String uuid = player.getUniqueId().toString();

        // 以“玩家名 + 最近一次记录的 UUID”为准，避免用新 UUID 反查永远查不到
        String recorded = dbManager.getLatestUuidByPlayerId(name);
        if (recorded == null) {
            dbManager.insertPlayerUUID(name, uuid);
            plugin.getLogger().info("记录新玩家UUID：" + name + " - " + uuid);
            return;
        }
        if (recorded.equalsIgnoreCase(uuid)) {
            // 正常回归，UUID 未变化
            return;
        }
        plugin.getLogger().info("玩家 " + name + " 的UUID已变化：" + recorded + " -> " + uuid);
        // 旧 uuid = recorded，新 uuid = 当前 uuid
        if (migrate(uuid, recorded)) {
            dbManager.updateNewUuid(name, recorded, uuid);
        }
    }

    private boolean migrate(String newUuid, String oldUuid) {
        return renameAndMoveFile(oldUuid, newUuid);
    }

    /**
     * 把旧 UUID 的玩家数据文件复制为所有世界 playerdata 下的新 UUID 文件。
     *
     * @return 至少成功写入一份时返回 true
     */
    private boolean renameAndMoveFile(String oldUuid, String newUuid) {
        File source = findSourceFile(oldUuid);
        if (source == null) {
            plugin.getLogger().severe("迁移失败：找不到旧UUID数据文件 " + oldUuid + ".dat（backups 与各世界 playerdata 均不存在，含 .dat_old）");
            return false;
        }

        boolean copied = false;
        for (File playerDataDir : getTargetPlayerDataDirs()) {
            if (!playerDataDir.exists() && !playerDataDir.mkdirs()) {
                plugin.getLogger().severe("迁移失败：无法创建目录 " + playerDataDir.getAbsolutePath());
                continue;
            }
            File target = new File(playerDataDir, newUuid + ".dat");
            try {
                // 使用复制而不是移动，保留原始备份文件
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied = true;
                plugin.getLogger().info("玩家数据已写入：" + target.getAbsolutePath() + "（来源 " + source.getAbsolutePath() + "）");
            } catch (Exception e) {
                plugin.getLogger().severe("写入玩家数据失败 " + target.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        if (!copied) {
            plugin.getLogger().severe("迁移失败：" + oldUuid + ".dat -> " + newUuid + ".dat 未写入任何世界");
            return false;
        }
        plugin.getLogger().info("文件迁移成功：" + oldUuid + ".dat -> " + newUuid + ".dat");
        return true;
    }

    /**
     * 查找源数据文件：插件 backups 目录 -> 各世界 playerdata 目录，先找 .dat 再找 .dat_old。
     */
    private File findSourceFile(String oldUuid) {
        String[] suffixes = {".dat", ".dat_old"};
        for (String suffix : suffixes) {
            File inBackups = new File(new File(plugin.getDataFolder(), "backups"), oldUuid + suffix);
            if (inBackups.isFile()) {
                return inBackups;
            }
            for (World world : plugin.getServer().getWorlds()) {
                File inPlayerData = new File(new File(world.getWorldFolder(), "playerdata"), oldUuid + suffix);
                if (inPlayerData.isFile()) {
                    return inPlayerData;
                }
            }
        }
        return null;
    }

    /**
     * 目标目录：所有世界的 playerdata 各一份；世界列表为空时退回主世界默认路径。
     */
    private List<File> getTargetPlayerDataDirs() {
        List<File> dirs = new ArrayList<File>();
        for (World world : plugin.getServer().getWorlds()) {
            File playerDataDir = new File(world.getWorldFolder(), "playerdata");
            if (!dirs.contains(playerDataDir)) {
                dirs.add(playerDataDir);
            }
        }
        if (dirs.isEmpty()) {
            dirs.add(new File(new File(plugin.getServer().getWorldContainer(), "world"), "playerdata"));
        }
        return dirs;
    }
}
