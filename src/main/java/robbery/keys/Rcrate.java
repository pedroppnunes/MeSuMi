package robbery.keys;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.booster.Booster;
import robbery.booster.BoosterManager;
import robbery.core.RewardHolder;
import robbery.messages.Messages;
import robbery.player.PlayerDataManager;
import net.kyori.adventure.text.Component;
import robbery.ranks.RankPaper;
import robbery.ranks.RankUpdate;

import java.io.File;
import java.util.*;

public class Rcrate implements CommandExecutor,Listener {
    private static final Map<UUID, Map<Material, Integer>> pendingRewards = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> pendingRankRewards = new HashMap<>();
    private static final Map<UUID, Integer> pendingCryptoMachines = new HashMap<>();
    private static final Robbery main = Robbery.getInstance();

    public static Map<UUID, Integer> getPendingCryptoMachines() {
        return pendingCryptoMachines;
    }

    public static void addPendingCryptoMachine(UUID uuid) {
        pendingCryptoMachines.merge(uuid, 1, Integer::sum);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length < 2) {
            Messages.send(sender, "command.rcrate.usage");
            return true;
        }

        String token = args[0].toLowerCase();

        if (token.startsWith("rank")) {
            if (args.length != 2) {
                Messages.send(sender, "command.rcrate.rank-usage");
                return true;
            }

            if (!RankUpdate.rankMap.containsKey(token)) {
                Messages.sendFormatted(sender, "command.rcrate.invalid-rank", "rank", token);
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }

            Map<String, Integer> rankMap = pendingRankRewards.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>());
            rankMap.merge(token, 1, Integer::sum);

