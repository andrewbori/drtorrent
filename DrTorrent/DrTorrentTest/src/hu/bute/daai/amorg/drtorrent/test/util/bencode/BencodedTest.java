package hu.bute.daai.amorg.drtorrent.test.util.bencode;

import hu.bute.daai.amorg.drtorrent.test.AssertHelper;
import hu.bute.daai.amorg.drtorrent.util.bencode.Bencoded;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedDictionary;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedException;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedInteger;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedList;
import hu.bute.daai.amorg.drtorrent.util.bencode.BencodedString;

import org.junit.Assert;
import org.junit.Test;

public class BencodedTest {

	@Test
	public void parseInteger() {
		testBencodedInteger(0);
		testBencodedInteger(-1);
		testBencodedInteger(1);
		testBencodedInteger(100000000000L);
		testBencodedInteger(-100000000000L);
		testBencodedInteger(Long.MAX_VALUE);
		testBencodedInteger(Long.MIN_VALUE);
	}
	
	@Test
	public void parseInteger_InvalidNumberFormat() {
		testBencodedInteger("e", BencodedException.class);
		testBencodedInteger("1.5e", BencodedException.class);
		testBencodedInteger("1E2e", BencodedException.class);
		testBencodedInteger("1ae", BencodedException.class);
		testBencodedInteger("abce", BencodedException.class);
	}
	
	@Test
	public void parseInteger_ArrayIndexOutOfBounds() {
		testBencodedInteger("", BencodedException.class);
		testBencodedInteger("1", BencodedException.class);
		testBencodedInteger("1.5", BencodedException.class);
		testBencodedInteger("1E2", BencodedException.class);
		testBencodedInteger("1a", BencodedException.class);
		testBencodedInteger("abc", BencodedException.class);
	}
	
	@Test
	public void parseString() {
		testBencodedString("");
		testBencodedString("a");
		testBencodedString("1");
		testBencodedString(":");
		testBencodedString("i12e");
		testBencodedString("8:12345678");
	}
	
	@Test
	public void parseString_ArrayIndexOutOfBounds() {
		testBencoded("0", BencodedException.class);
		testBencoded("1", BencodedException.class);
		testBencoded("2:a", BencodedException.class);
	}
	
	@Test
	public void parseList() {
		testBencodedList();
		testBencodedList("0:");
		testBencodedList("1:a");
		testBencodedList("i1e");
		testBencodedList("0:", "i0e", "de", "le");
		testBencodedList("3:abe", "i123456e", "d1:a1:be", "li1ei2ee");
	}
	
	@Test
	public void parseList_NoEnd() {
		testBencodedList(false);
		testBencodedList(false, "0:");
		testBencodedList(false, "1:a");
		testBencodedList(false, "i1e");
		testBencodedList(false, "0:", "i0e", "de", "le");
		testBencodedList(false, "3:abe", "i123456e", "d1:a1:be", "li1ei2ee");
	}
	
	@Test
	public void parseDictionary() {
		testBencodedDictionary();
		testBencodedDictionary("0:", "0:");
		testBencodedDictionary("1:a", "1:a");
		testBencodedDictionary("1:a", "i1e");
		testBencodedDictionary("1:a", "0:", "1:b", "i0e", "1:c", "de", "1:d", "le");
		testBencodedDictionary("1:a", "3:abe", "1:b", "i123456e", "1:c", "d1:a1:be", "1:d", "li1ei2ee");
	}
	
	@Test
	public void parseDictionary_NoEnd() {
		testBencodedDictionary(false);
		testBencodedDictionary(false, "0:", "0:");
		testBencodedDictionary(false, "1:a", "1:a");
		testBencodedDictionary(false, "1:a", "i1e");
		testBencodedDictionary(false, "1:a", "0:", "1:b", "i0e", "1:c", "de", "1:d", "le");
		testBencodedDictionary(false, "1:a", "3:abe", "1:b", "i123456e", "1:c", "d1:a1:be", "1:d", "li1ei2ee");
	}
	
	@Test
	public void parseInvalidMarker() {
		testBencoded("a", BencodedException.class);
		testBencoded("lae", BencodedException.class);
		testBencoded("dae", BencodedException.class);
	}

	public void testBencodedInteger(final String value, final Class<? extends Exception> exceptionType) {
		String str = "i" + value;
		testBencoded(str, exceptionType);
	}
	
	private void testBencodedInteger(final long value) {
		String str = "i" + value + "e";
		Bencoded bencoded;
		try {
			bencoded = Bencoded.parse(str.getBytes());
			Assert.assertEquals(Bencoded.BENCODED_INTEGER, bencoded.type());
			Assert.assertEquals(value, ((BencodedInteger)bencoded).getValue());
		} catch (BencodedException e) {
			Assert.fail("BencodedException was thrown!");
		}
	}
	
	private void testBencodedString(final String value) {
		String str = value.length() + ":" + value;
		Bencoded bencoded;
		try {
			bencoded = Bencoded.parse(str.getBytes());
			Assert.assertEquals(Bencoded.BENCODED_STRING, bencoded.type());
			final String val = new String(((BencodedString)bencoded).getValue());
			Assert.assertEquals(value, val);
		} catch (BencodedException e) {
			Assert.fail("BencodedException was thrown!");
		}
	}
	
	private void testBencodedList(final String... elements) {
		testBencodedList(true, elements);
	}
	
	private void testBencodedList(final boolean hasEnd, final String... elements) {
		String str = "l";
		for (String element : elements) {
			str += element;
		}
		if (hasEnd) {
			str += "e";
		}
		
		Bencoded bencoded;
		try {
			bencoded = Bencoded.parse(str.getBytes());
			Assert.assertEquals(Bencoded.BENCODED_LIST, bencoded.type());
			final int count = ((BencodedList)bencoded).count();
			Assert.assertEquals(elements.length, count);
		} catch (BencodedException e) {
			Assert.fail("BencodedException was thrown!");
		}
	}
	
	private void testBencodedDictionary(final String... elements) {
		testBencodedDictionary(true, elements);
	}
	
	private void testBencodedDictionary(final boolean hasEnd, final String... elements) {
		String str = "d";
		for (String element : elements) {
			str += element;
		}
		if (hasEnd) {
			str += "e";
		}
		
		Bencoded bencoded;
		try {
			bencoded = Bencoded.parse(str.getBytes());
			Assert.assertEquals(Bencoded.BENCODED_DICTIONARY, bencoded.type());
			final int count = ((BencodedDictionary)bencoded).count();
			Assert.assertEquals(elements.length / 2, count);
		} catch (BencodedException e) {
			Assert.fail("BencodedException was thrown!");
		}
	}
	
	public void testBencoded(final String bencodedStr, final Class<? extends Exception> exceptionType) {
		new AssertHelper() {
			public void operation() throws Exception {
				Bencoded.parse(bencodedStr.getBytes());
			}
		}.assertExpect(exceptionType);
	}
}
