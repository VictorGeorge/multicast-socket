package me.maeroso.protocol;

import me.maeroso.enums.EnumResourceId;
import me.maeroso.enums.EnumResourceStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Message implements Serializable {
    public Peer sourcePeer;
    public Peer destinationPeer;
    public MessageType messageType;
    public EnumResourceId resource;
    public EnumResourceStatus status;
    public byte[] signature;

    Instant timestamp;

    public Message(MessageType messageType, Peer sourcePeer) {
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
    }

    public Message(MessageType messageType, Peer sourcePeer, Peer destinationPeer) {
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
        this.destinationPeer = destinationPeer;
    }

    public Message(MessageType messageType, Peer sourcePeer, EnumResourceId resource, Instant timestamp) { //Mensagem de requisição de recurso ou mudança na fila
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
        this.resource = resource;
        this.timestamp = timestamp;
    }

    public Message(MessageType messageType, Peer sourcePeer, EnumResourceId resource) { //Mensagem de liberação de recurso
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
        this.resource = resource;
    }

    public Message(MessageType messageType, Peer sourcePeer, Peer destinationPeer, EnumResourceId resource, EnumResourceStatus status, Instant timestamp) { //Mensagem de resposta a requisição de recurso
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
        this.destinationPeer = destinationPeer;
        this.resource = resource;
        this.status = status;
        this.timestamp = timestamp;
    }

    public Message(MessageType messageType, Peer sourcePeer, EnumResourceId resource, Instant timestamp, byte[] signature) { //Mensagem de requisição de recurso ou mudança na fila
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
        this.resource = resource;
        this.timestamp = timestamp;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(sourcePeer, message.sourcePeer) &&
                Objects.equals(destinationPeer, message.destinationPeer) &&
                messageType == message.messageType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePeer, destinationPeer, messageType);
    }

    @Override
    public String toString() {
        return String.format("Message { sourcePeer=%s, destinationPeer=%s, messageType=%s }", sourcePeer, destinationPeer, messageType);
    }

    public EnumResourceId getResource() {
        return resource;
    }

    public Peer getDestinationPeer() {
        return destinationPeer;
    }

    public Peer getSourcePeer() {
        return sourcePeer;
    }

    public EnumResourceStatus getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public enum MessageType {
        GREETING_REQUEST, GREETING_RESPONSE, LEAVE_REQUEST, LEAVE_RESPONSE, RESOURCE_REQUEST, RESOURCE_RESPONSE, RESOURCE_RELEASE, QUEUE_ADD, QUEUE_REMOVE
    }
}
