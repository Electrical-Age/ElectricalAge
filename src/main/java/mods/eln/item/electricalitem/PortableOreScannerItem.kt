package mods.eln.item.electricalitem

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Obj3D
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.wiki.Data
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import org.lwjgl.opengl.GL11

class PortableOreScannerItem(name: String, obj: Obj3D,
                             private var energyStorage: Double, internal var chargePower: Double, private var dischargePower: Double,
                             private var viewRange: Float, private var viewYAlpha: Float, private var resWidth: Int, private var resHeight: Int) : GenericItemUsingDamageDescriptor(name), IItemEnergyBattery {

    internal var base: Obj3DPart = obj.getPart("Base")
    internal var led: Obj3DPart = obj.getPart("Led")
    private var ledHalo: Obj3DPart = obj.getPart("LedHalo")
    private var textBat: Array<Obj3DPart> = (0..3).map { obj.getPart("TextBat$it") }.toTypedArray()
    private var textRun: Obj3DPart = obj.getPart("TextRun")
    private var textInit: Obj3DPart = obj.getPart("TextInit")
    internal var buttons: Obj3DPart = obj.getPart("Buttons")
    private var screenDamage: Array<Obj3DPart> = (0..2).map { obj.getPart("ScreenDamageL" + (it + 1)) }.toTypedArray()
    private var screenLuma: Obj3DPart = obj.getPart("ScreenLuma")

    private val damagePerBreakLevel = 3

    override fun onUpdate(stack: ItemStack, world: World, entity: Entity, par4: Int, par5: Boolean) {
        if (world.isRemote) return
        if (entity !is EntityPlayerMP) return
        val state = getState(stack)
        var counter = getCounter(stack)

        if (getDamage(stack) / damagePerBreakLevel >= 4) {
            if (state != State.Idle)
                setState(stack, State.Idle)
            return
        }

        when (state) {
            State.Boot -> if ((--counter).toInt() != 0) {
                setCounter(stack, counter)
            } else {
                setState(stack, State.Run)
            }
            State.Stop -> if ((--counter).toInt() != 0) {
                setCounter(stack, counter)
            } else {
                setState(stack, State.Idle)
            }
            else -> {}
        }
    }

    override fun onItemRightClick(s: ItemStack, w: World, p: EntityPlayer): ActionResult<ItemStack> {
        if (w.isRemote) return ActionResult(EnumActionResult.SUCCESS, s)
        val energy = getEnergy(s)
        val state = getState(s)

        when (state) {
            State.Idle -> if (energy > dischargePower) {
                setState(s, State.Boot)
                setCounter(s, bootTime)
            }
            State.Run -> {
                setState(s, State.Stop)
                setCounter(s, stopTime)
            }
            else -> {}
        }
        return ActionResult(EnumActionResult.SUCCESS, s)
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
    }

    override fun getDefaultNBT(): NBTTagCompound? {
        val nbt = NBTTagCompound()
        nbt.setDouble("e", energyStorage * 0.2)
        nbt.setByte("s", State.Boot.serialized)
        nbt.setShort("c", bootTime)
        nbt.setByte("d", 0.toByte())
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Discharge power: %sW", Utils.plotValue(dischargePower)))
        if (itemStack != null) {
            list.add(tr("Stored energy: %sJ (%s)", Utils.plotValue(getEnergy(itemStack)),
                (getEnergy(itemStack) / energyStorage * 100).toInt()))
        }
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDouble("e")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack).setDouble("e", value)
    }

    private fun getState(stack: ItemStack): State {
        return State.from(getNbt(stack).getByte("s")) ?: State.Idle
    }

    private fun setState(stack: ItemStack, value: State) {
        getNbt(stack).setByte("s", value.serialized)
    }

    private fun getCounter(stack: ItemStack): Short {
        return getNbt(stack).getShort("c")
    }

    private fun setCounter(stack: ItemStack, value: Short) {
        getNbt(stack).setShort("c", value)
    }

    private fun getDamage(stack: ItemStack): Byte {
        return getNbt(stack).getByte("d")
    }

    private fun setDamage(stack: ItemStack, value: Byte) {
        getNbt(stack).setByte("d", value)
    }

    override fun onDroppedByPlayer(stack: ItemStack, player: EntityPlayer): Boolean {
        setState(stack, State.Idle)
        return super.onDroppedByPlayer(stack, player)
    }

    override fun getEnergyMax(stack: ItemStack): Double {
        return energyStorage
    }

    override fun getChargePower(stack: ItemStack): Double {
        return chargePower
    }

    override fun getDischagePower(stack: ItemStack): Double {
        return dischargePower
    }

    override fun getPriority(stack: ItemStack): Int {
        return 0
    }

    override fun onBlockStartBreak(itemstack: ItemStack, x: Int, y: Int, z: Int, player: EntityPlayer): Boolean {
        if (!player.worldObj.isRemote) {
            setDamage(itemstack, (getDamage(itemstack) + 1).toByte())
        }
        return super.onBlockStartBreak(itemstack, x, y, z, player)
    }

    // TODO(1.10): Fix rendering
