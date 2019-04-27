package mods.eln.transparentnode.transformer;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.LoopedSound;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class TransformerRender extends TransparentNodeElementRender {
    private final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(4, 64, this);
    private final TransformerDescriptor descriptor;

    private SlewLimiter load = new SlewLimiter(0.5f);

    public TransformerRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (TransformerDescriptor) descriptor;
        addLoopedSound(new LoopedSound("eln:Transformer", coordinate(), ISound.AttenuationType.LINEAR) {
            @Override
            public float getVolume() {
                if (load.getPosition() > TransformerRender.this.descriptor.minimalLoadToHum)
                    return 0.1f * (load.getPosition() - TransformerRender.this.descriptor.minimalLoadToHum) /
                        (1 - TransformerRender.this.descriptor.minimalLoadToHum);
                else
                    return 0f;
            }
        });

        coordinate = new Coordinate(tileEntity);
        doorOpen = new PhysicalInterpolator(0.4f, 4.0f, 0.9f, 0.05f);
    }

    @Override
    public void draw() {
        GL11.glPushMatrix();
        front.glRotateXnRef();
        descriptor.draw(feroPart, primaryStackSize, secondaryStackSize, hasCasing, doorOpen.get());
        GL11.glPopMatrix();
        cableRenderType = drawCable(front.down(), priRender, priConn, cableRenderType);
        cableRenderType = drawCable(front.down(), secRender, secConn, cableRenderType);
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new TransformerGuiDraw(player, inventory, this);
    }

    private byte primaryStackSize;
    private byte secondaryStackSize;
    private CableRenderDescriptor priRender;
    private CableRenderDescriptor secRender;
    public boolean isIsolator;

    private Obj3DPart feroPart;
    private boolean hasCasing = false;

    private final Coordinate coordinate;
    private final PhysicalInterpolator doorOpen;

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            primaryStackSize = stream.readByte();
            secondaryStackSize = stream.readByte();
            ItemStack feroStack = Utils.unserialiseItemStack(stream);
            FerromagneticCoreDescriptor feroDesc = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(feroStack, FerromagneticCoreDescriptor.class);
            if (feroDesc == null)
                feroPart = null;
            else
                feroPart = feroDesc.feroPart;

            ItemStack priStack = Utils.unserialiseItemStack(stream);
            ElectricalCableDescriptor priDesc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(priStack, ElectricalCableDescriptor.class);
            if (priDesc == null)
                priRender = null;
            else
                priRender = priDesc.render;

            ItemStack secStack = Utils.unserialiseItemStack(stream);
            ElectricalCableDescriptor secDesc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(secStack, ElectricalCableDescriptor.class);
            if (secDesc == null)
                secRender = null;
            else
                secRender = secDesc.render;

            eConn.deserialize(stream);

            priConn.mask = 0;
            secConn.mask = 0;
            for (LRDU lrdu : LRDU.values()) {
                if (!eConn.get(lrdu) || front.down().applyLRDU(lrdu) == front.left() || front.down().applyLRDU(lrdu) == front.right())
                    continue;
                CableRenderDescriptor render = getCableRender(front.down().applyLRDU(lrdu), LRDU.Down);

                if (render == priRender) priConn.set(lrdu, true);
                if (render == secRender) secConn.set(lrdu, true);

            }
            cableRenderType = null;
            isIsolator = stream.readBoolean();

            load.setTarget(stream.readFloat());
            hasCasing = stream.readBoolean();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final LRDUMask priConn = new LRDUMask();
    private final LRDUMask secConn = new LRDUMask();
    private final LRDUMask eConn = new LRDUMask();
    private CableRenderType cableRenderType;

    @Override
    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        if (lrdu == LRDU.Down) {
            if (side == front.left()) return priRender;
            if (side == front.right()) return secRender;
            if (side == front && !grounded) return priRender;
            if (side == front.back() && !grounded) return secRender;
        }
        return null;
    }

    @Override
    public void notifyNeighborSpawn() {
        super.notifyNeighborSpawn();
        cableRenderType = null;
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        load.step(deltaT);

        if (hasCasing) {
            if (!Utils.isPlayerAround(tileEntity.getWorld(), coordinate.moved(front).getAxisAlignedBB(0)))
                doorOpen.setTarget(0f);
            else
                doorOpen.setTarget(1f);
            doorOpen.step(deltaT);
        }
    }
}
