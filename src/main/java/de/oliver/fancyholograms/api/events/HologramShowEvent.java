package de.oliver.fancyholograms.api.events;

import de.oliver.fancyholograms.api.hologram.Hologram;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Called when a hologram is being shown to a player
 */
public interface HologramShowEvent {
    Event<HologramShowEvent> EVENT = EventFactory.createArrayBacked(HologramShowEvent.class,
            listeners -> (hologram, player) -> {
                for (HologramShowEvent listener : listeners) {
                    if(!listener.onEvent(hologram, player)) {
                        return false;
                    }
                }

                return true;
            });

    /**
     * If false, cancelled
     * @param hologram
     * @param serverPlayer
     * @return
     */
    boolean onEvent(Hologram hologram, ServerPlayer serverPlayer);
}