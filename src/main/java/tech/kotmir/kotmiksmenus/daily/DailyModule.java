package tech.kotmir.kotmiksmenus.daily;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;

/** /daily - ежедневная награда (кулдаун хранится в data.yml). */
public final class DailyModule implements Module, CommandExecutor {

    private final KotMiksMenus plugin;

    public DailyModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("daily", this);
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
        new DailyMenu(plugin, player).open();
        return true;
    }
}
