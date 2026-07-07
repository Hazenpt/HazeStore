package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import com.kala.hazestore.model.StoreItem;
import com.kala.hazestore.util.MaterialHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// made with ❤️ by haze

public class ConfigManager {
    private final Hazestore plugin;
    private FileConfiguration config;

    private String prefix;
    private long rotationMillis;
    private String currency;

    private String guiTitle;
    private List<Integer> guiSlots;
    private String fillerMaterial;
    private String fillerName;
    private String guiLorePrice;
    private String guiLorePurchaseHint;

    private final Map<String, String> messages = new HashMap<>();
    private final Map<Integer, String> warnings = new HashMap<>();

    private String openSound;
    private float openSoundVolume;
    private float openSoundPitch;

    private String purchaseSound;
    private float purchaseSoundVolume;
    private float purchaseSoundPitch;

    private String forceRotateSound;
    private float forceRotateSoundVolume;
    private float forceRotateSoundPitch;

    private final List<StoreItem> pool = new ArrayList<>();

    public ConfigManager(Hazestore plugin) {
        this.plugin = plugin;
        load();
    }


    public boolean load() {
        java.io.File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        boolean regenerated = false;
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getLogger().warning("Config file was missing! A new default config.yml has been generated.");
            regenerated = true;
        }
        plugin.reloadConfig();
        config = plugin.getConfig();

        prefix = config.getString("prefix", "<gray>[<gold>HazeStore</gold>]</gray> ");
        rotationMillis = parseRotationString(config.getString("rotation", "1d 0h 0m"));
        currency = config.getString("currency", "gold");

        guiTitle = config.getString("gui.title", "<dark_gray>The Mysterious Man</dark_gray>");
        guiSlots = config.getIntegerList("gui.slots");
        if (guiSlots.isEmpty()) {
            guiSlots = List.of(21, 22, 23);
        }

        fillerMaterial = config.getString("gui.filler.material", "BLACK_STAINED_GLASS_PANE");
        fillerName = config.getString("gui.filler.name", " ");
        guiLorePrice = config.getString("gui.lore.price", "<gold>Price: </gold><yellow>{price} {currency}</yellow>");
        guiLorePurchaseHint = config.getString("gui.lore.purchase-hint", "<gray>Click to <green>purchase</green>!</gray>");

        loadMessagesAndWarnings();
        loadSounds();
        loadPool();
        return regenerated;
    }

    private void loadMessagesAndWarnings() {
        messages.clear();
        warnings.clear();
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection == null) return;

        for (String key : msgSection.getKeys(false)) {
            if (!key.equals("warnings")) {
                messages.put(key, msgSection.getString(key));
            }
        }

        ConfigurationSection warnSection = msgSection.getConfigurationSection("warnings");
        if (warnSection != null) {
            for (String key : warnSection.getKeys(false)) {
                try {
                    warnings.put(Integer.parseInt(key), warnSection.getString(key));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void loadSounds() {
        openSound = config.getString("sounds.gui-open.sound", "BLOCK_CHEST_OPEN");
        openSoundVolume = (float) config.getDouble("sounds.gui-open.volume", 1.0);
        openSoundPitch = (float) config.getDouble("sounds.gui-open.pitch", 1.0);

        purchaseSound = config.getString("sounds.purchase.sound", "ENTITY_PLAYER_LEVELUP");
        purchaseSoundVolume = (float) config.getDouble("sounds.purchase.volume", 1.0);
        purchaseSoundPitch = (float) config.getDouble("sounds.purchase.pitch", 1.0);

        forceRotateSound = config.getString("sounds.force-rotate.sound", "BLOCK_NOTE_BLOCK_PLING");
        forceRotateSoundVolume = (float) config.getDouble("sounds.force-rotate.volume", 1.0);
        forceRotateSoundPitch = (float) config.getDouble("sounds.force-rotate.pitch", 1.0);
    }

    private void loadPool() {
        pool.clear();
        ConfigurationSection poolSection = config.getConfigurationSection("pool");
        if (poolSection == null) return;

        for (String key : poolSection.getKeys(false)) {
            ConfigurationSection itemSec = poolSection.getConfigurationSection(key);
            if (itemSec == null) continue;

            String itemType = itemSec.getString("type", "mmoitems");
            String mmoType = itemSec.getString("mmoitems_type");
            String mmoId = itemSec.getString("mmoitems_id");
            String material = itemSec.getString("material");
            int amount = itemSec.getInt("amount", 1);
            double price = itemSec.getDouble("price", 0);
            int weight = itemSec.getInt("weight", 10);

            if ("mmoitems".equalsIgnoreCase(itemType) && mmoType != null && mmoId != null) {
                if (plugin.isMmoItemsEnabled()) {
                    pool.add(new StoreItem(key, itemType, mmoType, mmoId, null, 1, price, weight));
                }
            } else if ("vanilla".equalsIgnoreCase(itemType) && material != null) {
                pool.add(new StoreItem(key, itemType, null, null, material, amount, price, weight));
            }
        }
    }

    private long parseRotationString(String input) {
        if (input == null || input.isBlank()) return 86400000L;
        
        long totalMillis = 0;
        for (String part : input.toLowerCase().split(" ")) {
            try {
                if (part.endsWith("d")) totalMillis += Long.parseLong(part.replace("d", "")) * 86400000L;
                else if (part.endsWith("h")) totalMillis += Long.parseLong(part.replace("h", "")) * 3600000L;
                else if (part.endsWith("min") || part.endsWith("m")) totalMillis += Long.parseLong(part.replace("min", "").replace("m", "")) * 60000L;
            } catch (NumberFormatException ignored) {}
        }
        return totalMillis > 0 ? totalMillis : 86400000L;
    }

    public String getMessage(String key) { return prefix + messages.getOrDefault(key, "<red>Message not found: " + key + "</red>"); }
    public String getRawMessage(String key) { return messages.getOrDefault(key, ""); }
    
    public String getPrefix() { return prefix; }
    public long getRotationMillis() { return rotationMillis; }
    public String getCurrency() { return currency; }
    
    public String getGuiTitle() { return guiTitle; }
    public List<Integer> getGuiSlots() { return guiSlots; }
    public String getFillerMaterial() { return fillerMaterial; }
    public String getFillerName() { return fillerName; }
    public String getGuiLorePrice() { return guiLorePrice; }
    public String getGuiLorePurchaseHint() { return guiLorePurchaseHint; }
    
    public Map<Integer, String> getWarnings() { return warnings; }
    
    public String getOpenSound() { return openSound; }
    public float getOpenSoundVolume() { return openSoundVolume; }
    public float getOpenSoundPitch() { return openSoundPitch; }
    
    public String getPurchaseSound() { return purchaseSound; }
    public float getPurchaseSoundVolume() { return purchaseSoundVolume; }
    public float getPurchaseSoundPitch() { return purchaseSoundPitch; }

    public String getForceRotateSound() { return forceRotateSound; }
    public float getForceRotateSoundVolume() { return forceRotateSoundVolume; }
    public float getForceRotateSoundPitch() { return forceRotateSoundPitch; }
    
    public List<StoreItem> getPool() { return pool; }
}
