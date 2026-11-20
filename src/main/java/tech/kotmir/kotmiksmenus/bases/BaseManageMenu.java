package tech.kotmir.kotmiksmenus.bases;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Управление (45 слотов): прокачка, базы друзей, назад (30), запросы (35). */
public final class BaseManageMenu extends Menu {

    private static final int UPGRADE = 11;
    private static final int INFO = 13;
    private static final int BACK = 30;
    private static final int REQUESTS = 35;
    private static final int[] FRIEND_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29};

    private final BasesModule module;
    private final Map<Integer, Base> friendBases = new HashMap<>();

    public BaseManageMenu(KotMiksMenus plugin, BasesModule module, Player player) {
        super(plugin, player);
        this.module = module;
        create(45, module.cfg().getString("manage-menu.title", "<dark_gray>- Управление -"));
        render();
    }

    private ConfigurationSection sec(String path) {
        return module.cfg().getConfigurationSection("manage-menu." + path);
    }

    private void render() {
        inventory.clear();
        friendBases.clear();
        UUID uuid = player.getUniqueId();
        Base base = module.get(uuid);

        if (base != null) {
            BasesModule.LevelInfo cur = module.level(base.level);
            boolean max = base.level >= module.maxLevel();
            BasesModule.LevelInfo next = module.level(base.level + 1);
            Map<String, String> ph = Text.map(
                    "level", String.valueOf(base.level),
                    "max-level", String.valueOf(module.maxLevel()),
                    "friends", String.valueOf(base.friends.size()),
                    "friends-limit", String.valueOf(cur.friends()),
                    "shulkers", String.valueOf(module.shulkerCount(base)),
                    "shulkers-limit", String.valueOf(cur.shulkers()),
                    "radius", String.valueOf(cur.radius()),
                    "price", Text.num(cur.upgradePrice()),
                    "next-friends", String.valueOf(next.friends()),
                    "next-shulkers", String.valueOf(next.shulkers()),
                    "next-radius", String.valueOf(next.radius()));
            ConfigurationSection up = sec(max ? "upgrade-max" : "upgrade");
            if (up != null) {
                set(UPGRADE, ItemParser.parse(up, ph));
            }
            ConfigurationSection info = sec("info");
            if (info != null) {
                set(INFO, ItemParser.parse(info, ph));
            }
        } else {
            ConfigurationSection none = sec("no-base");
            if (none != null) {
                set(UPGRADE, ItemParser.parse(none));
            }
        }

        // базы друзей, куда добавили игрока
        List<Base> mine = module.basesOfFriend(uuid);
        ConfigurationSection headTpl = sec("friend-base");
        for (int i = 0; i < mine.size() && i < FRIEND_SLOTS.length && headTpl != null; i++) {
            Base b = mine.get(i);
            String owner = module.nameOf(b.owner);
            ItemStack head = ItemParser.parse(headTpl, Text.map("owner", owner));
            if (head.getItemMeta() instanceof SkullMeta skull) {
                skull.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(b.owner));
                head.setItemMeta(skull);
            }
            set(FRIEND_SLOTS[i], head);
            friendBases.put(FRIEND_SLOTS[i], b);
        }

        ConfigurationSection back = sec("back");
        if (back != null) {
            set(BACK, ItemParser.parse(back));
        }
        boolean on = module.requestsEnabled(uuid);
        ConfigurationSection req = sec("requests");
        if (req != null) {
            set(REQUESTS, ItemParser.parse(req, Text.map("state", on ? "<green>✔" : "<red>✖")));
        }

        ItemStack filler = ItemParser.named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            if (inventory.getItem(i) == null && (i < 9 || i > 35)) {
                set(i, filler.clone());
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        UUID uuid = player.getUniqueId();
        if (slot == BACK) {
            click();
            new BaseMainMenu(plugin, module, player).open();
        } else if (slot == REQUESTS) {
            module.setRequestsEnabled(uuid, !module.requestsEnabled(uuid));
            click();
            render();
        } else if (slot == UPGRADE) {
            Base base = module.get(uuid);
            if (base == null) {
                fail();
                plugin.messages().send(player, "base-no-base");
                return;
            }
            if (module.upgrade(player, base)) {
                ok();
            } else {
                fail();
            }
            render();
        } else if (friendBases.containsKey(slot)) {
            click();
            player.closeInventory();
            module.teleport(player, friendBases.get(slot));
        }
    }
}
