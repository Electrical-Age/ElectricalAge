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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import mods.eln.gl.GL11;

import java.io.DataInputStream;
import java.io.IOException;

public class TransformerRender extends TransparentNodeElementRender {
    private final TransformerDescriptor descriptor;

    private SlewLimiter load = new SlewLimiter(0.5f);

    public TransformerRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (TransformerDescriptor) descriptor;
        addLoopedSound(new mods.eln.sound.LoopedSound("eln:transformer", coordonate(), mods.eln.sound.LoopedSound.Attenuation.LINEAR) {
            @Override
            public float getVolume() {
                if (load.getPosition() > TransformerRender.this.descriptor.minimalLoadToHum)
                    return 0.1f * (load.getPosition() - TransformerRender.this.descriptor.minimalLoadToHum) /
                        (1 - TransformerRender.this.descriptor.minimalLoadToHum);
                else
                    return 0f;
            }
        });

        coordinate = new Coordonate(tileEntity);
        doorOpen = new PhysicalInterpolator(0.4f, 4.0f, 0.9f, 0.05f);
    }

    @Override
    public void draw() {
        GL11.glPushMatrix();
        front.glRotateXnRef();
        if (window > 0) {
            double total = primaryTurns * mods.eln.node.voxel.CableSpec.MM2[primaryMm2] + secondaryTurns * mods.eln.node.voxel.CableSpec.MM2[secondaryMm2];
            descriptor.drawCoils(feroPart, Coil.of(primaryTurns, mods.eln.node.voxel.CableSpec.MM2[primaryMm2], window, total), rgb(primaryColour),
                Coil.of(secondaryTurns, mods.eln.node.voxel.CableSpec.MM2[secondaryMm2], window, total), rgb(secondaryColour), hasCasing, doorOpen.get());
        } else descriptor.draw(feroPart, primaryStackSize, secondaryStackSize, hasCasing, doorOpen.get());
        GL11.glPopMatrix();
        cableRenderType = drawCable(front.down(), priRender, priConn, cableRenderType);
        cableRenderType = drawCable(front.down(), secRender, secConn, cableRenderType);
    }

    /** The windings as published: turns, wire (CableSpec.MM2 index), insulation colour (DyeColor id), tier; the core's window. */
    int primaryTurns, secondaryTurns, primaryMm2, secondaryMm2, primaryColour, secondaryColour, primaryTier, secondaryTier;
    float window;

    static int rgb(int dye) {
        return net.minecraft.world.item.DyeColor.byId(dye).getTextureDiffuseColor() & 0xFFFFFF;
    }

    /** A winding's cable look (for the cables joining it): its tier's cable. */
    static CableRenderDescriptor tierRender(int tier) {
        ElectricalCableDescriptor d = switch (tier) {
            case 1 -> mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor;
            case 2 -> mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor;
            case 3 -> mods.eln.registry.ElnDeviceRegistry.highVoltageCableDescriptor;
            case 4 -> mods.eln.registry.ElnDeviceRegistry.veryHighVoltageCableDescriptor;
            default -> null;
        };
        return d == null ? null : d.render;
    }

    private byte primaryStackSize;
    private byte secondaryStackSize;
    private CableRenderDescriptor priRender;
    private CableRenderDescriptor secRender;
    public boolean isIsolator;

    private Obj3DPart feroPart;
    private boolean hasCasing = false;

    private final Coordonate coordinate;
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
            ElectricalCableDescriptor priDesc = mods.eln.node.voxel.CableItems.descriptor(priStack);
            if (priDesc == null)
                priRender = null;
            else
                priRender = priDesc.render;

            ItemStack secStack = Utils.unserialiseItemStack(stream);
            ElectricalCableDescriptor secDesc = mods.eln.node.voxel.CableItems.descriptor(secStack);
            if (secDesc == null)
                secRender = null;
            else
                secRender = secDesc.render;

            eConn.deserialize(stream);

            cableRenderType = null;
            isIsolator = stream.readBoolean();

            load.setTarget(stream.readFloat());
            hasCasing = stream.readBoolean();
            primaryTurns = stream.readShort();
            primaryMm2 = Math.max(0, Math.min(mods.eln.node.voxel.CableSpec.MM2.length - 1, stream.readByte()));
            primaryColour = stream.readByte() & 15;
            primaryTier = stream.readByte();
            secondaryTurns = stream.readShort();
            secondaryMm2 = Math.max(0, Math.min(mods.eln.node.voxel.CableSpec.MM2.length - 1, stream.readByte()));
            secondaryColour = stream.readByte() & 15;
            secondaryTier = stream.readByte();
            window = stream.readFloat();
            // (a winding isn't a cable item: its cables join with its tier's look)
            if (priRender == null && primaryTurns > 0) priRender = tierRender(primaryTier);
            if (secRender == null && secondaryTurns > 0) secRender = tierRender(secondaryTier);
            priConn.mask = 0;
            secConn.mask = 0;
            for (LRDU lrdu : LRDU.values()) {
                if (!eConn.get(lrdu) || front.down().applyLRDU(lrdu) == front.left() || front.down().applyLRDU(lrdu) == front.right())
                    continue;
                CableRenderDescriptor render = getCableRender(front.down().applyLRDU(lrdu), LRDU.Down);
                if (render == priRender) priConn.set(lrdu, true);
                if (render == secRender) secConn.set(lrdu, true);
            }

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
            if (!Utils.isPlayerAround(tileEntity.getLevel(), coordinate.moved(front).getAxisAlignedBB(0)))
                doorOpen.setTarget(0f);
            else
                doorOpen.setTarget(1f);
            doorOpen.step(deltaT);
        }
    }
}
