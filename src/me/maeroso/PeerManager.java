package me.maeroso;

import me.maeroso.enums.EnumResourceId;
import me.maeroso.protocol.Peer;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Singleton de estado da lista de peers conhecidos pela instância
 */
public enum PeerManager {
    INSTANCE();

    private Peer ourPeer;
    private List<Peer> peerList;
    private Boolean started = false;
    private Map<Instant, Peer> resourceWanted1;
    private Map<Instant, Peer> resourceWanted2;

    PeerManager() {
        this.peerList = new LinkedList<>();
        try {
            KeyPair keyPair = CryptoUtils.generateRSA();
            this.ourPeer = new Peer(keyPair.getPublic(), keyPair.getPrivate());
            System.err.println("Our peer ID: " + this.ourPeer.getId());
            this.resourceWanted1 = new TreeMap<Instant, Peer>();
            this.resourceWanted2 = new TreeMap<Instant, Peer>();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public Peer getOurPeer() {
        return this.ourPeer;
    }

    public void add(Peer p) {
        this.peerList.add(p);
        System.err.println("Added peer to list: " + p.getId());
        updateStarted(this.peerList);
    }

    public void remove(Peer p) {
        this.peerList.remove(p);
        System.err.println("Removing peer from list: " + p.getId());
    }

    public void printPeerList() {
        if (this.peerList.size() == 0)
            System.out.println("\nEmpty peers list\n");
        else {
            System.out.println("\nPeers list: \n");
            for (Peer peer : this.peerList) {
                System.out.println(peer.getId() + "\n");
            }
        }
    }

    public List<Peer> getPeerList() {
        return peerList;
    }

    public void updateStarted(List<Peer> peerList) {
        if (!started && peerList.size() + 1 >= Configuration.MINIMUM_PEERS)
            started = true;
    }

    public Map<Instant, Peer> getResourceWanted(EnumResourceId resource) {
        if (resource.equals(EnumResourceId.RESOURCE1))
            return resourceWanted1;
        return resourceWanted2;
    }

    public boolean isStarted() {
        return started;
    }
}
