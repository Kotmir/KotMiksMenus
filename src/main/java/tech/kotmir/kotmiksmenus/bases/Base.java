package tech.kotmir.kotmiksmenus.bases;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** База игрока: точка телепорта, уровень, друзья и права друзей. */
public final class Base {

    public final UUID owner;
    public String world;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public int level = 1;
    public final Set<UUID> friends = new LinkedHashSet<>();
    public boolean pvp;
    public boolean shulkers;
    public boolean build;

    public Base(UUID owner) {
        this.owner = owner;
    }

    public void setLocation(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getBlockX() + 0.5;
        this.y = loc.getY();
        this.z = loc.getBlockZ() + 0.5;
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    /** null, если мир не загружен. */
    public Location location() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }
}
