package mods.eln.mechanical

import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry

class SteamTurbineDescriptor(baseName : String, obj : Obj3D) :
        SimpleShaftDescriptor(baseName, SteamTurbineElement::class, SteamTurbineRender::class, EntityMetaTag.Fluid) {
    // Overall time for steam input changes to take effect, in seconds.
    public val steamInertia = 20f
    // Optimal steam consumed per second, mB.
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    public val steamConsumption = 7200f
    // Joules per mB, at optimal turbine speed.
    // Computed to equal what you'd get from Railcraft steam engines, plus a small
    // bonus because you're using Electrical Age you crazy person you.
    // This pretty much fills up a VHV line. The generator drag gives us a bit of leeway.
    // TODO: This should be tied into the config options.
    public val steamPower = 2.2f

    val optimalRads = absoluteMaximumShaftSpeed * 0.8f

    override val obj = obj
    override val static = arrayOf(
            obj.getPart("Cowl"),
            obj.getPart("Stand")
    )
    override val rotating = arrayOf(
            obj.getPart("Shaft"),
            obj.getPart("Fan")
    )

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add("Converts steam into mechanical energy.")
        list.add("Nominal usage ->")
        list.add("  Steam input: %f mB/s".format(steamConsumption))
        list.add(Utils.plotPower("  Power out: ", (steamConsumption * steamPower).toDouble()))
        list.add(Utils.plotRads("  Optimal rads: ", optimalRads))
        list.add(Utils.plotRads("Max rads:  ", absoluteMaximumShaftSpeed))
    }
}

class SteamTurbineElement(node : TransparentNode, desc_ : TransparentNodeDescriptor) :
        SimpleShaftElement(node, desc_) {
    val steam = FluidRegistry.getFluid("steam") ?: FluidRegistry.getFluid("lava")
    val desc = desc_ as SteamTurbineDescriptor

    val steamTank = TransparentNodeElementFluidHandler(1000)
    var steamRate = 0f
    var efficiency = 0.0
    val turbineSlowProcess = TurbineSlowProcess()

    inner class TurbineSlowProcess: IProcess, INBTTReady {
        val rc = RcInterpolator(desc.steamInertia)

        override fun process(time: Double) {
            val ss = steamTank.drain(ForgeDirection.DOWN, (desc.steamConsumption * time).toInt(), true)
            val steam = ss?.amount ?: 0
            rc.target = steam / time.toFloat()
            rc.step(time.toFloat())
            steamRate = rc.get()
            efficiency = Math.pow(Math.cos((shaft.rads - desc.optimalRads) / (desc.optimalRads * 1.1) * Math.PI / 2), 3.0)
            val power = steamRate * desc.steamPower * efficiency
            shaft.energy += power * time.toFloat()
        }

        override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
            rc.readFromNBT(nbt, str)
        }

        override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
            rc.writeToNBT(nbt, str)
        }
    }

    init {
        slowProcessList.add(turbineSlowProcess)
        steamTank.setFilter(steam)
    }

    override fun getFluidHandler() = steamTank

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?) = null
    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = null
    override fun getConnectionMask(side: Direction?, lrdu: LRDU?) = 0
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun thermoMeterString(side: Direction?) =  Utils.plotPercent(" Eff:", efficiency) + steamRate.toString() + "mB/s"

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        steamTank.writeToNBT(nbt, "tank")
        turbineSlowProcess.writeToNBT(nbt, "proc")
    }
    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        steamTank.readFromNBT(nbt, "tank")
        turbineSlowProcess.readFromNBT(nbt, "proc")
    }
}

class SteamTurbineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor): ShaftRender(entity, desc) {}
