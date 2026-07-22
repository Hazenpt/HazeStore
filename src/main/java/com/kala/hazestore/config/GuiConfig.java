// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiConfig {
    private final Hazestore plugin;
    private File guiFile;
    private FileConfiguration guiConfig;

    private String title;
    private int size;
    private Map<Integer, SlotConfig> slots;
    private String lorePrice;
    private String lorePurchaseHint;

    public record SlotConfig(
            String type,
            String material,
            String name,
            List<String> lore
    ) {}

    public GuiConfig(Hazestore plugin) {
        this.plugin = plugin;
        this.slots = new HashMap<>();
        load();
    }

    public boolean load() {
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        boolean regenerated = false;

        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
            regenerated = true;
        }

        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        loadGuiSettings();
        loadSlots();
        return regenerated;
    }

    private void loadGuiSettings() {
        title = guiConfig.getString("title", "<b><color:#2B2B2B><b>haze store</b></color></b>");
        size = guiConfig.getInt("size", 54);
        lorePrice = guiConfig.getString("lore.price", "<gold>Price: </gold><yellow>{price} {currency}</yellow>");
        lorePurchaseHint = guiConfig.getString("lore.purchase-hint", "<gray>Click to <green>purchase</green>!</gray>");
    }

    private void loadSlots() {
        slots.clear();
        ConfigurationSection slotsSection = guiConfig.getConfigurationSection("slots");
        if (slotsSection == null) return;

        for (String key : slotsSection.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                if (slot < 0 || slot >= size) {
                    plugin.getLogger().warning("Slot " + slot + " in gui.yml is invalid for inventory size " + size + " (slots must be between 0 and " + (size -1) + ")");
                    continue;
                }
                
                ConfigurationSection slotSec = slotsSection.getConfigurationSection(key);
                if (slotSec != null) {
                    String type = slotSec.getString("type", "filler");
                    String material = slotSec.getString("material", "BLACK_STAINED_GLASS_PANE");
                    String name = slotSec.getString("name", " ");
                    List<String> lore = slotSec.getStringList("lore");
                    slots.put(slot, new SlotConfig(type, material, name, lore));
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    public String getTitle() { return title; }
    public int getSize() { return size; }
    public Map<Integer, SlotConfig> getSlots() { return slots; }
    public String getLorePrice() { return lorePrice; }
    public String getLorePurchaseHint() { return lorePurchaseHint; }
}
