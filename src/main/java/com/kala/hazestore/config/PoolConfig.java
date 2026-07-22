// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoolConfig {
    private final Hazestore plugin;
    private final Map<String, List<StoreItem>> pools;
    private final Map<String, CustomGuiConfig> poolGuis;
    private File poolsFolder;

    public static class CustomGuiConfig {
        private final String title;
        private final int size;
        private final Map<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> slots;

        public CustomGuiConfig(String title, int size, Map<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> slots) {
            this.title = title;
            this.size = size;
            this.slots = slots;
        }

        public String getTitle() { return title; }
        public int getSize() { return size; }
        public Map<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> getSlots() { return slots; }
    }

    public PoolConfig(Hazestore plugin) {
        this.plugin = plugin;
        this.pools = new HashMap<>();
        this.poolGuis = new HashMap<>();
        load();
    }

    public boolean load() {
        poolsFolder = new File(plugin.getDataFolder(), "pools");
        boolean regenerated = false;

        if (!poolsFolder.exists()) {
            poolsFolder.mkdirs();
            regenerated = true;
        }
        
        File defaultPool = new File(poolsFolder, "default.yml");
        if (!defaultPool.exists()) {
            plugin.saveResource("pools/default.yml", false);
            regenerated = true;
        }

        pools.clear();
        poolGuis.clear();
        loadAllPools();
        return regenerated;
    }

    private void loadAllPools() {
        File[] files = poolsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String poolName = file.getName().replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<StoreItem> pool = loadPoolFromConfig(config);
            pools.put(poolName, pool);

            if (config.contains("gui")) {
                ConfigurationSection guiSec = config.getConfigurationSection("gui");
                if (guiSec != null) {
                    String title = guiSec.getString("title", "Store");
                    int size = guiSec.getInt("size", 27);
                    Map<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> slots = new HashMap<>();
                    ConfigurationSection slotsSection = guiSec.getConfigurationSection("slots");
                    if (slotsSection != null) {
                        for (String key : slotsSection.getKeys(false)) {
                            try {
                                int slot = Integer.parseInt(key);
                                ConfigurationSection slotSec = slotsSection.getConfigurationSection(key);
                                if (slotSec != null) {
                                    String type = slotSec.getString("type", "filler");
                                    String material = slotSec.getString("material", "BLACK_STAINED_GLASS_PANE");
                                    String name = slotSec.getString("name", " ");
                                    List<String> lore = slotSec.getStringList("lore");
                                    slots.put(slot, new com.kala.hazestore.config.GuiConfig.SlotConfig(type, material, name, lore));
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    poolGuis.put(poolName, new CustomGuiConfig(title, size, slots));
                }
            }
        }
    }

    private List<StoreItem> loadPoolFromConfig(FileConfiguration config) {
        List<StoreItem> pool = new ArrayList<>();
        ConfigurationSection poolSection = config.getConfigurationSection("items");
        if (poolSection == null) return pool;

        for (String key : poolSection.getKeys(false)) {
            ConfigurationSection itemSec = poolSection.getConfigurationSection(key);
            if (itemSec == null) continue;

            String itemType = itemSec.getString("type");
            String mmoType = itemSec.getString("mmoitems_type");
            if (mmoType == null) mmoType = itemSec.getString("mmoitems-type");
            String mmoId = itemSec.getString("mmoitems_id");
            if (mmoId == null) mmoId = itemSec.getString("mmoitems-id");
            String material = itemSec.getString("material");
            String itemsadderId = itemSec.getString("itemsadder_id");
            if (itemsadderId == null) itemsadderId = itemSec.getString("itemsadder-id");

            if (itemType == null || itemType.isBlank()) {
                if (mmoType != null && mmoId != null) {
                    itemType = "mmoitems";
                } else if (itemsadderId != null) {
                    itemType = "itemsadder";
                } else {
                    itemType = "vanilla";
                }
            }

            int amount = itemSec.getInt("amount", 1);
            double price = itemSec.getDouble("price", 0);
            int weight = itemSec.getInt("weight", 10);
            boolean vip = itemSec.getBoolean("vip", false);
            boolean hidden = itemSec.getBoolean("hidden", false);
            int maxPurchases = itemSec.getInt("max-purchases");
            if (!itemSec.contains("max-purchases")) {
                maxPurchases = itemSec.getInt("max_purchases", -1);
            }
            int slot = itemSec.getInt("slot", -1);
            String displayName = itemSec.getString("display-name");
            if (displayName == null) displayName = itemSec.getString("display_name");

            if ("mmoitems".equalsIgnoreCase(itemType) && mmoType != null && mmoId != null) {
                pool.add(new StoreItem(key, itemType, mmoType, mmoId, null, null, amount, price, weight, vip, hidden, maxPurchases, slot, displayName));
            } else if ("itemsadder".equalsIgnoreCase(itemType) && itemsadderId != null) {
                pool.add(new StoreItem(key, itemType, null, null, null, itemsadderId, amount, price, weight, vip, hidden, maxPurchases, slot, displayName));
            } else if ("vanilla".equalsIgnoreCase(itemType) && material != null) {
                pool.add(new StoreItem(key, itemType, null, null, material, null, amount, price, weight, vip, hidden, maxPurchases, slot, displayName));
            } else {
                plugin.getLogger().warning("[HazeStore] Item '" + key + "' ignorato: configurazione incompleta (type=" + itemType
                        + ", mmoitems=" + mmoType + "/" + mmoId + ", itemsadder=" + itemsadderId + ", material=" + material + ").");
            }
        }
        return pool;
    }

    public List<StoreItem> getPool(String name) {
        return pools.getOrDefault(name, new ArrayList<>());
    }

    public List<String> getPoolNames() {
        return new ArrayList<>(pools.keySet());
    }

    public Map<String, List<StoreItem>> getAllPools() {
        return new HashMap<>(pools);
    }

    public CustomGuiConfig getCustomGui(String name) {
        return poolGuis.get(name);
    }
}
