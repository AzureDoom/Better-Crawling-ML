package mod.azuredoom.bettercrawling.interfaces;

import net.minecraft.network.syncher.EntityDataAccessor;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public interface ILivingEntityDataManagerHook {
	public void onNotifyDataManagerChange(EntityDataAccessor<?> key);
}
