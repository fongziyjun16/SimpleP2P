package connect.message;

import java.io.Serializable;

public class HandShakeMessage implements Serializable {

    private static final long serialVersionUID = 8813636700808748900L;

    private final int peerID;

    public HandShakeMessage(int peerID) {
        this.peerID = peerID;
    }

    public int getPeerID() {
        return peerID;
    }

}
