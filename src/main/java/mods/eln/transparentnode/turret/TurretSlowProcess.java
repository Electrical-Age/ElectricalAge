package mods.eln.transparentnode.turret;

import mods.eln.fsm.CompositeState;
import mods.eln.fsm.State;
import mods.eln.fsm.StateMachine;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sound.SoundCommand;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;

import java.util.List;
import java.util.Random;

import static scala.Console.print;
import static scala.Console.println;


public class TurretSlowProcess extends StateMachine {
    private static final Random rand = new Random();
    private double actualPower;

    public TurretSlowProcess(TurretElement element) {
        actualPower = 0;
        setInitialState(new IdleState());
        reset();
        this.element = element;
    }

    private final TurretElement element;

    private class IdleState implements State {
        @Override
        public void enter() {
        }

        @Override
        public State state(double time) {
            if (element.load.getU() >= element.getDescriptor().getProperties().minimalVoltage *
                (1.0 + element.getDescriptor().getProperties().minimalVoltageHysteresisFactor)) {
                return new ActiveState();
            } else {
                return this;
            }
        }

        @Override
        public void leave() {
        }
    }

    private class ActiveState extends CompositeState {
        public ActiveState() {
            setInitialState(new SeekingState());
        }

        @Override
        public void enter() {
            element.setEnabled(true);
            actualPower = element.getDescriptor().getProperties().basePower;
            element.play(new SoundCommand("eln:TurretActivated").mulVolume(0.5));
            super.enter();
        }

        @Override
        public State state(double time) {
            super.state(time);
            if (element.load.getU() < element.getDescriptor().getProperties().minimalVoltage *
                (1.0 - element.getDescriptor().getProperties().minimalVoltageHysteresisFactor))
                return new WaitState();
            else if (element.load.getU() > element.getDescriptor().getProperties().maximalVoltage)
                return new DamageState();
            else
                return this;
        }

        @Override
        public void leave() {
            element.setEnabled(false);
            actualPower = 0;
            element.play(new SoundCommand("eln:TurretDeactivated").mulVolume(0.5));
            super.leave();
        }
    }

    private class DamageState implements State {
        @Override
        public void enter() {
            WorldExplosion explosion = new WorldExplosion(element).machineExplosion();
            explosion.destructImpl();
        }

        @Override
        public State state(double time) {
            return new IdleState();
        }

        @Override
        public void leave() {
        }

    }

    private class WaitState implements State {
        private double delay = 5.0;

        @Override
        public void enter() {
        }

        @Override
        public State state(double time) {
            delay -= time;
            if (delay <= 0.0) {
                return new IdleState();
            } else {
                return this;
            }
        }

        @Override
        public void leave() {
        }
    }

    private class SeekingState implements State {
        private double lastScanWasBefore = 0.;

        @Override
        public void enter() {
            actualPower = element.getDescriptor().getProperties().basePower;
            element.setGunPosition(0);
            element.setGunElevation(0);
            element.setSeekMode(true);
            element.setTurretAngle(element.getDescriptor().getProperties().actionAngle);
        }

        @Override
        public State state(double time) {
            if (element.getTurretAngle() >= element.getDescriptor().getProperties().actionAngle)
                element.setTurretAngle(-element.getDescriptor().getProperties().actionAngle);
            else if (element.getTurretAngle() <= -element.getDescriptor().getProperties().actionAngle)
                element.setTurretAngle(element.getDescriptor().getProperties().actionAngle);

            lastScanWasBefore += time;
            if (lastScanWasBefore < element.getDescriptor().getProperties().entityDetectionInterval) return null;
            lastScanWasBefore = 0;

            Class filterClass = null;
            ItemStack filterStack = element.getInventory().getStackInSlot(TurretContainer.filterId);
            if (filterStack != null) {
                GenericItemUsingDamageDescriptor gen = EntitySensorFilterDescriptor.getDescriptor(filterStack);
                if (gen != null && gen instanceof EntitySensorFilterDescriptor) {
                    EntitySensorFilterDescriptor filter = (EntitySensorFilterDescriptor) gen;
                    filterClass = filter.entityClass;
                }
            }

            Coordonate coord = element.coordonate();
            AxisAlignedBB bb = coord.getAxisAlignedBB((int) element.getDescriptor().getProperties().detectionDistance);
            @SuppressWarnings("unchecked")
            List<EntityLivingBase> list = coord.world().getEntitiesWithinAABB(EntityLivingBase.class, bb);
            for (EntityLivingBase entity : list) {
                double dx = (entity.posX - coord.x - 0.5);
                double dz = (entity.posZ - coord.z - 0.5);
                double entityAngle = -Math.toDegrees(Math.atan2(dz, dx));
                switch (element.front) {
                    case XN:
                        if (entityAngle > 0)
                            entityAngle -= 180;
                        else
                            entityAngle += 180;
                        break;

                    case ZP:
                        entityAngle += 90;
                        break;

                    case ZN:
                        entityAngle -= 90;
                        break;

                    default:
                        break;

                }

                if (Math.abs(entityAngle - element.getTurretAngle()) < 15 &&
                    Math.abs(entityAngle) < element.getDescriptor().getProperties().actionAngle) {

                    if (element.filterIsSpare) {
                        if (filterClass != null && filterClass.isAssignableFrom(entity.getClass())) return null;
                    } else {
                        if (filterClass == null || !filterClass.isAssignableFrom(entity.getClass())) return null;
                    }

                    List<Block> blockList = Utils.traceRay(coord.world(), coord.x + 0.5, coord.y + 0.5, coord.z + 0.5,
                        entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
                    boolean visible = true;
                    for (Block b : blockList)
                        if (b.isOpaqueCube()) {
                            visible = false;
                            break;
                        }

                    if (visible) {
                        if(entity.getHealth()>0) {
                            element.play(new SoundCommand("eln:TurretFire").mulVolume(0.4));
                        } else {
                            element.play(new SoundCommand("eln:TurretKill").mulVolume(0.4));
                        }
                        return new AimingState(entity);
                    }
                }
            }

            return null;
        }

        @Override
        public void leave() {
            element.setSeekMode(false);
        }
    }

