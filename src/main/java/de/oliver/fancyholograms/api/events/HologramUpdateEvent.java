package de.oliver.fancyholograms.api.events;

import de.oliver.fancyholograms.api.hologram.Hologram;
import de.oliver.fancyholograms.api.data.*;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Called when a hologram is being updated, the data in the hologram is current and the event holds the new data
 */
public interface HologramUpdateEvent {
    Event<HologramUpdateEvent> EVENT = EventFactory.createArrayBacked(HologramUpdateEvent.class,
            listeners -> (hologram, player, data, modification) -> {
                for (HologramUpdateEvent listener : listeners) {
                    if(!listener.onEvent(hologram, player, data, modification)) {
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
    boolean onEvent(Hologram hologram, ServerPlayer serverPlayer, HologramData hologramData, HologramModification modification);

    /**
     * Represents the various types of modifications that can be made to a Hologram.
     */
    enum HologramModification {
        TEXT,
        POSITION,
        SCALE,
        TRANSLATION,
        BILLBOARD,
        BACKGROUND,
        TEXT_SHADOW,
        TEXT_ALIGNMENT,
        SEE_THROUGH,
        SHADOW_RADIUS,
        SHADOW_STRENGTH,
        UPDATE_TEXT_INTERVAL,
        UPDATE_VISIBILITY_DISTANCE;
    }
}