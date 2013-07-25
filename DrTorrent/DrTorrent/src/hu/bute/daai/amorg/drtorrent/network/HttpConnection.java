package hu.bute.daai.amorg.drtorrent.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class HttpConnection {
	final private static String LOG_TAG = "HttpConnection";
	final private String url_;
	private String message_ = null;
	
	/** Constructor with the URL that has to be connecting to. */
	public HttpConnection(final String url) {
		url_ = url;
	}
	
	/** Executes the GET request and returns the response as a byte array. */
	public byte[] execute() {
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url_);
			
			Log.v(LOG_TAG, "Connecting to: " + url_);
			HttpResponse response = client.execute(get);
			Log.v(LOG_TAG, "Reading the respone from: " + url_);
			
			is = response.getEntity().getContent();
			
			Header contentEncoding = response.getFirstHeader("Content-Encoding");
			if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				is = new GZIPInputStream(is);
			}

			baos = new ByteArrayOutputStream();
			int ch;
            while((ch = is.read()) != -1)
            {
                baos.write(ch);
            }
            
            return baos.toByteArray();

		} catch (IOException e) {
			message_ = e.getMessage();
			Log.v(LOG_TAG, "IOException: " + message_);
			return null;
		} catch (Exception e) {
			message_ = e.getMessage();
			Log.v(LOG_TAG, "Exception: " + message_);
			return null;	
		} finally {
			try {
				if (is != null) is.close();
			} catch (IOException e) {}
			try {
				if (baos != null) baos.close();
			} catch (IOException e) {}
		}
	}
	
	/** Returns the error message. If the connection was successful it equals NULL. */
	public String getMessage() {
		return message_;
	}	
}