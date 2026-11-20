package tech.kotmir.kotmiksmenus.titles;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** /titles (/title) - титулы по редкостям. Данные: players.<uuid>.title и titles-owned. */
public final class TitlesModule implements Module, CommandExecutor, TabCompleter {

    public record TitleDef(String id, String rarity, String display) {
    }

    private static final String SELECTED = "title";
    private static final String OWNED = "titles-owned";

    private final KotMiksMenus plugin;

    public TitlesModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("titles", this);
    }

    @Override
    public void reload() {
    }

    public YamlConfiguration cfg() {
        return plugin.cfg("titles.yml");
    }

    // ---- каталог ----

    public List<String> rarityIds() {
        ConfigurationSection r = cfg().getConfigurationSection("rarities");
        return r == null ? List.of() : new ArrayList<>(r.getKeys(false));
    }

    public List<TitleDef> titlesOf(String rarity) {
        List<TitleDef> out = new ArrayList<>();
        ConfigurationSection t = cfg().getConfigurationSection("rarities." + rarity + ".titles");
        if (t != null) {
            for (String id : t.getKeys(false)) {
                out.add(new TitleDef(id, rarity, t.getString(id, id)));
            }
        }
        return out;
    }

    public TitleDef find(String id) {
        for (String rarity : rarityIds()) {
            for (TitleDef def : titlesOf(rarity)) {
                if (def.id().equalsIgnoreCase(id)) {
                    return def;
                }
            }
        }
        return null;
    }

    public int price(String rarity) {
        return cfg().getInt("rarities." + rarity + ".price", 0);
    }

    // ---- данные игрока ----

    public boolean owns(UUID uuid, String id) {
        return plugin.data().listHas(uuid, OWNED, id);
    }

    public void unlock(UUID uuid, String id) {
        plugin.data().listAdd(uuid, OWNED, id);
    }

    public String selectedId(UUID uuid) {
        return plugin.data().getString(uuid, SELECTED, "");
    }

    public void select(UUID uuid, String id) {
        plugin.data().set(uuid, SELECTED, id);
    }

    public void clear(UUID uuid) {
        plugin.data().set(uuid, SELECTED, "");
    }

    /** MiniMessage-строка выбранного титула или "". */
    public String selectedDisplay(UUID uuid) {
        String id = selectedId(uuid);
        if (id.isEmpty()) {
            return "";
        }
        TitleDef def = find(id);
        return def == null ? "" : def.display();
    }

    /** Для PlaceholderAPI: титул со старыми цветовыми кодами. */
    public String selectedLegacy(UUID uuid) {
        String display = selectedDisplay(uuid);
        return display.isEmpty() ? "" : Text.legacy(display);
    }

    // ---- команда ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("kotmiksmenus.admin")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            if (args.length < 3) {
                plugin.messages().send(sender, "titles-usage");
                return true;
            }
            TitleDef def = find(args[2]);
            if (def == null) {
                plugin.messages().send(sender, "title-unknown", "id", args[2]);
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            unlock(target.getUniqueId(), def.id());
            plugin.messages().send(sender, "title-given", "player", args[1], "title", Text.plain(def.display()));
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        new TitlesMenu(plugin, this, player).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("kotmiksmenus.admin")) {
            return out;
        }
        if (args.length == 1) {
            out.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (String rarity : rarityIds()) {
                for (TitleDef def : titlesOf(rarity)) {
                    if (def.id().startsWith(args[2].toLowerCase())) {
                        out.add(def.id());
                    }
                }
            }
        }
        return out;
    }
}
