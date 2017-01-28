package mods.eln.sixnode.modbusrtu;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;

public class ModbusRtuRender extends SixNodeElementRender {

    Coordonate coord;
    PhysicalInterpolator interpolator;
    float modbusActivityTimeout = 0;
    float modbusErrorTimeout = 0;

    ModbusRtuDescriptor descriptor;

    HashMap<Integer, WirelessTxStatus> wirelessTxStatusList = new HashMap<Integer, WirelessTxStatus>();
    HashMap<Integer, WirelessRxStatus> wirelessRxStatusList = new HashMap<Integer, WirelessRxStatus>();

    int station = -1;
    String name;
    boolean boot = true;

    boolean rxTxChange = false;

    public ModbusRtuRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ModbusRtuDescriptor) descriptor;

        interpolator = new PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f);
        coord = new Coordonate(tileEntity);
    }

    @Override
    public void draw() {
        super.draw();

        if (side.isY()) {
            front.inverse().glRotateOnX();
        } else {
            LRDU.Down.glRotateOnX();
        }

        descriptor.draw(interpolator.get(), modbusActivityTimeout > 0, modbusErrorTimeout > 0);
    }

    @Override
    public void refresh(float deltaT) {
        if (!Utils.isPlayerAround(tileEntity.getWorldObj(), coord.getAxisAlignedBB(0)))
            interpolator.setTarget(0f);
        else
            interpolator.setTarget(1f);

        interpolator.step(deltaT);

        if (modbusActivityTimeout > 0)
            modbusActivityTimeout -= deltaT;

        if (modbusErrorTimeout > 0)
            modbusErrorTimeout -= deltaT;
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);

        try {
            station = stream.readInt();
            name = stream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (boot)
            clientSend(ModbusRtuElement.serverAllSyncronise);
        boot = false;
    }

    @Override
    public void serverPacketUnserialize(DataInputStream stream) throws IOException {
        super.serverPacketUnserialize(stream);

        switch (stream.readByte()) {
            case ModbusRtuElement.clientAllSyncronise: {
                wirelessTxStatusList.clear();
                for (int idx = stream.readInt(); idx > 0; idx--) {
                    WirelessTxStatus tx = new WirelessTxStatus();
                    tx.readFrom(stream);
                    wirelessTxStatusList.put(tx.uuid, tx);
                }
                wirelessRxStatusList.clear();
                for (int idx = stream.readInt(); idx > 0; idx--) {
                    WirelessRxStatus rx = new WirelessRxStatus();
                    rx.readFrom(stream);
                    wirelessRxStatusList.put(rx.uuid, rx);
                }

                rxTxChange = true;
            }
            break;
            case ModbusRtuElement.clientTx1Syncronise: {
                WirelessTxStatus newTx = new WirelessTxStatus();
                newTx.readFrom(stream);
                wirelessTxStatusList.put(newTx.uuid, newTx);
                rxTxChange = true;
            }
            break;
            case ModbusRtuElement.clientRx1Syncronise: {
                WirelessRxStatus newRx = new WirelessRxStatus();
                newRx.readFrom(stream);
                wirelessRxStatusList.put(newRx.uuid, newRx);
                rxTxChange = true;
            }
            break;
            case ModbusRtuElement.clientTxDelete: {
                wirelessTxStatusList.remove(stream.readInt());
                rxTxChange = true;
            }
            break;
            case ModbusRtuElement.clientRxDelete: {
                wirelessRxStatusList.remove(stream.readInt());
                rxTxChange = true;
            }
            break;
            case ModbusRtuElement.clientRx1Connected:
                WirelessRxStatus rx = wirelessRxStatusList.get(stream.readInt());
                if (rx != null) {
                    rx.connected = stream.readBoolean();
                }
                break;
            case ModbusRtuElement.ClientModbusActivityEvent:
                modbusActivityTimeout = 0.05f;
                break;
            case ModbusRtuElement.ClientModbusErrorEvent:
                modbusErrorTimeout = 1f;
                break;
        }
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new ModbusRtuGui(player, this);
    }

    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return Eln.instance.signalCableDescriptor.render;
    }
}
