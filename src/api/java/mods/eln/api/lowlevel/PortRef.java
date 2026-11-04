package mods.eln.api.lowlevel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Objects;

/**
 * An existing EA port, by place: the EA block at pos (in the level the circuit is hosted in), its face side (the face
 * a cable from the neighbour at pos.relative(side) would meet), the cable position on that face, and the kind of port.
 * Plain data: a link resolves it before every step (PortLink). Save it with CODEC (its keys are fixed: pos, side,
 * position, kind), under a key of your own.
 */
public record PortRef(BlockPos pos, Direction side, PortPosition position, PortKind kind) {
    public PortRef {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(kind, "kind");
        pos = pos.immutable();
    }

    /** The port of kind on side of the EA block at pos, at whichever position has one (PortPosition.ANY). */
    public static PortRef of(BlockPos pos, Direction side, PortKind kind) {
        return new PortRef(pos, side, PortPosition.ANY, kind);
    }

    public static final Codec<PortRef> CODEC = RecordCodecBuilder.create(i -> i.group(
        BlockPos.CODEC.fieldOf("pos").forGetter(PortRef::pos),
        Direction.CODEC.fieldOf("side").forGetter(PortRef::side),
        named(PortPosition.class).fieldOf("position").forGetter(PortRef::position),
        named(PortKind.class).fieldOf("kind").forGetter(PortRef::kind)
    ).apply(i, PortRef::new));

    /** An enum by its name; an unknown name is an error, not a default. */
    private static <E extends Enum<E>> Codec<E> named(Class<E> type) {
        return Codec.STRING.comapFlatMap(s -> {
            try {
                return DataResult.success(Enum.valueOf(type, s));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "no " + type.getSimpleName() + " named " + s);
            }
        }, Enum::name);
    }
}
