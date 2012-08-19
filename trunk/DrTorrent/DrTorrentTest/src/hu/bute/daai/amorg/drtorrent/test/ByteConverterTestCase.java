package hu.bute.daai.amorg.drtorrent.test;

import static org.junit.Assert.*;
import hu.bute.daai.amorg.drtorrent.DrTorrentTools;

import org.junit.Test;

public class ByteConverterTestCase {

	@Test
	public void byteTest() {
		assertEquals("0 B", DrTorrentTools.bytesToString(0));
		assertEquals("1023 B", DrTorrentTools.bytesToString(1023));		
	}

	@Test
	public void kiloByteTest() {
		assertEquals("1 kB", DrTorrentTools.bytesToString(1024));
		assertEquals("1023 kB", DrTorrentTools.bytesToString(1023 * 1024));
		assertEquals("64,5 kB", DrTorrentTools.bytesToString((long) (64.5 * 1024.0)));
	}
	
	@Test
	public void megaByteTest() {
		assertEquals("1 MB", DrTorrentTools.bytesToString(1024 * 1024));
		assertEquals("1023 MB", DrTorrentTools.bytesToString(1023 * 1024 * 1024));
	}
	
	@Test
	public void gigaByteTest() {
		assertEquals("1 GB", DrTorrentTools.bytesToString(1024 * 1024 * 1024));
		assertEquals("1023 GB", DrTorrentTools.bytesToString(1023L * 1024L * 1024L * 1024L));
	}
	
	@Test
	public void teraByteTest() {
		assertEquals("1 TB", DrTorrentTools.bytesToString(1024L * 1024L * 1024L * 1024L));
		assertEquals("1023 TB", DrTorrentTools.bytesToString(1023L * 1024L * 1024L * 1024L * 1024L));
	}
}
