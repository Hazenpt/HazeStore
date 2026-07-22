// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
package com.kala.hazestore.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * ============================ HOW TO SET A HEAD ============================
 * To use a player head in any configurable section (hidden-item, sold-out-item,
 * confirmation, etc.) set the following in the config:
 *
 *   material: "PLAYER_HEAD"
 *   texture: "<BASE64_VALUE>"       # custom head (skin from a URL)
 *   # or, as an alternative to texture:
 *   owner: "<PlayerName>"           # a real player's head
 *
 * Where to find the BASE64_VALUE:
 *   - On sites like https://minecraft-heads.com open a head and copy the
 *     "Value" field (the long string). That is the base64 to paste into "texture".
 *   - The base64 encodes a JSON like:
 *       {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/...."}}}
 *
 * If "material" is not PLAYER_HEAD, texture/owner are ignored.
 * ==========================================================================
 */
public final class HeadHelper {

    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"(.*?)\"");

    private HeadHelper() {}

    public static void applyTexture(ItemStack head, String base64) {
        if (base64 == null || base64.isBlank()) return;
        if (head == null || !(head.getItemMeta() instanceof SkullMeta meta)) return;
        try {
            String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(decoded);
            if (!matcher.find()) return;
            URL url = URI.create(matcher.group(1)).toURL();

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(url);
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("deprecation")
    public static void applyOwner(ItemStack head, String playerName) {
        if (playerName == null || playerName.isBlank()) return;
        if (head == null || !(head.getItemMeta() instanceof SkullMeta meta)) return;
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
            head.setItemMeta(meta);
        } catch (Exception ignored) {}
    }
}
