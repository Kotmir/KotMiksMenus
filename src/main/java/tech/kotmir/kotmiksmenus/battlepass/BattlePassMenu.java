package tech.kotmir.kotmiksmenus.battlepass;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Главное окно батл-пасса (54 слота): уровни "змейкой", 3 страницы. */
public final class BattlePassMenu extends Menu {

    /** Путь змейки: ряд 1 слева направо, переход вниз, ряд 3 справа налево. */
    private static final int[] PATH = {10, 11, 12, 13, 14, 15, 16, 25, 34, 33, 32, 31, 30, 29, 28};
    public static final int PER_PAGE = PATH.length;

    private static final int PREV = 18;
    private static final int NEXT = 35;
    private static final int WEEKLY = 48;
    private static final int INFO = 49;
    private static final int DAILY = 50;

    private final BattlePassModule module;
    private final int page;
    private final int pages;

    public BattlePassMenu(KotMiksMenus plugin, BattlePassModule module, Player player, int page) {
        super(plugin, player);
        this.module = module;
        this.pages = Math.max(1, (module.levels().size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));
        create(54, module.cfg().getString("menu.title", "<dark_gray>- Батл-пасс [{page}/{pages}] -")
                .replace("{page}", String.valueOf(this.page + 1))
                .replace("{pages}", String.valueOf(pages)));
        render();
    }

    private ConfigurationSection sec(String path) {
        return module.cfg().getConfigurationSection("menu." + path);
    }

    private void render() {
        UUID uuid = player.getUniqueId();
        int coins = module.coins(uuid);
        int claimed = module.level(uuid);

        // рамка
        ItemStack blue = ItemParser.named(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        ItemStack gray = ItemParser.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            boolean border = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;
            set(i, (border ? blue : gray).clone());
        }

        // уровни
        List<BattlePassModule.Level> levels = module.levels();
        for (int i = 0; i < PATH.length; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= levels.size()) {
                break;
            }
            BattlePassModule.Level lvl = levels.get(idx);
            String state = idx < claimed ? "claimed" : idx == claimed ? "next" : "locked";
            ConfigurationSection tpl = sec("level-" + state);
            if (tpl != null) {
                set(PATH[i], ItemParser.parse(tpl, Text.map(
                        "level", String.valueOf(lvl.number()),
                        "coins", String.valueOf(coins),
                        "cost", String.valueOf(lvl.cost()),
                        "reward", lvl.rewardText())));
            }
        }

        // навигация
        if (page > 0) {
            ConfigurationSection prev = sec("prev");
            if (prev != null) {
                set(PREV, ItemParser.parse(prev));
            }
        }
        if (page < pages - 1) {
            ConfigurationSection next = sec("next");
            if (next != null) {
                set(NEXT, ItemParser.parse(next));
            }
        }

        // нижние кнопки
        ConfigurationSection weekly = sec("weekly");
        if (weekly != null) {
            set(WEEKLY, ItemParser.parse(weekly));
        }
        ConfigurationSection daily = sec("daily");
        if (daily != null) {
            set(DAILY, ItemParser.parse(daily));
        }
        BattlePassModule.Level next = module.next(uuid);
        ConfigurationSection info = sec(next == null ? "info-max" : "info");
        if (info != null) {
            Map<String, String> ph = Text.map(
                    "player", player.getName(),
                    "coins", String.valueOf(coins),
                    "claimed", String.valueOf(claimed),
                    "total", String.valueOf(levels.size()));
            if (next != null) {
                ph.put("level", String.valueOf(next.number()));
                ph.put("cost", String.valueOf(next.cost()));
                ph.put("missing", String.valueOf(Math.max(0, next.cost() - coins)));
            }
            set(INFO, ItemParser.parse(info, ph));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == PREV && page > 0) {
            click();
            new BattlePassMenu(plugin, module, player, page - 1).open();
            return;
        }
        if (slot == NEXT && page < pages - 1) {
            click();
            new BattlePassMenu(plugin, module, player, page + 1).open();
            return;
        }
        if (slot == WEEKLY) {
            click();
            new BattlePassTasksMenu(plugin, module, player, BattlePassModule.Period.WEEKLY, page).open();
            return;
        }
        if (slot == DAILY) {
            click();
            new BattlePassTasksMenu(plugin, module, player, BattlePassModule.Period.DAILY, page).open();
            return;
        }
        for (int i = 0; i < PATH.length; i++) {
            if (PATH[i] != slot) {
                continue;
            }
            int idx = page * PER_PAGE + i;
            if (idx == module.level(player.getUniqueId())) {
                if (module.claim(player)) {
                    ok();
                } else {
                    fail();
                }
                render();
            }
            return;
        }
    }
}
