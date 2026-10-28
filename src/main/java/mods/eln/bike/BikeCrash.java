package mods.eln.bike;

import mods.eln.Eln;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: the rider of this bike ran into something fast and comes off (ElectricBike.onCrash checks it). */
public record BikeCrash(int entityId) implements CustomPacketPayload {
    public static final Type<BikeCrash> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "bike_crash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BikeCrash> CODEC =
        ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(BikeCrash::new, BikeCrash::entityId);

    @Override
    public Type<BikeCrash> type() {
        return TYPE;
    }
}
