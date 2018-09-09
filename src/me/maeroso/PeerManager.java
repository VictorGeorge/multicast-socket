package me.maeroso;

import me.maeroso.protocol.Peer;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Singleton de estado da lista de peers conhecidos pela instância
 */
public class PeerManager {
    private static PeerManager ourInstance = new PeerManager();
    private Peer ourPeer;
    private List<Peer> peerList;
    private Boolean started = false;

    private PeerManager() {
        this.peerList = new LinkedList<>();
    }

    public static PeerManager getInstance() {
        return ourInstance;
    }

    public Peer getOurPeer() {
        if (ourInstance.ourPeer == null) {
            int generatedPort = new Random().nextInt(50) + 4200;
            System.err.println("Generated instance port: " + generatedPort);
            try {
                KeyPair keyPair = CryptoUtils.generateRSA();
                ourInstance.ourPeer = new Peer(keyPair.getPublic(), keyPair.getPrivate());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        }
        return ourInstance.ourPeer;
    }

    public void add(Peer p) {
        peerList.add(p);
        System.err.println("Added peer to list: " + p.getId());
        updateStarted(peerList);
    }

    public void remove(Peer p) {
        peerList.remove(p);
        System.err.println("Removing peer from list: " + p.getId());
    }

    public void printPeerList() {
        if (peerList.size() == 0)
            System.out.println("\nEmpty peers list\n");
        else {
            System.out.println("\nPeers list: \n");
            for (Peer peer : peerList) {
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

    public boolean isStarted() {
        return started;
    }
}
