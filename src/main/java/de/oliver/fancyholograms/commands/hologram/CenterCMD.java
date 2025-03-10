package de.oliver.fancyholograms.commands.hologram;

import de.oliver.fancyholograms.FancyHolograms;
import de.oliver.fancyholograms.api.hologram.Hologram;
import de.oliver.fancyholograms.commands.Subcommand;
import de.oliver.fancyholograms.util.Constants;
import de.oliver.fancylib.MessageHelper;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CenterCMD implements Subcommand {
    @Override
    public List<String> tabcompletion(@NotNull CommandSender player, @Nullable Hologram hologram, @NotNull String[] args) {
        return null;
    }

    @Override
    public boolean run(@NotNull CommandSender player, @Nullable Hologram hologram, @NotNull String[] args) {

        if (!(player.hasPermission("fancyholograms.hologram.edit.center"))) {
            MessageHelper.error(player, "You don't have the required permission to center a hologram");
            return false;
        }

        Location location = hologram.getData().getLocation();

        location.set(
            Math.floor(location.x()) + 0.5,
            location.y(),
            Math.floor(location.z()) + 0.5
        );

        hologram.getData().setLocation(location);

        if (FancyHolograms.get().getHologramConfiguration().isSaveOnChangedEnabled()) {
            FancyHolograms.get().getHologramStorage().save(hologram);
        }

        MessageHelper.success(player, "Centered the hologram to %s/%s/%s %s\u00B0 %s\u00B0".formatted(
            Constants.COORDINATES_DECIMAL_FORMAT.format(location.x()),
            Constants.COORDINATES_DECIMAL_FORMAT.format(location.y()),
            Constants.COORDINATES_DECIMAL_FORMAT.format(location.z()),
            Constants.COORDINATES_DECIMAL_FORMAT.format((location.getYaw() + 180f) % 360f),
            Constants.COORDINATES_DECIMAL_FORMAT.format((location.getPitch()) % 360f)
        ));
        return true;
    }
}
