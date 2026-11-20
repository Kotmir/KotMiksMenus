package tech.kotmir.kotmiksmenus.daily;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Items;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.List;
import java.util.Map;

/** Выбрасыватель на 9 слотов, награда в слоте 4. */
public final class DailyMenu extends Menu {

    private static final int SLOT = 4;
    private static final String LAST = "daily-last";

    public DailyMenu(KotMiksMenus plugin, Player player) {
        super(plugin, player);
        createType(InventoryType.DROPPER, plugin.cfg("config.yml").getString("daily.title", "<dark_gray>- Ежедневная награда -"));
        render();
    }

    private long cooldownMs() {
        return (long) (plugin.cfg("config.yml").getDouble("daily.cooldown-hours", 24) * 3_600_000L);
    }

    private long remaining() {
        long last = plugin.data().getLong(player.getUniqueId(), LAST, 0);
        return Math.max(0, cooldownMs() - (System.currentTimeMillis() - last));
    }

    private void render() {
        YamlConfiguration cfg = plugin.cfg("config.yml");
        long left = remaining();
        boolean ready = left <= 0;
        String status = ready
                ? cfg.getString("daily.status-ready", "")
                : cfg.getString("daily.status-wait", "").replace("{time}", Text.duration(left));
        ConfigurationSection itemSec = cfg.getConfigurationSection("daily.item");
        if (itemSec == null) {
            return;
        }
        ItemStack item = ItemParser.parse(itemSec,
                Text.map("status", status, "player", player.getName()),
                Map.of("{rewards}", cfg.getStringList("daily.reward-lines")));
        ItemParser.glow(item, ready);
        set(SLOT, item);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() != SLOT) {
            return;
        }
        long left = remaining();
        if (left > 0) {
            plugin.messages().send(player, "daily-wait", "time", Text.duration(left));
            fail();
            return;
        }
        YamlConfiguration cfg = plugin.cfg("config.yml");
        double money = cfg.getDouble("daily.rewards.money", 0);
        if (money > 0 && !plugin.money().available()) {
            plugin.messages().send(player, "economy-missing");
            fail();
            return;
        }
        plugin.data().set(player.getUniqueId(), LAST, System.currentTimeMillis());
        List<String> commands = cfg.getStringList("daily.rewards.commands");
        Items.runCommands(player, commands, 1);
        if (money > 0) {
            plugin.money().deposit(player, money);
        }
        plugin.messages().send(player, "daily-claimed");
        ok();
        render();
    }
}
