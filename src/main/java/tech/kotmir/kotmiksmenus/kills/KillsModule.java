package tech.kotmir.kotmiksmenus.kills;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;

/** /kills (/killrewards) - награды за убийства игроков. */
public final class KillsModule implements Module, CommandExecutor {

    private final KotMiksMenus plugin;

    public KillsModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("kills", this);
    }

    @Override
    public void reload() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        new KillsMenu(plugin, player).open();
        return true;
    }
}
