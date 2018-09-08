package me.maeroso.protocol;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;

public class Peer implements Serializable {
    private InetSocketAddress socketAddress;
    private PublicKey publicKey;

    public Peer(InetSocketAddress socketAddress, PublicKey publicKey) {
        this.socketAddress = socketAddress;
        this.publicKey = publicKey;
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

    @Override
    public String toString() {
        return "Peer{" +
                "socketAddress=" + socketAddress +
                ", publicKey=" + publicKey +
                '}';
    }
}
