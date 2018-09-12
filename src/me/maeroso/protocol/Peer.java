package me.maeroso.protocol;

import me.maeroso.CryptoUtils;
import me.maeroso.enums.EnumResourceId;
import me.maeroso.enums.EnumResourceStatus;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Peer implements Serializable {
    private String id;
    private PublicKey publicKey;
    private transient PrivateKey privateKey;
    private Map<EnumResourceId, EnumResourceStatus> resourcesState;
    private byte[] signature;

    public Peer(PublicKey publicKey, PrivateKey privateKey) {
        this.id = UUID.randomUUID().toString().substring(0, 4);
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.resourcesState = new HashMap<EnumResourceId, EnumResourceStatus>();

        byte[] idBytes = this.id.getBytes();
        try {
            this.signature = CryptoUtils.sign(this.privateKey, idBytes);
        } catch (InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

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

    public Map<EnumResourceId, EnumResourceStatus> getResourcesState() {
        return resourcesState;
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

    public byte[] getSignature() {
        return signature;
    }
}
