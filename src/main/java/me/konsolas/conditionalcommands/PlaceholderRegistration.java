package me.konsolas.conditionalcommands;

import me.konsolas.conditionalcommands.placeholders.Placeholder;
import org.bukkit.plugin.Plugin;

class PlaceholderRegistration {
    private final Plugin plugin;
    private final Placeholder placeholder;

    PlaceholderRegistration(Plugin plugin, Placeholder placeholder) {
        this.plugin = plugin;
        this.placeholder = placeholder;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Placeholder getPlaceholder() {
        return placeholder;
    }
}
