package tech.kotmir.kotmiksmenus;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import tech.kotmir.kotmiksmenus.activity.ActivityHub;
import tech.kotmir.kotmiksmenus.activity.ActivityListener;
import tech.kotmir.kotmiksmenus.bases.BasesModule;
import tech.kotmir.kotmiksmenus.battlepass.BattlePassModule;
import tech.kotmir.kotmiksmenus.currency.MoneyService;
import tech.kotmir.kotmiksmenus.currency.PapiExpansion;
import tech.kotmir.kotmiksmenus.currency.RubiesCommand;
import tech.kotmir.kotmiksmenus.currency.RubyService;
import tech.kotmir.kotmiksmenus.daily.DailyModule;
import tech.kotmir.kotmiksmenus.donate.DonateModule;
import tech.kotmir.kotmiksmenus.killeffects.KillEffectsModule;
import tech.kotmir.kotmiksmenus.kills.KillsModule;
import tech.kotmir.kotmiksmenus.quests.QuestsModule;
import tech.kotmir.kotmiksmenus.shop.ShopModule;
import tech.kotmir.kotmiksmenus.social.SocialModule;
import tech.kotmir.kotmiksmenus.storage.DataStore;
import tech.kotmir.kotmiksmenus.titles.TitlesModule;
import tech.kotmir.kotmiksmenus.util.HeadUtil;
import tech.kotmir.kotmiksmenus.util.MenuListener;

import java.util.ArrayList;
import java.util.List;

public final class KotMiksMenus extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static KotMiksMenus instance;

    private Configs configs;
    private Messages messages;
    private DataStore data;
    private MoneyService money;
    private RubyService rubies;
    private ActivityHub activity;

    private ShopModule shop;
    private TitlesModule titles;
    private BattlePassModule battlePass;
    private final List<Module> modules = new ArrayList<>();

    public static KotMiksMenus get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        configs = new Configs(this);
        configs.load();
        HeadUtil.setNamed(cfg("config.yml").getConfigurationSection("heads"));

        messages = new Messages(this);
        data = new DataStore(this);
        data.load();
        money = new MoneyService();
        rubies = new RubyService(this);
        activity = new ActivityHub();

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ActivityListener(activity), this);

        shop = new ShopModule(this);
        titles = new TitlesModule(this);
        battlePass = new BattlePassModule(this);

        modules.add(shop);
        modules.add(new SocialModule(this));
        modules.add(new DailyModule(this));
        modules.add(new KillsModule(this));
        modules.add(new DonateModule(this));
        modules.add(titles);
        modules.add(new BasesModule(this));
        modules.add(new KillEffectsModule(this));
        modules.add(battlePass);
        modules.add(new QuestsModule(this));

        for (Module module : modules) {
            try {
                module.enable();
                module.reload();
            } catch (Exception ex) {
                getLogger().severe("Ошибка запуска модуля " + module.getClass().getSimpleName() + ": " + ex);
                ex.printStackTrace();
            }
        }

        cmd("kmenus", this);
        cmd("rubies", new RubiesCommand(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PapiExpansion(this).register();
            getLogger().info("PlaceholderAPI: плейсхолдеры %kotmiksmenus_*% зарегистрированы.");
        }
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault не найден: покупки за монеты работать не будут.");
        }

        data.startAutosave();
        getLogger().info("KotMiksMenus включён.");
    }

    @Override
    public void onDisable() {
        for (Module module : modules) {
            try {
                module.disable();
            } catch (Exception ex) {
                getLogger().warning("Ошибка отключения модуля: " + ex);
            }
        }
        if (data != null) {
            data.stopAutosave();
            data.saveNow();
        }
    }

    /** /kmenus reload - перечитать все конфиги. */
    public void reloadAll() {
        configs.load();
        HeadUtil.setNamed(cfg("config.yml").getConfigurationSection("heads"));
        for (Module module : modules) {
            module.reload();
        }
    }

    public void cmd(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Команда /" + name + " не описана в plugin.yml");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    // ---- /kmenus ----

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kotmiksmenus.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            reloadAll();
            messages.send(sender, "reloaded");
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("givecoin")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                messages.send(sender, "player-offline");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                messages.send(sender, "invalid-number");
                return true;
            }
            ItemStack coin = shop.socialCoin(1);
            tech.kotmir.kotmiksmenus.util.Items.give(target, coin, Math.max(1, amount));
            messages.send(sender, "coin-given", "player", target.getName(), "amount", String.valueOf(amount));
            return true;
        }
        messages.send(sender, "kmenus-usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("reload", "givecoin")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("givecoin")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        }
        return out;
    }

    // ---- доступ к сервисам ----

    public YamlConfiguration cfg(String name) {
        return configs.get(name);
    }

    public Messages messages() {
        return messages;
    }

    public DataStore data() {
        return data;
    }

    public MoneyService money() {
        return money;
    }

    public RubyService rubies() {
        return rubies;
    }

    public ActivityHub activity() {
        return activity;
    }

    public ShopModule shop() {
        return shop;
    }

    public TitlesModule titles() {
        return titles;
    }

    public BattlePassModule battlePass() {
        return battlePass;
    }
}
