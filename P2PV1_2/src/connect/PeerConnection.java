package connect;

import config.PeerInfo;
import connect.message.type.*;
import connect.piece.PieceReceiver;
import connect.piece.PieceSender;
import main.PeerController;
import main.PeerLogger;
import utils.*;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class PeerConnection implements Runnable{

    private final PeerRegister peerRegister;

    private final int selfID;
    private final byte[] selfBitfield;

    private final int neighborID;
    private byte[] neighborBitfield;

    private final boolean[] chokeState = {true}; // true -- choke, false unchoke

    private final Socket socket;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;

    // runtime logger
    private static final Logger logger = Logger.getLogger(PeerRegister.class.getName());

    public PeerConnection(PeerRegister peerRegister, int neighborID,
                          Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws IOException {
        this.peerRegister = peerRegister;
        selfID = peerRegister.getSelfID();
        selfBitfield = peerRegister.getSelfBitfield();
        this.neighborID = neighborID;
        this.socket = connection;
        this.ois = ois;
        this.oos = oos;
    }

    @Override
    public void run() {
        try {
            PeerLogger.receiveConnection(selfID, neighborID);

            if (PeerInfo.doesPeerHaveFile(selfID)) {
                oos.writeObject(new BitfieldMessage(selfBitfield));
                logger.log(Level.INFO, "After handshake send bitfield to neighbor " + neighborID);
            }

            while (true) {
                Object receivedMessage = ois.readObject();
                if (receivedMessage instanceof ChokeMessage) {
                    synchronized (chokeState) {
                        chokeState[0] = true;
                    }
                    PeerLogger.choking(selfID, neighborID);
                    logger.log(Level.INFO, "Receive Choke from neighbor " + neighborID);
                } else if (receivedMessage instanceof UnchokeMessage) {
                    synchronized (chokeState) {
                        chokeState[0] = false;
                    }
                    int interestedIndex = BitfieldUtils.randomSelectOneInterested(selfBitfield, neighborBitfield);
                    if (interestedIndex != -1) {
                        synchronized (this) {
                            oos.writeObject(new RequestMessage(interestedIndex));
                        }
                    }
                    PeerLogger.unchoking(selfID, neighborID);
                    logger.log(Level.INFO, "Receive Unchoke from neighbor " + neighborID);
                } else if (receivedMessage instanceof InterestedMessage) {
                    peerRegister.addInterested(neighborID);
                    PeerLogger.interested(selfID, neighborID);
                    logger.log(Level.INFO, "Receive Interested from neighbor " + neighborID);
                } else if (receivedMessage instanceof NotInterestedMessage) {
                    peerRegister.removeInterested(neighborID);
                    PeerLogger.notInterested(selfID, neighborID);
                    logger.log(Level.INFO, "Receive Not Interested from neighbor " + neighborID);
                } else if (receivedMessage instanceof HaveMessage) {
                    HaveMessage haveMessage = (HaveMessage) receivedMessage;
                    BitfieldUtils.received(neighborBitfield, haveMessage.getIndex());
                    if (BitfieldUtils.doesHaveCompleteFile(neighborBitfield)) {
                        peerRegister.addCompletedPeer(neighborID);
                    }
                    PeerLogger.have(selfID, neighborID, haveMessage.getIndex());
                    logger.log(Level.INFO, "Receive Not Interested from neighbor " + neighborID);
                } else if (receivedMessage instanceof BitfieldMessage) {
                    BitfieldMessage bitfieldMessage = (BitfieldMessage) receivedMessage;
                    neighborBitfield = bitfieldMessage.getBitfield();
                    synchronized (this) {
                        if (BitfieldUtils.isInterested(selfBitfield, neighborBitfield)) {
                            oos.writeObject(new InterestedMessage());
                        } else {
                            oos.writeObject(new NotInterestedMessage());
                        }
                    }
                    logger.log(Level.INFO, "Receive Bitfield from neighbor " + neighborID);
                } else if (receivedMessage instanceof RequestMessage) {
                    RequestMessage requestMessage = (RequestMessage) receivedMessage;
                    int index = requestMessage.getIndex();
                    try {
                        ServerSocket sendingPieceServer = new ServerSocket(0);
                        PeerController.threadPool.submit(new PieceSender(this, index, sendingPieceServer));
                        synchronized (this) {
                            oos.writeObject(new PieceMessage(sendingPieceServer.getLocalPort(), index));
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Fail to Send Request to neighbor " + neighborID);
                    }
                    logger.log(Level.INFO, "Receive Request from neighbor " + neighborID);
                } else if (receivedMessage instanceof PieceMessage) {
                    PieceMessage pieceMessage = (PieceMessage) receivedMessage;
                    PeerController.threadPool.submit(
                            new PieceReceiver(this, pieceMessage.getPort(), pieceMessage.getIndex()));
                    logger.log(Level.INFO, "Receive Piece from neighbor " + neighborID);
                } else if (receivedMessage instanceof EndMessage) {
                    socket.close();
                    logger.log(Level.INFO, "Receive End from neighbor " + neighborID);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.log(Level.INFO, "Close Connection with " + neighborID);
    }

    public void receivedPiece(int index) {
        BitfieldUtils.received(selfBitfield, index);
        PeerLogger.downloadingOnePiece(selfID, neighborID, index,
                BitfieldUtils.numberOfPiecesHaving(selfBitfield));
        peerRegister.sendHave(index);
        if (BitfieldUtils.doesHaveCompleteFile(selfBitfield)) {
            peerRegister.addCompletedPeer(selfID);
        } else {
            synchronized (chokeState) {
                if (!chokeState[0]) {
                    try {
                        oos.writeObject(new RequestMessage(BitfieldUtils.randomSelectOneInterested(selfBitfield, neighborBitfield)));
                    } catch (Exception e) {
                        logger.log(Level.INFO, "Fail to send Request to neighbor " + neighborID);
                    }
                }
            }
        }
    }

    public void addDownloadedCapacity(int capacity) {
        peerRegister.addDownloadedCapacity(neighborID, capacity);
    }

    public int getSelfID() {
        return selfID;
    }

    public int getNeighborID() {
        return neighborID;
    }

    public boolean[] getChokeState() {
        return chokeState;
    }

    public ObjectOutputStream getOOS() {
        return oos;
    }
}
