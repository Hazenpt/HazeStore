// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final Hazestore plugin;
    private FileConfiguration config;

    private String prefix;
    private long rotationMillis;
    private String currency;
    private boolean broadcastEnabled;
    private boolean broadcastRotation;
    private boolean broadcastWarnings;
    private String defaultPool;

    private String hiddenItemMaterial;
    private String hiddenItemTexture;
    private String hiddenItemOwner;
    private String hiddenItemName;
    private List<String> hiddenItemLore;

    private String vipItemMaterial;
    private String vipItemTexture;
    private String vipItemOwner;
    private String vipItemName;
    private List<String> vipItemLore;

    private String soldOutMaterial;
    private String soldOutTexture;
    private String soldOutOwner;
    private String soldOutName;
    private List<String> soldOutLore;

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

    private String revealSound;
    private float revealSoundVolume;
    private float revealSoundPitch;

    public ConfigManager(Hazestore plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean load() {
        java.io.File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        boolean regenerated = false;
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            regenerated = true;
        }
        plugin.reloadConfig();
        config = plugin.getConfig();

        prefix = config.getString("prefix", "<gray>[<gold>HazeStore</gold>]</gray> ");
        rotationMillis = parseRotationString(config.getString("rotation", "1d 0h 0m"));
        currency = config.getString("currency", "gold");
        defaultPool = config.getString("default-pool", "default");
        broadcastEnabled = config.getBoolean("broadcast.enabled", true);
        broadcastRotation = config.getBoolean("broadcast.rotation", true);
        broadcastWarnings = config.getBoolean("broadcast.warnings", true);
        loadHiddenAndSoldOut();
        loadMessagesAndWarnings();
        loadSounds();
        return regenerated;
    }

    private void loadHiddenAndSoldOut() {
        hiddenItemMaterial = config.getString("hidden-item.material", config.getString("hidden-item-material", "BARRIER"));
        hiddenItemTexture = config.getString("hidden-item.texture", "");
        hiddenItemOwner = config.getString("hidden-item.owner", "");
        hiddenItemName = config.getString("hidden-item.name", config.getString("hidden-item-name", "<gray>Item segreto</gray>"));
        hiddenItemLore = config.getStringList("hidden-item.lore");

        vipItemMaterial = config.getString("vip-item.material", "BARRIER");
        vipItemTexture = config.getString("vip-item.texture", "");
        vipItemOwner = config.getString("vip-item.owner", "");
        vipItemName = config.getString("vip-item.name", "<gold>VIP item</gold>");
        vipItemLore = config.getStringList("vip-item.lore");

        soldOutMaterial = config.getString("sold-out-item.material", "BARRIER");
        soldOutTexture = config.getString("sold-out-item.texture", "");
        soldOutOwner = config.getString("sold-out-item.owner", "");
        soldOutName = config.getString("sold-out-item.name", "<red>Esaurito</red>");
        soldOutLore = config.getStringList("sold-out-item.lore");
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

        revealSound = config.getString("sounds.hidden-reveal.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        revealSoundVolume = (float) config.getDouble("sounds.hidden-reveal.volume", 1.0);
        revealSoundPitch = (float) config.getDouble("sounds.hidden-reveal.pitch", 1.2);
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
    public String getDefaultPool() { return defaultPool; }
    public boolean isBroadcastEnabled() { return broadcastEnabled; }
    public boolean isBroadcastRotation() { return broadcastRotation; }
    public boolean isBroadcastWarnings() { return broadcastWarnings; }
    public String getHiddenItemMaterial() { return hiddenItemMaterial; }
    public String getHiddenItemTexture() { return hiddenItemTexture; }
    public String getHiddenItemOwner() { return hiddenItemOwner; }
    public String getHiddenItemName() { return hiddenItemName; }
    public List<String> getHiddenItemLore() { return hiddenItemLore; }

    public String getVipItemMaterial() { return vipItemMaterial; }
    public String getVipItemTexture() { return vipItemTexture; }
    public String getVipItemOwner() { return vipItemOwner; }
    public String getVipItemName() { return vipItemName; }
    public List<String> getVipItemLore() { return vipItemLore; }

    public String getSoldOutMaterial() { return soldOutMaterial; }
    public String getSoldOutTexture() { return soldOutTexture; }
    public String getSoldOutOwner() { return soldOutOwner; }
    public String getSoldOutName() { return soldOutName; }
    public List<String> getSoldOutLore() { return soldOutLore; }
    
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

    public String getRevealSound() { return revealSound; }
    public float getRevealSoundVolume() { return revealSoundVolume; }
    public float getRevealSoundPitch() { return revealSoundPitch; }
}
