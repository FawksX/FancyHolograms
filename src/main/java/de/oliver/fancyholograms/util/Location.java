package de.oliver.fancyholograms.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record Location(ServerLevel serverLevel, BlockPos pos) {

    public Location copy() {
        return new Location(serverLevel, pos);
    }

}
