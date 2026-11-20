package tech.kotmir.kotmiksmenus.quests;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 36 слотов: 14 квестов в слотах 10..16 и 19..25. Клики ничего не делают (прогресс идёт сам). */
public final class QuestsMenu extends Menu {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    public QuestsMenu(KotMiksMenus plugin, QuestsModule module, Player player) {
        super(plugin, player);
        var cfg = module.cfg();
        create(36, cfg.getString("title", "<dark_gray>- Начальные квесты -"));
        UUID uuid = player.getUniqueId();
        QuestsModule.Quest active = module.active(uuid);

        List<QuestsModule.Quest> quests = module.quests();
        for (int i = 0; i < quests.size() && i < SLOTS.length; i++) {
            QuestsModule.Quest q = quests.get(i);
            String state = module.isDone(uuid, q) ? "done" : (active != null && active.id().equals(q.id())) ? "active" : "locked";
            ConfigurationSection tpl = cfg.getConfigurationSection("states." + state);
            if (tpl == null) {
                continue;
            }
            List<String> taskLines = new ArrayList<>();
            for (QuestsModule.Task t : q.tasks()) {
                int cur = state.equals("done") ? t.amount() : module.progress(uuid, q, t);
                String line = cfg.getString(cur >= t.amount() ? "format.task-done" : "format.task", "{text}");
                taskLines.add(line.replace("{text}", t.text())
                        .replace("{label}", t.label())
                        .replace("{current}", String.valueOf(cur))
                        .replace("{amount}", String.valueOf(t.amount())));
            }
            Map<String, List<String>> multi = Map.of(
                    "{description}", q.description(),
                    "{tasks}", taskLines);
            set(SLOTS[i], ItemParser.parse(tpl, Text.map(
                    "num", String.valueOf(q.number()),
                    "reward", q.rewardText()), multi));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        // информационное меню
    }
}
