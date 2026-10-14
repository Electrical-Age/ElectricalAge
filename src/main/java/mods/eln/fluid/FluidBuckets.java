package mods.eln.fluid;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * 26.x (Baughn): buckets work on every EA fluid device, so players can bootstrap by hand before they have pumps and
 * pipes. A filled bucket pours 1000 mB in, if the device takes all of it; an empty bucket takes 1000 mB out of the
 * first tank that has a full bucket of something with a bucket item.
 */
public final class FluidBuckets {
    private FluidBuckets() {
    }

    /** Use the player's main-hand bucket on the handler. True if the bucket was used (or is a bucket at all). */
    @SuppressWarnings("unchecked")
    public static boolean use(Player player, Object handler) {
        if (player == null || !(handler instanceof ResourceHandler<?> h0)) return false;
        ResourceHandler<FluidResource> h = (ResourceHandler<FluidResource>) h0;
        ItemStack held = player.getMainHandItem();
        if (held.is(Items.BUCKET)) {
            for (int i = 0; i < h.size(); i++) {
                FluidResource r = h.getResource(i);
                if (r.isEmpty() || h.getAmountAsLong(i) < FluidType.BUCKET_VOLUME) continue;
                ItemStack filled = new ItemStack(r.getFluid().getBucket());
                if (filled.isEmpty() || filled.is(Items.BUCKET)) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    if (h.extract(i, r, FluidType.BUCKET_VOLUME, tx) != FluidType.BUCKET_VOLUME) continue;
                    tx.commit();
                }
                give(player, held, filled);
                player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
                return true;
            }
            return true;   // nothing to take: don't open a GUI with a bucket in hand
        }
        if (held.getItem() instanceof BucketItem bucket && bucket.getContent() != Fluids.EMPTY) {
            FluidResource r = FluidResource.of(bucket.getContent());
            try (Transaction tx = Transaction.openRoot()) {
                if (h.insert(r, FluidType.BUCKET_VOLUME, tx) != FluidType.BUCKET_VOLUME) return true;
                tx.commit();
            }
            if (!player.isCreative()) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
            return true;
        }
        return false;
    }

    private static void give(Player player, ItemStack held, ItemStack filled) {
        if (player.isCreative()) {
            if (!player.getInventory().contains(filled)) player.getInventory().add(filled);
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, filled);
        else if (!player.getInventory().add(filled)) net.minecraft.world.Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), filled);
    }
}
