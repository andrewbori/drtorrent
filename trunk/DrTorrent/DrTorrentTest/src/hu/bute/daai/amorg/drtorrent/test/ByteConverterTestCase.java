package hu.bute.daai.amorg.drtorrent.test;

import static org.junit.Assert.assertEquals;
import hu.bute.daai.amorg.drtorrent.Tools;

import org.junit.Test;

public class ByteConverterTestCase {

	@Test
	public void byteTest() {
		assertEquals("0 B", Tools.bytesToString(0));
		assertEquals("1023 B", Tools.bytesToString(1023));		
	}

	@Test
	public void kiloByteTest() {
		assertEquals("1 kB", Tools.bytesToString(1024));
		assertEquals("1023 kB", Tools.bytesToString(1023 * 1024));
		assertEquals("64,5 kB", Tools.bytesToString((long) (64.5 * 1024.0)));
	}
	
	@Test
	public void megaByteTest() {
		assertEquals("1 MB", Tools.bytesToString(1024 * 1024));
		assertEquals("1023 MB", Tools.bytesToString(1023 * 1024 * 1024));
	}
	
	@Test
	public void gigaByteTest() {
		assertEquals("1 GB", Tools.bytesToString(1024 * 1024 * 1024));
		assertEquals("1023 GB", Tools.bytesToString(1023L * 1024L * 1024L * 1024L));
	}
	
	@Test
	public void teraByteTest() {
		assertEquals("1 TB", Tools.bytesToString(1024L * 1024L * 1024L * 1024L));
		assertEquals("1023 TB", Tools.bytesToString(1023L * 1024L * 1024L * 1024L * 1024L));
	}
}
