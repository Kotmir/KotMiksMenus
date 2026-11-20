package tech.kotmir.kotmiksmenus.currency;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

import java.util.ArrayList;
import java.util.List;

/** /rubies <give|take|set|balance> <игрок> [кол-во] */
public final class RubiesCommand implements CommandExecutor, TabCompleter {

    private final KotMiksMenus plugin;

    public RubiesCommand(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kotmiksmenus.admin")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.messages().send(sender, "rubies-usage");
            return true;
        }
        String sub = args[0].toLowerCase();
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player p && sub.equals("balance")) {
            target = p;
        } else {
            plugin.messages().send(sender, "rubies-usage");
            return true;
        }
        String name = target.getName() == null ? args.length >= 2 ? args[1] : "?" : target.getName();

        if (sub.equals("balance")) {
            plugin.messages().send(sender, "rubies-balance", "player", name,
                    "amount", String.valueOf(plugin.rubies().get(target.getUniqueId())));
            return true;
        }
        if (!(sub.equals("give") || sub.equals("take") || sub.equals("set")) || args.length < 3) {
            plugin.messages().send(sender, "rubies-usage");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            plugin.messages().send(sender, "invalid-number");
            return true;
        }
        if (amount < 0) {
            plugin.messages().send(sender, "invalid-number");
            return true;
        }
        switch (sub) {
            case "give" -> plugin.rubies().add(target.getUniqueId(), amount);
            case "take" -> {
                int cur = plugin.rubies().get(target.getUniqueId());
                plugin.rubies().take(target.getUniqueId(), Math.min(cur, amount));
            }
            default -> plugin.rubies().set(target.getUniqueId(), amount);
        }
        plugin.messages().send(sender, "rubies-done", "player", name,
                "amount", String.valueOf(plugin.rubies().get(target.getUniqueId())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("give", "take", "set", "balance")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    out.add(s);
                }
            }
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
