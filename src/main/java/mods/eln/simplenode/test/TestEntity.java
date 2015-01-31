package mods.eln.simplenode.test;

import mods.eln.node.simple.SimpleNodeEntity;

public class TestEntity extends SimpleNodeEntity {

	@Override
	public String getNodeUuid() {
		// TODO Auto-generated method stub
		return TestNode.getNodeUuidStatic();
	}
}
