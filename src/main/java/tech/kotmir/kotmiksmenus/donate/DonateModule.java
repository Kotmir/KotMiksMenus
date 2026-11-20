package tech.kotmir.kotmiksmenus.donate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;

/** /donate - донат-меню: привилегии, киты, инструменты, кейсы, предметы. */
public final class DonateModule implements Module, CommandExecutor {

    private final KotMiksMenus plugin;

    public DonateModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("donate", this);
    }

    @Override
    public void reload() {
    }

    public YamlConfiguration cfg() {
        return plugin.cfg("donate.yml");
    }

    public void openMain(Player player) {
        new DonateMainMenu(plugin, this, player).open();
    }

    /** Отправляет игроку кликабельную ссылку на сайт доната. */
    public void sendLink(Player player) {
        plugin.messages().send(player, "donate-link", "site", cfg().getString("site", "https://donat.kotmir.tech"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        openMain(player);
        return true;
    }
}
