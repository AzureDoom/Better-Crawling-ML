package mod.azuredoom.bettercrawling.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public interface IEntityMovementHook {
    public boolean onMove(MoverType type, Vec3 pos, boolean pre);

    @Nullable
    public BlockPos getAdjustedOnPosition(BlockPos onPosition);

    public boolean getAdjustedCanTriggerWalking(boolean canTriggerWalking);
}