    private class AimingState implements State {
        public AimingState(EntityLivingBase target) {
            this.target = target;
        }

        private final EntityLivingBase target;

        @Override
        public void enter() {
            actualPower = element.getDescriptor().getProperties().basePower +
                element.chargePower;
            element.setGunPosition(1);
        }

        @Override
        public State state(double time) {
            if (target.getHealth()<=0) return new SeekingState();

            Class filterClass = null;
            ItemStack filterStack = element.getInventory().getStackInSlot(TurretContainer.filterId);
            if (filterStack != null) {
                GenericItemUsingDamageDescriptor gen = EntitySensorFilterDescriptor.getDescriptor(filterStack);
                if (gen != null && gen instanceof EntitySensorFilterDescriptor) {
                    EntitySensorFilterDescriptor filter = (EntitySensorFilterDescriptor) gen;
                    filterClass = filter.entityClass;
                }
            }
            if (element.filterIsSpare) {
                if (filterClass != null && filterClass.isAssignableFrom(target.getClass())) return new SeekingState();
            } else {
                if (filterClass == null || !filterClass.isAssignableFrom(target.getClass())) return new SeekingState();
            }

            Coordonate coord = element.coordonate();

            double dx = (float) (target.posX - coord.x - 0.5);
            double dy = (float) (target.posY + target.getEyeHeight() - coord.y - 0.75);
            double dz = (float) (target.posZ - coord.z - 0.5);
            double entityAngle = -Math.toDegrees(Math.atan2(dz, dx));
            switch (element.front) {
                case XN:
                    if (entityAngle > 0)
                        entityAngle -= 180;
                    else
                        entityAngle += 180;
                    break;

                case ZP:
                    entityAngle += 90;
                    break;

                case ZN:
                    entityAngle -= 90;
                    break;

                default:
                    break;

            }

            double entityAngle2 = -Math.toDegrees(Math.asin(dy / Math.sqrt(dx * dx + dz * dz)));

            if (Math.abs(entityAngle) > element.getDescriptor().getProperties().actionAngle) return new SeekingState();

            element.setTurretAngle((float) entityAngle);
            element.setGunElevation((float) -entityAngle2);

            if (Math.abs(target.posX - coord.x) > element.getDescriptor().getProperties().aimDistance ||
                Math.abs(target.posZ - coord.z) > element.getDescriptor().getProperties().aimDistance)
                return new SeekingState();

            List<Block> blockList = Utils.traceRay(coord.world(), coord.x + 0.5, coord.y + 0.5, coord.z + 0.5,
                target.posX, target.posY + target.getEyeHeight(), target.posZ);
            for (Block b : blockList)
                if (b.isOpaqueCube())
                    return new SeekingState();

            if (element.getGunPosition() == 1 && element.isTargetReached() &&
                element.energyBuffer >= element.getDescriptor().getProperties().impulseEnergy)
                return new ShootState(target);

            return this;
        }

        @Override
        public void leave() {
        }
    }

    class ShootState implements State {
        public ShootState(EntityLivingBase target) {
            this.target = target;
        }

        private final EntityLivingBase target;

        @Override
        public void enter() {
            if (target != null){
            target.hurtResistantTime = 0;
		    target.attackEntityFrom(new DamageSource("Unknown"), 5);
			element.shoot();
            element.play(new SoundCommand("eln:LaserGun"));
		    }
        }

        @Override
        public State state(double time) {
            if (target == null || target.getHealth()<=0)
                return new SeekingState();
            else
                return new AimingState(target);
        }

        @Override
        public void leave() {
            element.energyBuffer = 0;
        }
    }

    @Override
    public void process(double time) {
        double MaximalEnergy = element.getDescriptor().getProperties().impulseEnergy;
        element.energyBuffer += element.powerResistor.getP() * time;
        boolean full = element.energyBuffer > MaximalEnergy;

        if (full) {
            element.energyBuffer = MaximalEnergy;
        }

        if (element.coordonate().getBlockExist())
            super.process(time);

        if (actualPower == 0 || full)
            element.powerResistor.highImpedance();
        else
            element.powerResistor.setR(element.load.getU() * element.load.getU() / actualPower);
    }
}
