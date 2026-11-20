package tech.kotmir.kotmiksmenus.currency;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/** Основная валюта сервера (монеты) через Vault. Провайдер экономики ищется лениво. */
public final class MoneyService {

    private VaultBridge bridge;

    private VaultBridge bridge() {
        if (bridge == null && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            VaultBridge candidate = new VaultBridge();
            if (candidate.ready()) {
                bridge = candidate;
            }
        }
        return bridge;
    }

    public boolean available() {
        return bridge() != null;
    }

    public double balance(OfflinePlayer player) {
        VaultBridge b = bridge();
        return b == null ? 0 : b.balance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return balance(player) >= amount;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        VaultBridge b = bridge();
        return b != null && b.withdraw(player, amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        VaultBridge b = bridge();
        return b != null && b.deposit(player, amount);
    }
}
