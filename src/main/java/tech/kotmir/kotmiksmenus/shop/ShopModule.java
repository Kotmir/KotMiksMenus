package tech.kotmir.kotmiksmenus.shop;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.Module;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Items;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** /shop - магазин на 6 вкладок. */
public final class ShopModule implements Module, CommandExecutor {

    private final KotMiksMenus plugin;
    private final Map<String, ShopTab> tabs = new LinkedHashMap<>();
    private String title = "<dark_gray>- Магазин -";
    private ItemStack separator = ItemParser.named(Material.GRAY_STAINED_GLASS_PANE, " ");
    private int stackSize = 64;

    public ShopModule(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        plugin.cmd("shop", this);
        plugin.getServer().getPluginManager().registerEvents(new KeepOnDeathListener(plugin), plugin);
    }

    @Override
    public void reload() {
        YamlConfiguration c = plugin.cfg("shop.yml");
        title = c.getString("title", title);
        stackSize = Math.max(2, c.getInt("stack-size", 64));
        ConfigurationSection sep = c.getConfigurationSection("separator");
        separator = sep != null ? ItemParser.parse(sep) : ItemParser.named(Material.GRAY_STAINED_GLASS_PANE, " ");

        tabs.clear();
        ConfigurationSection tabsSec = c.getConfigurationSection("tabs");
        if (tabsSec == null) {
            return;
        }
        for (String id : tabsSec.getKeys(false)) {
            ConfigurationSection ts = tabsSec.getConfigurationSection(id);
            if (ts == null) {
                continue;
            }
            ConfigurationSection iconSec = ts.getConfigurationSection("icon");
            if (iconSec == null) {
                continue;
            }
            Map<Integer, ShopItem> items = new LinkedHashMap<>();
            ConfigurationSection itemsSec = ts.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String slotKey : itemsSec.getKeys(false)) {
                    ConfigurationSection is = itemsSec.getConfigurationSection(slotKey);
                    if (is == null) {
                        continue;
                    }
                    try {
                        items.put(Integer.parseInt(slotKey), ShopItem.parse(is));
                    } catch (NumberFormatException ex) {
                        plugin.getLogger().warning("shop.yml: слот '" + slotKey + "' во вкладке " + id + " не число");
                    }
                }
            }
            tabs.put(id, new ShopTab(id, ts.getInt("slot", 0), ItemParser.parse(iconSec), items));
        }
    }

    /** Предмет "социальный коин" (с PDC-тегом social_coin). */
    public ItemStack socialCoin(int amount) {
        ConfigurationSection s = plugin.cfg("shop.yml").getConfigurationSection("social-coin");
        ItemStack coin = s != null ? ItemParser.parse(s) : ItemParser.named(Material.GOLD_NUGGET, "<#ffdd00>Социальный коин");
        Items.setTag(coin, "social_coin", "1");
        coin.setAmount(Math.max(1, Math.min(amount, coin.getMaxStackSize())));
        return coin;
    }

    /** Подходит ли предмет под указанный способ оплаты предметом. */
    public Predicate<ItemStack> payFilter(String payKey) {
        if (payKey.equalsIgnoreCase("social-coin") || payKey.equalsIgnoreCase("social_coin")) {
            return it -> Items.hasTag(it, "social_coin");
        }
        Material material = Material.matchMaterial(payKey);
        if (material == null) {
            return it -> false;
        }
        return it -> it.getType() == material && !Items.hasTag(it, "social_coin");
    }

    public String payLabel(String payKey, long n) {
        List<String> forms = plugin.cfg("shop.yml").getStringList("pay-names." + payKey);
        if (forms.size() < 3) {
            return payKey;
        }
        return Text.plural(n, forms);
    }

    public String priceText(ShopItem item, int multiplier) {
        double total = item.price() * multiplier;
        if (item.payment() == ShopItem.Payment.MONEY) {
            return Text.num(total) + " ◉";
        }
        return Text.num(total) + " " + payLabel(item.payKey(), (long) total);
    }

    /** Иконка товара: сам предмет + строки с ценой и подсказками. */
    public ItemStack icon(ShopItem item) {
        ItemStack icon = item.product().clone();
        if (item.payment() == ShopItem.Payment.NONE) {
            return icon;
        }
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(fmt("price", "price", priceText(item, 1)));
        lines.add(fmt("click-buy"));
        if (item.shiftStack()) {
            lines.add(fmt("click-stack", "price", priceText(item, stackSize), "stack", String.valueOf(stackSize)));
        }
        return ItemParser.appendLore(icon, lines);
    }

    private String fmt(String key, String... pairs) {
        String s = plugin.cfg("shop.yml").getString("format." + key, "");
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return s;
    }

    public String title() {
        return title;
    }

    public ItemStack separator() {
        return separator;
    }

    public int stackSize() {
        return stackSize;
    }

    public Map<String, ShopTab> tabs() {
        return tabs;
    }

    public ShopTab tabAt(int slot) {
        for (ShopTab tab : tabs.values()) {
            if (tab.slot() == slot) {
                return tab;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        if (tabs.isEmpty()) {
            plugin.messages().send(sender, "menu-empty");
            return true;
        }
        String first = tabs.keySet().iterator().next();
        new ShopMenu(plugin, player, this, first).open();
        return true;
    }
}
