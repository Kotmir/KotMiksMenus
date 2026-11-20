package tech.kotmir.kotmiksmenus.activity;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Простая шина: слушатель событий публикует активность, модули (квесты, батл-пасс) подписываются. */
public final class ActivityHub {

    public interface Consumer {
        /**
         * @param key материал блока / тип сущности / тип пойманного предмета (UPPER_CASE)
         */
        void onActivity(Player player, ActivityType type, String key, int amount);
    }

    private final List<Consumer> consumers = new ArrayList<>();

    public void register(Consumer consumer) {
        consumers.add(consumer);
    }

    public void fire(Player player, ActivityType type, String key, int amount) {
        for (Consumer c : consumers) {
            c.onActivity(player, type, key, amount);
        }
    }
}
