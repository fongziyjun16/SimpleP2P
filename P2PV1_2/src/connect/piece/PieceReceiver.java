package connect.piece;

import config.PeerInfo;
import connect.PeerConnection;

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class PieceReceiver implements Runnable{

    private final PeerConnection peerConnection;
    private final int sendingServerPort;
    private final int index;

    // runtime logger
    private static final Logger logger = Logger.getLogger(PieceSender.class.getName());

    public PieceReceiver(PeerConnection peerConnection, int sendingServerPort, int index) {
        this.peerConnection = peerConnection;
        this.sendingServerPort = sendingServerPort;
        this.index = index;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(PeerInfo.getPeerAddress(peerConnection.getNeighborID()), sendingServerPort);
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());



        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Fail to Receive Piece " + index + " to " + peerConnection.getNeighborID());
        }
    }

}
