package me.maeroso.protocol;

import java.io.Serializable;
import java.util.Objects;

public class Message implements Serializable {
    public Peer sourcePeer;
    public MessageType messageType;
    public Object content;

    public Message(MessageType messageType, Peer sourcePeer, Object content) {
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
        this.content = content;
    }

    public Message(MessageType messageType, Peer sourcePeer) {
        this.messageType = messageType;
        this.sourcePeer = sourcePeer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(sourcePeer, message.sourcePeer) &&
                messageType == message.messageType &&
                Objects.equals(content, message.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePeer, messageType, content);
    }

    @Override
    public String toString() {
        return "Message{" +
                "sourcePeer=" + sourcePeer +
                ", messageType=" + messageType +
                ", content=" + content +
                '}';
    }

    public enum MessageType {
        GREETING_REQUEST, GREETING_RESPONSE,
        SEARCH_REQUEST, SEARCH_RESPONSE,
        ESTABLISH_TRANSFER_REQUEST, SEND_RATING, ESTABLISH_TRANSFER_RESPONSE
    }
}
