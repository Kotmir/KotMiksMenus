package tech.kotmir.kotmiksmenus.battlepass;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;
import tech.kotmir.kotmiksmenus.activity.ActivityHub;
import tech.kotmir.kotmiksmenus.activity.ActivityType;
import tech.kotmir.kotmiksmenus.util.Rewards;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** /bp (/battlepass) - батл-пасс: уровни за батл-коины, ежедневные и еженедельные задания. */
public final class BattlePassModule implements Module, CommandExecutor, TabCompleter, ActivityHub.Consumer {

    public record Level(int number, int cost, String rewardText, List<String> rewards) {
    }

    public record Task(String id, String name, ActivityType type, List<String> keys, int amount, int coins) {
        boolean matches(ActivityType t, String key) {
            return t == type && (keys.isEmpty() || keys.contains(key));
        }
    }

    public enum Period { DAILY, WEEKLY }

    private final KotMiksMenus plugin;
    private final List<Level> levels = new ArrayList<>();
    private final Map<String, Task> dailyPool = new LinkedHashMap<>();
    private final Map<String, Task> weeklyPool = new LinkedHashMap<>();

    public BattlePassModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("bp", this);
        plugin.activity().register(this);
    }

    @Override
    public void reload() {
        levels.clear();
        ConfigurationSection ls = cfg().getConfigurationSection("levels");
        if (ls != null) {
            List<Integer> nums = new ArrayList<>();
            for (String k : ls.getKeys(false)) {
                try {
                    nums.add(Integer.parseInt(k));
                } catch (NumberFormatException ignored) {
                    // пропускаем нечисловые ключи
                }
            }
            Collections.sort(nums);
            for (int n : nums) {
                ConfigurationSection s = ls.getConfigurationSection(String.valueOf(n));
                if (s != null) {
                    levels.add(new Level(n, s.getInt("cost", 1), s.getString("reward-text", "-"), s.getStringList("rewards")));
                }
            }
        }
        loadPool("daily-tasks", dailyPool);
        loadPool("weekly-tasks", weeklyPool);
    }

    private void loadPool(String path, Map<String, Task> out) {
        out.clear();
        ConfigurationSection root = cfg().getConfigurationSection(path + ".pool");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            ActivityType type = ActivityType.parse(s.getString("type"));
            if (type == null) {
                plugin.getLogger().warning("battlepass.yml: задание '" + id + "' - неизвестный type");
                continue;
            }
            List<String> keys = new ArrayList<>();
            for (String k : s.getStringList("keys")) {
                keys.add(k.toUpperCase());
            }
            out.put(id, new Task(id, s.getString("name", id), type, keys, s.getInt("amount", 1), s.getInt("coins", 1)));
        }
    }

    public YamlConfiguration cfg() {
        return plugin.cfg("battlepass.yml");
    }

    public List<Level> levels() {
        return levels;
    }

    // ---- коины и уровень ----

    public int coins(UUID uuid) {
        return plugin.data().getInt(uuid, "bp-coins", 0);
    }

    public void setCoins(UUID uuid, int amount) {
        plugin.data().set(uuid, "bp-coins", Math.max(0, amount));
    }

    public void addCoins(UUID uuid, int amount) {
        setCoins(uuid, coins(uuid) + amount);
    }

    /** Сколько уровней уже забрано. */
    public int level(UUID uuid) {
        return plugin.data().getInt(uuid, "bp-level", 0);
    }

    /** Следующий уровень, который можно забрать (null - всё забрано). */
    public Level next(UUID uuid) {
        int done = level(uuid);
        return done < levels.size() ? levels.get(done) : null;
    }

    public boolean claim(Player player) {
        Level next = next(player.getUniqueId());
        if (next == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (coins(uuid) < next.cost()) {
            plugin.messages().send(player, "bp-not-enough", "need", String.valueOf(next.cost() - coins(uuid)));
            return false;
        }
        if (!plugin.money().available() && next.rewards().stream().anyMatch(r -> r.startsWith("money:"))) {
            plugin.messages().send(player, "economy-missing");
            return false;
        }
        addCoins(uuid, -next.cost());
        plugin.data().set(uuid, "bp-level", level(uuid) + 1);
        Rewards.give(plugin, player, next.rewards());
        plugin.messages().send(player, "bp-claimed", "level", String.valueOf(next.number()), "reward", next.rewardText());
        return true;
    }

    // ---- задания ----

    public Map<String, Task> pool(Period period) {
        return period == Period.DAILY ? dailyPool : weeklyPool;
    }

    private String periodKey(Period period) {
        ZoneId zone = ZoneId.of(plugin.cfg("config.yml").getString("timezone", "Europe/Moscow"));
        LocalDate now = LocalDate.now(zone);
        if (period == Period.DAILY) {
            return now.toString();
        }
        return now.get(IsoFields.WEEK_BASED_YEAR) + "-W" + now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    /** Задания текущего периода (одинаковые для всех игроков). */
    public List<Task> current(Period period) {
        Map<String, Task> pool = pool(period);
        List<Task> all = new ArrayList<>(pool.values());
        int count = cfg().getInt((period == Period.DAILY ? "daily-tasks" : "weekly-tasks") + ".count", 3);
        long seed = periodKey(period).hashCode() * 31L + (period == Period.DAILY ? 1 : 2);
        Collections.shuffle(all, new Random(seed));
        return all.subList(0, Math.min(count, all.size()));
    }

    private String base(Period period) {
        return "bp." + period.name().toLowerCase();
    }

    /** Сбрасывает прогресс, если период сменился. */
    private void ensurePeriod(UUID uuid, Period period) {
        String key = periodKey(period);
        if (!key.equals(plugin.data().getString(uuid, base(period) + ".period", ""))) {
            plugin.data().set(uuid, base(period), null);
            plugin.data().set(uuid, base(period) + ".period", key);
        }
    }

    public int progress(UUID uuid, Period period, Task t) {
        ensurePeriod(uuid, period);
        return plugin.data().getInt(uuid, base(period) + ".prog." + t.id(), 0);
    }

    public boolean done(UUID uuid, Period period, Task t) {
        ensurePeriod(uuid, period);
        return plugin.data().getBoolean(uuid, base(period) + ".done." + t.id(), false);
    }

    @Override
    public void onActivity(Player player, ActivityType type, String key, int amount) {
        UUID uuid = player.getUniqueId();
        for (Period period : Period.values()) {
            for (Task t : current(period)) {
                if (!t.matches(type, key) || done(uuid, period, t)) {
                    continue;
                }
                int now = Math.min(t.amount(), progress(uuid, period, t) + amount);
                plugin.data().set(uuid, base(period) + ".prog." + t.id(), now);
                if (now >= t.amount()) {
                    plugin.data().set(uuid, base(period) + ".done." + t.id(), true);
                    addCoins(uuid, t.coins());
                    plugin.messages().send(player, "bp-task-done", "task", t.name(), "coins", String.valueOf(t.coins()));
                }
            }
        }
    }

    // ---- команда ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("coins")) {
            if (!sender.hasPermission("kotmiksmenus.admin")) {
                plugin.messages().send(sender, "no-permission");
                return true;
            }
            if (args.length < 3) {
                plugin.messages().send(sender, "bp-usage");
                return true;
            }
            String sub = args[1].toLowerCase();
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            if (sub.equals("balance")) {
                plugin.messages().send(sender, "bp-balance", "player", args[2],
                        "amount", String.valueOf(coins(target.getUniqueId())));
                return true;
            }
            if (args.length < 4 || !(sub.equals("give") || sub.equals("take") || sub.equals("set"))) {
                plugin.messages().send(sender, "bp-usage");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                plugin.messages().send(sender, "invalid-number");
                return true;
            }
            switch (sub) {
                case "give" -> addCoins(target.getUniqueId(), amount);
                case "take" -> addCoins(target.getUniqueId(), -amount);
                default -> setCoins(target.getUniqueId(), amount);
            }
            plugin.messages().send(sender, "bp-balance", "player", args[2],
                    "amount", String.valueOf(coins(target.getUniqueId())));
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        int page = level(player.getUniqueId()) / BattlePassMenu.PER_PAGE;
        new BattlePassMenu(plugin, this, player, page).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("kotmiksmenus.admin")) {
            return out;
        }
        if (args.length == 1) {
            out.add("coins");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("coins")) {
            out.addAll(List.of("give", "take", "set", "balance"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("coins")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        }
        return out;
    }
}
