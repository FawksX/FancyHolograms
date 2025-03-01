package de.oliver.fancyholograms;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.oliver.fancyanalytics.api.FancyAnalyticsAPI;
import de.oliver.fancyanalytics.api.metrics.MetricSupplier;
import de.oliver.fancyanalytics.logger.ExtendedFancyLogger;
import de.oliver.fancyanalytics.logger.LogLevel;
import de.oliver.fancyanalytics.logger.appender.Appender;
import de.oliver.fancyanalytics.logger.appender.ConsoleAppender;
import de.oliver.fancyanalytics.logger.appender.JsonAppender;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramConfiguration;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.HologramStorage;
import de.oliver.fancyholograms.api.data.HologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import de.oliver.fancyholograms.commands.FancyHologramsCMD;
import de.oliver.fancyholograms.commands.FancyHologramsTestCMD;
import de.oliver.fancyholograms.commands.HologramCMD;
import de.oliver.fancyholograms.hologram.version.*;
import de.oliver.fancyholograms.listeners.PlayerListener;
import de.oliver.fancyholograms.listeners.WorldListener;
import de.oliver.fancyholograms.storage.FlatFileHologramStorage;
import de.oliver.fancylib.FancyLib;
import de.oliver.fancylib.Metrics;
import de.oliver.fancylib.VersionConfig;
import de.oliver.fancylib.serverSoftware.ServerSoftware;
import de.oliver.fancylib.versionFetcher.MasterVersionFetcher;
import de.oliver.fancylib.versionFetcher.VersionFetcher;
import de.oliver.fancysitula.api.IFancySitula;
import de.oliver.fancysitula.api.utils.ServerVersion;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class FancyHolograms implements FancyHologramsPlugin, ModInitializer {

    private static @Nullable FancyHolograms INSTANCE;
    private final Logger fancyLogger;
    private final VersionConfig versionConfig = new VersionConfig(this, versionFetcher);

    private final ScheduledExecutorService hologramThread = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("FancyHolograms-Holograms")
                    .build()
    );
    private final ExecutorService fileStorageExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setPriority(Thread.MIN_PRIORITY + 1)
                    .setNameFormat("FancyHolograms-FileStorageExecutor")
                    .build()
    );

    private HologramConfiguration configuration = new FancyHologramsConfiguration();
    private HologramStorage hologramStorage = new FlatFileHologramStorage();
    private @Nullable HologramManagerImpl hologramsManager;

    public FancyHolograms() {
        INSTANCE = this;

        Appender consoleAppender = new ConsoleAppender("[{loggerName}] ({threadName}) {logLevel}: {message}");
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
        File logsFile = new File("plugins/FancyHolograms/logs/FH-logs-" + date + ".txt");
        if (!logsFile.exists()) {
            try {
                logsFile.getParentFile().mkdirs();
                logsFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        JsonAppender jsonAppender = new JsonAppender(false, false, true, logsFile.getPath());
        this.fancyLogger = new ExtendedFancyLogger("FancyHolograms", LogLevel.INFO, List.of(consoleAppender, jsonAppender), new ArrayList<>());
    }

    public static @NotNull FancyHolograms get() {
        return Objects.requireNonNull(INSTANCE, "plugin is not initialized");
    }

    public static boolean canGet() {
        return INSTANCE != null;
    }

    @Override
    public void onLoad() {
        final var adapter = resolveHologramAdapter();

        hologramsManager = new HologramManagerImpl(this, adapter);

        fancyLogger.info("Successfully loaded FancyHolograms version %s".formatted(getDescription().getVersion()));
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onEnable);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onDisable);
    }


    public void onEnable(MinecraftServer server) {
        getHologramConfiguration().reload(this); // initialize configuration

        LogLevel logLevel;
        try {
            logLevel = LogLevel.valueOf(getHologramConfiguration().getLogLevel());
        } catch (IllegalArgumentException e) {
            logLevel = LogLevel.INFO;
        }
        fancyLogger.setCurrentLevel(logLevel);

        reloadCommands();

        WorldListener.init();
        PlayerListener.init(this);

        versionConfig.load();

        getHologramsManager().initializeTasks();

        if (getHologramConfiguration().isAutosaveEnabled()) {
            getHologramThread().scheduleAtFixedRate(() -> {
                if (hologramsManager != null) {
                    hologramsManager.saveHolograms();
                }
            }, getHologramConfiguration().getAutosaveInterval(), getHologramConfiguration().getAutosaveInterval() * 60L, TimeUnit.SECONDS);
        }

        fancyLogger.info("Successfully enabled FancyHolograms version %s".formatted(getDescription().getVersion()));
    }

    public void onDisable(MinecraftServer server) {
        hologramsManager.saveHolograms();
        hologramThread.shutdown();
        fileStorageExecutor.shutdown();
        INSTANCE = null;

        fancyLogger.info("Successfully disabled FancyHolograms version %s".formatted(getDescription().getVersion()));
    }

    @Override
    public Logger getFancyLogger() {
        return fancyLogger;
    }

    public @NotNull VersionConfig getVersionConfig() {
        return versionConfig;
    }

    @ApiStatus.Internal
    public @NotNull HologramManagerImpl getHologramsManager() {
        return Objects.requireNonNull(this.hologramsManager, "plugin is not initialized");
    }

    @Override
    public HologramManager getHologramManager() {
        return Objects.requireNonNull(this.hologramsManager, "plugin is not initialized");
    }

    @Override
    public HologramConfiguration getHologramConfiguration() {
        return configuration;
    }

    @Override
    public void setHologramConfiguration(HologramConfiguration configuration, boolean reload) {
        this.configuration = configuration;

        if (reload) {
            configuration.reload(this);
            reloadCommands();
        }
    }

    @Override
    public HologramStorage getHologramStorage() {
        return hologramStorage;
    }

    @Override
    public void setHologramStorage(HologramStorage storage, boolean reload) {
        this.hologramStorage = storage;

        if (reload) {
            getHologramsManager().reloadHolograms();
        }
    }

    public ScheduledExecutorService getHologramThread() {
        return hologramThread;
    }

    public ExecutorService getFileStorageExecutor() {
        return this.fileStorageExecutor;
    }

    private @NotNull Function<HologramData, Hologram> resolveHologramAdapter() {
        return Hologram::new;
    }

    public void reloadCommands() {
        Collection<Command> commands = Arrays.asList(new HologramCMD(this), new FancyHologramsCMD(this));

        if (getHologramConfiguration().isRegisterCommands()) {
            commands.forEach(command -> getServer().getCommandMap().register("fancyholograms", command));
        } else {
            commands.stream().filter(Command::isRegistered).forEach(command ->
                    command.unregister(getServer().getCommandMap()));
        }

        if (false) {
            FancyHologramsTestCMD fancyHologramsTestCMD = new FancyHologramsTestCMD(this);
            getServer().getCommandMap().register("fancyholograms", fancyHologramsTestCMD);
        }
    }
}
