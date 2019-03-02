package mods.eln.sixnode.electricalweathersensor;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.init.Cable;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;

public class ElectricalWeatherSensorRender extends SixNodeElementRender {

    ElectricalWeatherSensorDescriptor descriptor;

    public ElectricalWeatherSensorRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalWeatherSensorDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        drawSignalPin(front.right(), descriptor.pinDistance);

        descriptor.draw();
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return Cable.Companion.getSignal().descriptor.render;
    }
}
