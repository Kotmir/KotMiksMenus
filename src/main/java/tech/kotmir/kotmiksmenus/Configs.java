package tech.kotmir.kotmiksmenus;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Загрузка и перезагрузка всех YAML-конфигов плагина. */
public final class Configs {

    public static final List<String> FILES = List.of(
            "config.yml", "shop.yml", "donate.yml", "titles.yml", "bases.yml",
            "killeffects.yml", "battlepass.yml", "quests.yml");

    private final KotMiksMenus plugin;
    private final Map<String, YamlConfiguration> configs = new HashMap<>();

    public Configs(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        for (String name : FILES) {
            File file = new File(plugin.getDataFolder(), name);
            if (!file.exists()) {
                plugin.saveResource(name, false);
            }
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            try (InputStream in = plugin.getResource(name)) {
                if (in != null) {
                    // значения по умолчанию из jar: недостающие ключи не ломают плагин
                    yml.setDefaults(YamlConfiguration.loadConfiguration(
                            new InputStreamReader(in, StandardCharsets.UTF_8)));
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Не удалось прочитать значения по умолчанию для " + name + ": " + ex.getMessage());
            }
            configs.put(name, yml);
        }
    }

    public YamlConfiguration get(String name) {
        return configs.get(name);
    }
}
