package tech.kotmir.kotmiksmenus.kills;

import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Items;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.List;

/** 45 слотов: 14 уровней в слотах 10..16 и 19..25, закрытие - слот 40. */
public final class KillsMenu extends Menu {

    private static final String CLAIMED = "kills-claimed";
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final List<String> levelKeys = new ArrayList<>();

    public KillsMenu(KotMiksMenus plugin, Player player) {
        super(plugin, player);
        YamlConfiguration cfg = plugin.cfg("config.yml");
        create(45, cfg.getString("kills.title", "<dark_gray>- Награды за убийства -"));
        ConfigurationSection levels = cfg.getConfigurationSection("kills.levels");
        if (levels != null) {
            levelKeys.addAll(levels.getKeys(false));
        }
        render();
    }

    private int closeSlot() {
        return plugin.cfg("config.yml").getInt("kills.close.slot", 40);
    }

    private void render() {
        YamlConfiguration cfg = plugin.cfg("config.yml");
        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
        for (int i = 0; i < levelKeys.size() && i < SLOTS.length; i++) {
            ConfigurationSection lvl = cfg.getConfigurationSection("kills.levels." + levelKeys.get(i));
            if (lvl == null) {
                continue;
            }
            int need = lvl.getInt("kills", 1);
            String state;
            if (plugin.data().listHas(player.getUniqueId(), CLAIMED, levelKeys.get(i))) {
                state = "claimed";
            } else if (kills >= need) {
                state = "ready";
            } else {
                state = "locked";
            }
            ConfigurationSection template = cfg.getConfigurationSection("kills.states." + state);
            if (template == null) {
                continue;
            }
            set(SLOTS[i], ItemParser.parse(template, Text.map(
                    "level", String.valueOf(i + 1),
                    "name", lvl.getString("name", "Уровень " + (i + 1)),
                    "kills", Text.group(Math.min(kills, need)),
                    "total-kills", Text.group(kills),
                    "need", Text.group(need),
                    "reward", rewardText(lvl))));
        }
        ConfigurationSection close = cfg.getConfigurationSection("kills.close");
        if (close != null) {
            set(closeSlot(), ItemParser.parse(close));
        }
    }

    private String rewardText(ConfigurationSection lvl) {
        String text = lvl.getString("reward-text");
        if (text != null) {
            return text;
        }
        return Text.num(lvl.getDouble("money", 0)) + " ◉";
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == closeSlot()) {
            click();
            player.closeInventory();
            return;
        }
        int index = -1;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == slot) {
                index = i;
            }
        }
        if (index < 0 || index >= levelKeys.size()) {
            return;
        }
        String key = levelKeys.get(index);
        ConfigurationSection lvl = plugin.cfg("config.yml").getConfigurationSection("kills.levels." + key);
        if (lvl == null) {
            return;
        }
        if (plugin.data().listHas(player.getUniqueId(), CLAIMED, key)) {
            plugin.messages().send(player, "kills-already");
            fail();
            return;
        }
        int kills = player.getStatistic(Statistic.PLAYER_KILLS);
        int need = lvl.getInt("kills", 1);
        if (kills < need) {
            plugin.messages().send(player, "kills-not-enough", "kills", String.valueOf(kills), "need", String.valueOf(need));
            fail();
            return;
        }
        double money = lvl.getDouble("money", 0);
        if (money > 0 && !plugin.money().available()) {
            plugin.messages().send(player, "economy-missing");
            fail();
            return;
        }
        plugin.data().listAdd(player.getUniqueId(), CLAIMED, key);
        if (money > 0) {
            plugin.money().deposit(player, money);
        }
        Items.runCommands(player, lvl.getStringList("commands"), 1);
        plugin.messages().send(player, "kills-claimed", "name", Text.plain(lvl.getString("name", key)),
                "reward", rewardText(lvl));
        ok();
        render();
    }
}
