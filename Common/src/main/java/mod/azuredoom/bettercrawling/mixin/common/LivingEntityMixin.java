package mod.azuredoom.bettercrawling.mixin.common;

import mod.azuredoom.bettercrawling.interfaces.ILivingEntityDataManagerHook;
import mod.azuredoom.bettercrawling.interfaces.ILivingEntityJumpHook;
import mod.azuredoom.bettercrawling.interfaces.ILivingEntityLookAtHook;
import mod.azuredoom.bettercrawling.interfaces.ILivingEntityTravelHook;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ILivingEntityLookAtHook, ILivingEntityDataManagerHook, ILivingEntityTravelHook, ILivingEntityJumpHook {

    @ModifyVariable(method = "lookAt", at = @At("HEAD"), ordinal = 0)
    private Vec3 onLookAtModify(Vec3 vec, Anchor anchor, Vec3 vec2) {
        return this.onLookAt(anchor, vec);
    }

    @Override
    public Vec3 onLookAt(Anchor anchor, Vec3 vec) {
        return vec;
    }

    @Inject(method = "onSyncedDataUpdated", at = @At("HEAD"))
    private void onNotifyDataManagerChange(EntityDataAccessor<?> key, CallbackInfo ci) {
        this.onNotifyDataManagerChange(key);
    }

    @Override
    public void onNotifyDataManagerChange(EntityDataAccessor<?> key) {
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3 relative, CallbackInfo ci) {
        if (this.onTravel(relative, true))
            ci.cancel();
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelPost(Vec3 relative, CallbackInfo ci) {
        this.onTravel(relative, false);
    }

    @Override
    public boolean onTravel(Vec3 relative, boolean pre) {
        return false;
    }

    @Inject(method = "jumpFromGround()V", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo ci) {
        if (this.onJump())
            ci.cancel();
    }

    @Override
    public boolean onJump() {
        return false;
    }
}
