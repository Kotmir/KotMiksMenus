package tech.kotmir.kotmiksmenus.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Универсальный разбор предмета из YAML-секции.
 *
 * <pre>
 * material: GOLDEN_APPLE
 * amount: 1
 * name: "<gold>Название"
 * lore: ["строка", "{multi}"]
 * head: ключ-из-config.heads | base64 | url | хеш
 * glow: true
 * hide-enchants: false
 * enchants: { efficiency: 5 }
 * potion: { type: invisibility, duration: 180, amplifier: 0, color: "#a855f7" }
 * pdc: { special: small_trap }
 * </pre>
 */
public final class ItemParser {

    private ItemParser() {
    }

    public static ItemStack parse(ConfigurationSection s) {
        return parse(s, Collections.emptyMap(), Collections.emptyMap());
    }

    public static ItemStack parse(ConfigurationSection s, Map<String, String> ph) {
        return parse(s, ph, Collections.emptyMap());
    }

    /**
     * @param ph    плейсхолдеры {key} -> значение
     * @param multi строки лора, состоящие ровно из {key}, заменяются списком строк
     */
    public static ItemStack parse(ConfigurationSection s, Map<String, String> ph, Map<String, List<String>> multi) {
        Material material = Material.matchMaterial(s.getString("material", "STONE"));
        if (material == null || material.isAir() || !material.isItem()) {
            KotMiksMenus.get().getLogger().warning("Неизвестный материал '" + s.getString("material")
                    + "' в " + s.getCurrentPath() + ", использую STONE");
            material = Material.STONE;
        }
        int amount = Math.max(1, Math.min(99, s.getInt("amount", 1)));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (s.contains("name")) {
            meta.displayName(Text.item(Text.apply(s.getString("name"), ph)));
        }

        List<String> loreIn = s.getStringList("lore");
        if (!loreIn.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreIn) {
                List<String> expanded = multi.get(line.trim());
                if (expanded != null) {
                    for (String e : expanded) {
                        lore.add(Text.item(Text.apply(e, ph)));
                    }
                } else {
                    lore.add(Text.item(Text.apply(line, ph)));
                }
            }
            meta.lore(lore);
        }

        String head = s.getString("head");
        if (head != null && meta instanceof SkullMeta skull) {
            HeadUtil.apply(skull, head);
        }

        ConfigurationSection enchants = s.getConfigurationSection("enchants");
        if (enchants != null) {
            for (String key : enchants.getKeys(false)) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, enchants.getInt(key, 1), true);
                } else {
                    KotMiksMenus.get().getLogger().warning("Неизвестное зачарование '" + key + "' в " + s.getCurrentPath());
                }
            }
        }

        ConfigurationSection potion = s.getConfigurationSection("potion");
        if (potion != null && meta instanceof PotionMeta pm) {
            String type = potion.getString("type");
            if (type != null) {
                PotionEffectType effect = Registry.EFFECT.get(NamespacedKey.minecraft(type.toLowerCase(Locale.ROOT)));
                if (effect != null) {
                    pm.addCustomEffect(new PotionEffect(effect,
                            potion.getInt("duration", 180) * 20, potion.getInt("amplifier", 0)), true);
                } else {
                    KotMiksMenus.get().getLogger().warning("Неизвестный эффект зелья '" + type + "' в " + s.getCurrentPath());
                }
            }
            Color color = parseColor(potion.getString("color"));
            if (color != null) {
                pm.setColor(color);
            }
        }

        ConfigurationSection pdc = s.getConfigurationSection("pdc");
        if (pdc != null) {
            for (String key : pdc.getKeys(false)) {
                meta.getPersistentDataContainer().set(Items.key(key), PersistentDataType.STRING,
                        String.valueOf(pdc.get(key)));
            }
        }

        if (s.getBoolean("glow", false)) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (s.getBoolean("unbreakable", false)) {
            meta.setUnbreakable(true);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        if (!(meta instanceof PotionMeta)) {
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
        if (s.getBoolean("hide-enchants", false)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return Color.fromRGB(Integer.parseInt(h, 16));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Добавляет строки в конец лора (MiniMessage). */
    public static ItemStack appendLore(ItemStack item, List<String> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        for (String line : lines) {
            lore.add(Text.item(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack glow(ItemStack item, boolean glow) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Быстрое создание предмета из кода (материал, MiniMessage-имя, MiniMessage-лор). */
    public static ItemStack simple(String material, String name, List<String> lore) {
        org.bukkit.configuration.MemoryConfiguration m = new org.bukkit.configuration.MemoryConfiguration();
        m.set("material", material);
        m.set("name", name);
        m.set("lore", lore);
        return parse(m);
    }

    public static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
