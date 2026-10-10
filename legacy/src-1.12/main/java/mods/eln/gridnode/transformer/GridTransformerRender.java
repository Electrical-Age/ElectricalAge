package mods.eln.gridnode.transformer;

import mods.eln.gridnode.GridDescriptor;
import mods.eln.gridnode.GridRender;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;

/**
 * Created by svein on 07/08/15.
 */
public class GridTransformerRender extends GridRender {

    private final GridDescriptor descriptor;

    public GridTransformerRender(TransparentNodeEntity entity, TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
        this.descriptor = (GridDescriptor) descriptor;
    }
}
