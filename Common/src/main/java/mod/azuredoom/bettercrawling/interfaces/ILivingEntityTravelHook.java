package mod.azuredoom.bettercrawling.interfaces;

import net.minecraft.world.phys.Vec3;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public interface ILivingEntityTravelHook {
    public boolean onTravel(Vec3 relative, boolean pre);
}
