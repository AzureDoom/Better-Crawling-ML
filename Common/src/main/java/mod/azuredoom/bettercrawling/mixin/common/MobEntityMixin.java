package mod.azuredoom.bettercrawling.mixin.common;

import mod.azuredoom.bettercrawling.interfaces.IMobEntityLivingTickHook;
import mod.azuredoom.bettercrawling.interfaces.IMobEntityTickHook;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
@Mixin(Mob.class)
public abstract class MobEntityMixin implements IMobEntityLivingTickHook, IMobEntityTickHook {

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void onLivingTick(CallbackInfo ci) {
		this.onLivingTick();
	}

	@Override
	public void onLivingTick() {
	}

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void onTick(CallbackInfo ci) {
		this.onTick();
	}

	@Override
	public void onTick() {
	}
}
