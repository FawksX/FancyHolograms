package de.oliver.fancyholograms.api.events;

import de.oliver.fancyholograms.api.hologram.Hologram;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Called when a hologram is being deleted, any hologram data changed will be reflected in the hologram if
 * the event is called
 */
public interface HologramDeleteEvent {
    Event<HologramDeleteEvent> EVENT = EventFactory.createArrayBacked(HologramDeleteEvent.class,
            listeners -> (hologram, player) -> {
                for (HologramDeleteEvent listener : listeners) {
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
