package mods.eln.sixnode.lampsocket;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.LampDescriptor;
import mods.eln.item.LampDescriptor.Type;
import mods.eln.misc.Coordinate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.server.SaveConfig;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import net.minecraft.util.math.Vec3d;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class LampSocketProcess implements IProcess, INBTTReady /*,LightBlockObserver*/ {

    double time = 0;
    double deltaTBase = 0.2;
    double deltaT = deltaTBase;
    public double invulerabilityTimeLeft = 2;
    boolean overPoweredInvulerabilityArmed = true;
    LampSocketElement lamp;
    int light = 0; // 0..15
    double alphaZ = 0.0;

    double stableProb = 0;

    ItemStack lampStackLast = null;
    boolean boot = true;

    double vp[] = new double[3];

    private LampSupplyElement oldLampSupply;

    double updateLifeTimeout = 0, updateLifeTimeoutMax = 5;

    Coordinate lbCoord;

    public LampSocketProcess(LampSocketElement l) {
        this.lamp = l;
        lbCoord = new Coordinate(l.sixNode.coordinate);
    }

    @Override
    public void process(double time) {
        ItemStack lampStack = lamp.getInventory().getStackInSlot(0);

        if (!lamp.poweredByLampSupply || lamp.getInventory().getStackInSlot(LampSocketContainer.cableSlotId) == null) {
            lamp.setIsConnectedToLampSupply(false);
            oldLampSupply = null;
        } else {
            Coordinate myCoord = lamp.sixNode.coordinate;
            LampSupplyElement.PowerSupplyChannelHandle best = null;
            float bestDistance = 10000;
            List<LampSupplyElement.PowerSupplyChannelHandle> list = LampSupplyElement.channelMap.get(lamp.channel);
            if (list != null) {
                for (LampSupplyElement.PowerSupplyChannelHandle s : list) {
                    float distance = (float) s.element.sixNode.coordinate.trueDistanceTo(myCoord);
                    if (distance < bestDistance && distance <= s.element.getRange()) {
                        bestDistance = distance;
                        best = s;
                    }
                }
            }
            if (best != null && best.element.getChannelState(best.id)) {
                if (lampStack != null) {
                    LampDescriptor lampDescriptor = (LampDescriptor) ((GenericItemUsingDamage<GenericItemUsingDamageDescriptor>) lampStack.getItem()).getDescriptor(lampStack);
                    best.element.addToRp(lampDescriptor.getR());
                }
                lamp.positiveLoad.state = best.element.powerLoad.state;
                oldLampSupply = best.element;
            } else {
                lamp.positiveLoad.state = 0;
                oldLampSupply = null;
            }

            lamp.setIsConnectedToLampSupply(best != null);
        }

        if (lampStack != null) {
            LampDescriptor lampDescriptor = (LampDescriptor) ((GenericItemUsingDamage<GenericItemUsingDamageDescriptor>) lampStack.getItem()).getDescriptor(lampStack);

            if (lamp.getCoordonate().doesBlockExist() && lampDescriptor.vegetableGrowRate != 0.0) {
                double randTarget = 1.0 / lampDescriptor.vegetableGrowRate * time * (1.0 * light / lampDescriptor.nominalLight / 15.0);
                if (randTarget > Math.random()) {
                    boolean exit = false;
                    Vec3d vv = new Vec3d(1, 0, 0);
                    Vec3d vp = new Vec3d(myCoord().pos.getX() + 0.5, myCoord().pos.getY() + 0.5, myCoord().pos.getZ() + 0.5);

                    // TODO(1.10): I may have swapped these two.
                    vv = vv.rotatePitch((float) (alphaZ * Math.PI / 180.0));
                    vv = vv.rotateYaw((float) ((Math.random() - 0.5) * 2 * Math.PI / 4));
                    vv = vv.rotatePitch((float) ((Math.random() - 0.5) * 2 * Math.PI / 4));

                    vv = lamp.front.rotateOnXnLeft(vv);
                    vv = lamp.side.rotateFromXN(vv);

                    Coordinate c = new Coordinate(myCoord());

                    for (int idx = 0; idx < lamp.socketDescriptor.range + light; idx++) {
                        // newCoord.move(lamp.side.getInverse());
                        vp.addVector(vv.xCoord, vv.yCoord, vv.zCoord);
                        c.setPosition(vp);
                        Block b = c.getBlockState().getBlock();

                        if (!c.doesBlockExist()) {
                            exit = true;
                            break;
                        }
                        if (isOpaque(c)) {
                            vp.addVector(-vv.xCoord, -vv.yCoord, -vv.zCoord);
                            c.setPosition(vp);
                            b = c.getBlockState().getBlock();
                            break;
                        }
                    }

                    if (!exit) {
                        Block b = c.getBlockState().getBlock();

                        if (b != Blocks.AIR) {
                            b.updateTick(c.world(), new BlockPos(c.pos.getX(), c.pos.getY(), c.pos.getZ()), c.getBlockState(), new Random());
                        }
                    }
                }
            }
        }

        this.time += time;
        if (this.time < deltaT)
            return;

        this.time -= deltaT;

        lamp.computeElectricalLoad();
        int oldLight = light;
        int newLight = 0;

        if (!boot && (lampStack != lampStackLast || lampStack == null)) {
            stableProb = 0;
        }

        if (lampStack != null) {
            LampDescriptor lampDescriptor = (LampDescriptor) ((GenericItemUsingDamage<GenericItemUsingDamageDescriptor>) lampStack.getItem()).getDescriptor(lampStack);

            if (stableProb < 0)
                stableProb = 0;

            double lightDouble = 0;
            switch (lampDescriptor.type) {
                case Incandescent:
                case LED:
                    lightDouble = lampDescriptor.nominalLight * (Math.abs(lamp.lampResistor.getU()) - lampDescriptor.minimalU) / (lampDescriptor.nominalU - lampDescriptor.minimalU);
                    lightDouble = (lightDouble * 16);
                    break;

                case eco:
                    double U = Math.abs(lamp.lampResistor.getU());
                    if (U < lampDescriptor.minimalU) {
                        stableProb = 0;
                        lightDouble = 0;
                    } else {
                        double powerFactor = U / lampDescriptor.nominalU;
                        stableProb += U / lampDescriptor.stableU * deltaT / lampDescriptor.stableTime * lampDescriptor.stableUNormalised;

                        if (stableProb > U / lampDescriptor.stableU)
                            stableProb = U / lampDescriptor.stableU;
                        if (Math.random() > stableProb) {
                            lightDouble = 0;
                        } else {
                            lightDouble = lampDescriptor.nominalLight * powerFactor;
                            lightDouble = (lightDouble * 16);
                        }
                    }
                    break;

                default:
                    break;
            }

            if (lightDouble - oldLight > 1.0) {
                newLight = (int) lightDouble;
            } else if (lightDouble - oldLight < -0.3) {
                newLight = (int) lightDouble;
            } else {
                newLight = oldLight;
            }

            if (newLight < 0)
                newLight = 0;
            if (newLight > 14)
                newLight = 14;

			/*
             * double overFactor = (lamp.electricalLoad.Uc-lampDescriptor.minimalU) /(lampDescriptor.nominalU-lampDescriptor.minimalU);
			 */
            double overFactor = (lamp.lampResistor.getP()) / (lampDescriptor.nominalP);
            if (overFactor < 0)
                overFactor = 0;

            if (overFactor < 1.3)
                overPoweredInvulerabilityArmed = true;

            if (overFactor > 1.5 && overPoweredInvulerabilityArmed) {
                invulerabilityTimeLeft = 2;
                overPoweredInvulerabilityArmed = false;
            }

            if (invulerabilityTimeLeft != 0 && overFactor > 1.5)
                overFactor = 1.5;

            updateLifeTimeout += deltaT;
            if (updateLifeTimeout > updateLifeTimeoutMax &&
                !(lampDescriptor.type == Type.LED && Eln.ledLampInfiniteLife)) {
                // Utils.println("aging");
                updateLifeTimeout -= updateLifeTimeoutMax;
                double lifeLost = overFactor * updateLifeTimeoutMax / lampDescriptor.nominalLife;
                lifeLost = Utils.voltageMargeFactorSub(lifeLost);
                if (overFactor >= 1.21) {
                    lifeLost *= overFactor;
                }
                // lifeLost *= overFactor;
                // lifeLost *= overFactor;

                double life = lampDescriptor.getLifeInTag(lampStack) - lifeLost;
                if (SaveConfig.instance.electricalLampAging) {
                    lampDescriptor.setLifeInTag(lampStack, life);
                }
                if (life < 0 || overFactor > 3) {
                    lamp.getInventory().setInventorySlotContents(0, null);
                    light = 0;
                }

            }

            boot = false;
        }

        if (invulerabilityTimeLeft != 0) {
            invulerabilityTimeLeft -= deltaT;
            if (invulerabilityTimeLeft < 0)
                invulerabilityTimeLeft = 0;
        }
        deltaT = deltaTBase + deltaTBase * (-0.1 + 0.2 * Math.random());

        lampStackLast = lampStack;

        placeSpot(newLight);
    }

    // ElectricalConnectionOneWay connection = null;

    public void rotateAroundZ(Vec3d v, float par1) {
        float f1 = MathHelper.cos(par1);
        float f2 = MathHelper.sin(par1);
        double d0 = v.xCoord * (double) f1 + v.yCoord * (double) f2;
        double d1 = v.yCoord * (double) f1 - v.xCoord * (double) f2;
        double d2 = v.zCoord;
        v = new Vec3d(d0, d1, d2);
    }

    void placeSpot(int newLight) {
        boolean exit = false;
        if (!lbCoord.doesBlockExist())
            return;
        Vec3d vv = new Vec3d(1, 0, 0);
        Vec3d vp = Utils.getVec05(myCoord());

        rotateAroundZ(vv, (float) (alphaZ * Math.PI / 180.0));

        lamp.front.rotateOnXnLeft(vv);
        lamp.side.rotateFromXN(vv);

        Coordinate newCoord = new Coordinate(myCoord());
        for (int idx = 0; idx < lamp.socketDescriptor.range; idx++) {
            // newCoord.move(lamp.side.getInverse());
            vp.add(vv);
            newCoord.setPosition(vp);
            if (!newCoord.doesBlockExist()) {
                exit = true;
                break;
            }
            if (isOpaque(newCoord)) {
                vp.add(new Vec3d(-vv.xCoord, -vv.yCoord, -vv.zCoord));
                newCoord.setPosition(vp);
                break;
            }
        }
        if (!exit) {
            int count = 0;
            while (!newCoord.equals(myCoord())) {
                Block block = newCoord.getBlockState().getBlock();
                if (block == Blocks.AIR || block == Eln.lightBlock) {
                    count++;
                    if (count == 2)
                        break;
                }
                vp.add(new Vec3d(-vv.xCoord, -vv.yCoord, -vv.zCoord));
                newCoord.setPosition(vp);
            }
        }
        if (!exit)
            setLightAt(newCoord, newLight);
    }

    public boolean isOpaque(Coordinate coord) {
        Block block = coord.getBlockState().getBlock();
        boolean isNotOpaque = block == Blocks.AIR || !block.isOpaqueCube(block.getBlockState().getBaseState());
        if (block == Blocks.FARMLAND)

            isNotOpaque = false;
        return !isNotOpaque;
    }

    public void publish() {
        Utils.print("Light published");
    }

    public void setLightAt(Coordinate coord, int value) {
        Coordinate oldLbCoord = lbCoord;
        lbCoord = new Coordinate(coord);
        int oldLight = light;
        boolean same = coord.equals(oldLbCoord);
        light = value;

        if (!same && oldLbCoord.equals(myCoord())) {
            lamp.sixNode.recalculateLightValue();
        }

        if (lbCoord.equals(myCoord())) {
            if (light != oldLight || !same)
                lamp.sixNode.recalculateLightValue();
        } else {
			/*
			 * if(same) LightBlockEntity.remplaceLight(lbCoord, oldLight, light); else LightBlockEntity.addLight(lbCoord, light);
			 */
            LightBlockEntity.addLight(lbCoord, light, 5);
        }

        if (light != oldLight) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
            DataOutputStream packet = new DataOutputStream(bos);

            lamp.preparePacketForClient(packet);
            try {
                packet.writeByte(light);
            } catch (IOException e) {
                e.printStackTrace();
            }
            lamp.sendPacketToAllClient(bos);
        }
    }

    Coordinate myCoord() {
        return lamp.sixNode.coordinate;
    }

    public void destructor() {
        // if(lbCoord.equals(myCoord()) == false && lbCoord.getBlockId() ==
        // Eln.lightBlockId)
        // lbCoord.setBlock(0,0);
        // TODO

		/*
		 * LightBlockEntity.removeObserver(this); if(lbCoord.equals(myCoord()) == false) LightBlockEntity.removeLight(lbCoord, light);
		 */
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        stableProb = nbt.getDouble(str + "LSP" + "stableProb");
        lbCoord.readFromNBT(nbt, str + "lbCoordInst");
        alphaZ = nbt.getFloat(str + "alphaZ");
        light = nbt.getInteger(str + "light");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setDouble(str + "LSP" + "stableProb", stableProb);
        lbCoord.writeToNBT(nbt, str + "lbCoordInst");
        nbt.setFloat(str + "alphaZ", (float) alphaZ);
        nbt.setInteger(str + "light", light);
    }

    public int getBlockLight() {
        if (lbCoord.equals(myCoord())) {
            return light;
        } else {
            return 0;
        }
    }
	/*
	 * 
	 * @Override public void lightBlockDestructor(Coordinate coord) { if(coord.equals(lbCoord)) { light = 0; lbCoord = new Coordinate(myCoord()); //placeSpot(light); } }
	 */
}
