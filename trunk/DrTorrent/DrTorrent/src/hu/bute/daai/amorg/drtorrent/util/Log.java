package hu.bute.daai.amorg.drtorrent.util;

/**
 * Generally, use the Log.v() Log.d() Log.i() Log.w() and Log.e() methods. <br><br>
 * The order in terms of verbosity, from least to most is ERROR, WARN, INFO, DEBUG, VERBOSE. Verbose should never be compiled into an application except during development. Debug logs are compiled in but stripped at runtime. Error, warning and info logs are always kept. <br><br> 
 * <b>Tip:</b> A good convention is to declare a TAG constant in your class: <br><br>
 *  <code>private static final String TAG = "MyActivity";</code> <br><br>
 * and use that in subsequent calls to the log methods. <br><br>
 * <b>Tip:</b> Don't forget that when you make a call like <br><br>
 *  <code>Log.v(TAG, "index=" + i);</code> <br><br>
 * that when you're building the string to pass into Log.d, the compiler uses a StringBuilder and at least three allocations occur: the StringBuilder itself, the buffer, and the String object. Realistically, there is also another buffer allocation and copy, and even more pressure on the gc. That means that if your log message is filtered out, you might be doing significant work and incurring significant overhead.
 */
public class Log {
	  
	 /** 
	  * Sends a VERBOSE Log message.
	  * 
	  *  @param tag Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
	  *  @param msg  The message you would like logged.
	  */
	public static void v(final String tag, final String msg) {
		Log.v(tag, msg);
	}
	
}
