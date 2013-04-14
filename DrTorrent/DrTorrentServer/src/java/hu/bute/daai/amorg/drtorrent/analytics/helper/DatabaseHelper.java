/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.bute.daai.amorg.drtorrent.analytics.helper;

import hu.bute.daai.amorg.drtorrent.analytics.HibernateUtil;
import hu.bute.daai.amorg.drtorrent.analytics.entity.Peer;
import hu.bute.daai.amorg.drtorrent.analytics.entity.Process;
import hu.bute.daai.amorg.drtorrent.analytics.entity.Torrent;
import java.util.ArrayList;
import javax.annotation.ManagedBean;
import org.hibernate.Session;

/**
 *
 * @author Andreas
 */
@ManagedBean
public class DatabaseHelper {
    private Session session_ = null;
    
    public DatabaseHelper () {
        session_ = HibernateUtil.getSessionFactory().openSession();
    }
    
    public void test() {
        session_.beginTransaction();
    }
    
    public Torrent findTorrent(String infoHash) {
        Torrent torrent = null;
        try {
            session_.beginTransaction();
            torrent = (Torrent) session_.createQuery("from Torrent where infoHash like :infoHash")
                .setString("infoHash", infoHash)
                .uniqueResult();
        } catch (Exception e) {
        }
        return torrent;
    }
    
    public Torrent addTorrent(String infoHash, long size, int pieces) {
        try {
            session_.beginTransaction();
            
            Torrent torrent = (Torrent) session_.createQuery("from Torrent where infoHash like :infoHash")
                .setString("infoHash", infoHash)
                .uniqueResult();
                    
            if (torrent == null) {
                torrent = new Torrent(infoHash, size, pieces);
                session_.save(torrent);
                session_.getTransaction().commit();
            }
            
            return torrent;
        } catch (Exception e) {
        }
        return null;
    }
    
    public Peer findPeer(String peerIdentifier) {
        Peer peer = null;
        try {
            session_.beginTransaction();
            peer = (Peer) session_.createQuery("from Peer where peerIdentifier like :peerIdentifier")
                .setString("peerIdentifier", peerIdentifier)
                .uniqueResult();
        } catch (Exception e) {
        }
        return peer;
    }
    
    public Peer addPeer(String peerIdentifier) {
        try {
            session_.beginTransaction();
            
            Peer peer = (Peer) session_.createQuery("from Peer where peerIdentifier like :peerIdentifier")
                .setString("peerIdentifier", peerIdentifier)
                .uniqueResult();
            
            if (peer == null) {
                peer = new Peer(peerIdentifier);
                session_.save(peer);
                session_.getTransaction().commit();
            }
            
            return peer;
        } catch (Exception e) {
        }
        return null;
    }
    
    public Process saveOrUpdateProcess(Peer peer, Torrent torrent, long addedOn, int badPieces, int goodPieces, int failedConnections, int tcpConnections, int handshakes,  long downloaded, long completed, long uploaded, long downloadingTime, long seedingTime, long completedOn, long removedOn) {
        try {
            session_.beginTransaction();
            
            Process process = (Process) session_.createQuery("from Process where peerId = :peerId and torrentId = :torrentId and addedOn = :addedOn")
                    .setLong("peerId", peer.getId())
                    .setLong("torrentId", torrent.getId())
                    .setLong("addedOn", addedOn)
                    .uniqueResult();
            
            if (process == null) {
                process = new Process(torrent, peer, addedOn, badPieces, goodPieces, failedConnections, tcpConnections, handshakes, downloaded, completed, uploaded, downloadingTime, seedingTime, completedOn, removedOn);
            } else {
                process.setBadPieces(badPieces);
                process.setGoodPieces(goodPieces);
                process.setFailedConnections(failedConnections);
                process.setTcpConnections(tcpConnections);
                process.setHandshakes(handshakes);
                process.setDownloaded(downloaded);
                process.setCompleted(completed);
                process.setUploaded(uploaded);
                process.setDownloadingTime(downloadingTime);
                process.setSeedingTime(seedingTime);
                process.setCompletedOn(completedOn);
                process.setRemovedOn(removedOn);
            }
            session_.saveOrUpdate(process);
            session_.getTransaction().commit();
            
            return process;
        } catch (Exception e) {
        }
        
        return null;
    }
    
    public ArrayList<Process> getProcesses() {
        try {
            session_.beginTransaction();
            return (ArrayList<Process>) session_.createQuery("from Process").list();
        } catch (Exception e) {
        }
        return null;
    }
    
    public void close() {
        try {
            session_.close();
        } catch (Exception e) {
        }
    }
}
