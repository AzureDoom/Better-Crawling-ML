package mod.azuredoom.bettercrawling;

import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import mod.azuredoom.bettercrawling.interfaces.IClimberEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/*
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public record Constants() {

	public static BlockPos blockPos(double pX, double pY, double pZ) {
		return new BlockPos(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
	}

	public static BlockPos blockPos(Vec3 pVec3) {
		return blockPos(pVec3.x, pVec3.y, pVec3.z);
	}

	public static Optional<EntityDimensions> onEntitySize(Entity entity) {
		if (entity instanceof IClimberEntity)
			return Optional.of(EntityDimensions.scalable(0.9f, 2.45f));
		return Optional.empty();
	}

	public static void onPreRenderLiving(LivingEntity entity, float partialTicks, PoseStack matrixStack) {
		if (entity instanceof IClimberEntity climber) {
			var orientation = climber.getOrientation();
			var renderOrientation = climber.calculateOrientation(partialTicks);
			climber.setRenderOrientation(renderOrientation);

			matrixStack.mulPose(Axis.YP.rotationDegrees(renderOrientation.yaw()));
			matrixStack.mulPose(Axis.XP.rotationDegrees(renderOrientation.pitch()));
			matrixStack.mulPose(Axis.YP.rotationDegrees((float) Math.signum(0.5f - orientation.componentY() - orientation.componentZ() - orientation.componentX()) * renderOrientation.yaw()));
		}
	}

	public static void onPostRenderLiving(LivingEntity entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn) {
		if (entity instanceof IClimberEntity climber) {
			var orientation = climber.getOrientation();
			var renderOrientation = climber.getRenderOrientation();

			if (renderOrientation != null) {
				var verticalOffset = climber.getVerticalOffset(partialTicks);

				var x = climber.getAttachmentOffset(Direction.Axis.X, partialTicks) - (float) renderOrientation.normal().x * verticalOffset;
				var y = climber.getAttachmentOffset(Direction.Axis.Y, partialTicks) - (float) renderOrientation.normal().y * verticalOffset;
				var z = climber.getAttachmentOffset(Direction.Axis.Z, partialTicks) - (float) renderOrientation.normal().z * verticalOffset;

				matrixStack.mulPose(Axis.YP.rotationDegrees(-(float) Math.signum(0.5f - orientation.componentY() - orientation.componentZ() - orientation.componentX()) * renderOrientation.yaw()));
				matrixStack.mulPose(Axis.XP.rotationDegrees(-renderOrientation.pitch()));
				matrixStack.mulPose(Axis.YP.rotationDegrees(-renderOrientation.yaw()));

				matrixStack.translate(-x, -y, -z);
			}
		}
	}
}
