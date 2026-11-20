package tech.kotmir.kotmiksmenus.shop;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Вкладка магазина: иконка в левой колонке и товары по слотам. */
public record ShopTab(String id, int slot, ItemStack icon, Map<Integer, ShopItem> items) {
}
