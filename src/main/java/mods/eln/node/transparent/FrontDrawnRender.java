package mods.eln.node.transparent;

/**
 * The render for a device with one static model and nothing animated: it turns to the device's front and lets the
 * descriptor draw. Use it instead of writing a render class whose draw() is only {@code front.glRotateXnRef();
 * desc.draw();} (seven of those were folded into this one, consolidation 2026-11-21).
 */
public class FrontDrawnRender extends TransparentNodeElementRender {
    /** A descriptor that draws its model in front-relative coordinates. */
    public interface Drawn {
        void draw();
    }

    final Drawn drawn;

    public FrontDrawnRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        drawn = (Drawn) d;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        drawn.draw();
    }
}
