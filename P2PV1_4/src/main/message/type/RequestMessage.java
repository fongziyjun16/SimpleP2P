package main.message.type;

import main.message.*;

public class RequestMessage extends ActualMessage {

    private static final long serialVersionUID = -8359101015660234097L;
    private final int index;

    public RequestMessage(int index) {
        super(5, MessageType.REQUEST);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
