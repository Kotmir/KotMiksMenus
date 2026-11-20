package tech.kotmir.kotmiksmenus.currency;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Донат-валюта (рубины). Провайдер задаётся в config.yml: rubies.provider = internal | playerpoints.
 * internal - баланс в data.yml; playerpoints - через PlayerPoints (по рефлексии, зависимость не нужна при сборке).
 */
public final class RubyService {

    private final KotMiksMenus plugin;

    public RubyService(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    private boolean usePlayerPoints() {
        return "playerpoints".equalsIgnoreCase(plugin.cfg("config.yml").getString("rubies.provider", "internal"))
                && Bukkit.getPluginManager().getPlugin("PlayerPoints") != null;
    }

    public int get(UUID id) {
        if (usePlayerPoints()) {
            Object r = callPoints("look", id);
            return r instanceof Number n ? n.intValue() : 0;
        }
        return plugin.data().getInt(id, "rubies", 0);
    }

    public void set(UUID id, int amount) {
        amount = Math.max(0, amount);
        if (usePlayerPoints()) {
            callPoints("set", id, amount);
            return;
        }
        plugin.data().set(id, "rubies", amount);
    }

    public void add(UUID id, int amount) {
        if (usePlayerPoints()) {
            callPoints("give", id, amount);
            return;
        }
        set(id, get(id) + amount);
    }

    public boolean has(UUID id, int amount) {
        return get(id) >= amount;
    }

    /** Списывает рубины. false - если не хватает. */
    public boolean take(UUID id, int amount) {
        if (get(id) < amount) {
            return false;
        }
        if (usePlayerPoints()) {
            callPoints("take", id, amount);
        } else {
            set(id, get(id) - amount);
        }
        return true;
    }

    private Object callPoints(String method, UUID id, Object... extra) {
        try {
            Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
            Object api = pp.getClass().getMethod("getAPI").invoke(pp);
            for (Method m : api.getClass().getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == 1 + extra.length
                        && m.getParameterTypes()[0] == UUID.class) {
                    Object[] args = new Object[1 + extra.length];
                    args[0] = id;
                    System.arraycopy(extra, 0, args, 1, extra.length);
                    return m.invoke(api, args);
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Ошибка обращения к PlayerPoints (" + method + "): " + ex.getMessage());
        }
        return null;
    }
}
