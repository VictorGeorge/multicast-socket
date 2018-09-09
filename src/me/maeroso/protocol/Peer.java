package me.maeroso.protocol;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;
import java.util.UUID;

public class Peer implements Serializable {
    private String id;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public Peer(PublicKey publicKey, PrivateKey privateKey) {
        this.id = UUID.randomUUID().toString().substring(0, 4);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
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
                Objects.equals(publicKey, peer.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, publicKey, privateKey);
    }

    @Override
    public String toString() {
        return String.format("Peer { id='%s'}", id);
    }
}
