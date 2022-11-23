package dto;

import java.io.Serializable;

public class ActualMessage implements Serializable {

    private static final long serialVersionUID = -3512979772184191427L;

    private final int messageLength;
    private final int messageType;
    private final byte[] payload;

    public ActualMessage(int messageLength, int messageType, byte[] payload) {
        this.messageLength = messageLength;
        this.messageType = messageType;
        this.payload = payload;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public int getMessageType() {
        return messageType;
    }

    public byte[] getPayload() {
        return payload;
    }
}
