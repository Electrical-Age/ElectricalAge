package mods.eln.cable;


import mods.eln.compat.WorldCompat;
import mods.eln.cable.CableRenderType.CableRenderTypeMethodType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import mods.eln.gl.GL11;
import mods.eln.misc.UtilsClient;

public class CableRender {
    private CableRender() {
    }
    /*
	static final int connectionStandard = 0;	
	static final int connectionInternal = 1;
	static final int connectionWrappeHalf = 2;
	static final int connectionWrappeFull = 3;
	static final int connectionExtend = 5;
	*/

    public static CableRenderType connectionType(NodeBlockEntity entity, LRDUMask connectedSide, Direction side) {
        Block block;
        int x2, y2, z2;
        CableRenderType connectionTypeBuild = new CableRenderType();
        BlockEntity otherTileEntity;

        for (LRDU lrdu : LRDU.values()) {
            //noConnection
            if (!connectedSide.get(lrdu)) continue;

            Direction sideLrdu = side.applyLRDU(lrdu);

            x2 = entity.getBlockPos().getX();
            y2 = entity.getBlockPos().getY();
            z2 = entity.getBlockPos().getZ();

            switch (sideLrdu) {
                case XN:
                    x2--;
                    break;
                case XP:
                    x2++;
                    break;
                case YN:
                    y2--;
                    break;
                case YP:
                    y2++;
                    break;
                case ZN:
                    z2--;
                    break;
                case ZP:
                    z2++;
                    break;
                default:
                    break;
            }

            //standardConnection
            otherTileEntity = WorldCompat.getBlockEntity(entity.getLevel(), x2, y2, z2);
            if (otherTileEntity instanceof SixNodeEntity) {
                SixNodeEntity sixNodeEntity = (SixNodeEntity) otherTileEntity;
                if (sixNodeEntity.elementRenderList[side.getInt()] != null) {
                    Direction otherSide = side.applyLRDU(lrdu);
                    connectionTypeBuild.otherdry[lrdu.dir] = sixNodeEntity.getCableDry(otherSide, otherSide.getLRDUGoingTo(side));
                    connectionTypeBuild.otherRender[lrdu.dir] = sixNodeEntity.getCableRender(otherSide, otherSide.getLRDUGoingTo(side));
                    continue;
                }
            }

            //no wrappeConection ?
            if (!NodeBase.isBlockWrappable(entity.getLevel().getBlockState(new net.minecraft.core.BlockPos(x2, y2, z2)), entity.getLevel(), new net.minecraft.core.BlockPos(x2, y2, z2))) {
                continue;
            } else {
                switch (side) {
                    case XN:
                        x2--;
                        break;
                    case XP:
                        x2++;
                        break;
                    case YN:
                        y2--;
                        break;
                    case YP:
                        y2++;
                        break;
                    case ZN:
                        z2--;
                        break;
                    case ZP:
                        z2++;
                        break;
                    default:
                        break;
                }

                otherTileEntity = WorldCompat.getBlockEntity(entity.getLevel(), x2, y2, z2);

                if (otherTileEntity instanceof NodeBlockEntity) {
				/*
					Direction otherDirection = side.getInverse();
					LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
					CableRenderDescriptor render = entity.getCableRender(sideLrdu,sideLrdu.getLRDUGoingTo(side));
					//CableRenderDescriptor render = entity.getCableRender(side,lrdu);
					NodeBlockEntity otherNode =  ((NodeBlockEntity)otherTileEntity);
					CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);*/
				/*	Direction otherDirection = side.getInverse();
					LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
					CableRenderDescriptor render = entity.getCableRender(sideLrdu,sideLrdu.getLRDUGoingTo(side));
					NodeBlockEntity otherNode =  ((NodeBlockEntity)otherTileEntity);
					CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);
					*/
                    Direction otherDirection = side.getInverse();
                    LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
                    CableRenderDescriptor render = entity.getCableRender(sideLrdu, sideLrdu.getLRDUGoingTo(side));
                    NodeBlockEntity otherNode = (NodeBlockEntity) otherTileEntity;
                    CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);

                    if (render == null) {
                        //Utils.println("ASSERT cableRender missing");
                        continue;
                    }

                    if (otherRender == null) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.dir] = render.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //	=  += (connectionExtend + (render.heightPixel<<4))<<(lrdu.dir*8) ;
                        continue;
                    }
                    //	if(element.tileEntity.hashCode() > otherTileEntity.hashCode())
                    if (render.width == otherRender.width) {
                        if (sideLrdu.getInt() > otherDirection.getInt()) {
                            connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend;
                            connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel;
                            //connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        }
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU);
                        continue;
                    }
                    if (render.width < otherRender.width) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        continue;
                    }
                    continue;
                }
            }
        }
        return connectionTypeBuild;
    }

    public static CableRenderType connectionType(SixNodeElementRender element, Direction side) {
        Block block;
        int x2, y2, z2;
        CableRenderType connectionTypeBuild = new CableRenderType();
        BlockEntity otherTileEntity;

        for (LRDU lrdu : LRDU.values()) {
            //noConnection
            if (!element.connectedSide.get(lrdu)) continue;

            Direction sideLrdu = side.applyLRDU(lrdu);

            //InternalConnection
            if (element.tileEntity.elementRenderList[sideLrdu.getInt()] != null) {
                LRDU otherLRDU = sideLrdu.getLRDUGoingTo(side);
                CableRenderDescriptor render = element.getCableRender(lrdu);
                SixNodeElementRender otherElement = element.tileEntity.elementRenderList[sideLrdu.getInt()];
                CableRenderDescriptor otherRender = otherElement.getCableRender(otherLRDU);

                if (otherRender == null || render == null) {
                    continue;
                }

                if (render.width == otherRender.width) {
                    if (side.getInt() > sideLrdu.getInt()) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Internal;
                        connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel;
                        //connectionTypeBuild += (connectionInternal + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                    }
                    connectionTypeBuild.otherdry[lrdu.dir] = otherElement.getCableDry(otherLRDU);
                    connectionTypeBuild.otherRender[lrdu.dir] = otherElement.getCableRender(otherLRDU);
                    continue;
                }

                if (render.width < otherRender.width) {
                    connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Internal;
                    connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel;
                    connectionTypeBuild.otherdry[lrdu.dir] = otherElement.getCableDry(otherLRDU);
                    connectionTypeBuild.otherRender[lrdu.dir] = otherElement.getCableRender(otherLRDU);
                    //connectionTypeBuild += (connectionInternal + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                    continue;
                }
                connectionTypeBuild.otherdry[lrdu.dir] = otherElement.getCableDry(otherLRDU);
                connectionTypeBuild.otherRender[lrdu.dir] = otherElement.getCableRender(otherLRDU);
                continue;
            }

            x2 = element.tileEntity.getBlockPos().getX();
            y2 = element.tileEntity.getBlockPos().getY();
            z2 = element.tileEntity.getBlockPos().getZ();

            switch (sideLrdu) {
                case XN:
                    x2--;
                    break;
                case XP:
                    x2++;
                    break;
                case YN:
                    y2--;
                    break;
                case YP:
                    y2++;
                    break;
                case ZN:
                    z2--;
                    break;
                case ZP:
                    z2++;
                    break;
                default:
                    break;
            }

            //standardConnection
            otherTileEntity = WorldCompat.getBlockEntity(element.tileEntity.getLevel(), x2, y2, z2);
            if (otherTileEntity instanceof SixNodeEntity) {
                SixNodeEntity sixNodeEntity = (SixNodeEntity) otherTileEntity;
                if (sixNodeEntity.elementRenderList[side.getInt()] != null) {
                    connectionTypeBuild.otherdry[lrdu.dir] = sixNodeEntity.elementRenderList[side.getInt()].getCableDry(lrdu.inverse());
                    connectionTypeBuild.otherRender[lrdu.dir] = sixNodeEntity.elementRenderList[side.getInt()].getCableRender(lrdu.inverse());
                    continue;
                }
            }

            // 26.x: a floor cable ending at a device's terminal plate gets a lug (drawn by the cable, see drawKnuckle)
            if (side == Direction.YN && !sideLrdu.isY()) {
                connectionTypeBuild.knuckle[lrdu.dir] = CableTerminals.heightAt(otherTileEntity, sideLrdu.getInverse());
            }

            //no wrappeConection ?
            if (!NodeBase.isBlockWrappable(element.tileEntity.getLevel().getBlockState(new net.minecraft.core.BlockPos(x2, y2, z2)), element.tileEntity.getLevel(), new net.minecraft.core.BlockPos(x2, y2, z2))) {
                continue;
            } else {
                switch (side) {
                    case XN:
                        x2--;
                        break;
                    case XP:
                        x2++;
                        break;
                    case YN:
                        y2--;
                        break;
                    case YP:
                        y2++;
                        break;
                    case ZN:
                        z2--;
                        break;
                    case ZP:
                        z2++;
                        break;
                    default:
                        break;
                }

                otherTileEntity = WorldCompat.getBlockEntity(element.tileEntity.getLevel(), x2, y2, z2);

                if (otherTileEntity instanceof NodeBlockEntity) {
                    //Direction otherDirection = side.getInverse();
					/*Direction otherDirection = side.applyLRDU(lrdu).getInverse();
					LRDU otherLRDU = otherDirection.getLRDUGoingTo(side.getInverse());
					CableRenderDescriptor render = element.getCableRender(lrdu);
					NodeBlockEntity otherNode = ((NodeBlockEntity)otherTileEntity);
					CableRenderDescriptor otherRender = otherNode.getCableRender(side.getInverse(), lrdu//.inverse());
				*/
                    Direction otherDirection = side.getInverse();
                    LRDU otherLRDU = otherDirection.getLRDUGoingTo(sideLrdu).inverse();
                    CableRenderDescriptor render = element.getCableRender(lrdu);

                    if (render == null)
                        continue;
                    NodeBlockEntity otherNode = (NodeBlockEntity) otherTileEntity;
                    CableRenderDescriptor otherRender = otherNode.getCableRender(otherDirection, otherLRDU);

                    if (otherRender == null) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.dir] = render.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //connectionTypeBuild += (connectionExtend + (render.heightPixel<<4))<<(lrdu.dir*8) ;
                        continue;
                    }
                    //	if(element.tileEntity.hashCode() > otherTileEntity.hashCode())
                    if (render.width == otherRender.width) {
                        if (sideLrdu.getInt() > otherDirection.getInt()) {
                            connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend;
                            connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel;
                            ///connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        }
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU);
                        continue;
                    }
                    if (render.width < otherRender.width) {
                        connectionTypeBuild.method[lrdu.dir] = CableRenderTypeMethodType.Etend;
                        connectionTypeBuild.endAt[lrdu.dir] = otherRender.heightPixel;
                        connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU);
                        connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU);
                        //connectionTypeBuild += (connectionExtend + (otherRender.heightPixel<<4))<<(lrdu.dir*8);
                        continue;
                    }
                    connectionTypeBuild.otherdry[lrdu.dir] = otherNode.getCableDry(otherDirection, otherLRDU);
                    connectionTypeBuild.otherRender[lrdu.dir] = otherNode.getCableRender(otherDirection, otherLRDU);
                    continue;
                }
				
				/*
				if(otherTileEntity instanceof SixNodeEntity) {
				//	SixNodeEntity sixNodeEntity = (SixNodeEntity) otherTileEntity;
					//sixNodeEntity.elementRenderList[0].
					connectionTypeBuild += connectionWrappeHalf<<(lrdu.dir*8);
					continue;					
				}
				else {
					connectionTypeBuild += connectionWrappeFull<<(lrdu.dir*8);
					continue;
				}*/
            }
        }
        return connectionTypeBuild;
    }

    /**
     * 26.x: the conductor heights (px) a floor cable's arms meet, as {@link #connectionType(SixNodeElementRender, Direction)}
     * sets them in {@code knuckle}, without the rest (cheap enough to poll; see ElectricalCableRender.refresh).
     */
    public static float[] knuckleHeights(SixNodeElementRender element, Direction side) {
        float[] k = new float[4];
        if (side != Direction.YN || element.tileEntity == null || element.tileEntity.getLevel() == null) return k;
        for (LRDU lrdu : LRDU.values()) {
            if (!element.connectedSide.get(lrdu)) continue;
            Direction sideLrdu = side.applyLRDU(lrdu);
            if (sideLrdu.isY() || element.tileEntity.elementRenderList[sideLrdu.getInt()] != null) continue;
            BlockEntity other = element.tileEntity.getLevel().getBlockEntity(element.tileEntity.getBlockPos().relative(sideLrdu.toEnumFacing()));
            k[lrdu.dir] = CableTerminals.heightAt(other, sideLrdu.getInverse());
        }
        return k;
    }

    /** Copper of the lugs (the device models' copper texture, 196/110/70). */
    static final float LUG_R = 0.77f, LUG_G = 0.43f, LUG_B = 0.27f;

    /**
     * 26.x: cable lugs where a floor cable meets a device's terminal plate ({@code connectionType.knuckle}, px): the
     * cable turns up against the face (cable texture, takes the current dye colour), a copper crimp barrel tops it and
     * a copper palm lies on the plate's middle conductor. Sizes follow the cable (width, thickness); the height is the
     * device's. Local frame as drawCable: x = height above the face, Left/Right along z, Down/Up along y, edges at
     * +-0.5. Draw it after drawCable and before drawNode; it leaves the colour at copper.
     */
    public static void drawKnuckles(CableRenderDescriptor cable, CableRenderType connectionType) {
        if (cable == null) return;
        boolean any = false;
        for (float k : connectionType.knuckle) any |= k > 0;
        if (!any) return;
        final float px = 1f / 16f;
        float w2 = cable.widthDiv2, h = cable.height;
        UtilsClient.disableCulling();
        // pass 1: the risers, in the cable's texture and colour
        for (int idx = 0; idx < 4; idx++) {
            float H = connectionType.knuckle[idx] * px;
            if (H <= 0) continue;
            LRDU lrdu = LRDU.fromInt(idx);
            boolean alongZ = lrdu == LRDU.Left || lrdu == LRDU.Right;
            float edge = (lrdu == LRDU.Left || lrdu == LRDU.Down) ? -0.5f : 0.5f, in = -Math.signum(edge);
            lugBox(alongZ, true, 0, H - 0.1f * px, -w2 - 0.05f * px, w2 + 0.05f * px,
                edge + in * 0.25f * px, edge + in * (0.25f * px + h));
        }
        // pass 2: copper barrel and palm
        UtilsClient.disableTexture();
        GL11.glColor3f(LUG_R, LUG_G, LUG_B);
        for (int idx = 0; idx < 4; idx++) {
            float H = connectionType.knuckle[idx] * px;
            if (H <= 0) continue;
            LRDU lrdu = LRDU.fromInt(idx);
            boolean alongZ = lrdu == LRDU.Left || lrdu == LRDU.Right;
            float edge = (lrdu == LRDU.Left || lrdu == LRDU.Down) ? -0.5f : 0.5f, in = -Math.signum(edge);
            float bw = w2 + 0.15f * px, pw = Math.max(0.75f * px, w2 - 0.25f * px);
            lugBox(alongZ, false, H - 0.1f * px, H + 0.6f * px, -bw, bw, edge + in * 0.15f * px, edge + in * (0.35f * px + h));
            lugBox(alongZ, false, H - 0.85f * px, H + 0.85f * px, -pw, pw, edge, edge + in * 0.25f * px);
        }
        UtilsClient.enableTexture();
        UtilsClient.enableCulling();
    }

    /**
     * A box for drawKnuckles: x (height) x0..x1, across a0..a1, along the arm b0..b1 (either order). Textured: planar
     * UVs on the cable sprite's body half, 16 px per block like drawCable.
     */
    static void lugBox(boolean alongZ, boolean textured, float x0, float x1, float a0, float a1, float b0, float b1) {
        float[] xs = {Math.min(x0, x1), Math.max(x0, x1)}, as = {Math.min(a0, a1), Math.max(a0, a1)},
            bs = {Math.min(b0, b1), Math.max(b0, b1)};
        GL11.glBegin(GL11.GL_QUADS);
        // faces normal to x, a, b; k = 0 the min side, 1 the max side
        for (int axis = 0; axis < 3; axis++) {
            for (int k = 0; k < 2; k++) {
                float sgn = k == 0 ? -1 : 1;
                float nx = axis == 0 ? sgn : 0, na = axis == 1 ? sgn : 0, nb = axis == 2 ? sgn : 0;
                if (alongZ) GL11.glNormal3f(nx, na, nb);
                else GL11.glNormal3f(nx, nb, na);
                for (int c = 0; c < 4; c++) {
                    // the four corners in the face's plane: (p, q) = (0,0) (1,0) (1,1) (0,1)
                    int p = (c == 1 || c == 2) ? 1 : 0, q = (c >= 2) ? 1 : 0;
                    float x, a, b, u, v;
                    if (axis == 0) { x = xs[k]; a = as[p]; b = bs[q]; u = a; v = b; }
                    else if (axis == 1) { a = as[k]; x = xs[p]; b = bs[q]; u = b; v = x; }
                    else { b = bs[k]; a = as[p]; x = xs[q]; u = a; v = x; }
                    if (textured) GL11.glTexCoord2f(0.25f + u * 0.5f, 0.5f + v);
                    if (alongZ) GL11.glVertex3f(x, a, b);
                    else GL11.glVertex3f(x, b, a);
                }
            }
        }
        GL11.glEnd();
    }

    public static void drawCable(CableRenderDescriptor cable, LRDUMask connection, CableRenderType connectionType) {
        drawCable(cable, connection, connectionType, cable.widthDiv2 / 2f);
    }

    public static void drawCable(CableRenderDescriptor cable, LRDUMask connection, CableRenderType connectionType, float deltaStart) {
        if (cable == null) return;
        //GL11.glDisable(GL11.GL_TEXTURE);
        //if(connection.mask != 0) return;
        float tx, ty;
        {
            float endLeft = -deltaStart, endRight = deltaStart, endUp = deltaStart, endDown = -deltaStart;
            float startdLeft = -connectionType.startAt[0], startRight = connectionType.startAt[1], startUp = connectionType.startAt[2], startDown = -connectionType.startAt[3];

            if (connection.mask == 0 & deltaStart >= 0f) {
                endLeft = -cable.widthDiv2 - 3.0f / 16.0f;
                endRight = cable.widthDiv2 + 3.0f / 16.0f;
                endDown = -cable.widthDiv2 - 3.0f / 16.0f;
                endUp = cable.widthDiv2 + 3.0f / 16.0f;
            } else {
                if (connection.get(LRDU.Left)) {
                    endLeft = -0.5f;
                }
                if (connection.get(LRDU.Right)) {
                    endRight = 0.5f;
                }
                if (connection.get(LRDU.Down)) {
                    endDown = -0.5f;
                }
                if (connection.get(LRDU.Up)) {
                    endUp = 0.5f;
                }
            }

            switch (connectionType.method[0]) {
                case Internal:
                    endLeft += (connectionType.endAt[0]) / 16.0;
                    break;
                case Etend:
                    endLeft -= (connectionType.endAt[0]) / 16.0;
                    break;
                default:
                    break;
            }

            switch (connectionType.method[1]) {
                case Internal:
                    endRight -= (connectionType.endAt[1]) / 16.0;
                    break;
                case Etend:
                    endRight += (connectionType.endAt[1]) / 16.0;
                    break;
                default:
                    break;
            }

            switch (connectionType.method[2]) {
                case Internal:
                    endDown += (connectionType.endAt[2]) / 16.0;
                    break;
                case Etend:
                    endDown -= (connectionType.endAt[2]) / 16.0;
                    break;
                default:
                    break;
            }

            switch (connectionType.method[3]) {
                case Internal:
                    endUp -= (connectionType.endAt[3]) / 16.0;
                    break;
                case Etend:
                    endUp += (connectionType.endAt[3]) / 16.0;
                    break;
                default:
                    break;
            }

            float height = cable.height;
            tx = 0.25f;
            ty = 0.5f;

            //	Utils.bindTextureByName(cable.cableTexture);

            if (endLeft < startdLeft) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                GL11.glNormal3f(0f, 1f, 0f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endLeft);
                GL11.glVertex3f(0, cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startdLeft);
                GL11.glVertex3f(0, cable.widthDiv2, startdLeft);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endLeft);
                GL11.glVertex3f(height, cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startdLeft);
                GL11.glVertex3f(height, cable.widthDiv2, startdLeft);
                GL11.glNormal3f(1f, 0f, 0f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endLeft);
                GL11.glVertex3f(height, -cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startdLeft);
                GL11.glVertex3f(height, -cable.widthDiv2, startdLeft);
                GL11.glNormal3f(0f, -1f, 0f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + endLeft);
                GL11.glVertex3f(0, -cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + startdLeft);
                GL11.glVertex3f(0, -cable.widthDiv2, startdLeft);
                GL11.glEnd();


                GL11.glBegin(GL11.GL_QUADS);
                GL11.glNormal3f(0f, 0f, -1f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endLeft - height);
                GL11.glVertex3f(0, -cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endLeft - height);
                GL11.glVertex3f(0, cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endLeft);
                GL11.glVertex3f(height, cable.widthDiv2, endLeft);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endLeft);
                GL11.glVertex3f(height, -cable.widthDiv2, endLeft);
                GL11.glEnd();
            }

            if (endRight > startRight) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                GL11.glNormal3f(0f, 1f, 0f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startRight);
                GL11.glVertex3f(0, cable.widthDiv2, startRight);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endRight);
                GL11.glVertex3f(0, cable.widthDiv2, endRight);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startRight);
                GL11.glVertex3f(height, cable.widthDiv2, startRight);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endRight);
                GL11.glVertex3f(height, cable.widthDiv2, endRight);
                GL11.glNormal3f(1f, 0f, 0f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startRight);
                GL11.glVertex3f(height, -cable.widthDiv2, startRight);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endRight);
                GL11.glVertex3f(height, -cable.widthDiv2, endRight);
                GL11.glNormal3f(0f, -1f, 0f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + startRight);
                GL11.glVertex3f(0, -cable.widthDiv2, startRight);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f - height, ty + endRight);
                GL11.glVertex3f(0, -cable.widthDiv2, endRight);
                GL11.glEnd();

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glNormal3f(0f, 0f, 1f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endRight);
                GL11.glVertex3f(height, -cable.widthDiv2, endRight);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endRight);
                GL11.glVertex3f(height, cable.widthDiv2, endRight);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endRight + height);
                GL11.glVertex3f(0, cable.widthDiv2, endRight);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endRight + height);
                GL11.glVertex3f(0, -cable.widthDiv2, endRight);
                GL11.glEnd();
            }
            if (endDown < startDown) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                GL11.glNormal3f(0f, 0f, -1f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endDown);
                GL11.glVertex3f(0, endDown, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startDown);
                GL11.glVertex3f(0, startDown, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endDown);
                GL11.glVertex3f(height, endDown, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startDown);
                GL11.glVertex3f(height, startDown, -cable.widthDiv2);
                GL11.glNormal3f(1f, 0f, 0f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endDown);
                GL11.glVertex3f(height, endDown, cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startDown);
                GL11.glVertex3f(height, startDown, cable.widthDiv2);
                GL11.glNormal3f(0f, 0f, 1f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endDown);
                GL11.glVertex3f(0, endDown, cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startDown);
                GL11.glVertex3f(0, startDown, cable.widthDiv2);
                GL11.glEnd();

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glNormal3f(0f, -1f, 0f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endDown);
                GL11.glVertex3f(height, endDown, -cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endDown);
                GL11.glVertex3f(height, endDown, cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endDown - height);
                GL11.glVertex3f(0, endDown, cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endDown - height);
                GL11.glVertex3f(0, endDown, -cable.widthDiv2);
                GL11.glEnd();
            }
            if (endUp > startUp) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                GL11.glNormal3f(0f, 0f, -1f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + startUp);
                GL11.glVertex3f(0, startUp, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2 - height) * 0.5f, ty + endUp);
                GL11.glVertex3f(0, endUp, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + startUp);
                GL11.glVertex3f(height, startUp, -cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endUp);
                GL11.glVertex3f(height, endUp, -cable.widthDiv2);
                GL11.glNormal3f(1f, 0f, 0f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + startUp);
                GL11.glVertex3f(height, startUp, cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endUp);
                GL11.glVertex3f(height, endUp, cable.widthDiv2);
                GL11.glNormal3f(0f, 0f, 1f);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + startUp);
                GL11.glVertex3f(0, startUp, cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2 + height) * 0.5f, ty + endUp);
                GL11.glVertex3f(0, endUp, cable.widthDiv2);
                GL11.glEnd();

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glNormal3f(0f, 1f, 0f);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endUp + height);
                GL11.glVertex3f(0, endUp, -cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endUp + height);
                GL11.glVertex3f(0, endUp, cable.widthDiv2);
                GL11.glTexCoord2f(tx + (cable.widthDiv2) * 0.5f, ty + endUp);
                GL11.glVertex3f(height, endUp, cable.widthDiv2);
                GL11.glTexCoord2f(tx - (cable.widthDiv2) * 0.5f, ty + endUp);
                GL11.glVertex3f(height, endUp, -cable.widthDiv2);
                GL11.glEnd();
            }
        }
    }

    public static void drawNode(CableRenderDescriptor cable, LRDUMask connection, CableRenderType connectionType) {
        if ((connection.mask == 0 || ((connection.get(LRDU.Left) || connection.get(LRDU.Right)) && (connection.get(LRDU.Down) || connection.get(LRDU.Up))) || connection.mask == 1 || connection.mask == 2 || connection.mask == 4 || connection.mask == 8)) {
            float widthDiv2 = cable.widthDiv2 + 1.0f / 16.0f;
            float height = cable.height + 1.0f / 16.0f;
            float tx = 0.75f;
            float ty = 0.5f;
            GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
            //	Utils.bindTextureByName(ClientProxy.CABLENODE_PNG);
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            GL11.glNormal3f(0f, 1f, 0f);
            GL11.glTexCoord2f(tx + (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(0, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(0, widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx + (widthDiv2) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + (widthDiv2) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, widthDiv2, widthDiv2);
            GL11.glNormal3f(1f, 0f, 0f);
            GL11.glTexCoord2f(tx - (widthDiv2) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx - (widthDiv2) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, widthDiv2);
            GL11.glNormal3f(0f, -1f, 0f);
            GL11.glTexCoord2f(tx - (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(0, -widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx - (widthDiv2 + cable.height + 1.0f / 16.0f) * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(0, -widthDiv2, widthDiv2);
            GL11.glEnd();

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glNormal3f(0f, 0f, -1f);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty - widthDiv2 - cable.height - 1.0f / 16.0f);
            GL11.glVertex3f(0, -widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty - widthDiv2 - cable.height - 1.0f / 16.0f);
            GL11.glVertex3f(0, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, widthDiv2, -widthDiv2);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty - widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, -widthDiv2);

            GL11.glNormal3f(0f, 0f, 1f);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, -widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty + widthDiv2);
            GL11.glVertex3f(height, widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx + widthDiv2 * 0.5f, ty + widthDiv2 + cable.height + 1.0f / 16.0f);
            GL11.glVertex3f(0, widthDiv2, widthDiv2);
            GL11.glTexCoord2f(tx - widthDiv2 * 0.5f, ty + widthDiv2 + cable.height + 1.0f / 16.0f);
            GL11.glVertex3f(0, -widthDiv2, widthDiv2);
            GL11.glEnd();
        }
        //	GL11.glEnable(GL11.GL_TEXTURE);
    }
}
