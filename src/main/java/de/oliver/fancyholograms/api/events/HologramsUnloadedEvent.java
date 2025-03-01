package de.oliver.fancyholograms.api.events;

import com.google.common.collect.ImmutableList;
import de.oliver.fancyholograms.api.hologram.Hologram;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface HologramsUnloadedEvent {
    Event<HologramsUnloadedEvent> EVENT = EventFactory.createArrayBacked(HologramsUnloadedEvent.class,
            listeners -> (holograms) -> {
                for (HologramsUnloadedEvent listener : listeners) {
                    listener.onEvent(holograms);
                }
            });

    void onEvent(ImmutableList<Hologram> holograms);
}
