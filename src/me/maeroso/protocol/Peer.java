package me.maeroso.protocol;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;
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
        this.id = UUID.randomUUID().toString().substring(0,4);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(id, peer.id) &&
                Objects.equals(socketAddress, peer.socketAddress) &&
                Objects.equals(publicKey, peer.publicKey) &&
                Objects.equals(privateKey, peer.privateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, socketAddress, publicKey, privateKey);
    }

    @Override
    public String toString() {
        return String.format("Peer { id='%s', socketAddress=%s }", id, socketAddress);
    }
}
