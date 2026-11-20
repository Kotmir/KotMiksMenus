package tech.kotmir.kotmiksmenus.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Items;

import java.util.Map;
import java.util.function.Predicate;

/** Окно магазина на 54 слота: вкладки слева, товары справа. */
public final class ShopMenu extends tech.kotmir.kotmiksmenus.util.Menu {

    private static final int[] SEPARATORS = {1, 10, 19, 28, 37, 46};

    private final ShopModule shop;
    private String tabId;

    public ShopMenu(KotMiksMenus plugin, Player player, ShopModule shop, String tabId) {
        super(plugin, player);
        this.shop = shop;
        this.tabId = tabId;
        create(54, shop.title());
        render();
    }

    private void render() {
        inventory.clear();
        for (int slot : SEPARATORS) {
            set(slot, shop.separator().clone());
        }
        for (ShopTab tab : shop.tabs().values()) {
            ItemStack icon = tab.icon().clone();
            if (tab.id().equals(tabId)) {
                ItemParser.glow(icon, true);
            }
            set(tab.slot(), icon);
        }
        ShopTab current = shop.tabs().get(tabId);
        if (current != null) {
            for (Map.Entry<Integer, ShopItem> e : current.items().entrySet()) {
                set(e.getKey(), shop.icon(e.getValue()));
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        ShopTab clickedTab = shop.tabAt(slot);
        if (clickedTab != null) {
            if (!clickedTab.id().equals(tabId)) {
                tabId = clickedTab.id();
                render();
                click();
            }
            return;
        }

        ShopTab current = shop.tabs().get(tabId);
        if (current == null) {
            return;
        }
        ShopItem item = current.items().get(slot);
        if (item == null || item.payment() == ShopItem.Payment.NONE) {
            return;
        }
        ClickType type = event.getClick();
        if (type != ClickType.LEFT && type != ClickType.SHIFT_LEFT) {
            return;
        }
        buy(item, type == ClickType.SHIFT_LEFT && item.shiftStack());
    }

    private void buy(ShopItem item, boolean stack) {
        var messages = plugin.messages();
        int mult = stack ? shop.stackSize() : 1;
        double totalPrice = item.price() * mult;
        int units = item.amount() * mult;
        boolean givesItem = item.commands().isEmpty() && item.moneyReward() <= 0;

        if (givesItem && !Items.canFit(player, item.product(), units)) {
            messages.send(player, "no-space");
            fail();
            return;
        }
        if ((item.moneyReward() > 0 || item.payment() == ShopItem.Payment.MONEY) && !plugin.money().available()) {
            messages.send(player, "economy-missing");
            fail();
            return;
        }

        String priceText = shop.priceText(item, mult);
        if (item.payment() == ShopItem.Payment.MONEY) {
            if (!plugin.money().has(player, totalPrice)) {
                messages.send(player, "no-money", "price", priceText);
                fail();
                return;
            }
            if (!plugin.money().withdraw(player, totalPrice)) {
                messages.send(player, "no-money", "price", priceText);
                fail();
                return;
            }
        } else {
            Predicate<ItemStack> filter = shop.payFilter(item.payKey());
            int need = (int) Math.round(totalPrice);
            if (!Items.take(player, filter, need)) {
                messages.send(player, "no-items", "price", priceText);
                fail();
                return;
            }
        }

        if (!item.commands().isEmpty()) {
            Items.runCommands(player, item.commands(), units);
        }
        if (item.moneyReward() > 0) {
            plugin.money().deposit(player, item.moneyReward() * mult);
        }
        if (givesItem) {
            Items.give(player, item.product(), units);
        }

        String name = Items.plainName(item.product());
        if (units > 1 && givesItem) {
            name = name + " x" + units;
        }
        messages.send(player, "shop-bought", "item", name, "price", priceText);
        ok();
    }
}
