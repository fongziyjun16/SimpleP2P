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

    private final Socket socket;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;

    private final boolean[] chokeState = {true}; // true -- choke, false unchoke

    // runtime logger
    private static final Logger logger = Logger.getLogger(PeerRegister.class.getName());

    public PeerConnection(PeerRegister peerRegister, int neighborID, Socket connection) throws IOException {
        this.peerRegister = peerRegister;
        selfID = peerRegister.getSelfID();
        selfBitfield = peerRegister.getSelfBitfield();
        this.neighborID = neighborID;
        this.socket = connection;
        ois = new ObjectInputStream(socket.getInputStream());
        oos = new ObjectOutputStream(socket.getOutputStream());
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
                        chokeState[0] = true;
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
                } else if (receivedMessage instanceof PieceMessage) {
                    PieceMessage pieceMessage = (PieceMessage) receivedMessage;
                    PeerController.threadPool.submit(
                            new PieceReceiver(this, pieceMessage.getPort(), pieceMessage.getIndex()));
                }
                // close connection
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.log(Level.INFO, "Close Connection with " + neighborID);
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

    public Socket getSocket() {
        return socket;
    }
}
