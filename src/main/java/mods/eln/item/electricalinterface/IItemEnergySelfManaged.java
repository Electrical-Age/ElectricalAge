package mods.eln.item.electricalinterface;

/**
 * An item battery that manages its own charging from the player's batteries, so EA's item energy
 * (ItemEnergyInventoryProcess) leaves it alone: Stasis Delvers' suit pieces, which share charge evenly between the
 * pieces (a suit protects only while every piece has some; EA's would fill them one at a time). Chargers still charge it.
 */
public interface IItemEnergySelfManaged {
}
