package com.example.uuidmanager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class UUIDManagerPlugin extends JavaPlugin {
    public DatabaseManager getDbManager() {
        return dbManager;
    }
    private DatabaseManager dbManager;

    @Override
    public void onEnable() {
        getLogger().info("UUID Manager Plugin 已启用！");
        // 初始化数据库连接
        this.dbManager = new DatabaseManager(this);
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        // 注册命令执行器（plugin.yml 已声明 uuidmanager 命令）
        PluginCommand cmd = getCommand("uuidmanager");
        if (cmd != null) {
            cmd.setExecutor(new UUIDManagerCommand(this));
        } else {
            getLogger().severe("无法注册 uuidmanager 命令：plugin.yml 中未找到该命令声明");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("UUID Manager Plugin 已禁用！");
        // 关闭数据库连接
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
