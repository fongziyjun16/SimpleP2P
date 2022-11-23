package dto;

import java.io.Serializable;

public class HandshakeMessage implements Serializable {

    private static final long serialVersionUID = -3707105415890306567L;

    private final String header = "P2PFILESHARINGPROJ";
    private final int peerID;

    public HandshakeMessage(int peerID) {
        this.peerID = peerID;
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
