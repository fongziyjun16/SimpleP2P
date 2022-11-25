package connect;

import config.*;
import connect.message.HandshakeMessage;
import main.*;
import utils.PeerUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class PeerRegister {

    private final int selfID;
    private final byte[] selfBitfield;

    private final Map<Integer, PeerConnection> connectedNeighbors = new HashMap<>();

    private final PeerSelector peerSelector;

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerRegister.class.getName());

    public PeerRegister(int selfID, byte[] selfBitfield) {
        this.selfID = selfID;
        this.selfBitfield = selfBitfield;
        buildConnection();
        peerSelector = new PeerSelector(this);
    }

    private void buildConnection() {
        buildNegativeConnection();
        if (!PeerInfo.doesPeerHaveFile(selfID)) {
            buildPositiveConnection();
        }
    }

    private void buildNegativeConnection() {
        PeerController.threadPool.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PeerInfo.getPeerPort(selfID))) {
                while (true) {
                    Socket connection = serverSocket.accept();
                    try {
                        connectionInitialize(connection, false);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Fail to connect to " +
                                PeerUtils.getConnectionWholeAddress(connection) +
                                ". Exception Message: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Fail to build negative connection server");
            }
        });
    }

    private void buildPositiveConnection() {
        for (Integer neighborID : PeerInfo.getPeerIDs()) {
            if (neighborID != selfID) {
                String neighborAddress = PeerInfo.getPeerAddress(neighborID);
                int neighborPort = PeerInfo.getPeerPort(neighborID);
                PeerController.threadPool.submit(() -> {
                    while (true) {
                        try {
                            connectionInitialize(new Socket(neighborAddress, neighborPort), true);
                            break;
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Fail to connect to " +
                                    neighborAddress + ":" + neighborPort + " Try after 2s. " +
                                    "Exception Message: " + e.getMessage());
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                        }
                    }
                });
            }
        }
    }

    private void connectionInitialize(Socket connection, boolean isPositive) throws Exception {
        connection.setKeepAlive(true);
        int neighborID = handShake(connection, isPositive);
        synchronized (connectedNeighbors) {
            if (!connectedNeighbors.containsKey(neighborID)) {
                PeerConnection peerConnection = new PeerConnection(this, neighborID, connection);
                connectedNeighbors.put(neighborID, peerConnection);
                // peerSelector.downloadRegister(neighborID);
                PeerLogger.makeConnection(selfID, neighborID);
                // PeerController.threadPool.submit(peerConnection);
                logger.log(Level.INFO, "Make Connection with " + neighborID);
            }
        }
    }

    private int handShake(Socket connection, boolean isPositive) throws Exception {
        // receive message & extract neighbor id
        try {
            ObjectInputStream ois = null;
            ObjectOutputStream oos = null;
            if (isPositive) {
                oos = new ObjectOutputStream(connection.getOutputStream());
                ois = new ObjectInputStream(connection.getInputStream());
            } else {
                ois = new ObjectInputStream(connection.getInputStream());
                oos = new ObjectOutputStream(connection.getOutputStream());
            }

            // send handshake
            HandshakeMessage sentHandshakeMessage = new HandshakeMessage(selfID);
            oos.writeObject(sentHandshakeMessage);

            Object receivedMessage = ois.readObject();
            if (receivedMessage instanceof HandshakeMessage) {
                HandshakeMessage receivedHandshakeMessage = (HandshakeMessage) receivedMessage;
                return receivedHandshakeMessage.getPeerID();
            } else {
                throw new RuntimeException("The type of received message is not 'HandshakeMessage'");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to receive handshake message from " +
                    PeerUtils.getConnectionWholeAddress(connection));
            throw e;
        }
    }

    public void addInterested(int neighborID) {
        peerSelector.addInterested(neighborID);
    }

    public void removeInterested(int neighborID) {
        peerSelector.removeInterested(neighborID);
    }

    public int getSelfID() {
        return selfID;
    }

    public byte[] getSelfBitfield() {
        return selfBitfield;
    }
}