            Messages.sendFormatted(sender, "command.rcrate.rank-given", Map.of("rank", RankUpdate.rankMap.get(token) ,"player", target.getName()));
            return true;
        }

        if (token.equals("sp")) {
            if (args.length != 3) {
                Messages.send(sender, "command.rcrate.sp-usage");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                Messages.send(sender, "command.rcrate.sp-invalid-amount");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }

            PlayerDataManager.getPlayerData(target).addSkillPoints(amount);
            Messages.sendFormatted(sender, "command.rcrate.sp-given", Map.of("player", target.getName(), "amount", String.valueOf(amount)));
            return true;
        }

        if (token.startsWith("boost")) {
            if (args.length != 2) {
                Messages.send(sender, "command.rcrate.boost-usage");
                return true;
            }

            Booster booster = BoosterManager.getBooster(token);
            if (booster == null) {
                Messages.sendFormatted(sender, "command.rcrate.boost-unknown", "booster", token);
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }

            PlayerDataManager.getPlayerData(target).addBoosters(booster);
            Messages.sendFormatted(sender, "command.rcrate.boost-given", Map.of("player", target.getName(), "booster", token));
            return true;
        }

        if (args.length != 3) {
            Messages.send(sender, "command.rcrate.item-usage");
            return true;
        }

        Material mat = Material.getMaterial(token.toUpperCase());
        if (mat == null) {
            Messages.sendFormatted(sender, "command.rcrate.invalid-material", "material", args[0]);
            return true;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            Messages.send(sender, "command.rcrate.invalid-quantity");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        UUID uuid = target.getUniqueId();
        pendingRewards.computeIfAbsent(uuid, k -> new HashMap<>()).merge(mat, quantity, Integer::sum);

        Messages.sendFormatted(sender, "command.rcrate.item-given", Map.of("player", target.getName(), "material", mat.name().toLowerCase(), "quantity", String.valueOf(quantity)));

        return true;
    }


    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (List.of("SuperiorWorld","world").contains(p.getWorld().getName())) {
            Map<Material, Integer> rewards = pendingRewards.get(p.getUniqueId());
            Map<String, Integer> rankRewards = pendingRankRewards.get(p.getUniqueId());
            int cryptoCount = pendingCryptoMachines.getOrDefault(p.getUniqueId(), 0);

            boolean hasItemRewards = rewards != null && !rewards.isEmpty();
            boolean hasRankRewards = rankRewards != null && !rankRewards.isEmpty();
            boolean hasCryptoRewards = cryptoCount > 0;

            if (hasItemRewards || hasRankRewards || hasCryptoRewards) {
                Messages.send(p, "command.rcrate.notify-claim");
            }
        }
    }

    public static void openRewardGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Material, Integer> materialRewards = pendingRewards.getOrDefault(uuid, new HashMap<>());
        Map<String, Integer> rankRewards = pendingRankRewards.getOrDefault(uuid, new HashMap<>());
        int cryptoCount = pendingCryptoMachines.getOrDefault(uuid, 0);

        int totalRewards = materialRewards.size() + rankRewards.size() + (cryptoCount > 0 ? 1 : 0);
        if (totalRewards == 0) {
            Messages.send(player, "command.claim.no-rewards");
            return;
        }

        int guiSize = getOptimalSize(totalRewards);
        String title = Messages.get("command.claim.gui-title-base"); // use the message based title
        RewardHolder holder = new RewardHolder(uuid);
        Inventory gui = Bukkit.createInventory(holder, guiSize, title);

        int index = 0;

        // Render Crypto Machine reward if available
        if (cryptoCount > 0 && index < guiSize - 9) {
            ItemStack cryptoItem = robbery.crypto.CryptoItemHelper.createMachineItem(main);
            ItemMeta meta = cryptoItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                if (meta.hasLore() && meta.lore() != null) {
                    lore.addAll(meta.lore());
                }
                lore.add(net.kyori.adventure.text.Component.text("§7Amount: §ex" + cryptoCount));
                meta.lore(lore);
                cryptoItem.setItemMeta(meta);
            }
            gui.setItem(index++, cryptoItem);
        }

        for (Map.Entry<Material, Integer> entry : materialRewards.entrySet()) {
            if (index >= guiSize - 9) break;

            ItemStack item = new ItemStack(entry.getKey());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§f" + formatMaterialName(entry.getKey()));
            meta.setLore(Collections.singletonList("§7Amount: §ex" + entry.getValue()));
            item.setItemMeta(meta);
            gui.setItem(index++, item);
        }

        for (Map.Entry<String, Integer> entry : rankRewards.entrySet()) {
            if (index >= guiSize - 9) break;

            ItemStack display = RankPaper.create(entry.getKey());

            gui.setItem(index++, display);
        }

        int claimSlot = guiSize - 5;
        gui.setItem(claimSlot, createNavItem(Material.LIME_WOOL, Messages.get("command.claim.button-claim-all-name")));

        int rowStart = (claimSlot / 9) * 9;
        int rowEnd = rowStart + 9;
        for (int i = rowStart; i < rowEnd; i++) {
            if (i != claimSlot) {
                gui.setItem(i, createNavItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        player.openInventory(gui);
    }


    private static int getOptimalSize(int itemCount) {
        if (itemCount <= 18) return 27;
        else if (itemCount <= 27) return 36;
        else if (itemCount <= 36) return 45;
        else return 54;
    }

    private static ItemStack createNavItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatMaterialName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            formatted.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }
        return formatted.toString().trim();
    }

    public static void saveRewards(UUID uuid) {
        File f = new File(main.getDataFolder(), "player/" + uuid + "/rewards.yml");
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();

        YamlConfiguration cfg = new YamlConfiguration();

        Map<Material, Integer> rewards = pendingRewards.getOrDefault(uuid, Map.of());
        for (var e : rewards.entrySet()) {
            cfg.set("rewards." + e.getKey().name(), e.getValue());
        }

        Map<String, Integer> rankRewards = pendingRankRewards.getOrDefault(uuid, Map.of());
        for (var e : rankRewards.entrySet()) {
            cfg.set("rank_rewards." + e.getKey(), e.getValue());
        }

        int cryptoCount = pendingCryptoMachines.getOrDefault(uuid, 0);
        if (cryptoCount > 0) {
            cfg.set("crypto_machines", cryptoCount);
        }

        try {
            cfg.save(f);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public static void saveAllRewards() {
        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(pendingRewards.keySet());
        allUuids.addAll(pendingRankRewards.keySet());
        allUuids.addAll(pendingCryptoMachines.keySet());
        for (UUID uuid : allUuids) {
            saveRewards(uuid);
        }
    }

    public static void loadRewards(UUID uuid) {
        File f = new File(main.getDataFolder(), "player/" + uuid + "/rewards.yml");
        if (!f.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        Map<Material, Integer> rewards = new HashMap<>();
        if (cfg.isConfigurationSection("rewards")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("rewards")).getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m != null) rewards.put(m, cfg.getInt("rewards." + key));
            }
        }
        if (!rewards.isEmpty()) {
            pendingRewards.put(uuid, rewards);
        } else {
            pendingRewards.remove(uuid);
        }

        Map<String, Integer> rankRewards = new HashMap<>();
        if (cfg.isConfigurationSection("rank_rewards")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("rank_rewards")).getKeys(false)) {
                rankRewards.put(key, cfg.getInt("rank_rewards." + key));
            }
        }
        if (!rankRewards.isEmpty()) {
            pendingRankRewards.put(uuid, rankRewards);
        } else {
            pendingRankRewards.remove(uuid);
        }

        int cryptoCount = cfg.getInt("crypto_machines", 0);
        if (cryptoCount > 0) {
            pendingCryptoMachines.put(uuid, cryptoCount);
        } else {
            pendingCryptoMachines.remove(uuid);
        }
    }

    public static Map<UUID, Map<Material, Integer>> getPendingItemRewards() { return pendingRewards; }

    public static Map<UUID, Map<String, Integer>> getPendingRankRewards() { return pendingRankRewards; }

}

