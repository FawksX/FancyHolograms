package de.oliver.fancyholograms.api;

import de.oliver.fancyholograms.FancyHolograms;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.util.concurrent.ScheduledExecutorService;

public interface FancyHologramsPlugin {

    static FancyHologramsPlugin get() {
        if (isEnabled()) {
            return EnabledChecker.getPlugin();
        }

        throw new NullPointerException("Plugin is not enabled");
    }

    static boolean isEnabled() {
        return EnabledChecker.isFancyHologramsEnabled();
    }

    Logger getFancyLogger();

    HologramManager getHologramManager();

    /**
     * Returns the configuration of the plugin.
     *
     * @return The configuration.
     */
    HologramConfiguration getHologramConfiguration();

    /**
     * Sets the configuration of the plugin.
     *
     * @param configuration The new configuration.
     * @param reload        Whether the configuration should be reloaded.
     */
    void setHologramConfiguration(HologramConfiguration configuration, boolean reload);

    /**
     * @return The hologram storage.
     */
    HologramStorage getHologramStorage();

    /**
     * @return The hologram thread
     */
    ScheduledExecutorService getHologramThread();

    /**
     * Sets the hologram storage.
     *
     * @param storage The new hologram storage.
     * @param reload  Whether the current hologram cache should be reloaded.
     */
    void setHologramStorage(HologramStorage storage, boolean reload);

    class EnabledChecker {

        private static Boolean enabled;
        private static FancyHologramsPlugin plugin;

        public static Boolean isFancyHologramsEnabled() {
            return FabricLoader.getInstance().isModLoaded("fancyholograms");
        }

        public static FancyHologramsPlugin getPlugin() {
            return FancyHolograms.get();
        }
    }
}
