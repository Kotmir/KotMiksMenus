package tech.kotmir.kotmiksmenus.activity;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

/** Превращает события Bukkit в ActivityHub.fire(...). */
public final class ActivityListener implements Listener {

    private final ActivityHub hub;

    public ActivityListener(ActivityHub hub) {
        this.hub = hub;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        hub.fire(p, ActivityType.BREAK_BLOCK, e.getBlock().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        hub.fire(p, ActivityType.PLACE_BLOCK, e.getBlockPlaced().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || killer.equals(e.getEntity())) {
            return;
        }
        if (e.getEntity() instanceof Player) {
            hub.fire(killer, ActivityType.KILL_PLAYER, "PLAYER", 1);
        } else {
            hub.fire(killer, ActivityType.KILL_MOB, e.getEntity().getType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Entity caught = e.getCaught();
        String key = caught instanceof Item item ? item.getItemStack().getType().name() : "FISH";
        hub.fire(e.getPlayer(), ActivityType.FISH, key, 1);
    }
}
