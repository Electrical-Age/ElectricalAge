package mods.eln.cable;

public class CableRenderType {
	enum CableRenderTypeMethodType {Standard, Internal, WrapperHalf, WrapperFull, Etend}
	
	public CableRenderTypeMethodType[] method = new CableRenderTypeMethodType[4];
    public float[] endAt = new float[4];
    public float[] startAt = new float[4];
	public int[] otherdry = new int[4];
	public CableRenderDescriptor[] otherRender = new CableRenderDescriptor[4];
	
	public CableRenderType() {
		for(int idx = 0; idx < 4; idx++) {
			method[idx] = CableRenderTypeMethodType.Standard;
            endAt[idx] = 0;
            startAt[idx] = 0;
			otherdry[idx] = 0;
			otherRender[idx] = null;
		}
	}
}
