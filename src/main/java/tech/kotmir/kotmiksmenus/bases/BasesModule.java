package tech.kotmir.kotmiksmenus.bases;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;
import tech.kotmir.kotmiksmenus.util.Items;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /base (/bases) - базы игроков: покупка, телепорт, друзья, уровни, защита. */
public final class BasesModule implements Module, CommandExecutor, TabCompleter {

    public record LevelInfo(int radius, int friends, int shulkers, double upgradePrice) {
    }

    private static final String REQUESTS = "base-requests";

    private final KotMiksMenus plugin;
    private final Map<UUID, Base> bases = new LinkedHashMap<>();
    /** цель -> (владелец -> время истечения) */
    private final Map<UUID, Map<UUID, Long>> pending = new HashMap<>();

    public BasesModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("base", this);
        plugin.getServer().getPluginManager().registerEvents(new BaseProtectionListener(plugin, this), plugin);
    }

    @Override
    public void reload() {
        load();
    }

    public YamlConfiguration cfg() {
        return plugin.cfg("bases.yml");
    }

    // ---- хранение ----

    private void load() {
        bases.clear();
        ConfigurationSection root = plugin.data().yml().getConfigurationSection("bases");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                Base b = new Base(UUID.fromString(key));
                b.world = s.getString("world", "world");
                b.x = s.getDouble("x");
                b.y = s.getDouble("y");
                b.z = s.getDouble("z");
                b.yaw = (float) s.getDouble("yaw");
                b.pitch = (float) s.getDouble("pitch");
                b.level = Math.max(1, s.getInt("level", 1));
                for (String f : s.getStringList("friends")) {
                    b.friends.add(UUID.fromString(f));
                }
                b.pvp = s.getBoolean("pvp", false);
                b.shulkers = s.getBoolean("shulkers", false);
                b.build = s.getBoolean("build", false);
                bases.put(b.owner, b);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Некорректная запись базы '" + key + "' в data.yml");
            }
        }
    }

    public void save(Base b) {
        YamlConfiguration y = plugin.data().yml();
        String p = "bases." + b.owner + ".";
        y.set(p + "world", b.world);
        y.set(p + "x", b.x);
        y.set(p + "y", b.y);
        y.set(p + "z", b.z);
        y.set(p + "yaw", b.yaw);
        y.set(p + "pitch", b.pitch);
        y.set(p + "level", b.level);
        List<String> friends = new ArrayList<>();
        for (UUID f : b.friends) {
            friends.add(f.toString());
        }
        y.set(p + "friends", friends);
        y.set(p + "pvp", b.pvp);
        y.set(p + "shulkers", b.shulkers);
        y.set(p + "build", b.build);
        plugin.data().mark();
    }

    // ---- запросы ----

    public Base get(UUID owner) {
        return bases.get(owner);
    }

    public List<Base> basesOfFriend(UUID friend) {
        List<Base> out = new ArrayList<>();
        for (Base b : bases.values()) {
            if (b.friends.contains(friend)) {
                out.add(b);
            }
        }
        return out;
    }

    /** База, на территории которой находится точка (квадрат по X/Z на всю высоту). */
    public Base at(Location loc) {
        if (loc.getWorld() == null) {
            return null;
        }
        String world = loc.getWorld().getName();
        for (Base b : bases.values()) {
            if (!b.world.equals(world)) {
                continue;
            }
            int r = level(b.level).radius();
            if (Math.abs(loc.getX() - b.x) <= r && Math.abs(loc.getZ() - b.z) <= r) {
                return b;
            }
        }
        return null;
    }

    public int maxLevel() {
        ConfigurationSection levels = cfg().getConfigurationSection("levels");
        int max = 1;
        if (levels != null) {
            for (String k : levels.getKeys(false)) {
                try {
                    max = Math.max(max, Integer.parseInt(k));
                } catch (NumberFormatException ignored) {
                    // пропускаем нечисловые ключи
                }
            }
        }
        return max;
    }

    public LevelInfo level(int level) {
        int lvl = Math.max(1, Math.min(level, maxLevel()));
        ConfigurationSection s = cfg().getConfigurationSection("levels." + lvl);
        if (s == null) {
            return new LevelInfo(25, 3, 5, 0);
        }
        return new LevelInfo(s.getInt("radius", 25), s.getInt("friends", 3), s.getInt("shulkers", 5),
                s.getDouble("upgrade-price", 0));
    }

    public boolean canBuild(Player player, Base b) {
        return b.owner.equals(player.getUniqueId()) || player.hasPermission("kotmiksmenus.bases.bypass")
                || (b.friends.contains(player.getUniqueId()) && b.build);
    }

    public boolean canUseShulkers(Player player, Base b) {
        return b.owner.equals(player.getUniqueId()) || player.hasPermission("kotmiksmenus.bases.bypass")
                || (b.friends.contains(player.getUniqueId()) && b.shulkers);
    }

    /** Количество поставленных шалкеров на территории базы (по загруженным чанкам). */
    public int shulkerCount(Base b) {
        World w = Bukkit.getWorld(b.world);
        if (w == null) {
            return 0;
        }
        int r = level(b.level).radius();
        int minCx = (int) Math.floor((b.x - r) / 16.0);
        int maxCx = (int) Math.floor((b.x + r) / 16.0);
        int minCz = (int) Math.floor((b.z - r) / 16.0);
        int maxCz = (int) Math.floor((b.z + r) / 16.0);
        int count = 0;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!w.isChunkLoaded(cx, cz)) {
                    continue;
                }
                Chunk chunk = w.getChunkAt(cx, cz);
                for (BlockState state : chunk.getTileEntities(false)) {
                    if (Tag.SHULKER_BOXES.isTagged(state.getType())
                            && Math.abs(state.getX() + 0.5 - b.x) <= r && Math.abs(state.getZ() + 0.5 - b.z) <= r) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public boolean requestsEnabled(UUID uuid) {
        return plugin.data().getBoolean(uuid, REQUESTS, true);
    }

    public void setRequestsEnabled(UUID uuid, boolean enabled) {
        plugin.data().set(uuid, REQUESTS, enabled);
    }

    public String nameOf(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() == null ? uuid.toString().substring(0, 8) : p.getName();
    }

    // ---- действия ----

    /** Покупка базы за монеты. Вся проверка и сообщения - здесь. */
    public boolean purchase(Player player) {
        var messages = plugin.messages();
        if (bases.containsKey(player.getUniqueId())) {
            return false;
        }
        if (!plugin.money().available()) {
            messages.send(player, "economy-missing");
            return false;
        }
        List<String> allowed = cfg().getStringList("allowed-worlds");
        if (!allowed.isEmpty() && !allowed.contains(player.getWorld().getName())) {
            messages.send(player, "base-world");
            return false;
        }
        double price = cfg().getDouble("buy-price", 150000);
        int minDistance = cfg().getInt("min-distance", 100);
        Location loc = player.getLocation();
        for (Base other : bases.values()) {
            if (other.world.equals(loc.getWorld().getName())
                    && Math.max(Math.abs(other.x - loc.getX()), Math.abs(other.z - loc.getZ())) < minDistance) {
                messages.send(player, "base-min-distance", "distance", String.valueOf(minDistance));
                return false;
            }
        }
        if (!plugin.money().has(player, price) || !plugin.money().withdraw(player, price)) {
            messages.send(player, "no-money", "price", Text.num(price) + " ◉");
            return false;
        }
        Base base = new Base(player.getUniqueId());
        base.setLocation(loc);
        base.level = 1;
        base.pvp = cfg().getBoolean("defaults.pvp", false);
        base.shulkers = cfg().getBoolean("defaults.shulkers", false);
        base.build = cfg().getBoolean("defaults.build", false);
        bases.put(base.owner, base);
        save(base);

        List<String> commands = new ArrayList<>();
        for (String c : cfg().getStringList("create-commands")) {
            commands.add(c.replace("{world}", base.world)
                    .replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{y}", String.valueOf(loc.getBlockY()))
                    .replace("{z}", String.valueOf(loc.getBlockZ())));
        }
        Items.runCommands(player, commands, 1);
        messages.send(player, "base-bought");
        return true;
    }

    public void teleport(Player player, Base base) {
        Location loc = base.location();
        if (loc == null) {
            plugin.messages().send(player, "base-world-missing");
            return;
        }
        player.teleportAsync(loc);
        plugin.messages().send(player, "base-teleport", "owner", nameOf(base.owner));
    }

    public boolean upgrade(Player player, Base base) {
        var messages = plugin.messages();
        if (base.level >= maxLevel()) {
            messages.send(player, "base-max-level");
            return false;
        }
        double price = level(base.level).upgradePrice();
        if (!plugin.money().available()) {
            messages.send(player, "economy-missing");
            return false;
        }
        if (!plugin.money().has(player, price) || !plugin.money().withdraw(player, price)) {
            messages.send(player, "no-money", "price", Text.num(price) + " ◉");
            return false;
        }
        base.level++;
        save(base);
        messages.send(player, "base-upgraded", "level", String.valueOf(base.level));
        return true;
    }

    // ---- друзья ----

    private void sendRequest(Player owner, String targetName) {
        var messages = plugin.messages();
        Base base = bases.get(owner.getUniqueId());
        if (base == null) {
            messages.send(owner, "base-no-base");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messages.send(owner, "player-offline");
            return;
        }
        if (target.equals(owner)) {
            messages.send(owner, "base-self");
            return;
        }
        if (base.friends.contains(target.getUniqueId())) {
            messages.send(owner, "base-already-friend", "player", target.getName());
            return;
        }
        if (base.friends.size() >= level(base.level).friends()) {
            messages.send(owner, "base-friend-limit", "limit", String.valueOf(level(base.level).friends()));
            return;
        }
        if (!requestsEnabled(target.getUniqueId())) {
            messages.send(owner, "base-requests-disabled", "player", target.getName());
            return;
        }
        long ttl = cfg().getLong("request-ttl-seconds", 60) * 1000L;
        pending.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(owner.getUniqueId(), System.currentTimeMillis() + ttl);
        messages.send(owner, "base-request-sent", "player", target.getName());
        messages.send(target, "base-request-received", "owner", owner.getName());
    }

    private void respond(Player target, String ownerName, boolean accept) {
        var messages = plugin.messages();
        OfflinePlayer ownerOff = Bukkit.getOfflinePlayer(ownerName);
        Map<UUID, Long> map = pending.get(target.getUniqueId());
        Long expires = map == null ? null : map.remove(ownerOff.getUniqueId());
        if (expires == null || expires < System.currentTimeMillis()) {
            messages.send(target, "base-request-expired");
            return;
        }
        Base base = bases.get(ownerOff.getUniqueId());
        if (base == null) {
            messages.send(target, "base-request-expired");
            return;
        }
        Player owner = Bukkit.getPlayer(ownerOff.getUniqueId());
        if (!accept) {
            messages.send(target, "base-request-denied");
            if (owner != null) {
                messages.send(owner, "base-request-denied-owner", "player", target.getName());
            }
            return;
        }
        if (base.friends.size() >= level(base.level).friends()) {
            messages.send(target, "base-friend-limit", "limit", String.valueOf(level(base.level).friends()));
            return;
        }
        base.friends.add(target.getUniqueId());
        save(base);
        messages.send(target, "base-friend-joined", "owner", nameOf(base.owner));
        if (owner != null) {
            messages.send(owner, "base-friend-added", "player", target.getName());
        }
    }

    // ---- команда ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        var messages = plugin.messages();
        if (args.length == 0) {
            new BaseMainMenu(plugin, this, player).open();
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "home" -> {
                Base b = bases.get(player.getUniqueId());
                if (b == null) {
                    messages.send(player, "base-no-base");
                } else {
                    teleport(player, b);
                }
            }
            case "friend" -> {
                if (args.length >= 3 && args[1].equalsIgnoreCase("add")) {
                    sendRequest(player, args[2]);
                } else if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
                    Base b = bases.get(player.getUniqueId());
                    if (b == null) {
                        messages.send(player, "base-no-base");
                        return true;
                    }
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                    if (b.friends.remove(target.getUniqueId())) {
                        save(b);
                        messages.send(player, "base-friend-removed", "player", args[2]);
                    } else {
                        messages.send(player, "base-not-friend", "player", args[2]);
                    }
                } else {
                    messages.send(player, "base-usage");
                }
            }
            case "accept" -> {
                if (args.length >= 2) {
                    respond(player, args[1], true);
                } else {
                    messages.send(player, "base-usage");
                }
            }
            case "deny" -> {
                if (args.length >= 2) {
                    respond(player, args[1], false);
                } else {
                    messages.send(player, "base-usage");
                }
            }
            case "leave" -> {
                if (args.length < 2) {
                    messages.send(player, "base-usage");
                    return true;
                }
                Base b = bases.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId());
                if (b != null && b.friends.remove(player.getUniqueId())) {
                    save(b);
                    messages.send(player, "base-left", "owner", args[1]);
                } else {
                    messages.send(player, "base-not-friend", "player", args[1]);
                }
            }
            case "visit" -> {
                if (args.length < 2) {
                    messages.send(player, "base-usage");
                    return true;
                }
                Base b = bases.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId());
                if (b != null && b.friends.contains(player.getUniqueId())) {
                    teleport(player, b);
                } else {
                    messages.send(player, "base-not-friend", "player", args[1]);
                }
            }
            default -> messages.send(player, "base-usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("home", "friend", "accept", "deny", "leave", "visit")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("friend")) {
            out.add("add");
            out.add("remove");
        } else if ((args.length == 2 && !args[0].equalsIgnoreCase("friend"))
                || (args.length == 3 && args[0].equalsIgnoreCase("friend"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        }
        return out;
    }
}
