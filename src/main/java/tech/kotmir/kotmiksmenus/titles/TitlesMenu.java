package tech.kotmir.kotmiksmenus.titles;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Главное меню титулов на 27 слотов: информация, 4 редкости, очистка. */
public final class TitlesMenu extends Menu {

    private final TitlesModule module;
    private final Map<Integer, String> raritySlots = new HashMap<>();
    private int clearSlot = 22;

    public TitlesMenu(KotMiksMenus plugin, TitlesModule module, Player player) {
        super(plugin, player);
        this.module = module;
        var cfg = module.cfg();
        create(27, cfg.getString("title", "<dark_gray>- Титулы -"));
        UUID uuid = player.getUniqueId();

        ConfigurationSection info = cfg.getConfigurationSection("info");
        if (info != null) {
            set(info.getInt("slot", 4), ItemParser.parse(info));
        }
        for (String id : module.rarityIds()) {
            ConfigurationSection r = cfg.getConfigurationSection("rarities." + id);
            if (r == null) {
                continue;
            }
            int total = module.titlesOf(id).size();
            int owned = 0;
            for (TitlesModule.TitleDef def : module.titlesOf(id)) {
                if (module.owns(uuid, def.id())) {
                    owned++;
                }
            }
            int slot = r.getInt("slot", 10);
            set(slot, ItemParser.parse(r, Text.map(
                    "price", String.valueOf(r.getInt("price", 0)),
                    "total", String.valueOf(total),
                    "owned", String.valueOf(owned))));
            raritySlots.put(slot, id);
        }
        ConfigurationSection clear = cfg.getConfigurationSection("clear");
        if (clear != null) {
            clearSlot = clear.getInt("slot", 22);
            String selected = module.selectedDisplay(uuid);
            set(clearSlot, ItemParser.parse(clear, Text.map("title", selected.isEmpty() ? "-" : selected)));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        String rarity = raritySlots.get(slot);
        if (rarity != null) {
            click();
            new TitlesRarityMenu(plugin, module, player, rarity, 0).open();
            return;
        }
        if (slot == clearSlot) {
            module.clear(player.getUniqueId());
            plugin.messages().send(player, "title-cleared");
            click();
            player.closeInventory();
        }
    }
}
