package com.kala.hazestore.task;

import com.kala.hazestore.Hazestore;
import org.bukkit.scheduler.BukkitRunnable;

// made with ❤️ by haze

public class RotationTask extends BukkitRunnable {
    private final Hazestore plugin;

    public RotationTask(Hazestore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getItemPoolManager().checkTimer();
    }
}
