package robbery.warnings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class WarningManager {

    private final Robbery plugin;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public WarningManager(Robbery plugin) { this.plugin = plugin; }

    public void addWarning(UUID uuid, String reason, String issuer, String durationRaw) {
        long now = System.currentTimeMillis();

        File file = getFile(uuid);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        Duration duration;
        try {
            duration = parseDuration(durationRaw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid duration format: " + durationRaw);
        }

        long expireMillis = now + duration.toMillis();
        String expireDate = DATE_FMT.format(new Date(expireMillis));

        List<Map<String, Object>> list = getWarningList(cfg);

        Map<String, Object> warn = new LinkedHashMap<>();
        warn.put("uuid", uuid.toString());
        warn.put("name", playerName(uuid));
        warn.put("timestamp", DATE_FMT.format(new Date(now)));
        warn.put("duration", durationRaw);
        warn.put("expires_at", expireDate);
        warn.put("reason", reason);
        warn.put("issuer", issuer);
        warn.put("active", true);
        warn.put("last_messages", ChatWarningListener.getLastMessages(uuid));

        list.add(warn);

        cfg.set("warnings", null);
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> entry = list.get(i);
            for (Map.Entry<String, Object> e : entry.entrySet()) {
                cfg.set("warnings." + i + "." + e.getKey(), e.getValue());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void sendWarningGUI(Player p, String reason, String issuer, String duration) {
        String bar = Messages.get("warning.bar");
        String title = Messages.get("warning.title");

        String reasonLine = Messages.getFormatted("warning.reason_format", Map.of("reason", reason));
        String givenBy = Messages.getFormatted("warning.given_by", Map.of("issuer", issuer, "duration", duration));

        p.sendMessage(center(bar, 43));
        p.sendMessage(center(title, 43));
        p.sendMessage(center(reasonLine, 43));
        p.sendMessage(givenBy);
        p.sendMessage(bar);
    }

    private File getFile(UUID uuid) {
        File f = new File(plugin.getDataFolder(),
                "player/" + uuid + "/warnings.yml");
        f.getParentFile().mkdirs();
        return f;
    }

    private List<Map<String, Object>> getWarningList(YamlConfiguration cfg) {
        ConfigurationSection section = cfg.getConfigurationSection("warnings");
        List<Map<String, Object>> warnings = new ArrayList<>();
        if (section == null) return warnings;

        for (String key : section.getKeys(false)) {
            ConfigurationSection warningSection = section.getConfigurationSection(key);
            if (warningSection == null) continue;

            Map<String, Object> warningMap = new HashMap<>();
            for (String entryKey : warningSection.getKeys(false)) {
                warningMap.put(entryKey, warningSection.get(entryKey));
            }

            warnings.add(warningMap);
        }

        return warnings;
    }



    private String playerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "Unknown";
    }

    private Duration parseDuration(String input) {
        if (!input.matches("\\d+[smhdwy]")) {
            throw new IllegalArgumentException("Invalid duration format.");
        }

        int value = Integer.parseInt(input.replaceAll("[^\\d]", ""));
        char unit = input.charAt(input.length() - 1);

        return switch (unit) {
            case 's' -> Duration.ofSeconds(value);
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            case 'w' -> Duration.ofDays(7L * value);
            case 'y' -> Duration.ofDays(365L * value);
            default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
        };
    }



    private String center(String txt, int width) {
        int pad = (width - ChatColor.stripColor(txt).length()) / 2;
        return " ".repeat(Math.max(0, pad)) + txt;
    }

    public void markExpiredAndSave(UUID uuid) {
        File file = getFile(uuid);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        List<Map<String, Object>> warnings = getWarningList(cfg);
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (Map<String, Object> w : warnings) {
            boolean active = (Boolean) w.getOrDefault("active", true);
            if (!active) continue;

            String expiresAtStr = (String) w.get("expires_at");
            if (expiresAtStr == null) {
                try {
                    String tsStr = (String) w.get("timestamp");
                    String durStr = (String) w.get("duration");
                    long ts = DATE_FMT.parse(tsStr).getTime();
                    long expiresAt = ts + parseDuration(durStr).toMillis();
                    expiresAtStr = DATE_FMT.format(new Date(expiresAt));
                    w.put("expires_at", expiresAtStr);
                } catch (Exception ignored) {}
            }

            try {
                Date expiresAtDate = DATE_FMT.parse(expiresAtStr);
                if (now >= expiresAtDate.getTime()) {
                    w.put("active", false);
                    changed = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (changed) {
            cfg.set("warnings", null);

            for (int i = 0; i < warnings.size(); i++) {
                Map<String, Object> warning = warnings.get(i);
                for (Map.Entry<String, Object> entry : warning.entrySet()) {
                    cfg.set("warnings." + i + "." + entry.getKey(), entry.getValue());
                }
            }

            try {
                cfg.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public List<Map<String, String>> getActiveWarnings(UUID uuid) {
        File file = getFile(uuid);
        if (!file.exists()) return Collections.emptyList();

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<String, Object>> warnings = getWarningList(cfg);

        long now = System.currentTimeMillis();

        List<Map<String, String>> activeWarnings = new ArrayList<>();
        for (Map<String, Object> w : warnings) {
            boolean active = (Boolean) w.getOrDefault("active", true);
            if (!active) continue;

            String expiresAtStr = (String) w.get("expires_at");
            if (expiresAtStr == null) continue;

            try {
                Date expiresAtDate = DATE_FMT.parse(expiresAtStr);
                if (expiresAtDate.getTime() <= now) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Map<String, String> warningData = new HashMap<>();
            warningData.put("start_date", (String) w.get("timestamp"));
            warningData.put("reason", (String) w.get("reason"));
            warningData.put("issuer", (String) w.get("issuer"));
            warningData.put("expires_at", expiresAtStr);

            activeWarnings.add(warningData);
        }

        return activeWarnings;
    }
}
