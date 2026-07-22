// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.manager;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ItemPoolManager {
    private final Hazestore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, List<StoreItem>> activeItemsMap;
    private final Map<String, Set<Integer>> sentWarningsMap;
    private final Map<String, Long> poolGenerations;

    public ItemPoolManager(Hazestore plugin) {
        this.plugin = plugin;
        this.activeItemsMap = new HashMap<>();
        this.sentWarningsMap = new HashMap<>();
        this.poolGenerations = new HashMap<>();
        loadAllActiveItems();
    }

    public void loadAllActiveItems() {
        for (String poolName : plugin.getPoolConfig().getPoolNames()) {
            loadActiveItems(poolName);
        }
    }

    public void loadActiveItems(String poolName) {
        List<StoreItem> activeItems = new ArrayList<>();
        List<String> savedIds = plugin.getDataManager().getCurrentItems(poolName);
        List<StoreItem> pool = plugin.getPoolConfig().getPool(poolName);

        if (savedIds != null && !savedIds.isEmpty()) {
            savedIds.forEach(id -> pool.stream()
                    .filter(i -> i.id().equals(id))
                    .findFirst()
                    .ifPresent(activeItems::add));
        }

        long currentTime = System.currentTimeMillis();
        List<Integer> poolSlots = getPoolSlots(poolName);
        if (activeItems.size() != poolSlots.size() || 
            (plugin.getDataManager().getNextRotationTime(poolName) > 0 && 
             currentTime >= plugin.getDataManager().getNextRotationTime(poolName))) {
            rotatePool(poolName, false);
        } else {
            activeItemsMap.put(poolName, activeItems);
        }
    }

    private List<Integer> getPoolSlots(String poolName) {
        Map<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> slotConfig;
        com.kala.hazestore.config.PoolConfig.CustomGuiConfig custom = plugin.getPoolConfig().getCustomGui(poolName);
        if (custom != null && !custom.getSlots().isEmpty()) {
            slotConfig = custom.getSlots();
        } else {
            slotConfig = plugin.getGuiConfig().getSlots();
        }

        List<Integer> slots = new ArrayList<>();
        for (Map.Entry<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> entry : slotConfig.entrySet()) {
            if (entry.getValue().type().equals("pool")) {
                slots.add(entry.getKey());
            }
        }
        if (slots.isEmpty()) {
            slots = new ArrayList<>(Arrays.asList(21, 22, 23));
        }
        return slots;
    }

    public void rotatePool(String poolName, boolean force) {
        List<StoreItem> pool = plugin.getPoolConfig().getPool(poolName);
        if (pool.isEmpty()) {
            plugin.getLogger().warning("The item pool " + poolName + " is empty! Cannot rotate.");
            return;
        }

        List<StoreItem> activeItems = new ArrayList<>();
        List<StoreItem> available = new ArrayList<>(pool);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Integer> poolSlots = getPoolSlots(poolName);
        for (int i = 0; i < poolSlots.size() && !available.isEmpty(); i++) {
            int totalWeight = available.stream().mapToInt(StoreItem::weight).sum();
            if (totalWeight <= 0) break;

            int value = random.nextInt(totalWeight);
            int currentWeight = 0;

            for (StoreItem item : available) {
                currentWeight += item.weight();
                if (currentWeight > value) {
                    activeItems.add(item);
                    available.remove(item);
                    break;
                }
            }
        }

        activeItemsMap.put(poolName, activeItems);

        List<String> ids = activeItems.stream().map(StoreItem::id).toList();
        plugin.getDataManager().setCurrentItems(poolName, ids);

        long nextTime = System.currentTimeMillis() + plugin.getConfigManager().getRotationMillis();
        plugin.getDataManager().setNextRotationTime(poolName, nextTime);
        plugin.getDataManager().resetPurchases(poolName);
        plugin.getDataManager().resetRevealed(poolName);
        plugin.getDataManager().saveData();

        sentWarningsMap.put(poolName, new HashSet<>());
        poolGenerations.put(poolName, poolGenerations.getOrDefault(poolName, 0L) + 1);

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().buildCache(poolName);
            plugin.getGuiManager().refreshViewers(poolName);
        }

        if (plugin.getConfigManager().isBroadcastEnabled() && plugin.getConfigManager().isBroadcastRotation()) {
            Bukkit.broadcast(mm.deserialize(plugin.getConfigManager().getMessage("store-rotated")));
        }
    }

    public void setPoolItems(String poolName, List<String> itemIds) {
        List<StoreItem> pool = plugin.getPoolConfig().getPool(poolName);
        List<StoreItem> activeItems = new ArrayList<>();

        for (String id : itemIds) {
            pool.stream()
                .filter(i -> i.id().equals(id))
                .findFirst()
                .ifPresent(activeItems::add);
        }

        activeItemsMap.put(poolName, activeItems);
        plugin.getDataManager().setCurrentItems(poolName, itemIds);
        plugin.getDataManager().resetPurchases(poolName);
        plugin.getDataManager().resetRevealed(poolName);
        plugin.getDataManager().saveData();
        poolGenerations.put(poolName, poolGenerations.getOrDefault(poolName, 0L) + 1);

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().buildCache(poolName);
            plugin.getGuiManager().refreshViewers(poolName);
        }
    }

    public void refreshActiveItems(String poolName) {
        List<StoreItem> current = activeItemsMap.get(poolName);

        if ((current == null || current.isEmpty()) && plugin.getDataManager().getNextRotationTime(poolName) <= 0) {
            rotatePool(poolName, false);
            return;
        }

        List<StoreItem> pool = plugin.getPoolConfig().getPool(poolName);
        List<StoreItem> refreshed = new ArrayList<>();
        if (current != null) {
            for (StoreItem active : current) {
                pool.stream()
                    .filter(i -> i.id().equals(active.id()))
                    .findFirst()
                    .ifPresent(refreshed::add);
            }
        }
        activeItemsMap.put(poolName, refreshed);
    }

    public void checkTimer() {
        long currentTime = System.currentTimeMillis();

        for (String poolName : plugin.getPoolConfig().getPoolNames()) {
            long nextRotation = plugin.getDataManager().getNextRotationTime(poolName);

            if (nextRotation <= 0) continue;

            long diff = nextRotation - currentTime;

            if (diff <= 0) {
                rotatePool(poolName, false);
                continue;
            }

            if (!plugin.getConfigManager().isBroadcastEnabled() || !plugin.getConfigManager().isBroadcastWarnings()) continue;

            Set<Integer> sentWarnings = sentWarningsMap.computeIfAbsent(poolName, k -> new HashSet<>());
            Map<Integer, String> warnings = plugin.getConfigManager().getWarnings();
            for (Map.Entry<Integer, String> entry : warnings.entrySet()) {
                int minutes = entry.getKey();
                long thresholdMs = minutes * 60_000L;
                if (!sentWarnings.contains(minutes) && diff <= thresholdMs && diff > thresholdMs - 2000) {
                    sentWarnings.add(minutes);
                    Bukkit.broadcast(mm.deserialize(plugin.getConfigManager().getPrefix() + entry.getValue()));
                }
            }
        }
    }

    public List<StoreItem> getActiveItems(String poolName) { 
        return activeItemsMap.getOrDefault(poolName, new ArrayList<>()); 
    }

    public long getPoolGeneration(String poolName) { 
        return poolGenerations.getOrDefault(poolName, 0L); 
    }
}
