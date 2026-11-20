package tech.kotmir.kotmiksmenus.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.util.ItemParser;

import java.util.List;

/** Товар магазина (разобранная секция shop.yml). */
public record ShopItem(
        ItemStack product,
        Payment payment,
        String payKey,
        double price,
        boolean shiftStack,
        List<String> commands,
        double moneyReward) {

    public enum Payment { MONEY, ITEM, NONE }

    public int amount() {
        return product.getAmount();
    }

    public static ShopItem parse(ConfigurationSection s) {
        ItemStack product = ItemParser.parse(s);
        Payment payment;
        try {
            payment = Payment.valueOf(s.getString("payment", "money").toUpperCase());
        } catch (IllegalArgumentException ex) {
            payment = Payment.MONEY;
        }
        return new ShopItem(
                product,
                payment,
                s.getString("pay-item", ""),
                s.getDouble("price", 0),
                s.getBoolean("shift-stack", false),
                s.getStringList("commands"),
                s.getDouble("money-reward", 0));
    }
}
