package mods.eln.sixnode.electricalentitysensor;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;

public class ElectricalEntitySensorSlowProcess implements IProcess, INBTTReady {

    ElectricalEntitySensorElement element;

    double timeCounter = 0;
    static final double refreshPeriode = 0.2;

    RcInterpolator rc1 = new RcInterpolator(0.4f);
    RcInterpolator rc2 = new RcInterpolator(0.4f);

    boolean oldState = false;
    boolean state = false;

    HashMap<Object, Vec3> lastEPos = new HashMap<Object, Vec3>();

    public ElectricalEntitySensorSlowProcess(ElectricalEntitySensorElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        timeCounter += time;

        if (timeCounter > refreshPeriode) {
            timeCounter -= refreshPeriode;
            boolean useSpeed = element.descriptor.useEntitySpeed;
            double speedFactor = element.descriptor.speedFactor;
            Coordonate coord = element.sixNode.coordonate;
            ItemStack filterStack = element.getInventory().getItem(ElectricalEntitySensorContainer.filterId);

            Class filterClass = LivingEntity.class;

            if (!Utils.isEmpty(filterStack)) {
                GenericItemUsingDamageDescriptor gen = EntitySensorFilterDescriptor.getDescriptor(filterStack);
                if (gen != null && gen instanceof EntitySensorFilterDescriptor) {
                    EntitySensorFilterDescriptor filter = (EntitySensorFilterDescriptor) gen;
                    filterClass = filter.entityClass;
                }
            }

            Level world = coord.world();
            double rayMax = element.descriptor.maxRange;
            AABB bb = coord.getAxisAlignedBB((int) rayMax);
            List list = world.getEntitiesOfClass(filterClass, bb);
            double output = 0;
            // WP16: only entities seen in this scan are remembered (1.7.10 never evicted any: the map grew with
            // every entity that ever passed, and kept dead ones alive).
            HashMap<Object, Vec3> seen = new HashMap<Object, Vec3>();

            for (Object o : list) {
                Entity e = (Entity) o;
                Vec3 lastPos;
                if ((lastPos = lastEPos.get(e)) != null) {
                    double weight = 0.4;
                    List<Block> blockList = Utils.traceRay(world, coord.x + 0.5, coord.y + 0.5, coord.z + 0.5, e.getX(), e.getY() + e.getEyeHeight(), e.getZ());
                    boolean view = true;

                    for (Block b : blockList) {
                        if (b.defaultBlockState().isSolidRender()) {
                            view = false;
                            break;
                        }
                    }

                    if (view) {
                        if (e instanceof ServerPlayer) weight *= 2.0;
                        double distance = Utils.getLength(coord.x + 0.5, coord.y + 0.5, coord.z + 0.5, e.getX(), e.getY() + e.getEyeHeight(), e.getZ());
                        if (distance < rayMax) {
                            double sf = 1;
                            if (useSpeed) {
                                sf = speedFactor * Utils.getLength(e.getX(), e.getY(), e.getZ(), lastPos.x, lastPos.y, lastPos.z);

                                //Math.sqrt(e.motionX * e.motionX + e.motionY * e.motionY + e.motionZ * e.motionZ);
                                //	Utils.println(sf);
                            }
                            output += sf * weight * (rayMax - distance) / rayMax;
                        }
                    }
                }
                output = Math.min(1, output);
                seen.put(e, new Vec3(e.getX(), e.getY(), e.getZ()));
            }
            lastEPos = seen;
            //Utils.println(output);
            rc1.setTarget((float) output);
        }

        rc1.step((float) time);
        rc2.setTarget(rc1.get());
        rc2.step((float) time);

        element.outputGateProcess.setOutputNormalized(rc2.get());

        state = element.outputGateProcess.getOutputNormalized() > 0.6;
        if (state != oldState) element.needPublish();
        oldState = state;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        rc1.readFromNBT(nbt, str + "rc1");
        rc2.readFromNBT(nbt, str + "rc2");
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        rc1.writeToNBT(nbt, str + "rc1");
        rc2.writeToNBT(nbt, str + "rc2");
    }
}
