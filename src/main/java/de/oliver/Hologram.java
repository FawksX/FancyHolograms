package de.oliver;

import com.mojang.math.Transformation;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class Hologram {

    private final String name;
    private Location location;
    private List<String> lines;
    private Display.BillboardConstraints billboard;
    private float scale;
    private ChatFormatting background;

    private Display.TextDisplay entity;
    private boolean isDirty;

    public Hologram(String name, Location location, List<String> lines, Display.BillboardConstraints billboard, float scale, ChatFormatting background) {
        this.name = name;
        this.location = location;
        this.lines = lines;
        this.billboard = billboard;
        this.scale = scale;
        this.background = background;
        this.isDirty = false;
    }

    public void create(){
        Level level = ((CraftWorld) location.getWorld()).getHandle();
        entity = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);

        FancyHolograms.getInstance().getHologramManager().addHologram(this);
    }

    public void delete(){
        FancyHolograms.getInstance().getHologramManager().removeHologram(this);
        entity = null;
    }

    public void spawn(ServerPlayer serverPlayer){
        if(entity == null){
            create();
        }

        ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(entity);
        serverPlayer.connection.send(addEntityPacket);

        updateLocation(serverPlayer);
        updateText(serverPlayer);
        updateBillboard(serverPlayer);
        updateScale(serverPlayer);
        if(background != null){
            updateBackground(serverPlayer);
        }
    }

    public void remove(ServerPlayer serverPlayer) {
        ClientboundRemoveEntitiesPacket removeEntitiesPacket = new ClientboundRemoveEntitiesPacket(entity.getId());
        serverPlayer.connection.send(removeEntitiesPacket);
    }

    public void updateText(ServerPlayer serverPlayer){
        entity.setText(getText());

        if(serverPlayer != null) {
            entity.getEntityData().refresh(serverPlayer);
        }
    }

    public void updateLocation(ServerPlayer serverPlayer){
        entity.level = ((CraftWorld) location.getWorld()).getHandle();
        entity.setPosRaw(location.x(), location.y(), location.z());
        entity.setYRot(location.getYaw());

        ClientboundTeleportEntityPacket teleportEntityPacket = new ClientboundTeleportEntityPacket(entity);
        serverPlayer.connection.send(teleportEntityPacket);
    }

    public void updateBillboard(ServerPlayer serverPlayer){
        entity.setBillboardConstraints(billboard);


        if(serverPlayer != null) {
            entity.getEntityData().refresh(serverPlayer);
        }
    }

    public void updateScale(ServerPlayer serverPlayer){
        Transformation transformation = new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        );
        entity.setTransformation(transformation);


        if(serverPlayer != null) {
            entity.getEntityData().refresh(serverPlayer);
        }
    }

    public void updateBackground(ServerPlayer serverPlayer){
        entity.setBackgroundColor(background.getColor() | 0xC8000000);

        if(serverPlayer != null) {
            entity.getEntityData().refresh(serverPlayer);
        }
    }

    private Component getText(){
        String t = String.join("\n", lines);
        return PaperAdventure.asVanilla(MiniMessage.miniMessage().deserialize(t));
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        this.isDirty = true;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
        this.isDirty = true;
    }

    public Display.BillboardConstraints getBillboard() {
        return billboard;
    }

    public void setBillboard(Display.BillboardConstraints billboard) {
        this.billboard = billboard;
        this.isDirty = true;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.isDirty = true;
    }

    public ChatFormatting getBackground() {
        return background;
    }

    public void setBackground(ChatFormatting background) {
        this.background = background;
        this.isDirty = true;
    }

    public Display.TextDisplay getEntity() {
        return entity;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
}
