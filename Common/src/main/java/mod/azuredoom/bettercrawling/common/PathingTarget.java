package mod.azuredoom.bettercrawling.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/*
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public record PathingTarget(BlockPos pos, Direction side) {
}