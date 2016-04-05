package mods.eln.item.electricalinterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.FMLCommonHandler;

public class ItemEnergyInventoryProcess implements IProcess {

	static final double energyUpdatePeriod = 0.5;
	double energyUpdateTimout = energyUpdatePeriod;

	class Element {
		public ItemStack stack;
		public IItemEnergyBattery i;
		public int p;

		public Element(ItemStack stack, IItemEnergyBattery i) {
			this.stack = stack;
			this.i = i;
			p = i.getPriority(stack);
		}
	}

	static class Exclusion {
		public double timeout;
		public Object o;

		public Exclusion(double timeout, Object o) {
			this.timeout = timeout;
			this.o = o;
			Utils.println("new");
		}
	}

	LinkedList<Exclusion> exclude = new LinkedList<ItemEnergyInventoryProcess.Exclusion>();

	// HashMap<Object, Double> exclude = new HashMap<Object, Double>();

	public void addExclusion(Object o, double timeout) {
		Exclusion exclusion = null;
		for (Exclusion e : exclude) {
			if (e.o == o) {
				exclusion = e;
			}
		}

		if (exclusion == null) {
			exclusion = new Exclusion(timeout, o);
			exclude.add(exclusion);
		}

		exclusion.timeout = timeout;
	}

	boolean isExcluded(Object o) {
		for (Exclusion e : exclude) {
			if (e.o == o) {
				Utils.println("Exclude");
				return true;
			}
		}
		return false;
	}

	@Override
	public void process(double time) {
		Iterator<Exclusion> ie = exclude.iterator();
		while (ie.hasNext()) {
			Exclusion e = ie.next();
			e.timeout -= 0.05;
			if (e.timeout < 0) {
				ie.remove();
			}
		}

		energyUpdateTimout -= time;

		if (energyUpdateTimout > 0)
			return;
		energyUpdateTimout += energyUpdatePeriod;
		time = energyUpdatePeriod;

		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

		ArrayList<Element> list = new ArrayList<Element>();

		for (Object obj : server.getConfigurationManager().playerEntityList) {
			EntityPlayerMP player = (EntityPlayerMP) obj;
			list.clear();

			for (ItemStack stack : player.inventory.armorInventory) {

				Object o = Utils.getItemObject(stack);

				if (o instanceof IItemEnergyBattery) {
					if (isExcluded(o))
						continue;
					list.add(new Element(stack, (IItemEnergyBattery) o));
				}
			}

			for (ItemStack stack : player.inventory.mainInventory) {
				Object o = Utils.getItemObject(stack);

				if (o instanceof IItemEnergyBattery) {
					if (isExcluded(o))
						continue;
					list.add(new Element(stack, (IItemEnergyBattery) o));
				}
			}

			for (Element e : list) {
				e.i.electricalItemUpdate(e.stack, energyUpdatePeriod);
			}

			if (Eln.saveConfig.infinitePortableBattery) {
				for (Element e : list) {
					double chargePower = e.i.getChargePower(e.stack);
					double energy = Math.min(e.i.getEnergyMax(e.stack), e.i.getEnergy(e.stack) + e.i.getChargePower(e.stack) * time);
					e.i.setEnergy(e.stack, energy);
				}
			} else {
				boolean rememberDst = false;
				double rememberDstEToDstMax = 0;
				while (true) {
					Element src = getMax(list);

					if (src == null)
						break;

					double eFromSrc = Math.min(src.i.getEnergy(src.stack), src.i.getDischagePower(src.stack) * time);
					double eStart = eFromSrc;

					boolean done = false;
					while (eFromSrc != 0) {
						Element dst = getMin(list);
						if (dst.p == src.p) {
							done = true;
							break;
						}

						double eToDstMax;

						if (rememberDst) {
							eToDstMax = rememberDstEToDstMax;
							rememberDst = false;
						} else {
							eToDstMax = Math.min(dst.i.getEnergyMax(dst.stack) - dst.i.getEnergy(dst.stack), dst.i.getChargePower(dst.stack) * time);
						}

						double eToDst = Math.min(eFromSrc, eToDstMax);
						eFromSrc -= eToDst;
						dst.i.setEnergy(dst.stack, dst.i.getEnergy(dst.stack) + eToDst);
						eToDstMax -= eToDst;

						if (eToDstMax == 0) {
							list.remove(dst);
						} else {
							rememberDst = true;
							rememberDstEToDstMax = eToDstMax;
						}
					}

					src.i.setEnergy(src.stack, src.i.getEnergy(src.stack) - (eStart - eFromSrc));

					if (done)
						break;

					list.remove(src);

					if (list.size() < 2)
						break;
				}
			}
		}
	}

	Element getElement(List<Element> list, int priority) {

		for (Element e : list) {
			if (priority == e.p) {
				return e;
			}
		}
		return null;
	}

	Element getMin(List<Element> list) {
		Element find = null;
		for (Element e : list) {
			if (find == null || find.p > e.p) {
				find = e;
			}
		}
		return find;
	}

	Element getMax(List<Element> list) {
		Element find = null;
		for (Element e : list) {
			if (find == null || find.p < e.p) {
				find = e;
			}
		}
		return find;
	}
}
