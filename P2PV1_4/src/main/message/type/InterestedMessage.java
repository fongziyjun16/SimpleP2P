package main.message.type;

import main.message.*;

public class InterestedMessage extends ActualMessage {

    private static final long serialVersionUID = -664713051576539007L;

    public InterestedMessage() {
        super(1, MessageType.INTERESTED);
    }

}
