package de.oliver.fancyholograms.api.data;

import de.oliver.fancyholograms.api.hologram.HologramType;
import de.oliver.fancyholograms.util.Location;
import io.leangen.geantyref.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Objects;

public class BlockHologramData extends DisplayHologramData {

    public static BlockState DEFAULT_BLOCK = Blocks.GRASS_BLOCK.defaultBlockState();

    private BlockState block = DEFAULT_BLOCK;

    /**
     * @param name     Name of hologram
     * @param location Location of hologram
     *                 Default values are already set
     */
    public BlockHologramData(String name, Location location) {
        super(name, HologramType.BLOCK, location);
    }

    public BlockState getBlock() {
        return block;
    }

    public BlockHologramData setBlock(BlockState block) {
        if (!Objects.equals(this.block, block)) {
            this.block = block;
            setHasChanges(true);
        }

        return this;
    }

    @Override
    public boolean read(ConfigurationNode section, String name) {
        super.read(section, name);
        block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse(section.node("block").getString("minecraft:grass_block"))
        ).defaultBlockState();

        return true;
    }

    @Override
    public boolean write(ConfigurationNode section, String name){
        super.write(section, name);
        section.node("block").set(TypeToken.get(String.class), BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());

        return true;
    }

    @Override
    public BlockHologramData copy(String name) {
        BlockHologramData blockHologramData = new BlockHologramData(name, getLocation());
        blockHologramData
                .setBlock(this.getBlock())
                .setScale(this.getScale())
                .setShadowRadius(this.getShadowRadius())
                .setShadowStrength(this.getShadowStrength())
                .setBillboard(this.getBillboard())
                .setTranslation(this.getTranslation())
                .setBrightness(this.getBrightness())
                .setVisibilityDistance(getVisibilityDistance())
                .setVisibility(this.getVisibility())
                .setPersistent(this.isPersistent())
                .setLinkedNpcName(getLinkedNpcName());

        return blockHologramData;
    }
}
