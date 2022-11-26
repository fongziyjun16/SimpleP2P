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
            int contentLength = index == BitfieldUtils.pieceNumber - 1 ? (Common.fileSize - Common.pieceSize * index): Common.pieceSize;
            int receivedLength = 0;

            int inputLength;
            byte[] inputBuffer = new byte[1024];
            outputStream.write(1);

            while ((inputLength = inputStream.read(inputBuffer)) != -1) {
                synchronized (peerConnection.getChokeState()) {
                    if (peerConnection.getChokeState()[0]) {
                        break;
                    }
                }
                targetFile.write(inputBuffer, 0, inputLength);
                receivedLength += inputLength;
                peerConnection.addDownloadedCapacity(inputLength);
            }

            if (receivedLength == inputLength) {
                peerConnection.receivedPiece(index);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Fail to Receive Piece " + index + " to " + peerConnection.getNeighborID());
        }
    }

}
