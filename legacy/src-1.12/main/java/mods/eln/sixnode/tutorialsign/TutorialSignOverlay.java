package mods.eln.sixnode.tutorialsign;


import mods.eln.compat.WorldCompat;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

public class TutorialSignOverlay {

    TutorialSignRender oldRender = null;

    public TutorialSignOverlay() {
        int i = 0;
        i++;
    }

    // 26.x port: render() removed (client rendering; redone in P3)
}
