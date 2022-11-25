package connect.message.type;

import connect.message.*;

public class PieceMessage extends ActualMessage {

    private static final long serialVersionUID = 1681414311439118768L;
    private final int port;
    private final int index;

    public PieceMessage(int port, int index) {
        super(9, MessageType.PIECE);
        this.port = port;
        this.index = index;
    }

    public int getPort() {
        return port;
    }

    public int getIndex() {
        return index;
    }
}
