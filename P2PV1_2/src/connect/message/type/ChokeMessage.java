package connect.message.type;

import connect.message.*;

public class ChokeMessage extends ActualMessage {

    private static final long serialVersionUID = -839302884086436313L;

    public ChokeMessage() {
        super(1, MessageType.CHOKE);
    }

}
