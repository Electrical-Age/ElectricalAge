package mods.eln.bike;

import mods.eln.Eln;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: the rider switches the bike's front lamp (a right-click with an empty hand while riding). */
public record BikeLamp() implements CustomPacketPayload {
    public static final BikeLamp INSTANCE = new BikeLamp();
    public static final Type<BikeLamp> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "bike_lamp"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BikeLamp> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<BikeLamp> type() {
        return TYPE;
    }
}
