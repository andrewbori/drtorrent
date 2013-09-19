package hu.bute.daai.amorg.drtorrent.test;

import org.junit.Assert;


public class AssertHelper{
	
	public void assertExpect(Class<? extends Exception> exceptionClass) {
		try {
			operation();
			Assert.fail("Expected " + exceptionClass.getName() + " but no exception happened!");
		} catch (Exception e) {
			final String eType = e.getClass().getName();
			if (!eType.equals(exceptionClass.getName())) {
				Assert.fail("Expected " + exceptionClass.getName() + " but got " + eType + ".");
			}
		}
	}
	
	public void operation() throws Exception {
		throw new UnsupportedOperationException();
	};
}
