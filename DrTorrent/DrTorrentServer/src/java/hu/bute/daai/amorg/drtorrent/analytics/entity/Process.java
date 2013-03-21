package hu.bute.daai.amorg.drtorrent.analytics.entity;
// Generated 2013.03.16. 14:59:34 by Hibernate Tools 3.2.1.GA



/**
 * Process generated by hbm2java
 */
public class Process  implements java.io.Serializable {


     private Long id;
     private Torrent torrent;
     private Peer peer;
     private long addedOn;
     private int badPieces;
     private int goodPieces;
     private int failedConnections;
     private int tcpConnections;
     private int handshakes;

    public Process() {
    }

    public Process(Torrent torrent, Peer peer, long addedOn, int badPieces, int goodPieces, int failedConnections, int tcpConnections, int handshakes) {
       this.torrent = torrent;
       this.peer = peer;
       this.addedOn = addedOn;
       this.badPieces = badPieces;
       this.goodPieces = goodPieces;
       this.failedConnections = failedConnections;
       this.tcpConnections = tcpConnections;
       this.handshakes = handshakes;
    }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    public Torrent getTorrent() {
        return this.torrent;
    }
    
    public void setTorrent(Torrent torrent) {
        this.torrent = torrent;
    }
    public Peer getPeer() {
        return this.peer;
    }
    
    public void setPeer(Peer peer) {
        this.peer = peer;
    }
    public long getAddedOn() {
        return this.addedOn;
    }
    
    public void setAddedOn(long addedOn) {
        this.addedOn = addedOn;
    }
    public int getBadPieces() {
        return this.badPieces;
    }
    
    public void setBadPieces(int badPieces) {
        this.badPieces = badPieces;
    }
    public int getGoodPieces() {
        return this.goodPieces;
    }
    
    public void setGoodPieces(int goodPieces) {
        this.goodPieces = goodPieces;
    }
    public int getFailedConnections() {
        return this.failedConnections;
    }
    
    public void setFailedConnections(int failedConnections) {
        this.failedConnections = failedConnections;
    }
    public int getTcpConnections() {
        return this.tcpConnections;
    }
    
    public void setTcpConnections(int tcpConnections) {
        this.tcpConnections = tcpConnections;
    }
    public int getHandshakes() {
        return this.handshakes;
    }
    
    public void setHandshakes(int handshakes) {
        this.handshakes = handshakes;
    }




}


