package me.maeroso.protocol;

import me.maeroso.enums.EnumResourceId;
import me.maeroso.enums.EnumResourceStatus;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Peer implements Serializable {
    private String id;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Map<EnumResourceId, EnumResourceStatus> resourcesState;
    private Map<EnumResourceId, AtomicReference<Instant>> resourceWanted;

    public Peer(PublicKey publicKey, PrivateKey privateKey) {
        this.id = UUID.randomUUID().toString().substring(0, 4);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.resourcesState = new HashMap<EnumResourceId, EnumResourceStatus>();
        this.resourceWanted = new HashMap<EnumResourceId, AtomicReference<Instant>>();
        //Adiciona estados iniciais de cada um dos 2 recursos
        this.resourcesState.put(EnumResourceId.RESOURCE1,
                EnumResourceStatus.RELEASED);
        this.resourcesState.put(EnumResourceId.RESOURCE2,
                EnumResourceStatus.RELEASED);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getId() {
        return id;
    }

    public Map<EnumResourceId, EnumResourceStatus> getResourcesState(){
        return resourcesState;
    }

    public Map<EnumResourceId, AtomicReference<Instant>> getResourceWanted(){
        return resourceWanted;
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
