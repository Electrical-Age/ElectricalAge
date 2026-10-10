package mods.eln.transparentnode.autominer;

/**
 * Slot ids and other constants of the 1.12 AutoMinerContainer (legacy/). The menu itself (AbstractContainerMenu + Screen) comes
 * with P4; elements only need the constants until then.
 */
public final class AutoMinerContainer {
    private AutoMinerContainer() {
    }

    public static final int electricalDrillSlotId = 0;
    public static final int MiningPipeSlotId = 2;
    public static final int StorageStartId = 3;
    public static final int StorageSize = 0;
    public static final int inventorySize = StorageStartId + StorageSize;
}
