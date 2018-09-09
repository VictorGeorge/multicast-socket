package me.maeroso.protocol;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.UUID;

public class Peer implements Serializable {
    private String id;
    private InetSocketAddress socketAddress;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public Peer(InetSocketAddress socketAddress, PublicKey publicKey, PrivateKey privateKey) {
        this.socketAddress = socketAddress;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.id = UUID.randomUUID().toString().substring(0, 2);
    }

    public InetAddress getAddress() {
        return socketAddress.getAddress();
    }

    public int getPort() {
        return socketAddress.getPort();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Peer{" +
                "socketAddress=" + socketAddress +
                ", publicKey=" + publicKey +
                '}';
    }
}
