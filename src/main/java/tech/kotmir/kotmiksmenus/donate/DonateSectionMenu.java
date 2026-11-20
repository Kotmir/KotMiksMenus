package tech.kotmir.kotmiksmenus.donate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.Confirm;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Items;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.List;

/** Универсальное донат-подменю (киты, инструменты, кейсы, предметы): товары за рубины. */
public final class DonateSectionMenu extends Menu {

    private final DonateModule module;
    private final ConfigurationSection section;
    private final Confirm confirm = new Confirm();
    private int backSlot = 31;

    public DonateSectionMenu(KotMiksMenus plugin, DonateModule module, Player player, String id) {
        super(plugin, player);
        this.module = module;
        this.section = module.cfg().getConfigurationSection("sections." + id);
        create(section.getInt("size", 36), section.getString("title", "<dark_gray>- Донат -"));
        backSlot = section.getInt("back-slot", 31);

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String slotKey : items.getKeys(false)) {
                ConfigurationSection s = items.getConfigurationSection(slotKey);
                if (s == null) {
                    continue;
                }
                try {
                    ItemStack icon = ItemParser.parse(s);
                    List<String> lines = new ArrayList<>();
                    lines.add("");
                    lines.add(Text.apply(module.cfg().getString("format.price", ""),
                            Text.map("price", String.valueOf(s.getInt("price", 0)))));
                    lines.add(module.cfg().getString("format.click-buy", ""));
                    set(Integer.parseInt(slotKey), ItemParser.appendLore(icon, lines));
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("donate.yml: слот '" + slotKey + "' не число");
                }
            }
        }
        ConfigurationSection back = module.cfg().getConfigurationSection("back-item");
        if (back != null) {
            set(backSlot, ItemParser.parse(back));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == backSlot) {
            click();
            module.openMain(player);
            return;
        }
        ConfigurationSection items = section.getConfigurationSection("items");
        ConfigurationSection s = items == null ? null : items.getConfigurationSection(String.valueOf(slot));
        if (s == null) {
            return;
        }
        ClickType type = event.getClick();
        if (type != ClickType.LEFT && type != ClickType.SHIFT_LEFT) {
            return;
        }

        int price = s.getInt("price", 0);
        List<String> commands = s.getStringList("commands");
        ItemStack product = ItemParser.parse(s);
        boolean givesItem = commands.isEmpty();

        if (givesItem && !Items.canFit(player, product, product.getAmount())) {
            plugin.messages().send(player, "no-space");
            fail();
            return;
        }
        if (!plugin.rubies().has(player.getUniqueId(), price)) {
            plugin.messages().send(player, "no-rubies", "price", String.valueOf(price));
            fail();
            return;
        }
        if (plugin.cfg("config.yml").getBoolean("confirm-purchases", true) && !confirm.pass(slot)) {
            plugin.messages().send(player, "confirm-click");
            click();
            return;
        }
        if (!plugin.rubies().take(player.getUniqueId(), price)) {
            plugin.messages().send(player, "no-rubies", "price", String.valueOf(price));
            fail();
            return;
        }
        if (!commands.isEmpty()) {
            Items.runCommands(player, commands, product.getAmount());
        }
        if (givesItem) {
            Items.give(player, product, product.getAmount());
        }
        plugin.messages().send(player, "donate-bought", "item", Items.plainName(product), "price", String.valueOf(price));
        ok();
    }
}
