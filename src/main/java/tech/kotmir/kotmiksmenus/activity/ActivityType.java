package tech.kotmir.kotmiksmenus.activity;

/** Типы игровой активности, по которым считается прогресс квестов и заданий батл-пасса. */
public enum ActivityType {
    BREAK_BLOCK,
    PLACE_BLOCK,
    KILL_PLAYER,
    KILL_MOB,
    FISH;

    public static ActivityType parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
