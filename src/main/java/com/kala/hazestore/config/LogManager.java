package com.kala.hazestore.config;

import com.kala.hazestore.Hazestore;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// made with ❤️ by haze

public class LogManager {
    private final Hazestore plugin;
    private final File logFile;

    public LogManager(Hazestore plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "log.txt");
        checkAndCreate();
    }

    public boolean reload() {
        return checkAndCreate();
    }

    private boolean checkAndCreate() {
        if (!logFile.exists()) {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create log.txt");
            }
            return true;
        }
        return false;
    }

    public void logPurchase(Player player, String item, double price) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        String logEntry = String.format("Player: %s | Date: %s | Time: %s | Item: %s | Price: %.2f",
                player.getName(), date, time, item, price);

        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            out.println(logEntry);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write to log.txt");
        }
    }
}
