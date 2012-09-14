package hu.bute.daai.amorg.drtorrent.test;

import hu.bute.daai.amorg.drtorrent.Tools;
import junit.framework.Assert;

import org.junit.Test;

public class IntByteArrayConverterTestCase {

	@Test
	public void test0() {
		byte[] array = {
				12, 23, 34, 45
		};
		Assert.assertEquals(Tools.readInt32(array, 0), Tools.byteArrayToInt32(array));
	}
	
	@Test
	public void test1() {
		Assert.assertEquals(65000, Tools.byteArrayToInt32(Tools.int32ToByteArray(65000)));
		Assert.assertEquals(-1, Tools.readInt32(Tools.int32ToByteArray(-1), 0));
	}
	
	@Test
	public void test2() {
		byte[] array = {
				0, 23, -128, -1
		};
		Assert.assertEquals(Tools.readInt32(array, 0), Tools.byteArrayToInt32(array));
	}


}
