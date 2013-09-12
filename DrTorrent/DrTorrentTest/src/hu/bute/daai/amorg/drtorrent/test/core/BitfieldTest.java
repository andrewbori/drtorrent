package hu.bute.daai.amorg.drtorrent.test.core;

import hu.bute.daai.amorg.drtorrent.core.Bitfield;
import junit.framework.Assert;

import org.junit.Test;

public class BitfieldTest {
	
	@Test
	public void testConstructorSetAllBitsFalse() {
		Bitfield bitfield = new Bitfield(24, false);
		Assert.assertEquals(3, bitfield.getLengthInBytes());
		Assert.assertEquals(24, bitfield.getLengthInBits());
		byte[] data = bitfield.data();
		Assert.assertNotNull(data);
		
		for (int i = 0; i < data.length; i++) {
			Assert.assertEquals((byte) 0, data[i]);
		}
	}
	
	@Test
	public void testConstructorSetAllBitsTrue() {
		Bitfield bitfield = new Bitfield(30, true);
		Assert.assertEquals(4, bitfield.getLengthInBytes());
		Assert.assertEquals(30, bitfield.getLengthInBits());
		byte[] data = bitfield.data();
		Assert.assertNotNull(data);
		
		for (int i = 0; i < data.length; i++) {
			Assert.assertEquals((byte) 255, data[i]);
		}
	}
	
	@Test
	public void testConstructorByteArray() {
		byte[] data0 = { (byte)123, (byte)76, (byte)12 }; 
		Bitfield bitfield = new Bitfield(data0);
		Assert.assertEquals(3, bitfield.getLengthInBytes());
		Assert.assertEquals(3 * 8, bitfield.getLengthInBits());
		byte[] data = bitfield.data();
		Assert.assertNotNull(data);
		
		for (int i = 0; i < data.length; i++) {
			Assert.assertEquals(data0[i], data[i]);
		}
	}
	
	@Test
	public void testSetBit() {
		Bitfield bitfield = new Bitfield(30, false);
		
		Assert.assertEquals(false, bitfield.isBitSet(30));
		
		bitfield.setBit(30);
		
		Assert.assertEquals(true, bitfield.isBitSet(30));
	}
	
	@Test
	public void testUnsetBit() {
		Bitfield bitfield = new Bitfield(30, true);
		
		Assert.assertEquals(true, bitfield.isBitSet(30));
		
		bitfield.unsetBit(30);
		
		Assert.assertEquals(false, bitfield.isBitSet(30));
	}
	
	@Test
	public void testIsNull() {
		Bitfield bitfield = new Bitfield(30, false);
		Assert.assertEquals(true, bitfield.isNull());
		
		for (int i = 0; i < 30; i++) {
			bitfield = new Bitfield(30, false);
			bitfield.setBit(i);
			Assert.assertEquals(false, bitfield.isNull());
		}
		
		// Check unrelevant parts
		for (int i = 30; i < 32; i++) {
			bitfield = new Bitfield(30, false);
			bitfield.setBit(i);
			Assert.assertEquals(true, bitfield.isNull());
		}
	}
	
	@Test
	public void testIsFull() {
		Bitfield bitfield = new Bitfield(30, true);
		
		Assert.assertEquals(true, bitfield.isFull());
		
		for (int i = 0; i < 30; i++) {
			bitfield = new Bitfield(30, true);
			bitfield.unsetBit(i);
			Assert.assertEquals(false, bitfield.isFull());
		}
		
		// Check irrelevant parts
		for (int i = 30; i < 32; i++) {
			bitfield = new Bitfield(30, true);
			bitfield.unsetBit(i);
			Assert.assertEquals(true, bitfield.isFull());
		}
	}
	
	@Test
	public void testCountOfSet() {
		Bitfield bitfield = new Bitfield(13, false);
		Assert.assertEquals(0, bitfield.countOfSet());
		
		for(int i = 0; i < 13; i++) {
			bitfield.setBit(i);
			Assert.assertEquals(i + 1, bitfield.countOfSet());
		}
		
		// Check irrelevant parts
		for(int i = 13; i < 16; i++) {
			bitfield.setBit(i);
			Assert.assertEquals(13, bitfield.countOfSet());
		}
	}
	
//	@Test
//	public void testClone() {
//		byte[] data0 = { (byte)123, (byte)76, (byte)12 }; 
//		Bitfield bitfield0 = new Bitfield(data0);
//		
//		Bitfield bitfield = bitfield0.clone();
//		Assert.assertEquals(3, bitfield.getLengthInBytes());
//		Assert.assertEquals(3 * 8, bitfield.getLengthInBits());
//		byte[] data = bitfield.data();
//		Assert.assertNotNull(data);
//		
//		for (int i = 0; i < data.length; i++) {
//			Assert.assertEquals(data0[i], data[i]);
//		}
//	}
	
