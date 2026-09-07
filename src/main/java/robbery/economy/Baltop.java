package robbery.economy;

import net.milkbowl.vault.economy.Economy;
import org.MSM.mesumiEconomy.economy.MoneyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.util.*;

/**
 * Handles the /baltop command, which displays a paginated leaderboard
 * of players sorted by their balance in descending order.
 */
public class Baltop implements CommandExecutor {

    private final Economy economy;

    public Baltop() {
        this.economy = Robbery.getEconomy();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
                if (page <= 0) page = 1;
            } catch (NumberFormatException ignored) {
                Messages.send(player, "command.baltop.invalid-page");
                return true;
            }
        }

        MoneyManager moneyManager = Robbery.getMoneyManager();
        if (moneyManager != null) {
            List<MoneyManager.Entry> topEntries = moneyManager.getTop();
            int PAGE_SIZE = 10;
            int totalPages = Math.max(1, (int) Math.ceil(topEntries.size() / (double) PAGE_SIZE));
            if (page > totalPages) {
                Messages.sendFormatted(player, "command.baltop.page-not-found", "page", String.valueOf(page));
                return true;
            }

            int startIndex = (page - 1) * PAGE_SIZE;
            int endIndex = Math.min(startIndex + PAGE_SIZE, topEntries.size());

            Map<String, String> headerPlaceholders = Map.of(
                    "page", String.valueOf(page),
                    "total", String.valueOf(totalPages)
            );
            Messages.sendFormatted(player, "command.baltop.header", headerPlaceholders);

            for (int i = startIndex; i < endIndex; i++) {
                MoneyManager.Entry entry = topEntries.get(i);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("rank", String.valueOf(i + 1));
                placeholders.put("player", entry.name() != null ? entry.name() : "Unknown");
                placeholders.put("balance", format(entry.money()));

                Messages.sendFormatted(player, "command.baltop.entry", placeholders);
            }
            return true;
        }

        if (economy == null) return true;

        List<OfflinePlayer> sorted = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(p -> p.getName() != null)
                .sorted((p1, p2) -> {
                    double bal1 = economy.getBalance(p1);
                    double bal2 = economy.getBalance(p2);
                    if (bal1 != bal2) return Double.compare(bal2, bal1);
                    return p1.getName().compareToIgnoreCase(Objects.requireNonNull(p2.getName()));
                })
                .toList();

        int PAGE_SIZE = 10;
        int totalPages = (int) Math.ceil(sorted.size() / (double) PAGE_SIZE);
        if (page > totalPages) {
            Messages.sendFormatted(player, "command.baltop.page-not-found", "page", String.valueOf(page));
            return true;
        }

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, sorted.size());

        Map<String, String> headerPlaceholders = Map.of(
                "page", String.valueOf(page),
                "total", String.valueOf(totalPages)
        );
        Messages.sendFormatted(player, "command.baltop.header", headerPlaceholders);

        for (int i = startIndex; i < endIndex; i++) {
            OfflinePlayer p = sorted.get(i);
            double bal = economy.getBalance(p);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("rank", String.valueOf(i + 1));
            placeholders.put("player", p.getName());
            placeholders.put("balance", format(bal));

            Messages.sendFormatted(player, "command.baltop.entry", placeholders);
        }

        return true;
    }

    /**
     * Formats a numeric balance into a readable string using suffixes.
     * <p>
     * Examples:
     * <ul>
     *     <li>1500 â†’ 1.5k</li>
     *     <li>2_500_000 â†’ 2.50m</li>
     *     <li>3_000_000_000 â†’ 3.00b</li>
     * </ul>
     * </p>
     *
     * @param value the balance value to format
     * @return formatted balance string
     */
    private String format(double value) {
        if (value >= 1_000_000_000) return String.format("%.2fb", value / 1_000_000_000);
        if (value >= 1_000_000) return String.format("%.2fm", value / 1_000_000);
        if (value >= 1_000) return String.format("%.1fk", value / 1_000);
        return String.format("%.0f", value);
    }
}
