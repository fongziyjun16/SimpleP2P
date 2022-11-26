package connect.piece;

import config.Common;
import config.PeerInfo;
import connect.PeerConnection;
import main.PeerLogger;
import utils.BitfieldUtils;
import utils.PeerUtils;

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
        try (Socket socket = new Socket(PeerInfo.getPeerAddress(peerConnection.getNeighborID()), sendingServerPort);
             RandomAccessFile targetFile = new RandomAccessFile(PeerUtils.getTargetFilename(peerConnection.getSelfID()), "rw")) {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            targetFile.seek((long) index * Common.pieceSize);
            int contentLength = index == BitfieldUtils.pieceNumber - 1 ?
                    (Common.fileSize - Common.pieceSize * index): Common.pieceSize;

            int inputLength;
            byte[] inputBuffer = new byte[1024];
            outputStream.write(1);

            logger.log(Level.INFO, "Ready to Receive " + index + " from neighbor " + peerConnection.getNeighborID());
            while ((inputLength = inputStream.read(inputBuffer)) != -1 && contentLength != 0) {
                targetFile.write(inputBuffer, 0, inputLength);
                contentLength -= inputLength;
                peerConnection.addDownloadedCapacity(inputLength);
            }

            logger.log(Level.INFO, "contentLength is " + contentLength);
            if (contentLength == 0) {
                peerConnection.receivedPiece(index);
                logger.log(Level.INFO, "Successfully Receive " + index + " from neighbor " + peerConnection.getNeighborID());
            } else {
                logger.log(Level.SEVERE, "Fail to Receive " + index + " from neighbor " + peerConnection.getNeighborID());
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Fail to Receive Piece " + index + " to " + peerConnection.getNeighborID());
        }
    }

}
