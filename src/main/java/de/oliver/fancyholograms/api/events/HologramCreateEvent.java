package de.oliver.fancyholograms.api.events;

import de.oliver.fancyholograms.api.hologram.Hologram;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Called when a hologram is being created, any hologram data changed will be reflected in the new hologram
 */
public interface HologramCreateEvent {
    Event<HologramCreateEvent> EVENT = EventFactory.createArrayBacked(HologramCreateEvent.class,
            listeners -> (hologram, player) -> {
                for (HologramCreateEvent listener : listeners) {
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