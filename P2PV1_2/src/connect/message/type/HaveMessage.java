package connect.message.type;

import connect.message.*;

public class HaveMessage extends ActualMessage {

    private static final long serialVersionUID = 6597590541674923847L;
    private final int index;

    public HaveMessage(int index) {
        super(5, MessageType.HAVE);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
