package main.message.type;

import main.message.ActualMessage;
import main.message.MessageType;

public class EndMessage extends ActualMessage {

    private static final long serialVersionUID = -9183393183776356034L;

    public EndMessage() {
        super(1, MessageType.END);
    }

}
