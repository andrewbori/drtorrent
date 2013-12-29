package hu.bute.daai.amorg.drtorrent.analytics.entity;
// Generated 2013.12.29. 20:16:51 by Hibernate Tools 3.2.1.GA


import java.util.HashSet;
import java.util.Set;

/**
 * Peer generated by hbm2java
 */
public class Peer  implements java.io.Serializable {


     private Long id;
     private String peerIdentifier;
     private Set<Process> processes = new HashSet<Process>(0);
     private Set<Powerinfo> powerinfos = new HashSet<Powerinfo>(0);
     private Set<Networkinfo> networkinfos = new HashSet<Networkinfo>(0);

    public Peer() {
    }

	
    public Peer(String peerIdentifier) {
        this.peerIdentifier = peerIdentifier;
    }
    public Peer(String peerIdentifier, Set<Process> processes, Set<Powerinfo> powerinfos, Set<Networkinfo> networkinfos) {
       this.peerIdentifier = peerIdentifier;
       this.processes = processes;
       this.powerinfos = powerinfos;
       this.networkinfos = networkinfos;
    }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    public String getPeerIdentifier() {
        return this.peerIdentifier;
    }
    
    public void setPeerIdentifier(String peerIdentifier) {
        this.peerIdentifier = peerIdentifier;
    }
    public Set<Process> getProcesses() {
        return this.processes;
    }
    
    public void setProcesses(Set<Process> processes) {
        this.processes = processes;
    }
    public Set<Powerinfo> getPowerinfos() {
        return this.powerinfos;
    }
    
    public void setPowerinfos(Set<Powerinfo> powerinfos) {
        this.powerinfos = powerinfos;
    }
    public Set<Networkinfo> getNetworkinfos() {
        return this.networkinfos;
    }
    
    public void setNetworkinfos(Set<Networkinfo> networkinfos) {
        this.networkinfos = networkinfos;
    }




}


