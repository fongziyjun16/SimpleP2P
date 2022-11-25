package connect.message.type;

import connect.message.*;

public class UnchokeMessage extends ActualMessage {

    private static final long serialVersionUID = 339291381787968274L;

    public UnchokeMessage() {
        super(1, MessageType.UNCHOKE);
    }
}
