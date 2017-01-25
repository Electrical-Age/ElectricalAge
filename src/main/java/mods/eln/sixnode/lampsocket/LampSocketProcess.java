package mods.eln.sixnode.lampsocket;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.LampDescriptor;
import mods.eln.item.LampDescriptor.Type;
import mods.eln.misc.Coordonate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.server.SaveConfig;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

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

    Coordonate lbCoord;

    public LampSocketProcess(LampSocketElement l) {
		this.lamp = l;
		lbCoord = new Coordonate(l.sixNode.coordonate);
	}

	@Override
	public void process(double time) {
		ItemStack lampStack = lamp.getInventory().getStackInSlot(0);

		if (!lamp.poweredByLampSupply || lamp.getInventory().getStackInSlot(LampSocketContainer.cableSlotId) == null) {
			lamp.setIsConnectedToLampSupply(false);
			oldLampSupply = null;
		} else {
			Coordonate myCoord = lamp.sixNode.coordonate;
            LampSupplyElement.PowerSupplyChannelHandle best = null;
			float bestDistance = 10000;
			List<LampSupplyElement.PowerSupplyChannelHandle> list = LampSupplyElement.channelMap.get(lamp.channel);
			if (list != null) {
				for (LampSupplyElement.PowerSupplyChannelHandle s : list) {
					float distance = (float) s.element.sixNode.coordonate.trueDistanceTo(myCoord);
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

			if (lamp.getCoordonate().getBlockExist() && lampDescriptor.vegetableGrowRate != 0.0) {
				double randTarget = 1.0 / lampDescriptor.vegetableGrowRate * time * (1.0 * light / lampDescriptor.nominalLight / 15.0);
				if (randTarget > Math.random()) {
					boolean exit = false;
					Vec3 vv = Vec3.createVectorHelper(1, 0, 0);
					Vec3 vp = Vec3.createVectorHelper(myCoord().x + 0.5, myCoord().y + 0.5, myCoord().z + 0.5);

					vv.rotateAroundZ((float) (alphaZ * Math.PI / 180.0));

					vv.rotateAroundY((float) ((Math.random() - 0.5) * 2 * Math.PI / 4));
					vv.rotateAroundZ((float) ((Math.random() - 0.5) * 2 * Math.PI / 4));

					lamp.front.rotateOnXnLeft(vv);
					lamp.side.rotateFromXN(vv);

					Coordonate c = new Coordonate(myCoord());

					for (int idx = 0; idx < lamp.socketDescriptor.range + light; idx++) {
						// newCoord.move(lamp.side.getInverse());
						vp.xCoord += vv.xCoord;
						vp.yCoord += vv.yCoord;
						vp.zCoord += vv.zCoord;

						c.setPosition(vp);
						Block b = c.getBlock();
						if (!c.getBlockExist()) {
							exit = true;
							break;
						}
						if (isOpaque(c)) {
							vp.xCoord -= vv.xCoord;
							vp.yCoord -= vv.yCoord;
							vp.zCoord -= vv.zCoord;

							c.setPosition(vp);
							b = c.getBlock();
							break;
						}
					}

					if (!exit) {
						Block b = c.getBlock();

						if (b != Blocks.air) {
							b.updateTick(c.world(), c.x, c.y, c.z, c.world().rand);
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

	public void rotateAroundZ(Vec3 v, float par1) {
		float f1 = MathHelper.cos(par1);
		float f2 = MathHelper.sin(par1);
		double d0 = v.xCoord * (double) f1 + v.yCoord * (double) f2;
		double d1 = v.yCoord * (double) f1 - v.xCoord * (double) f2;
		double d2 = v.zCoord;
		v.xCoord = d0;
		v.yCoord = d1;
		v.zCoord = d2;
	}

	void placeSpot(int newLight) {
		boolean exit = false;
		if (!lbCoord.getBlockExist())
			return;
		Vec3 vv = Vec3.createVectorHelper(1, 0, 0);
		Vec3 vp = Utils.getVec05(myCoord());

		rotateAroundZ(vv, (float) (alphaZ * Math.PI / 180.0));

		lamp.front.rotateOnXnLeft(vv);
		lamp.side.rotateFromXN(vv);

		Coordonate newCoord = new Coordonate(myCoord());
		for (int idx = 0; idx < lamp.socketDescriptor.range; idx++) {
			// newCoord.move(lamp.side.getInverse());
			vp.xCoord += vv.xCoord;
			vp.yCoord += vv.yCoord;
			vp.zCoord += vv.zCoord;

			newCoord.setPosition(vp);
			if (!newCoord.getBlockExist()) {
				exit = true;
				break;
			}
			if (isOpaque(newCoord)) {
				vp.xCoord -= vv.xCoord;
				vp.yCoord -= vv.yCoord;
				vp.zCoord -= vv.zCoord;

				newCoord.setPosition(vp);
				break;
			}
		}
		if (!exit) {
			int count = 0;
			while (!newCoord.equals(myCoord())) {
				Block block = newCoord.getBlock();
				if (block == Blocks.air || block == Eln.lightBlock) {
					count++;
					if (count == 2)
						break;
				}

				vp.xCoord -= vv.xCoord;
				vp.yCoord -= vv.yCoord;
				vp.zCoord -= vv.zCoord;
				newCoord.setPosition(vp);
			}
		}
		if (!exit)
			setLightAt(newCoord, newLight);
	}

	public boolean isOpaque(Coordonate coord) {
		Block block = coord.getBlock();
		boolean isNotOpaque = block == Blocks.air || !block.isOpaqueCube();
		if (block == Blocks.farmland)
			isNotOpaque = false;
		return !isNotOpaque;
	}

	public void publish() {
		Utils.print("Light published");
	}

	public void setLightAt(Coordonate coord, int value) {
		Coordonate oldLbCoord = lbCoord;
		lbCoord = new Coordonate(coord);
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

	Coordonate myCoord() {
		return lamp.sixNode.coordonate;
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
	 * @Override public void lightBlockDestructor(Coordonate coord) { if(coord.equals(lbCoord)) { light = 0; lbCoord = new Coordonate(myCoord()); //placeSpot(light); } }
	 */
}
