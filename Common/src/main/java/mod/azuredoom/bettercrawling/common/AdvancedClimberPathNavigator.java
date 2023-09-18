package mod.azuredoom.bettercrawling.common;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableSet;

import mod.azuredoom.bettercrawling.interfaces.IClimberEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

/*
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public class AdvancedClimberPathNavigator<T extends Mob & IClimberEntity> extends AdvancedGroundPathNavigator<T> {
	protected final IClimberEntity climber;

	protected Direction verticalFacing = Direction.DOWN;

	protected boolean findDirectPathPoints = false;

	public AdvancedClimberPathNavigator(T entity, Level worldIn, boolean checkObstructions, boolean canPathWalls, boolean canPathCeiling) {
		super(entity, worldIn, checkObstructions);

		this.climber = entity;
		if (this.nodeEvaluator instanceof AdvancedWalkNodeProcessor processor) {
			processor.setStartPathOnGround(false);
			processor.setCanPathWalls(canPathWalls);
			processor.setCanPathCeiling(canPathCeiling);
		}
	}

	@Override
	protected Vec3 getTempMobPos() {
		return this.mob.position().add(0, this.mob.getBbHeight() / 2.0f, 0);
	}

	@Override
	@Nullable
	public Path createPath(BlockPos pos, int checkpointRange) {
		return this.createPath(ImmutableSet.of(pos), 8, false, checkpointRange);
	}

	@Override
	@Nullable
	public Path createPath(Entity entityIn, int checkpointRange) {
		return this.createPath(ImmutableSet.of(entityIn.blockPosition()), 16, true, checkpointRange);
	}

	@Override
	public void tick() {
		++this.tick;

		if (this.hasDelayedRecomputation)
			this.recomputePath();
		if (!this.isDone()) {
			if (this.canUpdatePath())
				this.followThePath();
			else if (this.path != null && !this.path.isDone()) {
				var pos = this.getTempMobPos();
				var targetPos = this.path.getNextEntityPos(this.mob);
				if (pos.y > targetPos.y && !this.mob.onGround() && Mth.floor(pos.x) == Mth.floor(targetPos.x) && Mth.floor(pos.z) == Mth.floor(targetPos.z))
					this.path.advance();
			}

			DebugPackets.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);

			if (!this.isDone()) {
				var targetPoint = this.path.getNode(this.path.getNextNodeIndex());
				Direction dir = null;
				if (targetPoint instanceof DirectionalPathPoint)
					dir = ((DirectionalPathPoint) targetPoint).getPathSide();
				if (dir == null)
					dir = Direction.DOWN;
				var targetPos = this.getExactPathingTarget(this.level, targetPoint.asBlockPos(), dir);
				var moveController = this.mob.getMoveControl();
				if (moveController instanceof ClimberMoveController climbercontroller && targetPoint instanceof DirectionalPathPoint dirpoint && dirpoint.getPathSide() != null)
					climbercontroller.setMoveTo(targetPos.x, targetPos.y, targetPos.z, targetPoint.asBlockPos().relative(dir), dirpoint.getPathSide(), this.speedModifier);
				else
					moveController.setWantedPosition(targetPos.x, targetPos.y, targetPos.z, this.speedModifier);
			}
		}
	}

	public Vec3 getExactPathingTarget(BlockGetter blockaccess, BlockPos pos, Direction dir) {
		var offsetPos = pos.relative(dir);
		var shape = blockaccess.getBlockState(offsetPos).getCollisionShape(blockaccess, offsetPos);
		var axis = dir.getAxis();
		var sign = dir.getStepX() + dir.getStepY() + dir.getStepZ();
		var offset = shape.isEmpty() ? sign /* undo offset if no collider */ : (sign > 0 ? shape.min(axis) - 1 : shape.max(axis));
		var marginXZ = 1 - (this.mob.getBbWidth() % 1);
		var marginY = 1 - (this.mob.getBbHeight() % 1);
		var pathingOffsetXZ = (int) (this.mob.getBbWidth() + 1.0F) * 0.5D;
		var pathingOffsetY = (int) (this.mob.getBbHeight() + 1.0F) * 0.5D - this.mob.getBbHeight() * 0.5f;
		var x = offsetPos.getX() + pathingOffsetXZ + dir.getStepX() * marginXZ;
		var y = offsetPos.getY() + pathingOffsetY + (dir == Direction.DOWN ? -pathingOffsetY : 0.0D) + (dir == Direction.UP ? -pathingOffsetY + marginY : 0.0D);
		var z = offsetPos.getZ() + pathingOffsetXZ + dir.getStepZ() * marginXZ;

		switch (axis) {
		default:
		case X:
			return new Vec3(x + offset, y, z);
		case Y:
			return new Vec3(x, y + offset, z);
		case Z:
			return new Vec3(x, y, z + offset);
		}
	}

	@Override
	protected void followThePath() {
		var pos = this.getTempMobPos();
		this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F ? this.mob.getBbWidth() / 2.0F : 0.75F - this.mob.getBbWidth() / 2.0F;
		var maxDistanceToWaypointY = Math.max(1 /* required for e.g. slabs */, this.mob.getBbHeight() > 0.75F ? this.mob.getBbHeight() / 2.0F : 0.75F - this.mob.getBbHeight() / 2.0F);
		var sizeX = Mth.ceil(this.mob.getBbWidth());
		var sizeY = Mth.ceil(this.mob.getBbHeight());
		var sizeZ = sizeX;
		var orientation = this.climber.getOrientation();
		var upVector = orientation.getGlobal(this.mob.yRot, -90);
		this.verticalFacing = Direction.getNearest((float) upVector.x, (float) upVector.y, (float) upVector.z);

		// Look up to 4 nodes ahead so it doesn't backtrack on positions with multiple path sides when changing/updating path
		for (var i = 4; i >= 0; i--) {
			if (this.path.getNextNodeIndex() + i < this.path.getNodeCount()) {
				var currentTarget = this.path.getNode(this.path.getNextNodeIndex() + i);
				var dx = Math.abs(currentTarget.x + (int) (this.mob.getBbWidth() + 1.0f) * 0.5f - this.mob.getX());
				var dy = Math.abs(currentTarget.y - this.mob.getY());
				var dz = Math.abs(currentTarget.z + (int) (this.mob.getBbWidth() + 1.0f) * 0.5f - this.mob.getZ());
				var isWaypointInReach = dx < this.maxDistanceToWaypoint && dy < maxDistanceToWaypointY && dz < this.maxDistanceToWaypoint;
				var isOnSameSideAsTarget = false;

				if (this.canFloat() && (currentTarget.type == BlockPathTypes.WATER || currentTarget.type == BlockPathTypes.WATER_BORDER || currentTarget.type == BlockPathTypes.LAVA))
					isOnSameSideAsTarget = true;
				else if (currentTarget instanceof DirectionalPathPoint dirpoint) {
					var targetSide = dirpoint.getPathSide();
					isOnSameSideAsTarget = targetSide == null || this.climber.getGroundDirection().getLeft() == targetSide;
				} else
					isOnSameSideAsTarget = true;

				if (isOnSameSideAsTarget && (isWaypointInReach || (i == 0 && this.mob.getNavigation().canCutCorner(this.path.getNextNode().type) && this.isNextTargetInLine(pos, sizeX, sizeY, sizeZ, 1 + i)))) {
					this.path.setNextNodeIndex(this.path.getNextNodeIndex() + 1 + i);
					break;
				}
			}
		}

		if (this.findDirectPathPoints) {
			var verticalAxis = this.verticalFacing.getAxis();
			var firstDifferentHeightPoint = this.path.getNodeCount();

			switch (verticalAxis) {
			case X:
				for (var i = this.path.getNextNodeIndex(); i < this.path.getNodeCount(); ++i)
					if (this.path.getNode(i).x != Math.floor(pos.x)) {
						firstDifferentHeightPoint = i;
						break;
					}
				break;
			case Y:
				for (var i = this.path.getNextNodeIndex(); i < this.path.getNodeCount(); ++i)
					if (this.path.getNode(i).y != Math.floor(pos.y)) {
						firstDifferentHeightPoint = i;
						break;
					}
				break;
			case Z:
				for (var i = this.path.getNextNodeIndex(); i < this.path.getNodeCount(); ++i)
					if (this.path.getNode(i).z != Math.floor(pos.z)) {
						firstDifferentHeightPoint = i;
						break;
					}
				break;
			}

			for (var i = firstDifferentHeightPoint - 1; i >= this.path.getNextNodeIndex(); --i)
				if (this.canMoveDirectly(pos, this.path.getEntityPosAtNode(this.mob, i)/* , sizeX, sizeY, sizeZ */)) {
					this.path.setNextNodeIndex(i);
					break;
				}
		}

		this.doStuckDetection(pos);
	}

	private boolean isNextTargetInLine(Vec3 pos, int sizeX, int sizeY, int sizeZ, int offset) {
		if (this.path.getNextNodeIndex() + offset >= this.path.getNodeCount())
			return false;
		else {
			var currentTarget = Vec3.atBottomCenterOf(this.path.getNextNodePos());

			if (!pos.closerThan(currentTarget, 2.0D))
				return false;
			else {
				var nextTarget = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + offset));
				var targetDir = nextTarget.subtract(currentTarget);
				var currentDir = pos.subtract(currentTarget);

				if (targetDir.dot(currentDir) > 0.0D) {
					Direction.Axis ax, ay, az;
					boolean invertY;
					switch (this.verticalFacing.getAxis()) {
					case X:
						ax = Direction.Axis.Z;
						ay = Direction.Axis.X;
						az = Direction.Axis.Y;
						invertY = this.verticalFacing.getStepX() < 0;
						break;
					default:
					case Y:
						ax = Direction.Axis.X;
						ay = Direction.Axis.Y;
						az = Direction.Axis.Z;
						invertY = this.verticalFacing.getStepY() < 0;
						break;
					case Z:
						ax = Direction.Axis.Y;
						ay = Direction.Axis.Z;
						az = Direction.Axis.X;
						invertY = this.verticalFacing.getStepZ() < 0;
						break;
					}
					// Make sure that the mob can stand at the next point in the same orientation it currently has
					return this.isSafeToStandAt(Mth.floor(nextTarget.x), Mth.floor(nextTarget.y), Mth.floor(nextTarget.z), sizeX, sizeY, sizeZ, currentTarget, 0, 0, -1, ax, ay, az, invertY);
				}

				return false;
			}
		}
	}

	// todo: fix this?
	@Override
	protected boolean canMoveDirectly(Vec3 start, Vec3 end/* , int sizeX, int sizeY, int sizeZ */) {
		var sizeX = 0;// (int) this.mob.getBbWidth();
		var sizeY = 0;// (int) this.mob.getBbHeight();
		var sizeZ = 0;// (int) this.mob.getBbWidth();
		switch (this.verticalFacing.getAxis()) {
		case X:
			return this.isDirectPathBetweenPoints(start, end, sizeX, sizeY, sizeZ, Direction.Axis.Z, Direction.Axis.X, Direction.Axis.Y, 0.0D, this.verticalFacing.getStepX() < 0);
		case Y:
			return this.isDirectPathBetweenPoints(start, end, sizeX, sizeY, sizeZ, Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z, 0.0D, this.verticalFacing.getStepY() < 0);
		case Z:
			return this.isDirectPathBetweenPoints(start, end, sizeX, sizeY, sizeZ, Direction.Axis.Y, Direction.Axis.Z, Direction.Axis.X, 0.0D, this.verticalFacing.getStepZ() < 0);
		}
		return false;
	}

	protected static double swizzle(Vec3 vec, Direction.Axis axis) {
		switch (axis) {
		case X:
			return vec.x;
		case Y:
			return vec.y;
		case Z:
			return vec.z;
		}
		return 0;
	}

	protected static int swizzle(int x, int y, int z, Direction.Axis axis) {
		switch (axis) {
		case X:
			return x;
		case Y:
			return y;
		case Z:
			return z;
		}
		return 0;
	}

	protected static int unswizzle(int x, int y, int z, Direction.Axis ax, Direction.Axis ay, Direction.Axis az, Direction.Axis axis) {
		Direction.Axis unswizzle;
		if (axis == ax)
			unswizzle = Direction.Axis.X;
		else if (axis == ay)
			unswizzle = Direction.Axis.Y;
		else
			unswizzle = Direction.Axis.Z;
		return swizzle(x, y, z, unswizzle);
	}

	protected boolean isDirectPathBetweenPoints(Vec3 start, Vec3 end, int sizeX, int sizeY, int sizeZ, Direction.Axis ax, Direction.Axis ay, Direction.Axis az, double minDotProduct, boolean invertY) {
		var bx = Mth.floor(swizzle(start, ax));
		var bz = Mth.floor(swizzle(start, az));
		var dx = swizzle(end, ax) - swizzle(start, ax);
		var dz = swizzle(end, az) - swizzle(start, az);
		var dSq = dx * dx + dz * dz;
		var by = (int) swizzle(start, ay);
		var sizeX2 = swizzle(sizeX, sizeY, sizeZ, ax);
		var sizeY2 = swizzle(sizeX, sizeY, sizeZ, ay);
		var sizeZ2 = swizzle(sizeX, sizeY, sizeZ, az);

		if (dSq < 1.0E-8D)
			return false;
		else {
			var d3 = 1.0D / Math.sqrt(dSq);
			dx = dx * d3;
			dz = dz * d3;
			sizeX2 = sizeX2 + 2;
			sizeZ2 = sizeZ2 + 2;

			if (!this.isSafeToStandAt(unswizzle(bx, by, bz, ax, ay, az, Direction.Axis.X), unswizzle(bx, by, bz, ax, ay, az, Direction.Axis.Y), unswizzle(bx, by, bz, ax, ay, az, Direction.Axis.Z), unswizzle(sizeX2, sizeY2, sizeZ2, ax, ay, az, Direction.Axis.X), unswizzle(sizeX2, sizeY2, sizeZ2, ax, ay, az, Direction.Axis.Y), unswizzle(sizeX2, sizeY2, sizeZ2, ax, ay, az, Direction.Axis.Z), start, dx, dz, minDotProduct, ax, ay, az, invertY))
				return false;
			else {
				sizeX2 = sizeX2 - 2;
				sizeZ2 = sizeZ2 - 2;
				var stepX = 1.0D / Math.abs(dx);
				var stepZ = 1.0D / Math.abs(dz);
				var relX = (double) bx - swizzle(start, ax);
				var relZ = (double) bz - swizzle(start, az);

				if (dx >= 0.0D)
					++relX;
				if (dz >= 0.0D)
					++relZ;

				relX = relX / dx;
				relZ = relZ / dz;
				var dirX = dx < 0.0D ? -1 : 1;
				var dirZ = dz < 0.0D ? -1 : 1;
				var ex = Mth.floor(swizzle(end, ax));
				var ez = Mth.floor(swizzle(end, az));
				var offsetX = ex - bx;
				var offsetZ = ez - bz;

				while (offsetX * dirX > 0 || offsetZ * dirZ > 0) {
					if (relX < relZ) {
						relX += stepX;
						bx += dirX;
						offsetX = ex - bx;
					} else {
						relZ += stepZ;
						bz += dirZ;
						offsetZ = ez - bz;
					}
					if (!this.isSafeToStandAt(unswizzle(bx, by, bz, ax, ay, az, Direction.Axis.X), unswizzle(bx, by, bz, ax, ay, az, Direction.Axis.Y), unswizzle(bx, by, bz, ax, ay, az, Direction.Axis.Z), unswizzle(sizeX2, sizeY2, sizeZ2, ax, ay, az, Direction.Axis.X), unswizzle(sizeX2, sizeY2, sizeZ2, ax, ay, az, Direction.Axis.Y), unswizzle(sizeX2, sizeY2, sizeZ2, ax, ay, az, Direction.Axis.Z), start, dx, dz, minDotProduct, ax, ay, az, invertY))
						return false;
				}

				return true;
			}
		}
	}

	protected boolean isSafeToStandAt(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3 start, double dx, double dz, double minDotProduct, Direction.Axis ax, Direction.Axis ay, Direction.Axis az, boolean invertY) {
		var sizeX2 = swizzle(sizeX, sizeY, sizeZ, ax);
		var sizeZ2 = swizzle(sizeX, sizeY, sizeZ, az);
		var bx = swizzle(x, y, z, ax) - sizeX2 / 2;
		var bz = swizzle(x, y, z, az) - sizeZ2 / 2;
		var by = swizzle(x, y, z, ay);

		if (!this.isPositionClear(unswizzle(bx, y, bz, ax, ay, az, Direction.Axis.X), unswizzle(bx, y, bz, ax, ay, az, Direction.Axis.Y), unswizzle(bx, y, bz, ax, ay, az, Direction.Axis.Z), sizeX, sizeY, sizeZ, start, dx, dz, minDotProduct, ax, ay, az))
			return false;
		else {
			for (var obx = bx; obx < bx + sizeX2; ++obx)
				for (var obz = bz; obz < bz + sizeZ2; ++obz) {
					var offsetX = (double) obx + 0.5D - swizzle(start, ax);
					var offsetZ = (double) obz + 0.5D - swizzle(start, az);

					if (offsetX * dx + offsetZ * dz >= minDotProduct) {
						var nodeTypeBelow = this.nodeEvaluator.getBlockPathType(this.level, unswizzle(obx, by + (invertY ? 1 : -1), obz, ax, ay, az, Direction.Axis.X), unswizzle(obx, by + (invertY ? 1 : -1), obz, ax, ay, az, Direction.Axis.Y), unswizzle(obx, by + (invertY ? 1 : -1), obz, ax, ay, az, Direction.Axis.Z), this.mob);
						if (nodeTypeBelow == BlockPathTypes.WATER)
							return false;
						if (nodeTypeBelow == BlockPathTypes.LAVA)
							return false;
						if (nodeTypeBelow == BlockPathTypes.OPEN)
							return false;
						var nodeType = this.nodeEvaluator.getBlockPathType(this.level, unswizzle(obx, by, obz, ax, ay, az, Direction.Axis.X), unswizzle(obx, by, obz, ax, ay, az, Direction.Axis.Y), unswizzle(obx, by, obz, ax, ay, az, Direction.Axis.Z), this.mob);
						var f = this.mob.getPathfindingMalus(nodeType);
						if (f < 0.0F || f >= 8.0F)
							return false;
						if (nodeType == BlockPathTypes.DAMAGE_FIRE || nodeType == BlockPathTypes.DANGER_FIRE || nodeType == BlockPathTypes.DAMAGE_OTHER)
							return false;
					}
				}
			return true;
		}
	}

	protected boolean isPositionClear(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3 start, double dx, double dz, double minDotProduct, Direction.Axis ax, Direction.Axis ay, Direction.Axis az) {
		for (var pos : BlockPos.betweenClosed(new BlockPos(x, y, z), new BlockPos(x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1))) {
			if (level.isLoaded(pos))
				continue;
			var offsetX = swizzle(pos.getX(), pos.getY(), pos.getZ(), ax) + 0.5D - swizzle(start, ax);
			var pffsetZ = swizzle(pos.getX(), pos.getY(), pos.getZ(), az) + 0.5D - swizzle(start, az);

			if (offsetX * dx + pffsetZ * dz >= minDotProduct)
				if (!this.level.getBlockState(pos).isPathfindable(this.level, pos, PathComputationType.LAND))
					return false;
		}

		return true;
	}
}