//    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
//        return true
//    }
//
//    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
//        return type != ItemRenderType.INVENTORY
//    }
//
//    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
//        if (type == ItemRenderType.INVENTORY) {
//            super.renderItem(type, item, *data)
//            UtilsClient.drawEnergyBare(type, (getEnergy(item) / getEnergyMax(item)).toFloat())
//            return
//        }
//
//        val energy = getEnergy(item)
//        val state = getState(item)
//
//        GL11.glPushMatrix()
//        val e: Entity?
//
//        when (type) {
//            IItemRenderer.ItemRenderType.ENTITY -> {
//                e = null
//                GL11.glTranslatef(0f, -0.2f, 0f)
//                GL11.glRotatef(90f, 0f, 0f, 1f)
//            }
//
//            IItemRenderer.ItemRenderType.EQUIPPED -> {
//                e = data[1] as Entity
//                GL11.glRotatef(130f, 0f, 0f, 1f)
//                GL11.glRotatef(140f, 1f, 0f, 0f)
//                GL11.glRotatef(-20f, 0f, 1f, 0f)
//                GL11.glScalef(1.6f, 1.6f, 1.6f)
//                GL11.glTranslatef(-0.2f, 0.7f, -0.0f)
//            }
//
//            IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON -> {
//                e = data[1] as Entity
//                GL11.glTranslatef(0f, 1f, 0f)
//                GL11.glRotatef(90f, 0f, 0f, 1f)
//                GL11.glRotatef(35f, 1f, 0f, 0f)
//                GL11.glTranslatef(0.0f, 1f, -0.2f)
//            }
//
//            IItemRenderer.ItemRenderType.INVENTORY -> {
//                GL11.glPopMatrix()
//                return
//            }
//
//            IItemRenderer.ItemRenderType.FIRST_PERSON_MAP -> e = null
//
//            else -> e = null
//        }
//
//        val drawScreen = e != null && UtilsClient.clientDistanceTo(e) < 10
//        val drawRay = drawScreen && state == State.Run
//
//        base.draw()
//
//        if (drawRay) {
//            GL11.glPushMatrix()
//
//            var oRender = Eln.clientLiveDataManager.getData(item, 1)
//            if (oRender == null)
//                oRender = Eln.clientLiveDataManager.newData(item, RenderStorage(viewRange, viewYAlpha, resWidth, resHeight), 1)
//            val render = oRender as RenderStorage
//
//            render.generate(e!!.worldObj, e.posX, Utils.getHeadPosY(e), e.posZ, e.rotationYaw * Math.PI.toFloat() / 180.0f, e.rotationPitch * Math.PI.toFloat() / 180.0f)
//
//            val scale = 1f / resWidth * 0.50f
//            GL11.glTranslatef(0.90668f, 0.163f, -0.25078f)
//            GL11.glRotatef(270f, 1f, 0f, 0f)
//            GL11.glRotatef(270f, 0f, 0f, 1f)
//            GL11.glScalef(scale, -scale, 1f)
//            render.draw()
//
//            GL11.glPopMatrix()
//
//            var r = 0f
//            var g = 0f
//            var b = 0f
//            var count = 0
//
//            var y = 0
//            while (y < resHeight) {
//                var x = 0
//                while (x < resHeight) {
//                    r += render.screenRed[y][x]
//                    g += render.screenGreen[y][x]
//                    b += render.screenBlue[y][x]
//                    count++
//                    x += 6
//                }
//                y += 6
//            }
//            r /= count.toFloat()
//            g /= count.toFloat()
//            b /= count.toFloat()
//            UtilsClient.drawHalo(screenLuma, r, g, b, e, false)
//        }
//
//        if (drawScreen) {
//            if (state == State.Idle) {
//                GL11.glColor4f(0.5f, 0.5f, 0.5f, 1f)
//                led.draw()
//                GL11.glColor4f(1f, 1f, 1f, 1f)
//                buttons.draw()
//            }
//            UtilsClient.disableLight()
//            if (state != State.Idle) {
//                GL11.glColor4f(1f, 1f, 1f, 1f)
//                buttons.draw()
//
//                var r = 0f
//                var g = 0f
//                var b = 0f
//                when (state) {
//                    State.Boot -> {
//                        r = 0.9f
//                        g = 0.4f
//                        b = 0f
//                    }
//                    State.Run -> {
//                        r = 0f
//                        g = 1f
//                        b = 0f
//                    }
//                    State.Stop -> {
//                        r = 1f
//                        g = 0f
//                        b = 0f
//                    }
//                    else -> {
//                    }
//                }
//                GL11.glColor4f(r * 0.6f, g * 0.6f, b * 0.6f, 1f)
//                led.draw()
//                UtilsClient.enableBlend()
//                UtilsClient.drawHaloNoLightSetup(ledHalo, r, g, b, e, false)
//            }
//
//            GL11.glColor4f(1f, 1f, 1f, 0.4f)
//            when (state) {
//                State.Boot -> textInit.draw()
//                State.Run -> {
//                    textRun.draw()
//                    val batLevel = Math.min(textBat.size - 1, (energy / energyStorage * textBat.size + 0.5f).toInt())
//                    textBat[batLevel].draw()
//                }
//                else -> {
//                }
//            }
//            UtilsClient.enableBlend()
//            GL11.glColor4f(1f, 1f, 1f, 1f)
//            var breakLevel = getDamage(item) / damagePerBreakLevel
//            if (state == State.Idle) breakLevel = Math.min(breakLevel, screenDamage.size - 1)
//            for (idx in 0 until breakLevel) {
//                if (idx == screenDamage.size) break
//                screenDamage[Math.min(screenDamage.size - 1, breakLevel - 1) - idx].draw()
//            }
//
//            UtilsClient.disableBlend()
//            UtilsClient.enableLight()
//        }
//
//        GL11.glPopMatrix()
//    }

    class RenderStorage(private var viewRange: Float, viewYAlpha: Float, var resWidth: Int, private var resHeight: Int) {
        private var camDist: Float = 0.toFloat()
        internal var screenRed: Array<FloatArray>
        internal var screenBlue: Array<FloatArray>
        internal var screenGreen: Array<FloatArray>
        private var worldBlocks: Array<Array<ShortArray>>
        private var worldBlocksDim: Int = 0
        private var worldBlocksDim2: Int = 0

        init {
            this.camDist = (resWidth.toDouble() / 2.0 / Math.tan((viewYAlpha / 2).toDouble())).toFloat()
            this.worldBlocksDim = (viewRange * 2 + 3).toInt()
            this.worldBlocksDim2 = this.worldBlocksDim / 2
            screenRed = Array(resHeight) { FloatArray(resWidth) }
            screenBlue = Array(resHeight) { FloatArray(resWidth) }
            screenGreen = Array(resHeight) { FloatArray(resWidth) }
            worldBlocks = Array(worldBlocksDim) { Array(worldBlocksDim) { ShortArray(worldBlocksDim) } }
        }

        class OreScannerConfigElement(var blockKey: Int, var factor: Float)

        fun generate(w: World, posX: Double, posY: Double, posZ: Double, alphaY: Float, alphaX: Float) {
            // TODO(1.10): This is pretty much entirely broken.
            val blockKeyFactor = OreColorMapping.map

            val posXint = Math.round(posX).toInt()
            val posYint = Math.round(posY).toInt()
            val posZint = Math.round(posZ).toInt()

            for (z in 0 until worldBlocksDim) {
                for (y in 0 until worldBlocksDim) {
                    for (x in 0 until worldBlocksDim) {
                        worldBlocks[x][y][z] = 65535.toShort()
                    }
                }
            }

            for (screenY in 0 until resHeight) {
                for (screenX in 0 until resWidth) {
                    var x = (posX - posXint).toFloat()
                    var y = (posY - posYint).toFloat()
                    var z = (posZ - posZint).toFloat()

                    var vx = (-(screenX - resWidth / 2)).toFloat()
                    var vy = (-(screenY - resHeight / 2)).toFloat()
                    var vz = camDist

                    run {
                        val sin = MathHelper.sin(alphaX)
                        val cos = MathHelper.cos(alphaX)

                        val temp = vy
                        vy = vy * cos - vz * sin
                        vz = vz * cos + temp * sin
                    }
                    run {
                        val sin = MathHelper.sin(alphaY)
                        val cos = MathHelper.cos(alphaY)

                        val temp = vx
                        vx = vx * cos - vz * sin
                        vz = vz * cos + temp * sin
                    }

                    val normInv = 1f / Math.sqrt((vx * vx + vy * vy + vz * vz).toDouble()).toFloat()
                    vx *= normInv
                    vy *= normInv
                    vz *= normInv

                    if (vx == 0f) vx += 0.0001f
                    if (vy == 0f) vy += 0.0001f
                    if (vz == 0f) vz += 0.0001f

                    val vxInv = 1f / vx
                    val vyInv = 1f / vy
                    val vzInv = 1f / vz

                    var stackRed = 0f
                    var stackBlue = 0f
                    var stackGreen = 0f
                    var d = 0f

                    while (d < viewRange) {
                        val xFloor = MathHelper.floor_float(x).toFloat()
                        val yFloor = MathHelper.floor_float(y).toFloat()
                        val zFloor = MathHelper.floor_float(z).toFloat()

                        var dx = x - xFloor
                        var dy = y - yFloor
                        var dz = z - zFloor
                        dx = if (vx > 0) (1 - dx) * vxInv else -dx * vxInv
                        dy = if (vy > 0) (1 - dy) * vyInv else -dy * vyInv
                        dz = if (vz > 0) (1 - dz) * vzInv else -dz * vzInv

                        val dBest = Math.min(Math.min(dx, dy), dz) + 0.01f

                        val xInt = xFloor.toInt() + worldBlocksDim2
                        val yInt = yFloor.toInt() + worldBlocksDim2
                        val zInt = zFloor.toInt() + worldBlocksDim2

                        var blockKey = worldBlocks[xInt][yInt][zInt]
                        if (blockKey == 65535.toShort()) {
                            val xBlock = posXint + xFloor.toInt()
                            val yBlock = posYint + yFloor.toInt()
                            val zBlock = posZint + zFloor.toInt()
                            blockKey = 0
                            if (yBlock in 0..255) {
                                val chunk = w.getChunkFromBlockCoords(BlockPos(xBlock, yBlock, zBlock))
                                val storage = chunk.blockStorageArray[yBlock shr 4]
                                if (storage != null) {
                                    val xLocal = xBlock and 0xF
                                    val yLocal = yBlock and 0xF
                                    val zLocal = zBlock and 0xF

                                    val state = storage.get(xLocal, yLocal, zLocal)
                                    blockKey = Block.getStateId(state).toShort()
                                }
                            }
                            if (blockKey >= 1024 * 64) {
                                blockKey = 0
                            }
                            worldBlocks[xInt][yInt][zInt] = blockKey
                        }

                        val dToStack: Float
                        dToStack = if (d + dBest < viewRange)
                            dBest
                        else {
                            viewRange - d
                        }

                        stackGreen += blockKeyFactor[blockKey.toInt()] * dToStack

                        // TODO(1.10): This needs a total rewrite.
                        val state = Block.getStateById(blockKey.toInt())
                        val b = state.block
                        //val b = Block.getBlockById((blockKey and 0xFFFU).toInt())
                        if (b !== Blocks.AIR && b !== Eln.lightBlock) {
                            stackRed += if (b.isVisuallyOpaque)
                                0.2f * dToStack
                            else
                                0.1f * dToStack
                        } else
                            stackBlue += 0.06f * dToStack

                        x += vx * dBest
                        y += vy * dBest
                        z += vz * dBest

                        d += dBest
                    }

                    screenRed[screenY][screenX] = stackRed - stackGreen * 0f
                    screenGreen[screenY][screenX] = stackGreen
                    screenBlue[screenY][screenX] = stackBlue - stackGreen * 0f
                }
            }
        }

        private fun noiseRand(): Float {
            return (Math.random().toFloat() - 0.5f) * 0.03f
        }

        @JvmOverloads
        fun draw(redFactor: Float = 1f, greenFactor: Float = 1f, blueFactor: Float = 1f) {
            UtilsClient.disableLight()
            UtilsClient.disableTexture()

            for (screenY in 0 until resHeight) {
                GL11.glBegin(GL11.GL_QUAD_STRIP)
                for (screenX in 0 until resWidth + 1) {

                    if (screenX != resWidth)
                        GL11.glColor3f(screenRed[screenY][screenX] * redFactor + noiseRand(), screenGreen[screenY][screenX] * greenFactor + noiseRand(), screenBlue[screenY][screenX] * blueFactor + noiseRand())
                    GL11.glVertex3f(screenX.toFloat(), screenY.toFloat(), 0f)
                    GL11.glVertex3f(screenX.toFloat(), (screenY + 1).toFloat(), 0f)

                }
                GL11.glEnd()
            }
            UtilsClient.enableTexture()
            UtilsClient.enableLight()
            GL11.glColor3f(1f, 1f, 1f)
        }
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {
        var energy = getEnergy(stack)
        val state = getState(stack)

        if (state != State.Idle) {
            energy -= dischargePower * time
            if (energy <= 0) {
                setState(stack, State.Idle)
                setEnergy(stack, 0.0)
                return
            }
            setEnergy(stack, energy)
        }
    }
}

private enum class State(val serialized: Byte) {
    Idle(0),
    Boot(1),
    Run(2),
    Stop(3),
    Error(4);

    companion object {
        private val map = State.values().associateBy(State::serialized)
        fun from(type: Byte) = map[type]
    }
}

private const val bootTime: Short = 4 * 20
private const val stopTime: Short = 1 * 20

object OreColorMapping {
    val map: FloatArray
        get() {
            return if (cache == null) {
                updateColorMapping()
            } else {
                cache!!
            }
        }

    private var cache: FloatArray? = null

    fun updateColorMapping(): FloatArray {
        val blockKeyMapping = FloatArray(1024 * 64)
        for (blockId in 0..4095) {
            for (meta in 0..15) {
                blockKeyMapping[blockId + (meta shl 12)] = 0f
            }
        }

        for (c in Eln.oreScannerConfig) {
            if (c.blockKey >= 0 && c.blockKey < blockKeyMapping.size)
                blockKeyMapping[c.blockKey] = c.factor
        }

        cache = blockKeyMapping
        return blockKeyMapping
    }
}
