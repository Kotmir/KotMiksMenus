package tech.kotmir.kotmiksmenus.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Единый класс для кастомных голов (PLAYER_HEAD).
 * Текстура - Base64 (как на minecraft-heads.com, поле "Value"),
 * либо полный URL текстуры, либо хеш текстуры, либо ключ из config.yml (раздел heads).
 */
public final class HeadUtil {

    private static final Map<String, String> NAMED = new HashMap<>();

    private HeadUtil() {
    }

    public static void setNamed(ConfigurationSection heads) {
        NAMED.clear();
        if (heads == null) {
            return;
        }
        for (String key : heads.getKeys(false)) {
            NAMED.put(key, heads.getString(key, ""));
        }
    }

    public static ItemStack create(String textureOrKey) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            apply(meta, textureOrKey);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void apply(SkullMeta meta, String textureOrKey) {
        if (textureOrKey == null) {
            return;
        }
        String texture = NAMED.getOrDefault(textureOrKey, textureOrKey).trim();
        if (texture.isEmpty()) {
            return;
        }
        String base64 = normalize(texture);
        PlayerProfile profile = Bukkit.createProfile(
                UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8)), null);
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
    }

    private static String normalize(String texture) {
        String url = null;
        if (texture.startsWith("http://") || texture.startsWith("https://")) {
            url = texture;
        } else if (texture.matches("^[0-9a-fA-F]{40,64}$")) {
            url = "http://textures.minecraft.net/texture/" + texture;
        }
        if (url != null) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return texture;
    }
}
