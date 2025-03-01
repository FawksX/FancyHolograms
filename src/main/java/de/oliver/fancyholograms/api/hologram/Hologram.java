package de.oliver.fancyholograms.api.hologram;

import com.mojang.math.Transformation;
import de.oliver.fancyholograms.api.data.BlockHologramData;
import de.oliver.fancyholograms.api.data.DisplayHologramData;
import de.oliver.fancyholograms.api.data.HologramData;
import de.oliver.fancyholograms.api.data.ItemHologramData;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.data.property.Visibility;
import de.oliver.fancyholograms.api.events.HologramHideEvent;
import de.oliver.fancyholograms.api.events.HologramShowEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.bukkit.Color;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.lushplugins.chatcolorhandler.ModernChatColorHandler;

import java.util.*;

/**
 * Abstract base class for creating, updating, and managing holograms.
 * <p>
 * This class provides the basic functionality needed to work with holograms
 * across multiple versions of Minecraft. To create a hologram specific to a version of Minecraft,
 * extend this class and implement the abstract methods.
 * <p>
 * Note that the specific way holograms are created, updated, and deleted
 * will vary depending on the Minecraft version.
 * <p>
 * A Hologram object includes data about the hologram and maintains a set of players to whom the hologram is shown.
 */
public class Hologram {

    public static final int LINE_WIDTH = 1000;
    public static final Color TRANSPARENT = Color.fromARGB(0);
    protected static final int MINIMUM_PROTOCOL_VERSION = 762;

    protected final @NotNull HologramData data;
    /**
     * Set of UUIDs of players to whom the hologram is currently shown.
     */
    protected final @NotNull Set<UUID> viewers = new HashSet<>();

    @Nullable
    private Display display;

    public Hologram(@NotNull final HologramData data) {
        this.data = data;
    }

    @NotNull
    public String getName() {
        return data.getName();
    }

    public final @NotNull HologramData getData() {
        return this.data;
    }

    /**
     * Returns the entity id of this hologram
     * This id is for packet use only as the entity is not registered to the server
     * @return entity id
     */
    public int getEntityId() {
        return display.getId();
    }

    /**
     * Returns the Display entity of this Hologram object.
     * The entity is not registered in the world or server.
     * Only use this method if you know what you're doing.
     * <p>
     * This method will return <code>null</code> in 1.20.5 and newer versions
     *
     * @return the Display entity of this Hologram object
     */
    @ApiStatus.Internal
    @Deprecated(forRemoval = true, since = "2.4.1")
    public @Nullable Display getDisplayEntity() {
        return display;
    }

