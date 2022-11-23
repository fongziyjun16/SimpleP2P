package connect.message;

public class HandShakeMessage {

    private final int peerID;

    public HandShakeMessage(int peerID) {
        this.peerID = peerID;
    }

    public int getPeerID() {
        return peerID;
    }

    
}
