package tech.kotmir.kotmiksmenus.currency;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import tech.kotmir.kotmiksmenus.KotMiksMenus;

/** %kotmiksmenus_rubies%, %kotmiksmenus_title%, %kotmiksmenus_bp_coins%, %kotmiksmenus_bp_level% */
public final class PapiExpansion extends PlaceholderExpansion {

    private final KotMiksMenus plugin;

    public PapiExpansion(KotMiksMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "kotmiksmenus";
    }

    @Override
    public String getAuthor() {
        return "KotMir";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        switch (params.toLowerCase()) {
            case "rubies":
                return String.valueOf(plugin.rubies().get(player.getUniqueId()));
            case "title":
                return plugin.titles().selectedLegacy(player.getUniqueId());
            case "bp_coins":
                return String.valueOf(plugin.battlePass().coins(player.getUniqueId()));
            case "bp_level":
                return String.valueOf(plugin.battlePass().level(player.getUniqueId()));
            default:
                return null;
        }
    }
}
