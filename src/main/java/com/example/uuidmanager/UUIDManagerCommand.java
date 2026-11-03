package com.example.uuidmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.Locale;

public class UUIDManagerCommand implements CommandExecutor {
    private final UUIDManagerPlugin plugin;

    public UUIDManagerCommand(UUIDManagerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e[UUIDManager] 用法: /" + label + " <reload|status>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            plugin.reloadConfig();
            sender.sendMessage("§a[UUIDManager] 配置已重载");
            sendStatus(sender);
            return true;
        }
        if ("status".equals(sub)) {
            sendStatus(sender);
            return true;
        }

        sender.sendMessage("§c[UUIDManager] 未知子命令: " + args[0] + "，可用: reload, status");
        return true;
    }

    private void sendStatus(CommandSender sender) {
        DatabaseManager dbManager = plugin.getDbManager();
        if (dbManager == null || dbManager.getConnection() == null) {
            sender.sendMessage("§c[UUIDManager] 数据库: 连接不可用");
            return;
        }
        int total = dbManager.countRecords();
        String dbPath = plugin.getDataFolder() + "/player_uuids.db";
        if (total < 0) {
            sender.sendMessage("§c[UUIDManager] 数据库: 已连接，但记录条数查询失败（" + dbPath + "）");
            return;
        }
        sender.sendMessage("§a[UUIDManager] 数据库: 已连接，记录条数 " + total + "（" + dbPath + "）");
        plugin.getLogger().info("数据库状态: 已连接，记录条数 " + total + "（" + dbPath + "）");
    }
}
