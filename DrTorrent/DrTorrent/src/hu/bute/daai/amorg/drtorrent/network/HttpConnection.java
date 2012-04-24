package hu.bute.daai.amorg.drtorrent.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class HttpConnection {
	private final static String LOG_TAG = "HttpConnection";
	private String url_;
	
	public HttpConnection(String url) {
		url_ = url;
	}
	
	public byte[] execute() {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url_);
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		
		try {
			Log.v(LOG_TAG, "Connecting to: " + url_);
			HttpResponse response = client.execute(get);
			Log.v(LOG_TAG, "Reading the respone from: " + url_);
			
			is = response.getEntity().getContent();
			baos = new ByteArrayOutputStream();
			int ch;
            while((ch = is.read()) != -1)
            {
                baos.write(ch);
            }
            
            return baos.toByteArray();

		} catch (IOException e) {
			Log.v(LOG_TAG, "Exception:" + e.getMessage());
			return null;
		} finally {
			try {
				if (is != null) is.close();
				if (baos != null) baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
