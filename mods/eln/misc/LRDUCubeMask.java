package mods.eln.misc;

public class LRDUCubeMask {

	public LRDUMask[] lrduMaskArray = new LRDUMask[6];
	
	public LRDUCubeMask() {
		for(int idx = 0; idx<6; idx++) {
			lrduMaskArray[idx] = new LRDUMask();
		}
	}
	
	LRDUMask getSide(Direction direction) {
		return lrduMaskArray[direction.getInt()];
	}
	
	public void clear() {
		for(LRDUMask lrduMask : lrduMaskArray) {
			lrduMask.set(0);
		}
	}
	
	public void set(Direction direction, LRDU lrdu, boolean value) {
		get(direction).set(lrdu,value);
	}

	public boolean get(Direction direction, LRDU lrdu) {
		return get(direction).get(lrdu);
	}

	public LRDUMask get(Direction direction) {
		return lrduMaskArray[direction.getInt()];
	}

	public LRDUMask getTranslate(Direction side) {
		LRDUMask mask = new LRDUMask();
		
		for(LRDU lrdu : LRDU.values()) {
			Direction otherSide = side.applyLRDU(lrdu);
			LRDU otherLrdu = otherSide.getLRDUGoingTo(side);
			mask.set(lrdu, this.get(otherSide, otherLrdu));
		}
		return mask;
	}
}
