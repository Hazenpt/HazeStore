package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// made with ❤️ by haze

public class DataManager {
    private final Hazestore plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private long nextRotationTime;
    private List<String> currentItems;

    public DataManager(Hazestore plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
    }

    public void loadData() {
        nextRotationTime = dataConfig.getLong("next-rotation-time", 0L);
        currentItems = dataConfig.getStringList("current-items");
        if (currentItems == null) {
            currentItems = new ArrayList<>();
        }
    }

    public void saveData() {
        dataConfig.set("next-rotation-time", nextRotationTime);
        dataConfig.set("current-items", currentItems);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
        }
    }

    public boolean reload() {
        boolean regenerated = false;
        if (!dataFile.exists()) {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            nextRotationTime = 0L;
            currentItems = new ArrayList<>();
            saveData();
            regenerated = true;
        } else {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            loadData();
        }
        return regenerated;
    }

    public long getNextRotationTime() { return nextRotationTime; }
    public void setNextRotationTime(long nextRotationTime) { this.nextRotationTime = nextRotationTime; }

    public List<String> getCurrentItems() { return currentItems; }
    public void setCurrentItems(List<String> currentItems) { this.currentItems = currentItems; }
}