	@Test
	public void testGetBitfieldAnd1() {
		byte[] data1 = { (byte)123, (byte)76, (byte)12, (byte)245 };
		byte[] data2 = { (byte)34, (byte)45, (byte)234 };
		
		Bitfield bitfield1 = new Bitfield(data1);
		Bitfield bitfield2 = new Bitfield(data2);
		
		Bitfield bitfield3 = bitfield1.getBitfieldAnd(bitfield2);
		Assert.assertEquals(bitfield1.getLengthInBytes(), bitfield3.getLengthInBytes());
		Assert.assertEquals(bitfield1.getLengthInBits(), bitfield3.getLengthInBits());
		
		byte[] data3 = bitfield3.data();
		Assert.assertEquals(data1.length, data3.length);
		for (int i = 0; i < data2.length; i++) {
			Assert.assertEquals((byte)(data1[i] & data2[i]), data3[i]);
		}
		
		for (int i = data2.length; i < data3.length; i++) {
			Assert.assertEquals(0, data3[i]);
		}
	}
	
	@Test
	public void testSet() {
		Bitfield bitfield = new Bitfield(34, false);
		byte[] data0 = { (byte)34, (byte)45, (byte)234, (byte)245 };
		bitfield.set(data0);
		
		byte[] data = bitfield.data();
		Assert.assertEquals(data0.length, data.length);
		for (int i = 0; i < data0.length; i++) {
			Assert.assertEquals(data0[i], data[i]);
		}
	}
	
	@Test
	public void testGetBitfieldAnd2() {
		byte[] data1 = { (byte)123, (byte)76, (byte)12, (byte)245 };
		byte[] data2 = { (byte)34, (byte)45, (byte)234 };
		
		Bitfield bitfield1 = new Bitfield(data1);
		Bitfield bitfield2 = new Bitfield(data2);
		
		Bitfield bitfield3 = bitfield2.getBitfieldAnd(bitfield1);
		Assert.assertEquals(bitfield2.getLengthInBytes(), bitfield3.getLengthInBytes());
		Assert.assertEquals(bitfield2.getLengthInBits(), bitfield3.getLengthInBits());
		
		byte[] data3 = bitfield3.data();
		Assert.assertEquals(data2.length, data3.length);
		for (int i = 0; i < data2.length; i++) {
			Assert.assertEquals((byte)(data1[i] & data2[i]), data3[i]);
		}
	}
	
	@Test
	public void testIndexOfFirstSet() {
		Bitfield bitfield = new Bitfield(13, false);
		Assert.assertEquals(-1, bitfield.indexOfFirstSet());
		
		// Check irrelevant parts
		for(int i = 15; i > 12; i--) {
			bitfield.setBit(i);
			Assert.assertEquals(-1, bitfield.indexOfFirstSet());
		}
		
		for(int i = 12; i >= 0; i--) {
			bitfield.setBit(i);
			Assert.assertEquals(i, bitfield.indexOfFirstSet());
		}
	}
	
	@Test
	public void testSetChanged() {
		Bitfield bitfield = new Bitfield(13, false);
		Assert.assertEquals(true, bitfield.isChanged());
		
		bitfield.setChanged(true);
		Assert.assertEquals(true, bitfield.isChanged());
		
		bitfield.setChanged(false);
		Assert.assertEquals(false, bitfield.isChanged());
	}
	
	@Test
	public void testHasSettedComparedTo() {
		Bitfield bitfield1 = new Bitfield(30, false);
		Bitfield bitfield2 = new Bitfield(30, false);
		
		Assert.assertFalse(bitfield1.hasSettedCompearedTo(bitfield2));
		Assert.assertFalse(bitfield2.hasSettedCompearedTo(bitfield1));
		
		for (int i = 0; i < 30; i++) {
			bitfield1.setBit(i);
			
			Assert.assertTrue(bitfield1.hasSettedCompearedTo(bitfield2));
			Assert.assertFalse(bitfield2.hasSettedCompearedTo(bitfield2));
			
			bitfield2.setBit(i);
		}
	}
	
	@Test
	public void testEquals() {
		byte[] data1 = { (byte)123, (byte)76, (byte)12, (byte)245 };
		byte[] data2 = { (byte)123, (byte)76, (byte)12, (byte)245 };
		byte[] data3 = { (byte)123, (byte)76, (byte)12 };
		Bitfield bitfield1 = new Bitfield(data1);
		Bitfield bitfield2 = new Bitfield(data2);
		Bitfield bitfield3 = new Bitfield(data3);
		
		Assert.assertTrue(bitfield1.equals(bitfield2));
		Assert.assertTrue(bitfield2.equals(bitfield1));
		Assert.assertFalse(bitfield1.equals(bitfield3));
		Assert.assertFalse(bitfield3.equals(bitfield1));
		
		bitfield1.setBit(13);
		bitfield2.unsetBit(13);
		
		Assert.assertFalse(bitfield1.equals(bitfield2));
		Assert.assertFalse(bitfield2.equals(bitfield1));
	}
}
