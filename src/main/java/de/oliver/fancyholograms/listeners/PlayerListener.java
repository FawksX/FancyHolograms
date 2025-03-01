package de.oliver.fancyholograms.listeners;

import de.oliver.fancyholograms.FancyHolograms;
import de.oliver.fancyholograms.api.hologram.Hologram;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

public final class PlayerListener {

    private static FancyHolograms plugin;

    private static Map<UUID, List<UUID>> loadingResourcePacks;

    public static void init(@NotNull FancyHolograms plugin) {
        PlayerListener.plugin = plugin;
        PlayerListener.loadingResourcePacks = new HashMap<>();

        ServerPlayConnectionEvents.JOIN.register(PlayerListener::onJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(PlayerListener::onQuit);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(PlayerListener::onWorldChange);
    }

    private static void onJoin(ServerGamePacketListenerImpl var1, PacketSender var2, MinecraftServer var3) {

        var player = var1.getPlayer();

        for (final var hologram : plugin.getHologramsManager().getHolograms()) {
            hologram.updateShownStateFor(player);
        }

//        if (!plugin.getHologramConfiguration().areVersionNotificationsMuted() && Permissions.check(player, "fancyholograms.admin")) {
//            FancyHolograms.get().getHologramThread().submit(() -> FancyHolograms.get().getVersionConfig().checkVersionAndDisplay(player, true));
//        }
    }

    private static void onQuit(ServerGamePacketListenerImpl var1, MinecraftServer var2) {
        FancyHolograms.get().getHologramThread().submit(() -> {
            for (final var hologram : plugin.getHologramsManager().getHolograms()) {
                hologram.hideHologram(var1.getPlayer());
            }
        });
    }

    // TODO --> Find out if this is needed
    // This should just be okay with the HologramManager

//    @EventHandler(priority = EventPriority.MONITOR)
//    private static void onTeleport(@NotNull final PlayerTeleportEvent event) {
//        for (final Hologram hologram : plugin.getHologramsManager().getHolograms()) {
//            hologram.updateShownStateFor(event.getPlayer());
//        }
//    }

    private static void onWorldChange(ServerPlayer var1, ServerLevel var2, ServerLevel var3) {
        for (final Hologram hologram : plugin.getHologramsManager().getHolograms()) {
            hologram.updateShownStateFor(var1);
        }
    }

    // todo -->
    // Disabled for now as is maybe not needed

//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onResourcePackStatus(@NotNull final PlayerResourcePackStatusEvent event) {
//        // Skipping event calls before player has fully loaded to the server.
//        // This should fix NPE due to vanillaPlayer.connection being null when sending resource-packs in the configuration stage.
//        if (!event.getPlayer().isOnline())
//            return;
//        final UUID playerUniqueId = event.getPlayer().getUniqueId();
//        final UUID packUniqueId = getResourcePackID(event);
//        // Adding accepted resource-pack to the list of currently loading resource-packs for that player.
//        if (event.getStatus() == Status.ACCEPTED)
//            loadingResourcePacks.computeIfAbsent(playerUniqueId, (___) -> new ArrayList<>()).add(packUniqueId);
//        // Once successfully loaded (or failed to download), removing resource-pack from the map.
//        else if (event.getStatus() == Status.SUCCESSFULLY_LOADED || event.getStatus() == Status.FAILED_DOWNLOAD) {
//            loadingResourcePacks.computeIfAbsent(playerUniqueId, (___) -> new ArrayList<>()).removeIf(uuid -> uuid.equals(packUniqueId));
//            // Refreshing holograms once (possibly) all resource-packs are loaded.
//            if (loadingResourcePacks.get(playerUniqueId) != null && loadingResourcePacks.get(playerUniqueId).isEmpty()) {
//                // Removing player from the map, as they're no longer needed here.
//                loadingResourcePacks.remove(playerUniqueId);
//                // Refreshing holograms as to make sure custom textures are loaded.
//                for (final Hologram hologram : this.plugin.getHologramsManager().getHolograms()) {
//                    hologram.refreshHologram(event.getPlayer());
//                }
//            }
//        }
//    }
//
//    // For 1.20.2 and higher this method returns actual pack identifier, while for older versions, the identifier is a dummy UUID full of zeroes.
//    // Versions prior 1.20.2 supports sending and receiving only one resource-pack and a dummy, constant identifier can be used as a key.
//    private static @NotNull UUID getResourcePackID(final @NotNull PlayerResourcePackStatusEvent event) {
//        try {
//            event.getClass().getMethod("getID");
//            return event.getID();
//        } catch (final @NotNull NoSuchMethodException e) {
//            return new UUID(0,0);
//        }
//    }

}
