package tech.kotmir.kotmiksmenus.social;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;
import tech.kotmir.kotmiksmenus.util.Menu;
import tech.kotmir.kotmiksmenus.util.Text;

import java.util.Map;

/** Воронка на 5 слотов: слот 1 - Discord, слот 3 - Telegram. */
public final class SocialMenu extends Menu {

    private static final int DISCORD_SLOT = 1;
    private static final int TELEGRAM_SLOT = 3;

    public SocialMenu(KotMiksMenus plugin, Player player) {
        super(plugin, player);
        var cfg = plugin.cfg("config.yml");
        createType(InventoryType.HOPPER, cfg.getString("social.title", "<dark_gray>- Привязка -"));
        Map<String, String> ph = Text.map("player", player.getName());
        ConfigurationSection discord = cfg.getConfigurationSection("social.discord");
        ConfigurationSection telegram = cfg.getConfigurationSection("social.telegram");
        if (discord != null) {
            set(DISCORD_SLOT, ItemParser.parse(discord, ph));
        }
        if (telegram != null) {
            set(TELEGRAM_SLOT, ItemParser.parse(telegram, ph));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        String key;
        switch (event.getSlot()) {
            case DISCORD_SLOT -> key = "discord";
            case TELEGRAM_SLOT -> key = "telegram";
            default -> {
                return;
            }
        }
        click();
        player.closeInventory();
        sendSteps(key);
    }

    private void sendSteps(String key) {
        ConfigurationSection steps = plugin.cfg("config.yml").getConfigurationSection("social." + key + ".steps");
        if (steps == null) {
            return;
        }
        for (String stepKey : steps.getKeys(false)) {
            ConfigurationSection step = steps.getConfigurationSection(stepKey);
            if (step == null) {
                continue;
            }
            Component line = Text.chat(step.getString("text", "").replace("{player}", player.getName()));
            String button = step.getString("button", "");
            if (!button.isBlank()) {
                String action = step.getString("action", "");
                String value = step.getString("value", "").replace("{player}", player.getName());
                Component b = Text.chat(button);
                boolean clickable = false;
                if (action.equalsIgnoreCase("open_url")
                        && (value.startsWith("http://") || value.startsWith("https://"))) {
                    b = b.clickEvent(ClickEvent.openUrl(value));
                    clickable = true;
                } else if (action.equalsIgnoreCase("copy") && !value.isBlank()) {
                    b = b.clickEvent(ClickEvent.copyToClipboard(value));
                    clickable = true;
                }
                if (clickable) {
                    String hover = step.getString("hover", "");
                    if (!hover.isBlank()) {
                        b = b.hoverEvent(HoverEvent.showText(Text.chat(hover)));
                    }
                    line = line.append(Component.space()).append(b);
                } else {
                    line = line.append(Component.space()).append(plugin.messages().component("link-missing"));
                }
            }
            player.sendMessage(line);
        }
    }
}
