package tech.kotmir.kotmiksmenus;

/** Один функциональный модуль плагина (меню + команды + события). */
public interface Module {

    /** Вызывается один раз при старте: регистрация команд и слушателей. */
    void enable();

    /** Перечитать конфиги модуля (вызывается и после enable, и по /kmenus reload). */
    void reload();

    default void disable() {
    }
}
