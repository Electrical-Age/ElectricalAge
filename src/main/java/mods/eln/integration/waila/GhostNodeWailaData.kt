package mods.eln.integration.waila

import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.packets.GhostNodeWailaResponsePacket
import net.minecraft.item.ItemStack

data class GhostNodeWailaData(val realCoord: Coordinate,
                              val itemStack: ItemStack?,
                              val realType: Byte = GhostNodeWailaResponsePacket.UNKNOWN_TYPE,
                              val realSide: Direction = Direction.XN)
