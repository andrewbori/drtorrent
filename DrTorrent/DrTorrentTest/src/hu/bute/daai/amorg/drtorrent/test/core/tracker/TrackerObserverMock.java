package hu.bute.daai.amorg.drtorrent.test.core.tracker;

import hu.bute.daai.amorg.drtorrent.core.peer.PeerImpl;
import hu.bute.daai.amorg.drtorrent.core.peer.PeerInfo;
import hu.bute.daai.amorg.drtorrent.core.tracker.TrackerObserver;

import java.util.ArrayList;

public class TrackerObserverMock implements TrackerObserver {

	private String infoHash_;
	final private ArrayList<PeerInfo> peers_;
	
	public TrackerObserverMock(String infoHash) {
		infoHash_ = infoHash;
		peers_ = new ArrayList<PeerInfo>();
	}
	
	@Override
	public int addPeer(String address, int port, String peerId) {
		peers_.add(new PeerImpl(address, port, peerId, 0));
		return 0;
	}

	@Override
	public void refreshSeedsAndLeechersCount() {
		
	}

	@Override
	public String getInfoHash() {
		return infoHash_;
	}

	@Override
	public byte[] getInfoHashByteArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getBytesLeft() {
		return 0;
	}

	@Override
	public long getBytesDownloaded() {
		return 0;
	}

	@Override
	public long getBytesUploaded() {
		return 0;
	}
	
	public ArrayList<PeerInfo> getPeers() {
		return peers_;
	}
}
