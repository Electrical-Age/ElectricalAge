package mods.eln.robotics.computer;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.robotics.computer.screen.Form;
import mods.eln.robotics.computer.screen.Scene;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The computer (ComputerDescriptor): its case, monitor and lamps, this client's copy of its screen (Scene), and the
 * monitor's picture of it (ScreenTexture), dark while it is off.
 */
public class ComputerRender extends TransparentNodeElementRender {
    final ComputerDescriptor desc;
    public byte status;
    public int address;
    public boolean onBus, switchedOn = true;
    /** The screen as the server sent it; the screen class draws it and keeps what is only this player's (edits). */
    public final Scene scene = new Scene();
    /** The screen's own state that outlives one opening of it (each line's half-typed text and history). */
    public final java.util.Map<String, Object> screenState = new java.util.HashMap<>();
    /** Replies to editors' saves, for the screen to take. */
    public final java.util.ArrayDeque<java.util.List<Object>> replies = new java.util.ArrayDeque<>();
    /** Bumped when the scene changes, so the screen can see it. */
    public int sceneVersion;

    public ComputerRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (ComputerDescriptor) d;
        addLoopedSound(new mods.eln.sound.LoopedSound("eln:floppy_spin", coordonate()) {
            @Override
            public float getVolume() {
                return SPIN_VOLUME * Math.max(0, Math.min(1, spin.get()));
            }

            @Override
            public float getPitch() {   // spins up and down
                return 0.6f + 0.4f * Math.max(0, Math.min(1, spin.get()));
            }
        });
    }

    /** A drive's motor (ComputerElement.spinning), smoothed: the whir rises and falls in about a third of a second. */
    final mods.eln.misc.RcInterpolator spin = new mods.eln.misc.RcInterpolator(0.3f);
    static final float SPIN_VOLUME = 0.18f;

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        spin.step(deltaT);
    }

    ScreenTexture picture;
    boolean noPicture;

    /** Is it on (anything to show)? Off, or without chips, the screen and the monitor show nothing (Baughn 2026-11-18). */
    public boolean lit() {
        return switchedOn && (status == ComputerElement.RUNNING || status == ComputerElement.SLEEPING || status == ComputerElement.HALTED);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw(status);
        if (!lit()) return;
        if (picture == null && !noPicture) noPicture = (picture = ScreenTexture.make()) == null;
        if (picture == null) return;
        // (the picture keeps the screen's shape, 6 x 10 px cells, with black bars on the glass: Baughn)
        float aspect = scene.w * ComputerScreen.CELL / (float) (scene.h * ComputerScreen.LINE);
        desc.drawPicture(picture.update(sceneVersion, () -> mods.eln.robotics.computer.screen.SceneCells.of(scene)), aspect);
    }

    /** Each pixmap's texture on this client, made when first drawn and again when it changes (screen and monitor alike). */
    final java.util.Map<Integer, Object[]> pixmapTextures = new java.util.HashMap<>();
    static int pixmapNext;

    /** The texture of a pixmap (nearest filtering: dots stay square), or null where there is no GPU. */
    public net.minecraft.resources.Identifier pixmapTexture(mods.eln.robotics.computer.screen.Scene.Pixmap p) {
        int key = System.identityHashCode(p);
        Object[] have = pixmapTextures.get(key);
        if (have != null && have[1] == p) return (net.minecraft.resources.Identifier) have[0];
        try {
            if (pixmapTextures.size() > 64) releasePixmaps();
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("eln", "dynamic/computer_pixmap_" + pixmapNext++);
            var tex = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "eln computer pixmap", p.w(), p.h(), true);
            var img = tex.getPixels();
            for (int y = 0; y < p.h(); y++)
                for (int x = 0; x < p.w(); x++) img.setPixel(x, y, p.argb()[y * p.w() + x]);
            tex.upload();
            net.minecraft.client.Minecraft.getInstance().getTextureManager().register(id, tex);
            pixmapTextures.put(key, new Object[]{id, p});
            return id;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    void releasePixmaps() {
        for (Object[] t : pixmapTextures.values())
            net.minecraft.client.Minecraft.getInstance().getTextureManager().release((net.minecraft.resources.Identifier) t[0]);
        pixmapTextures.clear();
    }

    @Override
    public void destructor() {
        super.destructor();
        if (picture != null) picture.close();
        releasePixmaps();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            status = stream.readByte();
            address = stream.readByte() & 0xFF;
            onBus = stream.readBoolean();
            switchedOn = stream.readBoolean();
            spin.setTarget(stream.readByte() != 0 ? 1 : 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Its state in words (the screen). */
    public String stateTextClient() {
        return switch (status) {
            case ComputerElement.RUNNING -> mods.eln.i18n.I18N.tr("Running");
            case ComputerElement.SLEEPING -> mods.eln.i18n.I18N.tr("Waiting");
            case ComputerElement.HALTED -> mods.eln.i18n.I18N.tr("Halted");
            case ComputerElement.NO_CHIPS -> mods.eln.i18n.I18N.tr("No chips");
            default -> mods.eln.i18n.I18N.tr("Off");
        };
    }

    /** Send a screen protocol event (one message) to the computer. */
    public void screenEvent(Object... parts) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
            new ScreenEventPayload(tileEntity.getBlockPos(), Form.print(parts)));
    }

    /** Protocol lines from the server, applied to this copy of the scene. */
    @Override
    public void serverPacketUnserialize(DataInputStream stream) {
        try {
            byte kind = stream.readByte();
            if (kind != ComputerElement.SCREEN_ID) return;
            int n = stream.readInt();
            if (n < 0 || n > 1 << 20) return;
            byte[] b = new byte[n];
            stream.readFully(b);
            for (String line : new String(b, StandardCharsets.UTF_8).split("\n")) {
                var m = Form.parse(line);
                if (m == null || !scene.apply(m)) continue;
                String v = Form.verb(m);
                if (v.equals("saved") || v.equals("conflict")) {   // (for the screen's editors)
                    replies.add(m);
                    while (replies.size() > 16) replies.poll();
                }
            }
            sceneVersion++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
