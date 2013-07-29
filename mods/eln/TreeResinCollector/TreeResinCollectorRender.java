package mods.eln.TreeResinCollector;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class TreeResinCollectorRender extends SixNodeElementRender{

	TreeResinCollectorDescriptor descriptor;
	public TreeResinCollectorRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (TreeResinCollectorDescriptor) descriptor;
	}

	
	
	@Override
	public void draw() {
		// TODO Auto-generated method stub
		super.draw();
		
		LRDU.Down.glRotateOnX();
		descriptor.draw(stock);
	}
	
	float stock;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		
		try {
			stock = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
