package tech.kotmir.kotmiksmenus.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.Items;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * "Сохранение при смерти": предметы с PDC-тегом keep_on_death не выпадают при смерти
 * и возвращаются игроку после возрождения (хранится в data.yml, переживает рестарт).
 */
public final class KeepOnDeathListener implements Listener {

    private static final String TAG = "keep_on_death";
    private static final String PATH = "keep-items";

    private final KotMiksMenus plugin;

    public KeepOnDeathListener(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return;
        }
        Player player = event.getEntity();
        List<String> stored = plugin.data().getList(player.getUniqueId(), PATH);
        boolean changed = false;
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (Items.hasTag(item, TAG)) {
                it.remove();
                stored.add(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
                changed = true;
            }
        }
        if (changed) {
            plugin.data().set(player.getUniqueId(), PATH, stored);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) {
                restore(p);
            }
        }, 2L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null && !p.isDead()) {
                restore(p);
            }
        }, 20L);
    }

    private void restore(Player player) {
        List<String> stored = plugin.data().getList(player.getUniqueId(), PATH);
        if (stored.isEmpty()) {
            return;
        }
        plugin.data().set(player.getUniqueId(), PATH, new ArrayList<String>());
        for (String encoded : stored) {
            try {
                ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
                Items.give(player, item, item.getAmount());
            } catch (Exception ex) {
                plugin.getLogger().warning("Не удалось восстановить предмет игрока " + player.getName() + ": " + ex.getMessage());
            }
        }
    }
}
