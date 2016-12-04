package mods.eln.integration.waila

import mods.eln.misc.Coordonate
import mods.eln.packets.GhostNodeWailaResponsePacket
import net.minecraft.item.ItemStack

data class GhostNodeWailaData(val realCoord: Coordonate,
                              val itemStack: ItemStack?,
                              val realType: Byte = GhostNodeWailaResponsePacket.UNKNOWN_TYPE)
