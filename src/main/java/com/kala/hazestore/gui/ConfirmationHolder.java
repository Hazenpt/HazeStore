// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.gui;

import com.kala.hazestore.model.StoreItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ConfirmationHolder implements InventoryHolder {
    private final StoreItem item;
    private final String poolName;

    public ConfirmationHolder(StoreItem item, String poolName) {
        this.item = item;
        this.poolName = poolName;
    }

    public StoreItem getItem() {
        return item;
    }

    public String getPoolName() {
        return poolName;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
