package com.kala.hazestore.command;

import com.kala.hazestore.Hazestore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// made with ❤️ by haze

public class HazestoreCommand implements CommandExecutor, TabCompleter {
    private final Hazestore plugin;

    public HazestoreCommand(Hazestore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            return handleOpen(sender, args);
        }

        if (args[0].equalsIgnoreCase("admin")) {
            return handleAdmin(sender, args);
        }

        return true;
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (args.length >= 2 && sender.hasPermission("hs.admin")) {
            Player target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(MiniMessage.miniMessage()
                        .deserialize(plugin.getConfigManager().getPrefix() + "<red>Player not found.</red>"));
                return true;
            }
            plugin.getGuiManager().open(target);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the store or specify a target player.");
            return true;
        }

        if (!player.hasPermission("hs.open")) {
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        plugin.getGuiManager().open(player);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hs.admin")) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("reload")) {
                boolean configRegen = plugin.getConfigManager().load();
                boolean dataRegen = plugin.getDataManager().reload();
                boolean logRegen = plugin.getLogManager().reload();
                plugin.getItemPoolManager().loadActiveItems();
                plugin.getGuiManager().invalidateFiller();

                if (configRegen || dataRegen || logRegen) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfigManager().getPrefix() + "<yellow>Files were missing and have been regenerated!</yellow>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getMessage("reloaded")));
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("forcerotate")) {
                plugin.getItemPoolManager().rotatePool(true);
                sender.sendMessage(
                        MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getMessage("rotation-forced")));
                        
                if (sender instanceof Player player) {
                    try {
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(plugin.getConfigManager().getForceRotateSound());
                        player.playSound(player.getLocation(), sound, plugin.getConfigManager().getForceRotateSoundVolume(), plugin.getConfigManager().getForceRotateSoundPitch());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid force-rotate sound in config.");
                    }
                }
                return true;
            }
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /hs admin [reload|forcerotate]</red>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("hs.open"))
                completions.add("open");
            if (sender.hasPermission("hs.admin"))
                completions.add("admin");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("hs.admin")) {
            completions.add("reload");
            completions.add("forcerotate");
        }
        return completions;
    }
}
