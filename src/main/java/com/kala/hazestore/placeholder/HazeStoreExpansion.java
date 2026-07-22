// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.placeholder;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HazeStoreExpansion extends PlaceholderExpansion {
    private final Hazestore plugin;

    public HazeStoreExpansion(Hazestore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hazestore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "haze";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("default_pool")) {
            return plugin.getConfigManager().getDefaultPool();
        }

        String[] parts = params.split("_");
        if (parts.length >= 2) {
            String poolName = parts[0];
            String action = parts[1];

            if (action.equalsIgnoreCase("rotation")) {
                if (parts.length >= 3 && parts[2].equalsIgnoreCase("time")) {
                    long nextRotation = plugin.getDataManager().getNextRotationTime(poolName);
                    long currentTime = System.currentTimeMillis();
                    long remaining = nextRotation - currentTime;
                    if (remaining <= 0) {
                        return "0";
                    }

                    long seconds = remaining / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;

                    if (parts.length >= 4 && parts[3].equalsIgnoreCase("seconds")) {
                        return String.valueOf(seconds);
                    }
                    if (parts.length >= 4 && parts[3].equalsIgnoreCase("formatted")) {
                        if (days > 0) {
                            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
                        } else if (hours % 24 > 0) {
                            return String.format("%dh %dm %ds", hours % 24, minutes % 60, seconds % 60);
                        } else {
                            return String.format("%dm %ds", minutes % 60, seconds % 60);
                        }
                    }
                    return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
                }
            } else if (action.equalsIgnoreCase("item") && parts.length >= 4) {
                int index;
                try {
                    index = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    return "";
                }

                List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);
                if (index >= 0 && index < activeItems.size()) {
                    StoreItem item = activeItems.get(index);
                    String property = parts[3];

                    switch (property.toLowerCase()) {
                        case "name":
                            if ("mmoitems".equalsIgnoreCase(item.type())) {
                                return item.mmoItemId();
                            } else if ("itemsadder".equalsIgnoreCase(item.type())) {
                                return item.itemsadderId();
                            } else {
                                return item.material();
                            }
                        case "price":
                            return String.valueOf(item.price());
                        case "purchases":
                            return String.valueOf(plugin.getDataManager().getPurchaseCount(poolName, item.id()));
                        case "max":
                            return String.valueOf(item.maxPurchases());
                        case "soldout":
                            boolean soldOut = item.maxPurchases() > 0 && plugin.getDataManager().getPurchaseCount(poolName, item.id()) >= item.maxPurchases();
                            return soldOut ? "yes" : "no";
                        case "type":
                            return item.type();
                    }
                }
            } else if (action.equalsIgnoreCase("pool")) {
                if (parts.length >= 3) {
                    int index;
                    try {
                        index = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        return "";
                    }
                    List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);
                    if (index >= 0 && index < activeItems.size()) {
                        StoreItem item = activeItems.get(index);
                        if (item.type().equals("mmoitems")) {
                            return item.mmoItemId();
                        } else if (item.type().equals("itemsadder")) {
                            return item.itemsadderId();
                        } else {
                            return item.material();
                        }
                    }
                }
            }
        } else if (parts.length == 1) {
            if (parts[0].equalsIgnoreCase("pool")) {
                return plugin.getConfigManager().getDefaultPool();
            }
        }
        
        return null;
    }
}
