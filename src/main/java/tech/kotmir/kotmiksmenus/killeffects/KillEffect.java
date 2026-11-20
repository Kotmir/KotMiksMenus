package tech.kotmir.kotmiksmenus.killeffects;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import tech.kotmir.kotmiksmenus.KotMiksMenus;
import tech.kotmir.kotmiksmenus.util.ItemParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Эффект убийства. Действия (actions) в killeffects.yml:
 * <pre>
 * PARTICLE:HEART:12:0.4          имя частицы : количество : разброс [: скорость]
 * SOUND:entity.lightning_bolt.thunder:1:1   ключ звука : громкость : тон
 * LIGHTNING                      визуальная молния без урона
 * FIREWORK:#ff0000               мгновенный фейерверк указанного цвета
 * </pre>
 */
public final class KillEffect {

    private final String id;
    private final int price;
    private final ItemStack icon;
    private final String displayName;
    private final List<String> actions;

    public KillEffect(String id, int price, ItemStack icon, String displayName, List<String> actions) {
        this.id = id;
        this.price = price;
        this.icon = icon;
        this.displayName = displayName;
        this.actions = actions;
    }

    public String id() {
        return id;
    }

    public int price() {
        return price;
    }

    public ItemStack icon() {
        return icon.clone();
    }

    public String displayName() {
        return displayName;
    }

    public void play(Location loc, KotMiksMenus plugin) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        for (String action : actions) {
            try {
                run(world, loc, action);
            } catch (Exception ex) {
                plugin.getLogger().warning("killeffects.yml: эффект '" + id + "', действие '" + action + "': " + ex.getMessage());
            }
        }
    }

    private void run(World world, Location loc, String action) {
        String[] p = action.split(":");
        String kind = p[0].trim().toUpperCase(Locale.ROOT);
        switch (kind) {
            case "PARTICLE" -> {
                Particle particle = Particle.valueOf(p[1].trim().toUpperCase(Locale.ROOT));
                int count = p.length > 2 ? Integer.parseInt(p[2].trim()) : 10;
                double spread = p.length > 3 ? Double.parseDouble(p[3].trim()) : 0.3;
                double speed = p.length > 4 ? Double.parseDouble(p[4].trim()) : 0.02;
                world.spawnParticle(particle, loc.clone().add(0, 1, 0), count, spread, spread, spread, speed);
            }
            case "SOUND" -> {
                float volume = p.length > 2 ? Float.parseFloat(p[2].trim()) : 1f;
                float pitch = p.length > 3 ? Float.parseFloat(p[3].trim()) : 1f;
                world.playSound(loc, p[1].trim().toLowerCase(Locale.ROOT), volume, pitch);
            }
            case "LIGHTNING" -> world.strikeLightningEffect(loc);
            case "FIREWORK" -> {
                Color color = p.length > 1 ? ItemParser.parseColor(action.substring(action.indexOf(':') + 1)) : null;
                if (color == null) {
                    color = Color.WHITE;
                }
                final Color c = color;
                Firework fw = world.spawn(loc, Firework.class, f -> {
                    FireworkMeta meta = f.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder().withColor(c).with(FireworkEffect.Type.BALL_LARGE).build());
                    meta.setPower(0);
                    f.setFireworkMeta(meta);
                });
                fw.detonate();
            }
            default -> throw new IllegalArgumentException("неизвестное действие " + kind);
        }
    }

    public static List<String> actionsOf(List<String> raw) {
        return raw == null ? new ArrayList<>() : raw;
    }
}
