package connect.message;

import java.io.Serializable;

public class HandshakeMessage implements Serializable {

    private static final long serialVersionUID = 8813636700808748900L;

    private final String header = "P2PFILESHARINGPROJ";
    private final int peerID;

    public HandshakeMessage(int peerID) {
        this.peerID = peerID;
    }

    public String getHeader() {
        return header;
    }

    public int getPeerID() {
        return peerID;
    }

    @Override
    public String toString() {
        return "HandshakeMessage{" +
                "header='" + header + '\'' +
                ", peerID=" + peerID +
                '}';
    }
}
