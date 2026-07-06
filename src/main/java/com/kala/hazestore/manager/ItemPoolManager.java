package com.kala.hazestore.manager;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

// made with ❤️ by haze

public class ItemPoolManager {
    private final Hazestore plugin;
    private final List<StoreItem> activeItems;
    private final Set<Integer> sentWarnings = new HashSet<>();
    private long poolGeneration;

    public ItemPoolManager(Hazestore plugin) {
        this.plugin = plugin;
        this.activeItems = new ArrayList<>();
        loadActiveItems();
    }

    public void loadActiveItems() {
        activeItems.clear();
        List<String> savedIds = plugin.getDataManager().getCurrentItems();
        List<StoreItem> pool = plugin.getConfigManager().getPool();

        if (savedIds != null && !savedIds.isEmpty()) {
            savedIds.forEach(id -> pool.stream()
                    .filter(i -> i.id().equals(id))
                    .findFirst()
                    .ifPresent(activeItems::add));
        }

        long currentTime = System.currentTimeMillis();
        if (activeItems.size() != 3 || (plugin.getDataManager().getNextRotationTime() > 0 && currentTime >= plugin.getDataManager().getNextRotationTime())) {
            rotatePool(false);
        } else if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().buildCache(activeItems);
        }
    }

    public void rotatePool(boolean force) {
        List<StoreItem> pool = plugin.getConfigManager().getPool();
        if (pool.isEmpty()) {
            plugin.getLogger().warning("The item pool is empty! Cannot rotate.");
            return;
        }

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().closeAll();
        }

        activeItems.clear();
        List<StoreItem> available = new ArrayList<>(pool);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < 3 && !available.isEmpty(); i++) {
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

        List<String> ids = activeItems.stream().map(StoreItem::id).toList();
        plugin.getDataManager().setCurrentItems(ids);

        long nextTime = System.currentTimeMillis() + plugin.getConfigManager().getRotationMillis();
        plugin.getDataManager().setNextRotationTime(nextTime);
        plugin.getDataManager().saveData();

        sentWarnings.clear();
        poolGeneration++;

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().buildCache(activeItems);
        }

        String rotatedMsg = plugin.getConfigManager().getMessage("store-rotated");
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(rotatedMsg));
    }

    public void checkTimer() {
        long currentTime = System.currentTimeMillis();
        long nextRotation = plugin.getDataManager().getNextRotationTime();

        if (nextRotation <= 0) return;

        long diff = nextRotation - currentTime;

        if (diff <= 0) {
            rotatePool(false);
            return;
        }

        Map<Integer, String> warnings = plugin.getConfigManager().getWarnings();
        for (Map.Entry<Integer, String> entry : warnings.entrySet()) {
            int minutes = entry.getKey();
            long thresholdMs = minutes * 60_000L;
            if (!sentWarnings.contains(minutes) && diff <= thresholdMs && diff > thresholdMs - 2000) {
                sentWarnings.add(minutes);
                String msg = plugin.getConfigManager().getPrefix() + entry.getValue();
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(msg));
            }
        }
    }

    public List<StoreItem> getActiveItems() {
        return activeItems;
    }

    public long getPoolGeneration() {
        return poolGeneration;
    }
}
