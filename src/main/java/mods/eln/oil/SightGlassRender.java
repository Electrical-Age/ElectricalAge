package mods.eln.oil;

import mods.eln.node.transparent.FrontDrawnRender;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * A static model with a sight glass (the Still Pot, the column base): FrontDrawnRender's model, then the liquid in the
 * glass as its element last published it (SightGlass).
 */
public class SightGlassRender extends FrontDrawnRender {
    /** A descriptor that draws its glass's liquid in front-relative coordinates. */
    public interface Glazed extends Drawn {
        void drawLevel(SightGlass glass);
    }

    final Glazed glazed;
    public SightGlass glass = SightGlass.EMPTY;

    public SightGlassRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        glazed = (Glazed) d;
    }

    @Override
    public void draw() {
        super.draw();
        glazed.drawLevel(glass);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            glass = SightGlass.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
