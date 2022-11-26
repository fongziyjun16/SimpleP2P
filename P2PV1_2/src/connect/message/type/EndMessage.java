package connect.message.type;

import connect.message.*;

public class EndMessage extends ActualMessage {

    private static final long serialVersionUID = -8790628380694445498L;

    public EndMessage() {
        super(1, MessageType.END);
    }
}
