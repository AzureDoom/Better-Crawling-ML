package mod.azuredoom.bettercrawling.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mod.azuredoom.bettercrawling.interfaces.ILivingEntityRotationHook;
import net.minecraft.world.entity.LivingEntity;

/*
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin implements ILivingEntityRotationHook {
	@ModifyVariable(method = "lerpTo", at = @At(value = "HEAD"), ordinal = 0)
	private float onSetPositionAndRotationDirectYaw(float yaw, double x, double y, double z, float yaw2, float pitch, int posRotationIncrements, boolean teleport) {
		return this.getTargetYaw(x, y, z, yaw, pitch, posRotationIncrements, teleport);
	}

	@Override
	public float getTargetYaw(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
		return yaw;
	}

	@ModifyVariable(method = "lerpTo", at = @At(value = "HEAD"), ordinal = 1)
	private float onSetPositionAndRotationDirectPitch(float pitch, double x, double y, double z, float yaw, float pitch2, int posRotationIncrements, boolean teleport) {
		return this.getTargetPitch(x, y, z, yaw, pitch, posRotationIncrements, teleport);
	}

	@Override
	public float getTargetPitch(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
		return pitch;
	}

	@ModifyVariable(method = "lerpHeadTo", at = @At("HEAD"), ordinal = 0)
	private float onSetHeadRotation(float yaw, float yaw2, int rotationIncrements) {
		return this.getTargetHeadYaw(yaw, rotationIncrements);
	}

	@Override
	public float getTargetHeadYaw(float yaw, int rotationIncrements) {
		return yaw;
	}
}
