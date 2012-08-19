package hu.bute.daai.amorg.drtorrent.test;

import hu.bute.daai.amorg.drtorrent.DrTorrentTools;

import org.junit.Assert;
import org.junit.Test;

public class TimeConverterTestCase {
	@Test
	public void wrongInputTest() throws Throwable {
		Assert.assertEquals("", DrTorrentTools.timeToString(0, DrTorrentTools.MSEC, -1));
		Assert.assertEquals("", DrTorrentTools.timeToString(0, -1, 2));
		Assert.assertEquals("???", DrTorrentTools.timeToString(-1, DrTorrentTools.MSEC, 2));
	}
	
	@Test
	public void secTest() throws Throwable {
		Assert.assertEquals("0 s", DrTorrentTools.timeToString(999, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("1 s", DrTorrentTools.timeToString(1000, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("59 s", DrTorrentTools.timeToString(59999, DrTorrentTools.MSEC, 2));
		
		Assert.assertEquals("59 s", DrTorrentTools.timeToString(59, DrTorrentTools.SEC, 2));
		Assert.assertEquals("1 m 0 s", DrTorrentTools.timeToString(60, DrTorrentTools.SEC, 2));
	}
	
	@Test
	public void minTest() throws Throwable {
		Assert.assertEquals("1 m 0 s", DrTorrentTools.timeToString(60000, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("59 m 59 s", DrTorrentTools.timeToString(3599999, DrTorrentTools.MSEC, 2));
	}
	
	@Test
	public void hourTest() throws Throwable {
		Assert.assertEquals("1 h 0 m", DrTorrentTools.timeToString(3600000, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("23 h 59 m", DrTorrentTools.timeToString(86399999, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("23 h 59 m 59 s", DrTorrentTools.timeToString(86399999, DrTorrentTools.MSEC, 3));
	}
		
	@Test
	public void dayTest() throws Throwable {
		Assert.assertEquals("1 d 0 h", DrTorrentTools.timeToString(86400000, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("1 d 0 h 0 m 0 s", DrTorrentTools.timeToString(86400000, DrTorrentTools.MSEC, 4));
		Assert.assertEquals("364 d 23 h", DrTorrentTools.timeToString(31535999999L, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("364 d 23 h 59 m 59 s", DrTorrentTools.timeToString(31535999999L, DrTorrentTools.MSEC, 4));
	}
	
	@Test
	public void yearTest() throws Throwable {
		Assert.assertEquals("1 y 0 d", DrTorrentTools.timeToString(31536000000L, DrTorrentTools.MSEC, 2));
		Assert.assertEquals("1 y 0 d 0 h 0 m 0 s", DrTorrentTools.timeToString(31536000000L, DrTorrentTools.MSEC, 5));
	}
}
