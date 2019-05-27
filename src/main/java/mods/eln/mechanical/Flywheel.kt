package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.debug.DebugType
import mods.eln.misc.Direction
import mods.eln.misc.LinearFunction
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.IProcess
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.DamageSource

class FlywheelDescriptor(baseName: String, obj: Obj3D) : SimpleShaftDescriptor(baseName,
    FlyWheelElement::class, ShaftRender::class, EntityMetaTag.Basic) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Flywheel"), obj.getPart("Shaft"))
}

class FlyWheelElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : StraightJointElement(node, desc_) {
    override val shaftMass = 10.0

    inner class FlyWheelFlingProcess : IProcess {
        val interval = 0.05
        val yTolerance = 1.0
        val xzTolerance = 0.5
        val minRads = 5.0
        val velocityF = LinearFunction(0f, 0f, 1000f, 10f)
        val damageF = LinearFunction(5f, 1f, 1000f, 10f)

        var timer = 0.0

        override fun process(time: Double) {
            timer += time
            if(timer >= interval) {
                timer = 0.0
                slowProcess()
            }
        }

        fun slowProcess() {
            // Utils.println("FFP.sP: tick")
            val rads = shaft.rads
            if(rads < minRads) return
            val coord = coordonate()
            val objects = coord.world().getEntitiesWithinAABB(Entity::class.java, coord.getAxisAlignedBB(1))
            //if(objects.size > 0) Utils.println("FFP.sP: within range: " + objects.size)
            for(obj in objects) {
                val ent = obj as Entity
                Eln.dp.println(DebugType.MECHANICAL, String.format("FPP.sP: considering %s", ent))
                val dx = Math.abs(ent.posX - coord.x - 0.5)
                val dy = Math.abs(ent.posY - coord.y - 1)
                val dz = Math.abs(ent.posZ - coord.z - 0.5)
                if(dy > yTolerance) {
                    Eln.dp.println(DebugType.MECHANICAL, "FPP.sP: dy out of range (" + dy + "; c.y " + coord.y + " e.y" + ent.posY + "): " + ent)
                    continue
                }
                if(dx > xzTolerance) {
                    Eln.dp.println(DebugType.MECHANICAL, "FPP.sP: dx out of range (" + dx + "; c.x " + coord.x + " e.x" + ent.posX + "): " + ent)
                    continue
                }
                if(dz > xzTolerance) {
                    Eln.dp.println(DebugType.MECHANICAL, "FPP.sP: dz out of range (" + dz + "; c.z " + coord.z + " e.z" + ent.posZ + "): " + ent)
                    continue
                }
                val mag = velocityF.getValue(rads)
                val vel = when(front) {
                    Direction.ZN, Direction.ZP -> arrayOf(0.0, mag * 0.1, mag)
                    Direction.XN, Direction.XP -> arrayOf(mag, mag * 0.1, 0.0)
                    else -> arrayOf(0.0, mag, 0.0) // XXX
                }
                ent.addVelocity(vel[0], vel[1], vel[2])
                var dmg = damageF.getValue(rads).toInt()
                if(ent !is EntityLivingBase) dmg = 0
                Eln.dp.println(DebugType.MECHANICAL, "FFP.sP: ent " + ent + " flung " + vel.joinToString(",") + " for damage " + dmg)
                if(dmg <= 0) continue
                ent.attackEntityFrom(DamageSource("Flywheel"), dmg.toFloat())
            }
        }
    }
    var flingProcess = FlyWheelFlingProcess()

    init {
        slowProcessList.add(flingProcess)
    }

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put("Speed", Utils.plotRads("", shaft.rads))
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        return info
    }
}
