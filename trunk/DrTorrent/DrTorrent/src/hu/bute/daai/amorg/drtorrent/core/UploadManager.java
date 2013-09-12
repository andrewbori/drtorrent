package hu.bute.daai.amorg.drtorrent.core;

import hu.bute.daai.amorg.drtorrent.core.peer.Peer;
import hu.bute.daai.amorg.drtorrent.util.Preferences;

import java.util.Vector;

public class UploadManager {

	private final Vector<BlockToUpload> blocksToUpload_;
	private UploadThread uploadThread_ = null;
	
	private final Speed uploadSpeed_;
	private int latestUploadedBytes_ = 0;
	
	public UploadManager() {
		blocksToUpload_ = new Vector<UploadManager.BlockToUpload>();
		uploadSpeed_ = new Speed();
	}
	
	/** Schedules the Upload Manager. */
	public void onTimer(long time) {
		synchronized (uploadSpeed_) {
			uploadSpeed_.addBytes(latestUploadedBytes_, time);
			latestUploadedBytes_ = 0;
			
			uploadSpeed_.notify();
		}
	}
	
	/** Adds a block to the list. */
	public void addBlock(Block block, Peer peer) {
		blocksToUpload_.addElement(new BlockToUpload(block, peer));
		
		if (uploadThread_ == null) {
			uploadThread_ = new UploadThread();
			uploadThread_.start();
		}
		synchronized (blocksToUpload_) {
			blocksToUpload_.notify();
		}
	}
	
	/** Removes a block from the list. */
	public void removeBlock(Block block, Peer peer) {
		synchronized (blocksToUpload_) {
			BlockToUpload blockToUpload;
			for (int i = 0; i < blocksToUpload_.size(); i++) {
				blockToUpload = blocksToUpload_.elementAt(i);
				if (blockToUpload.getBlock().equals(block) && blockToUpload.getPeer().equals(peer)) {
					blocksToUpload_.removeElement(blockToUpload);
					break;
				}
			}
		}
	}
	
	/** Shuts down the manager. */
	public void shutDown() {
		if (uploadThread_ != null) {
			uploadThread_.disable();
			uploadThread_ = null;
		}
	}
	
	private class UploadThread extends Thread {
		private boolean isEnabled_ = true; 
		
		@Override
		public void run() {
			while(isEnabled_) {
				while(blocksToUpload_.size() > 0 && isEnabled_) {
					if (uploadSpeed_.getSpeed() >= Preferences.getUploadSpeedLimit() || latestUploadedBytes_ >= Preferences.getUploadSpeedLimit()) {
						synchronized (uploadSpeed_) {
							try {
								uploadSpeed_.wait();
							} catch (Exception e) {
							}
						}
					} else {
						BlockToUpload blockToUpload = blocksToUpload_.remove(0);
						if (blockToUpload.getPeer().issueUpload(blockToUpload.getBlock())) {
							synchronized (uploadSpeed_) {
								latestUploadedBytes_ += blockToUpload.getBlock().length();
							}
						}
					}
				}
				
				if (isEnabled_) {
					synchronized (blocksToUpload_) {
						try {
							blocksToUpload_.wait();
						} catch (Exception e) {
						}
					}
				}
			}
		}
		
		public void disable() {
			isEnabled_ = false;
			synchronized (blocksToUpload_) {
				blocksToUpload_.notify();
			}
			synchronized (uploadSpeed_) {
				uploadSpeed_.notify();
			}
		}
	}
	
	/** Contains info from the block that has to be sent. */
	public class BlockToUpload {
		private Block block_;
		private Peer peer_;
		
		public BlockToUpload(Block block, Peer peer) {
			block_ = block;
			peer_ = peer;
		}

		public Block getBlock() {
			return block_;
		}

		public Peer getPeer() {
			return peer_;
		}
	}

	public int getUploadSpeed() {
		return uploadSpeed_.getSpeed();
	}
}
