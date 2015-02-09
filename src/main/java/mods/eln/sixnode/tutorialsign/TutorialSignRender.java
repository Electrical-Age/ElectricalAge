package mods.eln.sixnode.tutorialsign;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class TutorialSignRender extends SixNodeElementRender {
    
	TutorialSignDescriptor descriptor;

    String text;
    String baliseName;
    String texts[];

    RcInterpolator lightInterpol = new RcInterpolator(0.4f);

	public TutorialSignRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (TutorialSignDescriptor)descriptor;
	}

	@Override
	public void draw() {
		super.draw();
		descriptor.draw(lightInterpol.get());
	}
	
	@Override
	public void refresh(float deltaT) {
		lightInterpol.step(deltaT);
		super.refresh(deltaT);
	}

	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			baliseName = stream.readUTF();
			text = stream.readUTF();
			texts = text.split("\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new TutorialSignGui(this);
	}
}
