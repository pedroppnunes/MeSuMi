package robbery.core;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class AutoReloadTask extends BukkitRunnable {

    private final Robbery plugin;

    public AutoReloadTask(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();

        if ((hour == 11 && minute == 55 && second == 0) || (hour == 23 && minute == 55 && second == 0)) {
            broadcast("&c&lServer restart in &6&l5 minutes!");
        }

        if ((hour == 11 && minute == 59 && second == 0) || (hour == 23 && minute == 59 && second == 0)) {
            broadcast("&c&lServer restart in &6&l1 minute!");
        }

        if ((hour == 11 && minute == 59 && second == 50) || (hour == 23 && minute == 59 && second == 50)) {
            broadcast("&c&lServer restart in &6&l10 seconds!");
        }

        if ((hour == 12 && minute == 0 && second == 0) || (hour == 0 && minute == 0 && second == 0)) {
            broadcast("&c&lRestarting server now!");
            // The linux server wrapper handles the actual shutdown/restart.
        }
    }

    private void broadcast(String message) {
        Bukkit.broadcastMessage(Messages.colorize(message));
    }
}
