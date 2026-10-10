package mods.eln.node;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Which render object draws a device (1.12: the descriptor's RenderClass, built by reflection). On 26.x the client
 * setup registers a factory per descriptor class, so descriptors never name client classes. A descriptor without one
 * has no render yet (P3 in progress): its node draws nothing.
 */
public final class ElnRenders {
    private ElnRenders() {
    }

    public interface Six {
        SixNodeElementRender create(SixNodeEntity entity, Direction side, SixNodeDescriptor descriptor);
    }

    public interface Transparent {
        TransparentNodeElementRender create(TransparentNodeEntity entity, TransparentNodeDescriptor descriptor);
    }

    private static final Map<Class<?>, Six> SIX = new HashMap<>();
    private static final Map<Class<?>, Transparent> TRANSPARENT = new HashMap<>();

    public static void six(Class<? extends SixNodeDescriptor> descriptor, Six factory) {
        SIX.put(descriptor, factory);
    }

    public static void transparent(Class<? extends TransparentNodeDescriptor> descriptor, Transparent factory) {
        TRANSPARENT.put(descriptor, factory);
    }

    /** The factory for this descriptor's class (or the closest registered superclass), or null. */
    public static Six six(SixNodeDescriptor d) {
        for (Class<?> c = d.getClass(); c != null; c = c.getSuperclass()) {
            Six f = SIX.get(c);
            if (f != null) return f;
        }
        return null;
    }

    public static Transparent transparent(TransparentNodeDescriptor d) {
        for (Class<?> c = d.getClass(); c != null; c = c.getSuperclass()) {
            Transparent f = TRANSPARENT.get(c);
            if (f != null) return f;
        }
        return null;
    }

    public static int registered() {
        return SIX.size() + TRANSPARENT.size();
    }
}
