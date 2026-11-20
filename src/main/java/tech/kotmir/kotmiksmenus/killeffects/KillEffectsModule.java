package tech.kotmir.kotmiksmenus.killeffects;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;
import tech.kotmir.kotmiksmenus.util.ItemParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** /killeffects - эффекты убийства (частицы и звуки при убийстве игрока). */
public final class KillEffectsModule implements Module, CommandExecutor, Listener {

    private static final String SELECTED = "killeffect";
    private static final String OWNED = "killeffects-owned";

    private final KotMiksMenus plugin;
    private final Map<String, KillEffect> effects = new LinkedHashMap<>();

    public KillEffectsModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("killeffects", this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void reload() {
        effects.clear();
        ConfigurationSection root = cfg().getConfigurationSection("effects");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            effects.put(id, new KillEffect(id, s.getInt("price", 0), ItemParser.parse(s),
                    s.getString("name", id), KillEffect.actionsOf(s.getStringList("actions"))));
        }
    }

    public YamlConfiguration cfg() {
        return plugin.cfg("killeffects.yml");
    }

    public Map<String, KillEffect> effects() {
        return effects;
    }

    public boolean owns(UUID uuid, String id) {
        return plugin.data().listHas(uuid, OWNED, id);
    }

    public void unlock(UUID uuid, String id) {
        plugin.data().listAdd(uuid, OWNED, id);
    }

    public String selected(UUID uuid) {
        return plugin.data().getString(uuid, SELECTED, "");
    }

    public void select(UUID uuid, String id) {
        plugin.data().set(uuid, SELECTED, id);
    }

    /** Показывает эффект перед игроком (предпросмотр по ПКМ). */
    public void preview(Player player, KillEffect effect) {
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(3));
        loc.setY(player.getLocation().getY());
        effect.play(loc, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }
        String id = selected(killer.getUniqueId());
        if (id.isEmpty()) {
            return;
        }
        KillEffect effect = effects.get(id);
        if (effect != null) {
            effect.play(victim.getLocation(), plugin);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        new KillEffectsMenu(plugin, this, player).open();
        return true;
    }
}
