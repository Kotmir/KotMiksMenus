package tech.kotmir.kotmiksmenus.battlepass;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.List;
import java.util.UUID;

/** Подменю ежедневных / еженедельных заданий (27 слотов). */
public final class BattlePassTasksMenu extends Menu {

    private static final int BACK = 22;

    private final BattlePassModule module;
    private final BattlePassModule.Period period;
    private final int returnPage;

    public BattlePassTasksMenu(KotMiksMenus plugin, BattlePassModule module, Player player,
                               BattlePassModule.Period period, int returnPage) {
        super(plugin, player);
        this.module = module;
        this.period = period;
        this.returnPage = returnPage;
        String key = period == BattlePassModule.Period.DAILY ? "daily-tasks" : "weekly-tasks";
        create(27, module.cfg().getString(key + ".title", "<dark_gray>- Задания -"));
        render(key);
    }

    private int[] slotsFor(int n) {
        return switch (n) {
            case 1 -> new int[]{13};
            case 2 -> new int[]{11, 15};
            case 3 -> new int[]{11, 13, 15};
            case 4 -> new int[]{10, 12, 14, 16};
            default -> {
                int[] s = new int[n];
                for (int i = 0; i < n; i++) {
                    s[i] = 9 + i;
                }
                yield s;
            }
        };
    }

    private void render(String key) {
        UUID uuid = player.getUniqueId();
        List<BattlePassModule.Task> tasks = module.current(period);
        int[] slots = slotsFor(Math.min(tasks.size(), 9));
        for (int i = 0; i < slots.length; i++) {
            BattlePassModule.Task t = tasks.get(i);
            boolean done = module.done(uuid, period, t);
            ConfigurationSection tpl = module.cfg().getConfigurationSection(key + (done ? ".item-done" : ".item"));
            if (tpl == null) {
                continue;
            }
            set(slots[i], ItemParser.parse(tpl, Text.map(
                    "name", t.name(),
                    "current", String.valueOf(module.progress(uuid, period, t)),
                    "amount", String.valueOf(t.amount()),
                    "coins", String.valueOf(t.coins()))));
        }
        ConfigurationSection back = module.cfg().getConfigurationSection("menu.back");
        if (back != null) {
            set(BACK, ItemParser.parse(back));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() == BACK) {
            click();
            new BattlePassMenu(plugin, module, player, returnPage).open();
        }
    }
}
