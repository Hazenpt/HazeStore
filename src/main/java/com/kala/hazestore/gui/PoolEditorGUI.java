// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.gui;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import com.kala.hazestore.util.MaterialHelper;
import com.kala.hazestore.config.PoolConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
public class PoolEditorGUI implements Listener {
    private final Hazestore plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, ChatInputState> pendingChatInputs = new HashMap<>();

    public enum MenuState {
        MAIN_MENU,
        POOL_EDITOR,
        ITEM_EDITOR
    }

    public static class ChatInputState {
        private final String poolName;
        private final String itemId;
        private final String action;

        public ChatInputState(String poolName, String itemId, String action) {
            this.poolName = poolName;
            this.itemId = itemId;
            this.action = action;
        }

        public String getPoolName() { return poolName; }
        public String getItemId() { return itemId; }
        public String getAction() { return action; }
    }

    public PoolEditorGUI(Hazestore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private int getGuiSize(String poolName) {
        PoolConfig.CustomGuiConfig custom = plugin.getPoolConfig().getCustomGui(poolName);
        if (custom != null) return custom.getSize();
        return plugin.getGuiConfig().getSize();
    }

    public void openMainMenu(Player player) {
        EditorHolder holder = new EditorHolder(MenuState.MAIN_MENU, null, null, null, null);
        Inventory inv = Bukkit.createInventory(holder, 45, mm.deserialize("<gold>Hazestore Editor <gray>| <white>Pool List"));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        ItemStack addPool = new ItemStack(Material.LIME_WOOL);
        ItemMeta addMeta = addPool.getItemMeta();
        addMeta.displayName(mm.deserialize("<green>Crea Nuovo Pool").decoration(TextDecoration.ITALIC, false));
        addMeta.lore(List.of(mm.deserialize("<gray>Clicca per creare un nuovo pool!").decoration(TextDecoration.ITALIC, false)));
        addPool.setItemMeta(addMeta);
        inv.setItem(41, addPool);

        ItemStack refresh = new ItemStack(Material.ARROW);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.displayName(mm.deserialize("<yellow>Ricarica").decoration(TextDecoration.ITALIC, false));
        refresh.setItemMeta(refreshMeta);
        inv.setItem(39, refresh);

        List<String> pools = plugin.getPoolConfig().getPoolNames();
        String defaultPool = plugin.getConfigManager().getDefaultPool();
        for (int i = 0; i < Math.min(36, pools.size()); i++) {
            String poolName = pools.get(i);
            boolean isDefault = poolName.equalsIgnoreCase(defaultPool);
            ItemStack poolItem = new ItemStack(isDefault ? Material.ENDER_CHEST : Material.CHEST);
            ItemMeta poolMeta = poolItem.getItemMeta();
            poolMeta.displayName(mm.deserialize("<gold>" + poolName + (isDefault ? " <yellow>(Default)" : "")).decoration(TextDecoration.ITALIC, false));
            poolMeta.lore(List.of(
                mm.deserialize("<gray>Left Click: modifica pool").decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<gray>Right Click: imposta come default").decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<gray>Shift Click: forza rotazione pool").decoration(TextDecoration.ITALIC, false)
            ));
            poolItem.setItemMeta(poolMeta);
            inv.setItem(i, poolItem);
        }

        player.openInventory(inv);
    }

    private static final int EDITOR_ITEMS_PER_PAGE = 45;

    public void openPoolEditor(Player player, String poolName) {
        openPoolEditor(player, poolName, 0);
    }

