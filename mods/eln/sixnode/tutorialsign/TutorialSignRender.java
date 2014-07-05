package mods.eln.sixnode.tutorialsign;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class TutorialSignRender extends SixNodeElementRender {

	
	TutorialSignDescriptor descriptor;

	public TutorialSignRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (TutorialSignDescriptor)descriptor;
	}

	String text;
	String baliseName;
	String texts[];
	
	@Override
	public void draw() {
		super.draw();
		descriptor.draw();
		/*
		GL11.glPushMatrix();

		GL11.glTranslatef(0.5f, 0f, 0f);
		GL11.glRotatef(90, 0, 0, 1);
		float scale = 1/64f;
		GL11.glScalef(scale, scale, scale);
		Minecraft.getMinecraft().fontRenderer.drawString(text,0,0,0);
		GL11.glPopMatrix();*/
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
