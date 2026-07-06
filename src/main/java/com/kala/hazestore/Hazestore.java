package com.kala.hazestore;

import com.kala.hazestore.command.HazestoreCommand;
import com.kala.hazestore.config.ConfigManager;
import com.kala.hazestore.config.DataManager;
import com.kala.hazestore.config.LogManager;
import com.kala.hazestore.gui.HazestoreGUI;
import com.kala.hazestore.manager.ItemPoolManager;
import com.kala.hazestore.task.RotationTask;
import org.bukkit.plugin.java.JavaPlugin;

// made with ❤️ by haze

public class Hazestore extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private LogManager logManager;
    private ItemPoolManager itemPoolManager;
    private HazestoreGUI guiManager;

    @Override
    public void onEnable() {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
            "\n<gold>  _   _                    _               \n" +
            " | | | |                  | |              \n" +
            " | |_| | __ _ _______  ___| |_ ___  _ __ ___ \n" +
            " |  _  |/ _` |_  / _ \\/ __| __/ _ \\| '__/ _ \\\n" +
            " | | | | (_| |/ /  __/\\__ \\ || (_) | | |  __/\n" +
            " \\_| |_/\\__,_/___\\___||___/\\__\\___/|_|  \\___|\n" +
            "    <gray>made with ❤️ by haze - v" + getDescription().getVersion() + "</gray>\n" +
            "</gold>"
        ));

        if (getServer().getPluginManager().getPlugin("MMOItems") == null) {
            getLogger().severe("MMOItems not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("CoinsEngine") == null && getServer().getPluginManager().getPlugin("ExcellentEconomy") == null) {
            getLogger().warning("CoinsEngine or ExcellentEconomy not found! Transactions might fail if no compatible economy is present.");
        }

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.logManager = new LogManager(this);
        this.itemPoolManager = new ItemPoolManager(this);
        this.guiManager = new HazestoreGUI(this);
        this.guiManager.buildCache(this.itemPoolManager.getActiveItems());

        HazestoreCommand command = new HazestoreCommand(this);
        if (getCommand("hs") != null) {
            getCommand("hs").setExecutor(command);
            getCommand("hs").setTabCompleter(command);
        }

        new RotationTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("Hazestore enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }
        if (guiManager != null) {
            guiManager.closeAll();
        }
        getLogger().info("Hazestore disabled!");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public LogManager getLogManager() { return logManager; }
    public ItemPoolManager getItemPoolManager() { return itemPoolManager; }
    public HazestoreGUI getGuiManager() { return guiManager; }
}
