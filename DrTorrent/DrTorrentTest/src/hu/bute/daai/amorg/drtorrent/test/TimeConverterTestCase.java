package hu.bute.daai.amorg.drtorrent.test;

import hu.bute.daai.amorg.drtorrent.Tools;

import org.junit.Assert;
import org.junit.Test;

public class TimeConverterTestCase {
	@Test
	public void wrongInputTest() throws Throwable {
		Assert.assertEquals("", Tools.timeToString(0, Tools.MSEC, -1));
		Assert.assertEquals("", Tools.timeToString(0, -1, 2));
		Assert.assertEquals("???", Tools.timeToString(-1, Tools.MSEC, 2));
	}
	
	@Test
	public void secTest() throws Throwable {
		Assert.assertEquals("0 s", Tools.timeToString(999, Tools.MSEC, 2));
		Assert.assertEquals("1 s", Tools.timeToString(1000, Tools.MSEC, 2));
		Assert.assertEquals("59 s", Tools.timeToString(59999, Tools.MSEC, 2));
		
		Assert.assertEquals("59 s", Tools.timeToString(59, Tools.SEC, 2));
		Assert.assertEquals("1 m 0 s", Tools.timeToString(60, Tools.SEC, 2));
	}
	
	@Test
	public void minTest() throws Throwable {
		Assert.assertEquals("1 m 0 s", Tools.timeToString(60000, Tools.MSEC, 2));
		Assert.assertEquals("59 m 59 s", Tools.timeToString(3599999, Tools.MSEC, 2));
	}
	
	@Test
	public void hourTest() throws Throwable {
		Assert.assertEquals("1 h 0 m", Tools.timeToString(3600000, Tools.MSEC, 2));
		Assert.assertEquals("23 h 59 m", Tools.timeToString(86399999, Tools.MSEC, 2));
		Assert.assertEquals("23 h 59 m 59 s", Tools.timeToString(86399999, Tools.MSEC, 3));
	}
		
	@Test
	public void dayTest() throws Throwable {
		Assert.assertEquals("1 d 0 h", Tools.timeToString(86400000, Tools.MSEC, 2));
		Assert.assertEquals("1 d 0 h 0 m 0 s", Tools.timeToString(86400000, Tools.MSEC, 4));
		Assert.assertEquals("364 d 23 h", Tools.timeToString(31535999999L, Tools.MSEC, 2));
		Assert.assertEquals("364 d 23 h 59 m 59 s", Tools.timeToString(31535999999L, Tools.MSEC, 4));
	}
	
	@Test
	public void yearTest() throws Throwable {
		Assert.assertEquals("1 y 0 d", Tools.timeToString(31536000000L, Tools.MSEC, 2));
		Assert.assertEquals("1 y 0 d 0 h 0 m 0 s", Tools.timeToString(31536000000L, Tools.MSEC, 5));
	}
}
