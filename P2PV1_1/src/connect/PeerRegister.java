package connect;

import config.Common;
import config.PeerInfo;
import main.*;
import utils.PeerUtils;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.*;

public class PeerRegister {

    private final int selfID;
    private final byte[] selfBitfield;

    private final PeerSelector peerSelector;
    private final Map<Integer, PeerConnection> connectedNeighbors = new HashMap<>();
    private final Set<Integer> completedPeers = new HashSet<>();

    // runtime Logger
    private static final Logger logger = Logger.getLogger(PeerRegister.class.getName());

    public PeerRegister(int selfID, byte[] selfBitfield) {
        this.selfID = selfID;
        this.selfBitfield = selfBitfield;
        peerSelector = new PeerSelector(this);
        buildConnection();
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
                        connectionInitialize(connection);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Fail to connect to " +
                                PeerUtils.getConnectionWholeAddress(connection) +
                                ". Exception Message: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
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
                            connectionInitialize(new Socket(neighborAddress, neighborPort));
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

    private void connectionInitialize(Socket connection) throws Exception {
        connection.setKeepAlive(true);
        int neighborID = handShake(connection);
        synchronized (connectedNeighbors) {
            if (!connectedNeighbors.containsKey(neighborID)) {
                PeerConnection peerConnection = new PeerConnection(this, neighborID, connection);
                connectedNeighbors.put(neighborID, peerConnection);
                peerSelector.downloadRegister(neighborID);
                PeerLogger.makeConnection(selfID, neighborID);
                PeerController.threadPool.submit(peerConnection);
                logger.log(Level.INFO, "Make Connection with " + neighborID);
            }
        }
    }

    private int handShake(Socket connection) throws Exception {
        MessageSender.sendHandshake(connection, selfID);

        // receive message & extract neighbor id
        try {
            byte[] inputBuffer = new byte[32];
            InputStream inputStream = connection.getInputStream();
            // waiting handshake, message length = 32bytes
            int inputLength = inputStream.read(inputBuffer);
            if (inputLength != 32) {
                throw new RuntimeException("The length of received message is not 32");
            }
            String header = new String(Arrays.copyOfRange(inputBuffer, 0, 18));
            if (!header.equals("P2PFILESHARINGPROJ")) {
                throw new RuntimeException("The head of received message is not \"P2PFILESHARINGPROJ\"");
            }
            int neighborID = ByteBuffer.wrap(inputBuffer, inputBuffer.length - 4, 4).getInt();
            if (!PeerInfo.getPeerIDs().contains(neighborID)) {
                throw new RuntimeException("The extracted neighbor ID is incorrect");
            }
            return neighborID;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fail to receive handshake message from " +
                    PeerUtils.getConnectionWholeAddress(connection));
            throw e;
        }
    }

    public void addInterestedNeighbor(int neighborID) {
        peerSelector.addInterestedNeighbor(neighborID);
    }

    public void removeInterestedNeighbor(int neighborID) {
        peerSelector.removeInterestedNeighbor(neighborID);
    }

    public void updateUnchokedNeighbors(Set<Integer> interestedNeighbors, Set<Integer> unchokedNeighbors) {
        // send unchoke and choke
        for (int neighborID : interestedNeighbors) {
            PeerConnection peerConnection = connectedNeighbors.get(neighborID);
            if (unchokedNeighbors.contains(neighborID)) {
                // send unchoke
                synchronized (peerConnection.getChokeState()) {
                    if (peerConnection.getChokeState()[0]) {
                        MessageSender.sendUnchoke(peerConnection.getConnection(), neighborID);
                    }
                }
            } else {
                // send choke
                synchronized (peerConnection.getChokeState()) {
                    if (!peerConnection.getChokeState()[0]) {
                        MessageSender.sendChoke(peerConnection.getConnection(), neighborID);
                    }
                }
            }
        }
    }

    public void updateOptimisticallyUnchokedNeighbor(int neighborID) {
        // send unchoke
        PeerConnection peerConnection = connectedNeighbors.get(neighborID);
        MessageSender.sendUnchoke(peerConnection.getConnection(), neighborID);
    }

    public void addDownloadedCapacity(int peerID, long byteNumber) {
        peerSelector.addDownloadBytes(peerID, byteNumber);
    }

    public void sendHave(int index) {
        for (Map.Entry<Integer, PeerConnection> entry : connectedNeighbors.entrySet()) {
            MessageSender.sendHave(entry.getValue().getConnection(), entry.getKey(), index);
        }
    }

    public void addCompletedPeer(int peerID) {
        completedPeers.add(peerID);
        if (completedPeers.size() == PeerInfo.getPeerIDs().size()) {
            // terminate
            logger.log(Level.INFO, "File Sharing Completion");
            for (Map.Entry<Integer, PeerConnection> entry : connectedNeighbors.entrySet()) {
                try {
                    MessageSender.sendHead(entry.getValue().getConnection(), 0, (byte) 8);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Fail to send terminate to " + entry.getKey());
                }
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
