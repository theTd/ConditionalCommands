package me.konsolas.conditionalcommands.placeholders;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public abstract class AbstractStandardPlaceholder implements Placeholder {
    private final String name;
    private String matcher;

    public AbstractStandardPlaceholder(String matcher) {
        this.name = matcher;
        this.matcher = '-' + matcher + '-';
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final boolean shouldApply(String test) {
        return test.contains(matcher);
    }

    @Override
    public final String doSubstitution(String input, Player player) {
        return input.replaceAll(matcher, Double.toString(getStat(player)));
    }

    @Override
    public void init(Plugin plugin) {
    }

    protected abstract double getStat(Player player);
}
