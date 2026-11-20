package tech.kotmir.kotmiksmenus.donate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.HashMap;
import java.util.Map;

/** Главное донат-меню на 36 слотов. */
public final class DonateMainMenu extends Menu {

    private final DonateModule module;
    private final Map<Integer, String> sections = new HashMap<>();

    public DonateMainMenu(KotMiksMenus plugin, DonateModule module, Player player) {
        super(plugin, player);
        this.module = module;
        ConfigurationSection main = module.cfg().getConfigurationSection("main");
        create(main != null ? main.getInt("size", 36) : 36,
                main != null ? main.getString("title", "<dark_gray>- Донат меню -") : "<dark_gray>- Донат меню -");
        if (main == null) {
            return;
        }
        ConfigurationSection items = main.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        Map<String, String> ph = Text.map(
                "player", player.getName(),
                "rubies", String.valueOf(plugin.rubies().get(player.getUniqueId())));
        for (String slotKey : items.getKeys(false)) {
            ConfigurationSection s = items.getConfigurationSection(slotKey);
            if (s == null) {
                continue;
            }
            try {
                int slot = Integer.parseInt(slotKey);
                set(slot, ItemParser.parse(s, ph));
                sections.put(slot, s.getString("section", ""));
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("donate.yml: слот '" + slotKey + "' не число");
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        String section = sections.get(event.getSlot());
        if (section == null || section.isEmpty()) {
            return;
        }
        click();
        if (section.equalsIgnoreCase("privileges")) {
            new PrivilegesMenu(plugin, module, player).open();
        } else if (module.cfg().isConfigurationSection("sections." + section)) {
            new DonateSectionMenu(plugin, module, player, section).open();
        }
    }
}
