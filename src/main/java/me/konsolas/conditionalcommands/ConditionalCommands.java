package me.konsolas.conditionalcommands;

import me.konsolas.conditionalcommands.placeholders.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionalCommands extends JavaPlugin {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("/([0-9]*)/");

    private static ConditionalCommands instance;

    public static ConditionalCommands getInstance() {
        return instance;
    }

    private final Map<String, PlaceholderRegistration> placeholderRegistrationMap = new Hashtable<>();

    private final PlaceholderAACVL placeholderAACVL = new PlaceholderAACVL();
    private final PlaceholderChance placeholderChance = new PlaceholderChance();
    private final PlaceholderPerm placeholderPerm = new PlaceholderPerm();
    private final PlaceholderPing placeholderPing = new PlaceholderPing();
    private final PlaceholderPlayerCount placeholderPlayerCount = new PlaceholderPlayerCount();
    private final PlaceholderTimeOnline placeholderTimeOnline = new PlaceholderTimeOnline();
    private final PlaceholderTPS placeholderTPS = new PlaceholderTPS();
    private final PlaceholderUptime placeholderUptime = new PlaceholderUptime();

    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("Initializing placeholders...");

        registerPlaceholder(this, placeholderAACVL);
        registerPlaceholder(this, placeholderChance);
        registerPlaceholder(this, placeholderPerm);
        registerPlaceholder(this, placeholderPing);
        registerPlaceholder(this, placeholderPlayerCount);
        registerPlaceholder(this, placeholderTimeOnline);
        registerPlaceholder(this, placeholderTPS);
        registerPlaceholder(this, placeholderUptime);

        getLogger().info("Ready.");
    }

    @Override
    public void onDisable() {
        unregisterPlaceholder(placeholderAACVL);
        unregisterPlaceholder(placeholderChance);
        unregisterPlaceholder(placeholderPerm);
        unregisterPlaceholder(placeholderPing);
        unregisterPlaceholder(placeholderPlayerCount);
        unregisterPlaceholder(placeholderTimeOnline);
        unregisterPlaceholder(placeholderTPS);
        unregisterPlaceholder(placeholderUptime);
    }

    public void registerPlaceholder(Plugin plugin, Placeholder placeholder) {
        Validate.notNull(plugin, "plugin cannot be null");
        Validate.notNull(placeholder, "placeholder cannot be null");

        String name = placeholder.getName();
        Validate.notNull(name, "placeholder's name cannot be null");
        if (placeholderRegistrationMap.containsKey(name))
            throw new RuntimeException("there is already a placeholder named \"" + name + "\" registered");

        placeholder.init(plugin);
        placeholderRegistrationMap.put(name, new PlaceholderRegistration(plugin, placeholder));
    }

    public void unregisterPlaceholder(Placeholder placeholder) {
        Iterator<Map.Entry<String, PlaceholderRegistration>> ite = placeholderRegistrationMap.entrySet().iterator();
        while (ite.hasNext()) {
            if (ite.next().getValue().getPlaceholder() == placeholder) {
                ite.remove();
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean player = (sender instanceof Player);

        if (args.length == 0 || (args.length > 1 && args.length < 5)) {
            sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Incorrect number of arguments.");
            sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] >" + (player ? ChatColor.GREEN : "") + "   /cc help");
            return false;
        }

        if (args.length == 1) {
            if (args[0].equals("help")) {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "--------=ConditionalCommands=--------");
                sender.sendMessage((player ? ChatColor.GREEN : "") + "  /cc help");
                sender.sendMessage((player ? ChatColor.GREEN : "") + "  /cc <player> unless \"" + (player ? ChatColor.LIGHT_PURPLE : "") + "condition"
                        + (player ? ChatColor.GREEN : "") + "\" do \"" + (player ? ChatColor.LIGHT_PURPLE : "") + "command" + (player ? ChatColor.GREEN : "") + "\"");
                sender.sendMessage((player ? ChatColor.GREEN : "") + "  /cc <player> if \"" + (player ? ChatColor.LIGHT_PURPLE : "") + "condition"
                        + (player ? ChatColor.GREEN : "") + "\" do \"" + (player ? ChatColor.LIGHT_PURPLE : "") + "command" + (player ? ChatColor.GREEN : "") + "\"");
                sender.sendMessage((player ? ChatColor.GRAY : "") + "e.g.");
                sender.sendMessage((player ? ChatColor.GREEN : "") + "  /cc konsolas unless -ping->100|-tps-<10.0 do kick konsolas");
                sender.sendMessage((player ? ChatColor.GRAY : "") + "Please note that conditions cannot include any spaces.");
                sender.sendMessage((player ? ChatColor.GRAY : "") + "For debug information, enable dev mode in the configuration file. ");
                sender.sendMessage((player ? ChatColor.GOLD : "") + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
            } else {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Incorrect subcommand.");
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] >" + (player ? ChatColor.GREEN : "") + "   /cc help");
            }
            return false;
        }

        // Sub command
        String action = args[1];

        // Get the player
        Player placeholderFor = Bukkit.getPlayer(args[0]);
        if (placeholderFor == null || !placeholderFor.isOnline()) {
            sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Not dispatching command because " + args[0] + " is not online...");
            return true;
        }

        // Get the condition
        String modifiedStr = args[2];
        for (PlaceholderRegistration placeholder : placeholderRegistrationMap.values()) {
            if (placeholder.getPlaceholder().shouldApply(modifiedStr)) {
                try {
                    modifiedStr = placeholder.getPlaceholder().doSubstitution(modifiedStr, placeholderFor);
                } catch (Exception ex) {
                    if (getConfig().getBoolean("dev")) {
                        sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Failed to apply a placeholder: " + ex.getMessage());
                        sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] >" + (player ? ChatColor.GREEN : "") + "   /cc help");
                        getLogger().severe("An error occurred whilst applying a placeholder.");
                        ex.printStackTrace();
                        return false;
                    } else {
                        sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Couldn't process placeholder.");
                        return true;
                    }
                }
            }
        }

        // Get the command
        StringBuilder command = new StringBuilder();
        for (int i = 4; i < args.length; i++) {
            command.append(args[i]).append(' ');
        }

        // Make sure there's a 'do' third.
        if (!args[3].equals("do")) {
            if (getConfig().getBoolean("dev")) {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Missing 'do' clause. Make sure the condition has no spaces.");
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] >" + (player ? ChatColor.GREEN : "") + "   /cc help");
                return false;
            } else {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Command has no action.");
                return true;
            }
        }

        // Parse the expression
        Expression expression;
        try {
            expression = new Expression(modifiedStr);
        } catch (Expression.ParseException ex) {
            if (getConfig().getBoolean("dev")) {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Failed to parse \"" + modifiedStr + "\": " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Roughly translated, that means you spelt something wrong or made a syntax error in the condition.");
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] >" + ChatColor.GREEN + "   /cc help");
            } else {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Couldn't process command (syntax error).");
            }
            return false;
        }

        if (getConfig().getBoolean("dev")) {
            getLogger().info("Successfully parsed expression: " + expression.toString());
        }

        switch (action) {
            case "unless":
                if (!expression.evaluate()) {
                    dispatchCommand(sender, command.toString());
                } else {
                    sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Not dispatching command because \"" + args[2] + "\" evaluated to true");
                }
                break;
            case "if":
                if (expression.evaluate()) {
                    dispatchCommand(sender, command.toString());
                } else {
                    sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Not dispatching command because \"" + args[2] + "\" evaluated to false");
                }
                break;
            default:
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Incorrect subcommand.");
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] >" + (player ? ChatColor.GREEN : "") + "   /cc help");
                return false;
        }

        return true;
    }

    private void dispatchCommand(final CommandSender sender, String command) {
        boolean player = (sender instanceof Player);
        // Delayed/multi command syntax.
        Matcher matcher = SPLIT_PATTERN.matcher(command);

        if (!matcher.find()) {
            protectedDispatch(sender, command);
        } else {
            try {
                int delay, cmdStart, cmdEnd;
                do {
                    delay = Integer.parseInt(matcher.group(1));
                    cmdStart = matcher.end();
                    if (!matcher.find()) {
                        cmdEnd = command.length();
                    } else {
                        cmdEnd = matcher.start();
                    }

                    final String cmd = command.substring(cmdStart, cmdEnd).trim();

                    if (getConfig().getBoolean("dev")) {
                        sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Will dispatch command \"" + cmd + "\" in " + delay + " ticks");
                    }

                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                        @Override
                        public void run() {
                            protectedDispatch(sender, cmd);
                        }
                    }, delay);
                } while (cmdEnd != command.length());
            } catch (NumberFormatException e) {
                if (getConfig().getBoolean("dev")) {
                    sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Invalid delay in the format /<delay>/: " + e.getMessage());
                    sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > for " + command);
                } else {
                    sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Couldn't process delayed command.");
                }
            }
        }
    }

    private void protectedDispatch(CommandSender sender, String command) {
        boolean player = (sender instanceof Player);
        try {
            sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > Dispatching command \"" + command + "\"");
            this.getServer().dispatchCommand(sender, command);
        } catch (CommandException ex) {
            if (getConfig().getBoolean("dev")) {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > An error occured whilst executing the command. The stack trace has been printed to the console.");
                this.getLogger().warning("ConditionalCommands executed this command on the main thread through the sender " + sender);
                this.getLogger().warning("The command string is: " + command);
                this.getLogger().warning("Stack trace follows: ");
                ex.printStackTrace();
            } else {
                sender.sendMessage((player ? ChatColor.GOLD : "") + "[ConditionalCommands] > There was a problem trying to run the command.");
            }
        }
    }
}