package tech.kotmir.kotmiksmenus.bases;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

/** Права друзей на базе: PvP, шалкеры, строительство (27 слотов). */
public final class BaseFriendsMenu extends Menu {

    private static final int PVP = 11;
    private static final int SHULKERS = 13;
    private static final int BUILD = 15;
    private static final int BACK = 22;

    private final BasesModule module;
    private final Base base;

    public BaseFriendsMenu(KotMiksMenus plugin, BasesModule module, Player player) {
        super(plugin, player);
        this.module = module;
        this.base = module.get(player.getUniqueId());
        create(27, module.cfg().getString("friends-menu.title", "<dark_gray>- Разрешения для друзей -"));
        render();
    }

    private void toggleItem(int slot, String key, boolean value) {
        ConfigurationSection s = module.cfg().getConfigurationSection("friends-menu." + key);
        if (s == null) {
            return;
        }
        set(slot, ItemParser.parse(s, Text.map("state", value
                ? module.cfg().getString("friends-menu.state-on", "<green>✔ Включено")
                : module.cfg().getString("friends-menu.state-off", "<red>✖ Выключено"))));
    }

    private void render() {
        toggleItem(PVP, "pvp", base.pvp);
        toggleItem(SHULKERS, "shulkers", base.shulkers);
        toggleItem(BUILD, "build", base.build);
        ConfigurationSection back = module.cfg().getConfigurationSection("friends-menu.back");
        if (back != null) {
            set(BACK, ItemParser.parse(back));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case PVP -> base.pvp = !base.pvp;
            case SHULKERS -> base.shulkers = !base.shulkers;
            case BUILD -> base.build = !base.build;
            case BACK -> {
                click();
                new BaseMainMenu(plugin, module, player).open();
                return;
            }
            default -> {
                return;
            }
        }
        module.save(base);
        click();
        render();
    }
}
