package tech.kotmir.kotmiksmenus.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

/** Базовый класс всех меню. Клики внутри меню всегда отменяются (см. MenuListener). */
public abstract class Menu implements InventoryHolder {

    protected final KotMiksMenus plugin;
    protected final Player player;
    protected Inventory inventory;

    protected Menu(KotMiksMenus plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected void create(int size, String miniTitle) {
        inventory = Bukkit.createInventory(this, size, Text.chat(miniTitle));
    }

    protected void createType(InventoryType type, String miniTitle) {
        inventory = Bukkit.createInventory(this, type, Text.chat(miniTitle));
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void onClick(InventoryClickEvent event);

    public void onClose(InventoryCloseEvent event) {
    }

    protected void set(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    protected void fill(Material material) {
        ItemStack pane = ItemParser.named(material, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, pane);
            }
        }
    }

    protected void click() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    protected void ok() {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }

    protected void fail() {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
    }
}
