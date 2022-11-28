package connect.piece;

import config.Common;
import connect.PeerConnection;
import main.PeerController;
import utils.BitfieldUtils;
import utils.PeerUtils;

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
        try (RandomAccessFile targetFile = new RandomAccessFile(PeerUtils.getTargetFilename(peerConnection.getSelfID()), "r")) {
            Socket socket = sendingPieceServer.accept();
            OutputStream outputStream = socket.getOutputStream();

            targetFile.seek((long) index * Common.pieceSize);
            int contentLength = BitfieldUtils.getContentLength(index);

            long readLength = 0;
            byte[] readBuffer = new byte[1024];
            logger.log(Level.INFO, "Ready to Send " + index + " with length " + contentLength + " to neighbor " + peerConnection.getNeighborID());
            synchronized (PeerController.targetFileSegmentLocks.get(index)) {
                while ((readLength = targetFile.read(readBuffer)) != -1 && contentLength != 0) {
                    synchronized (peerConnection.getChokeState()) {
                        if (peerConnection.getChokeState()[0]) {
                            break;
                        }
                    }
                    if (contentLength > readLength) {
                        outputStream.write(readBuffer);
                        contentLength -= readLength;
                    } else {
                        outputStream.write(readBuffer, 0, contentLength);
                        contentLength = 0;
                    }
                }
                outputStream.flush();
            }

            socket.close();

            if (contentLength == 0) {
                logger.log(Level.INFO, "Successfully Send " + index + " to neighbor " + peerConnection.getNeighborID());
            } else {
                logger.log(Level.INFO, "Interrupted Sending " + index + " to neighbor " + peerConnection.getNeighborID());
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Fail to Send Piece " + index + " to " + peerConnection.getNeighborID());
        }
    }

}
