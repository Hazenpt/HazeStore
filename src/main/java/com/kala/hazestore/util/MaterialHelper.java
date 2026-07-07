package com.kala.hazestore.util;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

// made with ❤️ by haze

public class MaterialHelper {

    private static final Map<String, String> FALLBACKS = new HashMap<>();
    private static final Material DEFAULT_MATERIAL = Material.BARRIER;
    private static Logger logger;

    static {
        FALLBACKS.put("MUD_BRICK_SLAB", "PACKED_MUD_BRICK_SLAB");
        FALLBACKS.put("MUD_BRICK_STAIRS", "PACKED_MUD_BRICK_STAIRS");
        FALLBACKS.put("MUD_BRICK_WALL", "PACKED_MUD_BRICK_WALL");
        FALLBACKS.put("MUD_BRICKS", "PACKED_MUD_BRICKS");
        FALLBACKS.put("POTTED_MANGROVE_PROPAGULE", "POTTED_MANGROVE_PROPAGULE");
        FALLBACKS.put("SHORT_GRASS", "GRASS");
        FALLBACKS.put("SCULK_VEIN", "SCULK_VEIN");
        FALLBACKS.put("OCHRE_FROGLIGHT", "OCHRE_FROGLIGHT");
        FALLBACKS.put("VERDANT_FROGLIGHT", "VERDANT_FROGLIGHT");
        FALLBACKS.put("PEARLESCENT_FROGLIGHT", "PEARLESCENT_FROGLIGHT");
    }

    public static void init(Logger pluginLogger) {
        logger = pluginLogger;
    }

    public static Material getMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            logger.warning("[HazeStore/Compat] Invalid material name: null/empty");
            return DEFAULT_MATERIAL;
        }

        String checkName = materialName.toUpperCase();
        String fallback = FALLBACKS.get(checkName);
        if (fallback != null) {
            logger.info("[HazeStore/Compat] Applying material fallback: " + checkName + " -> " + fallback);
            checkName = fallback;
        }

        Material material = Material.matchMaterial(checkName);
        if (material == null) {
            logger.warning("[HazeStore/Compat] Material not found: " + materialName + " (checked fallback: " + (fallback != null ? fallback : "none") + ") - using " + DEFAULT_MATERIAL + " as fallback");
            return DEFAULT_MATERIAL;
        }
        return material;
    }

    public static Material getMaterialOrDefault(String materialName, Material defaultMaterial) {
        Material material = getMaterial(materialName);
        return material == DEFAULT_MATERIAL ? defaultMaterial : material;
    }

    public static void addFallback(String oldName, String newName) {
        FALLBACKS.put(oldName.toUpperCase(), newName.toUpperCase());
    }
}
