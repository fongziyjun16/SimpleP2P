package main.piece;

import config.Common;
import config.PeerInfo;
import main.*;
import utils.BitfieldUtils;
import utils.PeerUtils;

import java.io.*;
import java.net.Socket;
import java.util.logging.*;

public class PieceReceiver implements Runnable {

    private final PeerHandler peerHandler;
    private final PeerConnection peerConnection;
    private final int sendingServerPort;
    private final int index;

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PieceReceiver.class.getName());

    public PieceReceiver(PeerHandler peerHandler, PeerConnection peerConnection, int sendingServerPort, int index) {
        this.peerHandler = peerHandler;
        this.peerConnection = peerConnection;
        this.sendingServerPort = sendingServerPort;
        this.index = index;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(PeerInfo.getPeerAddress(peerConnection.getNeighborID()), sendingServerPort);
             RandomAccessFile targetFile = new RandomAccessFile(PeerUtils.getTargetFilename(peerHandler.getSelfID()), "rw")) {
            InputStream inputStream = socket.getInputStream();

            targetFile.seek((long) index * Common.pieceSize);
            int contentLength = BitfieldUtils.getContentLength(index);

            int inputLength;
            byte[] inputBuffer = new byte[1024];

            logger.log(Level.INFO, "Ready to Receive " + index + " with length " + contentLength + " from neighbor " + peerConnection.getNeighborID());
            peerHandler.getTargetFileSegmentLocks().get(index).lock();
            while ((inputLength = inputStream.read(inputBuffer)) != -1 && contentLength != 0) {
                targetFile.write(inputBuffer, 0, inputLength);
                contentLength -= inputLength;
                peerConnection.addDownloadedBytes(inputLength);
            }
            peerHandler.getTargetFileSegmentLocks().get(index).unlock();

            if (contentLength == 0) {
                peerConnection.receivePiece(index);
                logger.log(Level.INFO, "Successfully Receive " + index + " from neighbor " + peerConnection.getNeighborID());
            } else {
                logger.log(Level.SEVERE, "Fail to Receive " + index + " from neighbor " + peerConnection.getNeighborID());
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Fail to Receive Piece " + index + " to " + peerConnection.getNeighborID());
        }
    }

}
