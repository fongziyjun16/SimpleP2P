package connect;

import config.PeerInfo;
import main.*;
import utils.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.*;

public class PeerConnection implements Runnable {

    private final PeerRegister peerRegister;
    private final int selfID;
    private final byte[] selfBitfield;
    private final int neighborID;
    private final byte[] neighborBitfield;
    private final Socket connection;

    private final boolean[] chokeState = {true}; // true -- choke, false unchoke

    // runtime logger
    private static final Logger logger = Logger.getLogger(PeerRegister.class.getName());

    public PeerConnection(PeerRegister peerRegister, int neighborID, Socket connection) {
        this.peerRegister = peerRegister;
        selfID = peerRegister.getSelfID();
        selfBitfield = peerRegister.getSelfBitfield();
        this.neighborID = neighborID;
        neighborBitfield = new byte[BitfieldUtils.bitfieldLength];
        this.connection = connection;
    }

    @Override
    public void run() {
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = connection.getOutputStream()) {
            PeerLogger.receiveConnection(selfID, neighborID);

            if (PeerInfo.doesPeerHaveFile(selfID)) {
                MessageSender.sendSelfBitfield(connection, neighborID, selfBitfield);
                logger.log(Level.INFO, "After handshake send bitfield to neighbor " + neighborID);
            }

            int inputLength;
            byte[] inputBuffer = new byte[1024];
            while ((inputStream.read(inputBuffer)) != -1) {
                // receive length
                byte[] lengthInByte = new byte[4];
                System.arraycopy(inputBuffer, 0, lengthInByte, 0, 4);
                int length = PeerUtils.bytes2Int(lengthInByte);
                logger.log(Level.INFO, "receive message from " + neighborID + " with length " + length);
                outputStream.write(1);

                // receive type
                byte[] typeInByte = new byte[1];
                inputStream.read(typeInByte);
                int type = typeInByte[0];
                logger.log(Level.INFO, "receive message from " + neighborID + " with type " + type);
                outputStream.write(1);

                // receive payload
                if (type == 0) { // choke
                    PeerLogger.choking(selfID, neighborID);
                    synchronized (chokeState) {
                        chokeState[0] = true;
                    }
                    logger.log(Level.INFO, "choke by " + neighborID);
                } else if (type == 1) { // unchoke
                    PeerLogger.unchoking(selfID, neighborID);
                    synchronized (chokeState) {
                        chokeState[0] = false;
                    }
                    int interestedPieceIndex = BitfieldUtils.randomSelectOneInterested(selfBitfield, neighborBitfield);
                    if (interestedPieceIndex != -1) {
                        MessageSender.sendRequest(connection, neighborID, interestedPieceIndex);
                    }
                    logger.log(Level.INFO, "unchoke by " + neighborID);
                } else if (type == 2) { // interested
                    peerRegister.addInterestedNeighbor(neighborID);
                    PeerLogger.interested(selfID, neighborID);
                    logger.log(Level.INFO, "receive interested from " + neighborID);
                } else if (type == 3) { // not interested
                    peerRegister.removeInterestedNeighbor(neighborID);
                    PeerLogger.notInterested(selfID, neighborID);
                    logger.log(Level.INFO, "receive not interested from " + neighborID);
                } else if (type == 4) { // have
                    inputStream.read(inputBuffer);
                    byte[] indexInByte = new byte[4];
                    System.arraycopy(inputBuffer, 0, indexInByte, 0, 4);
                    int index = PeerUtils.bytes2Int(indexInByte);
                    PeerLogger.have(selfID, neighborID, index);
                    BitfieldUtils.received(neighborBitfield, index);
                    if (!BitfieldUtils.exists(selfBitfield, index)) {
                        MessageSender.sendInterested(connection, neighborID);
                    } else {
                        MessageSender.sendNotInterested(connection, neighborID);
                    }
                    logger.log(Level.INFO, "receive have " + index + " from " + neighborID);
                    if (BitfieldUtils.doesHaveCompleteFile(neighborBitfield)) {
                        peerRegister.addCompletedPeer(neighborID);
                    }
                } else if (type == 5) { // bitfield
                    int count = 0;
                    while ((inputLength = inputStream.read(inputBuffer)) != -1) {
                        int index = 0;
                        for (int i = 0; i < inputLength; i++) {
                            neighborBitfield[index++] = inputBuffer[i];
                        }
                        count += inputLength;
                        if (count == length - 1) {
                            break;
                        }
                    }
                    logger.log(Level.INFO, "receive bitfield '" + PeerUtils.bytes2String(neighborBitfield) + "' from " + neighborID);
                    if (BitfieldUtils.isInterested(selfBitfield, neighborBitfield)) {
                        MessageSender.sendInterested(connection, neighborID);
                        logger.log(Level.INFO, "interested in " + neighborID);
                    } else {
                        MessageSender.sendNotInterested(connection, neighborID);
                    }
                } else if (type == 6) { // request
                    inputStream.read(inputBuffer);
                    byte[] indexInByte = new byte[4];
                    System.arraycopy(inputBuffer, 0, indexInByte, 0, 4);
                    int index = PeerUtils.bytes2Int(indexInByte);
                    logger.log(Level.INFO, "receive request type message for piece " + index + " from neighbor " + neighborID);
                    MessageSender.sendHead(connection, 4 + BitfieldUtils.getPieceLength(index), (byte) 7);

                    inputStream.read(inputBuffer);
                    byte[] newPortInByte = new byte[4];
                    System.arraycopy(inputBuffer, 0, newPortInByte, 0, 4);
                    int receivingPort = PeerUtils.bytes2Int(newPortInByte);

                    PeerController.threadPool.submit(new PieceSender(this, receivingPort, index));
                } else if (type == 7) { // piece
                    PeerController.threadPool.submit(new PieceReceiver(this));
                } else { // type == 8
                    // terminate this connection
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.log(Level.INFO, "Close Connection with " + neighborID);
    }

    public PeerRegister getPeerRegister() {
        return peerRegister;
    }

    public int getSelfID() {
        return selfID;
    }

    public byte[] getSelfBitfield() {
        return selfBitfield;
    }

    public int getNeighborID() {
        return neighborID;
    }

    public Socket getConnection() {
        return connection;
    }

    public boolean[] getChokeState() {
        return chokeState;
    }

}

class PieceSender implements Runnable {

    private final PeerConnection peerConnection;
    private final int receivingPort;
    private final int index;

    public PieceSender(PeerConnection peerConnection, int receivingPort, int index) {
        this.peerConnection = peerConnection;
        this.receivingPort = receivingPort;
        this.index = index;
    }

    // runtime logger
    private static final Logger logger = Logger.getLogger(PieceSender.class.getName());

    @Override
    public void run() {
        try(Socket socket = new Socket(peerConnection.getConnection().getInetAddress().getHostAddress(), receivingPort);
            RandomAccessFile targetFile = new RandomAccessFile(
                PeerUtils.getTargetFilename(peerConnection.getSelfID()), "r")) {

            int inputLength = 0;
            byte[] inputBuffer = new byte[1024];
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            // send index
            outputStream.write(PeerUtils.int2Bytes(index));
            logger.log(Level.INFO, "be ready to send piece " + index + " to neighbor " + peerConnection.getNeighborID());
            inputStream.read(inputBuffer);

            // send content
            int startPtr = index * BitfieldUtils.pieceSize;
            int remainingContentLength = BitfieldUtils.getPieceLength(index);
            int readLength = 0;
            byte[] readBuffer = new byte[1024];
            targetFile.seek(startPtr);
            while ((readLength = targetFile.read(readBuffer)) != -1) {
                synchronized (peerConnection.getChokeState()) {
                    if (peerConnection.getChokeState()[0]) {
                        logger.log(Level.INFO, "Being Choked, stop sending");
                        break;
                    }
                }
                if (remainingContentLength > readLength) {
                    outputStream.write(readBuffer);
                } else {
                    outputStream.write(readBuffer, 0, remainingContentLength);
                }
                remainingContentLength -= readLength;
            }
            logger.log(Level.INFO, "sent piece " + index + " data to " + peerConnection.getNeighborID());
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Disconnected");
        }
    }

}

class PieceReceiver implements Runnable {

    private final PeerConnection peerConnection;

    public PieceReceiver(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    // runtime logger
    private static final Logger logger = Logger.getLogger(PieceReceiver.class.getName());

    @Override
    public void run() {
        try (ServerSocket receivingServer = new ServerSocket(0);
             RandomAccessFile targetFile = new RandomAccessFile(
                     PeerUtils.getTargetFilename(peerConnection.getSelfID()), "rw")) {
            peerConnection.getConnection().getOutputStream().write(PeerUtils.int2Bytes(receivingServer.getLocalPort()));
            // accept Socket
            Socket receiver = receivingServer.accept();

            int inputLength = 0;
            byte[] inputBuffer = new byte[1024];
            InputStream inputStream = receiver.getInputStream();
            OutputStream outputStream = receiver.getOutputStream();

            // receive index
            inputStream.read(inputBuffer);
            byte[] indexInByte = new byte[4];
            System.arraycopy(inputBuffer, 0, indexInByte, 0, 4);
            int index = PeerUtils.bytes2Int(indexInByte);
            logger.log(Level.INFO, "be ready to receive piece " + index + " from neighbor " + peerConnection.getNeighborID());
            outputStream.write(1);

            // receive content
            int startPtr = index * BitfieldUtils.pieceSize;
            int remainingContentLength = BitfieldUtils.getPieceLength(index);
            int readLength = 0;
            byte[] readBuffer = new byte[1024];
            targetFile.seek(startPtr);
            synchronized (PeerController.targetFileSegmentLock[index]) {
                while ((readLength = inputStream.read(readBuffer)) != -1) {
                    synchronized (peerConnection.getChokeState()) {
                        if (peerConnection.getChokeState()[0]) {
                            logger.log(Level.INFO, "Being Choked, stop sending");
                            break;
                        }
                    }
                    peerConnection.getPeerRegister().addDownloadedCapacity(peerConnection.getNeighborID(), readLength);
                    if (remainingContentLength > readLength) {
                        targetFile.write(readBuffer);
                    } else {
                        targetFile.write(readBuffer, 0, remainingContentLength);
                    }
                    remainingContentLength -= readLength;
                }
                logger.log(Level.INFO, "received piece " + index + " data from " + peerConnection.getNeighborID());
            }

            if (remainingContentLength == 0) {
                BitfieldUtils.received(peerConnection.getSelfBitfield(), index);
                peerConnection.getPeerRegister().sendHave(index);
                PeerLogger.downloadingOnePiece(
                        peerConnection.getSelfID(), peerConnection.getNeighborID(),
                        index, BitfieldUtils.numberOfPiecesHaving(peerConnection.getSelfBitfield()));
                if (BitfieldUtils.doesHaveCompleteFile(peerConnection.getSelfBitfield())) {
                    peerConnection.getPeerRegister().addCompletedPeer(peerConnection.getSelfID());
                    PeerLogger.completionOfDownload(peerConnection.getSelfID());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Disconnected");
        }

    }

}
