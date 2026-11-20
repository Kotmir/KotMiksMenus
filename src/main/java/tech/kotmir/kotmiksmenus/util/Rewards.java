package tech.kotmir.kotmiksmenus.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

import java.util.List;

/**
 * Награды одной строкой:
 * <pre>
 * item:GOLDEN_APPLE:8      предмет
 * money:5000               монеты (Vault)
 * rubies:10                рубины
 * bpcoins:3                батл-коины
 * cmd:crates give {player} common 1     команда от консоли
 * </pre>
 */
public final class Rewards {

    private Rewards() {
    }

    public static void give(KotMiksMenus plugin, Player player, List<String> rewards) {
        for (String r : rewards) {
            int idx = r.indexOf(':');
            if (idx < 0) {
                continue;
            }
            String kind = r.substring(0, idx).trim().toLowerCase();
            String value = r.substring(idx + 1).trim();
            try {
                switch (kind) {
                    case "item" -> {
                        String[] p = value.split(":");
                        Material m = Material.matchMaterial(p[0]);
                        int amount = p.length > 1 ? Integer.parseInt(p[1].trim()) : 1;
                        if (m != null) {
                            Items.give(player, new ItemStack(m), amount);
                        } else {
                            plugin.getLogger().warning("Награда: неизвестный материал '" + p[0] + "'");
                        }
                    }
                    case "money" -> plugin.money().deposit(player, Double.parseDouble(value));
                    case "rubies" -> plugin.rubies().add(player.getUniqueId(), Integer.parseInt(value));
                    case "bpcoins" -> plugin.battlePass().addCoins(player.getUniqueId(), Integer.parseInt(value));
                    case "cmd" -> Items.runCommands(player, List.of(value), 1);
                    default -> plugin.getLogger().warning("Награда: неизвестный тип '" + kind + "'");
                }
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Награда: некорректное число в '" + r + "'");
            }
        }
    }
}
