package tech.kotmir.kotmiksmenus.bases;

import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

/**
 * Лёгкая защита территории базы: строительство, шалкеры, PvP.
 * Территория - квадрат с радиусом по уровню базы. Выключается в bases.yml (protection.enabled).
 */
public final class BaseProtectionListener implements Listener {

    private final KotMiksMenus plugin;
    private final BasesModule bases;

    public BaseProtectionListener(KotMiksMenus plugin, BasesModule bases) {
        this.plugin = plugin;
        this.bases = bases;
    }

    private boolean enabled() {
        return bases.cfg().getBoolean("protection.enabled", true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!enabled()) {
            return;
        }
        Base b = bases.at(e.getBlock().getLocation());
        if (b != null && !bases.canBuild(e.getPlayer(), b)) {
            e.setCancelled(true);
            plugin.messages().send(e.getPlayer(), "base-protected");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!enabled()) {
            return;
        }
        Player p = e.getPlayer();
        Base b = bases.at(e.getBlock().getLocation());
        if (b == null) {
            return;
        }
        if (!bases.canBuild(p, b)) {
            e.setCancelled(true);
            plugin.messages().send(p, "base-protected");
            return;
        }
        if (Tag.SHULKER_BOXES.isTagged(e.getBlockPlaced().getType())) {
            if (!bases.canUseShulkers(p, b)) {
                e.setCancelled(true);
                plugin.messages().send(p, "base-shulker-denied");
                return;
            }
            int limit = bases.level(b.level).shulkers();
            if (bases.shulkerCount(b) > limit) {
                e.setCancelled(true);
                plugin.messages().send(p, "base-shulker-limit", "limit", String.valueOf(limit));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!enabled() || !(e.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = null;
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof org.bukkit.entity.Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) {
                attacker = p;
            }
        }
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        Base b = bases.at(victim.getLocation());
        if (b == null) {
            return;
        }
        boolean attackerIsFriendOrOwner = b.owner.equals(attacker.getUniqueId()) || b.friends.contains(attacker.getUniqueId());
        boolean victimIsFriendOrOwner = b.owner.equals(victim.getUniqueId()) || b.friends.contains(victim.getUniqueId());
        // Бой между участниками базы разрешён только при включённом праве "PvP".
        // На чужой территории (чужак бьёт чужака) PvP не ограничивается.
        if (attackerIsFriendOrOwner && victimIsFriendOrOwner && !b.pvp) {
            e.setCancelled(true);
            plugin.messages().send(attacker, "base-pvp-denied");
        }
    }
}
