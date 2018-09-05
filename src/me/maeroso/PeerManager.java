package me.maeroso;

import me.maeroso.protocol.Peer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PeerManager {
    private static PeerManager ourInstance = new PeerManager();
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
                ourInstance.ourPeer = new Peer(new InetSocketAddress(InetAddress.getLocalHost(), generatedPort), keyPair.getPublic());
            } catch (NoSuchAlgorithmException | UnknownHostException e) {
                e.printStackTrace();
            }

        }
        return ourInstance.ourPeer;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public void add(Peer p) {
        peerList.add(p);
        System.err.println("Added peer to list: " + p.toString());
    }
}
