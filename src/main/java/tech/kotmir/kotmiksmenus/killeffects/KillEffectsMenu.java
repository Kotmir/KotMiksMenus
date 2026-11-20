package tech.kotmir.kotmiksmenus.killeffects;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 36 слотов: эффекты в слотах 0..26, нижний ряд - снять эффект (31). ЛКМ - купить/выбрать, ПКМ - предпросмотр. */
public final class KillEffectsMenu extends Menu {

    private static final int MAX = 27;

    private final KillEffectsModule module;
    private final List<KillEffect> list = new ArrayList<>();
    private final Confirm confirm = new Confirm();

    public KillEffectsMenu(KotMiksMenus plugin, KillEffectsModule module, Player player) {
        super(plugin, player);
        this.module = module;
        list.addAll(module.effects().values());
        create(36, module.cfg().getString("title", "<dark_gray>Эффекты убийства"));
        render();
    }

    private String fmt(String key, String... pairs) {
        String s = module.cfg().getString("format." + key, "");
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return s;
    }

    private void render() {
        inventory.clear();
        UUID uuid = player.getUniqueId();
        String selected = module.selected(uuid);
        for (int i = 0; i < list.size() && i < MAX; i++) {
            KillEffect e = list.get(i);
            ItemStack icon = e.icon();
            List<String> lines = new ArrayList<>();
            lines.add("");
            boolean owned = module.owns(uuid, e.id());
            if (e.id().equals(selected)) {
                lines.add(fmt("selected"));
                ItemParser.glow(icon, true);
            } else if (owned) {
                lines.add(fmt("owned"));
                lines.add(fmt("click-select"));
            } else {
                lines.add(fmt("price", "price", String.valueOf(e.price())));
                lines.add(fmt("click-buy"));
            }
            lines.add(fmt("click-preview"));
            set(i, ItemParser.appendLore(icon, lines));
        }
        ConfigurationSection clear = module.cfg().getConfigurationSection("clear");
        if (clear != null) {
            set(clear.getInt("slot", 31), ItemParser.parse(clear));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        UUID uuid = player.getUniqueId();
        int clearSlot = module.cfg().getInt("clear.slot", 31);
        if (slot == clearSlot) {
            module.select(uuid, "");
            plugin.messages().send(player, "killeffect-cleared");
            click();
            render();
            return;
        }
        if (slot < 0 || slot >= list.size() || slot >= MAX) {
            return;
        }
        KillEffect effect = list.get(slot);
        ClickType type = event.getClick();

        if (type == ClickType.RIGHT || type == ClickType.SHIFT_RIGHT) {
            player.closeInventory();
            module.preview(player, effect);
            return;
        }
        if (type != ClickType.LEFT && type != ClickType.SHIFT_LEFT) {
            return;
        }
        if (module.owns(uuid, effect.id())) {
            module.select(uuid, effect.id());
            plugin.messages().send(player, "killeffect-selected", "name", Text.plain(effect.displayName()));
            ok();
            render();
            return;
        }
        if (!plugin.rubies().has(uuid, effect.price())) {
            plugin.messages().send(player, "no-rubies", "price", String.valueOf(effect.price()));
            fail();
            return;
        }
        if (plugin.cfg("config.yml").getBoolean("confirm-purchases", true) && !confirm.pass(slot)) {
            plugin.messages().send(player, "confirm-click");
            click();
            return;
        }
        if (!plugin.rubies().take(uuid, effect.price())) {
            plugin.messages().send(player, "no-rubies", "price", String.valueOf(effect.price()));
            fail();
            return;
        }
        module.unlock(uuid, effect.id());
        module.select(uuid, effect.id());
        plugin.messages().send(player, "killeffect-bought", "name", Text.plain(effect.displayName()),
                "price", String.valueOf(effect.price()));
        ok();
        render();
    }
}
