package hu.bute.daai.amorg.drtorrent.test.core.tracker;

import hu.bute.daai.amorg.drtorrent.core.peer.PeerInfo;
import hu.bute.daai.amorg.drtorrent.core.tracker.Tracker;
import hu.bute.daai.amorg.drtorrent.core.tracker.HttpTracker;
import hu.bute.daai.amorg.drtorrent.util.Preferences;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpTrackerTest {

	private static String url_ = "http://localhost:8084/DrTorrent/announce";
	
	@BeforeClass
	public static void beforeClass() {
		Preferences.setTesting(true);
	}
	
	@Test
	public void testAnnounce() {
		TrackerObserverMock torrent = new TrackerObserverMock("Correct");
		
		Tracker tracker = new HttpTracker(url_, torrent);
		Assert.assertEquals(url_, tracker.getUrl());
		
		// Started
		tracker.changeEvent(Tracker.EVENT_STARTED);
		
		Assert.assertEquals(Tracker.STATUS_WORKING, tracker.getStatus());
		
		Assert.assertEquals(23, tracker.getComplete());
		Assert.assertEquals(7, tracker.getIncomplete());
		Assert.assertEquals(1800, tracker.getInterval());
		
		ArrayList<PeerInfo> peers = torrent.getPeers();
		Assert.assertEquals(3, peers.size());
		
		Assert.assertEquals("abcdefghijklmnopqrst", peers.get(0).getPeerId());
		Assert.assertEquals("192.168.0.100", peers.get(0).getAddress());
		Assert.assertEquals(6881, peers.get(0).getPort());
		
		Assert.assertEquals("zyxwvutsrqponmlkjihi", peers.get(1).getPeerId());
		Assert.assertEquals("192.168.100.128", peers.get(1).getAddress());
		Assert.assertEquals(6882, peers.get(1).getPort());
		
		Assert.assertEquals("abc123efg456ijk7890z", peers.get(2).getPeerId());
		Assert.assertEquals("0.0.0.0", peers.get(2).getAddress());
		Assert.assertEquals(6883, peers.get(2).getPort());
		
		// Completed
		tracker.changeEvent(Tracker.EVENT_COMPLETED);
		Assert.assertEquals(Tracker.STATUS_WORKING, tracker.getStatus());
		Assert.assertEquals(6, peers.size());
	}
	
	@Test
	public void testAnnounce_Compact() {
		TrackerObserverMock torrent = new TrackerObserverMock("CorrectCompact");
		
		Tracker tracker = new HttpTracker(url_, torrent);
		tracker.changeEvent(Tracker.EVENT_NOT_SPECIFIED);
		
		Assert.assertEquals(Tracker.STATUS_WORKING, tracker.getStatus());
		
		Assert.assertEquals(37, tracker.getComplete());
		Assert.assertEquals(13, tracker.getIncomplete());
		Assert.assertEquals(1900, tracker.getInterval());
		
		ArrayList<PeerInfo> peers = torrent.getPeers();
		Assert.assertEquals(2, peers.size());
		
		Assert.assertNull(peers.get(0).getPeerId());
		Assert.assertEquals("192.168.0.100", peers.get(0).getAddress());
		Assert.assertEquals(6886, peers.get(0).getPort());
		
		Assert.assertNull(peers.get(1).getPeerId());
		Assert.assertEquals("192.168.100.128", peers.get(1).getAddress());
		Assert.assertEquals(6887, peers.get(1).getPort());
	}
	
	@Test
	public void testAnnounce_Null() {
		TrackerObserverMock torrent = new TrackerObserverMock("Null");
		
		Tracker tracker = new HttpTracker("http://localhost:8100/DrTorrent/announce", torrent);
		tracker.changeEvent(Tracker.EVENT_STARTED);
		
		Assert.assertEquals(Tracker.STATUS_FAILED, tracker.getStatus());
	}
	
	@Test
	public void testAnnounce_Empty() {
		TrackerObserverMock torrent = new TrackerObserverMock("Empty");
		
		Tracker tracker = new HttpTracker(url_, torrent);
		tracker.changeEvent(Tracker.EVENT_STARTED);
		
		Assert.assertEquals(Tracker.STATUS_FAILED, tracker.getStatus());
	}
	
	@Test
	public void testAnnounce_WrongConent() {
		TrackerObserverMock torrent = new TrackerObserverMock("WrongContent");
		
		Tracker tracker = new HttpTracker(url_, torrent);
		tracker.changeEvent(Tracker.EVENT_STARTED);
		
		Assert.assertEquals(Tracker.STATUS_FAILED, tracker.getStatus());
	}
	
	@Test
	public void testAnnounce_Stopped() {
		TrackerObserverMock torrent = new TrackerObserverMock("Correct");
		
		Tracker tracker = new HttpTracker(url_, torrent);
		tracker.changeEvent(Tracker.EVENT_STOPPED);
		
		Assert.assertEquals(Tracker.STATUS_UNKNOWN, tracker.getStatus());
	}
	
	@Test
	public void testAnnounce_Failure() {
		TrackerObserverMock torrent = new TrackerObserverMock("Failure");
		
		Tracker tracker = new HttpTracker(url_, torrent);
		tracker.changeEvent(Tracker.EVENT_COMPLETED);
		
		Assert.assertEquals(Tracker.STATUS_FAILED, tracker.getStatus());
	}
}
