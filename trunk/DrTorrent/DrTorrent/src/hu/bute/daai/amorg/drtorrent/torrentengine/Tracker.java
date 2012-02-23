package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.util.Vector;

import android.os.AsyncTask;


/** Class reprenting the tracker */
public class Tracker {

	private String url_;

	public Tracker(String url) {
		this.url_ = url;
	}
	
	public void downloadPeers() {
		(new TrackerConnectionAsyncTask()).execute(url_);
	}
	
	private class TrackerConnectionAsyncTask extends AsyncTask<String, Integer, Vector<Peer>> {

		@Override
		protected Vector<Peer> doInBackground(String... params) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		protected void onPostExecute(Vector<Peer> result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}
	}
}
