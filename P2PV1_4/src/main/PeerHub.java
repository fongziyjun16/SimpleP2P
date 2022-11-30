package main;

import config.PeerInfo;
import main.message.HandshakeMessage;
import utils.PeerUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class PeerHub {

    private final PeerHandler peerHandler;

    private final Map<Integer, ObjectInputStream> inMap = new HashMap<>();
    private final Map<Integer, ObjectOutputStream> outMap = new HashMap<>();
    private final Map<Integer, PeerConnection> connectedNeighbors = new HashMap<>();
    private final Set<Integer> finishedPeers = new HashSet<>();

    private final PeerSelector peerSelector;

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerHub.class.getName());

    public PeerHub(PeerHandler peerHandler) {
        this.peerHandler = peerHandler;

        if (PeerInfo.doesPeerHaveFile(peerHandler.getSelfID())) {
            addFinishedPeer(peerHandler.getSelfID());
        }

        buildConnections();

        peerSelector = new PeerSelector(peerHandler, this);
    }

    private void buildConnections() {
        buildNegativeConnections();
        buildPositiveConnections();
    }

    private void buildNegativeConnections() {
        peerHandler.getThreadPool().submit(() -> {
            try (ServerSocket server = new ServerSocket(PeerInfo.getPeerPort(peerHandler.getSelfID()));) {
                while (true) {
                    Socket connection = server.accept();
                    try {
                        buildConnection(connection, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.log(Level.WARNING, "Fail to connect to " +
                                PeerUtils.getConnectionWholeAddress(connection) +
                                ". Exception Message: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "Fail to build negative connection server");
                System.exit(1);
            }
        });
    }

    private void buildPositiveConnections() {
        for (int neighborID : PeerInfo.getPeerIDs()) {
            if (neighborID != peerHandler.getSelfID()) {
                String neighborAddress = PeerInfo.getPeerAddress(neighborID);
                int neighborPort = PeerInfo.getPeerPort(neighborID);
                peerHandler.getThreadPool().submit(() -> {
                    while (true) {
                        try {
                            buildConnection(new Socket(neighborAddress, neighborPort), true);
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

    private void buildConnection(Socket connection, boolean isPositive) throws Exception {
        connection.setKeepAlive(true);

        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        if (isPositive) {
            oos = new ObjectOutputStream(connection.getOutputStream()); // keep
            ois = new ObjectInputStream(connection.getInputStream());
        } else {
            ois = new ObjectInputStream(connection.getInputStream()); // keep
            oos = new ObjectOutputStream(connection.getOutputStream());
        }

        int neighborID = handshake(connection, ois, oos);

        synchronized (this) {
            if (isPositive) {
                logger.log(Level.INFO, "Positive Connection with Neighbor " + neighborID);
                outMap.put(neighborID, oos);
                PeerLogger.makeConnection(peerHandler.getSelfID(), neighborID);
            } else {
                logger.log(Level.INFO, "Negative Connection with Neighbor " + neighborID);
                inMap.put(neighborID, ois);
                PeerLogger.receiveConnection(peerHandler.getSelfID(), neighborID);
            }

            if (inMap.get(neighborID) != null && outMap.get(neighborID) != null) {
                peerSelector.downloadRegister(neighborID);

                PeerConnection peerConnection = new PeerConnection(peerHandler, this,
                        neighborID, inMap.get(neighborID), outMap.get(neighborID));
                connectedNeighbors.put(neighborID, peerConnection);
                peerHandler.getThreadPool().submit(peerConnection);

                logger.log(Level.INFO, "Make Connection with " + neighborID);
                if (connectedNeighbors.size() == PeerInfo.getPeerIDs().size() - 1) {
                    logger.log(Level.INFO, "Make Connection with All Neighbors");
                }
            }
        }
    }

    private int handshake(Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        // receive message & extract neighbor id
        try {
            // send handshake
            oos.writeObject(new HandshakeMessage(peerHandler.getSelfID()));
            // receive handshake
            Object receivedMessage = ois.readObject();
            if (receivedMessage instanceof HandshakeMessage) {
                return ((HandshakeMessage) receivedMessage).getPeerID();
            } else {
                throw new RuntimeException("The type of received message is not 'HandshakeMessage'");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Fail to send/receive handshake message to/from " +
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

    public void updateUnchokeNeighbors(Set<Integer> interestedNeighbors, Set<Integer> unchokeNeighbors) {
        PeerLogger.changePreferredNeighbors(peerHandler.getSelfID(), new ArrayList<>(unchokeNeighbors));
        synchronized (connectedNeighbors) {
            for (int interestedNeighborID : interestedNeighbors) {
                PeerConnection peerConnection = connectedNeighbors.get(interestedNeighborID);
                if (unchokeNeighbors.contains(interestedNeighborID)) {
                    peerConnection.sendUnchokeMessage();
                } else {
                    peerConnection.sendChokeMessage();
                }
            }
        }
        logger.log(Level.INFO, "Update Unchoke Neighbor: " + unchokeNeighbors);
    }

    public void updateOptimisticallyUnchokeNeighbor(int neighborID) {
        PeerLogger.changeOptimisticallyUnchokedNeighbor(peerHandler.getSelfID(), neighborID);
        synchronized (connectedNeighbors) {
            PeerConnection peerConnection = connectedNeighbors.get(peerHandler.getSelfID());
            peerConnection.sendUnchokeMessage();
        }
        logger.log(Level.INFO, "Update Optimistically Unchoke Neighbor: " + neighborID);
    }

    public void addDownloadedBytes(int neighborID, int byteNumber) {
        peerSelector.addDownloadBytes(neighborID, byteNumber);
    }

    public void sendNotInterestedMessage() {
        synchronized (connectedNeighbors) {
            for (PeerConnection peerConnection : connectedNeighbors.values()) {
                peerConnection.sendNotInterestedMessage();
            }
        }
    }

    public void sendHaveMessage(int index) {
        synchronized (connectedNeighbors) {
            for (PeerConnection peerConnection : connectedNeighbors.values()) {
                peerConnection.sendHaveMessage(index);
            }
        }
    }

    public void sendEndMessage() {
        synchronized (connectedNeighbors) {
            logger.log(Level.INFO, "Send End to " + connectedNeighbors.keySet());
            for (PeerConnection peerConnection : connectedNeighbors.values()) {
                peerConnection.sendEndMessage();
            }
        }
    }

    public void addFinishedPeer(int peerID) {
        synchronized (finishedPeers) {
            if (finishedPeers.add(peerID)) {
                PeerLogger.completionOfDownload(peerID);
                logger.log(Level.INFO, "Finished Peers: " + finishedPeers);
                if (finishedPeers.size() == PeerInfo.getPeerIDs().size()) {
                    logger.log(Level.INFO, "All Peers Successfully Receive Complete File");
                    sendEndMessage();
                    peerHandler.stopPeerProcess();
                }
            }
        }
    }

}
