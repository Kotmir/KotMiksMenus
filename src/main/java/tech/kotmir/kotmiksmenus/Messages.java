package tech.kotmir.kotmiksmenus;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import tech.kotmir.kotmiksmenus.util.Text;

/** Сообщения из config.yml (раздел messages). */
public final class Messages {

    private final KotMiksMenus plugin;

    public Messages(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    /** pairs: ключ1, значение1, ключ2, значение2 ... (подставляются как {ключ}). */
    public String raw(String key, String... pairs) {
        FileConfiguration c = plugin.cfg("config.yml");
        String s = c.getString("messages." + key);
        if (s == null) {
            s = "<red>Нет сообщения: " + key;
        }
        s = s.replace("{prefix}", c.getString("messages.prefix", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return s;
    }

    public Component component(String key, String... pairs) {
        return Text.chat(raw(key, pairs));
    }

    public void send(CommandSender to, String key, String... pairs) {
        to.sendMessage(component(key, pairs));
    }
}
