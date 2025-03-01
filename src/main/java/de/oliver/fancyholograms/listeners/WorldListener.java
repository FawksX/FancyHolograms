package de.oliver.fancyholograms.listeners;

import de.oliver.fancyholograms.FancyHolograms;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class WorldListener {

    public static void init() {
        ServerWorldEvents.LOAD.register(WorldListener::onWorldLoad);
        ServerWorldEvents.UNLOAD.register(WorldListener::onWorldUnload);
    }

    private static void onWorldLoad(MinecraftServer var1, ServerLevel var2) {
        FancyHolograms.get().getHologramThread().submit(() -> {
            FancyHolograms.get().getFancyLogger().info("Loading holograms for world " + var2.dimension().location());
            FancyHolograms.get().getHologramsManager().loadHolograms(var2.dimension().location().toString());
        });
    }

    private static void onWorldUnload(MinecraftServer var1, ServerLevel level) {
        FancyHolograms.get().getHologramThread().submit(() -> {
            FancyHolograms.get().getFancyLogger().info("Unloading holograms for world " + level.dimension().location());
            FancyHolograms.get().getHologramsManager().unloadHolograms(level.dimension().location().toString());
        });
    }

}
