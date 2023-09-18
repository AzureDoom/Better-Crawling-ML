package mod.azuredoom.bettercrawling.interfaces;

import net.minecraft.nbt.CompoundTag;

/*
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public interface IEntityReadWriteHook {
	public void onRead(CompoundTag nbt);

	public void onWrite(CompoundTag nbt);
}
