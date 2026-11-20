package tech.kotmir.kotmiksmenus.donate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.HeadUtil;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Подменю "Привилегии" (54 слота): 7 привилегий x 3 срока. Покупка - через сайт. */
public final class PrivilegesMenu extends Menu {

    private final DonateModule module;
    private final Set<Integer> rankSlots = new HashSet<>();
    private int backSlot = 47;
    private int howtoSlot = 51;

    public PrivilegesMenu(KotMiksMenus plugin, DonateModule module, Player player) {
        super(plugin, player);
        this.module = module;
        ConfigurationSection p = module.cfg().getConfigurationSection("privileges");
        create(p != null ? p.getInt("size", 54) : 54,
                p != null ? p.getString("title", "<dark_gray>- Привилегии -") : "<dark_gray>- Привилегии -");
        if (p == null) {
            return;
        }
        backSlot = p.getInt("back-slot", 47);
        ConfigurationSection howto = p.getConfigurationSection("howto");
        if (howto != null) {
            howtoSlot = howto.getInt("slot", 51);
        }

        ConfigurationSection ranks = p.getConfigurationSection("ranks");
        ConfigurationSection durations = p.getConfigurationSection("durations");
        ConfigurationSection template = p.getConfigurationSection("item");
        if (ranks != null && durations != null && template != null) {
            List<String> rankIds = new ArrayList<>(ranks.getKeys(false));
            String prefix = p.getString("perk-prefix", "<{color}>+ <gray>");
            for (String durId : durations.getKeys(false)) {
                ConfigurationSection d = durations.getConfigurationSection(durId);
                if (d == null) {
                    continue;
                }
                int start = d.getInt("start-slot", 10);
                int priceIndex = d.getInt("price-index", 0);
                for (int i = 0; i < rankIds.size(); i++) {
                    ConfigurationSection r = ranks.getConfigurationSection(rankIds.get(i));
                    if (r == null) {
                        continue;
                    }
                    List<Integer> prices = r.getIntegerList("prices");
                    int price = priceIndex < prices.size() ? prices.get(priceIndex) : 0;
                    String color = r.getString("color", "#ffffff");
                    List<String> perks = new ArrayList<>();
                    for (String perk : r.getStringList("perks")) {
                        perks.add(prefix.replace("{color}", color) + perk);
                    }
                    ItemStack item = ItemParser.parse(template, Text.map(
                            "color", "<" + color + ">",
                            "rank", r.getString("name", rankIds.get(i).toUpperCase()),
                            "duration", d.getString("label", durId),
                            "price", Text.group(price)), Map.of("{perks}", perks));
                    if (item.getItemMeta() instanceof SkullMeta skull) {
                        HeadUtil.apply(skull, r.getString("head", ""));
                        item.setItemMeta(skull);
                    }
                    int slot = start + i;
                    set(slot, item);
                    rankSlots.add(slot);
                }
            }
        }

        ConfigurationSection back = module.cfg().getConfigurationSection("back-item");
        if (back != null) {
            set(backSlot, ItemParser.parse(back));
        }
        if (howto != null) {
            set(howtoSlot, ItemParser.parse(howto, Text.map("site", module.cfg().getString("site", ""))));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == backSlot) {
            click();
            module.openMain(player);
        } else if (slot == howtoSlot || rankSlots.contains(slot)) {
            click();
            player.closeInventory();
            module.sendLink(player);
        }
    }
}
