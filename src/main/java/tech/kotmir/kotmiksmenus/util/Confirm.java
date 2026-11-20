package tech.kotmir.kotmiksmenus.util;

/** Подтверждение покупки повторным кликом по тому же слоту. */
public final class Confirm {

    private static final long WINDOW_MS = 5000;

    private int slot = -1;
    private long at;

    /** true - клик подтверждён (второй клик по тому же слоту в течение 5 секунд). */
    public boolean pass(int clickedSlot) {
        long now = System.currentTimeMillis();
        if (slot == clickedSlot && now - at <= WINDOW_MS) {
            slot = -1;
            return true;
        }
        slot = clickedSlot;
        at = now;
        return false;
    }

    public void reset() {
        slot = -1;
    }
}
