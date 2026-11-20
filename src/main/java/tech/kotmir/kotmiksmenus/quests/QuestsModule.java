package tech.kotmir.kotmiksmenus.quests;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;
import tech.kotmir.kotmiksmenus.activity.ActivityHub;
import tech.kotmir.kotmiksmenus.activity.ActivityType;
import tech.kotmir.kotmiksmenus.util.Items;
import tech.kotmir.kotmiksmenus.util.Rewards;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** /quests (/quest) - последовательные начальные квесты. */
public final class QuestsModule implements Module, CommandExecutor, ActivityHub.Consumer {

    public record Task(int index, ActivityType type, List<String> keys, int amount, String text, String label) {
        boolean matches(ActivityType t, String key) {
            return t == type && (keys.isEmpty() || keys.contains(key));
        }
    }

    public record Quest(String id, int number, List<String> description, List<Task> tasks,
                        String rewardText, double money, List<String> rewards, List<String> commands) {
    }

    private static final String DONE = "quests-done";

    private final KotMiksMenus plugin;
    private final List<Quest> quests = new ArrayList<>();

    public QuestsModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("quests", this);
        plugin.activity().register(this);
    }

    @Override
    public void reload() {
        quests.clear();
        ConfigurationSection root = cfg().getConfigurationSection("quests");
        if (root == null) {
            return;
        }
        int number = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            number++;
            List<Task> tasks = new ArrayList<>();
            ConfigurationSection ts = s.getConfigurationSection("tasks");
            if (ts != null) {
                int idx = 0;
                for (String tid : ts.getKeys(false)) {
                    ConfigurationSection t = ts.getConfigurationSection(tid);
                    if (t == null) {
                        continue;
                    }
                    ActivityType type = ActivityType.parse(t.getString("type"));
                    if (type == null) {
                        plugin.getLogger().warning("quests.yml: квест " + id + ", задача " + tid + " - неизвестный type");
                        continue;
                    }
                    List<String> keys = new ArrayList<>();
                    for (String k : t.getStringList("keys")) {
                        keys.add(k.toUpperCase());
                    }
                    tasks.add(new Task(idx++, type, keys, Math.max(1, t.getInt("amount", 1)),
                            t.getString("text", tid), t.getString("label", "добыто")));
                }
            }
            quests.add(new Quest(id, number, s.getStringList("description"), tasks,
                    s.getString("reward-text", "-"), s.getDouble("money", 0),
                    s.getStringList("rewards"), s.getStringList("commands")));
        }
    }

    public YamlConfiguration cfg() {
        return plugin.cfg("quests.yml");
    }

    public List<Quest> quests() {
        return quests;
    }

    public boolean isDone(UUID uuid, Quest q) {
        return plugin.data().listHas(uuid, DONE, q.id());
    }

    /** Первый невыполненный квест или null. */
    public Quest active(UUID uuid) {
        for (Quest q : quests) {
            if (!isDone(uuid, q)) {
                return q;
            }
        }
        return null;
    }

    public int progress(UUID uuid, Quest q, Task t) {
        return plugin.data().getInt(uuid, "quest-progress." + q.id() + "." + t.index(), 0);
    }

    @Override
    public void onActivity(Player player, ActivityType type, String key, int amount) {
        UUID uuid = player.getUniqueId();
        Quest q = active(uuid);
        if (q == null) {
            return;
        }
        boolean changed = false;
        for (Task t : q.tasks()) {
            if (t.matches(type, key)) {
                int cur = progress(uuid, q, t);
                if (cur < t.amount()) {
                    plugin.data().set(uuid, "quest-progress." + q.id() + "." + t.index(), Math.min(t.amount(), cur + amount));
                    changed = true;
                }
            }
        }
        if (!changed) {
            return;
        }
        for (Task t : q.tasks()) {
            if (progress(uuid, q, t) < t.amount()) {
                return;
            }
        }
        complete(player, q);
    }

    private void complete(Player player, Quest q) {
        plugin.data().listAdd(player.getUniqueId(), DONE, q.id());
        if (q.money() > 0) {
            plugin.money().deposit(player, q.money());
        }
        Rewards.give(plugin, player, q.rewards());
        Items.runCommands(player, q.commands(), 1);
        plugin.messages().send(player, "quest-done", "num", String.valueOf(q.number()), "reward", q.rewardText());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        new QuestsMenu(plugin, this, player).open();
        return true;
    }
}
