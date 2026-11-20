package tech.kotmir.kotmiksmenus.titles;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.Confirm;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.List;
import java.util.UUID;

/** Подменю редкости: 36 слотов, 18 титулов на страницу, пагинация, назад в слоте 31. */
public final class TitlesRarityMenu extends Menu {

    private static final int PER_PAGE = 18;

    private final TitlesModule module;
    private final String rarity;
    private final int page;
    private final List<TitlesModule.TitleDef> titles;
    private final Confirm confirm = new Confirm();

    public TitlesRarityMenu(KotMiksMenus plugin, TitlesModule module, Player player, String rarity, int page) {
        super(plugin, player);
        this.module = module;
        this.rarity = rarity;
        this.titles = module.titlesOf(rarity);
        int pages = Math.max(1, (titles.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));

        var cfg = module.cfg();
        String rarityName = Text.plain(cfg.getString("rarities." + rarity + ".name", rarity));
        create(36, cfg.getString("submenu.title", "<dark_gray>- {rarity} -")
                .replace("{rarity}", rarityName)
                .replace("{page}", String.valueOf(this.page + 1))
                .replace("{pages}", String.valueOf(pages)));
        render(pages);
    }

    private void render(int pages) {
        var cfg = module.cfg();
        UUID uuid = player.getUniqueId();
        String selected = module.selectedId(uuid);
        int price = module.price(rarity);
        String rarityName = cfg.getString("rarities." + rarity + ".name", rarity);

        inventory.clear();
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= titles.size()) {
                break;
            }
            TitlesModule.TitleDef def = titles.get(idx);
            String state;
            if (def.id().equals(selected)) {
                state = "selected";
            } else if (module.owns(uuid, def.id())) {
                state = "owned";
            } else {
                state = "locked";
            }
            ConfigurationSection template = cfg.getConfigurationSection("submenu.title-item." + state);
            if (template != null) {
                set(i, ItemParser.parse(template, Text.map(
                        "display", def.display(),
                        "player", player.getName(),
                        "price", String.valueOf(price),
                        "rarity", rarityName)));
            }
        }

        Material filler = Material.matchMaterial(cfg.getString("submenu.filler", "GRAY_STAINED_GLASS_PANE"));
        if (filler != null) {
            ItemStack pane = ItemParser.named(filler, " ");
            for (int i = PER_PAGE; i < 36; i++) {
                set(i, pane.clone());
            }
        }
        if (page > 0) {
            ConfigurationSection prev = cfg.getConfigurationSection("submenu.prev");
            if (prev != null) {
                set(prev.getInt("slot", 30), ItemParser.parse(prev));
            }
        }
        if (page < pages - 1) {
            ConfigurationSection next = cfg.getConfigurationSection("submenu.next");
            if (next != null) {
                set(next.getInt("slot", 32), ItemParser.parse(next));
            }
        }
        ConfigurationSection back = cfg.getConfigurationSection("submenu.back");
        if (back != null) {
            set(back.getInt("slot", 31), ItemParser.parse(back));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        var cfg = module.cfg();
        int slot = event.getSlot();
        int pages = Math.max(1, (titles.size() + PER_PAGE - 1) / PER_PAGE);

        if (slot == cfg.getInt("submenu.back.slot", 31)) {
            click();
            new TitlesMenu(plugin, module, player).open();
            return;
        }
        if (slot == cfg.getInt("submenu.prev.slot", 30) && page > 0) {
            click();
            new TitlesRarityMenu(plugin, module, player, rarity, page - 1).open();
            return;
        }
        if (slot == cfg.getInt("submenu.next.slot", 32) && page < pages - 1) {
            click();
            new TitlesRarityMenu(plugin, module, player, rarity, page + 1).open();
            return;
        }
        if (slot < 0 || slot >= PER_PAGE) {
            return;
        }
        int idx = page * PER_PAGE + slot;
        if (idx >= titles.size()) {
            return;
        }
        TitlesModule.TitleDef def = titles.get(idx);
        UUID uuid = player.getUniqueId();
        ClickType type = event.getClick();

        if (module.owns(uuid, def.id())) {
            if (type == ClickType.LEFT || type == ClickType.SHIFT_LEFT) {
                module.select(uuid, def.id());
                plugin.messages().send(player, "title-selected", "title", Text.plain(def.display()));
                ok();
                render(pages);
            }
            return;
        }
        if (type != ClickType.RIGHT && type != ClickType.SHIFT_RIGHT) {
            plugin.messages().send(player, "title-locked");
            fail();
            return;
        }
        int price = module.price(rarity);
        if (!plugin.rubies().has(uuid, price)) {
            plugin.messages().send(player, "no-rubies", "price", String.valueOf(price));
            fail();
            return;
        }
        if (plugin.cfg("config.yml").getBoolean("confirm-purchases", true) && !confirm.pass(slot)) {
            plugin.messages().send(player, "confirm-click-right");
            click();
            return;
        }
        if (!plugin.rubies().take(uuid, price)) {
            plugin.messages().send(player, "no-rubies", "price", String.valueOf(price));
            fail();
            return;
        }
        module.unlock(uuid, def.id());
        plugin.messages().send(player, "title-bought", "title", Text.plain(def.display()), "price", String.valueOf(price));
        ok();
        render(pages);
    }
}
