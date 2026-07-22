// made with ❤️ by haze
// made with ❤️ by haze
// made with ❤️ by haze
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

public class HazestoreCommand implements CommandExecutor, TabCompleter {
    private final Hazestore plugin;

    public HazestoreCommand(Hazestore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (player.hasPermission("hs.open")) {
                    plugin.getGuiManager().open(player);
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getMessage("no-permission")));
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("open")) {
            return handleOpen(sender, args);
        }

        if (args[0].equalsIgnoreCase("admin")) {
            return handleAdmin(sender, args);
        }

        return true;
    }

    private boolean handleOpen(CommandSender sender, String[] args) {
        if (args.length >= 3 && sender.hasPermission("hs.admin")) {
            String poolName = args[1];
            Player target = org.bukkit.Bukkit.getPlayer(args[2]);
            
            if (!plugin.getPoolConfig().getPoolNames().contains(poolName)) {
                sender.sendMessage(MiniMessage.miniMessage()
                        .deserialize(plugin.getConfigManager().getPrefix() + "<red>Pool not found.</red>"));
                return true;
            }
            
            if (target == null) {
                sender.sendMessage(MiniMessage.miniMessage()
                        .deserialize(plugin.getConfigManager().getPrefix() + "<red>Player not found.</red>"));
                return true;
            }
            
            plugin.getGuiManager().open(target, poolName);
            return true;
        }
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the store or specify a target player with /hs open <pool> <player>.");
            return true;
        }

        if (!player.hasPermission("hs.open")) {
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        String poolName = (args.length >= 2) ? args[1] : plugin.getConfigManager().getDefaultPool();
        
        if (!plugin.getPoolConfig().getPoolNames().contains(poolName)) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize(plugin.getConfigManager().getPrefix() + "<red>Pool not found.</red>"));
            return true;
        }

        plugin.getGuiManager().open(player, poolName);
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
                boolean guiRegen = plugin.getGuiConfig().load();
                boolean confirmRegen = plugin.getConfirmationConfig().load();
                boolean poolRegen = plugin.getPoolConfig().load();
                boolean dataRegen = plugin.getDataManager().reload();
                boolean logRegen = plugin.getLogManager().reload();
                plugin.getItemPoolManager().loadAllActiveItems();
                plugin.getGuiManager().invalidateFiller();

                if (configRegen || guiRegen || confirmRegen || poolRegen || dataRegen || logRegen) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        plugin.getConfigManager().getPrefix() + "<yellow>Files were missing and have been regenerated!</yellow>"));
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getMessage("reloaded")));
                }
                return true;
            }

            if (args[1].equalsIgnoreCase("forcerotate")) {
                String poolName = (args.length >= 3) ? args[2] : plugin.getConfigManager().getDefaultPool();
                
                if (!plugin.getPoolConfig().getPoolNames().contains(poolName)) {
                    sender.sendMessage(MiniMessage.miniMessage()
                            .deserialize(plugin.getConfigManager().getPrefix() + "<red>Pool not found.</red>"));
                    return true;
                }
                
                plugin.getItemPoolManager().rotatePool(poolName, true);
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

            if (args[1].equalsIgnoreCase("edit") && sender instanceof Player player) {
                if (args.length >= 3) {
                    String poolName = args[2];
                    if (plugin.getPoolConfig().getPoolNames().contains(poolName)) {
                        plugin.getPoolEditorGUI().openPoolEditor(player, poolName);
                    } else {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfigManager().getPrefix() + "<red>Pool non trovato!"));
                    }
                } else {
                    plugin.getPoolEditorGUI().openMainMenu(player);
                }
                return true;
            }
        }

        if (sender instanceof Player player) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Admin commands:</green>"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/hs admin edit [pool]</gray>"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/hs admin reload</gray>"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>/hs admin forcerotate [pool]</gray>"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>Per vedere tutti gli item (anche nascosti), usa la store normalmente con permesso hs.admin!</gold>"));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /hs admin [edit|reload|forcerotate <pool>]</red>"));
        }
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
            completions.add("edit");
            completions.add("reload");
            completions.add("forcerotate");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("edit")) {
            completions.addAll(plugin.getPoolConfig().getPoolNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            completions.addAll(plugin.getPoolConfig().getPoolNames());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("open") && sender.hasPermission("hs.admin")) {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("forcerotate")) {
            completions.addAll(plugin.getPoolConfig().getPoolNames());
        }
        return completions;
    }
}
