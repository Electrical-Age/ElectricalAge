package mods.eln.transparentnode.autominer;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class AutoMinerDescriptor extends TransparentNodeDescriptor {

	Coordonate[] powerCoord;
	Coordonate lightCoord, miningCoord;
	public Obj3D obj;
	public Obj3DPart core, gui, lampSocket, lampOff, lampOn, head, pipe;
	public Obj3DPart buttonFixed;
	public Obj3DPart[] buttons, ledsA, ledsP;
	float[] buttonsStateDefault;
	boolean[] ledsAStateDefault;
	boolean[] ledsPStateDefault;
	int buttonsCount = 5;
	int ledsACount = 11;
	int ledsPCount = 8;
	int deltaX, deltaY, deltaZ;
	ElectricalCableDescriptor cable;
	
	SoundCommand runningSound;

    double nominalVoltage;
    double pipeOperationTime, pipeOperationEnergy, pipeOperationPower;

    double pipeOperationRp;
	
	public AutoMinerDescriptor(String name, 
                               Obj3D obj, 
                               Coordonate[] powerCoord, Coordonate lightCoord, Coordonate miningCoord, 
                               int deltaX, int deltaY, int deltaZ, 
                               ElectricalCableDescriptor cable, 
                               double pipeOperationTime, double pipeOperationEnergy) {
		super(name, AutoMinerElement.class, AutoMinerRender.class);
		this.nominalVoltage = cable.electricalNominalVoltage;
		this.pipeOperationTime = pipeOperationTime;
		this.pipeOperationEnergy = pipeOperationEnergy;
		pipeOperationPower = pipeOperationEnergy / pipeOperationTime;
		pipeOperationRp = nominalVoltage * nominalVoltage / pipeOperationPower;
		this.cable = cable;
        
		this.powerCoord = powerCoord;
		this.lightCoord = lightCoord;
		this.miningCoord = miningCoord;
		this.obj = obj;
		
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		this.deltaZ = deltaZ;
		
		core = obj.getPart("AutominerCore");
		gui = obj.getPart("AutominerGUI");
		lampSocket = obj.getPart("LampSocket");
		lampOff = obj.getPart("LampOff");
		lampOn = obj.getPart("LampOn");
		head = obj.getPart("MinerHead");
		pipe = obj.getPart("MinerPipe");
		
		buttonFixed = obj.getPart("ButtonsFixed");

		buttons = new Obj3DPart[buttonsCount];
		buttonsStateDefault = new float[buttonsCount];
		for (int idx = 0; idx < buttonsCount; idx++) {
			buttons[idx] = obj.getPart("Button" + idx);
			buttonsStateDefault[idx] = (float) Math.random();
		}
		
		ledsA = new Obj3DPart[ledsACount];
		ledsAStateDefault = new boolean[ledsACount];
		for (int idx = 0; idx < ledsACount; idx++){
			ledsA[idx] = obj.getPart("ledA" + idx);
			ledsAStateDefault[idx] = Math.random() > 0.5;
		}
		
		ledsP = new Obj3DPart[ledsPCount];
		ledsPStateDefault = new boolean[ledsPCount];
		for (int idx = 0; idx < ledsPCount; idx++) {
			ledsP[idx] = obj.getPart("ledP" + idx);
			ledsPStateDefault[idx] = Math.random() > 0.5;
		}
		
		runningSound = new SoundCommand("eln:autominer", 2.13);
	}

	public void applyTo(ElectricalLoad load) {
		cable.applyTo(load);
	}
	
	@Override
	public void setParent(Item item, int damage) {
		super.setParent(item, damage);
		Data.addMachine(newItemStack());
	}
	
	@Override
	public boolean use2DIcon() {
		return false;
	}
	
	@Override
	public boolean mustHaveFloor() {
		return false;
	}
	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
		super.addInformation(itemStack, entityPlayer, list, par4);
		Collections.addAll(list, tr("Excavates on a small radius.\nExtracts ore on a bigger radius:\n10 blocks radius after 10 blocks depth.").split("\n"));
		list.add(tr("Nominal voltage: %1$V", Utils.plotValue(nominalVoltage)));
	}
	
	
	
	void draw(boolean lampState,float[] buttonsState, boolean[] ledsAState, boolean[] ledsPState) {
		GL11.glRotatef(-90, 0, 1, 0);
		GL11.glTranslatef(0, -1.5f, 0);
		//GL11.glScalef(0.5f, 0.5f, 0.5f);
        
		for (int idx = 0; idx < buttonsCount; idx++) {
			GL11.glPushMatrix();
			GL11.glTranslatef(0, (1 - buttonsState[idx]) * 0.01f, 0);
			if (buttons[idx] != null) buttons[idx].draw();
			GL11.glPopMatrix();
		}
		
		UtilsClient.disableLight();
		for (int idx = 0; idx < ledsACount; idx++) {
			GL11.glColor3f(0, ledsAState[idx] ? 0 : 1, 0);
			if (ledsA[idx] != null) ledsA[idx].draw();
		}

		for (int idx = 0; idx < ledsPCount; idx++) {
			GL11.glColor3f(0, ledsPState[idx] ? 0 : 1, 0);
			if (ledsP[idx] != null) ledsP[idx].draw();
		}
		UtilsClient.enableLight();
		GL11.glColor3f(1, 1, 1);
		
		UtilsClient.disableCulling();
		core.draw();
		gui.draw();
		buttonFixed.draw();
		lampSocket.draw();
		if (lampState)
			lampOff.draw();
		else
			lampOn.draw();
		UtilsClient.enableCulling();
	}
    
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		super.renderItem(type, item, data);
		GL11.glScalef(0.18f, 0.18f, 0.18f);
		draw(false, buttonsStateDefault, ledsAStateDefault, ledsPStateDefault);
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	public Coordonate[] getPowerCoordonate(World w) {
		Coordonate[] temp = new Coordonate[powerCoord.length];
		for (int idx = 0; idx < temp.length; idx++){
			temp[idx] = new Coordonate(powerCoord[idx]);
			temp[idx].setDimention(w.provider.dimensionId);
		}
		return temp;
	}
	
	public int getSpawnDeltaX() {
		return deltaX;
	}
    
	public int getSpawnDeltaY() {
		return deltaY;
	}
    
	public int getSpawnDeltaZ() {
		return deltaZ;
	}
}
