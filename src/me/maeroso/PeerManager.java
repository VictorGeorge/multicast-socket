package me.maeroso;

import me.maeroso.protocol.Peer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PeerManager {
    private static PeerManager ourInstance = new PeerManager();
    private KeyPair keyPair;
    private Peer ourPeer;
    private List<Peer> peerList;

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
                ourInstance.ourPeer = new Peer(new InetSocketAddress(InetAddress.getLocalHost(), generatedPort), keyPair.getPublic(), keyPair.getPrivate());
            } catch (UnknownHostException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        }
        return ourInstance.ourPeer;
    }

    public void add(Peer p) {
        peerList.add(p);
        System.err.println("Added peer to list: " + p.getId());
    }

    public void remove(Peer p) {
        for(Peer peer : peerList){
            if(peer.getId().equals(p.getId())) {
                peerList.remove(peer);
                System.err.println("Removing peer from list: " + p.getId());
                return;
            }
        }
        System.err.println("Failed to remove: " + p.getId());
    }

    public void printPeerList(){
        if(peerList.size() == 0)
            System.out.println("Empty peers list\n");
        else {
            System.out.println("Peers list: \n");
            for (Peer peer : peerList) {
                System.out.println(peer.getId() + "\n");
            }
        }
    }
}
