package tech.kotmir.kotmiksmenus.util;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Выдача/изъятие предметов, PDC-теги, выполнение консольных команд. */
public final class Items {

    private Items() {
    }

    public static NamespacedKey key(String name) {
        return new NamespacedKey(KotMiksMenus.get(), name.toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean hasTag(ItemStack item, String tag) {
        return getTag(item, tag) != null;
    }

    public static String getTag(ItemStack item, String tag) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key(tag), PersistentDataType.STRING);
    }

    public static ItemStack setTag(ItemStack item, String tag, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key(tag), PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Читаемое имя предмета без цветов. */
    public static String plainName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        String n = item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** Поместится ли total предметов proto в основной инвентарь. */
    public static boolean canFit(Player player, ItemStack proto, int total) {
        int max = proto.getMaxStackSize();
        int free = 0;
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (it == null || it.getType().isAir()) {
                free += max;
            } else if (it.isSimilar(proto)) {
                free += Math.max(0, max - it.getAmount());
            }
            if (free >= total) {
                return true;
            }
        }
        return free >= total;
    }

    /** Выдаёт total предметов; то, что не влезло, падает под ноги. */
    public static void give(Player player, ItemStack proto, int total) {
        int max = proto.getMaxStackSize();
        while (total > 0) {
            int n = Math.min(max, total);
            ItemStack copy = proto.clone();
            copy.setAmount(n);
            Map<Integer, ItemStack> left = player.getInventory().addItem(copy);
            for (ItemStack rest : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
            total -= n;
        }
    }

    public static int count(Player player, Predicate<ItemStack> filter) {
        int total = 0;
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (it != null && !it.getType().isAir() && filter.test(it)) {
                total += it.getAmount();
            }
        }
        return total;
    }

    /** Забирает amount подходящих предметов. false - если не хватает (ничего не забирается). */
    public static boolean take(Player player, Predicate<ItemStack> filter, int amount) {
        if (count(player, filter) < amount) {
            return false;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        int left = amount;
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType().isAir() || !filter.test(it)) {
                continue;
            }
            int t = Math.min(left, it.getAmount());
            left -= t;
            if (it.getAmount() - t <= 0) {
                contents[i] = null;
            } else {
                it.setAmount(it.getAmount() - t);
            }
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    /** Выполняет команды от имени консоли. Плейсхолдеры: {player}, {amount}. */
    public static void runCommands(Player player, List<String> commands, long amount) {
        for (String cmd : commands) {
            String c = cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount));
            if (c.startsWith("/")) {
                c = c.substring(1);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
        }
    }
}
