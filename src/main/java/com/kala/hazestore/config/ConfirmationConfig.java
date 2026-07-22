// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfirmationConfig {
    private final Hazestore plugin;
    private File confirmFile;
    private FileConfiguration confirmConfig;

    private boolean enabled;
    private String title;
    private int size;

    private int confirmSlot;
    private SlotItem confirmItem;
    private int cancelSlot;
    private SlotItem cancelItem;
    private int infoSlot;

    private SlotItem fillerItem;
    private final Map<Integer, SlotItem> decoSlots = new HashMap<>();

    public static class SlotItem {
        private final String material;
        private final String texture;
        private final String owner;
        private final String name;
        private final List<String> lore;

        public SlotItem(String material, String texture, String owner, String name, List<String> lore) {
            this.material = material;
            this.texture = texture;
            this.owner = owner;
            this.name = name;
            this.lore = lore;
        }

        public String getMaterial() { return material; }
        public String getTexture() { return texture; }
        public String getOwner() { return owner; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
    }

    public ConfirmationConfig(Hazestore plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean load() {
        confirmFile = new File(plugin.getDataFolder(), "confirmation.yml");
        boolean regenerated = false;

        if (!confirmFile.exists()) {
            plugin.saveResource("confirmation.yml", false);
            regenerated = true;
        }

        confirmConfig = YamlConfiguration.loadConfiguration(confirmFile);
        loadSettings();
        return regenerated;
    }

    private void loadSettings() {
        decoSlots.clear();

        enabled = confirmConfig.getBoolean("enabled", true);
        title = confirmConfig.getString("title", "<gold>Confirm Purchase</gold>");
        size = confirmConfig.getInt("size", 27);

        confirmSlot = confirmConfig.getInt("confirm.slot", 11);
        confirmItem = readSlotItem(confirmConfig.getConfigurationSection("confirm"), "LIME_WOOL", "<green>Confirm</green>");
        cancelSlot = confirmConfig.getInt("cancel.slot", 15);
        cancelItem = readSlotItem(confirmConfig.getConfigurationSection("cancel"), "RED_WOOL", "<red>Cancel</red>");
        infoSlot = confirmConfig.getInt("info.slot", 13);

        fillerItem = readSlotItem(confirmConfig.getConfigurationSection("filler"), "GRAY_STAINED_GLASS_PANE", " ");

        ConfigurationSection slotsSection = confirmConfig.getConfigurationSection("slots");
        if (slotsSection != null) {
            for (String key : slotsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    SlotItem item = readSlotItem(slotsSection.getConfigurationSection(key), "BLACK_STAINED_GLASS_PANE", " ");
                    decoSlots.put(slot, item);
                } catch (NumberFormatException ignored) {}
            }
        }

        validateSlot("confirm", confirmSlot);
        validateSlot("cancel", cancelSlot);
        validateSlot("info", infoSlot);
    }

    private SlotItem readSlotItem(ConfigurationSection section, String defaultMaterial, String defaultName) {
        if (section == null) {
            return new SlotItem(defaultMaterial, "", "", defaultName, new ArrayList<>());
        }
        String material = section.getString("material", defaultMaterial);
        String texture = section.getString("texture", "");
        String owner = section.getString("owner", "");
        String name = section.getString("name", defaultName);
        List<String> lore = section.getStringList("lore");
        return new SlotItem(material, texture, owner, name, lore);
    }

    private void validateSlot(String slotName, int slot) {
        if (slot < 0 || slot >= size) {
            plugin.getLogger().warning("Invalid " + slotName + " slot " + slot + " in confirmation.yml (must be between 0 and " + (size - 1) + ")");
        }
    }

    public boolean isEnabled() { return enabled; }
    public String getTitle() { return title; }
    public int getSize() { return size; }

    public int getConfirmSlot() { return confirmSlot; }
    public SlotItem getConfirmItem() { return confirmItem; }
    public int getCancelSlot() { return cancelSlot; }
    public SlotItem getCancelItem() { return cancelItem; }
    public int getInfoSlot() { return infoSlot; }

    public SlotItem getFillerItem() { return fillerItem; }
    public Map<Integer, SlotItem> getDecoSlots() { return decoSlots; }
}
