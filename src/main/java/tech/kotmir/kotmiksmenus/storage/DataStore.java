package tech.kotmir.kotmiksmenus.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Хранилище данных игроков и баз в data.yml (в памяти + периодическое асинхронное сохранение). */
public final class DataStore {

    private final KotMiksMenus plugin;
    private final Object writeLock = new Object();
    private File file;
    private YamlConfiguration yml;
    private volatile boolean dirty;
    private BukkitTask task;

    public DataStore(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.getDataFolder().mkdirs();
        file = new File(plugin.getDataFolder(), "data.yml");
        yml = YamlConfiguration.loadConfiguration(file);
    }

    public void startAutosave() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::flushAsync, 20L * 30, 20L * 30);
    }

    public void stopAutosave() {
        if (task != null) {
            task.cancel();
        }
    }

    private void flushAsync() {
        if (!dirty) {
            return;
        }
        dirty = false;
        String content = yml.saveToString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(content));
    }

    public void saveNow() {
        dirty = false;
        write(yml.saveToString());
    }

    private void write(String content) {
        synchronized (writeLock) {
            try {
                Path target = file.toPath();
                Path tmp = target.resolveSibling("data.yml.tmp");
                Files.writeString(tmp, content, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                plugin.getLogger().severe("Не удалось сохранить data.yml: " + ex.getMessage());
            }
        }
    }

    public YamlConfiguration yml() {
        return yml;
    }

    public void mark() {
        dirty = true;
    }

    // ---- данные игрока: players.<uuid>.<path> ----

    private String k(UUID id, String path) {
        return "players." + id + "." + path;
    }

    public int getInt(UUID id, String path, int def) {
        return yml.getInt(k(id, path), def);
    }

    public long getLong(UUID id, String path, long def) {
        return yml.getLong(k(id, path), def);
    }

    public boolean getBoolean(UUID id, String path, boolean def) {
        return yml.getBoolean(k(id, path), def);
    }

    public String getString(UUID id, String path, String def) {
        return yml.getString(k(id, path), def);
    }

    public List<String> getList(UUID id, String path) {
        return new ArrayList<>(yml.getStringList(k(id, path)));
    }

    public void set(UUID id, String path, Object value) {
        yml.set(k(id, path), value);
        dirty = true;
    }

    public boolean listHas(UUID id, String path, String value) {
        return yml.getStringList(k(id, path)).contains(value);
    }

    public void listAdd(UUID id, String path, String value) {
        List<String> list = getList(id, path);
        if (!list.contains(value)) {
            list.add(value);
            set(id, path, list);
        }
    }

    public void listRemove(UUID id, String path, String value) {
        List<String> list = getList(id, path);
        if (list.remove(value)) {
            set(id, path, list);
        }
    }
}
