package main.message;

import java.io.Serializable;

public class ActualMessage implements Serializable {

    private static final long serialVersionUID = -7308289941256116397L;

    private final int messageLength;
    private final MessageType messageType;

    public ActualMessage(int messageLength, MessageType messageType) {
        this.messageLength = messageLength;
        this.messageType = messageType;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public MessageType getMessageType() {
        return messageType;
    }
}
