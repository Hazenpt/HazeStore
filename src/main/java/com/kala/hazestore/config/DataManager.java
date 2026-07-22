// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {
    private final Hazestore plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private Map<String, Long> nextRotationTimes;
    private Map<String, List<String>> currentItemsMap;
    private Map<String, Map<String, Integer>> purchaseCounts;
    private Map<UUID, Map<String, Set<String>>> revealedItems;

    public DataManager(Hazestore plugin) {
        this.plugin = plugin;
        this.nextRotationTimes = new HashMap<>();
        this.currentItemsMap = new HashMap<>();
        this.purchaseCounts = new HashMap<>();
        this.revealedItems = new HashMap<>();
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
        nextRotationTimes.clear();
        currentItemsMap.clear();
        purchaseCounts.clear();
        revealedItems.clear();

        ConfigurationSection rotationSection = dataConfig.getConfigurationSection("rotations");
        if (rotationSection != null) {
            for (String poolName : rotationSection.getKeys(false)) {
                nextRotationTimes.put(poolName, rotationSection.getLong(poolName + ".next-time", 0L));
                currentItemsMap.put(poolName, rotationSection.getStringList(poolName + ".current-items"));
            }
        }

        ConfigurationSection purchasesSection = dataConfig.getConfigurationSection("purchases");
        if (purchasesSection != null) {
            for (String poolName : purchasesSection.getKeys(false)) {
                Map<String, Integer> poolPurchases = new HashMap<>();
                ConfigurationSection poolSection = purchasesSection.getConfigurationSection(poolName);
                if (poolSection != null) {
                    for (String itemId : poolSection.getKeys(false)) {
                        poolPurchases.put(itemId, poolSection.getInt(itemId, 0));
                    }
                }
                purchaseCounts.put(poolName, poolPurchases);
            }
        }

        ConfigurationSection revealedSection = dataConfig.getConfigurationSection("revealed-items");
        if (revealedSection != null) {
            for (String uuidString : revealedSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                Map<String, Set<String>> poolRevealed = new HashMap<>();
                ConfigurationSection playerSection = revealedSection.getConfigurationSection(uuidString);
                if (playerSection != null) {
                    for (String poolName : playerSection.getKeys(false)) {
                        Set<String> items = new HashSet<>(playerSection.getStringList(poolName));
                        poolRevealed.put(poolName, items);
                    }
                }
                revealedItems.put(uuid, poolRevealed);
            }
        }
    }

    public void saveData() {
        dataConfig.set("rotations", null);
        dataConfig.set("purchases", null);
        dataConfig.set("revealed-items", null);

        for (Map.Entry<String, Long> entry : nextRotationTimes.entrySet()) {
            String poolName = entry.getKey();
            dataConfig.set("rotations." + poolName + ".next-time", entry.getValue());
            dataConfig.set("rotations." + poolName + ".current-items", currentItemsMap.getOrDefault(poolName, new ArrayList<>()));
        }

        for (Map.Entry<String, Map<String, Integer>> entry : purchaseCounts.entrySet()) {
            String poolName = entry.getKey();
            for (Map.Entry<String, Integer> itemEntry : entry.getValue().entrySet()) {
                dataConfig.set("purchases." + poolName + "." + itemEntry.getKey(), itemEntry.getValue());
            }
        }

        for (Map.Entry<UUID, Map<String, Set<String>>> playerEntry : revealedItems.entrySet()) {
            String uuidString = playerEntry.getKey().toString();
            for (Map.Entry<String, Set<String>> poolEntry : playerEntry.getValue().entrySet()) {
                dataConfig.set("revealed-items." + uuidString + "." + poolEntry.getKey(), new ArrayList<>(poolEntry.getValue()));
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
        }
    }

    public boolean isItemRevealed(UUID playerUUID, String poolName, String itemId) {
        Map<String, Set<String>> playerRevealed = revealedItems.get(playerUUID);
        if (playerRevealed == null) return false;
        Set<String> poolRevealed = playerRevealed.get(poolName);
        return poolRevealed != null && poolRevealed.contains(itemId);
    }

    public void revealItem(UUID playerUUID, String poolName, String itemId) {
        revealedItems.computeIfAbsent(playerUUID, k -> new HashMap<>())
                     .computeIfAbsent(poolName, k -> new HashSet<>())
                     .add(itemId);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveData);
    }

    public boolean reload() {
        boolean regenerated = false;
        if (!dataFile.exists()) {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            nextRotationTimes.clear();
            currentItemsMap.clear();
            purchaseCounts.clear();
            saveData();
            regenerated = true;
        } else {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            loadData();
        }
        return regenerated;
    }

    public long getNextRotationTime(String poolName) { return nextRotationTimes.getOrDefault(poolName, 0L); }
    public void setNextRotationTime(String poolName, long time) { nextRotationTimes.put(poolName, time); }

    public List<String> getCurrentItems(String poolName) { return currentItemsMap.getOrDefault(poolName, new ArrayList<>()); }
    public void setCurrentItems(String poolName, List<String> items) { currentItemsMap.put(poolName, items); }

    public int getPurchaseCount(String poolName, String itemId) {
        return purchaseCounts.getOrDefault(poolName, new HashMap<>()).getOrDefault(itemId, 0);
    }
    public void incrementPurchaseCount(String poolName, String itemId) {
        purchaseCounts.computeIfAbsent(poolName, k -> new HashMap<>());
        purchaseCounts.get(poolName).merge(itemId, 1, Integer::sum);
    }

    public void resetPurchases(String poolName) {
        purchaseCounts.remove(poolName);
    }

    public void resetRevealed(String poolName) {
        for (Map<String, Set<String>> playerRevealed : revealedItems.values()) {
            playerRevealed.remove(poolName);
        }
    }
}
