// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore;

import com.kala.hazestore.command.HazestoreCommand;
import com.kala.hazestore.config.*;
import com.kala.hazestore.gui.HazestoreGUI;
import com.kala.hazestore.gui.PoolEditorGUI;
import com.kala.hazestore.manager.ItemPoolManager;
import com.kala.hazestore.task.RotationTask;
import com.kala.hazestore.util.MaterialHelper;
import com.kala.hazestore.util.ServerVersion;
import org.bukkit.plugin.java.JavaPlugin;

public class Hazestore extends JavaPlugin {

    private ConfigManager configManager;
    private GuiConfig guiConfig;
    private ConfirmationConfig confirmationConfig;
    private PoolConfig poolConfig;
    private DataManager dataManager;
    private LogManager logManager;
    private ItemPoolManager itemPoolManager;
    private HazestoreGUI guiManager;
    private PoolEditorGUI poolEditorGUI;
    private boolean mmoItemsEnabled;
    private boolean coinsEngineEnabled;
    private boolean placeholderApiEnabled;
    private boolean itemsAdderEnabled;

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
            "</gold>"));

        MaterialHelper.init(getLogger());
        ServerVersion detectedVersion = ServerVersion.getCurrent();
        getLogger().info("[HazeStore] Detected server version: 1." + detectedVersion.getMinor() + " - Compatibility: OK");

        if (!detectedVersion.isSupported()) {
            getLogger().severe("[HazeStore] Unsupported server version! HazeStore requires Minecraft 1.19 or newer.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.mmoItemsEnabled = getServer().getPluginManager().getPlugin("MMOItems") != null;
        this.coinsEngineEnabled = getServer().getPluginManager().getPlugin("CoinsEngine") != null || getServer().getPluginManager().getPlugin("ExcellentEconomy") != null;
        this.placeholderApiEnabled = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.itemsAdderEnabled = getServer().getPluginManager().getPlugin("ItemsAdder") != null;

        if (!mmoItemsEnabled) {
            getLogger().warning("[HazeStore] MMOItems not found! MMOItems shop items will be disabled.");
        }
        if (!coinsEngineEnabled) {
            getLogger().warning("[HazeStore] CoinsEngine or ExcellentEconomy not found! Transactions might fail.");
        }
        if (!placeholderApiEnabled) {
            getLogger().warning("[HazeStore] PlaceholderAPI not found! Placeholder support disabled.");
        }
        if (!itemsAdderEnabled) {
            getLogger().warning("[HazeStore] ItemsAdder not found! ItemsAdder shop items will be disabled.");
        }

        saveDefaultConfig();
        saveResource("gui.yml", false);
        saveResource("confirmation.yml", false);
        saveResource("pools/default.yml", false);
        
        this.configManager = new ConfigManager(this);
        this.guiConfig = new GuiConfig(this);
        this.confirmationConfig = new ConfirmationConfig(this);
        this.poolConfig = new PoolConfig(this);
        this.dataManager = new DataManager(this);
        this.logManager = new LogManager(this);
        this.itemPoolManager = new ItemPoolManager(this);
        this.guiManager = new HazestoreGUI(this);
        this.poolEditorGUI = new PoolEditorGUI(this);

        HazestoreCommand command = new HazestoreCommand(this);
        if (getCommand("hs") != null) {
            getCommand("hs").setExecutor(command);
            getCommand("hs").setTabCompleter(command);
        }

        if (placeholderApiEnabled) {
            new com.kala.hazestore.placeholder.HazeStoreExpansion(this).register();
        }

        new RotationTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("HazeStore enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveData();
        }
        if (guiManager != null) {
            guiManager.closeAll();
        }
        getLogger().info("HazeStore disabled!");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public GuiConfig getGuiConfig() { return guiConfig; }
    public ConfirmationConfig getConfirmationConfig() { return confirmationConfig; }
    public PoolConfig getPoolConfig() { return poolConfig; }
    public DataManager getDataManager() { return dataManager; }
    public LogManager getLogManager() { return logManager; }
    public ItemPoolManager getItemPoolManager() { return itemPoolManager; }
    public HazestoreGUI getGuiManager() { return guiManager; }
    public PoolEditorGUI getPoolEditorGUI() { return poolEditorGUI; }
    public boolean isMmoItemsEnabled() { return mmoItemsEnabled; }
    public boolean isCoinsEngineEnabled() { return coinsEngineEnabled; }
    public boolean isPlaceholderApiEnabled() { return placeholderApiEnabled; }
    public boolean isItemsAdderEnabled() { return itemsAdderEnabled; }
}
