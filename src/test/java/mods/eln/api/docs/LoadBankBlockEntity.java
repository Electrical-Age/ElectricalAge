package mods.eln.api.docs;

import mods.eln.api.RelativeFace;
import mods.eln.api.lowlevel.FaceConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** docs/api.md's low level at another mod's block entity. */
public class LoadBankBlockEntity extends BlockEntity {
    private LoadBank bank;

    public LoadBankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // doc:begin host
    @Override
    public void onLoad() {
        super.onLoad();
        if (!(getLevel() instanceof ServerLevel)) return;  // circuits are the server's
        bank = new LoadBank(worldPosition.below(3));      // a fresh circuit each load: circuits save nothing
        bank.circuit.hostAt(this, new FaceConnection(RelativeFace.BACK, bank.in));
    }
    // doc:end host
}
