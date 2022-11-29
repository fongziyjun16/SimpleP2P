package connect;

import config.*;
import connect.message.*;
import connect.message.type.*;
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
    private final Set<Integer> completedPeers = new HashSet<>();
    private final Set<Integer> endNeighbors = new HashSet<>();

    private final PeerSelector peerSelector;

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerRegister.class.getName());

    public PeerRegister(int selfID, byte[] selfBitfield) {
        this.selfID = selfID;
        this.selfBitfield = selfBitfield;
        if (PeerInfo.doesPeerHaveFile(selfID)) {
            completedPeers.add(selfID);
        }
        buildConnection();
        peerSelector = new PeerSelector(this);
    }

    private void buildConnection() {
        buildNegativeConnection();
        buildPositiveConnection();
    }

    private void buildNegativeConnection() {
        PeerController.threadPool.submit(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(PeerInfo.getPeerPort(selfID));
                while (true) {
                    Socket connection = serverSocket.accept();
                    try {
                        connectionInitialize(connection, false);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Fail to connect to " +
                                PeerUtils.getConnectionWholeAddress(connection) +
                                ". Exception Message: " + e.getMessage());
                    }
                    synchronized (connectedNeighbors) {
                        if (connectedNeighbors.size() == PeerInfo.getPeerIDs().size() - 1) {
                            break;
                        }
                    }
                }
                serverSocket.close();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Fail to build negative connection server");
            }
        });
    }

    private void buildPositiveConnection() {
        for (int neighborID : PeerInfo.getPeerIDs()) {
            if (neighborID != selfID) {
                String neighborAddress = PeerInfo.getPeerAddress(neighborID);
                int neighborPort = PeerInfo.getPeerPort(neighborID);
                PeerController.threadPool.submit(() -> {
                    while (true) {
                        try {
                            synchronized (endNeighbors) {
                                if (endNeighbors.contains(neighborID)) {
                                    break;
                                }
                            }
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
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        if (isPositive) {
            oos = new ObjectOutputStream(connection.getOutputStream());
            ois = new ObjectInputStream(connection.getInputStream());
        } else {
            ois = new ObjectInputStream(connection.getInputStream());
            oos = new ObjectOutputStream(connection.getOutputStream());
        }
        int neighborID = handShake(connection, ois, oos);
        synchronized (connectedNeighbors) {
            if (!connectedNeighbors.containsKey(neighborID)) {
                try {
                    PeerConnection peerConnection = new PeerConnection(this, neighborID, connection, ois, oos);
                    connectedNeighbors.put(neighborID, peerConnection);
                    peerSelector.downloadRegister(neighborID);
                    PeerLogger.makeConnection(selfID, neighborID);
                    PeerController.threadPool.submit(peerConnection);
                    logger.log(Level.INFO, "Make Connection with " + neighborID);
                } catch (IOException e) {
                    connectedNeighbors.remove(neighborID);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private int handShake(Socket connection, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        // receive message & extract neighbor id
        try {
            // send handshake
            oos.writeObject(new HandshakeMessage(selfID));

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

    public void sendHave(int index) {
        for (PeerConnection peerConnection : connectedNeighbors.values()) {
            synchronized (peerConnection) {
                try {
                    peerConnection.getOOS().writeObject(new HaveMessage(index));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Fail to Send Have Message to Neighbor " + peerConnection.getNeighborID());
                }
            }
        }
    }

    public void addCompletedPeer(int peerID) {
        synchronized (completedPeers) {
            completedPeers.add(peerID);
            logger.log(Level.INFO, "Finished Peers: " + completedPeers);
            if (completedPeers.size() == PeerInfo.getPeerIDs().size()) {
                logger.log(Level.INFO, "All Neighbors Successfully Receive Complete File");
                for (PeerConnection peerConnection : connectedNeighbors.values()) {
                    synchronized (peerConnection) {
                        try {
                            peerConnection.getOOS().writeObject(new EndMessage());
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.log(Level.SEVERE, "Fail to Send End Message to Neighbor " + peerConnection.getNeighborID());
                        }
                    }
                }
            }
        }
    }

    public void addDownloadedCapacity(int peerID, long byteNumber) {
        peerSelector.addDownloadBytes(peerID, byteNumber);
    }

    public void updateUnchokedNeighbors(Set<Integer> interestedNeighbors, Set<Integer> unchokeNeighbors) {
        PeerLogger.changePreferredNeighbors(selfID, new ArrayList<>(unchokeNeighbors));
        // send unchoke and choke
        for (int neighborID : interestedNeighbors) {
            PeerConnection peerConnection = connectedNeighbors.get(neighborID);
            if (unchokeNeighbors.contains(neighborID)) {
                // send unchoke
                synchronized (peerConnection.getChokeState()) {
                    if (peerConnection.getChokeState()[0]) {
                        synchronized (peerConnection) {
                            try {
                                peerConnection.getOOS().writeObject(new UnchokeMessage());
                                peerConnection.getChokeState()[0] = false;
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Fail to Send Unchoke Message to Neighbor " + peerConnection.getNeighborID());
                            }
                        }
                    }
                }
            } else {
                // send choke
                synchronized (peerConnection.getChokeState()) {
                    if (peerConnection.getChokeState()[0]) {
                        synchronized (peerConnection) {
                            try {
                                peerConnection.getOOS().writeObject(new ChokeMessage());
                                peerConnection.getChokeState()[0] = true;
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Fail to Send Choke Message to Neighbor " + peerConnection.getNeighborID());
                            }
                        }
                    }
                }
            }
        }
    }

    public void updateOptimisticallyUnchokedNeighbor(int neighborID) {
        PeerLogger.changeOptimisticallyUnchokedNeighbor(selfID, neighborID);
        // send unchoke
        PeerConnection peerConnection = connectedNeighbors.get(neighborID);
        synchronized (peerConnection.getOOS()) {
            try {
                peerConnection.getOOS().writeObject(new UnchokeMessage());
                peerConnection.getChokeState()[0] = false;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Fail to Send Unchoke Message to Neighbor " + peerConnection.getNeighborID());
            }
        }
    }

    public void neighborEnd(int neighborID) {
        synchronized (endNeighbors) {
            endNeighbors.add(neighborID);
            if (endNeighbors.size() == PeerInfo.getPeerIDs().size() - 1) {
                logger.log(Level.INFO, "All neighbors end.");
                peerSelector.stopScheduler();
                PeerController.stop();
                System.exit(0);
            }
        }
    }

    public int getSelfID() {
        return selfID;
    }

    public byte[] getSelfBitfield() {
        return selfBitfield;
    }
}
