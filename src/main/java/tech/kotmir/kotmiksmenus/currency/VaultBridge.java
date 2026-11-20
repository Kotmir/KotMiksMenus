package tech.kotmir.kotmiksmenus.currency;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Единственное место, где используется класс Vault Economy (загружается только если Vault есть). */
final class VaultBridge {

    private final Economy economy;

    VaultBridge() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        this.economy = rsp == null ? null : rsp.getProvider();
    }

    boolean ready() {
        return economy != null;
    }

    double balance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    boolean withdraw(OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    boolean deposit(OfflinePlayer player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
}
