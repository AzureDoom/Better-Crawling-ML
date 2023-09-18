package mod.azuredoom.bettercrawling.interfaces;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.Vec3;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public interface ILivingEntityLookAtHook {
	public Vec3 onLookAt(EntityAnchorArgument.Anchor anchor, Vec3 vec);
}
