// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.gui;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import com.kala.hazestore.util.MaterialHelper;
import com.kala.hazestore.util.HeadHelper;
import com.kala.hazestore.config.ConfirmationConfig;
import com.kala.hazestore.config.PoolConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class HazestoreGUI implements Listener {
    private final Hazestore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, List<ItemStack>> cachedItemsMap;

    public HazestoreGUI(Hazestore plugin) {
        this.plugin = plugin;
        this.cachedItemsMap = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private int getGuiSize(String poolName) {
        PoolConfig.CustomGuiConfig custom = plugin.getPoolConfig().getCustomGui(poolName);
        if (custom != null) return custom.getSize();
        return plugin.getGuiConfig().getSize();
    }

    private Component getGuiTitle(String poolName) {
        PoolConfig.CustomGuiConfig custom = plugin.getPoolConfig().getCustomGui(poolName);
        if (custom != null) return mm.deserialize(custom.getTitle());
        return mm.deserialize(plugin.getGuiConfig().getTitle());
    }

    private Map<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> getGuiSlots(String poolName) {
        PoolConfig.CustomGuiConfig custom = plugin.getPoolConfig().getCustomGui(poolName);
        if (custom != null) return custom.getSlots();
        return plugin.getGuiConfig().getSlots();
    }

    public void buildCache(String poolName) {
        List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);
        List<ItemStack> cachedItems = new ArrayList<>();
        String currency = plugin.getConfigManager().getCurrency();

        for (StoreItem sItem : activeItems) {
            ItemStack item = null;

            if ("mmoitems".equalsIgnoreCase(sItem.type())) {
                item = fetchMMOItem(sItem.mmoItemType(), sItem.mmoItemId());
                if (item == null || item.getType() == Material.AIR) {
                    plugin.getLogger().warning("Invalid MMOItem: " + sItem.mmoItemType() + "/" + sItem.mmoItemId());
                    cachedItems.add(null);
                    continue;
                }
            } else if ("itemsadder".equalsIgnoreCase(sItem.type())) {
                item = fetchItemsAdderItem(sItem.itemsadderId());
                if (item == null || item.getType() == Material.AIR) {
                    plugin.getLogger().warning("Invalid ItemsAdder item: " + sItem.itemsadderId());
                    cachedItems.add(null);
                    continue;
                }
            } else if ("vanilla".equalsIgnoreCase(sItem.type())) {
                Material mat = MaterialHelper.getMaterial(sItem.material());
                if (mat == Material.BARRIER && !sItem.material().equalsIgnoreCase("barrier")) {
                    plugin.getLogger().warning("Invalid vanilla material: " + sItem.material());
                    cachedItems.add(null);
                    continue;
                }
                item = new ItemStack(mat, sItem.amount());
            }

            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (sItem.displayName() != null && !sItem.displayName().isEmpty()) {
                        meta.displayName(mm.deserialize(sItem.displayName()).decoration(TextDecoration.ITALIC, false));
                    }
                    List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                    String priceLine = plugin.getGuiConfig().getLorePrice()
                            .replace("{price}", String.valueOf(sItem.price()))
                            .replace("{currency}", currency);
                    lore.add(mm.deserialize(priceLine).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.empty());
                    lore.add(mm.deserialize(plugin.getGuiConfig().getLorePurchaseHint()).decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
            }

            cachedItems.add(item);
        }

        cachedItemsMap.put(poolName, cachedItems);
    }

    public void open(Player player) {
        open(player, plugin.getConfigManager().getDefaultPool());
    }

    public void open(Player player, String poolName) {
        if (!cachedItemsMap.containsKey(poolName)) {
            buildCache(poolName);
        }

        long generation = plugin.getItemPoolManager().getPoolGeneration(poolName);
        StoreHolder holder = new StoreHolder(poolName, generation);

        Inventory inv = Bukkit.createInventory(holder, getGuiSize(poolName), getGuiTitle(poolName));

        fillBackground(inv, poolName);
        placePoolItems(inv, poolName, player);

        player.openInventory(inv);
        playOpenSound(player);
    }

    private void placePoolItems(Inventory inv, String poolName, Player player) {
        List<ItemStack> cachedItems = cachedItemsMap.getOrDefault(poolName, new ArrayList<>());
        List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);

        Map<Integer, StoreItem> slotToItem = buildSlotToItemMap(activeItems, poolName, inv.getSize());
        for (Map.Entry<Integer, StoreItem> entry : slotToItem.entrySet()) {
            int targetSlot = entry.getKey();
            StoreItem sItem = entry.getValue();
            int index = activeItems.indexOf(sItem);
            ItemStack item = getItemForPlayer(cachedItems, index, sItem, poolName, player);
            if (item != null) inv.setItem(targetSlot, item.clone());
        }
    }
    
    private ItemStack getItemForPlayer(List<ItemStack> cachedItems, int index, StoreItem sItem, String poolName, Player player) {
        ItemStack item = index < cachedItems.size() ? cachedItems.get(index) : null;
        if (item == null) return null;
        
        boolean hasAdminPermission = player.hasPermission("hs.admin");
        boolean hasVipPermission = player.hasPermission("hs.vip");
        boolean isRevealed = plugin.getDataManager().isItemRevealed(player.getUniqueId(), poolName, sItem.id());

        if (!hasAdminPermission) {
            if (!isRevealed && sItem.hidden()) {
                item = getHiddenItem();
            } else if (sItem.vip() && !hasVipPermission) {
                item = getVipItem();
            }
        }
        
        if (sItem.maxPurchases() > 0 && plugin.getDataManager().getPurchaseCount(poolName, sItem.id()) >= sItem.maxPurchases()) {
            item = getSoldOutItem();
        }
        return item;
    }

    private ItemStack buildConfigItem(String materialStr, String texture, String owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(MaterialHelper.getMaterial(materialStr));
        if (texture != null && !texture.isBlank()) {
            HeadHelper.applyTexture(item, texture);
        } else if (owner != null && !owner.isBlank()) {
            HeadHelper.applyOwner(item, owner);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComp = new ArrayList<>();
                for (String line : lore) {
                    loreComp.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(loreComp);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getHiddenItem() {
        return buildConfigItem(
                plugin.getConfigManager().getHiddenItemMaterial(),
                plugin.getConfigManager().getHiddenItemTexture(),
                plugin.getConfigManager().getHiddenItemOwner(),
                plugin.getConfigManager().getHiddenItemName(),
                plugin.getConfigManager().getHiddenItemLore());
    }

    private ItemStack getVipItem() {
        return buildConfigItem(
                plugin.getConfigManager().getVipItemMaterial(),
                plugin.getConfigManager().getVipItemTexture(),
                plugin.getConfigManager().getVipItemOwner(),
                plugin.getConfigManager().getVipItemName(),
                plugin.getConfigManager().getVipItemLore());
    }

    private ItemStack getSoldOutItem() {
        return buildConfigItem(
                plugin.getConfigManager().getSoldOutMaterial(),
                plugin.getConfigManager().getSoldOutTexture(),
                plugin.getConfigManager().getSoldOutOwner(),
                plugin.getConfigManager().getSoldOutName(),
                plugin.getConfigManager().getSoldOutLore());
    }

    private ItemStack getCachedStoreItem(String poolName, StoreItem sItem) {
        if (!cachedItemsMap.containsKey(poolName)) {
            buildCache(poolName);
        }
        List<StoreItem> active = plugin.getItemPoolManager().getActiveItems(poolName);
        List<ItemStack> cached = cachedItemsMap.get(poolName);
        if (cached == null) return null;
        for (int i = 0; i < active.size() && i < cached.size(); i++) {
            if (active.get(i) != null && active.get(i).id().equals(sItem.id())) {
                ItemStack cachedItem = cached.get(i);
                return cachedItem != null ? cachedItem.clone() : null;
            }
        }
        return null;
    }

    public void invalidateFiller() {
        cachedItemsMap.clear();
    }

    private void fillBackground(Inventory inv, String poolName) {
        for (Map.Entry<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> entry : getGuiSlots(poolName).entrySet()) {
            int slot = entry.getKey();
            com.kala.hazestore.config.GuiConfig.SlotConfig config = entry.getValue();

            if (slot >= inv.getSize()) continue;

            if (!config.type().equals("pool")) {
                Material mat = MaterialHelper.getMaterial(config.material());
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (config.name() != null && !config.name().isEmpty()) {
                        meta.displayName(mm.deserialize(config.name()).decoration(TextDecoration.ITALIC, false));
                    }
                    if (config.lore() != null && !config.lore().isEmpty()) {
                        List<Component> lore = new ArrayList<>();
                        for (String line : config.lore()) {
                            lore.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
                        }
                        meta.lore(lore);
                    }
                    for (ItemFlag flag : ItemFlag.values()) {
                        try { meta.addItemFlags(flag); } catch (Exception ignored) {}
                    }
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
            }
        }
    }

    private void playOpenSound(Player player) {
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(plugin.getConfigManager().getOpenSound());
            player.playSound(player.getLocation(), sound, plugin.getConfigManager().getOpenSoundVolume(), plugin.getConfigManager().getOpenSoundPitch());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid open sound in config: " + plugin.getConfigManager().getOpenSound());
        }
    }

    private ItemStack fetchMMOItem(String type, String id) {
        try {
            net.Indyuce.mmoitems.MMOItems mmoItems = net.Indyuce.mmoitems.MMOItems.plugin;
            net.Indyuce.mmoitems.api.Type mmoType = mmoItems.getTypes().get(type.toUpperCase());
            if (mmoType == null) {
                return null;
            }
            return mmoItems.getItem(mmoType, id.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack fetchItemsAdderItem(String id) {
        try {
            if (!plugin.isItemsAdderEnabled()) return null;
            Object customStack = Class.forName("dev.lone.itemsadder.api.CustomStack")
                    .getMethod("getInstance", String.class)
                    .invoke(null, id);
            if (customStack == null) return null;
            return (ItemStack) customStack.getClass().getMethod("getItemStack").invoke(customStack);
        } catch (Exception e) {
            return null;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (holder instanceof ConfirmationHolder confirmationHolder) {
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                event.setCancelled(true);
                handleConfirmationClick(event, player, confirmationHolder);
            }
            return;
        }

        if (holder instanceof StoreHolder storeHolder) {
            if (clickedInventory != null && clickedInventory.equals(topInventory)) {
                event.setCancelled(true);
                
                String poolName = storeHolder.getPoolName();
                if (!isValidViewer(player, poolName, storeHolder.getGeneration())) {
                    player.closeInventory();
                    return;
                }

                int slot = event.getSlot();

                List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);

                Map<Integer, StoreItem> slotToItem = buildSlotToItemMap(activeItems, poolName, getGuiSize(poolName));
                StoreItem sItem = slotToItem.get(slot);
                if (sItem == null) return;
                    
                    boolean hasAdminPermission = player.hasPermission("hs.admin");
                    boolean hasVipPermission = player.hasPermission("hs.vip");
                    boolean isRevealed = plugin.getDataManager().isItemRevealed(uuid, poolName, sItem.id());

                    if (!hasAdminPermission) {
                        if (!isRevealed && sItem.hidden()) {
                            plugin.getDataManager().revealItem(uuid, poolName, sItem.id());
                            try {
                                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(plugin.getConfigManager().getRevealSound());
                                player.playSound(player.getLocation(), sound, plugin.getConfigManager().getRevealSoundVolume(), plugin.getConfigManager().getRevealSoundPitch());
                            } catch (IllegalArgumentException ignored) {}
                            buildCache(poolName);
                            open(player, poolName);
                            return;
                        }
                        if (sItem.vip() && !hasVipPermission) return;
                    }
                    
                    if (sItem.maxPurchases() > 0 && plugin.getDataManager().getPurchaseCount(poolName, sItem.id()) >= sItem.maxPurchases()) return;

                    if (plugin.getConfirmationConfig().isEnabled()) {
                        openConfirmation(player, sItem, poolName);
                    } else {
                        handlePurchase(player, sItem, poolName);
                    }
            }
        }
    }

    private Map<Integer, StoreItem> buildSlotToItemMap(List<StoreItem> activeItems, String poolName, int invSize) {
        Map<Integer, StoreItem> map = new HashMap<>();

        Set<Integer> occupiedSlots = new HashSet<>();
        for (StoreItem sItem : activeItems) {
            if (sItem.slot() >= 0 && sItem.slot() < invSize) {
                map.put(sItem.slot(), sItem);
                occupiedSlots.add(sItem.slot());
            }
        }

        List<Integer> poolSlots = new ArrayList<>();
        for (Map.Entry<Integer, com.kala.hazestore.config.GuiConfig.SlotConfig> entry : getGuiSlots(poolName).entrySet()) {
            if (entry.getValue().type().equals("pool")) {
                poolSlots.add(entry.getKey());
            }
        }

        int poolSlotIdx = 0;
        for (StoreItem sItem : activeItems) {
            if (sItem.slot() == -1) {
                while (poolSlotIdx < poolSlots.size() && occupiedSlots.contains(poolSlots.get(poolSlotIdx))) {
                    poolSlotIdx++;
                }
                if (poolSlotIdx < poolSlots.size()) {
                    int targetSlot = poolSlots.get(poolSlotIdx);
                    map.put(targetSlot, sItem);
                    occupiedSlots.add(targetSlot);
                    poolSlotIdx++;
                }
            }
        }

        return map;
    }

    private void openConfirmation(Player player, StoreItem sItem, String poolName) {
        ConfirmationHolder holder = new ConfirmationHolder(sItem, poolName);
        ConfirmationConfig cc = plugin.getConfirmationConfig();
        Inventory inv = Bukkit.createInventory(holder, cc.getSize(), mm.deserialize(cc.getTitle()));

        ConfirmationConfig.SlotItem filler = cc.getFillerItem();
        ItemStack fillerItem = buildConfigItem(filler.getMaterial(), filler.getTexture(), filler.getOwner(), filler.getName(), filler.getLore());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, fillerItem.clone());
        }

        for (Map.Entry<Integer, ConfirmationConfig.SlotItem> entry : cc.getDecoSlots().entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= inv.getSize()) continue;
            ConfirmationConfig.SlotItem si = entry.getValue();
            inv.setItem(slot, buildConfigItem(si.getMaterial(), si.getTexture(), si.getOwner(), si.getName(), si.getLore()));
        }

        ConfirmationConfig.SlotItem confirm = cc.getConfirmItem();
        if (cc.getConfirmSlot() >= 0 && cc.getConfirmSlot() < inv.getSize()) {
            inv.setItem(cc.getConfirmSlot(), buildConfigItem(confirm.getMaterial(), confirm.getTexture(), confirm.getOwner(), confirm.getName(), confirm.getLore()));
        }

        ConfirmationConfig.SlotItem cancel = cc.getCancelItem();
        if (cc.getCancelSlot() >= 0 && cc.getCancelSlot() < inv.getSize()) {
            inv.setItem(cc.getCancelSlot(), buildConfigItem(cancel.getMaterial(), cancel.getTexture(), cancel.getOwner(), cancel.getName(), cancel.getLore()));
        }

        ItemStack info = getCachedStoreItem(poolName, sItem);
        if (info != null && cc.getInfoSlot() >= 0 && cc.getInfoSlot() < inv.getSize()) {
            inv.setItem(cc.getInfoSlot(), info);
        }

        player.openInventory(inv);
    }

    private void handleConfirmationClick(InventoryClickEvent event, Player player, ConfirmationHolder confirmationHolder) {
        int slot = event.getSlot();
        
        if (slot == plugin.getConfirmationConfig().getConfirmSlot()) {
            handlePurchase(player, confirmationHolder.getItem(), confirmationHolder.getPoolName());
        } else if (slot == plugin.getConfirmationConfig().getCancelSlot()) {
            open(player, confirmationHolder.getPoolName());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (holder instanceof StoreHolder || holder instanceof ConfirmationHolder) {
            for (int slot : event.getInventorySlots()) {
                if (topInventory.firstEmpty() != -1 && slot < topInventory.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean isValidViewer(Player player, String poolName, long generation) {
        return generation == plugin.getItemPoolManager().getPoolGeneration(poolName);
    }

    private void handlePurchase(Player player, StoreItem sItem, String poolName) {
        if (sItem.maxPurchases() > 0 && plugin.getDataManager().getPurchaseCount(poolName, sItem.id()) >= sItem.maxPurchases()) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getMessage("already-sold")));
            return;
        }

        String currencyId = plugin.getConfigManager().getCurrency();
        double price = sItem.price();
        double balance = getBalanceViaPlaceholder(player, currencyId);

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getMessage("inventory-full")));
            return;
        }

        if (balance < 0) {
            plugin.getLogger().warning("Could not read balance for currency '" + currencyId + "'. Make sure PlaceholderAPI and CoinsEngine are installed.");
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Could not read your balance. Contact an admin.</red>"));
            return;
        }

        if (balance < price) {
            String msg = plugin.getConfigManager().getMessage("not-enough-coins")
                    .replace("{currency}", currencyId)
                    .replace("{price}", String.valueOf(price));
            player.sendMessage(mm.deserialize(msg));
            return;
        }

        ItemStack item = null;
        if ("mmoitems".equalsIgnoreCase(sItem.type())) {
            item = fetchMMOItem(sItem.mmoItemType(), sItem.mmoItemId());
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Could not give the item. Invalid MMOItem.</red>"));
                return;
            }
        } else if ("itemsadder".equalsIgnoreCase(sItem.type())) {
            item = fetchItemsAdderItem(sItem.itemsadderId());
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Could not give the item. Invalid ItemsAdder item.</red>"));
                return;
            }
        } else if ("vanilla".equalsIgnoreCase(sItem.type())) {
            Material mat = MaterialHelper.getMaterial(sItem.material());
            if (mat == Material.BARRIER && !sItem.material().equalsIgnoreCase("barrier")) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Could not give the item. Invalid material.</red>"));
                return;
            }
            item = new ItemStack(mat, sItem.amount());
        }

        if (item == null) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Could not give the item.</red>"));
            return;
        }

        deductAndGive(player, sItem, item, currencyId, price, poolName);
    }

    private double getBalanceViaPlaceholder(Player player, String currency) {
        if (!plugin.isPlaceholderApiEnabled()) return -1;
        try {
            String rawPlaceholder = "%coinsengine_balance_raw_" + currency + "%";
            String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, rawPlaceholder);

            if (raw.equals(rawPlaceholder)) {
                raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%coinsengine_balance_" + currency + "%");
                if (raw.equals("%coinsengine_balance_" + currency + "%")) return -1;
            }

            return Double.parseDouble(raw.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void deductAndGive(Player player, StoreItem sItem, ItemStack item, String currencyId, double price, String poolName) {
        String takeCommand = currencyId + " take " + player.getName() + " " + price;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), takeCommand);

        player.getInventory().addItem(item);
        plugin.getDataManager().incrementPurchaseCount(poolName, sItem.id());
        plugin.getDataManager().saveData();

        buildCache(poolName);
        player.closeInventory();
        refreshViewers(poolName);

        String itemName = "mmoitems".equalsIgnoreCase(sItem.type()) ? sItem.mmoItemId() : 
                          "itemsadder".equalsIgnoreCase(sItem.type()) ? sItem.itemsadderId() : sItem.material();
        String msg = plugin.getConfigManager().getMessage("purchased")
                .replace("{currency}", currencyId)
                .replace("{price}", String.valueOf(price))
                .replace("{item}", itemName);
        player.sendMessage(mm.deserialize(msg));

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(plugin.getConfigManager().getPurchaseSound());
            player.playSound(player.getLocation(), sound, plugin.getConfigManager().getPurchaseSoundVolume(), plugin.getConfigManager().getPurchaseSoundPitch());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid purchase sound in config.");
        }

        plugin.getLogManager().logPurchase(player, itemName, price);
    }

    public void refreshViewers(String poolName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory topInv = player.getOpenInventory().getTopInventory();
            InventoryHolder holder = topInv != null ? topInv.getHolder() : null;
            if (holder instanceof StoreHolder storeHolder && storeHolder.getPoolName().equalsIgnoreCase(poolName)) {
                open(player, poolName);
            } else if (holder instanceof ConfirmationHolder confirmationHolder && confirmationHolder.getPoolName().equalsIgnoreCase(poolName)) {
                open(player, poolName);
            }
        }
    }

    public void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory topInv = player.getOpenInventory().getTopInventory();
            if (topInv != null && (topInv.getHolder() instanceof StoreHolder || topInv.getHolder() instanceof ConfirmationHolder)) {
                player.closeInventory();
            }
        }
    }
}
