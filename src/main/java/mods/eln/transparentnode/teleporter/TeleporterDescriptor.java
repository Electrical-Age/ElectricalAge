package mods.eln.transparentnode.teleporter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.GhostNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;

public class TeleporterDescriptor extends TransparentNodeDescriptor{

	private Obj3D obj;
	public Obj3DPart main,ext_control,ext_power;
	public Obj3DPart door_out,door_in,door_in_charge;
	public Obj3DPart indoor_open,indoor_closed;
	public Obj3DPart outlampline0_alpha,outlampline0;
	public Obj3DPart[] leds = new Obj3DPart[10];
	public Obj3DPart scr0_electrictity,scr1_cables,scr2_transporter,scr3_userin,scr5_dooropen,src4_doorclosed;
	public Obj3DPart gyro_alpha,gyro,whiteblur;
	
	public TeleporterDescriptor(
			String name,Obj3D obj,
			ElectricalCableDescriptor cable,
			Coordonate areaCoordonate,Coordonate lightCoordonate,
			int areaH,
			Coordonate[] powerCoordonate,
			GhostGroup ghostDoorOpen,GhostGroup ghostDoorClose
			
			) {
		super(name, TeleporterElement.class, TeleporterRender.class);
		this.cable = cable;
		this.obj = obj;
		this.powerCoordonate = powerCoordonate;
		if(obj != null){
			main = obj.getPart("main");
			ext_control = obj.getPart("ext_control");
			ext_power = obj.getPart("ext_power");
			door_out = obj.getPart("door_out");
			door_in_charge = obj.getPart("door_in_charge");
			door_in = obj.getPart("door_in");
			indoor_closed = obj.getPart("indoor_closed");
			indoor_open = obj.getPart("indoor_open");
			outlampline0_alpha = obj.getPart("outlampline0_alpha");
			outlampline0 = obj.getPart("outlampline0");
			scr0_electrictity = obj.getPart("scr0_electrictity");
			scr1_cables = obj.getPart("scr1_cables");
			scr2_transporter = obj.getPart("scr2_transporter");
			scr3_userin = obj.getPart("scr3_userin");
			scr5_dooropen = obj.getPart("scr5_dooropen");
			src4_doorclosed = obj.getPart("src4_doorclosed");
			gyro_alpha = obj.getPart("gyro_alpha");
			gyro = obj.getPart("gyro");
			whiteblur = obj.getPart("whiteblur");
			
			for(int idx = 0;idx < 10;idx++){
				leds[idx] = obj.getPart("led"+idx);
			}
		}
		this.areaCoordonate = areaCoordonate;
		this.areaH = areaH;
		this.ghostDoorClose = ghostDoorClose;
		this.ghostDoorOpen = ghostDoorOpen;
		this.lightCoordonate = lightCoordonate;
	}

	public GhostGroup ghostDoorOpen,ghostDoorClose;
	
	int areaH;
	public Coordonate areaCoordonate,lightCoordonate;
	
	public AxisAlignedBB getBB(Coordonate c,Direction front){
		Coordonate temp = new Coordonate(areaCoordonate);
		temp.setDimention(c.dimention);
		temp.applyTransformation(front, c);
		
		AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(temp.x,temp.y,temp.z,temp.x+1,temp.y + areaH,temp.z +1);
		return bb;
	}
	
	public Coordonate getTeleportCoordonate(Direction front,Coordonate c) {
		Coordonate temp = new Coordonate(areaCoordonate);
		temp.setDimention(c.dimention);
		temp.applyTransformation(front, c);
		
		return temp;
	}
	
	
	@Override
	public void setParent(Item item, int damage) {
		
		super.setParent(item, damage);
		Data.addMachine(newItemStack());
	}
	
	public ElectricalCableDescriptor cable;


	public void draw() {
		if(main != null) main.draw();
		if(ext_control != null) ext_control.draw();
		if(ext_power != null) ext_power.draw();
		if(door_out != null) door_out.draw();
	}

	Coordonate[] powerCoordonate;
	public Coordonate[] getPowerCoordonate(World w) {
		Coordonate[] temp = new Coordonate[powerCoordonate.length];
		for(int idx = 0;idx < temp.length;idx++){
			temp[idx] = new Coordonate(powerCoordonate[idx]);
			temp[idx].setDimention(w.provider.dimensionId);
		}
		return temp;
	}

	
	@Override
	public int getSpawnDeltaX() {
		
		return 4;
	}
	
	String chargeSound = null;
	float chargeVolume = 0;
	@Override
	public boolean use2DIcon() {
		return false;
	}
	public TeleporterDescriptor setChargeSound(String sound, float volume) {
		chargeSound = sound;
		chargeVolume = volume;
		return this;
	}
	
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		objItemScale(obj);
		main.draw();
		ext_control.draw();
		ext_power.draw();
		UtilsClient.disableCulling();
		door_out.draw();
		UtilsClient.enableCulling();
		indoor_open.draw();
	}
}