    public void openPoolEditor(Player player, String poolName, int page) {
        List<StoreItem> poolItems = new ArrayList<>(plugin.getPoolConfig().getPool(poolName));
        poolItems.sort(Comparator.comparing(StoreItem::id, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil(poolItems.size() / (double) EDITOR_ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        EditorHolder holder = new EditorHolder(MenuState.POOL_EDITOR, poolName, null, null, String.valueOf(page));
        Inventory inv = Bukkit.createInventory(holder, 54,
                mm.deserialize("<gold>Hazestore Editor <gray>| <white>" + poolName + " <gray>(" + (page + 1) + "/" + totalPages + ")"));

        int start = page * EDITOR_ITEMS_PER_PAGE;
        for (int i = 0; i < EDITOR_ITEMS_PER_PAGE; i++) {
            int index = start + i;
            if (index >= poolItems.size()) break;
            inv.setItem(i, getDisplayItemForItem(poolItems.get(index)));
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        ItemStack backItem = new ItemStack(Material.RED_BED);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(mm.deserialize("<red>Indietro").decoration(TextDecoration.ITALIC, false));
        backItem.setItemMeta(backMeta);
        inv.setItem(45, backItem);

        ItemStack addItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta addMeta = addItem.getItemMeta();
        addMeta.displayName(mm.deserialize("<green>Aggiungi Item").decoration(TextDecoration.ITALIC, false));
        addMeta.lore(List.of(mm.deserialize("<gray>Clicca per creare un nuovo item nel pool").decoration(TextDecoration.ITALIC, false)));
        addItem.setItemMeta(addMeta);
        inv.setItem(49, addItem);

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(mm.deserialize("<yellow>Pagina precedente").decoration(TextDecoration.ITALIC, false));
            prev.setItemMeta(prevMeta);
            inv.setItem(48, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(mm.deserialize("<yellow>Pagina successiva").decoration(TextDecoration.ITALIC, false));
            next.setItemMeta(nextMeta);
            inv.setItem(50, next);
        }

        player.openInventory(inv);
    }

    public void openItemEditor(Player player, String poolName, String itemId) {
        EditorHolder holder = new EditorHolder(MenuState.ITEM_EDITOR, poolName, itemId, null, null);
        List<StoreItem> poolItems = plugin.getPoolConfig().getPool(poolName);
        StoreItem item = poolItems.stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst()
                .orElse(null);
        if (item == null) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Item non trovato!"));
            openPoolEditor(player, poolName);
            return;
        }

        Inventory inv = Bukkit.createInventory(holder, 45, mm.deserialize("<gold>Hazestore Editor <gray>| <white>Item: " + itemId));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        ItemStack back = new ItemStack(Material.RED_BED);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(mm.deserialize("<red>Indietro").decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        inv.setItem(36, back);

        ItemStack delete = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.displayName(mm.deserialize("<red>Elimina Item").decoration(TextDecoration.ITALIC, false));
        delete.setItemMeta(deleteMeta);
        inv.setItem(44, delete);

        ItemStack typeItem = new ItemStack(Material.ITEM_FRAME);
        ItemMeta typeMeta = typeItem.getItemMeta();
        typeMeta.displayName(mm.deserialize("<blue>Tipo: <white>" + item.type()).decoration(TextDecoration.ITALIC, false));
        typeMeta.lore(List.of(
                mm.deserialize("<gray>Clicca per cambiare tipo").decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<gray>Disponibili: vanilla, mmoitems, itemsadder").decoration(TextDecoration.ITALIC, false)
        ));
        typeItem.setItemMeta(typeMeta);
        inv.setItem(10, typeItem);

        ItemStack matItem = new ItemStack(Material.PAPER);
        ItemMeta matMeta = matItem.getItemMeta();
        String idText = "vanilla".equalsIgnoreCase(item.type()) ? item.material() :
                "itemsadder".equalsIgnoreCase(item.type()) ? item.itemsadderId() :
                        item.mmoItemType() + "/" + item.mmoItemId();
        matMeta.displayName(mm.deserialize("<blue>Item: <white>" + idText).decoration(TextDecoration.ITALIC, false));
        matMeta.lore(List.of(mm.deserialize("<gray>Clicca per cambiare").decoration(TextDecoration.ITALIC, false)));
        matItem.setItemMeta(matMeta);
        inv.setItem(12, matItem);

        ItemStack amountItem = new ItemStack(Material.COOKIE);
        ItemMeta amountMeta = amountItem.getItemMeta();
        amountMeta.displayName(mm.deserialize("<blue>Quantità: <white>" + item.amount()).decoration(TextDecoration.ITALIC, false));
        amountMeta.lore(List.of(mm.deserialize("<gray>Clicca per cambiare quantità").decoration(TextDecoration.ITALIC, false)));
        amountItem.setItemMeta(amountMeta);
        inv.setItem(14, amountItem);

        ItemStack priceItem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta priceMeta = priceItem.getItemMeta();
        priceMeta.displayName(mm.deserialize("<blue>Prezzo: <white>" + item.price()).decoration(TextDecoration.ITALIC, false));
        priceMeta.lore(List.of(mm.deserialize("<gray>Clicca per cambiare prezzo").decoration(TextDecoration.ITALIC, false)));
        priceItem.setItemMeta(priceMeta);
        inv.setItem(16, priceItem);

        ItemStack weightItem = new ItemStack(Material.FEATHER);
        ItemMeta weightMeta = weightItem.getItemMeta();
        weightMeta.displayName(mm.deserialize("<blue>Peso: <white>" + item.weight()).decoration(TextDecoration.ITALIC, false));
        weightMeta.lore(List.of(mm.deserialize("<gray>Clicca per cambiare peso (probabilità di apparizione)").decoration(TextDecoration.ITALIC, false)));
        weightItem.setItemMeta(weightMeta);
        inv.setItem(28, weightItem);

        ItemStack maxPurchasesItem = new ItemStack(Material.CLOCK);
        ItemMeta maxPurchasesMeta = maxPurchasesItem.getItemMeta();
        maxPurchasesMeta.displayName(mm.deserialize("<blue>Max Acquisti: <white>" + item.maxPurchases()).decoration(TextDecoration.ITALIC, false));
        maxPurchasesMeta.lore(List.of(mm.deserialize("<gray>Clicca per cambiare (-1 = infinito)").decoration(TextDecoration.ITALIC, false)));
        maxPurchasesItem.setItemMeta(maxPurchasesMeta);
        inv.setItem(30, maxPurchasesItem);

        ItemStack slotBtn = new ItemStack(Material.ANVIL);
        ItemMeta slotMeta = slotBtn.getItemMeta();
        slotMeta.displayName(mm.deserialize("<blue>Slot: <white>" + (item.slot() == -1 ? "Casuale" : item.slot())).decoration(TextDecoration.ITALIC, false));
        slotMeta.lore(List.of(mm.deserialize("<gray>Clicca per cambiare slot via chat").decoration(TextDecoration.ITALIC, false)));
        slotBtn.setItemMeta(slotMeta);
        inv.setItem(22, slotBtn);

        ItemStack vipItem = item.vip() ? new ItemStack(Material.LIME_WOOL) : new ItemStack(Material.RED_WOOL);
        ItemMeta vipMeta = vipItem.getItemMeta();
        vipMeta.displayName(mm.deserialize("<blue>VIP: <white>" + item.vip()).decoration(TextDecoration.ITALIC, false));
        vipItem.setItemMeta(vipMeta);
        inv.setItem(32, vipItem);

        ItemStack hiddenItem = item.hidden() ? new ItemStack(Material.LIME_WOOL) : new ItemStack(Material.RED_WOOL);
        ItemMeta hiddenMeta = hiddenItem.getItemMeta();
        hiddenMeta.displayName(mm.deserialize("<blue>Nascosto: <white>" + item.hidden()).decoration(TextDecoration.ITALIC, false));
        hiddenItem.setItemMeta(hiddenMeta);
        inv.setItem(34, hiddenItem);

        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameItem.getItemMeta();
        String shownName = (item.displayName() == null || item.displayName().isEmpty()) ? "<gray>(default)" : item.displayName();
        nameMeta.displayName(mm.deserialize("<blue>Nome visualizzato: <white>" + shownName).decoration(TextDecoration.ITALIC, false));
        nameMeta.lore(List.of(
                mm.deserialize("<gray>Clicca per cambiare (supporta MiniMessage)").decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<gray>Scrivi 'reset' per usare il nome di default").decoration(TextDecoration.ITALIC, false)
        ));
        nameItem.setItemMeta(nameMeta);
        inv.setItem(24, nameItem);

        ItemStack renameItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta renameMeta = renameItem.getItemMeta();
        renameMeta.displayName(mm.deserialize("<blue>ID (sistema): <white>" + item.id()).decoration(TextDecoration.ITALIC, false));
        renameMeta.lore(List.of(
                mm.deserialize("<gray>Clicca per rinominare l'ID interno dell'item").decoration(TextDecoration.ITALIC, false),
                mm.deserialize("<gray>Nota: azzera il conteggio acquisti dell'item").decoration(TextDecoration.ITALIC, false)
        ));
        renameItem.setItemMeta(renameMeta);
        inv.setItem(20, renameItem);

        player.openInventory(inv);
    }

    private void startChatEdit(Player player, String poolName, String itemId, String action, String prompt) {
        player.closeInventory();
        pendingChatInputs.put(player.getUniqueId(), new ChatInputState(poolName, itemId, action));
        player.sendMessage(mm.deserialize(prompt));
    }

    private int parsePage(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private ItemStack getDisplayItemForItem(StoreItem item) {
        ItemStack stack = null;
        if ("vanilla".equalsIgnoreCase(item.type())) {
            Material mat = MaterialHelper.getMaterial(item.material());
            stack = new ItemStack(mat, Math.min(64, Math.max(1, item.amount())));
        } else if ("mmoitems".equalsIgnoreCase(item.type())) {
            try {
                net.Indyuce.mmoitems.MMOItems mmoItems = net.Indyuce.mmoitems.MMOItems.plugin;
                net.Indyuce.mmoitems.api.Type mmoType = mmoItems.getTypes().get(item.mmoItemType().toUpperCase());
                if (mmoType != null) {
                    stack = mmoItems.getItem(mmoType, item.mmoItemId().toUpperCase());
                }
            } catch (Exception ignored) {}
        } else if ("itemsadder".equalsIgnoreCase(item.type())) {
            try {
                if (!plugin.isItemsAdderEnabled()) throw new Exception();
                Object customStack = Class.forName("dev.lone.itemsadder.api.CustomStack")
                        .getMethod("getInstance", String.class)
                        .invoke(null, item.itemsadderId());
                if (customStack != null) {
                    stack = (ItemStack) customStack.getClass().getMethod("getItemStack").invoke(customStack);
                }
            } catch (Exception ignored) {}
        }

        if (stack == null) {
            stack = new ItemStack(Material.BARRIER);
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(plugin, "item_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, item.id());
            meta.displayName(mm.deserialize("<gold>" + item.id()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Prezzo: <yellow>" + item.price() + " " + plugin.getConfigManager().getCurrency()).decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Quantità: <yellow>" + item.amount()).decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Peso: <yellow>" + item.weight()).decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Slot: <yellow>" + (item.slot() == -1 ? "Casuale" : item.slot())).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>VIP: " + (item.vip() ? "<green>Si" : "<red>No")).decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Nascosto: " + (item.hidden() ? "<green>Si" : "<red>No")).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Clicca per modificare").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
            return;
        }

        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof EditorHolder editorHolder)) return;

        event.setCancelled(true);
        MenuState state = editorHolder.getState();
        String poolName = editorHolder.getPoolName();
        String itemId = editorHolder.getItemId();

        if (state == MenuState.MAIN_MENU) {
            int slot = event.getSlot();
            if (slot == 41) {
                startChatEdit(player, null, null, "createpool",
                        "<green>Digita in chat il nome del nuovo pool (lettere, numeri, _ e -; scrivi 'cancel' per annullare):</green>");
            } else if (slot == 39) {
                plugin.getPoolConfig().load();
                openMainMenu(player);
            } else if (slot < 36) {
                List<String> pools = plugin.getPoolConfig().getPoolNames();
                if (slot < pools.size()) {
                    String clickedPool = pools.get(slot);
                    if (event.isLeftClick()) {
                        openPoolEditor(player, clickedPool);
                    } else if (event.isRightClick()) {
                        plugin.getConfig().set("default-pool", clickedPool);
                        plugin.saveConfig();
                        plugin.getConfigManager().load();
                        player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<green>Impostato pool di default su: " + clickedPool));
                        openMainMenu(player);
                    } else if (event.isShiftClick()) {
                        plugin.getItemPoolManager().rotatePool(clickedPool, true);
                        player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<green>Rotazione forzata per il pool: " + clickedPool));
                        openMainMenu(player);
                    }
                }
            }
        } else if (state == MenuState.POOL_EDITOR) {
            int slot = event.getSlot();
            int page = parsePage(editorHolder.getTempText());

            if (slot == 45) {
                openMainMenu(player);
            } else if (slot == 49) {
                startChatEdit(player, poolName, null, "createitem",
                        "<green>Digita l'ID del nuovo item (o scrivi 'cancel' per annullare):</green>");
            } else if (slot == 48) {
                openPoolEditor(player, poolName, page - 1);
            } else if (slot == 50) {
                openPoolEditor(player, poolName, page + 1);
            } else if (slot < 45) {
                ItemStack clickedStack = topInventory.getItem(slot);
                if (clickedStack != null && clickedStack.getItemMeta() != null) {
                    NamespacedKey key = new NamespacedKey(plugin, "item_id");
                    String storedItemId = clickedStack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                    if (storedItemId != null) {
                        openItemEditor(player, poolName, storedItemId);
                    }
                }
            }
        } else if (state == MenuState.ITEM_EDITOR) {
            int slot = event.getSlot();
            List<StoreItem> items = plugin.getPoolConfig().getPool(poolName);
            StoreItem item = items.stream()
                    .filter(i -> i.id().equals(itemId))
                    .findFirst()
                    .orElse(null);

            if (item == null) {
                openPoolEditor(player, poolName);
                return;
            }

            if (slot == 36) {
                openPoolEditor(player, poolName);
            } else if (slot == 44) {
                deleteItemFromPool(poolName, itemId);
                openPoolEditor(player, poolName);
            } else if (slot == 10) {
                String current = item.type() == null ? "vanilla" : item.type().toLowerCase();
                String next = switch (current) {
                    case "vanilla" -> "mmoitems";
                    case "mmoitems" -> "itemsadder";
                    default -> "vanilla";
                };
                saveEditedField(player, poolName, itemId, "type", next);
            } else if (slot == 12) {
                String prompt = "itemsadder".equalsIgnoreCase(item.type())
                        ? "<green>Digita l'ID ItemsAdder (es. namespace:item_id):</green>"
                        : "mmoitems".equalsIgnoreCase(item.type())
                        ? "<green>Digita l'item MMOItems nel formato TIPO/ID (es. SWORD/CUTLASS):</green>"
                        : "<green>Digita il materiale vanilla (es. DIAMOND):</green>";
                startChatEdit(player, poolName, itemId, "edit_item", prompt);
            } else if (slot == 14) {
                startChatEdit(player, poolName, itemId, "edit_amount", "<green>Digita la quantità (1-64):</green>");
            } else if (slot == 16) {
                startChatEdit(player, poolName, itemId, "edit_price", "<green>Digita il prezzo (es. 250 oppure 99.5):</green>");
            } else if (slot == 28) {
                startChatEdit(player, poolName, itemId, "edit_weight", "<green>Digita il peso (probabilità di apparizione, es. 100):</green>");
            } else if (slot == 30) {
                startChatEdit(player, poolName, itemId, "edit_maxpurchases", "<green>Digita il massimo acquisti (-1 = infinito):</green>");
            } else if (slot == 24) {
                startChatEdit(player, poolName, itemId, "edit_displayname",
                        "<green>Digita il nome visualizzato (MiniMessage, es. <gold>Spada Fica; 'reset' per default):</green>");
            } else if (slot == 20) {
                startChatEdit(player, poolName, itemId, "renameid",
                        "<green>Digita il nuovo ID interno (lettere, numeri, _ e -; 'cancel' per annullare):</green>");
            } else if (slot == 22) {
                player.closeInventory();
                int guiSize = getGuiSize(poolName);
                pendingChatInputs.put(player.getUniqueId(), new ChatInputState(poolName, itemId, "slot"));
                player.sendMessage(mm.deserialize("<green>Digita in chat il numero dello slot (da 0 a " + (guiSize - 1) + " o -1 per casuale):</green>"));
            } else if (slot == 32) {
                toggleItemField(poolName, itemId, "vip");
                openItemEditor(player, poolName, itemId);
            } else if (slot == 34) {
                toggleItemField(poolName, itemId, "hidden");
                openItemEditor(player, poolName, itemId);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ChatInputState state = pendingChatInputs.get(uuid);
        if (state == null) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            pendingChatInputs.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (state.getPoolName() == null) {
                    openMainMenu(player);
                } else if (state.getItemId() != null) {
                    openItemEditor(player, state.getPoolName(), state.getItemId());
                } else {
                    openPoolEditor(player, state.getPoolName());
                }
            });
            return;
        }

        pendingChatInputs.remove(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> {
            handleChatInput(player, state, message);
        });
    }

    private void handleChatInput(Player player, ChatInputState state, String input) {
        String poolName = state.getPoolName();
        String itemId = state.getItemId();
        String action = state.getAction();

        if (action.equalsIgnoreCase("slot")) {
            int guiSize = getGuiSize(poolName);
            try {
                int slotNum = Integer.parseInt(input);
                if (slotNum < -1 || slotNum >= guiSize) {
                    player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + 
                        "<red>Lo slot deve essere compreso tra -1 e " + (guiSize - 1) + "!</red>"));
                    pendingChatInputs.put(player.getUniqueId(), state);
                    player.sendMessage(mm.deserialize("<green>Digita in chat il numero dello slot (da 0 a " + (guiSize - 1) + " o -1 per casuale):</green>"));
                    return;
                }

                File file = new File(plugin.getDataFolder(), "pools/" + poolName + ".yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("items." + itemId + ".slot", slotNum);
                config.save(file);

                reloadAndRefresh(poolName);

                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<green>Slot impostato a: " + slotNum + "</green>"));
                openItemEditor(player, poolName, itemId);
            } catch (NumberFormatException e) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Numero non valido!</red>"));
                pendingChatInputs.put(player.getUniqueId(), state);
                player.sendMessage(mm.deserialize("<green>Digita in chat il numero dello slot (da 0 a " + (guiSize - 1) + " o -1 per casuale):</green>"));
            } catch (IOException e) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Errore durante il salvataggio!</red>"));
                openItemEditor(player, poolName, itemId);
            }
        } else if (action.equalsIgnoreCase("createitem")) {
            if (!input.matches("[a-zA-Z0-9_-]+")) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>ID item non valido! Usa solo lettere, numeri, _ e -.</red>"));
                openPoolEditor(player, poolName);
                return;
            }
            addNewItemToPoolWithSlot(player, poolName, input, -1);
        } else if (action.equalsIgnoreCase("createpool")) {
            if (!input.matches("[a-zA-Z0-9_-]+")) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Nome pool non valido! Usa solo lettere, numeri, _ e -.</red>"));
                openMainMenu(player);
                return;
            }
            if (plugin.getPoolConfig().getPoolNames().contains(input)) {
                player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Esiste già un pool con quel nome!</red>"));
                openMainMenu(player);
                return;
            }
            createNewPool(input);
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<green>Creato nuovo pool: " + input + "</green>"));
            openMainMenu(player);
        } else if (action.equalsIgnoreCase("renameid")) {
            renameItemId(player, poolName, itemId, input);
        } else if (action.startsWith("edit_")) {
            String field = action.substring(5);
            saveEditedField(player, poolName, itemId, field, input);
        }
    }

    private void renameItemId(Player player, String poolName, String oldId, String newId) {
        if (!newId.matches("[a-zA-Z0-9_-]+")) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>ID non valido! Usa solo lettere, numeri, _ e -.</red>"));
            openItemEditor(player, poolName, oldId);
            return;
        }
        if (newId.equals(oldId)) {
            openItemEditor(player, poolName, oldId);
            return;
        }

        File file = new File(plugin.getDataFolder(), "pools/" + poolName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        org.bukkit.configuration.ConfigurationSection oldSection = config.getConfigurationSection("items." + oldId);
        if (oldSection == null) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Item originale non trovato!</red>"));
            openPoolEditor(player, poolName);
            return;
        }
        if (config.contains("items." + newId)) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Esiste già un item con l'ID '" + newId + "'!</red>"));
            openItemEditor(player, poolName, oldId);
            return;
        }

        for (Map.Entry<String, Object> entry : oldSection.getValues(false).entrySet()) {
            config.set("items." + newId + "." + entry.getKey(), entry.getValue());
        }
        config.set("items." + oldId, null);

        try {
            config.save(file);
            reloadAndRefresh(poolName);
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<green>ID rinominato: " + oldId + " -> " + newId + "</green>"));
            openItemEditor(player, poolName, newId);
        } catch (IOException e) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Errore durante il salvataggio!</red>"));
            openItemEditor(player, poolName, oldId);
        }
    }

    private void addNewItemToPoolWithSlot(Player player, String poolName, String itemId, int slot) {
        File file = new File(plugin.getDataFolder(), "pools/" + poolName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "items." + itemId;

        if (config.contains(path)) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Esiste già un item con quell'ID!"));
            openPoolEditor(player, poolName);
            return;
        }

        config.set(path + ".type", "vanilla");
        config.set(path + ".material", "STONE");
        config.set(path + ".amount", 1);
        config.set(path + ".price", 10);
        config.set(path + ".weight", 100);
        config.set(path + ".vip", false);
        config.set(path + ".hidden", false);
        config.set(path + ".max-purchases", -1);
        config.set(path + ".slot", slot);

        try {
            config.save(file);
            reloadAndRefresh(poolName);
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<green>Creato nuovo oggetto: " + itemId + " allo slot: " + slot));
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile aggiungere item!");
        }

        openItemEditor(player, poolName, itemId);
    }

    private void createNewPool(String name) {
        File file = new File(plugin.getDataFolder(), "pools/" + name + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("items.example_item.type", "vanilla");
        config.set("items.example_item.material", "STONE");
        config.set("items.example_item.amount", 1);
        config.set("items.example_item.price", 10);
        config.set("items.example_item.weight", 100);
        config.set("items.example_item.vip", false);
        config.set("items.example_item.hidden", false);
        config.set("items.example_item.max-purchases", -1);
        config.set("items.example_item.slot", -1);

        try {
            config.save(file);
            reloadAndRefresh(name);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile creare pool!");
        }
    }

    private void saveEditedField(Player player, String poolName, String itemId, String field, String value) {
        File file = new File(plugin.getDataFolder(), "pools/" + poolName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "items." + itemId;

        try {
            switch (field) {
                case "type":
                    config.set(path + ".type", value);
                    if ("itemsadder".equalsIgnoreCase(value)) {
                        config.set(path + ".material", null);
                        config.set(path + ".mmoitems-id", null);
                        config.set(path + ".mmoitems-type", null);
                        config.set(path + ".mmoitems_id", null);
                        config.set(path + ".mmoitems_type", null);
                        config.set(path + ".itemsadder-id", "");
                    } else if ("mmoitems".equalsIgnoreCase(value)) {
                        config.set(path + ".material", null);
                        config.set(path + ".mmoitems-id", "");
                        config.set(path + ".mmoitems-type", "SWORD");
                        config.set(path + ".itemsadder-id", null);
                        config.set(path + ".itemsadder_id", null);
                    } else {
                        config.set(path + ".material", "STONE");
                        config.set(path + ".mmoitems-id", null);
                        config.set(path + ".mmoitems-type", null);
                        config.set(path + ".mmoitems_id", null);
                        config.set(path + ".mmoitems_type", null);
                        config.set(path + ".itemsadder-id", null);
                        config.set(path + ".itemsadder_id", null);
                    }
                    break;
                case "item":
                    String type = config.getString(path + ".type", "vanilla");
                    if ("itemsadder".equalsIgnoreCase(type)) {
                        config.set(path + ".itemsadder-id", value);
                    } else if ("mmoitems".equalsIgnoreCase(type)) {
                        String[] split = value.split("/", 2);
                        if (split.length == 2) {
                            config.set(path + ".mmoitems-type", split[0]);
                            config.set(path + ".mmoitems-id", split[1]);
                        }
                    } else {
                        config.set(path + ".material", value);
                    }
                    break;
                case "amount":
                    config.set(path + ".amount", Integer.parseInt(value));
                    break;
                case "price":
                    config.set(path + ".price", Double.parseDouble(value));
                    break;
                case "weight":
                    config.set(path + ".weight", Integer.parseInt(value));
                    break;
                case "maxpurchases":
                    config.set(path + ".max-purchases", Integer.parseInt(value));
                    break;
                case "displayname":
                    if (value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("clear") || value.equalsIgnoreCase("none")) {
                        config.set(path + ".display-name", null);
                    } else {
                        config.set(path + ".display-name", value);
                    }
                    break;
            }
            config.save(file);
            reloadAndRefresh(poolName);
        } catch (Exception e) {
            player.sendMessage(mm.deserialize(plugin.getConfigManager().getPrefix() + "<red>Valore non valido!"));
        }

        openItemEditor(player, poolName, itemId);
    }

    private void toggleItemField(String poolName, String itemId, String field) {
        File file = new File(plugin.getDataFolder(), "pools/" + poolName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "items." + itemId + "." + field;
        config.set(path, !config.getBoolean(path, false));

        try {
            config.save(file);
            reloadAndRefresh(poolName);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile salvare campo!");
        }
    }

    private void deleteItemFromPool(String poolName, String itemId) {
        File file = new File(plugin.getDataFolder(), "pools/" + poolName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("items." + itemId, null);

        try {
            config.save(file);
            reloadAndRefresh(poolName);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile eliminare item!");
        }
    }

    private void reloadAndRefresh(String poolName) {
        plugin.getPoolConfig().load();
        plugin.getItemPoolManager().refreshActiveItems(poolName);
        plugin.getGuiManager().buildCache(poolName);
        plugin.getGuiManager().refreshViewers(poolName);
    }
}
