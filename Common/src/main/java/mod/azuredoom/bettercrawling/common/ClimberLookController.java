package mod.azuredoom.bettercrawling.common;

import mod.azuredoom.bettercrawling.interfaces.IClimberEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Credit to: https://github.com/Nyfaria/NyfsSpiders/tree/1.20.x
 */
public class ClimberLookController<T extends Mob & IClimberEntity> extends LookControl {
    protected final IClimberEntity climber;

    public ClimberLookController(T entity) {
        super(entity);
        this.climber = entity;
    }

    @Override
    protected @NotNull Optional<Float> getXRotD() {
        return Optional.of(this.climber.getOrientation().getLocalRotation(new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getEyeY(), this.wantedZ - this.mob.getZ())).getRight());
    }

    @Override
    protected @NotNull Optional<Float> getYRotD() {
        return Optional.of(this.climber.getOrientation().getLocalRotation(new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getEyeY(), this.wantedZ - this.mob.getZ())).getLeft());
    }
}
