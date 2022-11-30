package main;

import main.message.ActualMessage;
import main.message.type.*;
import main.piece.*;
import utils.BitfieldUtils;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

public class PeerConnection implements Runnable {

    private final PeerHandler peerHandler;

    private final PeerHub peerHub;

    private final int neighborID;
    private final byte[] neighborBitfield = new byte[BitfieldUtils.bitfieldLength];
    private final ReentrantReadWriteLock neighborBitfieldLock = new ReentrantReadWriteLock();

    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;
    private final ReentrantLock oosLock = new ReentrantLock();

    private final boolean[] chokeState = {true}; // true -- choke, false unchoke

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerConnection.class.getName());

    public PeerConnection(PeerHandler peerHandler, PeerHub peerHub, int neighborID, ObjectInputStream ois, ObjectOutputStream oos) {
        this.peerHandler = peerHandler;
        this.peerHub = peerHub;
        this.neighborID = neighborID;
        this.ois = ois;
        this.oos = oos;
    }

    @Override
    public void run() {
        try {
            logger.log(Level.INFO, "Start to Process Message with Neighbor " + neighborID);

            peerHandler.getSelfBitfieldLock().readLock().lock();
            if (BitfieldUtils.numberOfPiecesHaving(peerHandler.getSelfBitfield()) > 0) {
                sendMessage(new BitfieldMessage(peerHandler.getSelfBitfield()));
                logger.log(Level.INFO, "After Handshake, Send Bitfield to Neighbor " + neighborID);
            } else {
                logger.log(Level.INFO, "After Handshake, No Need to Send Bitfield to Neighbor " + neighborID);
            }
            peerHandler.getSelfBitfieldLock().readLock().unlock();

            while (true) {
                Object receivedMessage = ois.readObject();
                logger.log(Level.INFO, "Receive Message from " + neighborID);
                if (receivedMessage instanceof ChokeMessage) {
                    synchronized (chokeState) {
                        chokeState[0] = true;
                    }

                    PeerLogger.choking(peerHandler.getSelfID(), neighborID);
                    logger.log(Level.INFO, "Receive Choke from neighbor " + neighborID);
                } else if (receivedMessage instanceof UnchokeMessage) {
                    synchronized (chokeState) {
                        chokeState[0] = false;
                    }
                    peerHandler.getSelfBitfieldLock().readLock().lock();
                    neighborBitfieldLock.readLock().lock();
                    int interestedIndex = BitfieldUtils.randomSelectOneInterested(peerHandler.getSelfBitfield(), neighborBitfield);
                    neighborBitfieldLock.readLock().unlock();
                    peerHandler.getSelfBitfieldLock().readLock().unlock();

                    if (interestedIndex != -1) {
                        sendMessage(new RequestMessage(interestedIndex));
                    }

                    PeerLogger.unchoking(peerHandler.getSelfID(), neighborID);
                    logger.log(Level.INFO, "Receive Unchoke from neighbor " + neighborID);
                } else if (receivedMessage instanceof InterestedMessage) {
                    peerHub.addInterested(neighborID);

                    PeerLogger.interested(peerHandler.getSelfID(), neighborID);
                    logger.log(Level.INFO, "Receive Interested from neighbor " + neighborID);
                } else if (receivedMessage instanceof NotInterestedMessage) {
                    peerHub.removeInterested(neighborID);

                    PeerLogger.notInterested(peerHandler.getSelfID(), neighborID);
                    logger.log(Level.INFO, "Receive Not Interested from neighbor " + neighborID);
                } else if (receivedMessage instanceof HaveMessage) {
                    HaveMessage haveMessage = (HaveMessage) receivedMessage;

                    neighborBitfieldLock.writeLock().lock();
                    BitfieldUtils.received(neighborBitfield, haveMessage.getIndex());
                    neighborBitfieldLock.writeLock().unlock();

                    neighborBitfieldLock.readLock().lock();
                    if (BitfieldUtils.doesHaveCompleteFile(neighborBitfield)) {
                        peerHub.addFinishedPeer(neighborID);
                    }
                    neighborBitfieldLock.readLock().unlock();

                    peerHandler.getSelfBitfieldLock().readLock().lock();
                    if (BitfieldUtils.isInterested(peerHandler.getSelfBitfield(), haveMessage.getIndex())) {
                        sendMessage(new InterestedMessage());
                    }
                    peerHandler.getSelfBitfieldLock().readLock().unlock();

                    PeerLogger.have(peerHandler.getSelfID(), neighborID, haveMessage.getIndex());
                    logger.log(Level.INFO, "Receive Have from neighbor " + neighborID);
                } else if (receivedMessage instanceof BitfieldMessage) {
                    BitfieldMessage bitfieldMessage = (BitfieldMessage) receivedMessage;

                    logger.log(Level.INFO, "001 Read Read Read Neighbor " + neighborID);
                    neighborBitfieldLock.writeLock().lock();
                    for (int i = 0; i < neighborBitfield.length; i++) {
                        neighborBitfield[i] = bitfieldMessage.getBitfield()[i];
                    }
                    neighborBitfieldLock.writeLock().unlock();

                    logger.log(Level.INFO, "002 Read Read Read Neighbor " + neighborID);

                    peerHandler.getSelfBitfieldLock().readLock().lock();
                    neighborBitfieldLock.readLock().lock();

                    logger.log(Level.INFO, "003 Read Read Read Neighbor " + neighborID);

                    if (BitfieldUtils.doesHaveCompleteFile(neighborBitfield)) {
                        peerHub.addFinishedPeer(neighborID);
                    }
                    if (BitfieldUtils.isInterested(peerHandler.getSelfBitfield(), neighborBitfield)) {
                        sendMessage(new InterestedMessage());
                    } else {
                        sendMessage(new NotInterestedMessage());
                    }
                    neighborBitfieldLock.readLock().unlock();
                    peerHandler.getSelfBitfieldLock().readLock().unlock();

                    logger.log(Level.INFO, "Receive Bitfield from Neighbor " + neighborID);
                } else if (receivedMessage instanceof RequestMessage) {
                    RequestMessage requestMessage = (RequestMessage) receivedMessage;
                    int index = requestMessage.getIndex();
                    try {
                        ServerSocket sendingPieceServer = new ServerSocket(0);
                        peerHandler.getThreadPool().submit(new PieceSender(peerHandler, this, index, sendingPieceServer));
                        sendMessage(new PieceMessage(sendingPieceServer.getLocalPort(), index));
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Fail to build PieceSender Server ");
                    }
                    logger.log(Level.INFO, "Receive Request Piece " + index + " from neighbor " + neighborID);
                } else if (receivedMessage instanceof PieceMessage) {
                    PieceMessage pieceMessage = (PieceMessage) receivedMessage;
                    if (pieceMessage.getIndex() == -1) {
                        System.out.println();
                    }
                    peerHandler.getThreadPool().submit(new PieceReceiver(peerHandler, this,
                            pieceMessage.getPort(), pieceMessage.getIndex()));
                    logger.log(Level.INFO, "Receive Piece from neighbor " + neighborID);
                } else if (receivedMessage instanceof EndMessage) {
                    peerHub.addFinishedPeer(neighborID);
                    logger.log(Level.INFO, "Receive End from neighbor " + neighborID);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.log(Level.INFO, "Close Connection with " + neighborID);
    }

    private void sendMessage(ActualMessage message) {
        oosLock.lock();
        try {
            logger.log(Level.INFO, "Send out Message Type " + message.getMessageType().getName() + " to neighbor " + neighborID);
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Fail to Send " + message.getMessageType().getName() + " Message to Neighbor " + neighborID);
        }
        oosLock.unlock();
    }

    public void sendChokeMessage() {
        synchronized (chokeState) {
            if (!chokeState[0]) {
                chokeState[0] = true;
                sendMessage(new ChokeMessage());
            }
        }
    }

    public void sendUnchokeMessage() {
        synchronized (chokeState) {
            if (chokeState[0]) {
                chokeState[0] = false;
                sendMessage(new UnchokeMessage());
            }
        }
    }

    public void sendNotInterestedMessage() {
        sendMessage(new NotInterestedMessage());
    }

    public void addDownloadedBytes(int byteNumber) {
        peerHub.addDownloadedBytes(neighborID, byteNumber);
    }

    public void receivePiece(int index) {
        peerHandler.getSelfBitfieldLock().writeLock().lock();
        BitfieldUtils.received(peerHandler.getSelfBitfield(), index);
        peerHandler.getSelfBitfieldLock().writeLock().unlock();

        peerHandler.getSelfBitfieldLock().readLock().lock();
        neighborBitfieldLock.readLock().lock();

        PeerLogger.downloadingOnePiece(peerHandler.getSelfID(), neighborID, index,
                BitfieldUtils.numberOfPiecesHaving(peerHandler.getSelfBitfield()));

        peerHub.sendHaveMessage(index);

        if (BitfieldUtils.doesHaveCompleteFile(peerHandler.getSelfBitfield())) {
            peerHub.addFinishedPeer(peerHandler.getSelfID());
            peerHub.sendNotInterestedMessage();
        } else {
            if (!BitfieldUtils.isInterested(peerHandler.getSelfBitfield(), neighborBitfield)) {
                sendMessage(new NotInterestedMessage());
            } else {
                synchronized (chokeState) {
                    if (!chokeState[0]) {
                        int randomInterestedIndex = BitfieldUtils.randomSelectOneInterested(peerHandler.getSelfBitfield(), neighborBitfield);
                        if (randomInterestedIndex != -1) {
                            sendMessage(new RequestMessage(randomInterestedIndex));
                        }
                    }
                }
            }
        }

        neighborBitfieldLock.readLock().unlock();
        peerHandler.getSelfBitfieldLock().readLock().unlock();
    }

    public void sendHaveMessage(int index) {
        sendMessage(new HaveMessage(index));
    }

    public void sendEndMessage() {
        sendMessage(new EndMessage());
    }

    public int getNeighborID() {
        return neighborID;
    }

    public boolean[] getChokeState() {
        return chokeState;
    }
}
