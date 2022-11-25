package connect.message.type;

import connect.message.*;

public class NotInterestedMessage extends ActualMessage {

    private static final long serialVersionUID = 1700184645966244376L;

    public NotInterestedMessage() {
        super(1, MessageType.NOT_INTERESTED);
    }
}