    protected void create() {
        final var location = data.getLocation();
        if (!location.isWorldLoaded()) {
            return; // no location data, cannot be created
        }

        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();

        switch (data.getType()) {
            case TEXT -> this.display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, world);
            case BLOCK -> this.display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, world);
            case ITEM -> this.display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
        }

        if (data instanceof DisplayHologramData dd) {
            display.getEntityData().set(Display.DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, dd.getInterpolationDuration());
            display.getEntityData().set(Display.DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
        }

        update();
    }

    protected void delete() {
        this.display = null;
    }

    protected void update() {
        final var display = this.display;
        if (display == null) {
            return; // doesn't exist, nothing to update
        }

        // location data
        final var location = data.getLocation();
        if (location.getWorld() == null || !location.isWorldLoaded()) {
            return;
        } else {
            display.setPosRaw(location.x(), location.y(), location.z());
            display.setYRot(location.getYaw());
            display.setXRot(location.getPitch());
        }

        if (display instanceof Display.TextDisplay textDisplay && data instanceof TextHologramData textData) {
            // line width
            display.getEntityData().set(Display.TextDisplay.DATA_LINE_WIDTH_ID, Hologram.LINE_WIDTH);

            // background
            final var background = textData.getBackground();
            if (background == null) {
                display.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, Display.TextDisplay.INITIAL_BACKGROUND);
            } else if (background == Hologram.TRANSPARENT) {
                display.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, 0);
            } else {
                display.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, background.asARGB());
            }

            // text shadow
            if (textData.hasTextShadow()) {
                textDisplay.setFlags((byte) (textDisplay.getFlags() | Display.TextDisplay.FLAG_SHADOW));
            } else {
                textDisplay.setFlags((byte) (textDisplay.getFlags() & ~Display.TextDisplay.FLAG_SHADOW));
            }

            // text alignment
            if (textData.getTextAlignment() == org.bukkit.entity.TextDisplay.TextAlignment.LEFT) {
                textDisplay.setFlags((byte) (textDisplay.getFlags() | Display.TextDisplay.FLAG_ALIGN_LEFT));
            } else {
                textDisplay.setFlags((byte) (textDisplay.getFlags() & ~Display.TextDisplay.FLAG_ALIGN_LEFT));
            }

            // see through
            if (textData.isSeeThrough()) {
                textDisplay.setFlags((byte) (textDisplay.getFlags() | Display.TextDisplay.FLAG_SEE_THROUGH));
            } else {
                textDisplay.setFlags((byte) (textDisplay.getFlags() & ~Display.TextDisplay.FLAG_SEE_THROUGH));
            }

            if (textData.getTextAlignment() == org.bukkit.entity.TextDisplay.TextAlignment.RIGHT) {
                textDisplay.setFlags((byte) (textDisplay.getFlags() | Display.TextDisplay.FLAG_ALIGN_RIGHT));
            } else {
                textDisplay.setFlags((byte) (textDisplay.getFlags() & ~Display.TextDisplay.FLAG_ALIGN_RIGHT));
            }

        } else if (display instanceof Display.ItemDisplay itemDisplay && data instanceof ItemHologramData itemData) {
            // item
            itemDisplay.setItemStack(itemData.getItemStack());

        } else if (display instanceof Display.BlockDisplay blockDisplay && data instanceof BlockHologramData blockData) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:" + blockData.getBlock().name().toLowerCase(), ':'));
            blockDisplay.setBlockState(block.defaultBlockState());
        }

        if (data instanceof DisplayHologramData displayData) {
            // billboard data
            display.setBillboardConstraints(displayData.getBillboard());

            // brightness
            if (displayData.getBrightness() != null) {
                display.setBrightnessOverride(new Brightness(displayData.getBrightness().block(), displayData.getBrightness().sky()));
            }

            // entity scale AND MORE!
            display.setTransformation(new Transformation(
                    displayData.getTranslation(),
                    new Quaternionf(),
                    displayData.getScale(),
                    new Quaternionf())
            );

            // entity shadow
            display.setShadowRadius(displayData.getShadowRadius());
            display.setShadowStrength(displayData.getShadowStrength());

            // view range
            display.setViewRange(displayData.getVisibilityDistance());
        }
    }

    public boolean show(@NotNull final ServerPlayer player) {
        if (!HologramShowEvent.EVENT.invoker().onEvent(this, player)) {
            return false;
        }

        if (this.display == null) {
            create(); // try to create it if it doesn't exist every time
        }

        final var display = this.display;
        if (display == null) {
            return false; // could not be created, nothing to show
        }

        if (!data.getLocation().getWorld().getName().equals(player.getLocation().getWorld().getName())) {
            return false;
        }

        // TODO: cache player protocol version
        // TODO: fix this
//        final var protocolVersion = FancyHologramsPlugin.get().isUsingViaVersion() ? Via.getAPI().getPlayerVersion(player.getUniqueId()) : MINIMUM_PROTOCOL_VERSION;
//        if (protocolVersion < MINIMUM_PROTOCOL_VERSION) {
//            System.out.println("nope protocol");
//            return false;
//        }

        player.connection.send(new ClientboundAddEntityPacket(display));
        this.viewers.add(player.getUUID());
        refreshHologram(player);

        return true;
    }

    public boolean hide(@NotNull final ServerPlayer player) {
        if (!HologramHideEvent.EVENT.invoker().onEvent(this, player)) {
            return false;
        }

        final var display = this.display;
        if (display == null) {
            return false; // doesn't exist, nothing to hide
        }

        player.connection.send(new ClientboundRemoveEntitiesPacket(display.getId()));

        this.viewers.remove(player.getUUID());
        return true;
    }

    public void refresh(@NotNull final ServerPlayer player) {
        final var display = this.display;
        if (display == null) {
            return; // doesn't exist, nothing to refresh
        }

        if (!isViewer(player)) {
            return;
        }

        player.connection.send(new ClientboundTeleportEntityPacket(display));

        if (display instanceof Display.TextDisplay textDisplay) {
            textDisplay.setText(PaperAdventure.asVanilla(getShownText(player)));
        }

        final var values = new ArrayList<SynchedEntityData.DataValue<?>>();

        //noinspection unchecked
        for (final var item : ((Int2ObjectMap<SynchedEntityData.DataItem<?>>) getValue(display.getEntityData(), "e")).values()) {
            values.add(item.value());
        }

        player.connection.send(new ClientboundSetEntityDataPacket(display.getId(), values));
    }

    /**
     * Create the hologram entity.
     * Only run this if creating custom Hologram implementations as this is run in
     * {@link de.oliver.fancyholograms.api.HologramManager#create(HologramData)}.
     */
    public final void createHologram() {
        create();
    }

    /**
     * Deletes the hologram entity.
     */
    public final void deleteHologram() {
        delete();
    }

    /**
     * Shows the hologram to a collection of players.
     * Use {@link #forceShowHologram(ServerPlayer)} if this hologram is not registered to the HologramManager.
     *
     * @param players The players to show the hologram to
     */
    public final void showHologram(Collection<? extends ServerPlayer> players) {
        players.forEach(this::showHologram);
    }

    /**
     * Shows the hologram to a player.
     * Use {@link #forceShowHologram(ServerPlayer)} if this hologram is not registered to the HologramManager.
     *
     * @param player The player to show the hologram to
     */
    public final void showHologram(ServerPlayer player) {
        viewers.add(player.getUUID());
    }

    /**
     * Forcefully shows the hologram to a player.
     *
     * @param player The player to show the hologram to
     */
    public final void forceShowHologram(ServerPlayer player) {
        show(player);

        if (this.getData().getVisibility().equals(Visibility.MANUAL)) {
            Visibility.ManualVisibility.addDistantViewer(this, player.getUUID());
        }
    }

    /**
     * Hides the hologram from a collection of players.
     * Use {@link #forceHideHologram(ServerPlayer)} if this hologram is not registered to the HologramManager.
     *
     * @param players The players to hide the hologram from
     */
    public final void hideHologram(Collection<? extends ServerPlayer> players) {
        players.forEach(this::hideHologram);
    }

    /**
     * Hides the hologram from a player.
     * Use {@link #forceHideHologram(ServerPlayer)} if this hologram is not registered to the HologramManager.
     *
     * @param player The player to hide the hologram from
     */
    public final void hideHologram(ServerPlayer player) {
        viewers.remove(player.getUUID());
    }

    /**
     * Forcefully hides the hologram from a player.
     *
     * @param player The player to show the hologram to
     */
    public final void forceHideHologram(ServerPlayer player) {
        hide(player);

        if (this.getData().getVisibility().equals(Visibility.MANUAL)) {
            Visibility.ManualVisibility.removeDistantViewer(this, player.getUUID());
        }
    }

    /**
     * Queues hologram to update and refresh for players.
     *
     * @deprecated in favour of {@link #queueUpdate()}
     */
    @Deprecated(forRemoval = true)
    public final void updateHologram() {
        queueUpdate();
    }

    /**
     * Queues hologram to update and refresh for players
     * Use {@link #forceUpdate()} if this hologram is not registered to the HologramManager.
     */
    public final void queueUpdate() {
        data.setHasChanges(true);
    }

    /**
     * Forcefully updates and refreshes hologram for players.
     */
    public final void forceUpdate() {
        update();
    }

    /**
     * Refreshes the hologram for the players currently viewing it.
     */
    public void refreshForViewers() {
        final var players = getViewers()
                .stream()
                .map(Bukkit::getPlayer)
                .toList();

        refreshHologram(players);
    }

    /**
     * Refreshes the hologram for players currently viewing it in the same world as the hologram.
     */
    public void refreshForViewersInWorld() {
        World world = data.getLocation().getWorld();
        final var players = getViewers()
                .stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.getWorld().equals(world))
                .toList();

        refreshHologram(players);
    }

    /**
     * Refreshes the hologram's data for a player.
     *
     * @param player the player to refresh for
     */
    public final void refreshHologram(@NotNull final ServerPlayer player) {
        refresh(player);
    }

    /**
     * Refreshes the hologram's data for a collection of players.
     *
     * @param players the collection of players to refresh for
     */
    public final void refreshHologram(@NotNull final Collection<? extends ServerPlayer> players) {
        players.forEach(this::refreshHologram);
    }

    /**
     * @return a copy of the set of UUIDs of players currently viewing the hologram
     */
    public final @NotNull Set<UUID> getViewers() {
        return new HashSet<>(this.viewers);
    }

    /**
     * @param player the player to check for
     * @return whether the player is currently viewing the hologram
     */
    public final boolean isViewer(@NotNull final ServerPlayer player) {
        return isViewer(player.getUUID());
    }

    /**
     * @param player the uuid of the player to check for
     * @return whether the player is currently viewing the hologram
     */
    public final boolean isViewer(@NotNull final UUID player) {
        return this.viewers.contains(player);
    }

    protected boolean shouldShowTo(@NotNull final ServerPlayer player) {
        if (!meetsVisibilityConditions(player)) {
            return false;
        }

        return isWithinVisibilityDistance(player);
    }

    public boolean meetsVisibilityConditions(@NotNull final ServerPlayer player) {
        return this.getData().getVisibility().canSee(player, this);
    }

    public boolean isWithinVisibilityDistance(@NotNull final ServerPlayer player) {
        final var location = getData().getLocation();
        if (!location.getWorld().equals(player.getWorld())) {
            return false;
        }

        int visibilityDistance = data.getVisibilityDistance();
        double distanceSquared = location.distanceSquared(player.getLocation());

        return distanceSquared <= visibilityDistance * visibilityDistance;
    }

    /**
     * Checks and updates the shown state for a player.
     * If the hologram is shown and should not be, it hides it.
     * If the hologram is not shown and should be, it shows it.
     * Use {@link #forceUpdateShownStateFor(ServerPlayer)} if this hologram is not registered to the HologramManager.
     *
     * @param player the player to check and update the shown state for
     */
    public void updateShownStateFor(ServerPlayer player) {
        boolean isShown = isViewer(player);
        boolean shouldBeShown = shouldShowTo(player);

        if (isShown && !shouldBeShown) {
            showHologram(player);
        } else if (!isShown && shouldBeShown) {
            hideHologram(player);
        }
    }

    /**
     * Checks and forcefully updates the shown state for a player.
     * If the hologram is shown and should not be, it hides it.
     * If the hologram is not shown and should be, it shows it.
     *
     * @param player the player to check and update the shown state for
     */
    public void forceUpdateShownStateFor(ServerPlayer player) {
        boolean isShown = isViewer(player);

        if (meetsVisibilityConditions(player)) {
            if (isWithinVisibilityDistance(player)) {
                // Ran if the player meets the visibility conditions and is within visibility distance
                if (!isShown) {
                    show(player);

                    if (getData().getVisibility().equals(Visibility.MANUAL)) {
                        Visibility.ManualVisibility.removeDistantViewer(this, player.getUUID());
                    }
                }
            } else {
                // Ran if the player meets the visibility conditions but is not within visibility distance
                if (isShown) {
                    hide(player);

                    if (getData().getVisibility().equals(Visibility.MANUAL)) {
                        Visibility.ManualVisibility.addDistantViewer(this, player.getUUID());
                    }
                }
            }
        } else {
            // Ran if the player does not meet visibility conditions
            if (isShown) {
                hide(player);
            }
        }
    }

    /**
     * Gets the text shown in the hologram. If a player is specified, placeholders in the text are replaced
     * with their corresponding values for the player.
     *
     * @param player the player to get the placeholders for, or null if no placeholders should be replaced
     * @return the text shown in the hologram
     */
    public final Component getShownText(@Nullable final ServerPlayer player) {
        if (!(getData() instanceof TextHologramData textData)) {
            return null;
        }

        var text = String.join("\n", textData.getText());

        return ModernChatColorHandler.translate(text, player);
    }

    @Override
    public final boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (!(o instanceof Hologram that)) return false;

        return Objects.equals(this.getData(), that.getData());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.getData());
    }
}
