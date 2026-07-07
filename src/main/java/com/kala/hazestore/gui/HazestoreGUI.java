package com.kala.hazestore.gui;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import com.kala.hazestore.util.MaterialHelper;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// made with ❤️ by haze

public class HazestoreGUI implements Listener {
    private final Hazestore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final List<ItemStack> cachedItems = new ArrayList<>();
    private ItemStack cachedFiller;
    private Component cachedTitle;
    private final Set<UUID> openViewers = new HashSet<>();
    private final Map<UUID, Long> viewerGenerations = new HashMap<>();
    private Map<Integer, Integer> slotToIndex = Map.of();

    public HazestoreGUI(Hazestore plugin) {
        this.plugin = plugin;
        rebuildSlotIndex();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String getTitle() {
        return plugin.getConfigManager().getGuiTitle();
    }

    private Component getTitleComponent() {
        if (cachedTitle == null) {
            cachedTitle = mm.deserialize(getTitle());
        }
        return cachedTitle;
    }

    private void rebuildSlotIndex() {
        List<Integer> slots = plugin.getConfigManager().getGuiSlots();
        Map<Integer, Integer> index = new HashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            index.put(slots.get(i), i);
        }
        slotToIndex = index;
    }

    public void buildCache(List<StoreItem> activeItems) {
        cachedItems.clear();
        cachedFiller = null;

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
                    List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                    String priceLine = plugin.getConfigManager().getGuiLorePrice()
                            .replace("{price}", String.valueOf(sItem.price()))
                            .replace("{currency}", currency);
                    lore.add(mm.deserialize(priceLine).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.empty());
                    lore.add(mm.deserialize(plugin.getConfigManager().getGuiLorePurchaseHint()).decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
            }

            cachedItems.add(item);
        }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitleComponent());

        fillBackground(inv);

        List<Integer> slots = plugin.getConfigManager().getGuiSlots();
        for (int i = 0; i < Math.min(cachedItems.size(), slots.size()); i++) {
            ItemStack item = cachedItems.get(i);
            if (item != null) {
                inv.setItem(slots.get(i), item.clone());
            }
        }

        long generation = plugin.getItemPoolManager().getPoolGeneration();
        viewerGenerations.put(player.getUniqueId(), generation);
        openViewers.add(player.getUniqueId());
        player.openInventory(inv);
        playOpenSound(player);
    }

    private void fillBackground(Inventory inv) {
        if (cachedFiller == null) {
            cachedFiller = buildFiller();
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, cachedFiller.clone());
        }
    }

    private ItemStack buildFiller() {
        Material fillerMaterial = MaterialHelper.getMaterial(plugin.getConfigManager().getFillerMaterial());

        ItemStack glass = new ItemStack(fillerMaterial);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            String nameStr = plugin.getConfigManager().getFillerName();
            Component nameComponent = (nameStr != null && !nameStr.isBlank())
                    ? mm.deserialize(nameStr).decoration(TextDecoration.ITALIC, false)
                    : Component.empty();

            meta.displayName(nameComponent);
            for (ItemFlag flag : ItemFlag.values()) {
                try { meta.addItemFlags(flag); } catch (Exception ignored) {}
            }
            try {
                java.lang.reflect.Method method = ItemMeta.class.getMethod("setHideTooltip", boolean.class);
                method.invoke(meta, true);
            } catch (Exception ignored) {}

            glass.setItemMeta(meta);
        }
        return glass;
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
            Type mmoType = MMOItems.plugin.getTypes().get(type.toUpperCase());
            if (mmoType == null) {
                plugin.getLogger().warning("MMOItem type not found: " + type);
                return null;
            }
            return MMOItems.plugin.getItem(mmoType, id.toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not retrieve MMOItem " + type + "/" + id + ": " + e.getMessage());
            return null;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(getTitleComponent())) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) return;

        if (!isValidViewer(player)) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();
        Integer index = slotToIndex.get(slot);
        if (index == null) return;

        List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems();
        if (index < activeItems.size()) {
            handlePurchase(player, activeItems.get(index));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().title().equals(getTitleComponent())) return;
        UUID id = player.getUniqueId();
        openViewers.remove(id);
        viewerGenerations.remove(id);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().title().equals(getTitleComponent())) {
            event.setCancelled(true);
        }
    }

    private boolean isValidViewer(Player player) {
        Long openedGen = viewerGenerations.get(player.getUniqueId());
        return openedGen != null && openedGen == plugin.getItemPoolManager().getPoolGeneration();
    }

    private void handlePurchase(Player player, StoreItem sItem) {
        if (!isValidViewer(player)) {
            player.closeInventory();
            return;
        }

        String currencyId = plugin.getConfigManager().getCurrency();
        double price = sItem.price();
        String balancePlaceholder = "%coinsengine_balance_" + currencyId + "%";
        double balance = getBalanceViaPlaceholder(player, balancePlaceholder);

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getMessage("inventory-full")));
            return;
        }

        ItemStack item = null;

        if ("mmoitems".equalsIgnoreCase(sItem.type())) {
            item = fetchMMOItem(sItem.mmoItemType(), sItem.mmoItemId());
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Could not give the item. Invalid MMOItem.</red>"));
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

        deductAndGive(player, sItem, item, currencyId, price);
    }

    private double getBalanceViaPlaceholder(Player player, String placeholder) {
        if (!plugin.isPlaceholderApiEnabled()) return -1;
        try {
            String rawPlaceholder = placeholder.replace("%coinsengine_balance_", "%coinsengine_balance_raw_");
            String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, rawPlaceholder);

            if (raw.equals(rawPlaceholder)) {
                raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
                if (raw.equals(placeholder)) return -1;
            }

            return Double.parseDouble(raw.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void deductAndGive(Player player, StoreItem sItem, ItemStack mmoItem, String currencyId, double price) {
        String takeCommand = currencyId + " take " + player.getName() + " " + price;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), takeCommand);

        player.getInventory().addItem(mmoItem);
        String itemName = "mmoitems".equalsIgnoreCase(sItem.type()) ? sItem.mmoItemId() : sItem.material();
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
        player.closeInventory();
    }

    public void closeAll() {
        for (UUID id : new HashSet<>(openViewers)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.closeInventory();
            }
        }
        openViewers.clear();
        viewerGenerations.clear();
    }

    public void invalidateFiller() {
        cachedFiller = null;
        cachedTitle = null;
        rebuildSlotIndex();
    }
}
