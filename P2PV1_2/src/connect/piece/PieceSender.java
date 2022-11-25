package connect.piece;

import connect.PeerConnection;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class PieceSender implements Runnable{

    private final PeerConnection peerConnection;
    private final int index;
    private final ServerSocket sendingPieceServer;

    // runtime logger
    private static final Logger logger = Logger.getLogger(PieceSender.class.getName());

    public PieceSender(PeerConnection peerConnection, int index, ServerSocket sendingPieceServer) {
        this.peerConnection = peerConnection;
        this.index = index;
        this.sendingPieceServer = sendingPieceServer;
    }

    @Override
    public void run() {
        try {
            Socket socket = sendingPieceServer.accept();
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            

        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Fail to Send Piece " + index + " to " + peerConnection.getNeighborID());
        }
    }

}
