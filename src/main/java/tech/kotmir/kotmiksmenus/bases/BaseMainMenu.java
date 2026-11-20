package tech.kotmir.kotmiksmenus.bases;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitTask;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

/** Главное меню баз (45 слотов): покупка / телепорт, права друзей, управление. */
public final class BaseMainMenu extends Menu {

    private static final int BUY_SLOT = 21;
    private static final int FRIENDS_SLOT = 22;
    private static final int MANAGE_SLOT = 23;

    private final BasesModule module;
    private boolean confirming;
    private BukkitTask errorTask;

    public BaseMainMenu(KotMiksMenus plugin, BasesModule module, Player player) {
        super(plugin, player);
        this.module = module;
        create(45, module.cfg().getString("menu.title", "<dark_gray>- Базы -"));
        render();
    }

    private ConfigurationSection sec(String path) {
        return module.cfg().getConfigurationSection("menu." + path);
    }

    private java.util.Map<String, String> placeholders() {
        double price = module.cfg().getDouble("buy-price", 150000);
        return Text.map("price", Text.num(price), "player", player.getName());
    }

    private void render() {
        Base base = module.get(player.getUniqueId());
        var ph = placeholders();

        ConfigurationSection main;
        if (base != null) {
            main = sec("home");
        } else {
            main = sec(confirming ? "buy-confirm" : "buy");
        }
        if (main != null) {
            set(BUY_SLOT, ItemParser.parse(main, ph));
        }
        ConfigurationSection friends = sec("friends");
        if (friends != null) {
            set(FRIENDS_SLOT, ItemParser.parse(friends, ph));
        }
        ConfigurationSection manage = sec("manage");
        if (manage != null) {
            set(MANAGE_SLOT, ItemParser.parse(manage, ph));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        Base base = module.get(player.getUniqueId());

        if (slot == BUY_SLOT) {
            click();
            if (base != null) {
                player.closeInventory();
                module.teleport(player, base);
                return;
            }
            if (!confirming) {
                confirming = true;
                render();
                return;
            }
            confirming = false;
            if (module.purchase(player)) {
                ok();
            } else {
                fail();
            }
            render();
            return;
        }

        if (slot == FRIENDS_SLOT) {
            if (base == null) {
                showNoBase();
                return;
            }
            click();
            new BaseFriendsMenu(plugin, module, player).open();
            return;
        }

        if (slot == MANAGE_SLOT) {
            click();
            new BaseManageMenu(plugin, module, player).open();
        }
    }

    /** Временно показывает TROPICAL_FISH "У вас отсутствует база!" вместо кампфайра. */
    private void showNoBase() {
        fail();
        ConfigurationSection err = sec("no-base-error");
        if (err == null) {
            plugin.messages().send(player, "base-no-base");
            return;
        }
        set(FRIENDS_SLOT, ItemParser.parse(err, placeholders()));
        if (errorTask != null) {
            errorTask.cancel();
        }
        long ticks = module.cfg().getLong("menu.error-ticks", 40);
        errorTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() == this) {
                render();
            }
        }, ticks);
    }

    @Override
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (errorTask != null) {
            errorTask.cancel();
        }
    }
}
