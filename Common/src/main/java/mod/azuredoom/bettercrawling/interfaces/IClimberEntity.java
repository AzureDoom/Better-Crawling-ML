package mod.azuredoom.bettercrawling.interfaces;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import mod.azuredoom.bettercrawling.common.Orientation;
import mod.azuredoom.bettercrawling.common.PathingTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public interface IClimberEntity extends IAdvancedPathFindingEntity {
	public float getAttachmentOffset(Direction.Axis axis, float partialTicks);

	public float getVerticalOffset(float partialTicks);

	public Orientation getOrientation();

	public Orientation calculateOrientation(float partialTicks);

	public void setRenderOrientation(Orientation orientation);

	@Nullable
	public Orientation getRenderOrientation();

	public float getMovementSpeed();

	public Pair<Direction, Vec3> getGroundDirection();

	public boolean shouldTrackPathingTargets();

	@Nullable
	public Vec3 getTrackedMovementTarget();

	@Nullable
	public List<PathingTarget> getTrackedPathingTargets();

	public boolean canClimbOnBlock(BlockState state, BlockPos pos);

	public float getBlockSlipperiness(BlockPos pos);

	public boolean canClimberTriggerWalking();

	public boolean canClimbInWater();

	public void setCanClimbInWater(boolean value);

	public boolean canClimbInLava();

	public void setCanClimbInLava(boolean value);

	public float getCollisionsInclusionRange();

	public void setCollisionsInclusionRange(float range);

	public float getCollisionsSmoothingRange();

	public void setCollisionsSmoothingRange(float range);

	public void setJumpDirection(@Nullable Vec3 dir);
}
