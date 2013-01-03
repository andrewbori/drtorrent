package hu.bute.daai.amorg.drtorrent.network;

import java.util.List;

import android.net.Uri;

public class MagnetUri {

	final private Uri magnetUri_;
	final private Uri uri_;
	
	/** Creates a Magnet Uri from a Uri. (magnet:?xt=urn:btih:...&dn=...&tr=...) */
	public MagnetUri(Uri magnetUri) {
		magnetUri_ = magnetUri;
		String uriStr = magnetUri.getEncodedSchemeSpecificPart();
		String parsableUriStr = "http://www.example.com/" + uriStr;
		
		uri_ = Uri.parse(parsableUriStr);
	}
	
	/** Returns the info hash string. */
	public String getInfoHash() {
		String xt = uri_.getQueryParameter("xt");
		
		String prefix = "urn:btih:";
		if (xt != null && xt.startsWith(prefix) && xt.length() > prefix.length()) {
			return xt.substring(prefix.length());
		}
		
		return null;
	}
	
	/** Returns the name of the torrent. */
	public String getName() {
		String dn = uri_.getQueryParameter("dn");
		return dn;
	}
	
	/** Returns the list of trackers. */
	public List<String> getTrackers() {
		final List<String> trackers = uri_.getQueryParameters("tr");
		return trackers;
	}
	
	@Override
	public String toString() {
		return magnetUri_.toString();
	}
}
