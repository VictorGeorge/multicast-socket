package me.maeroso.protocol;

import java.io.Serializable;
import java.util.Objects;

public class Message implements Serializable {
    public Peer sourcePeer;
    public Peer destinationPeer;
    public MessageType messageType;

    public Message(MessageType messageType, Peer sourcePeer) {
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
    }

    public Message(MessageType messageType, Peer sourcePeer, Peer destinationPeer) {
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
        this.destinationPeer = destinationPeer;
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

    public enum MessageType {
        GREETING_REQUEST, GREETING_RESPONSE, LEAVE_REQUEST
    }
}
