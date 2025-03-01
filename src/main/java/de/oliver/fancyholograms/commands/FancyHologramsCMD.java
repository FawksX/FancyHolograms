package de.oliver.fancyholograms.commands;

import de.oliver.fancyholograms.FancyHolograms;
import de.oliver.fancyholograms.util.Constants;
import de.oliver.fancylib.MessageHelper;
import de.oliver.fancylib.translations.message.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class FancyHologramsCMD extends Command {

    @NotNull
    private final FancyHolograms plugin;

    public FancyHologramsCMD(@NotNull final FancyHolograms plugin) {
        super("fancyholograms");
        setPermission("fancyholograms.admin");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }

        if (args.length < 1) {
            MessageHelper.info(sender, Constants.FH_COMMAND_USAGE);
            return false;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> {
                this.plugin.getHologramsManager().saveHolograms();
                MessageHelper.success(sender, "Saved all holograms");
            }
            case "reload" -> {
                this.plugin.getHologramConfiguration().reload(plugin);
                this.plugin.getHologramsManager().reloadHolograms();
                this.plugin.reloadCommands();

                MessageHelper.success(sender, "Reloaded config and holograms");
            }
            case "version" -> {
                FancyHolograms.get().getHologramThread().submit(() -> {
                    FancyHolograms.get().getVersionConfig().checkVersionAndDisplay(sender, false);
                });
            }
            default -> {
                MessageHelper.info(sender, Constants.FH_COMMAND_USAGE);
                return false;
            }
        }

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length < 1) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("version", "reload", "save", "convert"));
        } else {
        }

        String lastArgument = args[args.length - 1];

        return suggestions.stream()
            .filter(alias -> alias.startsWith(lastArgument.toLowerCase(Locale.ROOT)))
            .toList();
    }
}
