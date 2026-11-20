package tech.kotmir.kotmiksmenus.social;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;

/** /social - привязка Discord и Telegram. */
public final class SocialModule implements Module, CommandExecutor {

    private final KotMiksMenus plugin;

    public SocialModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("social", this);
    }

    @Override
    public void reload() {
        // всё читается из config.yml в момент открытия меню
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        new SocialMenu(plugin, player).open();
        return true;
    }
}
